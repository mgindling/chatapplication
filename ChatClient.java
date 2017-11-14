import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

final class ChatClient {
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    private Socket socket;

    private final String server;
    private final String username;
    private final int port;

    //Provided constructor for all three parameters.
    private ChatClient(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    //Written constructor for two parameters (port + username).
    private ChatClient(int port, String username) {
        this.port = port;
        this.username = username;

        this.server = "localhost"; //Default IP
    }

    //Written constructor for one parameter (username).
    private ChatClient(String username) {
        this.username = username;

        this.server = "localhost"; //Default IP
        this.port = 1500; //Default port
    }

    //Written constructor for no parameters.
    private ChatClient() {
        this.server = "localhost";
        this.port = 1500;
        this.username = "Anonymous";
    }

    //Starts the chat client.
    private boolean start() {

        //Create a socket
        try {
            socket = new Socket(server, port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Create your input and output streams
        try {
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //This thread will listen from the server for incoming messages
        Runnable r = new ListenFromServer();
        Thread t = new Thread(r);
        t.start();

        //After starting, send the clients username to the server.
        try {
            sOutput.writeObject(username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    //This method is used to send a ChatMessage Objects to the server
    private void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        /*
        * Client can initialize with:
        *   The server, port, and username.
        *   The port and username.
        *   Just the username.
        *   No parameters.
        */

        //Create your client and start it.
        ChatClient client = new ChatClient("localhost", 1500, "CS 180 Student");
        client.start();

        //Used to read user input from terminal (believe that's what's supposed to be happening here).
        Scanner input = new Scanner(System.in);

        //Allows the client to send messages until "/LOGOUT" is entered.
        String message;
        do {
            //Reads user input and stores it in the variable message.
            message = input.next();

            //Checks to see whether user input is a normal message or a logout one.
            if (message.toUpperCase().equals("/LOGOUT")) {
                client.sendMessage(new ChatMessage(message, 1));
                return;
                //Input and output cannot be closed from static context; I believe that the if statement in run handles it now.
            } else {
                client.sendMessage(new ChatMessage(message, 0));
            }

        } while (!message.equals("/LOGOUT"));
        //SAMPLE CODE: Sends an empty message to the server
        //client.sendMessage(new ChatMessage());
    }


    /*
     * This is a private class inside of the ChatClient
     * It will be responsible for listening for messages from the ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     */
    private final class ListenFromServer implements Runnable {
        public void run() {

            //Allows client to persist. No kill switch besides force shutdown.
            while (true) {
                try {
                    String msg = (String) sInput.readObject();

                    //If the server sends a kill String (right now it's end), everything closes and the client ends. (Client actually ends in main.)
                    if (msg.equals("end")) {
                        sOutput.close();
                        sInput.close();
                        socket.close();
                        return;
                    }
                    else {
                        System.out.print(msg);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
