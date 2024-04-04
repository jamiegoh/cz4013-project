package client;

import utils.*;

import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

public class Client {

    // Constants
    // timeout
    private static final int TIMEOUT = 1000;
    // Max number of retries
    private static final int MAX_RETRIES = 3;
    // Drop rate
    private static final double DROP_RATE = 0.5;


    // Connection
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;
    private String clientAddress;
    private int clientPort;

    // Request id count
    private int requestId = 0;
    
    // Invocation semantics
    private InvocationSemantics invocationSemantics;

    // Simulation for invocation semantics
    private boolean isSimulation = false;

    // Cache of responses
    // Freshness time (in milliseconds)
    private int freshnessInterval;
    // Structure to store file content, considering pathname, byte in each entry
    private static HashMap<String, byte[]> fileCacheArray = new HashMap<>();
    // Structure to store cache entry last validated time
    private static HashMap<String, HashMap<Integer, Long>> entryLastValidatedTime = new HashMap<>();
    // Structure to store local file last modified time
    private static HashMap<String, HashMap<Integer, Long>> entryLastModifiedTime = new HashMap<>();



    

    // Constructor
    public Client(String serverAddress, int serverPort, InvocationSemantics invocationSemantics, int freshnessInterval) throws UnknownHostException, SocketException {
        this.socket = new DatagramSocket();
        this.serverAddress = InetAddress.getByName(serverAddress);
        this.serverPort = serverPort;
        this.clientAddress = InetAddress.getLocalHost().getHostAddress();
        this.clientPort = socket.getLocalPort();
        this.invocationSemantics = invocationSemantics;
        this.freshnessInterval = freshnessInterval;
    }

    // Run client (Core function)
    public void run() throws IOException {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("Client IP address: " + clientAddress);
        System.out.println("Client is running on port: " + clientPort);
        System.out.println("Client is connected to server: " + serverAddress + ":" + serverPort);
        System.out.println("Invocation semantics: " + invocationSemantics);
        System.out.println("Freshness interval: " + freshnessInterval);
        System.out.println();

        while (running) {
            System.out.println("Which service would you like to perform on server (READ, INSERT, LISTEN, CREATE, ATTR, STOP, SEARCH)?");
            System.out.println("Enter QUIT to exit local client.");
            String requestTypeStr = scanner.nextLine();

            if (requestTypeStr.equals("QUIT")) {
                running = false;
                continue;
            }

            RequestType requestType;
            try {
                requestType = RequestType.valueOf(requestTypeStr.toUpperCase());
            }
            catch (IllegalArgumentException e) {
                System.out.println("Invalid request type. Use READ, INSERT, LISTEN, CREATE, ATTR, STOP or SEARCH");
                continue;
            }

            String input;
            switch (requestType) {
                case READ:
                    System.out.println("Enter the pathname, offset, and readBytes separated by commas:");
                    input = scanner.nextLine();
                    if (!validateInput(requestType, input)) {
                        System.out.println("Invalid input. Please try again.");
                        continue;
                    }
                    break;
                case INSERT:
                    System.out.println("Enter the pathname, offset, and data separated by commas:");
                    input = scanner.nextLine();
                    if (!validateInput(requestType, input)) {
                        System.out.println("Invalid input. Please try again.");
                        continue;
                    }
                    break;
                case LISTEN:
                    System.out.println("Enter the pathname and monitor interval (minutes) separated by commas:");
                    input = scanner.nextLine();
                    if (!validateInput(requestType, input)) {
                        System.out.println("Invalid input. Please try again.");
                        continue;
                    }
                    break;
                case CREATE:
                    System.out.println("Enter a pathname to create: e.g. /dir1/file1");
                    input = scanner.nextLine();
                    if (!validateInput(requestType, input)) {
                        System.out.println("Invalid input. Please try again.");
                        continue;
                    }
                    break;
                case ATTR:
                    System.out.println("Enter a pathname to get time last modified e.g. /dir1/file1");
                    input = scanner.nextLine();
                    if (!validateInput(requestType, input)) {
                        System.out.println("Invalid input. Please try again.");
                        continue;
                    }
                    break;
                case STOP:
                    makeRequest(requestType, "");
                    running = false;
                    continue;
                case SEARCH:
                    System.out.println("Enter a substring to search for the file it first appears in)");
                    input = scanner.nextLine();
                    if (!validateInput(requestType, input)) {
                        System.out.println("Invalid input. Please try again.");
                        continue;
                    }
                    break;
                default:
                    System.out.println("Invalid request type. Use READ, INSERT, LISTEN, CREATE, ATTR, STOP or SEARCH");
                    continue;
            }

            makeRequest(requestType, input);
            
        }
        scanner.close();
        close();
    }

