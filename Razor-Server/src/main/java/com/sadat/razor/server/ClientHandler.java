package com.sadat.razor.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class ClientHandler implements Runnable {

    private final Server server;
    private final Socket client;
    private String user;
    private OutputStream os;

    private final String database = "jdbc:sqlite:razor.db";

    private boolean checkCredentials(String username, String password) {

        boolean login = false;

        try {
            Connection connection = DriverManager.getConnection(database);
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT id FROM users WHERE username=? AND password=? LIMIT 1"
            );
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            login = rs.next();
            pstmt.close();
            connection.close();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return login;
    }

    public ClientHandler(Server server, Socket client) {
        this.server = server;
        this.client = client;
    }

    public String getUser() {
        return user;
    }

    @Override
    public void run() {

        handleClients(client);
    }

    private void handleClients(Socket socket) {

        try {
            os = socket.getOutputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String input;

            while ((input = br.readLine()) != null) {

                String[] tokens = input.split(" ");

                if ((tokens[0] != null) && (tokens[0].length() > 0)) {

                    String command = tokens[0];

                    if ("quit".equalsIgnoreCase(command)) {

                        handleLogout();
                        break;

                    } else if ("login".equalsIgnoreCase(command)) {

                        handleLogin(tokens);

                    } else if ("msg".equalsIgnoreCase(command)) {

                        handleMessage(StringUtils.split(input, null, 3));

                    } else if ("reg".equalsIgnoreCase(command)) {

                        handleRegister(tokens);
                    } else if ("shake".equalsIgnoreCase(command)) {
                        handleShake(tokens);
                    } else {

                        String message = "Unknown Command!\n";
                        sendMessage(message);
                    }
                }
            }

            br.close();
            os.close();
            client.close();

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void handleShake(String[] tokens) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                String receiver = tokens[1];

                List<ClientHandler> clientList = server.getClientList();

                for (ClientHandler other : clientList) {

                    if (receiver.equalsIgnoreCase(other.getUser())) {

                        String message = "shake " + user + "\n";
                        other.sendMessageToUser(message);
                    }
                }
            }
        };

        new Thread(runnable).start();
    }

    private void notifyCurrentUser() {

        List<ClientHandler> clientList = server.getClientList();

        for (ClientHandler other : clientList) {

            if (other.getUser() != null) {
                if (!user.equals(other.getUser())) {

                    String status = "online " + other.getUser() + "\n";
                    sendMessageToUser(status);
                }
            }
        }

    }

    private void notifyOtherUser() {

        List<ClientHandler> clientList = server.getClientList();

        String status = "online " + user + "\n";
        for (ClientHandler other : clientList) {

            if (!user.equals(other.getUser())) {

                other.sendMessageToUser(status);
            }
        }
    }

    private void handleRegister(String[] tokens) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                if (tokens.length == 3) {

                    String username = tokens[1];
                    String password = tokens[2];

                    if (!checkUser(username)) {

                        registerUser(username, password);

                        sendMessage("GOOD\n");
                    } else {
                        sendMessage("BAD\n");
                    }
                }
            }
        };

        new Thread(runnable).start();
    }

    private boolean checkUser(String username) {

        boolean exist = false;

        try {
            Connection connection = DriverManager.getConnection(database);
            PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT id FROM users WHERE username=?"
            );
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            exist = rs.next();
            pstmt.close();
            connection.close();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return exist;
    }

    private void registerUser(String username, String password) {

        try {
            Connection connection = DriverManager.getConnection(database);
            PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO users (username, password) VALUES (?, ?)"
            );
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            pstmt.close();
            connection.close();

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }

    }

    private void handleLogin(String[] tokens) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {

                if (tokens.length == 3) {

                    String username = tokens[1];
                    String password = tokens[2];

                    if (checkCredentials(username, password)) {

                        sendMessage("OK\n");

                        user = username;
                        notifyCurrentUser();
                        notifyOtherUser();

                        System.out.println("[ " + user + " Logged In ]\n");

                    } else {

                        sendMessage("NOT\n");
                        System.err.println(username + " Not Found!");
                    }
                }
            }
        };

        new Thread(runnable).start();

    }

    private void handleLogout() {

        server.removeClient(this);
        List<ClientHandler> clientList = server.getClientList();

        String status = "offline " + user + "\n";
        for (ClientHandler other : clientList) {

            if (!user.equals(other.getUser())) {

                other.sendMessageToUser(status);
            }
        }

        System.out.println("[ " + user + " Logged out ]\n");

        try {
            user = null;
            os.close();
            client.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

    }

    private void handleMessage(String[] tokens) {

        String receiver = tokens[1];
        String body = tokens[2];

        List<ClientHandler> clientList = server.getClientList();

        for (ClientHandler other : clientList) {

            if (receiver.equalsIgnoreCase(other.getUser())) {

                String message = "msg " + user + " " + body + "\n";
                other.sendMessageToUser(message);
            }
        }
    }

    private void sendMessageToUser(String message) {

        if (user != null) {
            sendMessage(message);
        }
    }

    private void sendMessage(String message) {

        try {
            os.write(message.getBytes());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
