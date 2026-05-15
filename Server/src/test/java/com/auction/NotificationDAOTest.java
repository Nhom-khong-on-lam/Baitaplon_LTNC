package com.auction;

import com.auction.common.dto.NotificationDTO;
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.repository.NotificationDAO;
import server.repository.UserDAO;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NotificationDAOTest {

    private NotificationDAO notificationDAO;
    private UserDAO userDAO;
    private long validUserId; // Dùng để hứng ID thật từ Database

    @BeforeEach
    void setUp() {
        notificationDAO = new NotificationDAO();
        userDAO = new UserDAO();

        // 1. TẠO USER TẠM THỜI ĐỂ VƯỢ QUA LỖI FOREIGN KEY CỦA MYSQL
        UserDTO tempUser = new UserDTO();
        tempUser.setUsername("test_notif_user_" + System.currentTimeMillis()); // Thêm timestamp để tên không bị trùng
        tempUser.setPassword("123456");
        tempUser.setEmail("testnotif@uet.edu.vn");

        validUserId = userDAO.insert(tempUser);
        assertTrue(validUserId > 0, "Phải tạo được User tạm thời để test Notification");
    }

    @AfterEach
    void tearDown() {
        // 2. DỌN DẸP SẠCH SẼ (Nhờ ON DELETE CASCADE, xóa User sẽ tự động bay luôn các Notification của test này)
        userDAO.delete(validUserId);
    }

    @Test
    void testInsertAndFindByUserId() {
        // Arrange: Chuẩn bị dữ liệu (Test tạo thông báo có related_auction_id và không có related_bid_id)
        NotificationDTO newNotif = new NotificationDTO(
                validUserId,
                "Đấu giá thành công",
                "Bạn đã thắng phiên đấu giá siêu xe",
                "AUCTION_WON",
                null,
                null  // related_bid_id = null
        );

        // Act: Lưu xuống DB
        long newId = notificationDAO.insert(newNotif);
        assertTrue(newId > 0, "Lưu thông báo thất bại, ID phải > 0");

        // Act: Đọc lên từ DB
        List<NotificationDTO> userNotifs = notificationDAO.findByUserId(validUserId);

        // Assert: Kiểm tra tính đúng đắn
        assertFalse(userNotifs.isEmpty(), "Danh sách thông báo không được rỗng");

        NotificationDTO retrievedNotif = userNotifs.get(0);
        assertEquals("Đấu giá thành công", retrievedNotif.getTitle());
        assertEquals("AUCTION_WON", retrievedNotif.getType());
        assertFalse(retrievedNotif.isRead(), "Thông báo mới tạo mặc định phải là chưa đọc (false)");
        assertNull(retrievedNotif.getRelatedAuctionId(), "related_auction_id phải là null");
        assertNull(retrievedNotif.getRelatedBidId(), "related_bid_id phải là null");
    }

    @Test
    void testMarkAsRead() {
        // 1. Tạo và lưu một thông báo
        NotificationDTO notif = new NotificationDTO(validUserId, "Test Read", "Message", "SYSTEM", null, null);
        long notifId = notificationDAO.insert(notif);

        // 2. Đánh dấu đã đọc
        boolean isMarked = notificationDAO.markAsRead(notifId);
        assertTrue(isMarked, "Lệnh update trạng thái đã đọc phải trả về true");

        // 3. Lấy lên và kiểm tra lại trạng thái
        List<NotificationDTO> notifs = notificationDAO.findByUserId(validUserId);
        assertTrue(notifs.get(0).isRead(), "Trạng thái is_read phải được đổi thành true trong DB");
    }

    @Test
    void testMarkAllAsRead() {
        // 1. Tạo 2 thông báo mới (cả 2 đều chưa đọc)
        notificationDAO.insert(new NotificationDTO(validUserId, "TB 1", "Noi dung 1", "SYSTEM", null, null));
        notificationDAO.insert(new NotificationDTO(validUserId, "TB 2", "Noi dung 2", "SYSTEM", null, null));

        // 2. Đánh dấu ĐỌC TẤT CẢ
        int rowsUpdated = notificationDAO.markAllAsRead(validUserId);
        assertEquals(2, rowsUpdated, "Phải có đúng 2 thông báo được cập nhật trạng thái");

        // 3. Kiểm tra lại xem tất cả đã là true chưa
        List<NotificationDTO> notifs = notificationDAO.findByUserId(validUserId);
        for (NotificationDTO n : notifs) {
            assertTrue(n.isRead(), "Tất cả thông báo của user này đều phải là đã đọc (true)");
        }
    }
}