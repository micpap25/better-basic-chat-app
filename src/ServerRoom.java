import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerRoom {
    private ArrayList<ClientConnectionData> participants = new ArrayList<>();
    public final String NAME;
    private ExecutorService pool = Executors.newFixedThreadPool(100);
    private HashMap<ClientConnectionData,ServerClientHandler> map = new HashMap<>();

    public ServerRoom(String NAME) {
        this.NAME = NAME;
    }

    public synchronized void addparticipant(ClientConnectionData client){
        participants.add(client);
        //add pair to hashmap
        //add server client handler to pool
    }
    public synchronized void Removeparticipant(ClientConnectionData client){
        participants.remove(client);
        //remove server client handler from pool using hash map
    }


}
