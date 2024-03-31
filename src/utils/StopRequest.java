package utils;

import java.util.HashMap;
import java.util.Map;

public class StopRequest extends Request {
    public StopRequest() {
        super(RequestType.STOP);
    }

    public byte[] serialize() {
        return getRequestType().getType().getBytes();
    }

    public Map<String, Object> deserialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        return map;
    }

}
