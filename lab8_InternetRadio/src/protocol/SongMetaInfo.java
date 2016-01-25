package protocol;

public class SongMetaInfo {
    private final String songName;
    private final String performerName;

    public SongMetaInfo(String songName, String performerName) {
        this.songName = songName;
        this.performerName = performerName;
    }

    public String getSongName() {
        return songName;
    }

    public String getPerformerName() {
        return performerName;
    }
}
