package com.sadat.razor.client;

import com.sadat.razor.interfaces.MessageListener;
import com.sadat.razor.interfaces.ShakeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import com.sadat.razor.interfaces.StatusListener;
import com.sadat.razor.interfaces.UserListener;

public class Client implements StatusListener {

    private final String serverName;
    private final int port;
    private Socket socket;
    private InputStream in;
    private OutputStream os;
    private BufferedReader br;
    private final List<UserListener> userList = new ArrayList<>();
    private final List<MessageListener> messageList = new ArrayList<>();
    private final List<ShakeListener> shakeList = new ArrayList<>();

    public Client(String serverName, int port) {
        this.serverName = serverName;
        this.port = port;
    }

    public boolean connectToServer() {

        try {
            socket = new Socket(serverName, port);
            os = socket.getOutputStream();
            in = socket.getInputStream();
            br = new BufferedReader(new InputStreamReader(in));

            return true;
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return false;
    }
    
    public boolean register(String username, String password){
    
        String response = null;

        try {
            String command = "reg " + username + " " + password + "\n";
            sendMessage(command);

            response = br.readLine();
            System.out.println("Server Response: " + response);

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        if ("GOOD".equalsIgnoreCase(response)) {

            acceptServerReply();

            return true;
        } else {

            return false;
        }
    
    }

    public boolean login(String username, String password) {

        String response = null;

        try {
            String command = "login " + username + " " + password + "\n";
            sendMessage(command);

            response = br.readLine();
            System.out.println("Server Response: " + response);

        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        if ("OK".equalsIgnoreCase(response)) {

            acceptServerReply();

            return true;
        } else {

            return false;
        }
    }

    private void acceptServerReply() {

        Thread thread = new Thread() {

            @Override
            public void run() {

                if (socket.isConnected()) {

                    acceptServerReplyLoop();
                }
            }
        };

        thread.start();
    }

    private void acceptServerReplyLoop() {

        try {
            String input;

            while ((input = br.readLine()) != null) {

                String[] tokens = input.split(" ");

                if ((tokens != null) && (tokens.length > 0)) {

                    String command = tokens[0];

                    if ("online".equalsIgnoreCase(command)) {

                        handleOline(tokens[1]);

                    } else if ("offline".equalsIgnoreCase(command)) {

                        handleOffine(tokens[1]);

                    } else if ("msg".equalsIgnoreCase(command)) {

                        handleMessage(StringUtils.split(input, null, 3));

                    }
                    else if("reg".equalsIgnoreCase(command)){
                        System.out.println(command);
                    }
                    else if("shake".equalsIgnoreCase(command)){
                        System.out.println(tokens[1] + " Shaked you");
                        handleShake(tokens);
                    }
                }

            }
        } catch (IOException ex) {

            System.out.println(ex.getMessage());

        }
    }
    
    private void handleShake(String[] tokens) {

        String from = tokens[1];

        for (ShakeListener listener : shakeList) {
            listener.shakeUser(from);
        }
        
        for (MessageListener listener : messageList) {
            listener.conveyMessage(from, " BUZZ!");
        }

    }

    private void handleOline(String user) {

        userSet.add(user);

        for (UserListener listener : userList) {

            listener.online(user);
        }
    }

    private void handleOffine(String user) {

        userSet.remove(user);

        for (UserListener listener : userList) {

            listener.offline(user);
        }
    }

    public boolean logout() {

        sendMessage("quit\n");
        closeConnection();
        
        return socket.isClosed();
    }

    public void closeConnection() {

        try {
            socket.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void sendMessageToUser(String to, String message) {

        String command = "msg " + to + " " + message + "\n";
        sendMessage(command);
    }
    
    public void sendShakeToUser(String to) {

        String command = "shake " + to + "\n";
        sendMessage(command);
    }

    private void sendMessage(String message) {

        try {
            os.write(message.getBytes());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void addMessage(MessageListener message) {

        messageList.add(message);
    }

    public void removeMessage(MessageListener message) {

        messageList.remove(message);
    }

    private void handleMessage(String[] tokens) {

        String from = tokens[1];
        String message = tokens[2];

        for (MessageListener listener : messageList) {
            listener.conveyMessage(from, message);
        }

    }

    public void addUser(UserListener listener) {

        userList.add(listener);
    }

    public void removeUser(UserListener listener) {

        userList.remove(listener);
    }
    
    public void addShake(ShakeListener shake) {

        shakeList.add(shake);
    }
    
    public void removeShake(ShakeListener shake) {

        shakeList.remove(shake);
    }
}
