package com.auction;

import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import server.database.DBConnection;
import server.repository.UserDAO;

import java.sql.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserDAOTest {

    private UserDAO userDAO;

    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private ResultSet mockGeneratedKeys;

    private MockedStatic<DBConnection> staticDbMock;

    @BeforeEach
    void setUp() throws SQLException {
        // 1. Khởi tạo các đối tượng Mock cô lập tầng JDBC độc lập với DB thực tế
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
        mockGeneratedKeys = mock(ResultSet.class);

        // 2. Chặn đứng phương thức tĩnh DBConnection.getConnection()
        staticDbMock = mockStatic(DBConnection.class);
        staticDbMock.when(DBConnection::getConnection).thenReturn(mockConnection);

        // 3. Đóng gói Mock mặc định cho PreparedStatement để tránh lỗi gọi hàm trống
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);

        userDAO = new UserDAO();
    }

    @AfterEach
    void tearDown() {
        // Đóng static mock sau mỗi chu kỳ kiểm thử
        if (staticDbMock != null) {
            staticDbMock.close();
        }
    }

    // ── TEST: update() THÀNH CÔNG (MOCK HOÀN TOÀN KHÔNG CHẠM MYSQL THẬT) ──
    @Test
    void testUpdate_Success() throws SQLException {
        UserDTO user = new UserDTO();
        user.setId(660001L);
        user.setUsername("testuser");
        user.setEmail("test@gmail.com");
        user.setSystemRole("USER");
        user.setAccountStatus("ACTIVE");
        user.setPassword("hashed_password");
        user.setAccountNumber("123456789");
        user.setBankName("Techcombank");
        user.setCardholderName("NGUYEN VAN A");
        user.setBalance(500000.0);

        // Ép kiểu hành vi executeUpdate trả về 1 (Cập nhật thành công)
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean isUpdated = userDAO.update(user);

        // Xác minh kết quả trả về bắt buộc là true
        assertTrue(isUpdated, "Hàm update phải trả về true khi executeUpdate thành công");
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testUpdate_Fail() throws SQLException {
        UserDTO user = new UserDTO();
        user.setId(660001L);

        // Giả lập tình huống cập nhật thất bại (0 dòng bị tác động)
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        boolean isUpdated = userDAO.update(user);

        assertFalse(isUpdated);
    }

    // ── TEST: insert() THÀNH CÔNG ─────────────────────────────────────────
    @Test
    void testInsert_Success() throws SQLException {
        UserDTO user = new UserDTO();
        user.setUsername("newuser");
        user.setPassword("pass");
        user.setEmail("new@gmail.com");
        user.setBalance(10000.0);

        when(mockPreparedStatement.executeUpdate()).thenReturn(1);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockGeneratedKeys);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(999L);

        long generatedId = userDAO.insert(user);

        assertEquals(999L, generatedId);
    }

    // ── TEST: findById() TRÍCH XUẤT DỮ LIỆU MẪU ───────────────────────────
    @Test
    void testFindById_Success() throws SQLException {
        long targetId = 660001L;

        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getLong("id")).thenReturn(targetId);
        when(mockResultSet.getString("username")).thenReturn("testuser");
        when(mockResultSet.getString("email")).thenReturn("test@gmail.com");
        when(mockResultSet.getString("systemRole")).thenReturn("USER");
        when(mockResultSet.getString("accountStatus")).thenReturn("ACTIVE");
        when(mockResultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
        when(mockResultSet.getInt("bid_count")).thenReturn(0);

        UserDTO result = userDAO.findById(targetId);

        assertNotNull(result);
        assertEquals(targetId, result.getId());
        assertEquals("USER", result.getSystemRole());
    }

    // ── TEST: delete() THÀNH CÔNG ─────────────────────────────────────────
    @Test
    void testDelete_Success() throws SQLException {
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        boolean isDeleted = userDAO.delete(660001L);

        assertTrue(isDeleted);
    }
}