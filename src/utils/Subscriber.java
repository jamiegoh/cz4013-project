package utils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Subscriber {
    public static InetAddress address;
    public static int port;
    private String pathname;

    private int monitorInterval;
    private DatagramSocket socket;
    private InetAddress serverAddress;
    private int serverPort;

    public Subscriber(InetAddress address, int port, String pathname, int monitorInterval, InetAddress serverAddress, int serverPort) throws SocketException {
        this.address = address;
        this.port = port;
        this.pathname = pathname;
        this.monitorInterval = monitorInterval; // in minutes
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        socket = new DatagramSocket();
    }

    public Subscriber(DatagramPacket packet) throws UnknownHostException, SocketException {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.address = InetAddress.getByName(parts[1]);
        this.port = Integer.parseInt(parts[2]);
        this.pathname = parts[3];
        this.monitorInterval = Integer.parseInt(parts[4]);
        this.serverAddress = InetAddress.getByName(parts[5]);
        this.serverPort = Integer.parseInt(parts[6]);
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
        String key = currentDir + "/src/data/" + subscriber.getPathname();
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
        String key = currentDir + "/src/data/" + pathname;
        if (subscribersMap.containsKey(key)) {
            try {
                for (Subscriber subscriber : subscribersMap.get(key)) {
                    // todo: put 0 as request id for now, dont think we should use ReadRequst here
                    ReadRequest readRequest = new ReadRequest(pathname, 0, (int) Files.size(Paths.get(key)), ReadType.SUBSCRIBER, 0); 
                    byte[] requestBytes = readRequest.serialize();

                    DatagramPacket packet = new DatagramPacket(requestBytes, requestBytes.length, subscriber.serverAddress, subscriber.serverPort);
                    subscriber.socket.send(packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    

}