    //Check if input arguments are the correct number for the request type
    public static boolean validateInput(RequestType requestType, String input) {
        String[] inputArr = input.split(",");
        switch (requestType) {
            case READ, INSERT:
                return inputArr.length == 3;
            case LISTEN:
                return inputArr.length == 2;
            case CREATE, ATTR, SEARCH:
                return inputArr.length == 1;
            default:
                return false;
        }
    }

    public String makeRequest(RequestType requestType, String input) throws IOException {

        // parse input
        String[] parts = input.split(",");
        String pathname = requestType == RequestType.STOP ? null : parts[0];

        String received = null;

        // Process Cache
        // Check if cache is valid
        if (requestType == RequestType.READ) {
            System.out.println("Processing cache for READ request...");
            int offset = Integer.parseInt(parts[1]);
            int readBytes = Integer.parseInt(parts[2]);

            // if cache is valid
            if ( validateCache(pathname, offset, readBytes)) {
                System.out.println("Cache is fresh");
                received = new String(getFileCache(pathname, offset, readBytes));
            } else {
                // send request to server to getattr of file
                sendRequest(RequestType.ATTR, parts, pathname, false);
                // receive response
                DatagramPacket attrResponsePacket = receiveResponse(RequestType.ATTR, parts, pathname);
                if (attrResponsePacket == null) {
                    return null;
                }
                String attrReceived = processResponse(attrResponsePacket);

                // if file does not exist
                if (Objects.equals(attrReceived.substring(0, 4), "FAIL")) {
                    System.out.println("FAIL - File does not exist in server");
                    return attrReceived;
                }
                else{
                    // get last modifi ed time of file
                    long serverLastModifiedTime = Long.parseLong(attrReceived);
                    // format time
                    Instant instant = Instant.ofEpochMilli(serverLastModifiedTime);
                    String displayString = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(instant);
                    System.out.println("Server file last modified time: " + displayString);

                    long localLastModifiedTime = getLocalLastModifiedTime(pathname, offset, readBytes);
                    instant = Instant.ofEpochMilli(localLastModifiedTime);
                    displayString = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(instant);
                    System.out.println("Local file last modified time: " + displayString);

                    // if last modified time of file is greater than smallestTime, then cache is invalid
                    if (serverLastModifiedTime > localLastModifiedTime) {
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
                    sendRequest(requestType, parts, pathname, false);

                    // receive response
                    DatagramPacket responsePacket = receiveResponse(requestType, parts, pathname);
                    if (responsePacket == null) {
                        return null;
                    }

                    // If we get here, we have received a response
                    received = processResponse(responsePacket);

                    // Caching the response
                    if (requestType == RequestType.READ) {
                        storeFileCache(pathname, offset, received.getBytes());
                        // update last validated time
                        updateLastValidatedTime(pathname, offset, readBytes);
                        // update last modified time
                        updateLastModifiedTime(pathname, offset, readBytes, Long.parseLong(attrReceived));

                    }
                }
            }
        }

        // Send Request and Receive Response
        if (received == null){
            // send request
            sendRequest(requestType, parts, pathname, false);
            // receive response
            DatagramPacket responsePacket = receiveResponse(requestType, parts, pathname);
            if (responsePacket == null) {
                return null;
            }

            // If we get here, we have received a response
            received = processResponse(responsePacket);
        }

        // Print received data
        System.out.println("Client received data: " + received);


        // Handle Listen case, keep alive
        if ((requestType == RequestType.LISTEN) && (!received.equals("INTERVAL ENDED"))) {
            boolean running = true;
            if (received.equals("ACK")){
                socket.setSoTimeout(0);
            }
            else if (Objects.equals(received.substring(0, 4), "FAIL")) {
                System.out.println("File does not exist in server");
                return received;
            }
            while (running) {
                byte[] listenResponseBuf = new byte[4096];
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

        return received;
    }

    // Send request
    public void sendRequest(RequestType requestType, String[] parts, String pathname, boolean isRetry) throws IOException {
        // if we are simulating
        if (isSimulation){
            System.out.println("Simulation mode is on.");
            // Randomly drop packets
            if (Math.random() < DROP_RATE){
                System.out.println("Simulating client packet loss...");
                System.out.println("Dropping packet from client with request id: " + requestId);
            }
            else{
                socket.send(processRequest(requestType, parts, pathname, isRetry));
                System.out.println("Packet is not dropped! Sending " + requestType + " Request " + "(Id: " + requestId + ") to server...");
            }
        }
        else{
            socket.send(processRequest(requestType, parts, pathname, isRetry));
            System.out.println("Sending " + requestType + " Request " + "(Id: " + requestId + ") to server...");
        }
        System.out.println();
    }
    
    // Receive response
    public DatagramPacket receiveResponse(RequestType requestType, String[] parts, String pathname) throws IOException {
        // receive response
        byte[] responseBuf = new byte[4096];
        DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);

        socket.setSoTimeout(TIMEOUT);
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                socket.receive(responsePacket);
                return responsePacket;
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout, retrying...");
                sendRequest(requestType, parts, pathname, true);
            }
            retries++;
            if (retries == MAX_RETRIES) {
                try {
                    socket.receive(responsePacket);
                    return responsePacket;
                } catch (SocketTimeoutException e) {
                    System.out.println("Max retries reached, exiting...");
                    return null;
                }
            }
        }

       return null;
    }

