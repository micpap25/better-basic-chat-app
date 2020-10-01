import java.io.BufferedReader;

public class ServerListener implements Runnable {
    BufferedReader socketIn;

    public ServerListener(BufferedReader socketIn) {
        this.socketIn = socketIn;
    }

    @Override
    public void run() {
        try {
            String incoming = "";

            while((incoming = socketIn.readLine()) != null) {
                String Msg = "";
                System.out.println(incoming);
                String[] info = incoming.split(" ");
                if (info[0].equals("WELCOME")){
                    Msg = info[1]+" has joined";
                }
                else if(info[0].equals("CHAT")){
                    Msg = info[1]+": "+info[2];
                }
                else if(info[0].equals("PCHAT")){
                    Msg = info[1]+" (private): "+info[2];
                }
                else if(info[0].equals("EXIT")){
                    Msg = info[1]+" has left the server";
                }
                else if(info[0].equals("EXIT_ROOM")){
                    Msg = info[1]+" has left the room";
                }

                System.out.println(Msg);
            }
        } catch (Exception ex) {
            System.out.println("Exception caught in listener - " + ex);
        } finally{
            System.out.println("Client Listener exiting");
        }
    }
}

