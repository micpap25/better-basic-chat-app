import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static PrintWriter out;
    public static final String NAME = "NAME";
    public static final String CHAT = "CHAT";
    public static final String PCHAT = "PCHAT";
    public static final String QUIT = "QUIT";
    public static final String MAKE_ROOM = "MAKE_ROOM";
    public static final String JOIN_ROOM ="JOIN_ROOM";
    public static final String LEAVE_ROOM ="LEAVE_ROOM";
    public static final String LIST_ROOM="LIST_ROOM";
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


        System.out.print("Chat sessions has started - enter a user name: ");
        do {
            String name = userInput.nextLine().trim();
            out.println(name); //out.flush();
        }while(socketIn.readLine().equals("SUBMITNAME"));
        // start a thread to listen for server messages
        ServerListener listener = new ServerListener(socketIn);
        Thread t = new Thread(listener);
        t.start();



        String line = userInput.nextLine().trim();
        while (!line.toLowerCase().startsWith("/quit")) {
            line = line.trim();
            String prefix = parse(line);
            String body = line.replaceAll(prefix,"");
            String msg = String.format("%s %s",prefix, body);
            out.println(msg);
            line = userInput.nextLine().trim();
        }
        out.println(QUIT);
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
    }
    public static String parse(String msg){
        if(msg.charAt(0)=='@'){
            return PCHAT;
        }
        if(msg.startsWith("/list")){
            return LIST_ROOM;
        }
        if(msg.startsWith("join")){
            return JOIN_ROOM;
        }
        if (msg.startsWith("/leave")){
            return LEAVE_ROOM;
        }
        if(msg.startsWith("/make")){
            return MAKE_ROOM;
        }
        else{
            return CHAT;
        }


    }
}


