package utils;

import java.net.DatagramPacket;

public class Request {
    private final RequestType requestType;
    private int requestId;

    public RequestType getRequestType() {
        return requestType;
    }

    public int getRequestId() {
        return requestId;
    }

    // Client Request Constructor
    public Request(RequestType requestType, int requestId) {
        this.requestType = requestType;
        this.requestId = requestId;
    }

    // Server Request Constructor
    public Request(DatagramPacket packet) {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] serialStrSplit = serialStr.split(",");
        this.requestType = RequestType.valueOf(serialStrSplit[0]);
        this.requestId = Integer.parseInt(serialStrSplit[1]);
    }

}
