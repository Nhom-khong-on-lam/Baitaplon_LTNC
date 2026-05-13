package server.network;

import java.io.*;
import java.net.*;

public class AuctionServer {
    private static final int PORT = 8080; // Cổng kết nối, phải khớp với Client

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Auction Server đang chạy tại cổng: " + PORT);

            while (true) {
                // Đợi Client kết nối
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client mới đã kết nối: " + clientSocket.getInetAddress());

                // Tạo một luồng riêng để xử lý mỗi Client (Multi-threading)
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
        }
    }
}

