package client;

import java.awt.desktop.SystemSleepEvent;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;

public class Client {
    private DatagramSocket socket;
    private InetAddress address;
    private int port;

    public Client(int port) throws UnknownHostException, SocketException {
        this.port = port;
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");
    }

    public String sendData(String msg) throws IOException {
        System.out.println("Client sending data: " + msg);
        byte[] requestBuf = msg.getBytes(StandardCharsets.UTF_8);

        DatagramPacket requestPacket = new DatagramPacket(requestBuf, requestBuf.length, address, port);

        socket.send(requestPacket);

        byte[] responseBuf = new byte[256];

        DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);
        socket.receive(responsePacket);
        String received = new String(
                responsePacket.getData(), 0, responsePacket.getLength());

//        System.out.println("Client received data: " + received);

        if (received.equals("ACK")) {
            System.out.println("Client received ACK :)");
        } else {
            System.out.println("Client did not receive ACK :(");
            // todo: resend based on resend policy (lecturer calls it at-most-once/at-least-once)
        }

        return received;
    }

    public void close() {
        socket.close();
    }
}
