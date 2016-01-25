package server;

import protocol.NameOccupiedException;
import protocol.Protocol;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server implements Runnable {
    private final ServerSocket serverSocket;
    private final Map<String, ClientInfo> clientMap = new HashMap<>();
    private final Map<String, Object> lockMap = new HashMap<>();
    private int clientCount = 0;
    public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public static void main(String[] args) throws IOException {
        new Thread(new Server(5000)).start();
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Socket acceptedSocket = serverSocket.accept();
                try {
                    acceptClient(acceptedSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptClient(Socket socket) throws IOException {
        new Thread(new Worker(socket, this)).start();
    }

    public void deleteClient(String name) throws IOException {
        synchronized (lockMap) {
            Object clientLock = lockMap.remove(name);
            if (clientLock == null) {
                return;
            }
            synchronized (clientLock) {
                ClientInfo clientInfo = clientMap.remove(name);
                clientCount--;
                clientInfo.getOutputStream().close();
                clientInfo.getWorkerThread().interrupt();
            }
        }
        System.out.println("Client " + name + " deleted");
    }

    private void applyActionOnClient(String name, ClientAction action) throws IOException {
        synchronized (lockMap) {
            Object clientLock = lockMap.get(name);
            synchronized (clientLock) {
                ClientInfo clientInfo = clientMap.get(name);
                action.perform(clientInfo);
            }
        }
    }

    public void sendLoginSuccess(String target) throws IOException {
        applyActionOnClient(target, (info) -> {
            DataOutputStream outputStream = info.getOutputStream();
            outputStream.writeByte(Protocol.LOGIN_SUCCESS_ANSWER);
            outputStream.writeInt(clientCount - 1);
            for (String clientName : clientMap.keySet()) {
                if (!clientName.equals(target)) {
                    outputStream.writeUTF(clientName);
                }
            }
        });
    }

    public void sendOtherUserLoginEvent(String source) throws IOException {
        synchronized (lockMap) {
            for (String target : clientMap.keySet()) {
                if (!target.equals(source)) {
                    applyActionOnClient(target, (info) -> {
                        DataOutputStream outputStream = info.getOutputStream();
                        outputStream.writeByte(Protocol.USER_LOGIN_EVENT);
                        outputStream.writeUTF(source);
                    });
                    System.out.println("Send user login event to " + target);
                }
            }
        }
    }

    public void sendOtherUserLogoutEvent(String source) throws IOException {
        synchronized (lockMap) {
            for (String target : clientMap.keySet()) {
                if (!target.equals(source)) {
                    applyActionOnClient(target, (info) -> {
                        DataOutputStream outputStream = info.getOutputStream();
                        outputStream.writeByte(Protocol.USER_LOGOUT_EVENT);
                        outputStream.writeUTF(source);
                    });
                    System.out.println("Send user login event to " + target);
                }
            }
        }
    }

    public void sendMessageEvent(String from, String target, String message) throws IOException {
        applyActionOnClient(target, info -> {
            DataOutputStream outputStream = info.getOutputStream();
            outputStream.writeByte(Protocol.MESSAGE_EVENT);
            outputStream.writeUTF(from);
            outputStream.writeUTF(message);
        });
    }

    public boolean tryAddClient(String name, Socket socket) throws IOException, NameOccupiedException {
        synchronized (lockMap) {
            Object clientLock = lockMap.get(name);
            if (clientLock != null) {
                throw new NameOccupiedException();
            }
            clientLock = new Object();
            synchronized (clientLock) {
                lockMap.put(name, clientLock);
                clientMap.put(name, new ClientInfo(Thread.currentThread(), socket));
                clientCount++;
                return true;
            }
        }
    }
}
