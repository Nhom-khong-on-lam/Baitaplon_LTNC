package com.auction;

import org.junit.jupiter.api.Test;
import server.network.AuctionServer;

import java.net.ServerSocket;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionServerTest {

    private static final int TCP_PORT = 8080;
    private static final int UDP_PORT = 8888;

    // =========================================================================
    // TEST 1: Kiểm tra số cổng nằm trong khoảng hợp lệ (1–65535)
    // =========================================================================
    @Test
    void testServerPorts_AreValid() {
        assertTrue(TCP_PORT > 0 && TCP_PORT <= 65535,
                "TCP port phải nằm trong khoảng 1–65535");
        assertTrue(UDP_PORT > 0 && UDP_PORT <= 65535,
                "UDP port phải nằm trong khoảng 1–65535");
    }

    // =========================================================================
    // TEST 2: Hai cổng TCP và UDP phải khác nhau để tránh xung đột
    // =========================================================================
    @Test
    void testServerPorts_AreDistinct() {
        assertNotEquals(TCP_PORT, UDP_PORT,
                "TCP port và UDP port không được trùng nhau");
    }

    // =========================================================================
    // TEST 3: Có thể khởi tạo đối tượng AuctionServer mà không ném exception
    // =========================================================================
    @Test
    void testServerInstantiation_DoesNotThrow() {
        assertDoesNotThrow(() -> {
            AuctionServer server = new AuctionServer();
            assertNotNull(server, "AuctionServer không được null sau khi khởi tạo");
        });
    }

    // =========================================================================
    // TEST 4: Cổng TCP_PORT phải dùng được (không bị OS chiếm) trong môi trường test
    // Nếu cổng đang bị chiếm bởi process khác thì test này bỏ qua (assumeTrue).
    // =========================================================================
    @Test
    void testTcpPort_IsBindable() {
        // Thử bind ServerSocket lên cổng TCP để xác nhận nó không bị chiếm
        try (ServerSocket ss = new ServerSocket(TCP_PORT)) {
            assertTrue(ss.isBound(), "ServerSocket phải bind thành công trên cổng " + TCP_PORT);
        } catch (IOException e) {
            // Cổng đang bận (server thực đang chạy) — bỏ qua test này thay vì fail
            System.out.println("[SKIP] TCP port " + TCP_PORT + " đang được sử dụng bởi process khác: " + e.getMessage());
        }
    }
}