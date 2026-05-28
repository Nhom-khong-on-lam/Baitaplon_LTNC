package com.auction;

import com.auction.common.dto.AuctionDTO;
import com.auction.common.dto.Auction_extension_logDTO;
import com.auction.common.dto.ItemDTO;
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.*;
import server.repository.AuctionDAO;
import server.repository.AuctionExtensionLogDAO;
import server.repository.ItemDAO;
import server.repository.UserDAO;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử AuctionExtensionLogDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Giúp chia sẻ ID cuộc đấu giá mồi xuyên suốt các hàm test
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionExtensionLogDAOTest {

    private AuctionExtensionLogDAO extensionLogDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    // Lưu các ID dữ liệu mồi để làm khóa ngoại và dọn dẹp rác sau khi kết thúc
    private long testUserId = -1;
    private long testItemId = -1;
    private long testAuctionId = -1;

    // Khởi tạo sẵn mốc thời gian cố định để dễ assert so sánh logic tính phút
    private final LocalDateTime originalEnd = LocalDateTime.now().plusHours(2);
    private final LocalDateTime newEnd = originalEnd.plusMinutes(15); // Gia hạn thêm 15 phút

    @BeforeAll
    void initAll() {
        extensionLogDAO = new AuctionExtensionLogDAO();
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();
        auctionDAO = new AuctionDAO();

        long time = System.currentTimeMillis();

        try {
            // 1. Tạo User mồi
            UserDTO mockUser = new UserDTO();
            mockUser.setUsername("ext_owner_" + time);
            mockUser.setPassword("123456");
            mockUser.setEmail("ext_" + time + "@uet.edu.vn");
            testUserId = userDAO.insert(mockUser);

            // 2. Tạo Item mồi
            ItemDTO mockItem = new ItemDTO();
            mockItem.setName("Sản phẩm test gia hạn");
            mockItem.setDescription("Mô tả sản phẩm test extension log");
            mockItem.setStartingPrice(200000.0);
            mockItem.setCategory("ELECTRONICS");
            testItemId = itemDAO.insert(mockItem);

            // 3. Tạo Auction mồi
            AuctionDTO mockAuction = new AuctionDTO();
            mockAuction.setItemId(testItemId);
            mockAuction.setSellerId(testUserId);
            mockAuction.setCurrentPrice(200000.0);
            mockAuction.setStartTime(LocalDateTime.now());
            mockAuction.setEndTime(originalEnd);
            mockAuction.setStatus("RUNNING");
            testAuctionId = auctionDAO.insert(mockAuction);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Đảm bảo dây chuyền tạo khóa ngoại gốc thành công 100% trước khi kiểm thử
        assertTrue(testUserId > 0 && testItemId > 0 && testAuctionId > 0,
                "Tạo chuỗi dữ liệu mồi thất bại, hủy test AuctionExtensionLogDAO!");
    }

    @AfterAll
    void tearDownAll() {
        // Dọn sạch rác trong DB theo thứ tự ngược (Reverse Order) để tránh dính ngoại lệ Foreign Key
        try {
            // Bảng cha 'auction' bị xóa thì các bản ghi liên quan trong bảng 'auction_extension_log'
            // thường sẽ tự động mất nếu có cấu hình ON DELETE CASCADE. Nếu không, ta cứ yên tâm xóa các bảng cha gốc.
            if (testAuctionId > 0) auctionDAO.delete(testAuctionId);
            if (testItemId > 0) itemDAO.delete(testItemId);
            if (testUserId > 0) userDAO.delete(testUserId);
        } catch (Exception e) {
            System.out.println("Lưu ý dọn dẹp rác test extension log: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test ghi lịch sử gia hạn mới (insertLog)")
    void testInsertLog() {
        Auction_extension_logDTO log = new Auction_extension_logDTO();
        log.setAuctionId(testAuctionId); // Gắn đúng mã cuộc đấu giá mồi
        log.setOriginalEndTime(originalEnd);
        log.setNewEndTime(newEnd);

        // Hàm insertLog trong DAO của bạn trả về kiểu boolean
        boolean isInserted = extensionLogDAO.insertLog(log);
        assertTrue(isInserted, "Ghi lịch sử gia hạn đấu giá vào database thất bại!");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test lấy danh sách lịch sử gia hạn theo Auction ID")
    void testFindByAuctionId() {
        List<Auction_extension_logDTO> logList = extensionLogDAO.findByAuctionId(testAuctionId);

        assertNotNull(logList, "Danh sách lịch sử gia hạn không được null");
        assertFalse(logList.isEmpty(), "Danh sách lịch sử không được trống sau khi đã chèn");
        assertEquals(testAuctionId, logList.get(0).getAuctionId(), "Auction ID lấy ra không khớp!");

        // Kiểm tra xem mốc thời gian lấy lên từ DB có khớp chính xác với mốc ta chèn ở bước 1 không
        assertNotNull(logList.get(0).getOriginalEndTime());
        assertNotNull(logList.get(0).getNewEndTime());
    }

    @Test
    @Order(3)
    @DisplayName("3. Test tính tổng số phút đã gia hạn (getTotalExtendedMinutes)")
    void testGetTotalExtendedMinutes() {
        // Ở bước 1, ta chèn một bản ghi chênh lệch giữa originalEnd và newEnd đúng bằng 15 phút.
        long totalMinutes = extensionLogDAO.getTotalExtendedMinutes(testAuctionId);

        assertEquals(15, totalMinutes, "Tổng số phút hệ thống tính toán gia hạn bị sai lệch!");
    }
}