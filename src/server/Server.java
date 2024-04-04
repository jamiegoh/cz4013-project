package server;

import utils.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;

public class Server {
    // Consts
    // Drop rate
    private static final double DROP_RATE = 0.5;

    // Connection
    private DatagramSocket socket;
    private int serverPort;
    private boolean running;
    private byte[] tempRequestBuf = new byte[4096];
    private byte[] responseBuf;
    

    // history of processed requests, maintained in map for AT_MOST_ONCE semantics
    // request id, DatagramPacket map
    private static HashMap<Integer, DatagramPacket> processedRequests = new HashMap<>();

    // Invocation semantics
    private InvocationSemantics invocationSemantics;

    // Simulation for invocation semantics
    private boolean isSimulation = false;
    




    // Constructor
    public Server(int serverPort, InvocationSemantics invocationSemantics) throws SocketException {
        this.serverPort = serverPort;
        this.invocationSemantics = invocationSemantics;
        socket = new DatagramSocket(this.serverPort);
    }


    // Run server (Core function)
    public void run() throws IOException {
        running = true;
        System.out.println("Server IP address: " + InetAddress.getLocalHost().getHostAddress());
        System.out.println("Server is running on port " + this.serverPort);
        System.out.println("Invocation semantics: " + invocationSemantics);
        System.out.println();


        while (running) {
            // wait for client request
            System.out.println("Server is waiting for packet...");
            DatagramPacket requestPacket = new DatagramPacket(tempRequestBuf, tempRequestBuf.length);
            socket.receive(requestPacket);

            // parse client request
            InetAddress clientAddress = requestPacket.getAddress();
            int clientPort = requestPacket.getPort();

            // requestBuf is tempRequestBuf with null bytes removed
            byte[] requestBuf = new byte[requestPacket.getLength()];
            System.arraycopy(tempRequestBuf, 0, requestBuf, 0, requestPacket.getLength());

            requestPacket = new DatagramPacket(requestBuf, requestBuf.length, clientAddress, clientPort);

            // get request type and request id
            Request requestReceived = new Request(requestPacket);

            RequestType receivedRequestType = requestReceived.getRequestType();
            int requestId = requestReceived.getRequestId();

            // Check invocation semantics
            if (invocationSemantics == InvocationSemantics.AT_MOST_ONCE) {
                // Check if request has been processed before
                if (isProcessed(requestId)) {
                    System.out.println("Request has already been processed.");
                    // send response
                    System.out.println("Sending saved response to client...");
                    DatagramPacket responsePacket = processedRequests.get(requestId);
                    socket.send(responsePacket);
                    continue;
                }
            }
            // else, AT_LEAST_ONCE, so no need to check if request has been processed before


            String responseString = "ACK";

            String currentDir = Paths.get("").toAbsolutePath().toString();
            // switch on request type
            switch (receivedRequestType) {
                case READ:
                    HashMap<String, Object> readRequestArgs = new ReadRequest(requestPacket, requestId).deserialize();
                    System.out.println("Server received read request: " + readRequestArgs);

                    String readFileName = (String) readRequestArgs.get("pathname");
                    int readOffset = (int) readRequestArgs.get("offset");
                    int readBytes = (int) readRequestArgs.get("readBytes");
                    byte[] readBuf;

                    String readPathName = Paths.get(currentDir, "src", "data", readFileName).toString();

                    System.out.println("Current directory: " + currentDir);
                    System.out.println("Pathname: " + readPathName);

                    //Check if file exists before trying to read
                    if (!Paths.get(readPathName).toFile().exists()) {
                        responseString = "FAIL - File does not exist.";
                        break;
                    }

                    try (RandomAccessFile file = new RandomAccessFile(readPathName, "r")) {
                        if (readOffset >= file.length()) {
                            responseString = "FAIL - Offset exceeds file length.";
                            break;
                        } else if (readOffset + readBytes > file.length()) {
                            readBytes = (int) (file.length() - readOffset);
                            readBuf = new byte[readBytes];
                            System.out.println("Read bytes exceeds file length. Reading only " + readBytes + " bytes.");
                        } else {
                            readBuf = new byte[readBytes];
                        }
                        file.seek(readOffset);
                        file.read(readBuf, 0, readBytes);
                        responseString = new String(readBuf, StandardCharsets.UTF_8);
                        
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case INSERT:
                    HashMap<String, Object> insertRequestArgs = new InsertRequest(requestPacket, requestId).deserialize();
                    System.out.println("Server received insert request: " + insertRequestArgs);

                    String filename = (String) insertRequestArgs.get("pathname");
                    int writeOffset = (int) insertRequestArgs.get("offset");
                    String data = (String) insertRequestArgs.get("data");

                    String writePathName = Paths.get(currentDir, "src", "data", filename).toString();

                    if (!Paths.get(writePathName).toFile().exists()) {
                        responseString = "FAIL - File does not exist.";
                        break;
                    } else {

                        try (RandomAccessFile file = new RandomAccessFile(writePathName, "rw")) {
                            long fileLength = file.length();
                            if (writeOffset < fileLength) {
                                byte[] temp = new byte[(int) (fileLength - writeOffset)];
                                // read the rest of the file after the write offset
                                file.seek(writeOffset);
                                file.readFully(temp);
                                file.seek(writeOffset);
                                // write the data to be inserted
                                file.write(data.getBytes(StandardCharsets.UTF_8));
                                // write the rest of the file back
                                file.write(temp);
                            } else {
                                file.seek(fileLength);
                                file.write(data.getBytes(StandardCharsets.UTF_8));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        List<Subscriber> subscribers = Subscriber.getSubscribers(writePathName);
                        System.out.println("Subscribers: " + subscribers);
                        if (subscribers != null) {
                            for (Subscriber subscriber : subscribers) {
                                System.out.println("Currently notifying subscriber: " + subscriber);

                                notifySingleSubscriber(subscriber.getClientAddress(), subscriber.getClientPort(), writePathName);
                            }
                        }

                    }
                    break;

                case LISTEN:
                    HashMap<String, Object> listenRequestArgs = new ListenRequest(requestPacket, requestId).deserialize();
                    System.out.println("Server received listen request: " + listenRequestArgs);

                    String pathname = (String) listenRequestArgs.get("pathname");
                    String listenPathName = Paths.get(currentDir, "src", "data", pathname).toString();

                    if (!Paths.get(listenPathName).toFile().exists()) {
                        responseString = "FAIL - File does not exist.";
                        break;
                    }

                    int monitorInterval = (int) listenRequestArgs.get("monitorInterval");

                    Subscriber subscriber = new Subscriber(clientAddress, clientPort, pathname, monitorInterval);

                    Subscriber.addSubscriber(subscriber);
                    break;

                case STOP:
                    running = false;
                    System.out.println("Server is stopping...");
                    responseString = "SERVER IS STOPPING!";
                    break;

                case ATTR:
                    // get file attributes
                    HashMap<String, Object> attrRequestArgs = new AttrRequest(requestPacket, requestId).deserialize();
                    System.out.println("Server received attr request: " + attrRequestArgs);

                    String attrFileName = (String) attrRequestArgs.get("pathname");
                    String attrPathName = Paths.get(currentDir, "src", "data", attrFileName).toString();

                    if (!Paths.get(attrPathName).toFile().exists()) {
                        responseString = "FAIL - File does not exist.";
                        break;
                    }
                    // return last modified time
                    long lastModified = Paths.get(attrPathName).toFile().lastModified();
                    responseString = Long.toString(lastModified);

                    Instant instant = Instant.ofEpochMilli(lastModified);
                    String displayString = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .format(instant);
                    System.out.println("Last modified time: " + displayString);
                    break;

                case CREATE:
                    HashMap<String, Object> createRequestArgs = new CreateRequest(requestPacket, requestId).deserialize();
                    System.out.println("Server received create request: " + createRequestArgs);

                    String createFileName = (String) createRequestArgs.get("pathname");
                    String createPathName = Paths.get(currentDir, "src", "data", createFileName).toString();

                    // create directories if they don't exist
                    Paths.get(createPathName).getParent().toFile().mkdirs();


                    // create file
                    System.out.println("Creating file: " + Paths.get(createPathName).toString());
                    boolean creation = Paths.get(createPathName).toFile().createNewFile();
                    if (creation) {
                        responseString = "ACK - File Created";
                    } else {
                        responseString = "ACK - File already exists, creating new file with timestamp appended.";
                        // append timestamp to filename
                        createPathName = createPathName + "_" + System.currentTimeMillis();
                        Paths.get(createPathName).toFile().createNewFile();
                    }
                    break;

                case SEARCH:
                    //search based on substring
                    HashMap<String, Object> searchRequestArgs = new SearchRequest(requestPacket, requestId).deserialize();
                    System.out.println("Server received search request: " + searchRequestArgs);

                    String searchString = (String) searchRequestArgs.get("searchQuery");
                    String searchPathName = Paths.get(currentDir, "src", "data").toString();

                    System.out.println("Searching for substring: " + searchString);
                    System.out.println("Current directory: " + currentDir);

                    // search for files in data directory
                    responseString = searchForSubString(Paths.get(searchPathName), searchString);
                    break;

                default:
                    System.out.println("Invalid request type: " + receivedRequestType);
                    break;
            }

            DatagramPacket responsePacket = processResponse(receivedRequestType, requestId, responseString, clientAddress, clientPort);
            // Store processed request in history
            addProcessedRequest(requestId, responsePacket);
            // send response
            sendResponse(responsePacket, requestId, responseString);
            
            

        }
        socket.close();
        System.out.println("Server has stopped.");
    }

    // Send response
    public void sendResponse(DatagramPacket responsePacket, int requestId, String responseString) throws IOException {
        // if we are simulating
        if (isSimulation){
            System.out.println("Simulation mode is on.");
            // Randomly drop packets
            if (Math.random() < DROP_RATE){
                System.out.println("Simulating server packet loss...");
                System.out.println("Dropping packet from server with request id: " + requestId);
            }
            else{
                System.out.println("Packet is not dropped! Sending response...");
                socket.send(responsePacket);
                System.out.println("Server responded with: " + responseString);
            }
        }
        else{
            socket.send(responsePacket);
            System.out.println("Server responded with: " + responseString);
        }
        System.out.println();
    }

    // Process request
    public DatagramPacket processResponse(RequestType requestType, int requestId, String responseString, InetAddress clientAddress, int clientPort) {
        byte[] responseBuf = new Response(requestType, requestId, responseString).serialize();
        return new DatagramPacket(responseBuf, responseBuf.length, clientAddress, clientPort);
    }

        // History of processed requests
    public boolean isProcessed(int requestId) {
        return processedRequests.containsKey(requestId);
    }

    public void addProcessedRequest(int requestId, DatagramPacket requestPacket) {
        processedRequests.put(requestId, requestPacket);
    }

    // Notify Single Subscriber
    public void notifySingleSubscriber(InetAddress clientAddress, int clientPort, String key) {
        try (RandomAccessFile file = new RandomAccessFile(key, "r")) {
            byte[] readBuf = new byte[(int) file.length()];
            file.seek(0);
            file.read(readBuf, 0, (int) file.length());
            responseBuf = new String(readBuf, StandardCharsets.UTF_8).getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, clientAddress, clientPort);
            socket.send(responsePacket);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Search
    public String searchForSubString(Path path, String searchString) {
        //Returns the first file that contains the substring
        String result = "FAIL - Substring not found in any files.";
        try {
            List<Path> paths = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .toList();
            for (Path filePath : paths) {
                try {
                    String content = Files.readString(filePath);
                    if (content.contains(searchString)) {
                        System.out.println("Found substring in file: " + filePath);
                        result = "Found " + searchString  + " in file: " + filePath;
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("Error reading file: " + filePath);
                }
            }
        } catch (IOException e) {
            System.out.println("Error walking the file tree: " + path);
        }
        return result;
    }
}
