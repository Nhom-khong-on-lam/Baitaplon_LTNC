package com.auction;

import org.junit.jupiter.api.*;
import server.common.model.UserDTO;
import server.repository.UserDAO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {

    private static final Logger LOGGER =
            Logger.getLogger(UserDAOTest.class.getName());

    private UserDAO userDAO;

    // Lưu ID user được tạo để dùng cho các test sau
    private Long newlyCreatedId;

    // Username unique để tránh trùng dữ liệu
    private final String uniqueUsername =
            "testuser_" + System.currentTimeMillis();

    @BeforeAll
    void init() {
        userDAO = new UserDAO();
    }

    // Cleanup dữ liệu sau khi test xong
    @AfterAll
    void cleanup() {
        if (newlyCreatedId != null) {
            userDAO.delete(newlyCreatedId);
            LOGGER.info("CLEANUP: Đã xóa user test ID = " + newlyCreatedId);
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Test Insert User")
    void testInsert() {

        UserDTO user = new UserDTO();
        user.setUsername(uniqueUsername);
        user.setPassword("123456");
        user.setEmail("test@gmail.com");
        user.setSystemRole("USER");
        user.setAccountStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        long id = userDAO.insert(user);

        assertTrue(id > 0,
                "Insert thất bại, ID trả về phải > 0");

        newlyCreatedId = id;

        LOGGER.info("TEST INSERT SUCCESS: User ID = " + newlyCreatedId);
    }

    @Test
    @Order(2)
    @DisplayName("2. Test Find By ID")
    void testFindById() {

        Assumptions.assumeTrue(
                newlyCreatedId != null,
                "Bỏ qua test vì ID null"
        );

        UserDTO user = userDAO.findById(newlyCreatedId);

        assertNotNull(user,
                "Không tìm thấy user với ID = " + newlyCreatedId);

        assertEquals(
                newlyCreatedId.longValue(),
                user.getId()
        );

        assertEquals(uniqueUsername, user.getUsername());
    }

    @Test
    @Order(3)
    @DisplayName("3. Test Find By Username")
    void testFindByUsername() {

        Assumptions.assumeTrue(newlyCreatedId != null);

        UserDTO user = userDAO.findByUsername(uniqueUsername);

        assertNotNull(user,
                "Không tìm thấy user theo username");

        assertEquals(uniqueUsername, user.getUsername());
    }

    @Test
    @Order(4)
    @DisplayName("4. Test Update User")
    void testUpdate() {

        Assumptions.assumeTrue(newlyCreatedId != null);

        UserDTO user = userDAO.findById(newlyCreatedId);

        assertNotNull(user);

        user.setEmail("new_email@example.com");
        user.setAccountStatus("SUSPENDED");

        boolean updated = userDAO.update(user);

        assertTrue(updated,
                "Update phải trả về true");

        UserDTO updatedUser = userDAO.findById(newlyCreatedId);

        assertNotNull(updatedUser);

        assertEquals(
                "new_email@example.com",
                updatedUser.getEmail()
        );

        assertEquals(
                "SUSPENDED",
                updatedUser.getAccountStatus()
        );
    }

    @Test
    @Order(5)
    @DisplayName("5. Test Find All")
    void testFindAll() {

        List<UserDTO> users = userDAO.findAll();

        assertNotNull(users,
                "Danh sách user không được null");

        assertFalse(users.isEmpty(),
                "Danh sách user không được rỗng");

        boolean found = users.stream()
                .anyMatch(u -> u.getId() == newlyCreatedId);

        assertTrue(found,
                "User mới insert phải tồn tại trong findAll()");
    }

    @Test
    @Order(6)
    @DisplayName("6. Test Is Existed")
    void testIsExisted() {

        Assumptions.assumeTrue(newlyCreatedId != null);

        boolean exists = userDAO.isExisted(
                "username",
                uniqueUsername
        );

        assertTrue(exists,
                "User đáng lẽ phải tồn tại");
    }

    @Test
    @Order(7)
    @DisplayName("7. Test Find By ID - Not Found")
    void testFindById_NotFound() {

        UserDTO user = userDAO.findById(-999999);

        assertNull(user,
                "User không tồn tại phải trả về null");
    }

    @Test
    @Order(8)
    @DisplayName("8. Test Delete Invalid ID")
    void testDelete_InvalidId() {

        boolean deleted = userDAO.delete(-999999);

        assertFalse(deleted,
                "Delete ID không tồn tại phải trả về false");
    }

    @Test
    @Order(9)
    @DisplayName("9. Test Delete User")
    void testDelete() {

        Assumptions.assumeTrue(newlyCreatedId != null);

        boolean deleted = userDAO.delete(newlyCreatedId);

        assertTrue(deleted,
                "Xóa user thất bại");

        UserDTO deletedUser =
                userDAO.findById(newlyCreatedId);

        assertNull(deletedUser,
                "User phải null sau khi xóa");

        LOGGER.info("TEST DELETE SUCCESS: User ID = " + newlyCreatedId);

        // Tránh cleanup xóa lại lần nữa
        newlyCreatedId = null;
    }
}