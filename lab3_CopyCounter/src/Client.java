import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class Client {
    private static final String USAGE = "Usage: java Client <port> <broadcast ip>";
    private static final int BUFFER_SIZE = 1024;
    private static final String CHARSET = "UTF-8";
    private static final String ALIVE_MESSAGE = "IBORN";
    private static final String REPLY_MESSAGE = "ILIVE";
    private static final String EXIT_MESSAGE = "IEXIT";

    private final String broadcastIp;
    private final int port;
    private DatagramPacket sendPacket;
    private DatagramSocket socket;
    private byte[] exitMessage;
    private InetSocketAddress broadcast;

    public Client(String broadcastIp, int port) {
        this.broadcastIp = broadcastIp;
        this.port = port;
    }


    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException(USAGE);
        }

        int port = Integer.parseInt(args[0]);
        String broadcastIp = args[1];

        Client client = new Client(broadcastIp, port);
        client.run();
    }

    public void run() {
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            byte[] aliveMessage = ALIVE_MESSAGE.getBytes(CHARSET);
            byte[] replyMessage = REPLY_MESSAGE.getBytes(CHARSET);
            exitMessage = EXIT_MESSAGE.getBytes(CHARSET);
            socket = new DatagramSocket(port);
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            sendPacket = new DatagramPacket(aliveMessage, aliveMessage.length);
            Set<SocketAddress> addressSet = new HashSet<>();
            broadcast = new InetSocketAddress(broadcastIp, port);
            ShutdownHook shutdownHook = new ShutdownHook();
            Runtime.getRuntime().addShutdownHook(shutdownHook);


            sendPacket.setSocketAddress(broadcast);
            socket.send(sendPacket);
            while(true) {
                socket.receive(receivePacket);

                String message = new String(receivePacket.getData(), 0, receivePacket.getLength(), CHARSET);
                if (message.equals(ALIVE_MESSAGE)) {
                    sendPacket.setData(replyMessage);
                    sendPacket.setLength(replyMessage.length);
                    sendPacket.setSocketAddress(receivePacket.getSocketAddress());
                    socket.send(sendPacket);
                    addressSet.add(receivePacket.getSocketAddress());
                } else if (message.equals(EXIT_MESSAGE)) {
                    addressSet.remove(receivePacket.getSocketAddress());
                } else if (message.equals(REPLY_MESSAGE)) {
                    addressSet.add(receivePacket.getSocketAddress());
                } else {
                    continue;
                }
                System.out.println("Current copies:");
                addressSet.stream().forEach(System.out::println);
                System.out.println("Total: " + addressSet.size());
                System.out.println(message);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    class ShutdownHook extends Thread{
        @Override
        public void run() {
            sendPacket.setData(exitMessage);
            sendPacket.setLength(exitMessage.length);
            sendPacket.setSocketAddress(broadcast);
            try {
                socket.send(sendPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

