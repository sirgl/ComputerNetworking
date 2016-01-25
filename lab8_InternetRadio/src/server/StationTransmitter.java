package server;

import protocol.SongMetaInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class StationTransmitter implements Runnable {
    private static final int PACKET_SIZE = 4096;
    private static final int DELAY = 3000;
    private final MulticastSocket socket;
    private final Path tracksFolder;
    private final DatagramPacket packet;
    private final String group;
    private SongMetaInfo metaInfo;

    public StationTransmitter(String group, int port, MulticastSocket socket, Path tracksFolder) throws IOException {
        this.socket = socket;
        this.tracksFolder = tracksFolder;
        this.socket.joinGroup(InetAddress.getByName(group));
        this.group = group;
        packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE, InetAddress.getByName(group), port);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tracksFolder)) {
                for (Path path : stream) {
                    try (InputStream inputSteam = Files.newInputStream(path)) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputSteam));
                        readSongMeta(reader);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            byte[] buf = line.getBytes();
                            packet.setData(buf, 0, buf.length);
                            socket.send(packet);
                            Thread.sleep(DELAY);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void readSongMeta(BufferedReader reader) throws IOException {
        String songName = reader.readLine();
        String performerName = reader.readLine();
        metaInfo = new SongMetaInfo(songName, performerName);
    }

    public String getGroup() {
        return group;
    }

    public SongMetaInfo getSongMetaInfo() {
        return metaInfo;
    }
}
