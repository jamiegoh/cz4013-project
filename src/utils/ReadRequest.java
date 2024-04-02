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
    private int requestId;

    // Constructor for client 
    public ReadRequest(String pathname, int offset, int readBytes, ReadType type, int requestId) {
        super(RequestType.READ);
        this.pathname = pathname;
        this.offset = offset;
        this.readBytes = readBytes;
        this.type = type; //subscriber update read or regular read
        this.requestId = requestId;
    }

    // Constructor for server
    public ReadRequest(DatagramPacket packet) {
        super(RequestType.READ);
        String serialStr = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        System.out.println("ReadRequest: " + serialStr);
        String[] parts = serialStr.split(",");
        this.requestId = Integer.parseInt(parts[1]);
        this.pathname = parts[2];
        this.offset = Integer.parseInt(parts[3]);
        this.readBytes = Integer.parseInt(parts[4]);
        this.type = ReadType.valueOf(parts[5].toUpperCase());
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + requestId + "," + pathname + "," + offset + "," + readBytes + "," + type).getBytes(StandardCharsets.UTF_8);
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", requestId);
        map.put("pathname", pathname);
        map.put("offset", offset);
        map.put("readBytes", readBytes);
        map.put("type", type);
        return map;
    }
}
