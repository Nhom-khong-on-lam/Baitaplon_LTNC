package com.auction;

import com.auction.common.dto.AuctionDTO;
import com.auction.common.dto.BidDTO;
import com.auction.common.dto.ItemDTO;
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.*;
import server.repository.AuctionDAO;
import server.repository.BidDAO;
import server.repository.ItemDAO;
import server.repository.UserDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử BidDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Chia sẻ ID thực thể mồi giữa các phương thức kiểm thử
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BidDAOTest {

    private BidDAO bidDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    // Lưu trữ các ID mồi để làm khóa ngoại liên kết dữ liệu
    private long bidderId = -1;
    private long itemId = -1;
    private long auctionId = -1;
    private long createdBidId = -1;

    @BeforeAll
    void initAll() {
        bidDAO = new BidDAO();
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();
        auctionDAO = new AuctionDAO();

        long time = System.currentTimeMillis();

        try {
            // 1. Tạo Người đặt giá (Bidder) mồi
            UserDTO bidder = new UserDTO();
            bidder.setUsername("bidder_test_" + time);
            bidder.setPassword("password_bid");
            bidder.setEmail("bidder_" + time + "@uet.edu.vn");
            bidder.setAccountStatus("ACTIVE");
            bidderId = userDAO.insert(bidder);

            // 2. Tạo Vật phẩm (Item) mồi
            ItemDTO item = new ItemDTO();
            item.setName("Sản phẩm thử nghiệm Đặt Giá");
            item.setDescription("Hàng hóa mẫu để kiểm thử chức năng trả giá");
            item.setStartingPrice(300000.0);
            item.setCategory("ELECTRONICS");
            itemId = itemDAO.insert(item);

            // 3. Tạo Phòng đấu giá (Auction) mồi
            AuctionDTO auction = new AuctionDTO();
            auction.setItemId(itemId);
            auction.setSellerId(bidderId); // Cho tạm bidder làm seller để tinh giản dữ liệu mồi
            auction.setCurrentPrice(300000.0);
            auction.setStartTime(LocalDateTime.now());
            auction.setEndTime(LocalDateTime.now().plusDays(1));
            auction.setStatus("RUNNING");
            auctionId = auctionDAO.insert(auction);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Kiểm tra điều kiện tiên quyết: Dây chuyền tạo khóa ngoại gốc bắt buộc phải thành công
        assertTrue(bidderId > 0 && itemId > 0 && auctionId > 0,
                "Khởi tạo chuỗi dữ liệu mồi thất bại, hủy toàn bộ bài test BidDAO!");
    }

    @AfterAll
    void tearDownAll() {
        // Dọn dẹp sạch sẽ dữ liệu rác dưới DB theo thứ tự ngược để tránh xung đột Foreign Key
        try {
            // Xóa toàn bộ các lượt cược của phòng đấu giá này trước
            bidDAO.deleteBidsByAuctionId(auctionId);

            // Xóa các bảng cha gốc
            if (auctionId > 0) auctionDAO.delete(auctionId);
            if (itemId > 0) itemDAO.delete(itemId); // Hoặc itemDAO.delete tùy cấu hình của bạn
            if (bidderId > 0) userDAO.delete(bidderId);
        } catch (Exception e) {
            System.out.println("Lưu ý khi dọn dẹp dữ liệu rác đấu giá: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test thêm mới một lượt đặt giá (Insert)")
    void testInsertBid() {
        BidDTO bid = new BidDTO();
        bid.setAuctionId(auctionId); // Gắn chính xác mã phòng đấu giá mồi
        bid.setBidderId(bidderId);   // Gắn chính xác ID người đặt giá mồi
        bid.setAmount(350000.0);     // Giá trả cao hơn giá khởi điểm (300k)
        bid.setBidTime(LocalDateTime.now());
        bid.setAutoBid(false);       // Đặt giá thủ công thông thường

        // Hàm insert của bạn trả về ID tự sinh kiểu long
        createdBidId = bidDAO.insert(bid);

        assertTrue(createdBidId > 0, "Lưu lượt đặt giá mới vào database thất bại!");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test lấy lịch sử đặt giá của một phiên (getBidsByAuctionId)")
    void testGetBidsByAuctionId() {
        List<BidDTO> history = bidDAO.getBidsByAuctionId(auctionId);

        assertNotNull(history, "Danh sách lịch sử đặt giá không được null!");
        assertFalse(history.isEmpty(), "Danh sách lịch sử đặt giá không được rỗng!");
        assertEquals(1, history.size(), "Số lượng lượt đặt giá trong phiên không khớp!");
        assertEquals(bidderId, history.get(0).getBidderId(), "ID người đặt giá không khớp!");
        assertEquals(350000.0, history.get(0).getAmount(), "Số tiền đặt giá không khớp!");

        // Kiểm tra xem hàm có LEFT JOIN thành công để lấy Username từ bảng User không
        assertNotNull(history.get(0).getBidderName(), "Hàm chưa lấy được tên hiển thị của người đấu giá!");
        assertNotEquals("Unknown User", history.get(0).getBidderName(), "Lỗi ánh xạ tên người dùng!");
    }

    @Test
    @Order(3)
    @DisplayName("3. Test đếm số lần trả giá (countByAuctionId & countAllGroupedByAuction)")
    void testCountBids() {
        // 3.1 Test đếm đơn lẻ theo Auction ID
        int totalBids = bidDAO.countByAuctionId(auctionId);
        assertEquals(1, totalBids, "Tổng số lượt đặt giá tính toán đơn lẻ bị sai lệch!");

        // 3.2 Test đếm gộp nhóm tất cả các phiên đấu giá cùng lúc
        Map<Long, Integer> groupedMap = bidDAO.countAllGroupedByAuction();
        assertNotNull(groupedMap);
        assertTrue(groupedMap.containsKey(auctionId), "Bản đồ đếm gộp nhóm phải chứa ID phòng đấu giá mồi!");
        assertEquals(1, groupedMap.get(auctionId), "Số lượng lượt trả giá trong bản đồ nhóm bị sai lệch!");
    }

    @Test
    @Order(4)
    @DisplayName("4. Test lấy danh sách ID những người đã tham gia trả giá công khai (getDistinctBiddersByAuction)")
    void testGetDistinctBiddersByAuction() {
        List<Long> bidderIds = bidDAO.getDistinctBiddersByAuction(auctionId);

        assertNotNull(bidderIds);
        assertFalse(bidderIds.isEmpty());
        assertEquals(1, bidderIds.size(), "Số lượng người tham gia trả giá duy nhất bị sai!");
        assertEquals(bidderId, bidderIds.get(0), "ID người tham gia đấu giá lấy ra không trùng khớp!");
    }

    @Test
    @Order(5)
    @DisplayName("5. Test xóa toàn bộ lượt đặt giá của phòng đấu giá (deleteBidsByAuctionId)")
    void testDeleteBidsByAuctionId() {
        // Thực hiện xóa toàn bộ lịch sử đặt giá của phòng mồi
        boolean isDeleted = bidDAO.deleteBidsByAuctionId(auctionId);
        assertTrue(isDeleted, "Hàm thực thi xóa lịch sử đặt giá trả về giá trị thất bại!");

        // Đối chứng lại bằng hàm đếm, số lượng lúc này phải trả về bằng 0
        int afterDeletedCount = bidDAO.countByAuctionId(auctionId);
        assertEquals(0, afterDeletedCount, "Lịch sử đặt giá vẫn còn tồn tại dưới DB sau khi đã gọi lệnh hủy!");
    }
}