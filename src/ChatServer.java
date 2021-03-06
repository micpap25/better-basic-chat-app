import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    public static final int PORT = 54321;
    private static final ArrayList<ClientConnectionData> clientList = new ArrayList<>();

    public static final String WELCOME = "WELCOME";
    public static final String SUBMITNAME = "SUBMITNAME";
    public static final String ACCEPT = "ACCEPT";
    public static final String CHAT = "CHAT";
    public static final String PCHAT = "PCHAT";
    public static final String QUIT = "QUIT";
    public static final String JOIN_ROOM = "JOIN_ROOM";
    public static final String LEAVE_ROOM = "LEAVE_ROOM";
    public static final String LIST = "LIST";
    public static final String NAME = "NAME";
    public static final String ROSTER = "ROSTER";


    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(100);

        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Chat Server started.");
            System.out.println("Local IP: "
                    + Inet4Address.getLocalHost().getHostAddress());
            System.out.println("Local Port: " + serverSocket.getLocalPort());
            //set up 1 room to start
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Connected to %s:%d on local port %d\n",
                        socket.getInetAddress(), socket.getPort(), socket.getLocalPort());


                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    String name = socket.getInetAddress().getHostName();

                    ClientConnectionData client = new ClientConnectionData(socket, in, out, name);
                    synchronized (clientList) {
                        clientList.add(client);
                    }
                    
                    System.out.println("added client " + name);

                    //handle client business in another thread
                    pool.execute(new ServerClientHandler(client, clientList));
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

            }
        } 
    }
}
