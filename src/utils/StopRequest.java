package utils;

import java.util.HashMap;
import java.util.Map;

public class StopRequest extends Request {
    private int requestId;

    public StopRequest(int requestId) {
        super(RequestType.STOP);
        this.requestId = requestId;
    }

    public byte[] serialize() {
        return (getRequestType().getType() + "," + requestId).getBytes();
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", requestId);
        return map;
    }

}
