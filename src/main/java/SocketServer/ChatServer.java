package SocketServer;

import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {

    private static Map<Integer,PrintWriter> clients = new HashMap<>();

    public static void main(String[] args) throws Exception{

        ServerSocket server = null;
        try {
            server = new ServerSocket(5002);
            System.out.println("Server chat started on port 5002...");
            while (true) {
                Socket socket = server.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (server != null) {
                try { server.close(); } catch(IOException ignored) {}
            }
        }
    }

    static class ClientHandler extends Thread{

        Socket socket;
        BufferedReader in;
        PrintWriter out;

        int userId;

        ClientHandler(Socket s) throws Exception{

            socket = s;

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);

        }

        public void run(){

            try{

                userId = Integer.parseInt(in.readLine());

                clients.put(userId,out);

                while(true){

                    String msg = in.readLine();

                    String[] data = msg.split(";");

                    int receiver = Integer.parseInt(data[0]);

                    if(clients.containsKey(receiver)){

                        clients.get(receiver).println(msg);

                    }

                }

            }catch(Exception e){
                e.printStackTrace();
            }

        }
    }
}