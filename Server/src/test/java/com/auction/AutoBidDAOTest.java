package com.auction;

import com.auction.common.dto.AuctionDTO;
import com.auction.common.dto.AutoBidDTO;
import com.auction.common.dto.ItemDTO;
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.*;
import server.repository.AuctionDAO;
import server.repository.AutoBidDAO;
import server.repository.ItemDAO;
import server.repository.UserDAO;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử AutoBidDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Chia sẻ các ID mồi xuyên suốt các bước chạy test
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AutoBidDAOTest {

    private AutoBidDAO autoBidDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    // Lưu trữ các ID mồi để làm khóa ngoại và dọn dẹp sạch sẽ sau khi test xong
    private long testUserId = -1;
    private long testItemId = -1;
    private long testAuctionId = -1;
    private long createdAutoBidId = -1;

    @BeforeAll
    void initAll() {
        autoBidDAO = new AutoBidDAO();
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();
        auctionDAO = new AuctionDAO();

        long time = System.currentTimeMillis();

        try {
            // 1. Mồi Người dùng (Bidder)
            UserDTO bidder = new UserDTO();
            bidder.setUsername("autobidder_" + time);
            bidder.setPassword("password123");
            bidder.setEmail("bidder_" + time + "@uet.edu.vn");
            bidder.setAccountStatus("ACTIVE");
            testUserId = userDAO.insert(bidder);

            // 2. Mồi Vật phẩm (Item)
            ItemDTO item = new ItemDTO();
            item.setName("Sản phẩm thử nghiệm AutoBid");
            item.setDescription("Hàng hóa mẫu để kiểm thử chức năng tự động trả giá");
            item.setStartingPrice(200000.0);
            item.setCategory("ELECTRONICS");
            testItemId = itemDAO.insert(item);

            // 3. Mồi Phòng đấu giá (Auction) công khai đang diễn ra
            AuctionDTO auction = new AuctionDTO();
            auction.setItemId(testItemId);
            auction.setSellerId(testUserId); // Cho tạm chính user này làm chủ phòng
            auction.setCurrentPrice(200000.0);
            auction.setStartTime(LocalDateTime.now());
            auction.setEndTime(LocalDateTime.now().plusDays(1));
            auction.setStatus("RUNNING");
            testAuctionId = auctionDAO.insert(auction);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Đảm bảo hệ thống mồi thành công chuỗi khóa ngoại, nếu không sẽ dừng bài test ngay
        assertTrue(testUserId > 0 && testItemId > 0 && testAuctionId > 0,
                "Tạo chuỗi dữ liệu mồi thất bại, hủy toàn bộ bài test AutoBidDAO!");
    }

    @AfterAll
    void tearDownAll() {
        // Dọn dẹp sạch sẽ dữ liệu rác dưới Database theo thứ tự ngược (Reverse Order)
        try {
            // Do trong AutoBidDAO của bạn không có hàm delete bản ghi cấu hình,
            // khi chúng ta xóa phòng đấu giá cha (auctionDAO.delete), MySQL/TiDB có cơ chế ON DELETE CASCADE
            // sẽ tự động quét sạch bản ghi con trong bảng auto_bid.
            if (testAuctionId > 0) auctionDAO.delete(testAuctionId);
            if (testItemId > 0) itemDAO.delete(testItemId); // Hoặc itemDAO.delete tùy cấu hình của bạn
            if (testUserId > 0) userDAO.delete(testUserId);
        } catch (Exception e) {
            System.out.println("Lưu ý dọn dẹp dữ liệu rác AutoBid: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test thêm mới cấu hình tự động đặt giá (Insert)")
    void testInsertAutoBid() {
        AutoBidDTO config = new AutoBidDTO();
        config.setAuctionId(testAuctionId);  // Gắn chính xác mã phòng đấu giá mồi
        config.setBidderId(testUserId);     // Gắn chính xác ID người mua mồi
        config.setMaxPrice(1500000.0);      // Giá tối đa sẵn sàng trả là 1tr5
        config.setStepIncrement(50000.0);   // Mỗi lần tự động tăng thêm 50k
        config.setActive(true);             // Kích hoạt ngay lập tức

        // Hàm insert của bạn trả về ID tự sinh của bản ghi dưới DB
        createdAutoBidId = autoBidDAO.insert(config);

        // Kiểm định kết quả
        assertTrue(createdAutoBidId > 0, "Lưu cấu hình AutoBid mới vào Database thất bại!");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test tìm kiếm cấu hình theo ID (findById)")
    void testFindById() {
        assertTrue(createdAutoBidId > 0, "Không tìm thấy ID cấu hình mẫu từ bước test trước!");

        AutoBidDTO retrieved = autoBidDAO.findById(createdAutoBidId);

        assertNotNull(retrieved, "Hệ thống phải tìm thấy bản ghi AutoBid tương ứng với ID vừa sinh!");
        assertEquals(testAuctionId, retrieved.getAuctionId(), "Mã phòng đấu giá không khớp!");
        assertEquals(testUserId, retrieved.getBidderId(), "Mã người đặt giá không khớp!");
        assertEquals(1500000.0, retrieved.getMaxPrice(), "Giá tối đa cấu hình bị sai lệch!");
        assertEquals(50000.0, retrieved.getStepIncrement(), "Bước giá tăng cấu hình bị sai lệch!");
        assertTrue(retrieved.isActive(), "Trạng thái hoạt động ban đầu phải là true!");
    }

    @Test
    @Order(3)
    @DisplayName("3. Test lấy danh sách các cấu hình đang hoạt động của một phòng đấu giá")
    void testGetActiveConfigsForAuction() {
        List<AutoBidDTO> activeConfigs = autoBidDAO.getActiveConfigsForAuction(testAuctionId);

        assertNotNull(activeConfigs, "Danh sách cấu hình trả về không được phép null!");
        assertFalse(activeConfigs.isEmpty(), "Danh sách cấu hình hoạt động không được trống!");
        assertEquals(1, activeConfigs.size(), "Số lượng cấu hình hoạt động trong phòng không khớp!");
        assertEquals(createdAutoBidId, activeConfigs.get(0).getId(), "ID cấu hình lấy ra từ danh sách bị sai!");
    }

    @Test
    @Order(4)
    @DisplayName("4. Test cập nhật trạng thái tắt/bật tính năng tự động đặt giá (updateActiveStatus)")
    void testUpdateActiveStatus() {
        assertTrue(createdAutoBidId > 0, "Không tìm thấy ID cấu hình mẫu!");

        // Thực hiện hủy/tắt tính năng AutoBid (chuyển active từ true -> false)
        boolean isUpdated = autoBidDAO.updateActiveStatus(createdAutoBidId, false);
        assertTrue(isUpdated, "Cập nhật trạng thái hoạt động của cấu hình AutoBid thất bại!");

        // Đọc lại từ DB lên để đối chứng trực tiếp
        AutoBidDTO afterUpdate = autoBidDAO.findById(createdAutoBidId);
        assertNotNull(afterUpdate);
        assertFalse(afterUpdate.isActive(), "Trạng thái hoạt động dưới DB chưa được cập nhật thành false!");

        // Đảm bảo sau khi tắt, cấu hình này không còn xuất hiện trong danh sách ActiveConfigs nữa
        List<AutoBidDTO> activeConfigs = autoBidDAO.getActiveConfigsForAuction(testAuctionId);
        assertTrue(activeConfigs.isEmpty(), "Cấu hình đã tắt nhưng vẫn xuất hiện trong danh sách hoạt động!");
    }
}