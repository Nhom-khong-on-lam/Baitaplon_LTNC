package com.auction;

import com.auction.common.dto.UserDTO;
import com.auction.common.dto.User_SessionDTO;
import org.junit.jupiter.api.*;
import server.repository.UserDAO;
import server.repository.UserSessionDAO;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử UserSessionDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Giúp chia sẻ ID người dùng và Token xuyên suốt các bước test
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserSessionDAOTest {

    private UserSessionDAO userSessionDAO;
    private UserDAO userDAO;

    private long testUserId = -1;       // ID của User mồi để làm khóa ngoại
    private String testToken;           // Token ngẫu nhiên sinh ra để test

    @BeforeAll
    void initAll() {
        userSessionDAO = new UserSessionDAO();
        userDAO = new UserDAO();

        // 1. CHỦ ĐỘNG MỒI USER MỚI ĐỂ NÉ LỖI KHÓA NGOẠI (FOREIGN KEY)
        try {
            UserDTO mockUser = new UserDTO();
            mockUser.setUsername("session_user_" + System.currentTimeMillis());
            mockUser.setPassword("password_session");
            mockUser.setEmail("session_" + System.currentTimeMillis() + "@uet.edu.vn");
            mockUser.setAccountStatus("ACTIVE");

            testUserId = userDAO.insert(mockUser);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sinh mã token ngẫu nhiên dạng UUID không lo bị trùng lặp dữ liệu dưới DB
        testToken = "TOKEN-" + UUID.randomUUID().toString();

        // Đảm bảo phải có User gốc thành công thì mới bắt đầu bài test phiên làm việc
        assertTrue(testUserId > 0, "Không thể mồi được người dùng tạm thời, hủy toàn bộ bài test Session!");
    }

    @AfterAll
    void tearDownAll() {
        // 2. DỌN SẠCH DỮ LIỆU RÁC THEO THỨ TỰ NGƯỢC (REVERSE ORDER)
        try {
            // Xóa triệt để Token session trước (nếu bước test số 3 lỡ thất bại)
            userSessionDAO.deleteByToken(testToken);

            // Xóa người dùng mồi sau cùng để không dính ràng buộc
            if (testUserId > 0) {
                userDAO.delete(testUserId);
            }
        } catch (Exception e) {
            System.out.println("Lưu ý khi dọn dẹp rác session test: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test thêm mới phiên đăng nhập (Insert)")
    void testInsertSession() {
        User_SessionDTO session = new User_SessionDTO();
        session.setUserId(testUserId); // Gắn ID User mồi vừa sinh từ DB lên
        session.setToken(testToken);   // Sử dụng Token UUID duy nhất
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusHours(2)); // Hết hạn sau 2 tiếng

        // Hàm insert của bạn trả về kiểu boolean
        boolean isInserted = userSessionDAO.insert(session);

        // Sử dụng assertTrue nguyên bản của JUnit 5
        assertTrue(isInserted, "Thêm phiên làm việc mới vào database thất bại!");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test tìm kiếm phiên đăng nhập bằng mã Token")
    void testFindByToken() {
        // Thực hiện tìm kiếm bằng mã token đã lưu ở bài test 1
        User_SessionDTO retrieved = userSessionDAO.findByToken(testToken);

        assertNotNull(retrieved, "Phải tìm thấy thông tin phiên đăng nhập tương ứng với Token!");
        assertEquals(testUserId, retrieved.getUserId(), "User ID liên kết với Session lấy ra không khớp!");
        assertEquals(testToken, retrieved.getToken(), "Chuỗi Token lấy ra không khớp!");
        assertNotNull(retrieved.getExpiresAt(), "Thời gian hết hạn không được để null!");
    }

    @Test
    @Order(3)
    @DisplayName("3. Test xóa phiên đăng nhập khi đăng xuất (Delete By Token)")
    void testDeleteByToken() {
        // Thực hiện đăng xuất / xóa session
        boolean isDeleted = userSessionDAO.deleteByToken(testToken);
        assertTrue(isDeleted, "Xóa phiên làm việc tương ứng với mã Token thất bại!");

        // Kiểm tra lại xem Token đã thực sự biến mất khỏi hệ thống chưa
        User_SessionDTO afterDeleted = userSessionDAO.findByToken(testToken);
        assertNull(afterDeleted, "Session phải hoàn toàn biến mất (trả về null) sau khi đã thực hiện lệnh xóa!");
    }
}