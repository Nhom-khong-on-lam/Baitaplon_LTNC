package com.auction;

import org.junit.jupiter.api.*;
import server.common.model.Auction_extension_logDTO;
import server.repository.AuctionExtensionLogDAO;
import server.database.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionExtensionLogDAOTest {

    private static final Logger LOGGER = Logger.getLogger(AuctionExtensionLogDAOTest.class.getName());

    private AuctionExtensionLogDAO logDAO;
    private Long testAuctionId;

    @BeforeAll
    void init() throws Exception {
        logDAO = new AuctionExtensionLogDAO();

        // Tự động lấy 1 ID Auction có thật để đảm bảo tính toàn vẹn dữ liệu (Foreign Key)
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM auction LIMIT 1")) {
            if (rs.next()) {
                testAuctionId = rs.getLong("id");
            }
        }

        Assumptions.assumeTrue(testAuctionId != null,
                "Bảng 'auction' đang trống, không thể thực hiện test ghi log gia hạn!");

        LOGGER.info("SETUP SUCCESS: Sẵn sàng test với Auction ID = " + testAuctionId);
    }

    @Test
    @Order(1)
    @DisplayName("1. Test Insert Extension Log")
    void testInsertLog() {
        LocalDateTime originalEnd = LocalDateTime.now().plusHours(1);
        LocalDateTime newEnd = LocalDateTime.now().plusHours(2);

        Auction_extension_logDTO log = new Auction_extension_logDTO();
        log.setAuctionId(testAuctionId);
        log.setOriginalEndTime(originalEnd);
        log.setNewEndTime(newEnd);

        boolean success = logDAO.insertLog(log);
        assertTrue(success, "Ghi log gia hạn thất bại!");
        LOGGER.info("TEST INSERT SUCCESS");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test Find Logs By Auction ID")
    void testFindByAuctionId() {
        List<Auction_extension_logDTO> logs = logDAO.findByAuctionId(testAuctionId);

        assertNotNull(logs);
        assertFalse(logs.isEmpty(), "Lịch sử gia hạn không được rỗng sau khi đã insert");

        // Kiểm tra xem log mới nhất có đúng auction_id không
        assertEquals(testAuctionId, logs.get(0).getAuctionId());
    }

    @Test
    @Order(3)
    @DisplayName("3. Test Get Total Extended Minutes")
    void testGetTotalExtendedMinutes() {
        long minutes = logDAO.getTotalExtendedMinutes(testAuctionId);

        // Vì ở bước 1 chúng ta gia hạn thêm 1 tiếng (60 phút)
        assertTrue(minutes >= 60, "Tổng số phút gia hạn phải >= 60");
        LOGGER.info("Tổng số phút đã gia hạn cho Auction " + testAuctionId + " là: " + minutes);
    }

    @Test
    @Order(4)
    @DisplayName("4. Test Find Logs With Invalid ID")
    void testFindByInvalidId() {
        List<Auction_extension_logDTO> logs = logDAO.findByAuctionId(-999L);
        assertTrue(logs.isEmpty(), "ID không tồn tại phải trả về danh sách rỗng");
    }
}