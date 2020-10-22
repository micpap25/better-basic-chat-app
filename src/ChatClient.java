import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static PrintWriter out;
    private static AtomicBoolean naming = new AtomicBoolean(true);
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

        // start a thread to listen for server messages
        ServerListener listener = new ServerListener(socketIn, naming);
        Thread t = new Thread(listener);
        t.start();

        String line = "";
        while (!line.toLowerCase().startsWith("/quit")) {
            line = userInput.nextLine().trim();
            String msg = parse(line);
            out.println(msg);
        }
        out.println(ChatServer.QUIT);
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
    }

    public static String parse(String msg){
        String tempMsg = msg.toLowerCase();
        
        if(msg.length() == 0){
            return null;
        }

        if (naming.get() || tempMsg.startsWith("/name")) {
            naming.set(true);
            msg = msg.replace("/name","").trim();
            return String.format("%s %s", ChatServer.NAME,msg);
        }

        if (tempMsg.charAt(0)=='@') {
            return String.format("%s %s", ChatServer.PCHAT, msg.substring("@".length()).trim());
        }
        else if (tempMsg.startsWith("/list")) {
            return ChatServer.LIST;
        }
        else if(tempMsg.startsWith("/join")) {
            return String.format("%s %s", ChatServer.JOIN_ROOM, msg.substring("/join".length()).trim());
        }
        else if (tempMsg.startsWith("/leave")) {
            return ChatServer.LEAVE_ROOM;
        }
        else {
            return String.format("%s %s", ChatServer.CHAT, msg.trim());
        }
    }
}


