package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientInfo {
    private final Thread workerThread;
    private final DataOutputStream outputStream;

    public ClientInfo(Thread workerThread, Socket socket) throws IOException {
        this.workerThread = workerThread;
        outputStream = new DataOutputStream(socket.getOutputStream());
    }

    public Thread getWorkerThread() {
        return workerThread;
    }

    public DataOutputStream getOutputStream() {
        return outputStream;
    }
}
