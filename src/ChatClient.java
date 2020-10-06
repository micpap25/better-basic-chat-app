import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static PrintWriter out;
    public static final String CHAT = "CHAT";
    public static final String PCHAT = "PCHAT";
    public static final String QUIT = "QUIT";
    public static final String JOIN_ROOM ="JOIN_ROOM";
    public static final String LEAVE_ROOM ="LEAVE_ROOM";
    public static final String LIST ="LIST";

    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);

        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        String name = "";

        String firstMsg;
        while((firstMsg=socketIn.readLine()).equals("SUBMITNAME") || !firstMsg.equals("ACCEPT")){
            System.out.print("Chat sessions has started - enter a user name: ");
            name = userInput.nextLine().trim();
            out.println(name); //out.flush();
        }
        System.out.println("Thank you: " + name);

        // start a thread to listen for server messages
        ServerListener listener = new ServerListener(socketIn);
        Thread t = new Thread(listener);
        t.start();

        String line = "";
        while (!line.toLowerCase().startsWith("/quit")) {
            line = userInput.nextLine().trim();
            String msg = parse(line);
            out.println(msg);
        }
        out.println(QUIT);
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
    }

    public static String parse(String msg){
        String tempMsg = msg.toLowerCase();
        if (tempMsg.length() == 0) {
            return null;
        }
        else if(tempMsg.charAt(0)=='@'){
            return String.format("%s %s",PCHAT,msg.substring("@".length()).trim());
        }
        else if(tempMsg.startsWith("/list")){
            return LIST;
        }
        else if(tempMsg.startsWith("join")){
            return String.format("%s %s",JOIN_ROOM,msg.substring("join".length()).trim());
        }
        else if (tempMsg.startsWith("/leave")){
            return LEAVE_ROOM;
        }
        else{
            return String.format("%s %s",CHAT,msg.trim());
        }
    }
}


