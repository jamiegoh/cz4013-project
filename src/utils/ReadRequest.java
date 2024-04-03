package utils;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ReadRequest extends Request {
    private String pathname;
    private int offset;
    private int readBytes;
    private ReadType type;

    // Constructor for client 
    public ReadRequest(String pathname, int offset, int readBytes, ReadType type, int requestId) {
        super(RequestType.READ, requestId);
        this.pathname = pathname;
        this.offset = offset;
        this.readBytes = readBytes;
        this.type = type; //subscriber update read or regular read
    }

    // Constructor for server
    public ReadRequest(DatagramPacket packet, int requestId) {
        super(RequestType.READ, requestId);
        String serialStr = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        System.out.println("ReadRequest: " + serialStr);
        String[] parts = serialStr.split(",");
        this.pathname = parts[2];
        this.offset = Integer.parseInt(parts[3]);
        this.readBytes = Integer.parseInt(parts[4]);
        this.type = ReadType.valueOf(parts[5].toUpperCase());
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + getRequestId() + "," + pathname + "," + offset + "," + readBytes + "," + type).getBytes(StandardCharsets.UTF_8);
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        map.put("pathname", pathname);
        map.put("offset", offset);
        map.put("readBytes", readBytes);
        map.put("type", type);
        return map;
    }
}
