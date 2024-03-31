package server;

import utils.ReadRequest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
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
            Map<String, Object> received = new ReadRequest(requestPacket).deserialize();

            System.out.println("Server received data: " + received.get("pathname") + " " + received.get("offset") + " " + received.get("readBytes"));

            if (received.equals("STOP")) {
                running = false;
                System.out.println("Server is stopping...");
                continue;
            }

            // response packet
            responseBuf = "ACK".getBytes();
            DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, address, port);
            socket.send(responsePacket);
            System.out.println("Server sent ACK to client");
        }

        socket.close();
        System.out.println("Server has stopped.");

    }


}
