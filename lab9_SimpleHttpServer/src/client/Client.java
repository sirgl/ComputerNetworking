package client;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;

public class Client implements Runnable {
    private final int port;
    private final String host;
    private final URI fileUri;
    private final Path resourceFolder = Paths.get("res", "Client");

    private byte[] buffer = new byte[16384];

    public Client(int port, String host, String filePath) throws IOException, URISyntaxException {
        this.port = port;
        this.host = host;
        fileUri = new URI(filePath);
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        if(args.length < 3){
            throw new IllegalArgumentException("Usage: Client <port> <host> <file>");
        }
        new Thread(new Client(Integer.parseInt(args[0]), args[1], args[2])).start();
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port)) {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            String reqLine = "GET " + fileUri + " HTTP/1.0" + "\n\n";
            outputStream.write(reqLine.getBytes());
            String responseLine = readLine(inputStream);
            if (responseLine == null) {
                System.err.println("Message is not consistent with the protocol");
                return;
            }
            Scanner scanner = new Scanner(responseLine);
            scanner.useDelimiter(" ");
            scanner.next();
            String code = scanner.next();
            switch (code) {
                case "200":
                    String line = readLine(inputStream);
                    if(line == null || !line.equals("")) {
                        System.err.println("Message is not consistent with the protocol");
                        return;
                    }
                    int read;
                    Path endPointPath = resourceFolder.resolve(Paths.get(fileUri.getPath()));
                    OutputStream fileOutputStream =
                            Files.newOutputStream(endPointPath, StandardOpenOption.CREATE_NEW);
                    while((read = inputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, read);
                    }
                    return;
                default:
                    scanner.useDelimiter("\n");
                    String reasonPhrase = scanner.next();
                    System.err.println(code + " " + reasonPhrase);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String readLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int c;
        for (c = inputStream.read(); c != '\n' && c != -1; c = inputStream.read()) {
            byteArrayOutputStream.write(c);
        }
        if (c == -1 && byteArrayOutputStream.size() == 0) {
            return null;
        }
        return byteArrayOutputStream.toString();
    }
}
