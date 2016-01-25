import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client implements Runnable {
    private final Path file;
    private final String serverAddress;
    private final int port;

    public Client(String serverAddress, int port, String path) throws IOException {
        this.serverAddress = serverAddress;
        this.port = port;
        this.file = Paths.get(path);


    }

    public static void main(String[] args) throws IOException {
        new Client("10.9.84.165", 5000, "UploadingFiles/pg11.txt").run();
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(serverAddress, port) ;
            OutputStream outputStream = socket.getOutputStream()){
            DataOutputStream objectOutputStream = new DataOutputStream(outputStream);
//            FileHeader fileHeader = new FileHeader();
//            fileHeader.setFileName(file.getFileName().toString());
//            fileHeader.setLength(Files.size(file));
//            objectOutputStream.writeObject(fileHeader);

            objectOutputStream.writeUTF(file.getFileName().toString());
            objectOutputStream.flush();
            FileInputStream fileInputStream = new FileInputStream(file.toFile());
            byte[] buffer = new byte[1024];
            int read;
            while (-1 != (read = fileInputStream.read(buffer))) {
                objectOutputStream.write(buffer, 0, read);
                objectOutputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
