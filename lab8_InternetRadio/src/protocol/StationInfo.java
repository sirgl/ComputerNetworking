package protocol;

public class StationInfo {
    private final String name;
    private final String group;

    public StationInfo(String name, String group) {
        this.name = name;
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }
}
