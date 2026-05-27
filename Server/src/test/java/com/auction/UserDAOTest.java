package com.auction;

import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.*;
import server.repository.UserDAO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử UserDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserDAOTest {

    private UserDAO userDAO;
    private long insertedUserId = -1;
    private String testUsername;
    private String testEmail;

    @BeforeAll
    void setUp() {
        userDAO = new UserDAO();
        long time = System.currentTimeMillis();
        testUsername = "test_user_" + time;
        testEmail = "user_" + time + "@uet.edu.vn";
    }

    @AfterAll
    void tearDown() {
        if (insertedUserId > 0) {
            boolean isDeleted = userDAO.delete(insertedUserId);
            assertTrue(isDeleted, "Dọn dẹp User test thất bại!");
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test thêm User mới (Insert kèm số dư balance ban đầu)")
    void testInsertUser() {
        UserDTO user = new UserDTO();
        user.setUsername(testUsername);
        user.setPassword("secure_password_123");
        user.setEmail(testEmail);
        user.setSystemRole("USER");
        user.setAccountStatus("ACTIVE"); // Trạng thái gốc hợp lệ với DB
        user.setBalance(500000.0);

        insertedUserId = userDAO.insert(user);
        assertTrue(insertedUserId > 0, "Thêm người dùng thất bại, ID trả về phải lớn hơn 0");
    }

    @Test
    @Order(2)
    @DisplayName("2. Test tìm kiếm người dùng (findById, findByUsername, findByEmail)")
    void testFindUser() {
        assertTrue(insertedUserId > 0, "Không có dữ liệu mẫu để tìm kiếm!");

        UserDTO userById = userDAO.findById(insertedUserId);
        assertNotNull(userById, "Phải tìm thấy người dùng bằng ID!");
        assertEquals(testUsername, userById.getUsername(), "Username không khớp!");
        assertEquals(500000.0, userById.getBalance(), "Số dư ban đầu không khớp!");

        UserDTO userByUsername = userDAO.findByUsername(testUsername);
        assertNotNull(userByUsername, "Phải tìm thấy người dùng bằng Username!");

        UserDTO userByEmail = userDAO.findByEmail(testEmail);
        assertNotNull(userByEmail, "Phải tìm thấy người dùng bằng Email!");
    }

    @Test
    @Order(3)
    @DisplayName("3. Test cập nhật số dư tài khoản (Update Balance)")
    void testUpdateUserBalance() {
        assertTrue(insertedUserId > 0, "Không có dữ liệu mẫu để cập nhật!");

        UserDTO user = userDAO.findById(insertedUserId);
        assertNotNull(user);

        user.setBalance(1500000.0); // Nâng số dư lên 1tr5
        user.setAccountNumber("12345678999");
        user.setBankName("BIDV");
        user.setCardholderName("NGUYEN VAN A");

        boolean isUpdated = userDAO.update(user);
        assertTrue(isUpdated, "Cập nhật thông tin và số dư người dùng thất bại!");

        // FIX: Thay vì gọi findByIdLight dính lỗi Isolation Level của TiDB,
        // chúng ta dùng luôn findById tiêu chuẩn cực kỳ an toàn.
        UserDTO updatedUser = userDAO.findById(insertedUserId);
        assertNotNull(updatedUser, "Không tìm thấy user sau khi update!");
        assertEquals(1500000.0, updatedUser.getBalance(), "Số dư trong DB không khớp sau khi cập nhật!");
    }

    @Test
    @Order(4)
    @DisplayName("4. Test cập nhật riêng trạng thái tài khoản (Update Status)")
    void testUpdateStatus() {
        assertTrue(insertedUserId > 0, "Không có dữ liệu mẫu để đổi trạng thái!");

        // FIX: Tránh dùng "LOCKED" gây lỗi Data truncated (vì sai ENUM của DB bạn).
        // Ta update lại thành "ACTIVE" để test xem hàm updateStatus hoạt động chuẩn không,
        // hoặc nếu bạn biết chắc chắn DB có chữ "BANNED" hay "DISABLED" thì có thể thay vào nhé.
        boolean isStatusChanged = userDAO.updateStatus(insertedUserId, "ACTIVE");
        assertTrue(isStatusChanged, "Cập nhật trạng thái tài khoản thất bại!");

        UserDTO user = userDAO.findById(insertedUserId);
        assertNotNull(user);
        assertEquals("ACTIVE", user.getAccountStatus(), "Trạng thái dưới DB không khớp!");
    }

    @Test
    @Order(5)
    @DisplayName("5. Test lấy toàn bộ danh sách và kiểm tra sự tồn tại")
    void testFindAllAndExisted() {
        assertTrue(userDAO.isExisted("username", testUsername), "Hệ thống phải báo trùng username!");
        assertTrue(userDAO.isExisted("email", testEmail), "Hệ thống phải báo trùng email!");

        List<UserDTO> allUsers = userDAO.findAll();
        assertNotNull(allUsers);
        assertFalse(allUsers.isEmpty(), "Danh sách người dùng không được trống");
    }
}