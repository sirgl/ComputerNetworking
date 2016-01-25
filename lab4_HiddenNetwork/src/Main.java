import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    private static final String NODE_THREAD_NAME = "Node";
    private static final String READER_THREAD_NAME = "Reader";
    private static final String USAGE = "Usage: java Node <parent ip> <port> or java Node for root";

    public static void main(String[] args) {
        boolean isRoot = false;
        if (args.length == 0) {
            isRoot = true;
        }

        if (!isRoot && args.length != 2) {
            throw new IllegalArgumentException(USAGE);
        }

        int port = 0;
        String parentIp = null;
        if (!isRoot) {
            port = Integer.parseInt(args[1]);
            parentIp = args[0];
        }

        try {
            Node node;
            if (!isRoot) {
                node = new Node(new InetSocketAddress(parentIp, port));
            } else {
                node = new Node();
            }
            new Thread(node, NODE_THREAD_NAME).start();
            ConsoleReader reader = new ConsoleReader(node);
            new Thread(reader, READER_THREAD_NAME).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
