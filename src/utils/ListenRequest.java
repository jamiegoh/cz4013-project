package utils;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;

public class ListenRequest extends Request{
    private String pathname;
    private int monitorInterval;


    public ListenRequest( String pathname, int monitorInterval, int requestId) {
        super(RequestType.LISTEN, requestId);
        this.pathname = pathname;
        this.monitorInterval = monitorInterval;
    }

    public ListenRequest(DatagramPacket packet, int requestId) {
        super(RequestType.LISTEN, requestId);
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.pathname = parts[2];
        this.monitorInterval = Integer.parseInt(parts[3]);
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + getRequestId() + "," + pathname + "," + monitorInterval).getBytes();
    }

    public HashMap<String,Object> deserialize() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        map.put("pathname", pathname);
        map.put("monitorInterval", monitorInterval);
        return map;
    }

}
