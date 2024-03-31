package client;

import utils.ReadRequest;
import utils.RequestType;
import utils.StopRequest;

import java.io.IOException;
import java.net.*;

public class Client {
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    public Client(int port) throws UnknownHostException, SocketException {
        this.port = port;
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");
    }

    public String makeRequest(RequestType requestType, String input) throws IOException {


        String[] parts = input.split(",");
        String pathname = requestType == RequestType.STOP ? null : parts[0];
        int offset = requestType == RequestType.STOP ? 0 : Integer.parseInt(parts[1]);
        int readBytes = requestType == RequestType.STOP ? 0 : Integer.parseInt(parts[2]);

        System.out.println("Client requesting: " + requestType + " " + pathname + " " + offset + " " + readBytes);
        DatagramPacket requestPacket;

        switch (requestType) {
            case READ:
                byte[] readRequestBuf = new ReadRequest(pathname, offset, readBytes).serialize();
                requestPacket = new DatagramPacket(readRequestBuf, readRequestBuf.length, address, port);
                break;
//            case INSERT:
//                //TODO: implement
//                break;
//            case LISTEN:
//                //TODO: implement
//                break;
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

        String received = new String(
                responsePacket.getData(), 0, responsePacket.getLength());

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
