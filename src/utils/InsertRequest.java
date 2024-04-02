package utils;

import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;

public class InsertRequest {
    private String pathname;
    private int offset;
    private String data;
    private int requestId;

    public InsertRequest(String pathname, int offset, String data, int requestId) {
        this.pathname = pathname;
        this.offset = offset;
        this.data = data;
        this.requestId = requestId;
    }

    public InsertRequest(DatagramPacket packet) {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.requestId = Integer.parseInt(parts[1]);
        this.pathname = parts[2];
        this.offset = Integer.parseInt(parts[3]);
        this.data = parts[4];
    }

    public byte[] serialize() {
        return (RequestType.INSERT.getType() + "," + requestId + "," + pathname + "," + offset + "," + data).getBytes();
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", RequestType.INSERT);
        map.put("requestId", requestId);
        map.put("pathname", pathname);
        map.put("offset", offset);
        map.put("data", data);
        return map;
    }
}
