package com.auction;

import org.junit.jupiter.api.*;
import server.common.model.PaymentDTO;
import server.database.DBConnection;
import server.repository.PaymentDAO;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentDAOTest {
    private static PaymentDAO paymentDAO = new PaymentDAO();
    private static Long validAuctionId;
    private static Long validBuyerId;
    private static Long validSellerId;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DBConnection.getConnection()) {
            // Lấy 1 Auction ID đang có
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, seller_id FROM auction LIMIT 1")) {
                if (rs.next()) {
                    validAuctionId = rs.getLong("id");
                    validSellerId = rs.getLong("seller_id");
                }
            }
            // Lấy 1 User ID bất kỳ làm người mua (Buyer)
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM user LIMIT 1")) {
                if (rs.next()) validBuyerId = rs.getLong("id");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Test
    @Order(1)
    void testInsertPayment() {
        // Chỉ chạy nếu đã có Auction và User trong DB
        Assumptions.assumeTrue(validAuctionId != null && validBuyerId != null, "Thiếu dữ liệu nền để test Payment!");

        PaymentDTO p = new PaymentDTO();
        p.setAuctionId(validAuctionId);
        p.setBuyerId(validBuyerId);
        p.setSellerId(validSellerId);
        p.setAmount(5000.0);
        p.setStatus("COMPLETED");
        p.setCreatedAt(LocalDateTime.now());

        assertTrue(paymentDAO.insert(p), "Thêm thanh toán thất bại!");
    }

    @Test
    @Order(2)
    void testGetAllPayments() {
        List<PaymentDTO> list = paymentDAO.getAll();
        assertNotNull(list);
    }
}