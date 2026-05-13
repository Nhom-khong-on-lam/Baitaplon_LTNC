package com.auction;

import server.repository.UserDAO;
import org.junit.jupiter.api.*;
import server.common.model.UserDTO;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Giúp giữ trạng thái của các biến instance (như newlyCreatedId) trong suốt quá trình test
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {

    private UserDAO userDAO;
    private Long newlyCreatedId; // Không cần static nữa nhờ Lifecycle.PER_CLASS

    @BeforeAll
    void init() {
        userDAO = new UserDAO();
    }

    @Test
    @Order(1)
    @DisplayName("1. Test Insert User")
    void testInsert() {
        UserDTO user = new UserDTO();
        user.setUsername("testuser_" + System.currentTimeMillis());
        user.setPassword("123456");
        user.setEmail("test@gmail.com");
        user.setSystemRole("USER");
        user.setAccountStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        long id = userDAO.insert(user);

        // Kiểm tra nếu id <= 0 nghĩa là insert vào DB thất bại (có thể do lỗi SQL)
        assertTrue(id > 0, "Insert thất bại, ID trả về phải > 0");
        this.newlyCreatedId = id;
        System.out.println(">>> Đã tạo User với ID: " + newlyCreatedId);
    }

    @Test
    @Order(2)
    @DisplayName("2. Test Find By ID")
    void testFindById() {
        // Đảm bảo newlyCreatedId không null trước khi chạy
        Assumptions.assumeTrue(newlyCreatedId != null, "Bỏ qua test vì ID bị null");

        UserDTO user = userDAO.findById(newlyCreatedId);
        assertNotNull(user, "Không tìm thấy User với ID: " + newlyCreatedId);
        assertEquals(newlyCreatedId, user.getId());
    }

    @Test
    @Order(3)
    @DisplayName("3. Test Update User")
    void testUpdate() {
        Assumptions.assumeTrue(newlyCreatedId != null);

        UserDTO user = userDAO.findById(newlyCreatedId);
        user.setEmail("new_email@example.com");
        user.setAccountStatus("SUSPENDED");

        boolean updated = userDAO.update(user);
        assertTrue(updated, "Update phải trả về true");

        UserDTO updatedUser = userDAO.findById(newlyCreatedId);
        assertEquals("new_email@example.com", updatedUser.getEmail());
        assertEquals("SUSPENDED", updatedUser.getAccountStatus());
    }

    @Test
    @Order(4)
    @DisplayName("4. Test Find All")
    void testFindAll() {
        List<UserDTO> list = userDAO.findAll();
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Danh sách user không được rỗng");
    }

    @Test
    @Order(5)
    @DisplayName("5. Test IsExisted")
    void testIsExisted() {
        Assumptions.assumeTrue(newlyCreatedId != null);
        UserDTO user = userDAO.findById(newlyCreatedId);

        boolean exists = userDAO.isExisted("username", user.getUsername());
        assertTrue(exists, "User đáng lẽ phải tồn tại");
    }

    @Test
    @Order(6)
    @DisplayName("6. Test Delete User")
    void testDelete() {
        Assumptions.assumeTrue(newlyCreatedId != null);

        boolean deleted = userDAO.delete(newlyCreatedId);
        assertTrue(deleted, "Xóa thất bại");

        UserDTO user = userDAO.findById(newlyCreatedId);
        assertNull(user, "User phải null sau khi xóa");
    }
}
