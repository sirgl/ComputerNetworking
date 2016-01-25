package client;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class UDPInputConnectionHandler implements Runnable {
    private static final int BUFFER_SIZE = 4096;
    private final StationSynchronizer currentStation;
    private final int port;
    private final MulticastSocket socket;
    private final DatagramPacket packet;
    private String group = null;


    public UDPInputConnectionHandler(StationSynchronizer currentStation, int port) throws IOException {
        this.currentStation = currentStation;
        this.port = port;
        socket = new MulticastSocket(port);
        packet = new DatagramPacket(new byte[BUFFER_SIZE], 4096);
        packet.setPort(port);
    }

    @Override
    public void run() {
        try {
            while(!Thread.interrupted()) {
                String group = currentStation.waitForGroup();
                if (this.group == null) {
                    socket.joinGroup(InetAddress.getByName(group));
                    this.group = group;
                } else {
                    if(group == null) {
                        socket.leaveGroup(InetAddress.getByName(this.group));
                        this.group = null;
                    } else {
                        if (!this.group.equals(group)) {
                            socket.leaveGroup(InetAddress.getByName(this.group));
                            socket.joinGroup(InetAddress.getByName(group));
                            this.group = group;
                        }
                    }
                }
                socket.receive(packet);
                System.out.println(">>>" + new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8));
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}
