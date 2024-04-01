package utils;

public enum ReadType {
    NORMAL("NORMAL"), SUBSCRIBER("SUBSCRIBER");

    private final String type;

    ReadType(String type) {
        this.type = type;
    }
}