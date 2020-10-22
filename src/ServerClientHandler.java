import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;

//TODO: Change all the "writeObjects" to actually send objects
//Make sure you trim stuff before sending it

//New exception to be aware of is ClassNotFound exceptions with readObject()

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
            StringBuilder roomList = new StringBuilder();
            for (String s : rooms) {
                roomList.append(s.trim()).append(" ");
            }
            client.getOut().writeObject(new ChatMessage(ChatServer.LIST, roomList.toString()));
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
                    //TODO: check if we need this extra newline?
                    c.getOut().writeObject(new ChatMessage(ChatServer.JOIN_ROOM, client.getUserName() + "\n"));
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
                    //TODO: check if we need this extra newline?
                    c.getOut().writeObject(new ChatMessage(ChatServer.LEAVE_ROOM, client.getUserName() + "\n"));
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
     * If the client is in a room, broadcasts to those in the room instead.
     */
    public void broadcast(ChatMessage msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    if (c.getUserName() != null && !c.getUserName().equals(client.getUserName()) && c.getRoom().equals(client.getRoom())) {
                        c.getOut().writeObject(msg);
                        client.getOut().flush();
                    }
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
                    client.getOut().writeObject(new ChatMessage(ChatServer.SUBMITNAME));
                    client.getOut().flush();
                    ChatMessage nameSubmission = (ChatMessage) client.getInput().readObject();
                    userName = nameSubmission.getMessage();
                } else {
                    client.setUserName(userName);
                    client.getOut().writeObject(new ChatMessage(ChatServer.ACCEPT, userName));
                    client.getOut().flush();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
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
            naming(null);
            //notify all that client has joined
            broadcast(new ChatMessage(ChatServer.WELCOME, client.getUserName()));

            ChatMessage incoming;
            active:
            while( (incoming = (ChatMessage) in.readObject()) != null) {
                String msgHeader = incoming.getMsgHeader();
                String clientMsg = incoming.getMessage();

                System.out.printf("%s %s\n",
                        msgHeader, clientMsg);

                switch (msgHeader) {
                    case ChatServer.CHAT: {
                        ChatMessage msg = new ChatMessage(ChatServer.CHAT, String.format("%s %s", client.getUserName(), clientMsg));
                        broadcast(msg);
                        break;
                    }
                    case ChatServer.PCHAT: {
                        ChatMessage msg = new ChatMessage(ChatServer.PCHAT, String.format("%s %s", client.getUserName(), clientMsg));
                        ArrayList<ClientConnectionData> recipients = new ArrayList<>();
                        for (ClientConnectionData c : clientList) {
                            if (incoming.getRecipients().contains(c.getUserName())) {
                                recipients.add(c);
                            }
                        }
                        whisper(msg, recipients);
                        break;
                    }
                    case ChatServer.NAME:
                        naming(clientMsg);
                        break;
                    case ChatServer.JOIN_ROOM:
                        joinRoom(clientMsg);
                        break;
                    case ChatServer.LEAVE_ROOM:
                        leaveRoom();
                        break;
                    case ChatServer.LIST:
                        listRooms();
                        break;
                    case ChatServer.QUIT:
                        break active;
                }
            }
        } catch (Exception ex) {
            if (ex instanceof SocketException) {
                System.out.println("Caught socket ex for " +
                        client.getName());
            } else {
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