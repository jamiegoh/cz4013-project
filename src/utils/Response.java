package utils;

import java.net.DatagramPacket;
import java.util.HashMap;

public class Response {
  private final RequestType clientRequestType;
  private final int requestId;
  private String message;

  // Server Response Constructor
  public Response(RequestType clientRequestType, int requestId, String message) {
    this.clientRequestType = clientRequestType;
    this.requestId = requestId;
    this.message = message;
  }

  // Client Response Constructor
  public Response(DatagramPacket packet) {
    String serialStr = new String(packet.getData(), 0, packet.getLength());
    String[] serialStrSplit = serialStr.split(",");
    this.clientRequestType = RequestType.valueOf(serialStrSplit[0]);
    this.requestId = Integer.parseInt(serialStrSplit[1]);
    this.message = serialStrSplit[2];
  }

  public byte[] serialize() {
    return (clientRequestType.getType() + "," + requestId + "," + message).getBytes();
  }

  public HashMap<String, Object> deserialize() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("clientRequestType", clientRequestType);
    map.put("requestId", requestId);
    map.put("message", message);
    return map;
  }
  
}
