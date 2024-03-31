package client;

import java.io.IOException;
import java.net.*;

public class Client {
    private DatagramSocket socket;
    private InetAddress address;
    private byte[] buf;
    private int port;

    public Client(int port) throws UnknownHostException, SocketException {
        this.port = port;
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");
    }

    public String sendData(String msg) throws IOException {
        System.out.println("Client is sending data: " + msg);
        buf = msg.getBytes();
        DatagramPacket packet
                = new DatagramPacket(buf, buf.length, address, port);
        System.out.println("Client is sending packet...");
        socket.send(packet);
        System.out.println("Client sent packet");
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        String received = new String(
                packet.getData(), 0, packet.getLength());
        return received;
    }

    public void close() {
        socket.close();
    }
}
