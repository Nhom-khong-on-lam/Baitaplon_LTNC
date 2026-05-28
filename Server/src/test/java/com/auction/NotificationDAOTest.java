package com.auction;

import com.auction.common.dto.NotificationDTO;
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.*;
import server.repository.NotificationDAO;
import server.repository.UserDAO; // 1. IMPORT USERDAO ĐỂ ĐỒNG BỘ KHÓA NGOẠI

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử NotificationDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Cho phép chia sẻ biến mồi qua các hàm @Test
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationDAOTest {

    private NotificationDAO notificationDAO;
    private UserDAO userDAO;

    private long testUserId = -1;       // ID của User mồi nhận thông báo
    private long createdNotificationId = -1; // ID của thông báo vừa tạo

    @BeforeAll
    void initAll() {
        notificationDAO = new NotificationDAO();
        userDAO = new UserDAO();

        // 2. CHỦ ĐỘNG TẠO USER MỒI ĐỂ TRÁNH LỖI PHỤ THUỘC TRONG DB
        try {
            UserDTO mockUser = new UserDTO();
            mockUser.setUsername("notif_receiver_" + System.currentTimeMillis());
            mockUser.setPassword("123456");
            mockUser.setEmail("notif_" + System.currentTimeMillis() + "@uet.edu.vn");

            testUserId = userDAO.insert(mockUser);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Đảm bảo phải tạo được User mồi thành công mới bắt đầu test thông báo
        assertTrue(testUserId > 0, "Phải tạo được User tạm thời để test Notification");
    }

    @AfterAll
    void tearDownAll() {
        // 3. DỌN SẠCH DỮ LIỆU RÁC THEO THỨ TỰ NGƯỢC (REVERSE ORDER)
        try {
            // Khi User bị xóa, cơ chế ON DELETE CASCADE (nếu có trong DB) hoặc xóa thủ công
            // sẽ quét sạch bảng notification liên quan, nhưng xóa User là bước gốc an toàn nhất.
            if (testUserId > 0) {
                userDAO.delete(testUserId);
            }
        } catch (Exception e) {
            System.out.println("Lỗi dọn dẹp dữ liệu test thông báo: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test gửi thông báo mới (Insert)")
    void testInsertNotification() {
        NotificationDTO notif = new NotificationDTO();
        notif.setUserId(testUserId); // Gán ID của User mồi vừa sinh ra
        notif.setTitle("Đấu giá thành công!");
        notif.setMessage("Chúc mừng bạn đã chiến thắng cuộc đấu giá mã số #10.");
        notif.setType("SYSTEM");
        notif.setRead(false); // Mặc định là chưa đọc
        notif.setCreatedAt(LocalDateTime.now());
        notif.setExpiresAt(LocalDateTime.now().plusDays(7));

        // Cột liên quan đến Đấu giá và Lượt ra giá đặt NULL cho đơn giản, không cần mồi thêm bảng khác
        notif.setRelatedAuctionId(null);
        notif.setRelatedBidId(null);

        // Gọi hàm insert của bạn, hứng lấy ID tự sinh từ MySQL
        createdNotificationId = notificationDAO.insert(notif);

        assertTrue(createdNotificationId > 0, "Gửi thông báo thất bại, ID trả về phải lớn hơn 0");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test đếm số thông báo chưa đọc")
    void testCountUnread() {
        int unreadCount = notificationDAO.countUnread(testUserId);
        // Vì ở bước 1 chúng ta đã chèn thành công 1 thông báo chưa đọc (is_read = false)
        assertEquals(1, unreadCount, "Số lượng thông báo chưa đọc phải chính xác bằng 1");
    }

    @Test
    @Order(3)
    @DisplayName("3. Test lấy danh sách thông báo chưa đọc")
    void testGetUnreadNotifications() {
        List<NotificationDTO> unreadList = notificationDAO.getUnreadNotifications(testUserId);

        assertNotNull(unreadList, "Danh sách không được phép null");
        assertFalse(unreadList.isEmpty(), "Danh sách không được trống");
        assertEquals(createdNotificationId, unreadList.get(0).getId(), "ID thông báo chưa đọc không khớp!");
    }

    @Test
    @Order(4)
    @DisplayName("4. Test đánh dấu một thông báo là đã đọc")
    void testMarkAsRead() {
        boolean success = notificationDAO.markAsRead(createdNotificationId);
        assertTrue(success, "Đánh dấu đã đọc thất bại");

        // Kiểm tra lại xem số lượng chưa đọc đã giảm về 0 chưa
        int unreadCountAfter = notificationDAO.countUnread(testUserId);
        assertEquals(0, unreadCountAfter, "Thông báo phải chuyển sang trạng thái đã đọc");
    }

    @Test
    @Order(5)
    @DisplayName("5. Test lấy toàn bộ danh sách thông báo của User")
    void testFindByUserId() {
        List<NotificationDTO> allNotifs = notificationDAO.findByUserId(testUserId);

        assertNotNull(allNotifs);
        assertEquals(1, allNotifs.size(), "User này phải có đúng 1 thông báo tổng cộng");
    }

    @Test
    @Order(6)
    @DisplayName("6. Test đánh dấu đọc tất cả thông báo của User")
    void testMarkAllAsRead() {
        // Tạo thêm 1 thông báo chưa đọc mới để thử nghiệm tính năng đọc tất cả
        NotificationDTO secondNotif = new NotificationDTO();
        secondNotif.setUserId(testUserId);
        secondNotif.setTitle("Thông báo số 2");
        secondNotif.setMessage("Nội dung thông báo số 2");

        // ĐỔI TỪ "ALERT" THÀNH "SYSTEM" CHO KHỚP ENUM DATABASE CỦA BẠN
        secondNotif.setType("SYSTEM");

        secondNotif.setRead(false);
        notificationDAO.insert(secondNotif);

        // Thực hiện cập nhật đọc tất cả
        int rowsUpdated = notificationDAO.markAllAsRead(testUserId);
        assertEquals(1, rowsUpdated, "Phải cập nhật thành công 1 thông báo chưa đọc còn lại");

        // Đảm bảo không còn thông báo nào chưa đọc
        assertEquals(0, notificationDAO.countUnread(testUserId));
    }
}