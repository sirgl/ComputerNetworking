import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Proxy implements Runnable {
    private final ServerSocket serverSocket;
    private final ExecutorService executorService = Executors.newFixedThreadPool(64);

    public Proxy(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: proxy <port>");
        }
        Proxy proxy = new Proxy(Integer.parseInt(args[0]));
        new Thread(proxy).start();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                executorService.execute(new Connection(socket, executorService));
            }
        } catch (IOException e) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
