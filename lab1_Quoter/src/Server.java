import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Server {
    public static final int BUFFER_SIZE = 1024;
    public static final String MODULE_NAME = "lab1_Quoter/";
    private static final String usage = "Usage: java Server <port> <path>";

    public static void main(String[] args) {
        if(args.length < 2) {
            throw new IllegalArgumentException(usage);
        }

        int port = Integer.parseInt(args[0]);
        String path = args[1];

        try {
            FileInputStream fileInputStream = new FileInputStream(MODULE_NAME + path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream));
            String line;
            DatagramSocket datagramSocket = new DatagramSocket(port);
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while(null != (line = reader.readLine())){
                datagramSocket.receive(packet);
                System.out.println(packet.getAddress() + " " + line);
                byte[] message = line.getBytes("UTF-8");
                packet.setData(message);
                packet.setLength(message.length);
                datagramSocket.send(packet);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found in path: " + path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
