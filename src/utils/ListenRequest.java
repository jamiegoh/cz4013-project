package utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class ListenRequest extends Request{
    private String clientAddress;
    private String pathname;
    private int monitorInterval;


    public ListenRequest(String currentAddress, String pathname, int monitorInterval, int requestId) {
        super(RequestType.LISTEN, requestId);
        this.clientAddress = currentAddress;
        this.pathname = pathname;
        this.monitorInterval = monitorInterval;
    }

    public ListenRequest(DatagramPacket packet, int requestId) {
        super(RequestType.LISTEN, requestId);
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.clientAddress = parts[2];
        this.pathname = parts[3];
        this.monitorInterval = Integer.parseInt(parts[4]);
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + getRequestId() + "," + currentAddress + "," + pathname + "," + monitorInterval).getBytes();
    }

    public Map<String,Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        map.put("clientAddress", clientAddress);
        map.put("pathname", pathname);
        map.put("monitorInterval", monitorInterval);
        return map;
    }

}
