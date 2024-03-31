package utils;

import java.net.DatagramPacket;

public class Request {
    private final RequestType requestType;

    public RequestType getRequestType() {
        return requestType;
    }

    public Request(RequestType requestType) {
        this.requestType = requestType;
    }

    public Request(DatagramPacket packet) {
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] serialStrSplit = serialStr.split(",");
        this.requestType = RequestType.valueOf(serialStrSplit[0]);
    }

}
