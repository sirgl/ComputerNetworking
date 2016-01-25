import java.io.*;
import java.net.Socket;

public class SmtpSender implements Runnable {
    private static final String HELLO_MESSAGE_TEMPLATE = "HELO %s\r\n";
    private static final String FROM_MESSAGE_TEMPLATE = "MAIL FROM: %s\r\n";
    private static final String TO_MESSAGE_TEMPLATE = "RCPT TO: %s\r\n";
    private static final String SUBJECT_LINE_TEMPLATE = "Subject: %s\n\n";
    private static final String DATA_MESSAGE_TEMPLATE = "DATA\n";
    private static final String QUIT_MESSAGE_TEMPLATE = "QUIT\r\n";

    private static final String HELLO_MESSAGE_HOST = "localhost";

    private static final int READY_TO_WORK = 220;
    private static final int COMMAND_SUCCESS = 250;
    private static final int DATA_ACCEPTED_AND_WAIT_FOR_MESSAGE = 354;
    private static final int SUCCESSFULLY_CLOSED = 221;
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintStream outputStream;

    public SmtpSender(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        outputStream = new PrintStream(socket.getOutputStream());
//        new PrintWriter()
    }

    public static void main(String[] args) throws IOException {
        if(args.length < 2) {
            throw new IllegalArgumentException("Usage: <host> <port>");
        }
        new Thread(new SmtpSender(args[0], Integer.parseInt(args[1]))).start();
    }

    @Override
    public void run() {
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        try {
            expectAnswer(READY_TO_WORK);
            outputStream.print(String.format(HELLO_MESSAGE_TEMPLATE, HELLO_MESSAGE_HOST));
            expectAnswer(COMMAND_SUCCESS);
            System.out.println("Please, enter your mail address");
            String fromAddress = consoleReader.readLine();
            outputStream.print(String.format(FROM_MESSAGE_TEMPLATE, fromAddress));
            expectAnswer(COMMAND_SUCCESS);
            System.out.println("Please, enter recipient address");
            String toAddress = consoleReader.readLine();
            outputStream.print(String.format(TO_MESSAGE_TEMPLATE, toAddress));
            expectAnswer(COMMAND_SUCCESS);
            outputStream.print(DATA_MESSAGE_TEMPLATE);
            System.out.println("DATA");
            expectAnswer(DATA_ACCEPTED_AND_WAIT_FOR_MESSAGE);

            System.out.println("Please, enter subject of the letter");
            String subject = consoleReader.readLine();
            outputStream.print(String.format(SUBJECT_LINE_TEMPLATE, subject));

            System.out.println("Please, enter your message(line with only . symbol is the end of the letter)");
            String line;
            while((line = consoleReader.readLine()) != null) {
                outputStream.print(line);
                outputStream.print("\n");

                if(line.equals(".")) {
                    break;
                }
            }
            outputStream.print(QUIT_MESSAGE_TEMPLATE);
            expectAnswer(COMMAND_SUCCESS);
            expectAnswer(SUCCESSFULLY_CLOSED);
            System.out.println("Mail have been sent");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void expectAnswer(int expectedCode) throws IOException {
        String line = reader.readLine();
        String[] split = line.split(" ");
        if(split.length < 1) {
            throw new IOException("Invalid response");
        }
        if(Integer.parseInt(split[0]) != expectedCode) {
            throw new IOException("Unexpected code: " + line);
        }
    }


}
