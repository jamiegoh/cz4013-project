package utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class ListenRequest {

    private InetAddress serverAddress;
    private String pathname;
    private int monitorInterval;

    public ListenRequest(InetAddress serverAddress, String pathname, int monitorInterval) {
        this.serverAddress = serverAddress;
        this.pathname = pathname;
        this.monitorInterval = monitorInterval;
    }

    public ListenRequest(DatagramPacket packet) {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.serverAddress = packet.getAddress();
        this.pathname = parts[2];
        this.monitorInterval = Integer.parseInt(parts[3]);
    }

    public byte[] serialize() {
        return (RequestType.LISTEN.getType() + "," + serverAddress + "," + pathname + "," + monitorInterval).getBytes();
    }

    public Map<String,Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", RequestType.LISTEN);
        map.put("serverAddress", serverAddress);
        map.put("pathname", pathname);
        map.put("monitorInterval", monitorInterval);
        return map;
    }

}
