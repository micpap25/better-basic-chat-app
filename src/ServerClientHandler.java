import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;

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
            client.getOut().println(sb.toString());
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
                    c.getOut().printf("%s %s\n", ChatServer.JOIN_ROOM, client.getUserName());
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
                    c.getOut().printf("%s %s\n", ChatServer.LEAVE_ROOM, client.getUserName());
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
    public void broadcast(String msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    if (c.getUserName()!=null && !c.getUserName().equals(client.getUserName()) && c.getRoom().equals(client.getRoom()))
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
            assert usr.getUserName()!=null;
            System.out.println("Whispering to " + usr.getUserName() + " -- " + msg);
            usr.getOut().println(msg);
        } catch (AssertionError ex) {
            System.out.println("That user does not exist.");
        } catch (Exception ex) {
            System.out.println("whisper caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    public void naming(String userName){
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
                    client.getOut().println("SUBMITNAME");
                    userName = client.getInput().readLine();
                    userName = userName.substring(ChatServer.NAME.length());
                    userName = userName.trim();
                } else {
                    client.setUserName(userName);
                    client.getOut().println("ACCEPT " + userName);
                }
            }
        }catch (IOException e) {
            System.out.println("naming failed");
            userName = client.getName();
            client.setUserName(userName);
            client.getOut().println("ACCEPT " + userName);
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = client.getInput();
            //TODO: get userName, first message from user
            naming(null);
            //notify all that client has joined
            broadcast(String.format("WELCOME %s", client.getUserName()));

            String incoming;
            while( (incoming = in.readLine()) != null) {
                System.out.println(incoming);
                if (incoming.startsWith(ChatServer.CHAT)) {
                    String chat = incoming.substring(4).trim();
                    if (chat.length() > 0) {
                        String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                        broadcast(msg);
                    }
                } else if (incoming.startsWith(ChatServer.PCHAT)) {
                    String chat = incoming.substring(5).trim();
                    String name = chat.split(" ")[0];
                    String content = chat.substring(name.length()).trim();
                    if (content.length() > 0) {
                        String msg = String.format("PCHAT %s %s", client.getUserName(), content);
                        for (ClientConnectionData c : clientList) {
                            if (c.getUserName().equals(name)) {
                                whisper(msg, c);
                                break;
                            }
                        }
                    }
                } else if(incoming.startsWith(ChatServer.NAME)){
                    naming(incoming.substring(ChatServer.NAME.length()).trim());
                } else if (incoming.startsWith(ChatServer.JOIN_ROOM)) {
                    String room = incoming.substring(9).trim();
                    if (room.length() > 0) {
                        joinRoom(room);
                    }
                } else if (incoming.startsWith(ChatServer.LEAVE_ROOM)) {
                    leaveRoom();
                } else if (incoming.startsWith(ChatServer.LIST)) {
                    listRooms();
                } else if (incoming.startsWith(ChatServer.QUIT)){
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
            broadcast(String.format("%s %s",ChatServer.QUIT, client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ignored) {}
        }
    }
}