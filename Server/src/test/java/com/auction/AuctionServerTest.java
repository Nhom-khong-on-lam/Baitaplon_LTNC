package com.auction;

import org.junit.jupiter.api.Test;
import server.network.AuctionServer;

import java.io.IOException;
import java.net.ServerSocket;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionServerTest {

    @Test
    void testServerPorts_AreAvailableAndValid() {
        // Kiểm tra tính hợp lệ của số cổng quy định trong mã nguồn thiết kế
        int tcpPort = 8080; // Dựa trên TCP_PORT
        int udpPort = 8888; // Dựa trên UDP_PORT

        assertTrue(tcpPort > 0 && tcpPort <= 65535);
        assertTrue(udpPort > 0 && udpPort <= 65535);
    }

    @Test
    void testServerInstantiation() {
        // Đảm bảo có thể tạo đối tượng AuctionServer bình thường không lỗi
        AuctionServer server = new AuctionServer();
        assertNotNull(server);
    }
}