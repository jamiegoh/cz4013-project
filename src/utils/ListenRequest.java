package utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class ListenRequest {

    private InetAddress serverAddress;
    private String pathname;
    private int monitorInterval;
    private int requestId;


    public ListenRequest(InetAddress serverAddress, String pathname, int monitorInterval, int requestId) {
        this.serverAddress = serverAddress;
        this.pathname = pathname;
        this.monitorInterval = monitorInterval;
        this.requestId = requestId;
    }

    public ListenRequest(DatagramPacket packet) {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.requestId = Integer.parseInt(parts[1]);
        this.serverAddress = packet.getAddress();
        this.pathname = parts[3];
        this.monitorInterval = Integer.parseInt(parts[4]);
    }

    public byte[] serialize() {
        return (RequestType.LISTEN.getType() + "," + requestId + "," + serverAddress + "," + pathname + "," + monitorInterval).getBytes();
    }

    public Map<String,Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", RequestType.LISTEN);
        map.put("requestId", requestId);
        map.put("serverAddress", serverAddress);
        map.put("pathname", pathname);
        map.put("monitorInterval", monitorInterval);
        return map;
    }

}
