package utils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Subscriber {
    private InetAddress address;
    private int port;
    private String pathname;

    private int monitorInterval;
    private DatagramSocket socket;

    public Subscriber(InetAddress address, int port, String pathname, int monitorInterval) throws SocketException {
        this.address = address;
        this.port = port;
        this.pathname = pathname;
        this.monitorInterval = monitorInterval; // in minutes
        socket = new DatagramSocket();
    }

    public Subscriber(DatagramPacket packet) throws UnknownHostException, SocketException {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.address = InetAddress.getByName(parts[1]);
        this.port = Integer.parseInt(parts[2]);
        this.pathname = parts[3];
        this.monitorInterval = Integer.parseInt(parts[4]);
        socket = new DatagramSocket();

    }

    public String getPathname() {
        return pathname;
    }

    public int getMonitorInterval() {
        return monitorInterval;
    }

    private static HashMap<String, List<Subscriber>> subscribersMap = new HashMap<>();
    private static Timer timer = new Timer();
    static String currentDir = Paths.get("").toAbsolutePath().toString();

    public static void addSubscriber(Subscriber subscriber) {
        System.out.println("Subscriber has been added for" + subscriber.getPathname() + " with interval " + subscriber.getMonitorInterval() + " minutes from" + subscriber.address + ":" + subscriber.port);
        String key = currentDir + "/src/data/" + subscriber.getPathname();
        if (subscribersMap.containsKey(key)) {
            subscribersMap.get(key).add(subscriber);
        } else {
            List<Subscriber> newList = new ArrayList<>();
            newList.add(subscriber);
            subscribersMap.put(key, newList);
        }

        System.out.println("Subscribers Map: " + subscribersMap);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    removeSubscriber(subscriber);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, subscriber.getMonitorInterval() * 60 *  1000); // Convert monitor interval to ms
    }

    public static void removeSubscriber(Subscriber subscriber) throws IOException {
        System.out.println("Subscriber has been removed for" + subscriber.getPathname() + " with interval " + subscriber.getMonitorInterval() + " minutes from" + subscriber.address + ":" + subscriber.port);
        String key = subscriber.getPathname();
        if (subscribersMap.containsKey(key)) {
            subscribersMap.get(key).remove(subscriber);
            if (subscribersMap.get(key).isEmpty()) {
                subscribersMap.remove(key);
            }
        }
        String message = "INTERVAL ENDED";
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, subscriber.address, subscriber.port);
        subscriber.socket.send(packet);

    }

    public static void notifySubscribers(String pathname) {
        System.out.println("subscriber for " + pathname + " has been notified");
        if (subscribersMap.containsKey(pathname)) {
            try {
                for (Subscriber subscriber : subscribersMap.get(pathname)) {
                    ReadRequest readRequest = new ReadRequest(pathname, 0, (int) Files.size(Paths.get(pathname)));
                    byte[] requestBytes = readRequest.serialize();

                    DatagramPacket packet = new DatagramPacket(requestBytes, requestBytes.length, subscriber.address, subscriber.port);
                    subscriber.socket.send(packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
