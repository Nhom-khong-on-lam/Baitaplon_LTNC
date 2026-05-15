package com.auction;

import com.auction.common.dto.AuctionDTO;
import com.auction.common.dto.BidDTO;
import com.auction.common.dto.ItemDTO;
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.repository.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BidDAOTest {

    private BidDAO bidDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    private long sellerId;
    private long bidderId;
    private String bidderUsername;
    private long itemId;
    private long auctionId;

    @BeforeEach
    void setUp() {
        bidDAO = new BidDAO();
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();
        auctionDAO = new AuctionDAO();

        long time = System.currentTimeMillis();

        // 1. Tạo Seller mồi
        UserDTO seller = new UserDTO();
        seller.setUsername("seller_bid_" + time);
        seller.setPassword("123456"); // Rút kinh nghiệm, luôn có password
        seller.setEmail("seller_bid@uet.edu.vn");
        sellerId = userDAO.insert(seller);

        // 2. Tạo Bidder mồi
        UserDTO bidder = new UserDTO();
        bidderUsername = "bidder_bid_" + time;
        bidder.setUsername(bidderUsername);
        bidder.setPassword("123456");
        bidder.setEmail("bidder_bid@uet.edu.vn");
        bidderId = userDAO.insert(bidder);

        // 3. Tạo Item mồi
        ItemDTO item = new ItemDTO();
        item.setName("Laptop Bid Test");
        item.setDescription("Hàng test trả giá");
        item.setStartingPrice(1000.0);
        item.setCategory("ELECTRONICS");
        item.setBrandMake("Asus");
        item.setModel("ROG");
        item.setProductionYear(2024); // Đủ trường để không dính NullPointerException
        itemId = itemDAO.insert(item);

        // 4. Tạo Auction mồi
        AuctionDTO auction = new AuctionDTO();
        auction.setItemId(itemId);
        auction.setSellerId(sellerId);
        auction.setCurrentPrice(1000.0);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(2));
        auction.setStatus("RUNNING");
        auctionId = auctionDAO.insert(auction);

        assertTrue(auctionId > 0, "Setup Auction thất bại");
    }

    @AfterEach
    void tearDown() {
        // Dọn dẹp rác sau khi Test xong (Xóa từ ngọn xuống gốc)
//        try (Connection conn = DBConnection.getConnection()) {
//            conn.createStatement().executeUpdate("DELETE FROM bid WHERE auction_id = " + auctionId);
//            conn.createStatement().executeUpdate("DELETE FROM auction WHERE id = " + auctionId);
//            conn.createStatement().executeUpdate("DELETE FROM item WHERE id = " + itemId);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        userDAO.delete(sellerId);
//        userDAO.delete(bidderId);
    }

    @Test
    @DisplayName("Test Insert Bid và lấy danh sách Bid theo Auction")
    void testInsertAndGetBids() {
        // --- ACT 1: Insert Bid thứ nhất (Trả giá thủ công) ---
        BidDTO bid1 = new BidDTO();
        bid1.setAuctionId(auctionId);
        bid1.setBidderId(bidderId);
        bid1.setAmount(1200.0);
        bid1.setBidTime(LocalDateTime.now().minusMinutes(5));
        bid1.setAutoBid(false);

        long bid1Id = bidDAO.insert(bid1);
        assertTrue(bid1Id > 0, "Thêm Bid 1 phải thành công");

        // --- ACT 2: Insert Bid thứ hai (Tự động trả giá) ---
        BidDTO bid2 = new BidDTO();
        bid2.setAuctionId(auctionId);
        bid2.setBidderId(bidderId);
        bid2.setAmount(1500.0);
        bid2.setBidTime(LocalDateTime.now());
        bid2.setAutoBid(true);

        long bid2Id = bidDAO.insert(bid2);
        assertTrue(bid2Id > 0, "Thêm Bid 2 phải thành công");

        // --- ASSERT: Lấy danh sách và kiểm tra ---
        List<BidDTO> history = bidDAO.getBidsByAuctionId(auctionId);

        // Phải có đúng 2 lượt trả giá
        assertNotNull(history);
        assertEquals(2, history.size(), "Lịch sử phải chứa đúng 2 lượt bid");

        // Vì lấy ra theo ORDER BY bid_time DESC, lượt mới nhất (bid2) phải nằm đầu tiên
        BidDTO newestBid = history.get(0);
        assertEquals(1500.0, newestBid.getAmount(), 0.001);
        assertTrue(newestBid.isAutoBid());

        // Kiểm tra xem BidDAO đã lấy được Username của Bidder chưa (Logic join/lookup)
        assertEquals(bidderUsername, newestBid.getBidderName(), "DAO phải map được Username của người trả giá");
    }
}