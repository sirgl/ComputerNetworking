package client;

import protocol.Protocol;
import protocol.StationInfo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client implements Runnable {
    private final int port;
    private final String address;
    private final Socket socket;
    private final DataOutputStream outputStream;
    private final StationSynchronizer currentStation = new StationSynchronizer(null);
    private final Map<Integer, StationInfo> stationsMap = new ConcurrentHashMap<>();
    private String stationName = null;

    public Client(int port, String address) throws IOException {
        this.port = port;
        this.address = address;
        socket = new Socket(address, port);
        outputStream = new DataOutputStream(socket.getOutputStream());


        new Thread(new TCPInputConnectionHandler(socket.getInputStream(), this)).start();
        new Thread(new UDPInputConnectionHandler(currentStation, port)).start();
    }


    public static void main(String[] args) throws IOException {
        new Thread(new Client(5000, "192.168.43.122")).start();
    }

    public void addStationInfo(int number, StationInfo info) {
        stationsMap.put(number, info);
    }

    public void clearStationInfos() {
        stationsMap.clear();
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] strings = line.split(" ");
                switch (strings[0]) {
                    case "list":
                        sendListStationsRequest();
                        break;
                    case "help":
                        System.out.println("list - to get a list of stations\n" +
                                "switch <number from list> - to switch to <number> channel of radio\n");
                        break;
                    case "switch":
                        if (strings.length < 2) {
                            System.err.println("Usage: switch <number from list of stations>");
                            break;
                        }
                        int stationNumber;
                        try {
                            stationNumber = Integer.parseInt(strings[1]);
                        } catch (NumberFormatException e) {
                            System.err.println("Incorrect number format");
                            break;
                        }
                        StationInfo stationInfo = stationsMap.get(stationNumber);
                        if (stationInfo == null) {
                            System.err.println("No station with such number");
                            break;
                        }
                        currentStation.setGroup(stationInfo.getGroup());
                        stationName = stationInfo.getName();
                        break;
                    case "song":
                        if(stationName == null) {
                            System.err.println("No station running");
                            break;
                        }
                        sendSongMetaRequest();
                    default:
                        break;
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void sendListStationsRequest() throws IOException {
        outputStream.writeByte(Protocol.STATIONS_LIST_REQUEST);
        System.out.println("list request sent");
    }

    private void sendSongMetaRequest() throws IOException {
        outputStream.writeByte(Protocol.SONG_META_REQUEST);
        outputStream.writeUTF(stationName);
        System.out.println("song meta request sent");
    }
}
