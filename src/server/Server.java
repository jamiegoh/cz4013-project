package server;

import utils.ReadRequest;
import utils.Request;
import utils.RequestType;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Map;

public class Server {
    private DatagramSocket socket;
    private boolean running;
    private byte[] tempRequestBuf = new byte[1024];
    private byte[] responseBuf;

    private int port;

    public Server(int port) throws SocketException {
        this.port = port;
        socket = new DatagramSocket(this.port);
    }

    public void run() throws IOException {
        running = true;
        System.out.println("Server is running on port " + port);

        while (running) {
            // wait for client request
            System.out.println("Server is waiting for packet...");
            DatagramPacket requestPacket = new DatagramPacket(tempRequestBuf, tempRequestBuf.length);
            socket.receive(requestPacket);

            // parse client request
            InetAddress address = requestPacket.getAddress();
            int port = requestPacket.getPort();

            // requestBuf is tempRequestBuf with null bytes removed
            byte[] requestBuf = new byte[requestPacket.getLength()];
            System.arraycopy(tempRequestBuf, 0, requestBuf, 0, requestPacket.getLength());

            requestPacket = new DatagramPacket(requestBuf, requestBuf.length, address, port);

            RequestType receivedRequestType = new Request(requestPacket).getRequestType();


            String responseString = "ACK";

            // switch on request type
            switch (receivedRequestType) {
                case READ:
                    Map<String,Object> readRequestArgs = new ReadRequest(requestPacket).deserialize();
                    String filename = (String) readRequestArgs.get("pathname");
                    int offset = (int) readRequestArgs.get("offset");
                    int readBytes = (int) readRequestArgs.get("readBytes");
                    byte[] data = new byte[readBytes];

                    String currentDir = Paths.get("").toAbsolutePath().toString();
                    String pathname = currentDir + "/src/data/" + filename; //won't work on Windows //todo: use path separator

                    System.out.println("Current directory: " + currentDir);
                    System.out.println("Pathname: " + pathname);

                    try (RandomAccessFile file = new RandomAccessFile(pathname, "r")) {
                        file.seek(offset);
                        file.read(data, 0, readBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                        //TODO: respond w/ meaningful error message
                    }
                    responseString = new String(data, StandardCharsets.UTF_8);
                    System.out.println("Server received read request: " + readRequestArgs);
                    break;
                case INSERT:
                    responseString = "ACK - INSERTED!";
                    break;
                case LISTEN:
                    responseString = "ACK - LISTENING!";
                    //TODO: cannot kill connection
                    break;
                case STOP:
                    running = false;
                    System.out.println("Server is stopping...");
                    responseString = "SERVER IS STOPPING!";
                    break;
                default:
                    System.out.println("Invalid request type: " + receivedRequestType);
                    break;
            }


            // response packet
            responseBuf = responseString.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, address, port);
            socket.send(responsePacket);
            System.out.println("Server responded with: " + responseString);
        }

        socket.close();
        System.out.println("Server has stopped.");
    }
}
