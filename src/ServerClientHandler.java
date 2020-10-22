import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

//TODO: Change all the "writeObjects" to actually send objects
//Make sure you trim stuff before sending it

public class ServerClientHandler implements Runnable {

    // Maintain data about the client serviced by this thread
    ClientConnectionData client;
    final ArrayList<ClientConnectionData> clientList;

    public ServerClientHandler(ClientConnectionData client, ArrayList<ClientConnectionData> clientList) {
        this.client = client;
        this.clientList = clientList;
    }

    /**
     * Lists all rooms in the server
     */

    private void listRooms(){
        try {
            System.out.println("Listing rooms");
            HashSet<String> rooms = new HashSet<>();
            synchronized (clientList) {
                for (ClientConnectionData c : clientList) {
                    rooms.add(c.getRoom());
                }
            }
            StringBuilder sb = new StringBuilder(ChatServer.LIST + " ");
            for (String s : rooms) {
                sb.append(s.trim()).append(" ");
            }
            client.getOut().writeObject(sb.toString());
            client.getOut().flush();
        } catch (Exception ex) {
            System.out.println("listing caught exception: " + ex);
            ex.printStackTrace();
        }
    }
    /**
     * Join a room
     */
    private void joinRoom(String room) {
        try {
            System.out.println(client.getUserName() + " joining room " + room);
            client.setRoom(room);
            for (ClientConnectionData c : clientList) {
                if (c.getRoom().equals(room)) {
                    c.getOut().writeObject(String.format("%s %s\n", ChatServer.JOIN_ROOM, client.getUserName()));
                    client.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("join_room caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    /**
     * Leave current room
     */
    private void leaveRoom(){
        try {
            System.out.println(client.getUserName() + " leaving room " + client.getRoom());
            String room = client.getRoom();
            client.setRoom("");
            for (ClientConnectionData c : clientList) {
                if (c.getRoom().equals(room)) {
                    c.getOut().writeObject(String.format("%s %s\n", ChatServer.LEAVE_ROOM, client.getUserName()));
                    client.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("leave_room caught exception: " + ex);
            ex.printStackTrace();
        }
    }
    /**
     * Broadcasts a message to all clients
     * other than the message sender connected to the server.
     * If the client is in a room, braodcasts to those in the room instead.
     */
    public void broadcast(ChatMessage msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    if (c.getUserName() != null && !c.getUserName().equals(client.getUserName()) && c.getRoom().equals(client.getRoom()))
                        c.getOut().writeObject(msg);
                    client.getOut().flush();
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
    public void whisper(ChatMessage msg, ArrayList<ClientConnectionData> users) {
        try {
            for (ClientConnectionData user : users) {
                assert user != null;
                assert user.getUserName() != null;
                System.out.println("Whispering to " + user.getUserName() + " -- " + msg);
                user.getOut().writeObject(msg);
                client.getOut().flush();
            }
        } catch (AssertionError ex) {
            System.out.println("That user does not exist.");
        } catch (Exception ex) {
            System.out.println("whisper caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    public void naming(String userName) {
        try {
            client.setUserName(null);
            boolean nameValidity = false;
            while (!nameValidity) {
                nameValidity = true;

                if (userName == null || userName.contains(" ") || userName.equals("")) {
                    nameValidity = false;
                } else {
                    synchronized (clientList) {
                        for (ClientConnectionData c : clientList) {
                            if (c.getUserName() != null && c.getUserName().equals(userName)) {
                                nameValidity = false;
                                break;
                            }
                        }
                    }
                }

                if (!nameValidity) {
                    client.getOut().writeObject("SUBMITNAME");
                    client.getOut().flush();
                    userName = client.getInput().readLine();
                    userName = userName.substring(ChatServer.NAME.length());
                    userName = userName.trim();
                } else {
                    client.setUserName(userName);
                    client.getOut().writeObject("ACCEPT " + userName);
                    client.getOut().flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Naming failed!");
            userName = client.getName();
            client.setUserName(userName);
            try {
                client.getOut().writeObject("ACCEPT " + userName);
                client.getOut().flush();
            } catch (IOException ignored){}
        }
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = client.getInput();
            //TODO: get userName, first message from user
            naming(null);
            //notify all that client has joined
            broadcast(new ChatMessage(ChatServer.WELCOME, client.getUserName()));

            ChatMessage incoming;
            while( (incoming = (ChatMessage) in.readObject()) != null) {
                String msgHeader = incoming.getMsgHeader();
                String clientMsg = incoming.getMessage();

                System.out.printf("%s %s %s\n",
                        msgHeader, incoming.getRecipients().toString(), clientMsg);

                if (msgHeader.equals(ChatServer.CHAT)) {
                    ChatMessage msg = new ChatMessage(ChatServer.CHAT, String.format("%s %s", client.getUserName(), clientMsg));
                    broadcast(msg);
                } else if (msgHeader.equals(ChatServer.PCHAT)) {
                    ChatMessage msg = new ChatMessage(ChatServer.PCHAT, String.format("%s %s", client.getUserName(), clientMsg));
                    ArrayList<ClientConnectionData> recipients = new ArrayList<>();
                    for (ClientConnectionData c : clientList) {
                        if (incoming.getRecipients().contains(c.getUserName())) {
                            recipients.add(c);
                        }
                    }
                    whisper(msg, recipients);
                } else if(msgHeader.equals(ChatServer.NAME)){
                    naming(clientMsg);
                } else if (msgHeader.equals(ChatServer.JOIN_ROOM)) {
                    joinRoom(clientMsg);
                } else if (msgHeader.equals(ChatServer.LEAVE_ROOM)) {
                    leaveRoom();
                } else if (msgHeader.equals(ChatServer.LIST)) {
                    listRooms();
                } else if (msgHeader.equals(ChatServer.QUIT)){
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
            client.setRoom("");
            synchronized (clientList) {
                clientList.remove(client);
            }
            System.out.println(client.getName() + " has left.");
            broadcast(new ChatMessage(ChatServer.QUIT, client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ignored) {}
        }
    }
}