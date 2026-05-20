package com.auction;

import com.auction.common.dto.Auction_watchDTO;
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.*;
import server.repository.AuctionWatchDAO;
import server.repository.UserDAO;
import server.database.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionWatchDAOTest {

    private AuctionWatchDAO watchDAO;
    private UserDAO userDAO;
    private Long testUserId;
    private Long testAuctionId;

    @BeforeAll
    void init() throws Exception {
        watchDAO = new AuctionWatchDAO();
        userDAO = new UserDAO();

        // Lấy 1 ID auction bất kỳ để test tính hợp lệ của Foreign Key
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id FROM auction LIMIT 1")) {
            if (rs.next()) testAuctionId = rs.getLong("id");
        }

        Assumptions.assumeTrue(testAuctionId != null, "Cần ít nhất 1 Auction trong DB để test");

        // Tạo User mới để test
        UserDTO user = new UserDTO();
        user.setUsername("watcher_" + System.currentTimeMillis());
        user.setPassword("123456");
        user.setEmail("test@gmail.com");
        testUserId = userDAO.insert(user);
    }

    @AfterAll
    void cleanup() {
        if (testUserId != null) userDAO.delete(testUserId);
    }

    @Test
    @Order(1)
    @DisplayName("1. Test Add Watch")
    void testAddWatch() {
        Auction_watchDTO watch = new Auction_watchDTO();
        watch.setUserId(testUserId);
        watch.setAuctionId(testAuctionId);
        // Chỉ cần insert được vào bảng auction_watch là thành công
        assertTrue(watchDAO.addWatch(watch));
    }

    @Test
    @Order(2)
    @DisplayName("2. Test Find By User ID")
    void testFindByUserId() {
        List<Auction_watchDTO> list = watchDAO.getWatchListByUserId(testUserId);
        assertNotNull(list);
        assertFalse(list.isEmpty());
        // Kiểm tra xem ID auction lấy ra có đúng cái mình vừa lưu không
        assertEquals(testAuctionId, list.get(0).getAuctionId());
    }

    @Test
    @Order(3)
    @DisplayName("3. Test Is Watching")
    void testIsWatching() {
        assertTrue(watchDAO.isWatching(testUserId, testAuctionId));
    }

    @Test
    @Order(4)
    @DisplayName("4. Test Remove Watch")
    void testRemoveWatch() {
        assertTrue(watchDAO.removeWatch(testUserId, testAuctionId));
        assertFalse(watchDAO.isWatching(testUserId, testAuctionId));
    }
}