import java.io.BufferedReader;
import java.io.ObjectInputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerListener implements Runnable {

    ObjectInputStream socketIn;
    AtomicBoolean naming;
    public ServerListener(ObjectInputStream socketIn, AtomicBoolean naming) {
        this.naming = naming;
        this.socketIn = socketIn;
    }

    private String slice(String[] array,int start,int end,String insert){
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
                String[] body = incoming.getMessage().split(" ");
                switch (info) {
                    case "SUBMITNAME":
                        msg = "Please choose a valid username";
                        break;
                    case "ACCEPT":
                        msg = "Username set as: " + body[0];
                        naming.set(false);
                        break;
                    case "WELCOME":
                        msg = body[0] + " has joined";
                        break;
                    case ChatServer.CHAT:
                        msg = body[0] + ": " + slice(body,1,body.length," ");
                        break;
                    case ChatServer.PCHAT:
                        msg = body[0] + " (private): " + slice(body,1,body.length," ");;
                        break;
                    case ChatServer.QUIT:
                        msg = body[0] + " has left the server";
                        break;
                    case ChatServer.LIST:
                        StringBuilder k = new StringBuilder("\n-----ACTIVE ROOMS------");
                        k.append("\n");
                        for (int i = 1; i < body.length ; i++) {
                            k.append(body[i]);
                            k.append("\n");
                        }
                        msg = k.toString();
                        break;
                    case ChatServer.JOIN_ROOM:
                        msg = body[0] + " has joined the room";
                        break;
                    case ChatServer.LEAVE_ROOM:
                        msg = body[0] + " has left the room";
                        break;
                }

                System.out.println(msg);
            }
        } catch (Exception ex) {
            System.out.println("Exception caught in listener - " + ex);
            ex.printStackTrace();
        } finally{
            System.out.println("Client Listener exiting");
        }
    }
}

