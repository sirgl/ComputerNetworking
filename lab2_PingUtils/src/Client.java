import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

public class Client {
    public static final int TIMEOUT = 1000;
    private static final String PING_MESSAGE = "ping";
    private static final String PONG_MESSAGE = "pong";
    private static final String usage = "Usage: java Client <hostname> <port> <iterations>";

    public static void main(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException(usage);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int iterations = Integer.parseInt(args[2]);

        try (DatagramSocket datagramSocket = new DatagramSocket()){
            int lost = 0;
            datagramSocket.setSoTimeout(TIMEOUT);
            for (int i = 0; i < iterations; i++) {
                byte[] pingMessage = PING_MESSAGE.getBytes("UTF-8");
                DatagramPacket datagramPacket = new DatagramPacket(pingMessage, pingMessage.length, new InetSocketAddress(host, port));
                long startTime = System.currentTimeMillis();
                datagramSocket.send(datagramPacket);
                try {
                    datagramSocket.receive(datagramPacket);
                    long endTime = System.currentTimeMillis();
                    String accepted = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
                    if (!accepted.equals(PONG_MESSAGE)) {
                        System.err.println("Unexpected message accepted");
                        iterations = i + 1;
                        lost++;
                        break;
                    }
                    System.out.println(new String(datagramPacket.getData()));
                    System.out.println("Ping success: " + (endTime - startTime) + " millisecond");
                } catch (SocketTimeoutException e) {
                    lost++;
                    System.out.println("Ping failure, timeout");
                }
            }
            System.out.println("Total: " + iterations + " Lost: " + lost);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
