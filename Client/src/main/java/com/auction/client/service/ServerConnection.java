package com.auction.client.service;

import java.io.*;
import java.net.Socket;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // ĐỊA CHỈ VÀ CỔNG CỦA SERVER ĐÂY NÈ!
    private final String HOST = "127.0.0.1";
    private final int PORT = 8080; // Phải khớp với 8080 của Server

    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) instance = new ServerConnection();
        return instance;
    }

    public Object sendRequest(Object request) throws IOException, ClassNotFoundException {
        // Mỗi lần gửi, Client sẽ nhìn vào PORT 8080 để đi
        try (Socket tempSocket = new Socket(HOST, PORT);
             ObjectOutputStream tempOut = new ObjectOutputStream(tempSocket.getOutputStream());
             ObjectInputStream tempIn = new ObjectInputStream(tempSocket.getInputStream())) {

            tempOut.writeObject(request);
            tempOut.flush();
            return tempIn.readObject();
        }
    }
}