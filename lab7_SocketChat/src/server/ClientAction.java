package server;

import java.io.IOException;

public interface ClientAction {
    void perform(ClientInfo info) throws IOException;
}
