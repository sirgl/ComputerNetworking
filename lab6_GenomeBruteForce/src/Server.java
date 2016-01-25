import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Server implements Runnable {
    private int prefixLength = 2;
    private int rangeCount = (int) Math.pow(4, prefixLength);
    private final int port;
    private final byte[] hash;
    private final int stringLength;
    private Map<InetAddress, char[]> clientRangeMap = new HashMap<>();
    private Set<String> resultSet = new HashSet<>();

    private char[] currentRange;
    private int currentRangeIndex = 1;
    private boolean messageExpected = true;
    private boolean keepRunning = true;

    public Server(int port, byte[] hash, int stringLength) {
        this.port = port;
        this.hash = hash;
        if(stringLength < 1) {
            throw new IllegalArgumentException();
        }
        if(stringLength == 1){
            prefixLength = 1;
            rangeCount = 4;
        }
        this.stringLength = stringLength;
        currentRange = new char[prefixLength];
        for (int i = 0; i < currentRange.length; i++) {
            currentRange[i] = 'A';
        }

    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("MD5").digest("ATGGAAAAT".getBytes());
        new Thread(new Server(5000, digest, 9)).run();
    }


    @Override
    public void run() {
        byte[] buffer = new byte[2048];
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (keepRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("client accepted");
                    messageExpected = true;
                    handleClient(buffer, socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(byte[] buffer, Socket socket) throws IOException {
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
//        InetSocketAddress inetAddress = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
        InetAddress inetAddress = socket.getInetAddress();
        while (messageExpected) {
            int length = inputStream.readInt();
            System.out.println();
            byte messageType = inputStream.readByte();
            int read = 0;
            while (read != length) {
                read += inputStream.read(buffer, 0, length);
            }
            switch (messageType) {
                case Protocol.NEW_CLIENT_MESSAGE:
                    System.out.println("new client");
                    sendHandshakeMessage(outputStream);
                    clientRangeMap.put(inetAddress, null);
                    break;
                case Protocol.GET_NEXT_RANGE_MESSAGE:
                    System.out.println("range");
                    if (currentRangeIndex < rangeCount) {
                        sendRangeMessage(outputStream);
                        clientRangeMap.put(inetAddress, currentRange.clone());
                        Protocol.convertToNextSequence(currentRange);
                        currentRangeIndex++;
                    } else {
                        sendNoRangeAvailableMessage(outputStream);
                        if (clientRangeMap.isEmpty()) {
                            keepRunning = false;
                        }
                    }
                    messageExpected = false;
                    socket.close();
                    break;
                case Protocol.RESULTS_MESSAGE:
                    System.out.println("results");
                    parseResults(buffer, length);
                    clientRangeMap.remove(inetAddress);
//                    messageExpected = false;
                    break;
                default:
                    break;
            }
            printCondition();
        }
    }

    private void printCondition() {
        System.out.println("------------");
        System.out.println("Clients:");
        for (Map.Entry<InetAddress, char[]> entry : clientRangeMap.entrySet()) {
            System.out.println(entry.getKey() + " : " +
                    (entry.getValue() == null ? "" : new String(entry.getValue())));
        }
        System.out.println("Results:");
        for (String chars : resultSet) {
            System.out.println(chars);
        }
        System.out.println("--------------");
    }

    private void parseResults(byte[] buffer, int length) {
        int resultInBytesLength = stringLength / 4 + (stringLength % 4 == 0 ? 0 : 1);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byte[] currentResult = new byte[resultInBytesLength];
        int resultsCount = length / resultInBytesLength;
        for (int i = 0; i < resultsCount; i++) {
            byteBuffer.get(currentResult);
            resultSet.add(new String(Protocol.unpackGenomeString(currentResult, stringLength)));
        }
    }

    private void sendNoRangeAvailableMessage(DataOutputStream stream) throws IOException {
        stream.writeInt(0);
        stream.write(Protocol.NO_RANGE_AVAILABLE_MESSAGE);
    }

    private void sendHandshakeMessage(DataOutputStream stream) throws IOException {
        stream.writeInt(4 + hash.length);
        stream.writeByte(Protocol.NEW_CLIENT_RESPONSE_MESSAGE);
        stream.writeInt(stringLength);
        stream.write(hash);
        stream.flush();
    }

    private void sendRangeMessage(DataOutputStream stream) throws IOException {
        byte[] packedGenome = Protocol.packGenomeString(currentRange);
        stream.writeInt(packedGenome.length + 4);
        stream.writeByte(Protocol.NEXT_RANGE_RESPONSE_MESSAGE);
        stream.writeInt(prefixLength);
        stream.write(packedGenome);
    }
}
