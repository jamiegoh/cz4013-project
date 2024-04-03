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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Server {
    private DatagramSocket socket;
    private boolean running;
    private byte[] tempRequestBuf = new byte[1024];
    private byte[] responseBuf;

    // history of processed requests, maintained in map for AT_MOST_ONCE semantics
    // request id, DatagramPacket map
    private static Map<Integer, DatagramPacket> processedRequests = new HashMap<>();


    private InvocationSemantics invocationSemantics;

    private int serverPort;

    public Server(int serverPort, InvocationSemantics invocationSemantics) throws SocketException {
        this.serverPort = serverPort;
        this.invocationSemantics = invocationSemantics;
        socket = new DatagramSocket(this.serverPort);
    }

    public boolean isProcessed(int requestId) {
        return processedRequests.containsKey(requestId);
    }

    public void addProcessedRequest(int requestId, DatagramPacket requestPacket) {
        processedRequests.put(requestId, requestPacket);
    }

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

    public void run() throws IOException {
        running = true;
        // output server ip address
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

            RequestType receivedRequestType = new Request(requestPacket).getRequestType();
            int requestId = new Request(requestPacket).getRequestId();

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
                    Map<String, Object> readRequestArgs = new ReadRequest(requestPacket, requestId).deserialize();
                    String readFileName = (String) readRequestArgs.get("pathname");
                    int readOffset = (int) readRequestArgs.get("offset");
                    int readBytes = (int) readRequestArgs.get("readBytes");
                    byte[] readBuf;

                    String readPathName = currentDir + "/src/data/" + readFileName; //won't work on Windows //todo: use path separator

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
                        System.out.println("Server received read request: " + readRequestArgs);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case INSERT:
                    //TODO: check if file exists
                    Map<String, Object> insertRequestArgs = new InsertRequest(requestPacket, requestId).deserialize();
                    String filename = (String) insertRequestArgs.get("pathname");
                    int writeOffset = (int) insertRequestArgs.get("offset");
                    String data = (String) insertRequestArgs.get("data");

                    String writePathName = currentDir + "/src/data/" + filename;

                    if (!Paths.get(writePathName).toFile().exists()) {
                        responseString = "FAIL - File does not exist.";
                        break;
                    } else {

                        try (RandomAccessFile file = new RandomAccessFile(writePathName, "rw")) {
                            long fileLength = file.length();
                            if (writeOffset < fileLength) {
                                byte[] temp = new byte[(int) (fileLength - writeOffset)];
                                file.seek(writeOffset);
                                file.readFully(temp);
                                file.seek(writeOffset);
                                file.write(data.getBytes(StandardCharsets.UTF_8));
                                file.write(temp);
                            } else {
                                file.seek(fileLength);
                                file.write(data.getBytes(StandardCharsets.UTF_8));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        System.out.println("Server received insert request: " + insertRequestArgs);

                        responseBuf = responseString.getBytes();


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
                    System.out.println("Server received listen request");
                    Map<String, Object> listenRequestArgs = new ListenRequest(requestPacket, requestId).deserialize();

                    String pathname = (String) listenRequestArgs.get("pathname");

                    if (!Paths.get(pathname).toFile().exists()) {
                        responseString = "FAIL - File does not exist.";
                        break;
                    }

                    int monitorInterval = (int) listenRequestArgs.get("monitorInterval");

                    Subscriber subscriber = new Subscriber(clientAddress, clientPort, pathname, monitorInterval);

                    Subscriber.addSubscriber(subscriber);

                    System.out.println("Server received listen request: " + listenRequestArgs);

                    break;

                case STOP:
                    running = false;
                    System.out.println("Server is stopping...");
                    responseString = "SERVER IS STOPPING!";
                    break;

                case ATTR:
                    // get file attributes
                    Map<String, Object> attrRequestArgs = new AttrRequest(requestPacket, requestId).deserialize();
                    String attrFileName = (String) attrRequestArgs.get("pathname");
                    String attrPathName = currentDir + "/src/data/" + attrFileName; //won't work on Windows //todo: use path separator

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
                    System.out.println("Server received attr request: " + attrRequestArgs);
                    break;

                case CREATE:
                    Map<String, Object> createRequestArgs = new CreateRequest(requestPacket, requestId).deserialize();
                    String createFileName = (String) createRequestArgs.get("pathname");
                    String createPathName = currentDir + "/src/data/" + createFileName; //won't work on Windows //todo: use path separator


                    // create directories if they don't exist
                    Paths.get(createPathName).getParent().toFile().mkdirs();


                    // create file
                    System.out.println("Creating file: " + Paths.get(createPathName).toString());
                    boolean creation = Paths.get(createPathName).toFile().createNewFile();
                    if (creation) {
                        responseString = "ACK - File Created";
                    } else {
                        responseString = "FAIL - File already exists";
                    }
                    System.out.println("Server received create request: " + createRequestArgs);
                    break;

                case SEARCH:
                    //search based on substring
                    Map<String, Object> searchRequestArgs = new SearchRequest(requestPacket, requestId).deserialize();
                    String searchString = (String) searchRequestArgs.get("searchQuery");
                    String searchPathName = currentDir + "/src/data/";

                    System.out.println("Searching for substring: " + searchString);
                    System.out.println("Current directory: " + currentDir);

                    // search for files in data directory
                    responseString = searchForSubString(Paths.get(searchPathName), searchString);
                    break;

                        default:
                            System.out.println("Invalid request type: " + receivedRequestType);
                            break;
                    }

                    responseBuf = responseString.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, clientAddress, clientPort);
                    socket.send(responsePacket);
                    addProcessedRequest(requestId, responsePacket);

                    System.out.println("Server responded with: " + responseString);

            }

            socket.close();
            System.out.println("Server has stopped.");
        }


    }
