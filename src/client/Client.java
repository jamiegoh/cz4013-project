package client;

import utils.*;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private static HashMap<String, byte[]> fileCacheArray = new HashMap<>();
    // Structure to store cache entry last validated time
    private static HashMap<String, HashMap<Integer, Long>> entryLastValidatedTime = new HashMap<>();
    // Structure to store local file last modified time
    private static HashMap<String, HashMap<Integer, Long>> entryLastModifiedTime = new HashMap<>();

    

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
            int offset = Integer.parseInt(parts[1]);
            int readBytes = Integer.parseInt(parts[2]);

            // get smallest last validated time
            long smallestValidatedTime = getSmallestLastValidatedTime(pathname, offset, readBytes);
            System.out.println("Local validated time: " + smallestValidatedTime);

            // if cache is valid
            if (System.currentTimeMillis() - smallestValidatedTime < FRESHNESS_TIME) {
                System.out.println("Cache is fresh");
                received = new String(getFileCache(pathname, offset, readBytes));
            } else {
                // send request to server to getattr of file
                socket.send(processRequest(RequestType.ATTR, parts, pathname));
                // receive response
                DatagramPacket attrResponsePacket = receiveResponse(RequestType.ATTR, parts, pathname);
                String attrReceived = new String(attrResponsePacket.getData(), 0, attrResponsePacket.getLength());

                // if file does not exist
                if (Objects.equals(attrReceived.substring(0, 4), "FAIL")) {
                    System.out.println("FAIL - File does not exist in server");
                    return attrReceived;
                }
                else{
                    // get last modified time of file
                    long serverLastModifiedTime = Long.parseLong(attrReceived);
                    System.out.println("Server file last modified time: " + serverLastModifiedTime);
                    // if last modified time of file is greater than smallestTime, then cache is invalid
                    if (serverLastModifiedTime > getLocalLastModifiedTime(pathname, offset, readBytes)) {
                        System.out.println("Cache is invalid, updating cache...");
                        received = null;
                    } else {
                        System.out.println("File is not changed in server, returning cache...");
                        // cache is valid
                        received = new String(getFileCache(pathname, offset, readBytes));
                        // update last validated time
                        updateLastValidatedTime(pathname, offset, readBytes);
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
                        storeFileCache(pathname, offset, responsePacket.getData());
                        // update last validated time
                        updateLastValidatedTime(pathname, offset, readBytes);
                        // update last modified time
                        updateLastModifiedTime(pathname, offset, readBytes, Long.parseLong(attrReceived));

                    }
                }
            }
        }

        if (received == null){
            // send request
            socket.send(processRequest(requestType, parts, pathname));
            // receive response
            DatagramPacket responsePacket = receiveResponse(requestType, parts, pathname);

            // If we get here, we have received a response
            received = new String(responsePacket.getData(), 0, responsePacket.getLength());
        }



        System.out.println("Client received data: " + received);


        // Listen case
        if (requestType == RequestType.LISTEN) {
            boolean running = true;
            if (received.equals("ACK")){
                socket.setSoTimeout(0);
            }
            if (Objects.equals(received.substring(0, 4), "FAIL")) {
                System.out.println("File does not exist in server");
                return received;
            }
            while (running) {
                byte[] listenResponseBuf = new byte[1024];
                DatagramPacket listenResponsePacket = new DatagramPacket(listenResponseBuf, listenResponseBuf.length);
                socket.receive(listenResponsePacket);
                String listenReceived = new String(listenResponsePacket.getData(), 0, listenResponsePacket.getLength());
                System.out.println("Client received data: " + listenReceived);
                // Store in cache
                storeFileCache(pathname, 0, listenResponsePacket.getData());
                if (listenReceived.equals("INTERVAL ENDED")) {
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
            case CREATE:
                byte[] createRequestBuf = new CreateRequest(pathname, requestId).serialize();
                requestPacket = new DatagramPacket(createRequestBuf, createRequestBuf.length, serverAddress, clientPort);
                break;
            default:
                throw new IllegalArgumentException("Invalid request type: " + requestType);
        }
        return requestPacket;
    }

    // validate cache freshness
    public boolean validateCache(String pathname, int offset, int readBytes) {
        if (!entryLastValidatedTime.containsKey(pathname)) {
            return false;
        }

        // current time
        long currentTime = System.currentTimeMillis();
        
        Map<Integer, Long> lastValidatedTime = entryLastValidatedTime.get(pathname);
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
        if (!entryLastValidatedTime.containsKey(pathname)) {
            return 0;
        }
        Map<Integer, Long> lastValidatedTime = entryLastValidatedTime.get(pathname);
        for (int i = offset; i < offset + readBytes; i++) {
            if (!lastValidatedTime.containsKey(i)) {
                return 0;
            }
            if (lastValidatedTime.get(i) < smallestTime) {
                smallestTime = lastValidatedTime.get(i);
            }
        }
        return smallestTime;
    }

    // get last modified time of file
    public long getLocalLastModifiedTime(String pathname, int offset, int readBytes) {
        long smallestTime = Long.MAX_VALUE;
        if (!entryLastModifiedTime.containsKey(pathname)) {
            return 0;
        }
        Map<Integer, Long> lastModifiedTime = entryLastModifiedTime.get(pathname);
        for (int i = offset; i < offset + readBytes; i++) {
            if (!lastModifiedTime.containsKey(i)) {
                return 0;
            }
            if (lastModifiedTime.get(i) < smallestTime) {
                smallestTime = lastModifiedTime.get(i);
            }
        }
        return smallestTime;
    }

    // update last validated time
    public void updateLastValidatedTime(String pathname, int offset, int readBytes) {
        if (!entryLastValidatedTime.containsKey(pathname)) {
            entryLastValidatedTime.put(pathname, new HashMap<>());
        }
        HashMap<Integer, Long> lastValidatedTime = entryLastValidatedTime.get(pathname);

        long currentTime = System.currentTimeMillis();
        for (int i = offset; i < offset + readBytes; i++) {
            lastValidatedTime.put(i, currentTime);
        }
        entryLastValidatedTime.put(pathname, lastValidatedTime);
    }

    // update last modified time
    public void updateLastModifiedTime(String pathname, int offset, int readBytes, long serverLastModifiedTime) {
        if (!entryLastModifiedTime.containsKey(pathname)) {
            entryLastModifiedTime.put(pathname, new HashMap<>());
        }
        HashMap<Integer, Long> lastModifiedTime = entryLastModifiedTime.get(pathname);
        for (int i = offset; i < offset + readBytes; i++) {
            lastModifiedTime.put(i, serverLastModifiedTime);
        }
        entryLastModifiedTime.put(pathname, lastModifiedTime);
    }

    // store file content in cache
    public void storeFileCache(String pathname, int offset, byte[] data) {
        // get pathname cache and last validated time
        if (!fileCacheArray.containsKey(pathname)) {
            fileCacheArray.put(pathname, new byte[4069]);
        }
        byte[] fileContent = fileCacheArray.get(pathname);

        // store data in cache and update last validated time
        for (int i = 0; i < data.length; i++) {
            fileContent[offset + i] = data[i];
        }

        fileCacheArray.put(pathname, fileContent);
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
