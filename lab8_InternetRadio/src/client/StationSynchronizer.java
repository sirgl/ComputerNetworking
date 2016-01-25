package client;

public class StationSynchronizer {
    private String group;
    private final Object lock = new Object();

    public StationSynchronizer(String group) {
        this.group = group;
    }

    public void setGroup(String group) {
        synchronized (lock) {
            if(this.group == null) {
                lock.notify();
            }
            this.group = group;
        }
    }

    public String waitForGroup() throws InterruptedException {
        synchronized (lock) {
            while (group == null) {
                lock.wait();
            }
            return group;
        }
    }
}
