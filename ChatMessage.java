import java.io.Serializable;

final class ChatMessage implements Serializable {
    private static final long serialVersionUID = 6898543889087L;
    private String msg;
    private int type;
    private String recipient;

    //Constructor for ChatMessage
    //msg is the message to be displayed; type is an integer representing what kind of message msg is. 0 = general message, 1 = logout message, 2 = direct message
    public ChatMessage(String msg, int type, String recipient) {
        this.msg = msg;
        this.type = type;

        if (type == 2) {
            this.recipient = recipient;
        } else {
            this.recipient = null; //might this cause errors? Should it instead be 'this.recipient = "";'  ?
        }
    }

    public String getMsg() {
        return msg;
    }

    public int getType() {
        return type;
    }

    public String getRecipient() {
        return recipient;
    }
}
