package client;

import utils.*;

import java.io.IOException;
import java.net.*;

public class Client {
    // timeout
    private static final int TIMEOUT = 1000;

    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    private InvocationSemantics invSemantics;
    

    // Request id 
    private int requestId = 1;
    

    // Constructor
    public Client(String serverAddress, int port, InvocationSemantics invocationSemantics) throws UnknownHostException, SocketException {
//        address = InetAddress.getByName("localhost");
        this.address = InetAddress.getByName(serverAddress);
        this.port = port;
        this.invSemantics = invocationSemantics;
        socket = new DatagramSocket();
    }

    public String makeRequest(RequestType requestType, String input) throws IOException {


        String[] parts = input.split(",");
        String pathname = requestType == RequestType.STOP ? null : parts[0];

        DatagramPacket requestPacket;

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
                socket.send(requestPacket);
                boolean running = true;
                while (running) {
                    byte[] responseBuf = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);
                    socket.receive(responsePacket);
                    String received = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    System.out.println("Client received data: " + received);
                    if (received.equals("INTERVAL ENDED")) {
//                        running = false;
                        return received;
                    }
                }
                break;
            case STOP:
                byte[] stopRequestBuf = new StopRequest(requestId).serialize();
                requestPacket = new DatagramPacket(stopRequestBuf, stopRequestBuf.length, address, port);
                break;
            default:
                throw new IllegalArgumentException("Invalid request type: " + requestType);
        }


        socket.send(requestPacket);


        byte[] responseBuf = new byte[1024];
        DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);
        socket.receive(responsePacket);

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

        // Increment request id
        requestId++;

        return received;
    }

    public void close() {
        socket.close();
    }
}
