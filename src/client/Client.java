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
    private InetAddress address;
    private int port;

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
        this.address = InetAddress.getByName(serverAddress);
        this.port = port;
        this.invocationSemantics = invocationSemantics;
        socket = new DatagramSocket();
    }

    public String makeRequest(RequestType requestType, String input) throws IOException {

        // parse input
        String[] parts = input.split(",");
        String pathname = requestType == RequestType.STOP ? null : parts[0];

        String received;
        // Check if cache is valid
        if (requestType == RequestType.READ && validateCache(pathname, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]))) {
            System.out.println("Cache is valid, returning cached data");
            received = new String(getFileCache(pathname, Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
        }
        else{
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


        if(requestType == RequestType.INSERT && received.equals("ACK")) {
            System.out.println("notify subscribers w/ pathname " + pathname);
            Subscriber.notifySubscribers(pathname);
        }

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
                byte[] readRequestBuf = new ReadRequest(pathname, readOffset, readBytes, ReadType.NORMAL, requestId).serialize();
                requestPacket = new DatagramPacket(readRequestBuf, readRequestBuf.length, address, port);
                break;
            case INSERT:
                int writeOffset = Integer.parseInt(parts[1]);
                String data = parts[2];
                byte[] insertRequestBuf = new InsertRequest(pathname, writeOffset, data, requestId).serialize();
                requestPacket = new DatagramPacket(insertRequestBuf, insertRequestBuf.length, address, port);
                // todo: should we cache the data here?
                break;
            case LISTEN:
                int monitorInterval = Integer.parseInt(parts[1]);
                byte[] listenRequestBuf = new ListenRequest(address, pathname, monitorInterval, requestId).serialize();
                requestPacket = new DatagramPacket(listenRequestBuf, listenRequestBuf.length, address, port);
                break;
            case STOP:
                byte[] stopRequestBuf = new StopRequest(requestId).serialize();
                requestPacket = new DatagramPacket(stopRequestBuf, stopRequestBuf.length, address, port);
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
