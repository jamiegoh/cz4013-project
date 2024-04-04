package utils;

import java.net.DatagramPacket;
import java.util.HashMap;

public class CreateRequest extends Request{

    private String pathname;

    // Constructor for client
    public CreateRequest(String pathname, int requestId) {
        super(RequestType.CREATE, requestId);
        this.pathname = pathname;
    }

    // Constructor for server
    public CreateRequest(DatagramPacket packet, int requestId) {
        super(RequestType.CREATE, requestId);
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.pathname = parts[2];
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + getRequestId() + "," + pathname).getBytes();
    }

    public HashMap<String, Object> deserialize() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        map.put("pathname", pathname);
        return map;
    }

}
