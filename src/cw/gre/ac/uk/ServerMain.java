package cw.gre.ac.uk;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static void main(String[] args) {
        //server port
        int port = 1111;
        try {
            System.out.println("Waiting for clients.");
            ServerSocket serverSocket = new ServerSocket(port);
            // while loop true, because server listens constantly for new connections.
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("User joined the server." + clientSocket);
                //worker is a thread, to handle multiple connections at once.
                ServerWorker worker = new ServerWorker(clientSocket);
                worker.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
