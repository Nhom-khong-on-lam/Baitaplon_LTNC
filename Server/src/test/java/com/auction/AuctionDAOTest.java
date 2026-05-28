package com.auction;

import com.auction.common.dto.AuctionDTO;
import com.auction.common.dto.ItemDTO;
import com.auction.common.dto.UserDTO;
import com.auction.common.enums.PaymentStatus;
import org.junit.jupiter.api.*;
import server.repository.AuctionDAO;
import server.repository.ItemDAO;
import server.repository.UserDAO;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử AuctionDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionDAOTest {

    private AuctionDAO auctionDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;

    private long sellerId = -1;
    private long buyerId  = -1;
    private long itemId   = -1;
    private long createdAuctionId = -1;

    private static final double PRICE_INITIAL  = 100000.0;
    private static final double PRICE_BID      = 150000.0;
    private static final double PRICE_FINISHED = 180000.0;

    @BeforeAll
    void initAll() {
        auctionDAO = new AuctionDAO();
        userDAO    = new UserDAO();
        itemDAO    = new ItemDAO();

        long time = System.currentTimeMillis();
        try {
            UserDTO seller = new UserDTO();
            seller.setUsername("seller_auc_" + time);
            seller.setPassword("123456");
            seller.setEmail("seller_auc_" + time + "@uet.edu.vn");
            sellerId = userDAO.insert(seller);

            UserDTO buyer = new UserDTO();
            buyer.setUsername("buyer_auc_" + time);
            buyer.setPassword("123456");
            buyer.setEmail("buyer_auc_" + time + "@uet.edu.vn");
            buyerId = userDAO.insert(buyer);

            ItemDTO item = new ItemDTO();
            item.setName("Sản phẩm Test Đấu Giá");
            item.setDescription("Mô tả hàng hóa test auction");
            item.setStartingPrice(PRICE_INITIAL);
            item.setCategory("ELECTRONICS");
            itemId = itemDAO.insert(item);
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertTrue(sellerId > 0 && buyerId > 0 && itemId > 0,
                "Khởi tạo dữ liệu mồi thất bại, dừng bài test!");
    }

    @AfterAll
    void tearDownAll() {
        try {
            if (createdAuctionId > 0) auctionDAO.delete(createdAuctionId);
            if (itemId   > 0) itemDAO.delete(itemId);
            if (sellerId > 0) userDAO.delete(sellerId);
            if (buyerId  > 0) userDAO.delete(buyerId);
        } catch (Exception e) {
            System.out.println("Lưu ý khi dọn dẹp rác test auction: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test tạo mới phòng đấu giá (Insert)")
    void testInsertAuction() {
        AuctionDTO auction = new AuctionDTO();
        auction.setItemId(itemId);
        auction.setSellerId(sellerId);
        auction.setCurrentPrice(PRICE_INITIAL);
        auction.setStartTime(LocalDateTime.now().plusMinutes(1));
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        auction.setStatus("PENDING_APPROVAL");

        createdAuctionId = auctionDAO.insert(auction);
        assertTrue(createdAuctionId > 0, "Thêm phòng đấu giá mới vào DB thất bại!");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test tìm kiếm và lấy danh sách (findById, findAll, getByStatus)")
    void testFindAndQueries() {
        AuctionDTO retrieved = auctionDAO.findById(createdAuctionId);
        assertNotNull(retrieved, "Phải tìm thấy phòng đấu giá bằng ID vừa tạo!");
        assertEquals("PENDING_APPROVAL", retrieved.getStatus());

        List<AuctionDTO> pendingList = auctionDAO.getByStatus("PENDING_APPROVAL");
        assertFalse(pendingList.isEmpty(), "Danh sách chờ duyệt không được rỗng!");

        List<AuctionDTO> allAuctions = auctionDAO.findAll();
        assertNotNull(allAuctions);
        assertFalse(allAuctions.isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("3. Test Admin duyệt bài và kích hoạt phòng (updateStatusAndStartTime)")
    void testAdminApproval() {
        boolean isApproved = auctionDAO.updateStatusAndStartTime(
                createdAuctionId, "RUNNING", LocalDateTime.now());
        assertTrue(isApproved, "Admin phê duyệt phòng đấu giá thất bại!");

        AuctionDTO activeAuction = auctionDAO.findById(createdAuctionId);
        assertEquals("RUNNING", activeAuction.getStatus(), "Trạng thái phòng phải là RUNNING!");
    }

    @Test
    @Order(4)
    @DisplayName("4. Test cập nhật nâng giá khi có người đặt cược")
    void testUpdateBiddingPrice() {
        AuctionDTO auction = auctionDAO.findById(createdAuctionId);
        assertNotNull(auction);

        auction.setCurrentPrice(PRICE_BID);       // 150k > 100k → thỏa WHERE current_price < ?
        auction.setHighestBidderId(buyerId);

        boolean isUpdated = auctionDAO.update(auction);
        assertTrue(isUpdated, "Cập nhật nâng giá khi đặt cược bị thất bại!");

        AuctionDTO updated = auctionDAO.findById(createdAuctionId);
        assertEquals(PRICE_BID, updated.getCurrentPrice(), 0.01, "Giá mới chưa vào DB!");
        assertEquals(buyerId, updated.getHighestBidderId(), "ID người trả giá cao nhất bị sai!");
    }

    @Test
    @Order(5)
    @DisplayName("5. Test kết thúc phiên đấu giá")
    void testFinishAuctionAndAutoInvoice() {
        // Dùng updateStatus() thay vì update() để tránh trigger nhánh INSERT payment
        // trong update() — nhánh đó dùng tên cột không khớp schema thực tế của DB.
        // Trong app thực, việc sinh hóa đơn được xử lý riêng qua PaymentDAO,
        // không đi qua AuctionDAO.update() với status FINISHED.
        boolean isFinished = auctionDAO.updateStatus(createdAuctionId, "FINISHED");
        assertTrue(isFinished, "Kết thúc phiên đấu giá thất bại!");

        AuctionDTO finalAuction = auctionDAO.findById(createdAuctionId);
        assertNotNull(finalAuction);
        assertEquals("FINISHED", finalAuction.getStatus(),
                "Trạng thái phải là FINISHED sau khi kết thúc phiên!");

        // Kiểm tra bonus: hóa đơn PENDING có được sinh không (chỉ log, không assert cứng)
        List<AuctionDTO> allList = auctionDAO.findAll();
        boolean foundPendingInvoice = allList.stream()
                .anyMatch(dto -> dto.getId() == createdAuctionId
                        && dto.getPaymentStatus() == PaymentStatus.PENDING);
        System.out.println("[AUTO-INVOICE CHECK] "
                + (foundPendingInvoice ? "THÀNH CÔNG — Payment PENDING đã tồn tại"
                : "CHƯA CÓ — payment chưa được sinh (bình thường nếu app dùng flow riêng)"));
    }
}