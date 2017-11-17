package src; //Probably should be removed at the end of programming.

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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
        ChatServer server;

        switch (args.length) {
            case 0: server = new ChatServer();
                    server.start();
                    break;
            case 1: server = new ChatServer(Integer.parseInt(args[0]));
                    server.start();
                    break;
            case 2: server = new ChatServer(Integer.parseInt(args[0]), args[1]);
                    server.start();
                    break;
            default:server = new ChatServer(Integer.parseInt(args[0]), args[1]);
                    server.start();
                    break;
        }

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

                //Checks to see if the username is taken
                for (int u = 0; u < clients.size(); u++) {
                    if (clients.get(u).username.equals(username)) {
                        username = null;
                    }
                }

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
                    message = cf.filter(message);
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

        public List<ClientThread> getClients() {
            return clients;
        }

        //Broadcast method. Sends message to all clients. I think it's concurrent; any client can access it.
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

        private void list(String username) {
            ArrayList <ClientThread> list = new ArrayList<>();

            for (int i = 0; i < clients.size(); i++) {
                if (!clients.get(i).username.equals(username)) {
                    list.add(clients.get(i));
                }
            }

            if (list.size() == 0) {
                writeMessage("Nobody else is online");
            } else {
                writeMessage(list.toString());
            }
        }

        //This is what the client thread actually runs.
        @Override
        public void run() {

            //"Killed before you began--and you say I'm heartless?"
            if (username == null) {
                remove(id);
                close();
                return;
            }

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
                try {
                    if (cm.getType() == 0) {
                        broadcast(username + ": " + cm.getMsg());
                    }
                } catch (NullPointerException e) {
                    System.out.println("Client " + username + " has dropped.");
                    return;
                }

                //If the message is a logout message the server sends a message to the client THAT CALLED IT telling it to close.
                if (cm.getType() == 1) {
                    try {
                        //Program should use the arraylist to search for the client with the same ID and then send that to the method to be deleted.
                        //Not sure if that's what it's doing here.
                        remove(id);
                        broadcast(username + "has logged out.");

                        /*try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*/

                        sOutput.writeObject("end");
                        close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Boolean variable = false;
                if (cm.getType() == 2) {
                    if (cm.getMsg().toUpperCase().equals("ERROR")) {
                        directMessage("Please add a message to your direct message", username);
                    } else {
                        if (cm.getRecipient().toUpperCase().equals(username)) {
                            directMessage("You can't direct message yourself.", username);
                        } else {
                            for (int i = 0; i < clients.size(); i++) {
                                if (clients.get(i).username.equals(cm.getRecipient())) {
                                    directMessage(cm.getMsg(), cm.getRecipient());
                                    variable = true;
                                }
                            }
                            if (!variable) {
                                directMessage("That person is not online or does not exist.", username);
                            }
                        }
                    }
                }

                if (cm.getType() == 3) {
                    list(username);
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
