import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Client {
    public static final String NAME = "";
    public static final int BUFFER_SIZE = 1024;
    private static final String USAGE = "Usage: java Client <hostname> <port>";

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException(USAGE);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            byte[] message = NAME.getBytes("UTF-8");
            DatagramSocket datagramSocket = new DatagramSocket();
            DatagramPacket datagramPacket = new DatagramPacket(message, message.length, new InetSocketAddress(host, port));
            datagramSocket.send(datagramPacket);

            datagramPacket.setData(buffer);
            datagramPacket.setLength(buffer.length);
            datagramSocket.receive(datagramPacket);
            System.out.println(new String(buffer, "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
