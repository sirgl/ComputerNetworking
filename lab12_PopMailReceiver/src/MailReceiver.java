import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailReceiver implements Runnable {
    private static final String LOGIN_MESSAGE_TEMPLATE = "USER %s\r\n";
    private static final String PASSWORD_MESSAGE_TEMPLATE = "PASS %s\r\n";
    private static final String LIST_MESSAGE_TEMPLATE = "LIST\r\n";

    private static final String LOGIN_FILE = "login";

    private final Socket socket;
    private final BufferedReader reader;
    private final OutputStream outputStream;
    private final Map<Integer, String> messageMap = new HashMap<>();
    private String login;
    private String pass;
    private List<Integer> toDownload = new ArrayList<>();
    private int iteration = 1;

    public MailReceiver(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        outputStream = socket.getOutputStream();
    }

    public static void main(String[] args) throws IOException {
        new Thread(new MailReceiver("pop3.ccfit.nsu.ru", 110)).start();
    }

    @Override
    public void run() {
        try {
            parseLoginFile();
            handleResponse();
            outputStream.write(String.format(LOGIN_MESSAGE_TEMPLATE, login).getBytes());
            handleResponse();
            outputStream.write(String.format(PASSWORD_MESSAGE_TEMPLATE, pass).getBytes());
            handleResponse();
            while (true) {
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                String line = consoleReader.readLine();
                String[] split = line.split(" ");
                if (split.length < 1) {
                    continue;
                }
                switch (split[0]) {
                    case "load":
                        outputStream.write(LIST_MESSAGE_TEMPLATE.getBytes());
                        handleResponse();
                        toDownload = getUnreadMessageIds();
                        downloadMessages(toDownload);
                        System.out.println("You have " + toDownload.size() + " new letters");
                        iteration = 1;
                        break;
                    case "next":
                        if (iteration > toDownload.size()) {
                            System.out.println("No new messages");
                            break;
                        }
                        System.out.println(messageMap.get(iteration++));
                        break;
                    case "show":
                        if (split.length < 2) {
                            System.out.println("Usage: show <letterId>");
                            break;
                        }
                        int letterNumber = Integer.parseInt(split[1]);
                        String message = messageMap.get(letterNumber);
                        iteration = letterNumber;
                        System.out.println(message == null ? "No such letter" : message);
                        break;
                    default:
                        System.out.println("Commands: \n" +
                                "load - to load new messages\n" +
                                "next - to show next new message\n" +
                                "show <letterId> - to show <letterId> message");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseLoginFile() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(LOGIN_FILE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
        login = reader.readLine();
        pass = reader.readLine();
    }

    private void downloadMessages(List<Integer> toDownload) throws IOException {
        for (Integer letterId : toDownload) {
            outputStream.write(String.format("RETR %d\r\n", letterId).getBytes());
            handleResponse();
            String line;
            StringBuilder stringBuilder = new StringBuilder();
            while (!(line = reader.readLine()).equals(".")) {
                stringBuilder.append(line).append("\n");
            }
            messageMap.put(letterId, stringBuilder.toString());
        }
    }

    private List<Integer> getUnreadMessageIds() throws IOException {
        String line;
        List<Integer> toDownload = new ArrayList<>();
        while (!(line = reader.readLine()).equals(".")) {
            String[] messageNumbersTokens = line.split(" ");
            if (messageNumbersTokens.length < 2) {
                throw new IOException("Bad list line");
            }
            int letterId = Integer.parseInt(messageNumbersTokens[0]);
            if (!messageMap.containsKey(letterId)) {
                toDownload.add(letterId);
            }
        }
        return toDownload;
    }

    private void handleResponse() throws IOException {
        String line = reader.readLine();
        String[] split = line.split(" ");
        if (split.length < 1) {
            throw new IOException("Bad response");
        }
        if (split[0].equals("-ERR")) {
            throw new IOException("Error response");
        }
    }
}