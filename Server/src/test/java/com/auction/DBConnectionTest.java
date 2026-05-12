package com.auction;

import org.junit.jupiter.api.*;
import server.database.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử kết nối Database (DBConnection)")
class DBConnectionTest {

    @AfterAll
    static void tearDown() {
        // Đảm bảo đóng kết nối sau khi chạy xong toàn bộ test
        DBConnection.closeConnection();
    }

    @Test
    @DisplayName("Kiểm tra getConnection() trả về kết nối hợp lệ")
    void testGetConnectionSuccess() throws SQLException {
        Connection connection = DBConnection.getConnection();

        assertNotNull(connection, "Kết nối không được null");
        assertFalse(connection.isClosed(), "Kết nối phải đang ở trạng thái mở");
    }

    @Test
    @DisplayName("Kiểm tra tính Singleton (Nhiều lần gọi trả về cùng 1 instance)")
    void testSingletonInstance() throws SQLException {
        Connection conn1 = DBConnection.getConnection();
        Connection conn2 = DBConnection.getConnection();

        // Kiểm tra xem 2 biến có trỏ cùng vào 1 vùng nhớ không
        assertSame(conn1, conn2, "Hai lần gọi getConnection() phải trả về cùng một instance");
    }

    @Test
    @DisplayName("Kiểm tra tự động kết nối lại sau khi đóng")
    void testReconnectionAfterClose() throws SQLException {
        Connection conn1 = DBConnection.getConnection();

        // Giả lập việc đóng kết nối
        DBConnection.closeConnection();
        assertTrue(conn1.isClosed(), "Instance cũ phải bị đóng");

        // Gọi lại getConnection() - Singleton phải tạo instance mới vì instance cũ đã closed
        Connection conn2 = DBConnection.getConnection();
        assertNotNull(conn2);
        assertFalse(conn2.isClosed(), "Kết nối mới phải được mở");
    }
}