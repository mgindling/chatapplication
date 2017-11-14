import java.io.Serializable;

final class ChatMessage implements Serializable {
    private static final long serialVersionUID = 6898543889087L;
    private String msg;
    private int type;

    //Constructor for ChatMessage
    //msg is the message to be displayed; type is an integer representing what kind of message msg is.
    public ChatMessage(String msg, int type) {
        this.msg = msg;
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public int getType() {
        return type;
    }

}