    public String processResponse(DatagramPacket responsePacket) {
        Response responseReceived = null;
        try{
            responseReceived = new Response(responsePacket);
        }
        catch (IllegalArgumentException e){
            // This is caused by packet loss
            // ACK might be lost, hence if we receive an invalid response, we check the content
            String responseStr = new String(responsePacket.getData(), 0, responsePacket.getLength());
            System.out.println("Invalid response received: " + responseStr);
            return responseStr;
        }
        
        HashMap<String, Object> responseMap = responseReceived.deserialize();
        System.out.println("Server response to Request Type: " + responseMap.get("clientRequestType"));
        System.out.println("Server response to Request Id: " + responseMap.get("requestId"));
        System.out.println("Returning message...");
        return (String) responseMap.get("message");
    }


    // Process request
    public DatagramPacket processRequest(RequestType requestType, String[] parts, String pathname, boolean isRetry) {
        // Increment request id
        if (!isRetry){
            requestId++;
        }

        DatagramPacket requestPacket;
        // process request
        switch (requestType) {
            case READ:
                int readOffset = Integer.parseInt(parts[1]);
                int readBytes = Integer.parseInt(parts[2]);
                byte[] readRequestBuf = new ReadRequest(pathname, readOffset, readBytes, requestId).serialize();
                requestPacket = new DatagramPacket(readRequestBuf, readRequestBuf.length, serverAddress, serverPort);
                break;
            case INSERT:
                int writeOffset = Integer.parseInt(parts[1]);
                String data = parts[2];
                byte[] insertRequestBuf = new InsertRequest(pathname, writeOffset, data, requestId).serialize();
                requestPacket = new DatagramPacket(insertRequestBuf, insertRequestBuf.length, serverAddress, serverPort);
                break;
            case LISTEN:
                int monitorInterval = Integer.parseInt(parts[1]);
                byte[] listenRequestBuf = new ListenRequest(pathname, monitorInterval, requestId).serialize();
                requestPacket = new DatagramPacket(listenRequestBuf, listenRequestBuf.length, serverAddress, serverPort);
                break;
            case STOP:
                byte[] stopRequestBuf = new StopRequest(requestId).serialize();
                requestPacket = new DatagramPacket(stopRequestBuf, stopRequestBuf.length, serverAddress, serverPort);
                break;
            case ATTR:
                byte[] attrRequestBuf = new AttrRequest(pathname, requestId).serialize();
                requestPacket = new DatagramPacket(attrRequestBuf, attrRequestBuf.length, serverAddress, serverPort);
                break;
            case CREATE:
                byte[] createRequestBuf = new CreateRequest(pathname, requestId).serialize();
                requestPacket = new DatagramPacket(createRequestBuf, createRequestBuf.length, serverAddress, serverPort);
                break;
            case SEARCH:
                byte[] searchRequestBuf = new SearchRequest(pathname, requestId).serialize();
                requestPacket = new DatagramPacket(searchRequestBuf, searchRequestBuf.length, serverAddress, serverPort);
                break;
            default:
                throw new IllegalArgumentException("Invalid request type: " + requestType);
        }

        return requestPacket;
    }

    // validate cache freshness
    public boolean validateCache(String pathname, int offset, int readBytes) {

        // get smallest last validated time
        long smallestValidatedTime = getSmallestLastValidatedTime(pathname, offset, readBytes);

        if (smallestValidatedTime == 0) {
            return false;
        }
        Instant instant = Instant.ofEpochMilli(smallestValidatedTime);
        String displayString = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(instant);

        System.out.println("Local validated time: " + displayString);

        if (System.currentTimeMillis() - smallestValidatedTime < freshnessInterval) {
            return true;
        }
        else {
            return false;
        }
    }

    // get smallest last validated time
    public long getSmallestLastValidatedTime(String pathname, int offset, int readBytes) {
        long smallestTime = Long.MAX_VALUE;
        if (!entryLastValidatedTime.containsKey(pathname)) {
            return 0;
        }
        HashMap<Integer, Long> lastValidatedTime = entryLastValidatedTime.get(pathname);
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
        HashMap<Integer, Long> lastModifiedTime = entryLastModifiedTime.get(pathname);
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
        System.out.println("Updating local last validated time...");
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
        System.out.println("Updating local last modified time...");
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
            fileCacheArray.put(pathname, new byte[4096]);
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
