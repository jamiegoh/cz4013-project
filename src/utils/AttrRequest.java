package utils;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class AttrRequest extends Request {
  private String pathname;

  // Constructor for client
  public AttrRequest(String pathname, int requestId) {
    super(RequestType.ATTR, requestId);
    this.pathname = pathname;
  }

  // Constructor for server
  public AttrRequest(DatagramPacket packet, int requestId) {
    super(RequestType.ATTR, requestId);
    String serialStr = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    String[] parts = serialStr.split(",");
    this.pathname = parts[2];
  }

  public byte[] serialize() {
    return (getRequestType().getType() + "," + getRequestId() + "," + pathname).getBytes(StandardCharsets.UTF_8);
  }

  public HashMap<String, Object> deserialize() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("requestType", getRequestType());
    map.put("requestId", getRequestId());
    map.put("pathname", pathname);
    return map;
  }
  
}
