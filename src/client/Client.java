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
        System.out.println("Client is sending data: " + msg);
        byte[] requestBuf = msg.getBytes(StandardCharsets.UTF_8);

        DatagramPacket requestPacket
                = new DatagramPacket(requestBuf, requestBuf.length, address, port);

        System.out.println("buf length" + requestBuf.length);

        System.out.println("Client is sending packet...");
        socket.send(requestPacket);
        System.out.println("Client sent packet");

        byte[] responseBuf = new byte[256];

        DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);
        socket.receive(responsePacket);
        String received = new String(
                responsePacket.getData(), 0, responsePacket.getLength());
        System.out.println("Length of response Packet" + responsePacket.getLength());

        return received;
    }

    public void close() {
        socket.close();
    }
}
