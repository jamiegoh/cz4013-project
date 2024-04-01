package utils;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;

public class ListenRequest {
    private String pathname;
    private int monitorInterval;

    public ListenRequest(String pathname, int monitorInterval) {
        this.pathname = pathname;
        this.monitorInterval = monitorInterval;
    }

    public ListenRequest(DatagramPacket packet) {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.pathname = parts[1];
        this.monitorInterval = Integer.parseInt(parts[2]);
    }

    public byte[] serialize() {
        return (RequestType.LISTEN.getType() + "," + pathname + "," + monitorInterval).getBytes();
    }

    public Map<String,Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", RequestType.LISTEN);
        map.put("pathname", pathname);
        map.put("monitorInterval", monitorInterval);
        return map;
    }

}
