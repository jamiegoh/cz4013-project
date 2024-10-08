package utils;

import javax.xml.crypto.Data;
import java.net.DatagramPacket;
import java.util.HashMap;

public class SearchRequest extends Request{

    private String searchQuery;

    // Constructor for client
    public SearchRequest(String searchQuery,  int requestId){
        super(RequestType.SEARCH, requestId);
        this.searchQuery = searchQuery;
    }

    // Constructor for server
    public SearchRequest (DatagramPacket packet, int requestId){
        super(RequestType.SEARCH, requestId);
        String serialStr = new String(packet.getData(), 0, packet.getLength());
        String[] parts = serialStr.split(",");
        this.searchQuery = parts[2];
    }

    public String getSearchQuery(){
        return searchQuery;
    }

    public byte[] serialize(){
        return (getRequestType().getType() + "," + getRequestId() + "," + searchQuery).getBytes();
    }

    public HashMap<String, Object> deserialize(){
        HashMap<String, Object> map = new HashMap<>();
        map.put("requestType", getRequestType());
        map.put("requestId", getRequestId());
        map.put("searchQuery", searchQuery);
        return map;
    }
}
