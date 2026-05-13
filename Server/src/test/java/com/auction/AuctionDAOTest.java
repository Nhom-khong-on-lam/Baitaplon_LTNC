package com.auction;

import org.junit.jupiter.api.*;
import server.common.model.AuctionDTO;
import server.database.DBConnection;
import server.repository.AuctionDAO;
import java.sql.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionDAOTest {
    private static AuctionDAO auctionDAO = new AuctionDAO();
    private static Long validUserId;
    private static Long validItemId;

    @BeforeAll
    static void findExistingIds() {
        try (Connection conn = DBConnection.getConnection()) {
            // Lấy ID user bất kỳ
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM user LIMIT 1")) {
                if (rs.next()) validUserId = rs.getLong("id");
            }
            // Lấy ID item bất kỳ
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM item LIMIT 1")) {
                if (rs.next()) validItemId = rs.getLong("id");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Test
    @Order(1)
    void testInsertAuction() {
        // Chỉ chạy nếu đã có dữ liệu mẫu trong DB
        Assumptions.assumeTrue(validUserId != null && validItemId != null, "DB trống!");

        AuctionDTO auction = new AuctionDTO();
        auction.setItemId(validItemId);
        auction.setSellerId(validUserId); // Dùng Long thay vì UserDTO
        auction.setCurrentPrice(2000.0);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        auction.setStatus("OPEN"); // Khớp với ENUM trong ERD

        long newAuctionId = auctionDAO.insert(auction);
        assertTrue(newAuctionId > 0, "Insert thành công phải trả về ID lớn hơn 0");
    }
}
