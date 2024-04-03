package utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class ListenRequest extends Request{
    private InetAddress serverAddress;
    private String pathname;
    private int monitorInterval;


    public ListenRequest(InetAddress serverAddress, String pathname, int monitorInterval, int requestId) {
        super(RequestType.LISTEN, requestId);
        this.serverAddress = serverAddress;
        this.pathname = pathname;
        this.monitorInterval = monitorInterval;
    }

    public ListenRequest(DatagramPacket packet, int requestId) {
        super(RequestType.LISTEN, requestId);
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.serverAddress = packet.getAddress();
        this.pathname = parts[3];
        this.monitorInterval = Integer.parseInt(parts[4]);
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + getRequestId() + "," + serverAddress + "," + pathname + "," + monitorInterval).getBytes();
    }

    public Map<String,Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        map.put("serverAddress", serverAddress);
        map.put("pathname", pathname);
        map.put("monitorInterval", monitorInterval);
        return map;
    }

}
