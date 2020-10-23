import java.io.ObjectInputStream;
import java.lang.invoke.SerializedLambda;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ServerListener implements Runnable {

    ObjectInputStream socketIn;
    AtomicBoolean naming;
    Consumer<String> l;
    public volatile boolean appRunning = true;
    public ServerListener(ObjectInputStream socketIn, AtomicBoolean naming, Consumer<String> l) {
        this.naming = naming;
        this.socketIn = socketIn;
        this.l = l;
    }

    private String slice(String[] array, int start, int end, String insert){
        StringBuilder message = new StringBuilder();
        for (int i = start; i < end || i<array.length; i++) {
            message.append(insert);
            message.append(array[i]);
        }
        return message.toString().trim();
    }

    @Override
    public void run() {
        try {
            ChatMessage incoming;
            while((incoming = (ChatMessage) socketIn.readObject()) != null) {
                String msg = "";
                //System.out.println(incoming);
                String info = incoming.getMsgHeader();
                String[] body = null;
                if(incoming.getMessage()!=null){
                    body = incoming.getMessage().split(" ");
                    for (int i =0; i<body.length;i++) {
                        body[i]=body[i].strip();
                    }
                }
    
                assert body != null;

                switch (info) {
                    case "SUBMITNAME":
                        msg = "Please choose a valid username";
                        break;
                    case "ACCEPT":
                        msg = "Username set as: "+ body[0];
                        naming.set(false);
                        break;
                    case "WELCOME":
                        msg = body[0] + " has joined";
                        break;
                    case ChatServer.CHAT:
                        msg = body[0] + ": " + slice(body,1,body.length," ");
                        break;
                    case ChatServer.PCHAT:
                        msg = body[0] + " (private): " + slice(body,1,body.length," ");
                        break;
                    case ChatServer.QUIT:
                        msg = body[0] + " has left the server";
                        break;
                    case ChatServer.LIST:
                        StringBuilder k = new StringBuilder("\n------ACTIVE ROOMS------");
                        k.append("\n");
                        for (int i = 1; i < body.length ; i++) {
                            k.append(body[i]);
                            k.append("\n");
                        }
                        msg = k.toString();
                        break;
                    case ChatServer.ROSTER:
                        String[] roster = incoming.getMessage().split("/");
                        String[] room = roster[0].split(" ");
                        String[] server = {""};
                        if(roster.length>1) {
                            server = roster[1].split(" ");
                        }
                        System.out.println("---------Roster---------");
                        System.out.printf("%-30s %-30s\n","ROOM",roster.length>1?"SERVER":"");
                        for (int i = 0; i < server.length || i <room.length ; i++) {
                            String serveruser ="";
                            String roomuser ="";
                            if(i<server.length) {
                                serveruser = server[i];
                            }
                            if(i<room.length) {
                                roomuser = room[i];
                            }
                            System.out.printf("%-30s %-30s\n",roomuser.trim(),serveruser.trim());
                        }
                        break;
                    case ChatServer.JOIN_ROOM:
                        msg = body[0] + " has joined the room";
                        break;
                    case ChatServer.LEAVE_ROOM:
                        msg = body[0] + " has left the room";
                        break;
                }

                l.accept(msg);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Exception caught in listener - " + ex);
        } finally{
            System.out.println("Client Listener exiting");
        }
    }
}

