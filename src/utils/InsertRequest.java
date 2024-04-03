package utils;

import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;

public class InsertRequest extends Request{
    private String pathname;
    private int offset;
    private String data;

    public InsertRequest(String pathname, int offset, String data, int requestId) {
        super(RequestType.INSERT, requestId);
        this.pathname = pathname;
        this.offset = offset;
        this.data = data;
    }

    public InsertRequest(DatagramPacket packet, int requestId) {
        super(RequestType.INSERT, requestId);
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.pathname = parts[2];
        this.offset = Integer.parseInt(parts[3]);
        this.data = parts[4];
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + getRequestId() + "," + pathname + "," + offset + "," + data).getBytes();
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        map.put("pathname", pathname);
        map.put("offset", offset);
        map.put("data", data);
        return map;
    }
}
