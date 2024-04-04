package utils;

import java.util.HashMap;

public class StopRequest extends Request {

    // Constructor for client
    public StopRequest(int requestId) {
        super(RequestType.STOP, requestId);
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + getRequestId()).getBytes();
    }

    public HashMap<String, Object> deserialize() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        return map;
    }

}
