package server;

import protocol.NameOccupiedException;
import protocol.Protocol;
import protocol.ProtocolException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Worker implements Runnable {
    private final Socket socket;
    private final Server server;
    DataInputStream inputStream;
    private String clientName;

    public Worker(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.inputStream = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        byte messageType;
        try {
            handleLogin();
            while (!Thread.interrupted()) {
                messageType = inputStream.readByte();
                switch (messageType) {
                    case Protocol.MESSAGE_COMMAND:
                        String target = inputStream.readUTF();
                        String message = inputStream.readUTF();
                        server.sendMessageEvent(clientName, target, message);
                        System.out.println("send message " + message + " to " + target);
                        break;
                    case Protocol.LOGOUT_COMMAND:
                        server.sendOtherUserLogoutEvent(clientName);
                        break;
                    default:
                        throw new ProtocolException("Unexpected message type: " + messageType);
                }
            }
        } catch (NameOccupiedException e) {
            try {
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeByte(Protocol.LOGIN_FAILURE_ANSWER);
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (IOException ignored) {
        } catch (ProtocolException e) {
            e.printStackTrace();
        } finally {
            try {
                server.deleteClient(clientName);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    private void handleLogin() throws IOException, ProtocolException, NameOccupiedException {
        byte messageType;
        messageType = inputStream.readByte();
        if (messageType != Protocol.LOGIN_COMMAND) {
            throw new ProtocolException("Expected login message type, but found: " + messageType);
        }
        clientName = inputStream.readUTF();
        System.out.println("Received client with name " + clientName);
        server.tryAddClient(clientName, socket);
        server.sendLoginSuccess(clientName);
        server.sendOtherUserLoginEvent(clientName);
    }
}
