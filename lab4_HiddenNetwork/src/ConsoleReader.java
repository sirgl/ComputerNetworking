import java.io.IOException;
import java.util.Scanner;

public class ConsoleReader implements Runnable {

    private static final String CHARSET = "UTF-8";

    private final Node node;

    public ConsoleReader(Node node) {
        this.node = node;
    }

    @Override
    public void run() {
        try {
            Scanner scanner = new Scanner(System.in, CHARSET);
            while (true) {
                String str = scanner.nextLine();
                byte[] message = str.getBytes(CHARSET);
                if (message.length > 127) {
                    System.err.println("Too long message. Supported only message < 255 bytes");
                    continue;
                }
                node.sendToAll(Node.getSendReadyMessage(Node.TEXT_MESSAGE_TAG, message));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
