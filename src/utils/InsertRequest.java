package utils;

import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;

public class InsertRequest {
    private String pathname;
    private int offset;
    private String data;

    public InsertRequest(String pathname, int offset, String data) {
        this.pathname = pathname;
        this.offset = offset;
        this.data = data;
    }

    public InsertRequest(DatagramPacket packet) {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.pathname = parts[1];
        this.offset = Integer.parseInt(parts[2]);
        this.data = parts[3];
    }

    public byte[] serialize() {
        return (RequestType.INSERT.getType() + "," + pathname + "," + offset + "," + data).getBytes();
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", RequestType.INSERT);
        map.put("pathname", pathname);
        map.put("offset", offset);
        map.put("data", data);
        return map;
    }
}
