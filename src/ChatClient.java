import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient {
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream socketIn;
    private static AtomicBoolean naming = new AtomicBoolean(true);
    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);

        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);


        out = new ObjectOutputStream(socket.getOutputStream());
        socketIn = new ObjectInputStream(socket.getInputStream());


        // start a thread to listen for server messages
        ServerListener listener = new ServerListener(socketIn, naming, System.out::println);
        Thread t = new Thread(listener);
        t.start();

        String line = "";
        while (!line.toLowerCase().startsWith("/quit")) {
            line = userInput.nextLine().trim();
            ChatMessage msg = parse(line);
            if(msg==null){
                continue;
            }
            out.writeObject(msg);
            out.flush();
        }
        out.writeObject(new ChatMessage(ChatServer.QUIT));
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
    }

    public static ChatMessage parse(String msg){
        String tempMsg = msg.toLowerCase();
        
        if(msg.length() == 0){
            return null;
        }

        if (naming.get() || tempMsg.startsWith("/name")) {
            naming.set(true);
            msg = msg.replace("/name","").trim();
            return new ChatMessage(ChatServer.NAME, msg);
        }

        if (tempMsg.charAt(0)=='@') {
            String[] temp = tempMsg.split(" ");
            ArrayList<String> names = new ArrayList<>();
            int i;
            for (i = 0; i < temp.length ; i++) {
                if(temp[i].startsWith("@")){
                    names.add(temp[i].substring("@".length()).trim());
                }
                else{
                    break;
                }
            }
            StringBuilder newmsg = new StringBuilder();
            for (; i < temp.length ; i++) {
                newmsg.append(temp[i]);
                newmsg.append(" ");
            }

            return new ChatMessage(ChatServer.PCHAT,newmsg.toString().trim(),names);
        }
        else if (tempMsg.startsWith("/list")) {
            return new ChatMessage(ChatServer.LIST);
        }
        else if(tempMsg.startsWith("/join")) {
            return new ChatMessage(ChatServer.JOIN_ROOM, msg.substring("/join".length()).trim());
        }
        else if (tempMsg.startsWith("/leave")) {
            return new ChatMessage(ChatServer.LEAVE_ROOM);
        }
        else if(tempMsg.startsWith("/whoishere")){
            return new ChatMessage(ChatServer.ROSTER);
        }
        else {
            return new ChatMessage(ChatServer.CHAT, msg.trim());
        }
    }
}


