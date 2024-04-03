package utils;

import java.net.DatagramPacket;
import java.util.HashMap;
import java.util.Map;

public class CreateRequest extends Request{

    private String pathname;

    public CreateRequest(String pathname, int requestId) {
        super(RequestType.CREATE, requestId);
        this.pathname = pathname;
    }

    public CreateRequest(DatagramPacket packet, int requestId) {
        super(RequestType.CREATE, requestId);
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.pathname = parts[2];
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + getRequestId() + "," + pathname).getBytes();
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        map.put("pathname", pathname);
        return map;
    }

}
