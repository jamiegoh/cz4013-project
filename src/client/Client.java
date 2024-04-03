package client;

import utils.*;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class Client {
    // Invocation semantics
    private InvocationSemantics invocationSemantics;
    // timeout
    private static final int TIMEOUT = 1000;
    // Max number of retries
    private static final int MAX_RETRIES = 3;
    // Request id 
    private int requestId = 1;

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private String clientAddress;
    private int clientPort;

    // Cache of responses
    // Freshness time
    private static final long FRESHNESS_TIME = 1000;
    // Structure to store file content, considering pathname, byte in each entry
    private static Map<String, byte[]> fileCacheArray = new HashMap<>();
    // Structure to store cache entry last validated time
    private static Map<String, Map<Integer, Long>> fileLastValidatedTime = new HashMap<>();

    

    // Constructor
    public Client(String serverAddress, int port, InvocationSemantics invocationSemantics) throws UnknownHostException, SocketException {
//        address = InetAddress.getByName("localhost");
        this.serverAddress = InetAddress.getByName(serverAddress);
        this.clientAddress = InetAddress.getLocalHost().getHostAddress();
        this.clientPort = port;
        this.invocationSemantics = invocationSemantics;
        socket = new DatagramSocket();
    }

    public String makeRequest(RequestType requestType, String input) throws IOException {

        // parse input
        String[] parts = input.split(",");
        String pathname = requestType == RequestType.STOP ? null : parts[0];

        String received = null;
        // Check if cache is valid
        if (requestType == RequestType.READ) {
            // get smallest last validated time
            long smallestTime = getSmallestLastValidatedTime(pathname, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));

            // if cache is valid
            if (System.currentTimeMillis() - smallestTime < FRESHNESS_TIME) {
                received = new String(getFileCache(pathname, Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            } else {
                // send request to server to getattr of file
                socket.send(processRequest(RequestType.ATTR, parts, pathname));
                // receive response
                DatagramPacket attrResponsePacket = receiveResponse(RequestType.ATTR, parts, pathname);
                String attrReceived = new String(attrResponsePacket.getData(), 0, attrResponsePacket.getLength());

                // if file does not exist
                if (attrReceived.equals("-1")) {
                    System.out.println("File does not exist");
                }
                else{
                    // get last modified time of file
                    long lastModifiedTime = Long.parseLong(attrReceived);
                    // if last modified time of file is greater than smallestTime, then cache is invalid
                    if (lastModifiedTime > smallestTime) {
                        received = null;
                    } else {
                        // cache is valid
                        received = new String(getFileCache(pathname, Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                    }
                }
            }
        }
        
        if (received == null) {
            // send request
            socket.send(processRequest(requestType, parts, pathname));
            // receive response
            DatagramPacket responsePacket = receiveResponse(requestType, parts, pathname);

            // If we get here, we have received a response
            received = new String(responsePacket.getData(), 0, responsePacket.getLength());

            // Caching the response
            if (requestType == RequestType.READ) {
                storeFileCache(pathname, Integer.parseInt(parts[1]), responsePacket.getData());
            }
        }


//        if(requestType == RequestType.INSERT && received.equals("ACK")) {
//            System.out.println("notify subscribers w/ pathname " + pathname);
//            Subscriber.notifySubscribers(pathname);
//        }

        System.out.println("Client received data: " + received);

//        if (received.equals("ACK")) {
//            System.out.println("Client received ACK :)");
//        } else {
//            System.out.println("Client did not receive ACK :(");
//            // todo: resend based on resend policy (lecturer calls it at-most-once/at-least-once)
//        }

        // Listen case
        if (requestType == RequestType.LISTEN) {
            boolean running = true;
            socket.setSoTimeout(0);
            while (running) {
                byte[] listenResponseBuf = new byte[1024];
                DatagramPacket listenResponsePacket = new DatagramPacket(listenResponseBuf, listenResponseBuf.length);
                socket.receive(listenResponsePacket);
                String listenReceived = new String(listenResponsePacket.getData(), 0, listenResponsePacket.getLength());
                System.out.println("Client received data: " + listenReceived);
                // Store in cache
                storeFileCache(pathname, Integer.parseInt(parts[1]), listenResponsePacket.getData());
                if (received.equals("INTERVAL ENDED")) {
//                        running = false;
                    return listenReceived;
                }
            }
        }

        // Increment request id
        requestId++;

        return received;
    }

    // Receive response
    public DatagramPacket receiveResponse(RequestType requestType, String[] parts, String pathname) throws IOException {
        // receive response
        byte[] responseBuf = new byte[1024];
        DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);

        socket.setSoTimeout(TIMEOUT);
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                socket.receive(responsePacket);
                break;
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout, retrying...");
                socket.send(processRequest(requestType, parts, pathname));
                retries++;
            }
        }

        if (retries == MAX_RETRIES) {
            System.out.println("Max retries reached, exiting...");
            return null;
        }

        return responsePacket;
    }


    // Process request
    public DatagramPacket processRequest(RequestType requestType, String[] parts, String pathname) {
        DatagramPacket requestPacket;
        // process request
        switch (requestType) {
            case READ:
                //TODO: handle if offset exceeds file size
                int readOffset = Integer.parseInt(parts[1]);
                int readBytes = Integer.parseInt(parts[2]);
                byte[] readRequestBuf = new ReadRequest(pathname, readOffset, readBytes, requestId).serialize();
                requestPacket = new DatagramPacket(readRequestBuf, readRequestBuf.length, serverAddress, clientPort);
                break;
            case INSERT:
                int writeOffset = Integer.parseInt(parts[1]);
                String data = parts[2];
                byte[] insertRequestBuf = new InsertRequest(pathname, writeOffset, data, requestId).serialize();
                requestPacket = new DatagramPacket(insertRequestBuf, insertRequestBuf.length, serverAddress, clientPort);
                // todo: should we cache the data here?
                break;
            case LISTEN:
                int monitorInterval = Integer.parseInt(parts[1]);
                // todo: are we passing server own address here?
                byte[] listenRequestBuf = new ListenRequest(pathname, monitorInterval, requestId).serialize();
                requestPacket = new DatagramPacket(listenRequestBuf, listenRequestBuf.length, serverAddress, clientPort);
                break;
            case STOP:
                byte[] stopRequestBuf = new StopRequest(requestId).serialize();
                requestPacket = new DatagramPacket(stopRequestBuf, stopRequestBuf.length, serverAddress, clientPort);
                break;
            case ATTR:
                byte[] attrRequestBuf = new AttrRequest(pathname, requestId).serialize();
                requestPacket = new DatagramPacket(attrRequestBuf, attrRequestBuf.length, serverAddress, clientPort);
                break;
            default:
                throw new IllegalArgumentException("Invalid request type: " + requestType);
        }

        return requestPacket;
    }


    // validate cache freshness
    public boolean validateCache(String pathname, int offset, int readBytes) {
        if (!fileLastValidatedTime.containsKey(pathname)) {
            return false;
        }

        // current time
        long currentTime = System.currentTimeMillis();
        
        Map<Integer, Long> lastValidatedTime = fileLastValidatedTime.get(pathname);
        for (int i = offset; i < offset + readBytes; i++) {
            if (!lastValidatedTime.containsKey(i)) {
                return false;
            }
            if (currentTime - lastValidatedTime.get(i) >= FRESHNESS_TIME) {
                return false;
            }
        }
        return true;
    }

    // get smallest last validated time
    public long getSmallestLastValidatedTime(String pathname, int offset, int readBytes) {
        long smallestTime = Long.MAX_VALUE;
        if (!fileLastValidatedTime.containsKey(pathname)) {
            return 0;
        }
        Map<Integer, Long> lastValidatedTime = fileLastValidatedTime.get(pathname);
        for (int i = offset; i < offset + readBytes; i++) {
            if (lastValidatedTime.containsKey(i) && lastValidatedTime.get(i) < smallestTime) {
                smallestTime = lastValidatedTime.get(i);
            }
        }
        return smallestTime;
    }

    // store file content in cache
    public void storeFileCache(String pathname, int offset, byte[] data) {
        // get pathname cache and last validated time
        if (!fileCacheArray.containsKey(pathname)) {
            fileCacheArray.put(pathname, new byte[4069]);
        }
        byte[] fileContent = fileCacheArray.get(pathname);

        if (!fileLastValidatedTime.containsKey(pathname)) {
            fileLastValidatedTime.put(pathname, new HashMap<>());
        }
        Map<Integer, Long> lastValidatedTime = fileLastValidatedTime.get(pathname);

        long currentTime = System.currentTimeMillis();        
        // store data in cache and update last validated time
        for (int i = 0; i < data.length; i++) {
            fileContent[offset + i] = data[i];
            lastValidatedTime.put(offset + i, currentTime);
        }

        fileCacheArray.put(pathname, fileContent);
        fileLastValidatedTime.put(pathname, lastValidatedTime);
    }

    // get file content from cache
    public byte[] getFileCache(String pathname, int offset, int readBytes) {
        if (!fileCacheArray.containsKey(pathname)) {
            // Not gonna happen, just for error handling
            return null;
        }
        byte[] fileContent = fileCacheArray.get(pathname);
        byte[] data = new byte[readBytes];
        for (int i = 0; i < readBytes; i++) {
            data[i] = fileContent[offset + i];
        }
        return data;
    }

    public void close() {
        socket.close();
    }
}
