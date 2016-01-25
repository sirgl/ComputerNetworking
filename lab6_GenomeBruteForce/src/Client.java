import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Client implements Runnable {
    private final String host;
    private final int port;
    private final MessageDigest md5;
    private byte[] hash;
    private int stringLength;
    private boolean messageExpected = true;
    private boolean rangesAvailable = true;
    private Set<String> resultSet = new HashSet<>();

    public Client(String host, int port) throws IOException, NoSuchAlgorithmException {
        this.host = host;
        this.port = port;
        md5 = MessageDigest.getInstance("MD5");
        md5.digest();
    }


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        if(args.length != 2) {
            throw new IllegalArgumentException("Usage: Client <host> <port>");
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        new Thread(new Client(host, port)).run();
    }

    private static void readToArray(DataInputStream inputStream, byte[] array) throws IOException {
        int read = 0;
        while (read != array.length) {
            read += inputStream.read(array, read, array.length - read);
        }
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port)) {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            sendNewClientMessage(outputStream);
            handleServerMessages(inputStream, outputStream);
//            messageExpected = true;
//            handleServerMessages(inputStream, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        messageExpected = true;
        while (true) {
            try (Socket socket = new Socket(host, port)) {
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                sendResultsMessage(outputStream);
                sendNextRangeMessage(outputStream);
                handleServerMessages(inputStream, outputStream);
                if (!rangesAvailable) {
                    break;
                }
                messageExpected = true;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

        }
    }

    private void handleServerMessages(DataInputStream inputStream, DataOutputStream outputStream) throws IOException {
        while (messageExpected) {
            int messageLength = inputStream.readInt();
            byte messageType = inputStream.readByte();
            switch (messageType) {
                case Protocol.NEW_CLIENT_RESPONSE_MESSAGE:
                    System.out.println("new client response");
                    stringLength = inputStream.readInt();
                    hash = new byte[messageLength - 4];
                    readToArray(inputStream, hash);
                    sendNextRangeMessage(outputStream);
                    break;
                case Protocol.NEXT_RANGE_RESPONSE_MESSAGE:
                    System.out.println("get range");
                    messageExpected = false; //
                    int unpackedGenomeLength = inputStream.readInt();
                    byte[] packedPrefix = new byte[stringLength];
                    readToArray(inputStream, packedPrefix);
                    char[] currentRange = new char[stringLength];
                    for (int i = 0, currentRangeLength = currentRange.length; i < currentRangeLength; i++) {
                        currentRange[i] = 'A';
                    }
                    char[] prefix = Protocol.unpackGenomeString(packedPrefix, unpackedGenomeLength);
                    System.arraycopy(prefix, 0, currentRange, 0, prefix.length);
                    long maxRangeIndex = (long) Math.pow(4, stringLength - unpackedGenomeLength);
                    resultSet.clear();
                    for (long i = 0; i < maxRangeIndex; i++) {
                        byte[] digest = md5.digest(new String(currentRange).getBytes(StandardCharsets.UTF_8));
                        if (Arrays.equals(digest, hash)) {
                            resultSet.add(new String(currentRange.clone()));
                        }
                        Protocol.convertToNextSequence(currentRange);
                    }
                    break;
                case Protocol.NO_RANGE_AVAILABLE_MESSAGE:
                    System.out.println("No range available");
                    rangesAvailable = false;
                    messageExpected = false;
            }
        }
    }

    private void sendResultsMessage(DataOutputStream stream) throws IOException {
        int size = 0;
        List<byte[]> sendReady = new ArrayList<>();
        for (String result : resultSet) {
            byte[] packedGenomeString = Protocol.packGenomeString(result.toCharArray());
            size += packedGenomeString.length;
            sendReady.add(packedGenomeString);
        }
        stream.writeInt(size);
        stream.writeByte(Protocol.RESULTS_MESSAGE);
        //Warning! Don't send int!
        for (byte[] bytes : sendReady) {
            stream.write(bytes);
        }
        System.out.println("send results");
    }

    private void sendNextRangeMessage(DataOutputStream stream) throws IOException {
        stream.writeInt(0);
        stream.writeByte(Protocol.GET_NEXT_RANGE_MESSAGE);
        System.out.println("send get next range message");
    }

    private void sendNewClientMessage(DataOutputStream outputStream) throws IOException {
        outputStream.writeInt(0);
        outputStream.writeByte(Protocol.NEW_CLIENT_MESSAGE);
    }
}
