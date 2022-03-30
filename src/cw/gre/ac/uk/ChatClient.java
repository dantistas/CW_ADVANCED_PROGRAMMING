package cw.gre.ac.uk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ChatClient {

    private final String serverIp;
    private final int port;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public ChatClient(String Ip, int port) {
        this.serverIp =  Ip;
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        ChatClient chatClient = new ChatClient("localhost", 1111);
//        boolean clientStatus = chatClient.connect();
        if(!chatClient.connect()){
            System.err.println("Connection failed");
        }else{
            System.out.println("Connected successfully");
            System.out.println(chatClient.socket);
        }

    }

    private boolean connect() {
        try{
            this.socket = new Socket(serverIp, port);
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
            return true;
        }catch(IOException e){
            e.printStackTrace();
        }
        return false;
    }
}
