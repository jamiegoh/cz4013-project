package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Server {
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];

    private int port;

    public Server(int port) throws SocketException {
        this.port = port;
        socket = new DatagramSocket(this.port);
    }

    public void run() throws IOException {
        running = true;
        System.out.println("Server is running on port " + port);

        while (running) {
            System.out.println("Server is waiting for packet...");
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            System.out.println("Received packet" + packet);

            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            packet = new DatagramPacket(buf, buf.length, address, port);
            String received = new String(packet.getData(), 0, packet.getLength());

            if (received.equals("end")) {
                running = false;
                continue;
            }

            socket.send(packet);
        }

        socket.close();
        System.out.println("Server stopped");

    }


}
