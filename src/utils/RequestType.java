package utils;

public enum RequestType {
    READ("READ"), INSERT("INSERT"), LISTEN("LISTEN"), STOP("STOP");

    private final String type;

    RequestType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
