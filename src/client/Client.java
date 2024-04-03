package client;

import utils.*;

import java.io.IOException;
import java.net.*;

public class Client {
    // timeout
    private static final int TIMEOUT = 1000;
    // Max number of retries
    private static final int MAX_RETRIES = 3;

    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    private InvocationSemantics invocationSemantics;
    

    // Request id 
    private int requestId = 1;
    

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


        // send request
        socket.send(processRequest(requestType, parts, pathname));

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

        String received = new String(responsePacket.getData(), 0, responsePacket.getLength());

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

    public void close() {
        socket.close();
    }
}
