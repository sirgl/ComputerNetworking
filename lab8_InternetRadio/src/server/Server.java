package server;

import protocol.SongMetaInfo;
import protocol.StationInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Server implements Runnable {
    private final int port;
    private final Path stationsDirectory;
    private static final Path tracksFolderName = Paths.get("tracks");
    private static final Path metaInfoFileName = Paths.get("meta");
    private final Map<String, StationTransmitter> transmitterMap = new ConcurrentHashMap<>();
    private final Set<Worker> workers = Collections.synchronizedSet(new HashSet<>());
    private final MulticastSocket udpSocket;
    private ServerSocket serverSocket;

    public Server(int port, String stationsDirectory) throws IOException {
        this.port = port;
        this.stationsDirectory = Paths.get(stationsDirectory);
        udpSocket = new MulticastSocket(port);
        serverSocket = new ServerSocket(port);
    }

    public static void main(String[] args) throws IOException {
        new Thread(new Server(5000, "stations")).start();
    }

    @Override
    public void run() {
        try {
            startStations();
            while (!Thread.interrupted()){
                Socket acceptedSocket = serverSocket.accept();
                Worker worker = new Worker(acceptedSocket, this);
                workers.add(worker);
                new Thread(worker).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                serverSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public List<StationInfo> getStationInfoList() {
        return transmitterMap.entrySet().stream()
                .map(entry -> new StationInfo(entry.getKey(), entry.getValue().getGroup()))
                .collect(Collectors.toList());
    }

    public void removeWorker(Worker worker) {
        workers.remove(worker);
    }

    public SongMetaInfo getCurrentSongMetaInfo(String stationName) {
        StationTransmitter stationTransmitter = transmitterMap.get(stationName);
        return stationTransmitter.getSongMetaInfo();
    }

    private void startStations() throws IOException {
        try(DirectoryStream<Path> stationsDirectoryStream = Files.newDirectoryStream(stationsDirectory)) {
            for (Path stationDirectory : stationsDirectoryStream) {
                Path tracksFolder = stationDirectory.resolve(tracksFolderName);
                Path metaInfoPath = stationDirectory.resolve(metaInfoFileName);
                try(InputStream inputStream = Files.newInputStream(metaInfoPath)) {
                    Properties meta = new Properties();
                    meta.load(inputStream);
                    String group = meta.getProperty("group");
                    String stationName = meta.getProperty("name");
                    if(group == null || stationName == null) {
                        throw new IOException("group and stationName must present in meta file of station");
                    }
                    StationTransmitter stationTransmitter = new StationTransmitter(group, port, udpSocket, tracksFolder);
                    transmitterMap.put(stationName, stationTransmitter);
                    new Thread(stationTransmitter, stationName).start();
                }
            }
        }
    }
}
