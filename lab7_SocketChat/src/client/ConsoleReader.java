package client;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Scanner;

public class ConsoleReader implements Runnable {
    private final Scanner scanner = new Scanner(System.in);
    private final Client client;

    public ConsoleReader(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            String line;
            while (!Thread.interrupted()){
                line  = scanner.nextLine();
                String[] strings = line.split(":");
                if(strings.length < 1) {
                    continue;
                }
                switch(strings[0]) {
                    case "send":
                        if(strings.length < 3) {
                            System.err.println("format: send:<message>:<target name>");
                            break;
                        }
                        try {
                            client.sendMessage(strings[2], strings[1]);
                        } catch (NoSuchClientException e) {
                            System.err.println("No client with name" + strings[2]);
                        }
                        break;
                    case "list":
                        System.out.println("Users:");
                        List<String> users = client.getClients();
                        users.forEach(System.out::println);
                        break;
                    case "help":
                        System.out.println("commands: send, list");
                        break;
                }
            }
        } catch (SocketException ignored) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
