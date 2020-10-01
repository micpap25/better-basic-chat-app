import java.io.BufferedReader;

public class ServerListener implements Runnable {
    BufferedReader socketIn;

    public ServerListener(BufferedReader socketIn) {
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
                    case "WELCOME":
                        msg = info[1] + " has joined";
                        break;
                    case "CHAT":
                        msg = info[1] + ": " + slice(info,2,info.length," ");
                        break;
                    case "PCHAT":
                        msg = info[1] + " (private): " + slice(info,2,info.length," ");
                        break;
                    case "EXIT":
                        msg = info[1] + " has left the server";
                        break;
                    case "EXIT_ROOM":
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

