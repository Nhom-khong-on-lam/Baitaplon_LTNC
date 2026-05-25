package com.auction.client.service;

import java.io.*;
import java.net.*;

public class ServerConnection {
    private static ServerConnection instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Biến static lưu lại IP Server sau khi quét được để không phải quét lại khi đổi tab
    private static String discoveredServerIp = null;

    private final int TCP_PORT = 8080;
    private final int UDP_PORT = 8888;

    private ServerConnection() {
        if (discoveredServerIp == null) {
            discoverServerIP();  // chờ tối đa 3 giây
        }
        connectToServer();
    }

    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    // 📡 Hàm bắt sóng UDP để lấy IP thật của máy Server trong mạng LAN
    private void discoverServerIP() {
        System.out.println("🔍 [UDP Client] Scanning for Server in LAN...");
        try (DatagramSocket udpSocket = new DatagramSocket(UDP_PORT)) {
            udpSocket.setSoTimeout(3000); // Đợi tối đa 3 giây, quá hạn coi như lỗi

            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // Đứng đợi gói tin UDP từ Server bắn ra
            udpSocket.receive(packet);

            String message = new String(packet.getData(), 0, packet.getLength()).trim();
            if ("AUCTION_SERVER_IP_HERE".equals(message)) {
                // Hốt lấy IP của máy Server gửi đến
                discoveredServerIp = packet.getAddress().getHostAddress();
                System.out.println("🟢 [UDP Client] Found LAN Server IP: " + discoveredServerIp);
                return;
            }
        } catch (Exception e) {
            System.out.println("⚠️ [UDP Client] LAN Server not found via UDP, reverting to default localhost.");
        }
        discoveredServerIp = "127.0.0.1"; // Phương án dự phòng nếu chạy cùng 1 máy
    }

    // 🔌 Hàm mở đúng 1 ống TCP duy nhất bằng IP LAN đã quét được
    private synchronized void connectToServer() {
        try {
            if (socket != null && !socket.isClosed()) return;

            this.socket = new Socket(discoveredServerIp, TCP_PORT);
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            System.out.println("🟢 [TCP Client] Single TCP connection established to the server!");
        } catch (IOException e) {
            System.err.println("❌ [TCP Client] Socket connection error: " + e.getMessage());
        }
    }

    // Gửi thông tin chuyển tab, login trên ĐÚNG 1 ống TCP
    public Object sendRequest(Object request) throws IOException, ClassNotFoundException {
        if (socket == null || socket.isClosed()) {
            connectToServer();
        }

        synchronized (this) {
            try {
                out.writeObject(request);
                out.flush();
                out.reset();
                return in.readObject();
            } catch (IOException e) {
                System.err.println("❌ [TCP Client] Connection lost, re-establishing connection...");
                closeConnection();
                connectToServer();
                throw e;
            }
        }
    }
    // THÊM hàm này — gọi khi app khởi động, chạy ngầm
    public static void connectAsync(Runnable onDone) {
        new Thread(() -> {
            getInstance(); // chạy discovery + connect ngầm
            if (onDone != null) onDone.run();
        }).start();
    }

    public synchronized void closeConnection() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("🧹 [Client] Socket closed after logging out.");
        } catch (IOException e) {
            System.err.println("❌ [Client] Error cleaning up socket: " + e.getMessage());
        } finally {
            socket = null; out = null; in = null;
        }
    }
}