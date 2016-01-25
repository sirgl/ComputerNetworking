import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Server {
    public static final int BUFFER_SIZE = 64;
    private static final String USAGE = "Usage: java Server <port>";
    private static final String PONG_MESSAGE = "pong";
    public static final String PING_MESSAGE = "ping";

    public static void main(String[] args) {
        if(args.length < 1) {
            throw new IllegalArgumentException(USAGE);
        }

        int port = Integer.parseInt(args[0]);

        try (DatagramSocket datagramSocket = new DatagramSocket(port)){
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while(true){
                datagramSocket.receive(packet);
                if(!new String(packet.getData(), 0, packet.getLength()).equals(PING_MESSAGE)) {
                    System.err.println("Unexpected message accepted");
                    break;
                }
                byte[] pongMessage = PONG_MESSAGE.getBytes("UTF-8");
                packet.setData(pongMessage);
                packet.setLength(pongMessage.length);
                datagramSocket.send(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
