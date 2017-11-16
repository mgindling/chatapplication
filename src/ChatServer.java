package src;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

final class ChatServer {
    private static int uniqueId = 0;
    private final List<ClientThread> clients = new ArrayList<>();
    private final int port;
    private final String filterFile;


    //Provided constructor with two parameters (port and badwords text file)
    private ChatServer(int port, String filterFile) {
        this.port = port;
        this.filterFile = filterFile;
    }

    //Provided constructor with one parameter (port)
    private ChatServer(int port) {
        this.port = port;
        this.filterFile = "badwords.txt";
    }

    //Written constructor with no parameters.
    private ChatServer() {
        this.port = 1500;
        this.filterFile = "badwords.txt";
    }

    //This is what starts the src.ChatServer.
    //Right now it just creates the socketServer and adds a new ClientThread to a list to be handled
    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Socket socket = serverSocket.accept();
            Runnable r = new ClientThread(socket, uniqueId++);
            Thread t = new Thread(r);
            clients.add((ClientThread) r);
            t.start();

            //Keeps server up forever. No kill switch as of now (other than force shutdown).
            while (true) {
                socket = serverSocket.accept();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Initializes port (set at 1500; port number can be removed and it will initialize the same) and goes to start().
    public static void main(String[] args) {
        ChatServer server = new ChatServer(1500);
        server.start();
    }

    //This is a private class inside of the src.ChatServer
    //A new thread will be created to run this every time a new client connects.
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;

        private ClientThread(Socket socket, int id) {
            this.id = id;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        //Write message method. Returns false if there's no socket connection; returns true otherwise.
        //Also filters out bad words, provided in text file
        public boolean writeMessage(String message) {
            //Tests if the socket is still open (I think).
            if (socket.isConnected()) {
                try {
                    Date now = new Date(); //Makes a new date object. I feel like there's an easier way to do this.
                    SimpleDateFormat date = new SimpleDateFormat("MM.dd.yyyy");
                    SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss"); //Could be condensed.
                    ChatFilter cf = new ChatFilter("badwords.txt");
                    cf.filter(message);
                    sOutput.writeObject(message + " @ " + date.format(now) + " at " + time.format(now)); //Writes message to the output stream which lets other clients see it.
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                return false;
            }
        }

        //Broadcast method. Sends message to all clients. I think it's concurrent; any client can access it.
        //TODO: (really a question, but I wanted it highlighted in blue) Should this be edited so that if one person tries to send a message while broadcast() is still running, they will both work? i.e. multiple threads or something?
        private void broadcast(String message) {
            for (int c = 0; c < clients.size(); c++) {
                if (!clients.get(c).writeMessage(message)) {
                    writeMessage("Message failed to send to " + clients.get(c).username);
                }
            }
        }

        //Removes the client at the specified slot.
        private void remove(int id) {
            clients.remove(id);
        }

        //"Does the same thing as logging out in the client."
        private void close() {
            try {
                socket.close();
                sOutput.close();
                sInput.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void directMessage(String message, String username) {
            for (int i = 0; i < clients.size(); i++) {
                if (username.equals(clients.get(i).username)) {
                    if (!clients.get(i).writeMessage(message)) {
                        writeMessage("Message failed to send to " + clients.get(i).username);
                    }
                    return; //should only be sending to one person, as usernames are unique
                }
            }
        }

        //This is what the client thread actually runs.
        @Override
        public void run() {

            //Allows the server to respond to messages indefinitely.
            while (true) {
                //Read the username sent to you by client
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
                System.out.println(username + ": Ping");

                //Calls the broadcast method, which makes all clients receive the same message.
                //Should NOT cause every client on the server to logoff in the case of a logout message.
                broadcast(username + ": " + cm.getMsg());
                //If the message is a logout message the server sends a message to the client THAT CALLED IT telling it to close.
                if (cm.getType() == 1) {
                    try {
                        //Program should use the arraylist to search for the client with the same ID and then send that to the method to be deleted.
                        //Not sure if that's what it's doing here.
                        remove(clients.indexOf(id));
                        sOutput.writeObject("end");
                        close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (cm.getType() == 2) {
                    directMessage(cm.getMsg(), cm.getRecipient());
                }
                // SAMPLE CODE: Simply "Pongs" a message back to the client.
                // else {
                //    Send message back to client.
                //    try {
                //        sOutput.writeObject("Pong");
                //    } catch (IOException e) {
                //        e.printStackTrace();
                //    }
                //}
            }
        }
    }
}