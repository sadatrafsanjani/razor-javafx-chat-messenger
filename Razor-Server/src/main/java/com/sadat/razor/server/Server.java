package com.sadat.razor.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server implements Runnable {

    private final int port;
    private final List<ClientHandler> clientList = new ArrayList<>();

    public Server(int port) {
        this.port = port;
    }

    public List<ClientHandler> getClientList() {
        return clientList;
    }

    @Override
    public void run() {

        try {

            InetAddress ip = InetAddress.getByName("127.0.0.1");
            ServerSocket serverSocket = new ServerSocket(port, 50, ip);
            
            System.out.println("Server started...\n");

            while (true) {

                Socket socket = serverSocket.accept();
                
                publishConnectionReport(socket);

                ClientHandler client = new ClientHandler(Server.this, socket);
                Thread thread = new Thread(client);
                thread.start();

                clientList.add(client);
            }

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void removeClient(ClientHandler client) {

        clientList.remove(client);
    }

    private void publishConnectionReport(Socket socket) {

        System.out.println("######### Connection #########");
        System.out.println("Address: " + socket.getInetAddress());
        System.out.println("Source Port: " + socket.getPort());
        System.out.println("Listening Port: " + socket.getLocalPort());
        System.out.println("------------------------------------\n");

    }

}
