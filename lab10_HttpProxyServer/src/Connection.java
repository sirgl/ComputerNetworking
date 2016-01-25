import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class Connection implements Runnable {
    private static final String GET_METHOD_TAG = "GET";
    private static final int BUFFER_SIZE = 16384;
    private final static String errorCodeTemplate =
            "<html>\n" +
                    "<center><h1>%d %s</h1></center>\n" +
                    "</body>\n" +
                    "</html>\n>";
    private static final int BAD_REQUEST = 400;
    private static final int METHOD_NOT_ALLOWED = 405;
    private static final int INTERNAL_ERROR = 500;
    private static final int BAD_GATEWAY = 502;
    private final static Map<Integer, String> errorCodes = new HashMap<>();

    private static final int SERVER_TIMEOUT = 2000;
    private static final int CLIENT_TIMEOUT = 1000;
    static AtomicInteger connClientListenersCount = new AtomicInteger(0);
    static AtomicInteger connServerListenersCount = new AtomicInteger(0);

    static {
        errorCodes.put(BAD_GATEWAY, "Bad Gateway");
        errorCodes.put(BAD_REQUEST, "Bad Request");
        errorCodes.put(METHOD_NOT_ALLOWED, "Method not allowed");
        errorCodes.put(INTERNAL_ERROR, "Internal error");
    }

    private final Socket socket;
    private final ExecutorService executorService;
    private final OutputStream toClient;
    private final InputStream fromClient;
    byte[] toServerBuffer = new byte[BUFFER_SIZE];
    byte[] toClientBuffer = new byte[BUFFER_SIZE];

    public Connection(Socket socket, ExecutorService executorService) throws IOException {
        this.socket = socket;
        this.executorService = executorService;
        toClient = new BufferedOutputStream(socket.getOutputStream());
        fromClient = new BufferedInputStream(socket.getInputStream());
        socket.setSoTimeout(CLIENT_TIMEOUT);
        System.out.println(">> connection up");
    }

    @Override
    public void run() {
        try {
            System.out.println("Server: " + connServerListenersCount.incrementAndGet());
            String requestLine = readLine(fromClient);

            URL url = parseRequestLine(requestLine);
            if (url == null) {
                return;
            }
            int port = url.getPort();
            port = port != -1 ? port : 80;

            System.out.println(">> connecting to " + url.getHost() + " " + port);
            try (Socket toServerSocket = new Socket(url.getHost(), port)) {
                toServerSocket.setSoTimeout(SERVER_TIMEOUT);
                OutputStream toServer = new BufferedOutputStream(toServerSocket.getOutputStream());
                InputStream fromServer = new BufferedInputStream(toServerSocket.getInputStream());

                executorService.execute(() -> {
                    try {
                        System.out.println("Client: " + connClientListenersCount.incrementAndGet());
                        toServer.write(requestLine.getBytes());

                        transferData(fromClient, toServer, toServerBuffer);
                    } catch (SocketTimeoutException ignored) {
                    } catch (IOException e) {
                        try {
                            toServerSocket.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    } finally {
                        System.out.println("Client: " + connClientListenersCount.decrementAndGet());
                    }
                });

                transferData(fromServer, toClient, toClientBuffer);
            }
        } catch (SocketTimeoutException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
            close(INTERNAL_ERROR);
        } finally {
            close();
            System.out.println("Server: " + connServerListenersCount.decrementAndGet());
        }
    }

    private URL parseRequestLine(String requestLine) throws MalformedURLException {
        String method;
        URL url;
        if (requestLine == null) {
            return null;
        }
        String[] requestLineTokens = requestLine.split(" ");
        if (requestLineTokens.length < 3) {
            close(BAD_REQUEST);
            return null;
        }
        method = requestLineTokens[0];
        if (!method.equals(GET_METHOD_TAG)) {
            close(METHOD_NOT_ALLOWED);
            return null;
        }
        url = new URL(requestLineTokens[1]);
        return url;
    }

    private void transferData(InputStream from, OutputStream to, byte[] buffer) throws IOException {
        int read;
        while (-1 != (read = from.read(buffer))) {
            to.write(buffer, 0, read);
            to.flush();
        }
    }

    private void close(int errorCode) {
        try {
            toClient.write(String.format(errorCodeTemplate, errorCode,
                    errorCodes.get(errorCode)).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        close();
    }

    private void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(">> connection down");
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
        byteArrayOutputStream.write('\n');
        return byteArrayOutputStream.toString();
    }
}
