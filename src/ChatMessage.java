import java.io.Serializable;
import java.util.ArrayList;

public class ChatMessage implements Serializable {
    long serialVersionUID = 1L;

    private String msgHeader;
    private String message;
    private ArrayList<String> recipients;

    public ChatMessage(String msgHeader) {
        this.msgHeader = msgHeader;
    }

    public ChatMessage(String msgHeader, String message) {
        this.msgHeader = msgHeader;
        this.message = message;
    }

    public ChatMessage(String msgHeader, String message, ArrayList<String> recipients) {
        this.msgHeader = msgHeader;
        this.message = message;
        this.recipients = recipients;
    }
}
