package client;

import protocol.NameOccupiedException;
import protocol.Protocol;
import protocol.ProtocolException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Client implements Runnable {
    private final String clientName;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private final Set<String> clients = new HashSet<>();

    public Client(int port, String serverIp, String clientName) throws IOException {
        this.clientName = clientName;
        Socket socket = new Socket(serverIp, port);
        outputStream = new DataOutputStream(socket.getOutputStream());
        inputStream = new DataInputStream(socket.getInputStream());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                sendLogout();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: Client <host> <port> <name>");
        }
        int port = Integer.parseInt(args[0]);
        String host = args[1];
        String name = args[2];

        Client client = new Client(port, host, name);
        new Thread(client).start();
        ConsoleReader reader = new ConsoleReader(client);
        new Thread(reader).start();
    }


    private void sendLogin() throws IOException {
        outputStream.writeByte(Protocol.LOGIN_COMMAND);
        outputStream.writeUTF(clientName);
    }

    private void sendLogout() throws IOException {
        outputStream.writeByte(Protocol.LOGOUT_COMMAND);
    }

    public void sendMessage(String target, String message) throws NoSuchClientException, IOException {
        synchronized (clients) {
            if (!clients.contains(target)) {
                throw new NoSuchClientException();
            }
            outputStream.writeByte(Protocol.MESSAGE_COMMAND);
            outputStream.writeUTF(target);
            outputStream.writeUTF(message);
        }
    }

    public List<String> getClients() {
        synchronized (clients) {
            List<String> copy = new ArrayList<>(clients.size());
            copy.addAll(clients);
            return copy;
        }
    }

    private void handleServerLoginAnswer() throws IOException, ProtocolException, NameOccupiedException {
        byte messageType = inputStream.readByte();
        switch (messageType) {
            case Protocol.LOGIN_SUCCESS_ANSWER:
                int usersCount = inputStream.readInt();
                synchronized (clients) {
                    for (int i = 0; i < usersCount; i++) {
                        String name = inputStream.readUTF();
                        clients.add(name);
                    }
                }
                break;
            case Protocol.LOGIN_FAILURE_ANSWER:
                throw new NameOccupiedException();
            default:
                throw new ProtocolException("Login answer expected, received: " + messageType);
        }
    }

    @Override
    public void run() {
        try {
            sendLogin();
            handleServerLoginAnswer();
            byte messageType;
            while (!Thread.interrupted()) {
                messageType = inputStream.readByte();
                switch (messageType) {
                    case Protocol.MESSAGE_EVENT:
                        String from = inputStream.readUTF();
                        String message = inputStream.readUTF();
                        System.out.println(from + " > " + message);
                        break;
                    case Protocol.USER_LOGIN_EVENT:
                        String newClientName = inputStream.readUTF();
                        synchronized (clients) {
                            clients.add(newClientName);
                        }
                        break;
                    case Protocol.USER_LOGOUT_EVENT:
                        String logoutClientName = inputStream.readUTF();
                        clients.remove(logoutClientName);
                        break;
                    default:
                        throw new ProtocolException("Unexpected message type: " + messageType);
                }
            }

        } catch (IOException | ProtocolException e) {
            e.printStackTrace();
        } catch (NameOccupiedException e) {
            System.err.println("Sorry, name " + clientName + " is already occupied");
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
