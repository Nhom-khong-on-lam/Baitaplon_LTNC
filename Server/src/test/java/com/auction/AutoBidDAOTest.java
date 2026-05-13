package com.auction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.common.model.*;
import server.repository.*;
import server.database.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidDAOTest {

    private AutoBidDAO autoBidDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    private long sellerId;
    private long bidderId;
    private long itemId;
    private long auctionId;

    @BeforeEach
    void setUp() {
        autoBidDAO = new AutoBidDAO();
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();
        auctionDAO = new AuctionDAO();

        long time = System.currentTimeMillis();

        // 1. Tạo 2 User mồi
        UserDTO seller = new UserDTO();
        seller.setUsername("seller_auto_" + time);
        seller.setPassword("123456");
        seller.setEmail("seller_auto@uet.edu.vn");
        sellerId = userDAO.insert(seller);

        UserDTO bidder = new UserDTO();
        bidder.setUsername("bidder_auto_" + time);
        bidder.setPassword("123456");
        bidder.setEmail("bidder_auto@uet.edu.vn");
        bidderId = userDAO.insert(bidder);

        // 2. Tạo Item mồi
        ItemDTO item = new ItemDTO();
        item.setName("Laptop AutoBid Test");
        item.setDescription("Hàng test đấu giá");
        item.setStartingPrice(1000.0);
        item.setCategory("ELECTRONICS");
        item.setBrandMake("Dell");
        item.setModel("XPS");
        item.setProductionYear(2024);
        itemId = itemDAO.insert(item);

        // 3. Tạo Auction mồi
        AuctionDTO auction = new AuctionDTO();
        auction.setItemId(itemId);
        auction.setSellerId(sellerId);
        auction.setCurrentPrice(1000.0);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(2));
        auction.setStatus("RUNNING");
        auctionId = auctionDAO.insert(auction);

        assertTrue(auctionId > 0, "Phải setup được Auction mồi để test AutoBid");
    }

    @AfterEach
    void tearDown() {
        // Dọn dẹp Database (Reverse Order)
//        try (Connection conn = DBConnection.getConnection()) {
//            conn.createStatement().executeUpdate("DELETE FROM auto_bid WHERE auction_id = " + auctionId);
//            conn.createStatement().executeUpdate("DELETE FROM auction WHERE id = " + auctionId);
//            conn.createStatement().executeUpdate("DELETE FROM item WHERE id = " + itemId);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//        userDAO.delete(sellerId);
//        userDAO.delete(bidderId);
    }

    @Test
    void testInsertAndToggleActive() {
        // Arrange: Thiết lập tự động đấu giá
        AutoBidDTO autoBid = new AutoBidDTO();
        autoBid.setAuctionId(auctionId);
        autoBid.setBidderId(bidderId);
        autoBid.setMaxPrice(5000.0); // Giá cao nhất sẵn sàng trả
        autoBid.setStepIncrement(100.0);  // Mỗi lần tự nâng giá thêm 100
        autoBid.setActive(true);
        autoBid.setRegisteredAt(LocalDateTime.now());

        // Act: Lưu vào DB
        long id = autoBidDAO.insert(autoBid);
        assertTrue(id > 0, "Lưu AutoBid phải thành công");

        // Act: Thử tắt chế độ tự động đấu giá
        boolean deactivated = autoBidDAO.updateActiveStatus(id, false);
        assertTrue(deactivated, "Phải cập nhật được trạng thái active");

        // Assert: Kiểm tra lại trong DB
        AutoBidDTO retrieved = autoBidDAO.findById(id);
        assertNotNull(retrieved);
        assertFalse(retrieved.isActive(), "Trạng thái active phải là false sau khi update");
    }
}