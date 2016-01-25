package protocol;

public class Protocol {
    //Client
    public static final byte LOGIN_COMMAND = 1;
    public static final byte MESSAGE_COMMAND = 2;
    public static final byte LOGOUT_COMMAND = 3;

    //Server
    public static final byte LOGIN_SUCCESS_ANSWER = 4;
    public static final byte LOGIN_FAILURE_ANSWER = 5;
    public static final byte USER_LOGIN_EVENT = 6;
    public static final byte USER_LOGOUT_EVENT = 7;
    public static final byte MESSAGE_EVENT = 8;
}
