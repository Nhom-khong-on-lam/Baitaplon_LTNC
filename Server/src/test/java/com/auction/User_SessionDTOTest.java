package com.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.common.model.User_SessionDTO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class User_SessionDTOTest {

    @Test
    @DisplayName("Test khởi tạo bằng Constructor rỗng và dùng Setter/Getter")
    void testNoArgsConstructorAndSetters() {
        // Arrange: Chuẩn bị dữ liệu
        User_SessionDTO session = new User_SessionDTO(); 
        LocalDateTime createdTime = LocalDateTime.now();
        LocalDateTime expireTime = createdTime.plusDays(1); // Hết hạn sau 1 ngày

        // Act: Gán giá trị bằng Setter
        session.setId(1L); 
        session.setUserId(100L); 
        session.setToken("abc-123-xyz"); 
        session.setCreatedAt(createdTime); 
        session.setExpiresAt(expireTime); 

        // Assert: Kiểm tra lại bằng Getter
        assertEquals(1L, session.getId()); 
        assertEquals(100L, session.getUserId()); 
        assertEquals("abc-123-xyz", session.getToken()); 
        assertEquals(createdTime, session.getCreatedAt()); 
        assertEquals(expireTime, session.getExpiresAt()); 
    }

    @Test
    @DisplayName("Test khởi tạo bằng Constructor đầy đủ tham số")
    void testAllArgsConstructor() {
        // Arrange & Act
        LocalDateTime createdTime = LocalDateTime.now();
        LocalDateTime expireTime = createdTime.plusHours(2);

        // Sử dụng Constructor chứa tất cả tham số
        User_SessionDTO session = new User_SessionDTO(
                5L,
                200L,
                "token-xyz-789",
                expireTime,
                createdTime
        ); // [cite: 200, 201]

        // Assert: Kiểm tra lại dữ liệu
        assertEquals(5L, session.getId()); // [cite: 202]
        assertEquals(200L, session.getUserId()); // [cite: 204]
        assertEquals("token-xyz-789", session.getToken()); // [cite: 206]
        assertEquals(expireTime, session.getExpiresAt()); // [cite: 208]
        assertEquals(createdTime, session.getCreatedAt()); // [cite: 210]
    }

    @Test
    @DisplayName("Test logic của hàm isExpired")
    void testIsExpired() {
        User_SessionDTO session = new User_SessionDTO(); // [cite: 200]

        // Trường hợp 1: Thời gian hết hạn nằm ở Tương Lai -> Chưa hết hạn
        session.setExpiresAt(LocalDateTime.now().plusHours(1)); // [cite: 209]
        assertFalse(session.isExpired(), "Session có hạn ở tương lai thì không được tính là hết hạn"); //

        // Trường hợp 2: Thời gian hết hạn nằm ở Quá Khứ -> Đã hết hạn
        session.setExpiresAt(LocalDateTime.now().minusHours(1)); // [cite: 209]
        assertTrue(session.isExpired(), "Session có hạn ở quá khứ phải được tính là hết hạn"); //

        // Trường hợp 3: Thời gian hết hạn chưa được set (null) -> Xử lý an toàn tránh NullPointerException
        session.setExpiresAt(null); // [cite: 209]
        assertFalse(session.isExpired(), "Nếu expiresAt là null thì coi như chưa hết hạn"); //
    }
}
