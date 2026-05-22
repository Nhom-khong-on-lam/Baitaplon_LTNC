package server.network;

import java.io.IOException;
import java.net.*;

public class AuctionServer {
    private static final int TCP_PORT = 8080; // Cổng truyền thông tin chính (TCP)
    private static final int UDP_PORT = 8888; // Cổng phát sóng tìm IP (UDP)

    public static void main(String[] args) {
        // 📡 1. Kích hoạt luồng phát sóng UDP ngầm ra mạng LAN
        startUDPBeacon();

        // 🔌 2. Mở cổng TCP đón Client vào truyền thông tin
        try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
            System.out.println("🟢 [TCP Server] Đang mở tại cổng: " + TCP_PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("📶 [TCP Server] Client kết nối từ IP: " + clientSocket.getInetAddress().getHostAddress());

                // Bàn giao ống Socket duy nhất này cho ClientHandler quản lý vĩnh viễn
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("❌ [TCP Server] Lỗi khởi động: " + e.getMessage());
        }
    }

    // Hàm phát tín hiệu UDP mạng LAN
    private static void startUDPBeacon() {
        Thread udpThread = new Thread(() -> {
            try (DatagramSocket udpSocket = new DatagramSocket(UDP_PORT)) {
                udpSocket.setBroadcast(true); // Cho phép phát sóng toàn mạng LAN

                String msg = "AUCTION_SERVER_IP_HERE";
                byte[] buffer = msg.getBytes();

                // Gửi gói tin đến địa chỉ đại diện toàn mạng LAN (255.255.255.255) tại cổng 8888
                DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length,
                        InetAddress.getByName("255.255.255.255"),
                        UDP_PORT
                );

                System.out.println("📡 [UDP Beacon] Đang phát sóng tìm Client ngầm từng giây...");
                while (true) {
                    udpSocket.send(packet);
                    Thread.sleep(1000); // Cứ 1 giây phát tín hiệu 1 lần
                }
            } catch (Exception e) {
                System.err.println("❌ [UDP Beacon] Lỗi phát sóng: " + e.getMessage());
            }
        });
        udpThread.setDaemon(true); // Luồng chạy ngầm tự tắt khi tắt app chính
        udpThread.start();
    }
}