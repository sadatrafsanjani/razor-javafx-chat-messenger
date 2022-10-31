package com.sadat.razor.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Connection {

    private static Client instance;

    private Connection() {
    }

    public static Client getInstance() {

        if (instance == null) {

            try {
                InetAddress inetAddress = InetAddress.getByName("localhost");
                instance = new Client(inetAddress.getHostAddress(), 8818);
            } catch (UnknownHostException ex) {
                System.out.println(ex.getMessage());
            }

        }
        return instance;
    }
}
