package server;

import http.ResponseCode;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server implements Runnable {
    private static final String PROTOCOL = "HTTP/1.0";
    private final static Path RESOURCES_PATH = Paths.get("res", "Server");

    private final ServerSocket serverSocket;
    private final byte[] buffer = new byte[4096];

    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: Server <port>");
        }
        new Thread(new Server(Integer.parseInt(args[0]))).start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                try (Socket socket = serverSocket.accept()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    OutputStream outputStream = socket.getOutputStream();

                    String[] tokens = reader.readLine().split(" ");
                    if (!tokens[0].equals("GET")) {
                        outputStream.write(getResponseLine(ResponseCode.METHOD_NOT_ALLOWED).getBytes());
                        outputStream.write("405 Method not allowed".getBytes());
                        continue;
                    }

                    Path path = Paths.get(tokens[1].substring(1));
                    Path resource = RESOURCES_PATH.resolve(path);
                    if (Files.isDirectory(resource)) {
                        resource = resource.resolve("index.html");
                    }
                    try {
                        InputStream fileInputStream = Files.newInputStream(resource);
                        while (true) { // skipping headers
                            if (reader.readLine().equals("")) {
                                break;
                            }
                        }

                        outputStream.write(getResponseLine(ResponseCode.OK).getBytes());
                        int read;
                        while ((read = fileInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, read);
                        }
                    } catch (FileNotFoundException e) {
                        outputStream.write(getResponseLine(ResponseCode.NOT_FOUND).getBytes());
                        outputStream.write("404 Not found".getBytes());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getResponseLine(ResponseCode code) {
        return PROTOCOL + " " + code.getCode() + " " + code + "\n\n";
    }
}
