package server;

import protocol.Protocol;
import protocol.SongMetaInfo;
import protocol.StationInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Worker implements Runnable {
    private final Socket socket;
    private final DataInputStream inputStream;
    private final Server server;
    private final DataOutputStream outputStream;

    public Worker(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        try {
            System.out.println("Client accepted");
            byte messageType;
            while(!Thread.interrupted()){
                messageType = inputStream.readByte();
                switch (messageType) {
                    case Protocol.STATIONS_LIST_REQUEST:
                        System.out.println("get list request");
                        List<StationInfo> stationInfoList = server.getStationInfoList();
                        outputStream.writeByte(Protocol.STATIONS_LIST_RESPONSE);
                        outputStream.writeInt(stationInfoList.size());
                        for (StationInfo stationInfo : stationInfoList) {
                            outputStream.writeUTF(stationInfo.getGroup());
                            outputStream.writeUTF(stationInfo.getName());
                        }
                        break;
                    case Protocol.SONG_META_REQUEST:
                        String stationName = inputStream.readUTF();
                        SongMetaInfo currentSongMetaInfo = server.getCurrentSongMetaInfo(stationName);
                        outputStream.write(Protocol.SONG_META_RESPONSE);
                        outputStream.writeUTF(currentSongMetaInfo.getSongName());
                        outputStream.writeUTF(currentSongMetaInfo.getPerformerName());
                    default:
                        break;
                }
            }
        } catch (IOException ignored) {
        } finally {
            server.removeWorker(this);
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Client removed");
        }
    }
}
