package utils;

import java.util.HashMap;
import java.util.Map;

public class StopRequest extends Request {
    public StopRequest(int requestId) {
        super(RequestType.STOP, requestId);
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + getRequestId()).getBytes();
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        return map;
    }

}
