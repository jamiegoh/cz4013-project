package client;

import utils.*;

import java.io.IOException;
import java.net.*;

public class Client {
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    public Client(String serverAddress, int port) throws UnknownHostException, SocketException {
//        address = InetAddress.getByName("localhost");
        this.address = InetAddress.getByName(serverAddress);
        this.port = port;
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
                byte[] readRequestBuf = new ReadRequest(pathname, readOffset, readBytes).serialize();
                requestPacket = new DatagramPacket(readRequestBuf, readRequestBuf.length, address, port);
                break;
            case INSERT:
                int writeOffset = Integer.parseInt(parts[1]);
                String data = parts[2];
                byte[] insertRequestBuf = new InsertRequest(pathname, writeOffset, data).serialize();
                requestPacket = new DatagramPacket(insertRequestBuf, insertRequestBuf.length, address, port);
                break;
            case LISTEN:
                int monitorInterval = Integer.parseInt(parts[1]);
                byte[] listenRequestBuf = new ListenRequest(pathname, monitorInterval).serialize();
                requestPacket = new DatagramPacket(listenRequestBuf, listenRequestBuf.length, address, port);
                socket.send(requestPacket);
                boolean running = true;
                while (running) {
                    byte[] responseBuf = new byte[256];
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
                byte[] stopRequestBuf = new StopRequest().serialize();
                requestPacket = new DatagramPacket(stopRequestBuf, stopRequestBuf.length, address, port);
                break;
            default:
                throw new IllegalArgumentException("Invalid request type: " + requestType);
        }


        socket.send(requestPacket);

        byte[] responseBuf = new byte[256];
        DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);
        socket.receive(responsePacket);

        String received = new String(responsePacket.getData(), 0, responsePacket.getLength());

        System.out.println("Client received data: " + received);

//        if (received.equals("ACK")) {
//            System.out.println("Client received ACK :)");
//        } else {
//            System.out.println("Client did not receive ACK :(");
//            // todo: resend based on resend policy (lecturer calls it at-most-once/at-least-once)
//        }

        return received;
    }

    public void close() {
        socket.close();
    }
}
