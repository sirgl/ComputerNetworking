import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private final ServerSocket serverSocket;

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }


    public static void main(String[] args) throws IOException {
        new Server(4000).run();
    }

    @Override
    public void run() {
        ExecutorService threadPool = Executors.newFixedThreadPool(4);
        try {
            Socket socket;
            while(true) {
                socket = serverSocket.accept();
                threadPool.execute(new Task(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Task implements Runnable {
        private final Socket incomingSocket;

        private Task(Socket incomingSocket) {
            this.incomingSocket = incomingSocket;
        }

        @Override
        public void run() {
            try {
                InputStream inputStream = incomingSocket.getInputStream();
                DataInputStream objectInputStream = new DataInputStream(inputStream);
                byte[] buffer = new byte[1024];
                String name = objectInputStream.readUTF();
                FileOutputStream outputStream = new FileOutputStream("downloaded/" + name);
                int read;
                while(-1 != (read = inputStream.read(buffer))) {
                    outputStream.write(buffer, 0, read);
                }
                incomingSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
