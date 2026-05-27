package com.auction;

import com.auction.common.dto.AuctionDTO;
import com.auction.common.dto.Auction_watchDTO;
import com.auction.common.dto.ItemDTO;
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.*;
import server.repository.AuctionDAO;
import server.repository.AuctionWatchDAO;
import server.repository.ItemDAO;
import server.repository.UserDAO;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử AuctionWatchDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Chia sẻ các ID mồi xuyên suốt các bước test
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionWatchDAOTest {

    private AuctionWatchDAO auctionWatchDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    // Lưu các ID mồi để làm khóa ngoại và dọn dẹp sau khi kết thúc
    private long testUserId = -1;
    private long testItemId = -1;
    private long testAuctionId = -1;

    @BeforeAll
    void initAll() {
        auctionWatchDAO = new AuctionWatchDAO();
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();
        auctionDAO = new AuctionDAO();

        long time = System.currentTimeMillis();

        try {
            // 1. Tạo Người dùng (User) mồi để theo dõi cuộc đấu giá
            UserDTO mockUser = new UserDTO();
            mockUser.setUsername("watcher_" + time);
            mockUser.setPassword("123456");
            mockUser.setEmail("watcher_" + time + "@uet.edu.vn");
            mockUser.setAccountStatus("ACTIVE");
            testUserId = userDAO.insert(mockUser);

            // 2. Tạo Món hàng (Item) mồi
            ItemDTO mockItem = new ItemDTO();
            mockItem.setName("Sản phẩm test Watch");
            mockItem.setDescription("Mô tả sản phẩm test watch");
            mockItem.setStartingPrice(100000.0);
            mockItem.setCategory("ELECTRONICS");
            testItemId = itemDAO.insert(mockItem);

            // 3. Tạo Cuộc đấu giá (Auction) mồi gắn với sản phẩm trên
            AuctionDTO mockAuction = new AuctionDTO();
            mockAuction.setItemId(testItemId);
            mockAuction.setSellerId(testUserId); // Cho tạm chính user này làm seller luôn
            mockAuction.setCurrentPrice(100000.0);
            mockAuction.setStartTime(LocalDateTime.now());
            mockAuction.setEndTime(LocalDateTime.now().plusDays(1));
            mockAuction.setStatus("RUNNING");
            testAuctionId = auctionDAO.insert(mockAuction);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Đảm bảo dây chuyền tạo dữ liệu mồi thành công 100% trước khi kiểm thử
        assertTrue(testUserId > 0 && testItemId > 0 && testAuctionId > 0,
                "Tạo chuỗi dữ liệu mồi thất bại, hủy test AuctionWatchDAO!");
    }

    @AfterAll
    void tearDownAll() {
        // Dọn sạch rác trong DB theo thứ tự ngược (Reverse Order) để tránh dính ngoại lệ Foreign Key
        try {
            // Xóa mối quan hệ trong bảng trung gian trước (nếu bước xóa ở Test 4 lỡ thất bại)
            auctionWatchDAO.removeWatch(testUserId, testAuctionId);

            // Xóa các bảng cha gốc
            if (testAuctionId > 0) auctionDAO.delete(testAuctionId);
            if (testItemId > 0) itemDAO.delete(testItemId);
            if (testUserId > 0) userDAO.delete(testUserId);
        } catch (Exception e) {
            System.out.println("Lưu ý dọn dẹp rác test watch: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test thêm cuộc đấu giá vào danh sách theo dõi (addWatch)")
    void testAddWatch() {
        Auction_watchDTO watch = new Auction_watchDTO();
        watch.setUserId(testUserId);       // Gán ID User mồi
        watch.setAuctionId(testAuctionId); // Gán ID Auction mồi
        watch.setWatchedAt(LocalDateTime.now());

        // Hàm addWatch trả về kiểu boolean
        boolean isAdded = auctionWatchDAO.addWatch(watch);
        assertTrue(isAdded, "Thêm cuộc đấu giá vào danh sách theo dõi thất bại!");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test kiểm tra trạng thái đang theo dõi (isWatching)")
    void testIsWatching() {
        // Cặp trùng khớp đã lưu ở bước 1 phải trả về true
        boolean watching = auctionWatchDAO.isWatching(testUserId, testAuctionId);
        assertTrue(watching, "Hệ thống phải báo đang theo dõi cặp User và Auction này!");

        // Một cặp ID giả không tồn tại phải trả về false
        boolean notWatching = auctionWatchDAO.isWatching(testUserId, 999999L);
        assertFalse(notWatching, "Hệ thống phải báo sai (false) với Auction ID không tồn tại!");
    }

    @Test
    @Order(3)
    @DisplayName("3. Test lấy danh sách theo dõi theo User ID")
    void testGetWatchListByUserId() {
        List<Auction_watchDTO> list = auctionWatchDAO.getWatchListByUserId(testUserId);

        assertNotNull(list, "Danh sách theo dõi không được null");
        assertFalse(list.isEmpty(), "Danh sách theo dõi không được trống");
        assertEquals(testUserId, list.get(0).getUserId(), "User ID trong danh sách không khớp!");
        assertEquals(testAuctionId, list.get(0).getAuctionId(), "Auction ID trong danh sách không khớp!");
    }

    @Test
    @Order(4)
    @DisplayName("4. Test xóa cuộc đấu giá khỏi danh sách theo dõi (removeWatch)")
    void testRemoveWatch() {
        // Thực hiện xóa khỏi danh sách theo dõi
        boolean isRemoved = auctionWatchDAO.removeWatch(testUserId, testAuctionId);
        assertTrue(isRemoved, "Xóa cuộc đấu giá khỏi danh sách theo dõi thất bại!");

        // Kiểm tra lại bằng hàm isWatching, lúc này phải trả về false
        boolean stillWatching = auctionWatchDAO.isWatching(testUserId, testAuctionId);
        assertFalse(stillWatching, "Sản phẩm vẫn hiển thị đang theo dõi sau khi đã xóa!");
    }
}