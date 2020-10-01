import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

public class ServerClientHandler implements Runnable {
    // Maintain data about the client serviced by this thread
    ClientConnectionData client;
    final ArrayList<ClientConnectionData> clientList;

    public ServerClientHandler(ClientConnectionData client, ArrayList<ClientConnectionData> clientList) {
        this.client = client;
        this.clientList = clientList;
    }

    /**
     * Broadcasts a message to all clients connected to the server.
     */
    public void broadcast(String msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    c.getOut().println(msg);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to all clients
     * other than the message sender connected to the server.
     */
    public void broadcastExcludeSender(String msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    if (!c.getSocket().getInetAddress().equals(client.getSocket().getInetAddress()))
                        c.getOut().println(msg);
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to one other client connected to the server.
     */
    public void whisper(String msg, ClientConnectionData usr) {
        try {
            assert usr != null;
            System.out.println("Whispering -- " + msg);
            usr.getOut().println(msg);
        } catch (AssertionError ex) {
            System.out.println("That user does not exist.");
        } catch (Exception ex) {
            System.out.println("whisper caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = client.getInput();
            //TODO: get userName, first message from user

            client.getOut().println("SUBMITNAME");
            boolean nameValidity = false;


            while (!nameValidity) {
                String userName = in.readLine().trim();
                boolean repeat = false;

                synchronized (clientList) {
                    for (ClientConnectionData c : clientList) {
                        if (c.getUserName()!=null && c.getUserName().equals(userName)) {
                            repeat = true;
                            break;
                        }
                    }
                }
                if (userName.contains(" ") || userName.equals("") || repeat) {
                    client.getOut().println("SUBMITNAME");
                }
                else {
                    nameValidity = true;
                    client.setUserName(userName);
                    client.getOut().println("ACCEPT");

                }
            }

            //notify all that client has joined
            broadcast(String.format("WELCOME %s", client.getUserName()));


            String incoming;

            while( (incoming = in.readLine()) != null) {
                if (incoming.startsWith("CHAT")) {
                    String chat = incoming.substring(4).trim();
                    if (chat.length() > 0) {
                        String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                        broadcastExcludeSender(msg);
                    }
                } else if (incoming.startsWith("PCHAT")) {
                    String chat = incoming.substring(5).trim();
                    String name= chat.split(" ")[0];
                    String content = chat.split(" ")[1];
                    if (content.length() > 0) {
                        String msg = String.format("PCHAT %s %s", client.getUserName(), content);
                        for (ClientConnectionData c : clientList){
                            if (c.getName().equals(name)) {
                                whisper(msg, c);
                                break;
                            }
                        }
                    }
                } else if (incoming.startsWith("QUIT")){
                    break;
                }
            }
        } catch (Exception ex) {
            if (ex instanceof SocketException) {
                System.out.println("Caught socket ex for " +
                        client.getName());
            } else {
                System.out.println(ex);
                ex.printStackTrace();
            }
        } finally {
            //Remove client from clientList, notify all
            synchronized (clientList) {
                clientList.remove(client);
            }
            System.out.println(client.getName() + " has left.");
            broadcast(String.format("EXIT %s", client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ignored) {}
        }
    }
}