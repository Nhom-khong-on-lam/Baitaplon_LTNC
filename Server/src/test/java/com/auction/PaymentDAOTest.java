package com.auction;

import com.auction.common.dto.PaymentDTO;
import org.junit.jupiter.api.*;
import server.database.DBConnection;
import server.repository.PaymentDAO;
import java.sql.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentDAOTest {
    private static PaymentDAO paymentDAO = new PaymentDAO();
    private static Long validAuctionId = 60001L;
    private static Long validBuyerId = 180003L;
    private static Long validSellerId = 180003L;

    @Test
    @Order(1)
    @DisplayName("Quy trình chuẩn: Xóa cũ -> Thêm mới -> Lấy ra kiểm tra")
    void testPaymentLifeCycle() {
        // 1. Dọn dẹp dữ liệu cũ để tránh lỗi Duplicate entry '60001'
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM payment WHERE auction_id = ?")) {
            ps.setLong(1, validAuctionId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }

        // 2. Insert dữ liệu mới
        PaymentDTO p = new PaymentDTO();
        p.setAuctionId(validAuctionId);
        p.setBuyerId(validBuyerId);
        p.setSellerId(validSellerId);
        p.setAmount(5000.0);
        p.setStatus("COMPLETED");
        p.setCreatedAt(LocalDateTime.now());

        assertTrue(paymentDAO.insert(p), "Thêm thanh toán vào DB thất bại!");

        // 3. Lấy ra kiểm tra (Đảm bảo getByAuctionId không còn trả về null)
        PaymentDTO found = paymentDAO.getByAuctionId(validAuctionId);

        assertNotNull(found, "Lỗi: Không tìm thấy Payment sau khi Insert cho ID: " + validAuctionId);
        assertEquals(validAuctionId, found.getAuctionId());
        assertEquals(5000.0, found.getAmount());
        System.out.println("Test thành công: Đã tìm thấy thanh toán cho Auction " + validAuctionId);
    }
}