package src; //Probably should be removed at the end of programming.

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
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

        if (username.contains(" ")) {
            username = username.replaceAll(" ", "");
        }
        this.username = username;
    }

    //Written constructor for two parameters (port + username).
    private ChatClient(int port, String username) {
        this.port = port;

        if (username.contains(" ")) {
            username = username.replaceAll(" ", "");
        }
        this.username = username;

        this.server = "localhost"; //Default IP
    }

    //Written constructor for one parameter (username).
    private ChatClient(String username) {
        if (username.contains(" ")) {
            username = username.replaceAll(" ", "");
        }
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

    //This method is used to send a src.ChatMessage Objects to the server
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
        ChatClient client;
        ChatClient client2;
        client = new ChatClient(args[2], Integer.parseInt(args[1]), args[0]);
        client2 = new ChatClient();

        try {
            client.start();
        } catch (NullPointerException e) { //If the server is not available, the client just ends.
            System.out.println("Server not available!");
            return;
        }


        //Instructions for the user (and us).
        System.out.println("Welcome to the CS18000 chat application!");
        System.out.println("To send a general message, simply type your message!");
        System.out.println("To list the clients currently on the server, type '/list'");
        System.out.println("To send a private message, type '/msg username message'");
        System.out.println("To logout, type '/logout'");
        System.out.println();

        //Used to read user input from terminal (believe that's what's supposed to be happening here).
        Scanner input = new Scanner(System.in);

        //Allows the client to send messages until "/LOGOUT" is entered.
        String message;
        do {

            //Makes it look nice ("if it doesn't look nice, it doesn't work.")
            //It doesn't work right now. The code input.hasNextLine() works opposite (i.e. only displays "Chat: " when the server outputs something. This does nothing (???). Just having it print out "Chat:" prints it out every line.
            // if (!input.hasNextLine()) {
            //     System.out.print("Chat: ");
            // }

            //Reads user input and stores it in the variable message.
            message = input.nextLine();

            if (client.socket == null) {
                return;
            }

            //Checks to see whether user input is a normal message or a logout one.
            if (message.toUpperCase().equals("/LOGOUT")) { //Logs out user
                client.sendMessage(new ChatMessage(message, 1, null));
                return;
                //Input and output cannot be closed from static context; I believe that the if statement in run handles it now.
            } else if (message.toUpperCase().equals("/LIST")) { //Prints out list
                client.sendMessage(new ChatMessage("This will be replaced by the list", 3, client.username));
            } else if (message.length() > 5 && message.substring(0, 5).toUpperCase().equals("/LIST") && !message.substring(5, message.length()).equals(" ")) {
                client.sendMessage(new ChatMessage("Please type exactly /list when asking for a list", 2, client.username));
            } else if (message.length() > 4) { //avoiding exceptions (ex: sending the message "hi" should move on to the else statement)
                if (message.substring(0, 5).toUpperCase().equals("/MSG ")) { //Sends direct message
                    //remove "/msg " from message
                    message = message.substring(5, message.length());

                    //get username (recipient of dm)
                    String username = "";
                    int counter = 0;

                    while (!(Character.isWhitespace(message.charAt(counter)))) {
                        username += message.charAt(counter++);
                    }

                    message = message.substring(username.length(), message.length());

                    // Right now if they try to dm themselves, the person will "successfully" dm themselves but the message will be "you can't dm yourself".
                    // I get the feeling Vocareum will see this as an error even though it would work fine for whoever is using the program.
                    if (username.equals(client.username)) {
                        client.sendMessage(new ChatMessage("You can't direct message yourself.", 2, client.username)); //would this line cause an error? I don't think so
                    } else {
                        client.sendMessage(new ChatMessage(message, 2, username));
                    }
                }
                else { //Sends a general message of more than 4 letters
                    client.sendMessage(new ChatMessage(message, 0, null));
                }
            }
            else { //Sends a general message of less than or exactly 4 letters.
                client.sendMessage(new ChatMessage(message, 0, null));
            }

        } while (!message.equals("/LOGOUT"));
        //SAMPLE CODE: Sends an empty message to the server
        //client.sendMessage(new src.ChatMessage());
    }


    /*
     * This is a private class inside of the src.ChatClient
     * It will be responsible for listening for messages from the src.ChatServer.
     * ie: When other clients send messages, the server will relay it to the client.
     */
    private final class ListenFromServer implements Runnable {
        public void run() {

            //Allows client to persist. No kill switch besides force shutdown.
            while (true) {
                    try {
                        String msg;
                        try {
                            msg = (String) sInput.readObject();
                        }catch (SocketException | ClassNotFoundException e) {
                            System.out.println("The server has closed!");
                            socket = null;
                            return;
                        }

                        //If the server sends a kill String (right now it's end), everything closes and the client ends. (Client actually ends in main.)
                        if (msg.equals("end")) {
                            sOutput.close();
                            sInput.close();
                            socket.close();
                            return;
                        } else {
                            System.out.println(msg);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

