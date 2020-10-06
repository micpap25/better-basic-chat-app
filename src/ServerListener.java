import java.io.BufferedReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerListener implements Runnable {

    BufferedReader socketIn;
    AtomicBoolean naming;
    public ServerListener(BufferedReader socketIn, AtomicBoolean naming) {
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
            String incoming;
            while((incoming = socketIn.readLine()) != null) {
                String msg = "";
                //System.out.println(incoming);
                String[] info = incoming.split(" ");
                switch (info[0]) {
                    case "SUBMITNAME":
                        msg = "Please choose a valid username";
                        break;
                    case "ACCEPT":
                        msg = "Username set as: "+slice(info,1,info.length," ");
                        naming.set(false);
                        break;
                    case "WELCOME":
                        msg = info[1] + " has joined";
                        break;
                    case ChatServer.CHAT:
                        msg = info[1] + ": " + slice(info,2,info.length," ");
                        break;
                    case ChatServer.PCHAT:
                        msg = info[1] + " (private): " + slice(info,2,info.length," ");
                        break;
                    case ChatServer.QUIT:
                        msg = info[1] + " has left the server";
                        break;
                    case ChatServer.LIST:
                        StringBuilder k = new StringBuilder("\n-----ACTIVE ROOMS------");
                        k.append("\n");
                        for (int i = 1; i < info.length ; i++) {
                            k.append(info[i]);
                            k.append("\n");
                        }
                        msg = k.toString();
                        break;
                    case ChatServer.JOIN_ROOM:
                        msg = info[1] + " has joined the room";
                        break;
                    case ChatServer.LEAVE_ROOM:
                        msg = info[1] + " has left the room";
                        break;
                }

                System.out.println(msg);
            }
        } catch (Exception ex) {
            System.out.println("Exception caught in listener - " + ex);
        } finally{
            System.out.println("Client Listener exiting");
        }
    }
}

