import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
/**
 * For Java 8, javafx is installed with the JRE. You can run this program normally.
 * For Java 9+, you must install JavaFX separately: https://openjfx.io/openjfx-docs/
 * If you set up an environment variable called PATH_TO_FX where JavaFX is installed
 * you can compile this program with:
 *  Mac/Linux:
 *      > javac --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows CMD:
 *      > javac --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows Powershell:
 *      > javac --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *
 * Then, run with:
 *
 *  Mac/Linux:
 *      > java --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient
 *  Windows CMD:
 *      > java --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui.ChatGuiClient
 *  Windows Powershell:
 *      > java --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient
 *
 * There are ways to add JavaFX to your to your IDE so the compile and run process is streamlined.
 * That process is a little messy for VSCode; it is easiest to do it via the command line there.
 * However, you should open  Explorer -> Java Projects and add to Referenced Libraries the javafx .jar files
 * to have the syntax coloring and autocomplete work for JavaFX
 */

class ServerInfo {
    public final String serverAddress;
    public final int serverPort;

    public ServerInfo(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
}

public class ChatGuiClient extends Application {
    private static Socket socket;
    private static ObjectInputStream in;
    private static ObjectOutputStream out;
    private AtomicBoolean naming = new AtomicBoolean(true);

    private Stage stage;
    private TextArea messageArea;
    private TextField textInput;
    private Button sendButton;

    private ServerInfo serverInfo;
    //volatile keyword makes individual reads/writes of the variable atomic
    // Since username is accessed from multiple threads, atomicity is important
    private volatile String username = "";
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //If ip and port provided as command line arguments, use them
        List<String> args = getParameters().getUnnamed();
        if (args.size() == 2){
            this.serverInfo = new ServerInfo(args.get(0), Integer.parseInt(args.get(1)));
        }
        else {
            //otherwise, use a Dialog.
            Optional<ServerInfo> info = getServerIpAndPort();
            if (info.isPresent()) {
                this.serverInfo = info.get();
            }
            else{
                Platform.exit();
                return;
            }
        }

        this.stage = primaryStage;
        BorderPane borderPane = new BorderPane();

        messageArea = new TextArea();
        messageArea.setWrapText(true);
        messageArea.setEditable(false);
        borderPane.setCenter(messageArea);

        //At first, can't send messages - wait for WELCOME!
        textInput = new TextField();
        textInput.setEditable(true);
        textInput.setOnAction(e -> sendMessage());
        sendButton = new Button("Send");
        sendButton.setDisable(false);
        sendButton.setOnAction(e -> sendMessage());

        HBox hbox = new HBox();
        hbox.getChildren().addAll(new Label("Message: "), textInput, sendButton);
        HBox.setHgrow(textInput, Priority.ALWAYS);
        borderPane.setBottom(hbox);

        Scene scene = new Scene(borderPane, 400, 500);
        stage.setTitle("Chat Client");
        stage.setScene(scene);
        stage.show();


        socket = new Socket(serverInfo.serverAddress, serverInfo.serverPort);
        System.out.println("set up socket");
        out = new ObjectOutputStream(socket.getOutputStream());
        System.out.println("set up output");
        in = new ObjectInputStream(socket.getInputStream());
        System.out.println("set up input");

        ServerListener socketListener = new ServerListener(in, naming);

        //Handle GUI closed event
        stage.setOnCloseRequest(e -> {
            try {
            out.writeObject(new ChatMessage(ChatServer.QUIT));
            out.flush();
            socketListener.appRunning = false;
                socket.close();
            } catch (IOException ex) {}
        });

        new Thread(socketListener).start();
    }

