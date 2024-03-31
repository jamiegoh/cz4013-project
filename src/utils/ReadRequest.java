package utils;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ReadRequest extends Request {
    private String pathname;
    private int offset;
    private int readBytes;

    public ReadRequest(String pathname, int offset, int readBytes) {
        super(RequestType.READ);
        this.pathname = pathname;
        this.offset = offset;
        this.readBytes = readBytes;
    }

    public ReadRequest(DatagramPacket packet) {
        super(RequestType.READ);
        String serialStr = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        System.out.println("ReadRequest: " + serialStr);
        String[] parts = serialStr.split(",");
        this.pathname = parts[1];
        this.offset = Integer.parseInt(parts[2]);
        this.readBytes = Integer.parseInt(parts[3]);
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + pathname + "," + offset + "," + readBytes).getBytes(StandardCharsets.UTF_8);
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("pathname", pathname);
        map.put("offset", offset);
        map.put("readBytes", readBytes);
        return map;
    }
}
