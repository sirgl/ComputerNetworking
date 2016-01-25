import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Node implements Runnable {
    public static final byte I_AM_YOUR_CHILD_MESSAGE_TAG = 0;
    public static final byte TEXT_MESSAGE_TAG = 1;
    public static final byte I_LEAVE_MESSAGE_TAG = 2;
    public static final byte HERE_IS_YOUR_PARENT_MESSAGE_TAG = 3;
    public static final byte YOU_ARE_ROOT_MESSAGE_TAG = 4;
    private static final String CHARSET = "UTF-8";
    private static final int BUFFER_SIZE = 256;
    private static final String INCOMING_MESSAGE_TAG = "> ";

    private final DatagramPacket sendPacket;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private final Set<SocketAddress> childSet = new HashSet<>();
    private final DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
    private final byte[] iAmYourChildMessage;
    private final Object socketSendLock = new Object();
    private boolean isRoot = false;
    private SocketAddress parentAddress;
    private DatagramSocket socket;

    //child
    public Node(SocketAddress parentAddress) throws IOException {
        this(false, parentAddress);
    }

    //root
    public Node() throws IOException {
        this(true, null);
    }

    private Node(boolean rootFlag, SocketAddress parentAddress) throws IOException {
        this.isRoot = rootFlag;
        this.parentAddress = parentAddress;
        socket = new DatagramSocket();
        iAmYourChildMessage = getSendReadyMessage(I_AM_YOUR_CHILD_MESSAGE_TAG, null);
        sendPacket = new DatagramPacket(iAmYourChildMessage, iAmYourChildMessage.length);

        System.out.println("Port: " + socket.getLocalPort());

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isRoot) {
                        if (!childSet.isEmpty()) {
                            Iterator<SocketAddress> iterator = childSet.iterator();
                            SocketAddress newRootAddress = iterator.next();
                            sendToAddress(getSendReadyMessage(YOU_ARE_ROOT_MESSAGE_TAG, null), newRootAddress);
                            iterator.remove();
                            byte[] parentAddressInBytes = newRootAddress.toString().getBytes(CHARSET);
                            sendToAllChild(getSendReadyMessage(HERE_IS_YOUR_PARENT_MESSAGE_TAG, parentAddressInBytes));
                        }
                    } else {
                        sendToParent(getSendReadyMessage(I_LEAVE_MESSAGE_TAG, null));
                        byte[] parentAddressInBytes = parentAddress.toString().getBytes(CHARSET);
                        sendToAllChild(getSendReadyMessage(HERE_IS_YOUR_PARENT_MESSAGE_TAG, parentAddressInBytes));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    socket.close();
                }
            }
        }));
    }

    public static byte[] getSendReadyMessage(byte messageId, byte[] payload) {
        if (payload == null) {
            byte[] buffer = new byte[1];
            buffer[0] = messageId;
            return buffer;
        }
        ByteBuffer buffer = ByteBuffer.allocate(payload.length + 2);
        buffer
                .put(messageId)
                .put((byte) payload.length)
                .put(payload);
        return buffer.array();
    }

    @Override
    public void run() {
        try {
            if (!isRoot) {
                sendToParent(iAmYourChildMessage);
            }
            while (true) {
                socket.receive(receivePacket);
                System.out.println("Message accepted");
                byte messageId = receivePacket.getData()[0];
                int messageLength = receivePacket.getData()[1];
//                System.out.println(messageLength);
                String message = new String(receivePacket.getData(), 2, messageLength, CHARSET);
                SocketAddress receivedAddress = receivePacket.getSocketAddress();
                switch (messageId) {
                    case I_AM_YOUR_CHILD_MESSAGE_TAG:
                        childSet.add(receivedAddress);
                        System.out.println("Add " + receivedAddress);
                        break;
                    case TEXT_MESSAGE_TAG:
                        System.out.println(INCOMING_MESSAGE_TAG + message);
                        resendToAll(receivePacket.getData(), receivedAddress);
                        break;
                    case I_LEAVE_MESSAGE_TAG:
                        childSet.remove(receivedAddress);
                        System.out.println("Removed " + receivedAddress);
                        break;
                    case HERE_IS_YOUR_PARENT_MESSAGE_TAG:
                        String[] split = message.split(":");
                        String[] splitAddress = split[0].split("/");
                        parentAddress = new InetSocketAddress(splitAddress[0], Integer.parseInt(split[1]));
                        sendToParent(iAmYourChildMessage);
                        System.out.println("New parent " + parentAddress);
                        break;
                    case YOU_ARE_ROOT_MESSAGE_TAG:
                        this.parentAddress = null;
                        this.isRoot = true;
                        System.out.println("I am root now");
                    default:
                        break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendToParent(byte[] message) throws IOException {
        sendToAddress(message, parentAddress);
    }

    public void sendToAllChild(byte[] message) throws IOException {
        synchronized (socketSendLock) {
            for (SocketAddress childAddress : childSet) {
                sendToAddress(message, childAddress);
            }
        }
    }

    public void sendToAllChildExcept(byte[] message, SocketAddress address) throws IOException {
        synchronized (socketSendLock) {
            for (SocketAddress childAddress : childSet) {
                if (!childAddress.equals(address)) {
                    sendToAddress(message, childAddress);
                }
            }
        }
    }

    public void sendToAll(byte[] message) throws IOException {
        if (!isRoot) {
            sendToParent(message);
        }
        sendToAllChild(message);
    }

    public void sendToAddress(byte[] message, SocketAddress address) throws IOException {
        synchronized (socketSendLock) {
            sendPacket.setSocketAddress(address);
            sendPacket.setData(message);
            sendPacket.setLength(message.length);
            socket.send(sendPacket);
        }
    }

    public void resendToAll(byte[] message, SocketAddress source) throws IOException {
        if (parentAddress != null && parentAddress.equals(source)) {
            sendToAllChild(message);
        } else {
            if (parentAddress != null) {
                sendToParent(message);
            }
            sendToAllChildExcept(message, source);
        }
    }
}