    private void sendMessage() {
        String message = textInput.getText().trim();
        if (message.length() == 0)
            return;
        textInput.clear();
        try {
            ChatMessage msg = parse(message);
            if(!message.startsWith("/")) {
                Platform.runLater(() -> {
                    messageArea.appendText(username + ": "+message + "\n");
                });
            }
            out.writeObject(msg);
            out.flush();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private Optional<ServerInfo> getServerIpAndPort() {
        // In a more polished product, we probably would have the ip /port hardcoded
        // But this a great way to demonstrate making a custom dialog
        // Based on Custom Login Dialog from https://code.makery.ch/blog/javafx-dialogs-official/

        // Create a custom dialog for server ip / port
        Dialog<ServerInfo> getServerDialog = new Dialog<>();
        getServerDialog.setTitle("Enter Server Info");
        getServerDialog.setHeaderText("Enter your server's IP address and port: ");

        // Set the button types.
        ButtonType connectButtonType = new ButtonType("Connect", ButtonData.OK_DONE);
        getServerDialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Create the ip and port labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField ipAddress = new TextField();
        ipAddress.setPromptText("e.g. localhost, 127.0.0.1");
        grid.add(new Label("IP Address:"), 0, 0);
        grid.add(ipAddress, 1, 0);

        TextField port = new TextField();
        port.setPromptText("e.g. 54321");
        grid.add(new Label("Port number:"), 0, 1);
        grid.add(port, 1, 1);


        // Enable/Disable connect button depending on whether a username was entered.
        Node connectButton = getServerDialog.getDialogPane().lookupButton(connectButtonType);
        connectButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        ipAddress.textProperty().addListener((observable, oldValue, newValue) -> {
            connectButton.setDisable(newValue.trim().isEmpty());
        });

        port.textProperty().addListener((observable, oldValue, newValue) -> {
            // Only allow numeric values
            if (! newValue.matches("\\d*"))
                port.setText(newValue.replaceAll("[^\\d]", ""));

            connectButton.setDisable(newValue.trim().isEmpty());
        });

        getServerDialog.getDialogPane().setContent(grid);

        // Request focus on the username field by default.
        Platform.runLater(() -> ipAddress.requestFocus());


        // Convert the result to a ServerInfo object when the login button is clicked.
        getServerDialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new ServerInfo(ipAddress.getText(), Integer.parseInt(port.getText()));
            }
            return null;
        });

        return getServerDialog.showAndWait();
    }

    class ServerListener implements Runnable {

        ObjectInputStream socketIn;
        AtomicBoolean naming;
        public volatile boolean appRunning = true;
        public ServerListener(ObjectInputStream socketIn, AtomicBoolean naming) {
            this.naming = naming;
            this.socketIn = socketIn;
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
                            msg = "Username set as: " + body[0];
                            username = body[0];
                            naming.set(false);
                            Platform.runLater(() -> {
                                stage.setTitle("Chatter - " + username);
                            });
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

                    String finalMsg = msg;
                    Platform.runLater(() -> {
                        messageArea.appendText(finalMsg + "\n");
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Exception caught in listener - " + ex);
            } finally{
                System.out.println("Client Listener exiting");
            }
        }



        /*public void run() {
            try {
                // Set up the socket for the Gui
                socket = new Socket(serverInfo.serverAddress, serverInfo.serverPort);
                in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());

                appRunning = true;

                //handle all kinds of incoming messages
                String incoming = "";
                while (appRunning && (incoming = in.readLine()) != null) {
                    if (incoming.startsWith("WELCOME")) {
                        String user = incoming.substring(8);
                        //got welcomed? Now you can send messages!
                        if (user.equals(username)) {
                            Platform.runLater(() -> {
                                stage.setTitle("Chatter - " + username);
                                textInput.setEditable(true);
                                sendButton.setDisable(false);
                                messageArea.appendText("Welcome to the chatroom, " + username + "!\n");
                            });
                        }
                        else {
                            Platform.runLater(() -> {
                                messageArea.appendText(user + " has joined the chatroom.\n");
                            });
                        }

                    } else if (incoming.startsWith("CHAT")) {
                        int split = incoming.indexOf(" ", 5);
                        String user = incoming.substring(5, split);
                        String msg = incoming.substring(split + 1);

                        Platform.runLater(() -> {
                            messageArea.appendText(user + ": " + msg + "\n");
                        });
                    } else if (incoming.startsWith("EXIT")) { // // TODO : this feels like a workaround to end the listener thread
                        String user = incoming.substring(8);
                        Platform.runLater(() -> {
                            messageArea.appendText(user + "has left the chatroom.\n");
                        });
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (Exception e) {
                if (appRunning)
                    e.printStackTrace();
            }
            finally {
                Platform.runLater(() -> {
                    stage.close();
                });
                try {
                    if (socket != null)
                        socket.close();
                }
                catch (IOException e){
                }
            }
        } */
    }


    public ChatMessage parse(String msg){
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
