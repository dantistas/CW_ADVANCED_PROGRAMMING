package cw.gre.ac.uk;
import org.apache.commons.lang3.StringUtils; //lang 3 library

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

//thread in order to handle multiple connections at once.
public class ServerWorker extends Thread{
    private final Socket clientSocket; // socket of the client
    private String userName; //username of the client
    private String ID; //id of the client
    private String IpAddress; //ip address of the client
    private int port; //port of the client
    private Boolean isCoordinator = false; // coordinator status of the client
    private Boolean isAMember = false; // member status of the client
    private InputStream inputStream; //input stream
    private OutputStream outputStream; //output stream
    private BufferedReader reader; // reader for input stream
    private String input; // client input
    public static ArrayList<ServerWorker> workers = new ArrayList<>(); // list of all clients
    public static ArrayList<ServerWorker> members = new ArrayList<>(); // list of the clients who are the members

    //constructor function, defines the socket
    public ServerWorker(Socket clientSocket){
        this.clientSocket = clientSocket; // takes parameter clientSocket and defines the socket of this particular client
    }
    //runs try/catch and handles the handleClient function with parameters of this particular client socket
    @Override
    public void run(){
        try {
            handleClient(clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //client handler takes socket as argument and reads the client input and send it as output.
    private void handleClient(Socket clientSocket) throws IOException {
        this.inputStream = clientSocket.getInputStream(); //defines input stream
        this.outputStream = clientSocket.getOutputStream(); //defines output stream
        this.reader = new BufferedReader(new InputStreamReader(inputStream)); // defines buffered reader for input stream

        if(this.userName == null){ // if username is not defined, login function is invoked
            login();
        }
        while ((input = reader.readLine()) != null ){ // while user input exist, it is assigned to the user input
            String[] tokens = StringUtils.split(input); // splits the user input by space into array of strings
            if(tokens != null && tokens.length > 0){ // checks if there is strings in the array of strings.
                String command = tokens[0]; // command is assigned by the first string of the array ex. "private", "user1", "hello world!", string "private" would be assigned as a command.
                switch(command.toLowerCase()){ //switch statement for the command.
                    case "/quit":  //case "quit", quit method is invoked.
                        quit(this); // quit method with a parameter of this particular client
                        break;
                    case "/private": //case "private", tokens assigned to the new set of array of strings, and invokes sendPrivateMessage method
                        tokens = StringUtils.split(input, null, 3); //the max amount of strings are 3 in order to not split by the white space of the text body. ex. tokens[0] is command, tokens[1] is userName to send, tokens[2] text body.
                        if(tokens.length == 3){ // checks if there are 3 given parameters
                            sendPrivateMessage(tokens); // if so invokes sendPrivateMessage method
                        }else{
                            sendNotification("Must include text."); // else invokes sendNotification method
                        }
                        break;
                    case "/whoisonline": //case whoisonline, info method is invoked
                        info();
                        break;
                    case "/addmember": // case addmember, addMember method is invoked.
                        addMember(tokens, this); //  parameters: tokens, eg. tokens[0] = command "addmember", tokens[1] is username of the client from the workers list, that should be added to the members list. Another parameter this, which represents the client that invokes this method in order to check if it is a coordinator.
                        break;
                    case "/deletemember": //case deletemember, removeMember method is invoked.
                        removeMember(tokens, this); //  parameters: tokens, eg. tokens[0] = command "deletemember", tokens[1] is username of the client from the workers list, that should be deleted from the members list. Another parameter this, which represents the client that invokes this method in order to check if it is a coordinator.
                        break;
                    default: //default case, invokes sendPublicMessage method
                        tokens = StringUtils.split(input, null, 1); // tokens assigned to the new set of array of strings, in this case, max amount is 1, so the whole input is regarded as one string.
                        sendPublicMessage(tokens);
                        break;
                }
            }
        }
    }

    // client commands
    public void login() throws IOException { // login method
        sendNotification("Enter your username to join the chat server."); // invokes sendNotification method with one parameter, that is string
        while ((input = reader.readLine()) != null && userName == null ){ // while this particular client username and input exists, it is assigned to the user input
            if(input.length() != 0){ // checks if the input length is not  0, because it cannot assign "nothing" as a username
                if(input.equalsIgnoreCase("swx")){
                    sendNotification("Invalid username."); // invokes a sendNotification method
                }else{
                    boolean usernameExist = false; // boolean expression if the username already exist
                    for(ServerWorker client : workers){ //for loop to check if the given username already taken by any other user from the all clients list
                        if(client.userName.equals(input)){ // checks if the given username matches any username from the all clients list
                            usernameExist = true; // assigns usernameExist, if there is a match
                            sendNotification("Username already exist."); // invokes a sendNotification method
                        }
                    }
                    if(!usernameExist){ // checks if the usernameExist was not assigned as true
                        this.userName = input; //assigns given username
                        this.ID = UUID.randomUUID().toString(); // generates random ID, and assigns it
                        this.IpAddress = clientSocket.getInetAddress().toString(); // assigns ip address of the socket
                        this.port = clientSocket.getPort(); // assigns port of the socket
                        workers.add(this); // adds this particular client to the workers list
                        if(workers.size() == 1){ // checks if all clients list is size of 1, meaning that this client joined the server first
                            sendNotification("You are the first to join this server."); // sendNotification method is invoked
                            assignCoordinator(); // assignCoordinator method is invoked
                            members.add(this); // adds this particular client to the members list
                        }
                        sendPublicNotification(userName + " has joined the server."); // sendPublicNotification method is invoked
                        sendNotification("Welcome to the server " + userName); // sendNotification method is invoked
                        break; // breaks the while loop
                    }
                }
            }else{
                sendNotification("Enter your username to join chat the server."); // sendNotification method is invoked, if the given input length is 0
            }
        }
    }

    public void assignCoordinator () throws IOException { // assignCoordinator method
        ServerWorker newCoordinator; //newCoordinator that will be assigned
        if(members.size() > 0){ // checks if there are any members in the members list
            newCoordinator = members.get(0); // if so, gets the first member from the list
            newCoordinator.isCoordinator= true; // assigns its isCoordinator state to true
            sendPublicNotification(newCoordinator.userName + " has become new coordinator of the server."); // invokes sendPublicNotification method
        }else if(workers.size() > 0){ // checks if there are any client in the all clients list
            newCoordinator = workers.get(0); //if so, gets the first client from the list
            newCoordinator.isCoordinator= true; // assigns its isCoordinator state to true
            newCoordinator.isAMember = true; // assigns its isAMember state to true
            sendPublicNotification(newCoordinator.userName + " has become new coordinator of the server."); // invokes sendPublicNotification method
        }
    }

    public void quit(ServerWorker client) throws IOException { // quit method
        workers.remove(client); // removes this particular client from the all clients list
        members.remove(client); // removes this particular client from the members list
        sendPublicNotification(userName + " left the server."); // invokes sendPublicNotification method
        sendNotification("You have left the server."); // invokes sendNotification method
        if(client.isCoordinator){ //checks if this particular client was a coordinator
            assignCoordinator(); //if so assigns new coordinator, with a member from the members list. If the member list is empty assigns with a client from a clients list
        }
        client.clientSocket.close(); //closes socket
    }

    public void sendPublicNotification(String notification) throws IOException { //sendPublicNotification method, with String parameter
        for(ServerWorker client : workers){ //runs a for loop on all clients list, in order to send a notification for each client.
            client.sendNotification(notification); // selects a client, and invokes a sendNotification method with a string.
        }
    }

    public void sendNotification(String notification) throws IOException { //sendNotification method, with String parameter
        String notificationToSend = "notification: " + notification + "\r\n";
        outputStream.write(notificationToSend.getBytes()); // output stream outputs the output.
    }

    public void sendPublicMessage(String[] tokens) throws IOException { //sendPublicMessage method, with strings array parameter, in this case String array contains one string which is a body text.
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String body = "message " + timestamp + " " + userName + ": " + tokens[0] + "\r\n"; // constructs a body
        for(ServerWorker client : workers){ // runs a for loop on all clients list
            client.outputStream.write(body.getBytes()); // selects a client, and outputs output Stream with the text body to each client.
        }

    }

    public void sendPrivateMessage(String[] tokens) throws IOException {//sendPublicMessage method, with strings array parameter.
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String sendTo = tokens[1]; // assigns sendTo from the input
        String body = tokens[2]; // assigns body from the input
        boolean toUserDoesNotExist = false; // boolean value for if the userTo exist in the clients list.
        for(ServerWorker client : workers){ // runs for loop on all the clients
            if(sendTo.equalsIgnoreCase(client.userName)){ // checks if sendTo exist in the all clients list
                toUserDoesNotExist = true; // overwrite boolean value ir true
                String message = "message: " +  timestamp + " Private message from " + userName + " >>> " + body + "\r\n"; // constructs a message that is going to be sent
                client.outputStream.write(message.getBytes()); //// selects a client, and outputs output Stream with the message to each client.
            }
        }
        if(toUserDoesNotExist == false){
            sendNotification( "This user does not exist."); //invokes sendNotification method
        }
    }

    public void info() throws IOException { //info method
        sendNotification("Currently there are " + workers.size() + " users online."); //invokes sendNotification method
        sendNotification("Currently there are " + members.size() + " members online."); //invokes sendNotification method
        int index = 1; //assigns index in order to list the members
        for(ServerWorker client : members){ // runs for loop on the members list
            sendNotification( "Member " + index + ": " + client.userName + " " + " " + client.ID + " " + client.IpAddress + " " + client.port); //invokes sendNotification method
            index++; // increases the index by 1
        }
    }

    public void addMember(String[] tokens, ServerWorker client) throws IOException { // addMember method, takes Array fo strings, and this particular client as argument
        String usernameToAdd = tokens[1]; // assigns username of the client that is going to be added to the member list
        if(client.isCoordinator){ // checks if the client that invoked this method is a coordinator.
            for(ServerWorker worker : workers){ // runs a for loop on all clients list
                if(worker.userName.equals(usernameToAdd)){ //  checks if the username matches username of the client from the all clients list
                    worker.isAMember = true; // if so assigns isMember state as true for that client
                    members.add(worker); // adds this client to the members list
                    sendPublicNotification( worker.userName  + " successfully added to the members."); // invokes sendPublicNotification method
                }
            }
        }else{ // if the client which invoked this method is not a coordinator
            sendNotification("Only coordinators allowed to add members."); // invokes sendPublicNotification method
        }
    }

    public void removeMember(String[] tokens, ServerWorker client) throws IOException {// removeMember method, takes Array fo strings, and this particular client as argument
        String usernameToDelete = tokens[1]; // assigns username of the client that is going to be deleted from the member list
        if(client.isCoordinator){ // checks if the client that invoked this method is a coordinator.
            for(ServerWorker worker : workers){ // runs a for loop on all clients list
                if(worker.userName.equals(usernameToDelete)){ //  checks if the username matches username of the client from the all clients list
                    worker.isAMember = false; // if so assigns isMember state as false for that client
                    members.remove(worker); // removes that client from the members list
                    sendPublicNotification( worker.userName  + " is removed from the members."); // invokes sendPublicNotification method
                }
            }
        }else{ // if the client which invoked this method is not a coordinator
            sendNotification("Only coordinators allowed to remove members."); // invokes sendPublicNotification method
        }
    }
}


