package SocketServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ChatClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public void startListening(Consumer<String> onMessageReceived){

        new Thread(() -> {

            try{
                String msg;
                while((msg = in.readLine()) != null){
                    onMessageReceived.accept(msg);
                }
            }catch(Exception e){
                e.printStackTrace();
            }

        }).start();
    }
    public ChatClient(int userId) throws Exception{

        socket = new Socket("localhost",5002);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(),true);

        out.println(userId);

    }

    public void send(int receiver,String msg){
        out.println(receiver + ";" + msg);
    }

}
