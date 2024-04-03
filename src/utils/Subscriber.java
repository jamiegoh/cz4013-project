package utils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Subscriber {
    public InetAddress clientAddress;
    public int clientPort;
    private String pathname;

    private int monitorInterval;
    private DatagramSocket socket;

    public Subscriber(InetAddress clientAddress, int clientPort, String pathname, int monitorInterval) throws SocketException {
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.pathname = pathname;
        this.monitorInterval = monitorInterval; // in minutes
        socket = new DatagramSocket();
    }

    public Subscriber(DatagramPacket packet) throws UnknownHostException, SocketException {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.clientAddress = InetAddress.getByName(parts[1]);
        this.clientPort = Integer.parseInt(parts[2]);
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
        System.out.println("Subscriber has been added for" + subscriber.getPathname() + " with interval " + subscriber.getMonitorInterval() + " minutes from" + subscriber.clientAddress + ":" + subscriber.clientPort);
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
        }, subscriber.getMonitorInterval() * 60 * 1000); // Convert monitor interval to ms
    }

    public static void removeSubscriber(Subscriber subscriber) throws IOException {
        System.out.println("Subscriber has been removed for" + subscriber.getPathname() + " with interval " + subscriber.getMonitorInterval() + " minutes from" + subscriber.clientAddress + ":" + subscriber.clientPort);
        String key = currentDir + "/src/data/" + subscriber.getPathname();
        if (subscribersMap.containsKey(key)) {
            subscribersMap.get(key).remove(subscriber);
            if (subscribersMap.get(key).isEmpty()) {
                subscribersMap.remove(key);
            }
        }
        String message = "INTERVAL ENDED";
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, subscriber.clientAddress, subscriber.clientPort);
        subscriber.socket.send(packet);

    }

//    public static void notifySubscribers(String pathname) {
//        System.out.println("subscriber for " + pathname + " has been notified");
//        String key = currentDir + "/src/data/" + pathname;
//        if (subscribersMap.containsKey(key)) {
//            try {
//                for (Subscriber subscriber : subscribersMap.get(key)) {
//                    // todo: put 0 as request id for now, dont think we should use ReadRequst here
//
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public InetAddress getClientAddress() {
        return clientAddress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public static List<Subscriber> getSubscribers(String pathname) {
        String key = pathname;
        System.out.println("Subscriber map currently contains: " + subscribersMap);
        System.out.println("In get subscribers, key is " + key);
        if (subscribersMap.containsKey(key)) {
            return subscribersMap.get(key);
        }
        return null;
    }


}
