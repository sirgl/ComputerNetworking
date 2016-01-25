package client;

import protocol.Protocol;
import protocol.StationInfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TCPInputConnectionHandler implements Runnable {
    private final DataInputStream inputStream;
    private final Client client;
    private final Map<Integer, StationInfo> stationsMap = new HashMap<>();

    public TCPInputConnectionHandler(InputStream inputStream, Client client) {
        this.client = client;
        this.inputStream = new DataInputStream(inputStream);
    }

    @Override
    public void run() {
        try{
            byte messageType;
            while (!Thread.interrupted()) {
                messageType =  inputStream.readByte();
                switch (messageType) {
                    case Protocol.STATIONS_LIST_RESPONSE:
                        System.out.println("got stations response");
                        int listSize = inputStream.readInt();
                        client.clearStationInfos();
                        for (int i = 0; i < listSize; i++) {
                            String group = inputStream.readUTF();
                            String name = inputStream.readUTF();
                            client.addStationInfo(i, new StationInfo(name, group));
                            System.out.println(String.format("%d: %s %s", i, name, group));
                        }
                        break;
                    case Protocol.SONG_META_RESPONSE:
                        String songName = inputStream.readUTF();
                        String performerName = inputStream.readUTF();
                        System.out.println("Song: " + songName);
                        System.out.println("Performer: " + performerName);
                        break;
                }
            }
        } catch (IOException ignored) {
        }
    }
}
