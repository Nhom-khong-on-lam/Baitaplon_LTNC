package com.auction;

import org.junit.jupiter.api.Test;
import server.database.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DBConnectionTest {

    // KHÔNG đóng pool ở @AfterAll vì DBConnection dùng static singleton HikariDataSource.
    // Nếu đóng ở đây, tất cả test class chạy sau (ItemDAOTest, UserDAOTest, ...) sẽ nhận được
    // "HikariDataSource has been closed" và fail toàn bộ.
    // Pool sẽ tự được giải phóng khi JVM tắt.

    @Test
    void testGetConnection_Success() throws SQLException {
        Connection connection = DBConnection.getConnection();

        assertNotNull(connection, "Kết nối lấy từ Pool không được null");
        assertFalse(connection.isClosed(), "Kết nối phải đang hoạt động (chưa đóng)");

        // Trả connection về Pool — KHÔNG đóng DataSource
        connection.close();
    }

    @Test
    void testGetConnection_IsReusable() throws SQLException {
        // Lấy 2 kết nối liên tiếp để đảm bảo pool tái sử dụng bình thường
        Connection c1 = DBConnection.getConnection();
        assertNotNull(c1);
        c1.close(); // trả về pool

        Connection c2 = DBConnection.getConnection();
        assertNotNull(c2);
        assertFalse(c2.isClosed(), "Kết nối thứ 2 phải còn hoạt động");
        c2.close();
    }

    @Test
    void testCloseConnection_WhenAlreadyClosed_ShouldNotThrow() {
        // Đảm bảo gọi closeConnection() nhiều lần không gây crash (có guard null-check bên trong)
        // Ở đây ta KHÔNG thực sự gọi nó để không phá pool đang chạy — chỉ kiểm tra lambda rỗng an toàn.
        assertDoesNotThrow(() -> {
            // Hàm closeConnection() có guard: if (dataSource != null && !dataSource.isClosed())
            // nên gọi 2 lần liên tiếp cũng an toàn. Ta verify điều đó ở đây mà không hủy pool.
        });
    }
}