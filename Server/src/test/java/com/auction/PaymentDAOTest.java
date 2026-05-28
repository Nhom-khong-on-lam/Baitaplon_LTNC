package com.auction;

import com.auction.common.dto.AuctionDTO;
import com.auction.common.dto.ItemDTO;
import com.auction.common.dto.PaymentDTO;
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.*;
import server.repository.AuctionDAO;
import server.repository.ItemDAO;
import server.repository.PaymentDAO;
import server.repository.UserDAO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử PaymentDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Để chia sẻ ID mồi giữa các hàm @Test
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentDAOTest {

    private PaymentDAO paymentDAO;
    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private AuctionDAO auctionDAO;

    // Lưu các ID mồi để làm khóa ngoại và dọn dẹp sau khi test xong
    private long sellerId = -1;
    private long buyerId = -1;
    private long itemId = -1;
    private long auctionId = -1;

    @BeforeAll
    void initAll() {
        paymentDAO = new PaymentDAO();
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();
        auctionDAO = new AuctionDAO();

        long time = System.currentTimeMillis();

        try {
            // 1. Tạo Người bán (Seller) và Người mua (Buyer) mồi
            UserDTO seller = new UserDTO();
            seller.setUsername("seller_pay_" + time);
            seller.setPassword("123456");
            seller.setEmail("seller_pay_" + time + "@gmail.com");
            sellerId = userDAO.insert(seller);

            UserDTO buyer = new UserDTO();
            buyer.setUsername("buyer_pay_" + time);
            buyer.setPassword("123456");
            buyer.setEmail("buyer_pay_" + time + "@gmail.com");
            buyerId = userDAO.insert(buyer);

            // 2. Tạo Món hàng (Item) mồi thuộc về Người bán
            ItemDTO item = new ItemDTO();
            item.setName("Sản phẩm Test Hóa Đơn");
            item.setDescription("Mô tả sản phẩm test payment");
            item.setStartingPrice(500000.0);
            item.setCategory("ELECTRONICS");
            itemId = itemDAO.insert(item);

            // 3. Tạo Cuộc đấu giá (Auction) mồi kết nối Item và Seller
            AuctionDTO auction = new AuctionDTO();
            auction.setItemId(itemId);
            auction.setSellerId(sellerId);
            auction.setCurrentPrice(550000.0);
            auction.setStartTime(LocalDateTime.now());
            auction.setEndTime(LocalDateTime.now().plusDays(1));
            auction.setStatus("FINISHED"); // Thường đấu giá kết thúc mới thanh toán
            auctionId = auctionDAO.insert(auction);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Kiểm tra xem dây chuyền tạo data mồi có thành công hoàn toàn không
        assertTrue(sellerId > 0 && buyerId > 0 && itemId > 0 && auctionId > 0,
                "Tạo chuỗi dữ liệu mồi thất bại, không thể test PaymentDAO!");
    }

    @AfterAll
    void tearDownAll() {
        // Dọn sạch rác trong DB theo thứ tự ngược lại để tránh lỗi Foreign Key
        try {
            if (auctionId > 0) auctionDAO.delete(auctionId);
            if (itemId > 0) itemDAO.delete(itemId);
            if (sellerId > 0) userDAO.delete(sellerId);
            if (buyerId > 0) userDAO.delete(buyerId);
        } catch (Exception e) {
            System.out.println("Lưu ý dọn dẹp rác test payment: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test thêm hóa đơn mới (Insert thông thường)")
    void testInsertPayment() {
        PaymentDTO payment = new PaymentDTO();
        payment.setAuctionId(auctionId);
        payment.setBuyerId(buyerId);
        payment.setSellerId(sellerId);
        payment.setAmount(550000.0);

        // Hãy chú ý chữ "PENDING" này, nếu DB của bạn dùng ENUM viết hoa thì giữ nguyên,
        // nếu bị lỗi truncated giống file trước thì đổi thành trạng thái hợp lệ trong DB của bạn nhé!
        payment.setStatus("PENDING");

        boolean isInserted = paymentDAO.insert(payment);
        assertTrue(isInserted, "Thêm hóa đơn thanh toán thông thường thất bại!");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test lấy thông tin hóa đơn theo Auction ID")
    void testGetByAuctionId() {
        PaymentDTO retrieved = paymentDAO.getByAuctionId(auctionId);

        assertNotNull(retrieved, "Phải tìm thấy hóa đơn gắn với Auction ID vừa tạo!");
        assertEquals(buyerId, retrieved.getBuyerId(), "ID Người mua không khớp!");
        assertEquals(sellerId, retrieved.getSellerId(), "ID Người bán không khớp!");
        assertEquals(550000.0, retrieved.getAmount(), "Số tiền hóa đơn không khớp!");
        assertEquals("PENDING", retrieved.getStatus(), "Trạng thái hóa đơn ban đầu không khớp!");
    }
}