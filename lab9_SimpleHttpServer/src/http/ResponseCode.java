package http;

public enum ResponseCode {
    OK(200, "OK"),
    NOT_FOUND(404, "Not found"),
    METHOD_NOT_ALLOWED(405, "Method not allowed");

    private final int code;
    private final String reasonPhrase;

    ResponseCode(int code, String reasonPhrase) {
        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    @Override
    public String toString() {
        return reasonPhrase;
    }

    public int getCode() {
        return code;
    }
}
