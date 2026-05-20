package com.auction;

import com.auction.common.dto.UserDTO;
import com.auction.common.dto.User_SessionDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.repository.UserDAO;
import server.repository.UserSessionDAO;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class UserSessionDAOTest {

    private UserSessionDAO sessionDAO;
    private UserDAO userDAO;

    private String testToken = "TEST_TOKEN_XYZ_123";
    private long validUserId; // Sẽ lưu ID thật của User được tạo lúc test

    @BeforeEach
    void setUp() {
        sessionDAO = new UserSessionDAO();
        userDAO = new UserDAO();

        // 1. TẠO MỘT USER TẠM THỜI ĐỂ CÓ ID HỢP LỆ CHO KHÓA NGOẠI
        UserDTO tempUser = new UserDTO();
        tempUser.setUsername("test_session_user");
        tempUser.setPassword("123456");
        tempUser.setEmail("testsession@uet.edu.vn");

        validUserId = userDAO.insert(tempUser);
        assertTrue(validUserId > 0, "Phải tạo được User tạm thời để test");
    }
    @AfterEach
    void tearDown() {
        // 2. DỌN DẸP DỮ LIỆU SAU KHI TEST XONG
        // Nhờ cơ chế ON DELETE CASCADE bạn đã cài đặt, khi xóa User, các Session của User đó cũng tự động bay màu
       sessionDAO.deleteByToken(testToken);
       userDAO.delete(validUserId);
    }

    @Test
    void testInsertAndFindByToken() {
        // Sử dụng validUserId (ID thật vừa được cấp) thay vì số 999 ảo
        User_SessionDTO newSession = new User_SessionDTO();
        newSession.setUserId(validUserId);
        newSession.setToken(testToken);
        newSession.setExpiresAt(LocalDateTime.now().plusDays(1));
        newSession.setCreatedAt(LocalDateTime.now());

        boolean success = sessionDAO.insert(newSession);

        assertTrue(success, "Insert thất bại");

        User_SessionDTO retrievedSession = sessionDAO.findByToken(testToken);

        assertNotNull(retrievedSession, "Không tìm thấy session vừa tạo");
        assertEquals(validUserId, retrievedSession.getUserId());
        assertEquals(testToken, retrievedSession.getToken());
    }

    @Test
    void testDeleteByToken() {
        User_SessionDTO session = new User_SessionDTO(null, validUserId, testToken, LocalDateTime.now().plusHours(1), LocalDateTime.now());
        sessionDAO.insert(session);

        boolean isDeleted = sessionDAO.deleteByToken(testToken);
        assertTrue(isDeleted, "Phải xóa thành công token");

        User_SessionDTO afterDelete = sessionDAO.findByToken(testToken);
        assertNull(afterDelete, "Session phải bị xóa hoàn toàn khỏi DB");
    }
}