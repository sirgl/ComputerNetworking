package protocol;

public class Protocol {
    //client
    public static final byte STATIONS_LIST_REQUEST = 0;
    public static final byte SONG_META_REQUEST = 1;

    //server
    public static final byte STATIONS_LIST_RESPONSE = 2;
    public static final byte SONG_META_RESPONSE = 3;
}
