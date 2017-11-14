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

    //Provided constructor with one parameter (port)
    private ChatServer(int port) {
        this.port = port;
    }

    //Written constructor with no parameters.
    private ChatServer() {
        this.port = 1500;
    }

    //This is what starts the ChatServer.
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

    //This is a private class inside of the ChatServer
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
        public boolean writeMessage(String message) {
            //Tests if the socket is still open (I think).
            if (socket.isConnected()) {
                try {
                    Date time = new Date(); //Includes date. Don't think this is how it's supposed to be done.
                    sOutput.writeObject(message + " " + time); //Writes message to the output stream which lets other clients see it.
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                return false;
            }
        }


        //Broadcast method. Sends message to all clients. Needs to be concurrent.
        public void Broadcast(String message) {
            for (int c = 0; c < clients.size(); c++) {
                clients.get(c).writeMessage(message);
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

                //If the message is a logout message the server sends a message to the client telling it to close.
                if (cm.getType() == 1) {
                    try {
                        sOutput.writeObject("end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    //Send message back to client.
                    try {
                        sOutput.writeObject("Pong");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
