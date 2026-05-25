package com.auction;

import com.auction.common.dto.PaymentDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import server.database.DBConnection;
import server.repository.PaymentDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PaymentDAOTest {

    private PaymentDAO paymentDAO;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private MockedStatic<DBConnection> mockedDbConnection;

    @BeforeEach
    void setUp() throws SQLException {
        paymentDAO = new PaymentDAO();

        // Khởi tạo các đối tượng mock của JDBC
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        // Mock phương thức tĩnh DBConnection.getConnection()
        mockedDbConnection = Mockito.mockStatic(DBConnection.class);
        mockedDbConnection.when(DBConnection::getConnection).thenReturn(mockConnection);

        // Cấu hình mặc định khi Connection tạo PreparedStatement
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }

    @AfterEach
    void tearDown() {
        // Giải phóng và đóng mock static sau mỗi hàm test để tránh xung đột luồng
        mockedDbConnection.close();
    }

    @Test
    void testGetByAuctionId_Found() throws SQLException {
        long auctionId = 100L;

        // Giả lập ResultSet trả về một bản ghi dữ liệu hóa đơn
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false); // Trả về true lần đầu, false lần sau để kết thúc vòng lặp
        when(mockResultSet.getLong("id")).thenReturn(1L);
        when(mockResultSet.getLong("auction_id")).thenReturn(auctionId);
        when(mockResultSet.getLong("buyer_id")).thenReturn(2L);
        when(mockResultSet.getLong("seller_id")).thenReturn(3L);
        when(mockResultSet.getDouble("amount")).thenReturn(1500.0);
        when(mockResultSet.getString("status")).thenReturn("PENDING");

        // Gọi hàm cần kiểm thử
        PaymentDTO result = paymentDAO.getByAuctionId(auctionId);

        // Kiểm tra dữ liệu được ánh xạ từ ResultSet sang DTO chính xác
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(auctionId, result.getAuctionId());
        assertEquals(2L, result.getBuyerId());
        assertEquals(3L, result.getSellerId());
        assertEquals(1500.0, result.getAmount());
        assertEquals("PENDING", result.getStatus());

        // Xác minh xem PreparedStatement đã truyền đúng tham số auctionId vào câu lệnh chưa
        verify(mockPreparedStatement, times(1)).setLong(1, auctionId);
    }

    @Test
    void testGetByAuctionId_NotFound() throws SQLException {
        long auctionId = 999L;

        // Giả lập ResultSet rỗng (không tìm thấy hóa đơn)
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        PaymentDTO result = paymentDAO.getByAuctionId(auctionId);

        // Hệ thống phải trả về null
        assertNull(result);
    }

    @Test
    void testInsert_Success() throws SQLException {
        // Chuẩn bị dữ liệu đầu vào
        PaymentDTO dto = new PaymentDTO();
        dto.setAuctionId(10L);
        dto.setBuyerId(20L);
        dto.setSellerId(30L);
        dto.setAmount(500.0);
        dto.setStatus("PAID");

        // Giả lập câu lệnh INSERT thực thi thành công (ảnh hưởng > 0 dòng)
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean isInserted = paymentDAO.insert(dto);

        assertTrue(isInserted);

        // Xác minh các vị trí gán tham số dữ liệu chuẩn theo mã nguồn
        verify(mockPreparedStatement, times(1)).setLong(1, 10L);
        verify(mockPreparedStatement, times(1)).setLong(2, 20L);
        verify(mockPreparedStatement, times(1)).setLong(3, 30L);
        verify(mockPreparedStatement, times(1)).setDouble(4, 500.0);
        verify(mockPreparedStatement, times(1)).setString(5, "PAID");
    }

    @Test
    void testUpdateStatusWithConn_Success() throws SQLException {
        long paymentId = 1L;
        String status = "COMPLETED";

        // Giả lập câu lệnh UPDATE thành công
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Truyền một đối tượng mockConnection trực tiếp vào hàm
        boolean isUpdated = paymentDAO.updateStatusWithConn(mockConnection, paymentId, status);

        assertTrue(isUpdated);
        verify(mockPreparedStatement, times(1)).setString(1, status);
        verify(mockPreparedStatement, times(1)).setLong(2, paymentId);
    }

    @Test
    void testInsertWithConn_Success() throws SQLException {
        PaymentDTO dto = new PaymentDTO();
        dto.setAuctionId(50L);
        dto.setBuyerId(60L);
        dto.setSellerId(70L);
        dto.setAmount(2500.0);
        dto.setStatus("REFUNDED");

        // Giả lập câu lệnh thực thi có kết nối truyền vào thành công
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean isInserted = paymentDAO.insertWithConn(mockConnection, dto);

        assertTrue(isInserted);
        verify(mockPreparedStatement, times(1)).setLong(1, 50L);
        verify(mockPreparedStatement, times(1)).setLong(2, 60L);
        verify(mockPreparedStatement, times(1)).setLong(3, 70L);
        verify(mockPreparedStatement, times(1)).setDouble(4, 2500.0);
        verify(mockPreparedStatement, times(1)).setString(5, "REFUNDED");
    }
}