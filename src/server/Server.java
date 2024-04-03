package server;

import utils.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        System.out.println("In notify single sub func");
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


    public void run() throws IOException {
        running = true;
        System.out.println("Server is running on port " + this.serverPort);

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
                    byte[] readBuf = new byte[readBytes];

                    String readPathName = currentDir + "/src/data/" + readFileName; //won't work on Windows //todo: use path separator

                    System.out.println("Current directory: " + currentDir);
                    System.out.println("Pathname: " + readPathName);

                    try (RandomAccessFile file = new RandomAccessFile(readPathName, "r")) {
                        file.seek(readOffset);
                        file.read(readBuf, 0, readBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                        //TODO: respond w/ meaningful error message
                    }
                    responseString = new String(readBuf, StandardCharsets.UTF_8);
                    System.out.println("Server received read request: " + readRequestArgs);
                    responseBuf = responseString.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, clientAddress, clientPort);
                    socket.send(responsePacket);

                    break;

                case INSERT:
                    //TODO: check if file exists
                    Map<String, Object> insertRequestArgs = new InsertRequest(requestPacket, requestId).deserialize();
                    String filename = (String) insertRequestArgs.get("pathname");
                    int writeOffset = (int) insertRequestArgs.get("offset");
                    String data = (String) insertRequestArgs.get("data");

                    String writePathName = currentDir + "/src/data/" + filename;

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
                    DatagramPacket updateResponse = new DatagramPacket(responseBuf, responseBuf.length, clientAddress, clientPort);


                    List<Subscriber> subscribers = Subscriber.getSubscribers(writePathName);
                    System.out.println("Subscribers: " + subscribers);
                    if (subscribers != null) {
                        for (Subscriber subscriber : subscribers) {
                            System.out.println("Currently notifying subscriber: " + subscriber);

                            notifySingleSubscriber(subscriber.getClientAddress(), subscriber.getClientPort(), writePathName);
                        }
                    }
                    socket.send(updateResponse);

                    break;
                case LISTEN:
                    System.out.println("Server received listen request");
                    Map<String, Object> listenRequestArgs = new ListenRequest(requestPacket, requestId).deserialize();

                    String pathname = (String) listenRequestArgs.get("pathname");
                    int monitorInterval = (int) listenRequestArgs.get("monitorInterval");

                    Subscriber subscriber = new Subscriber(clientAddress, clientPort, pathname, monitorInterval);

                    Subscriber.addSubscriber(subscriber);

                    System.out.println("Server received listen request: " + listenRequestArgs);
                    responseString = "SERVER IS LISTENING!";

                    break;

//                case NOTIFY:
//                    Map<String, Object> notifyRequestArgs = new NotifyRequest(requestPacket, requestId).deserialize();
//
//                    String key = (String) notifyRequestArgs.get("key");
//                    InetAddress clientAddress = (InetAddress) notifyRequestArgs.get("clientAddress");
//                    int clientPort = (int) notifyRequestArgs.get("clientPort");
//
//                    try (RandomAccessFile file = new RandomAccessFile(key, "r")) {
//                        byte[] readBuf = new byte[(int) file.length()];
//                        file.seek(0);
//                        file.read(readBuf, 0, (int) file.length());
//                        responseBuf = new String(readBuf, StandardCharsets.UTF_8).getBytes();
//                        DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, clientAddress, clientPort);
//                        socket.send(responsePacket);
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                case STOP:
                    //TODO: remove all subscribers
                    running = false;
                    System.out.println("Server is stopping...");
                    responseString = "SERVER IS STOPPING!";
                    break;

                case ATTR:
                    // get file attributes
                    Map<String, Object> attrRequestArgs = new AttrRequest(requestPacket, requestId).deserialize();
                    String attrFileName = (String) attrRequestArgs.get("pathname");
                    String attrPathName = currentDir + "/src/data/" + attrFileName; //won't work on Windows //todo: use path separator
                    // if file does not exist, return -1
                    if (!Paths.get(attrPathName).toFile().exists()) {
                        responseString = "-1";
                        break;
                    }
                    // return last modified time    
                    long lastModified = Paths.get(attrPathName).toFile().lastModified();
                    responseString = Long.toString(lastModified);
                    System.out.println("Server received attr request: " + attrRequestArgs);
                    break;

                default:
                    System.out.println("Invalid request type: " + receivedRequestType);
                    break;
            }

            if (receivedRequestType != RequestType.READ && receivedRequestType != RequestType.INSERT) {
                responseBuf = responseString.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, clientAddress, clientPort);
                socket.send(responsePacket);
                addProcessedRequest(requestId, responsePacket);
            }

            System.out.println("Server responded with: " + responseString);

        }

        socket.close();
        System.out.println("Server has stopped.");
    }


}
