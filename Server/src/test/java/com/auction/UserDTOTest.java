package com.auction;

import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class UserDTOTest {

    @Test
    @DisplayName("1. Test Constructor mặc định")
    void testDefaultConstructor() {

        UserDTO user = new UserDTO();

        assertNotNull(user);

        // Kiểm tra giá trị mặc định
        assertEquals(0, user.getId());
        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getEmail());
        assertNull(user.getSystemRole());
        assertNull(user.getAccountStatus());
        assertNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("2. Test Constructor Register")
    void testRegisterConstructor() {

        UserDTO user = new UserDTO(
                "dung123",
                "123456",
                "dung@gmail.com"
        );

        assertEquals("dung123", user.getUsername());
        assertEquals("123456", user.getPassword());
        assertEquals("dung@gmail.com", user.getEmail());

        // Các field khác chưa được set
        assertEquals(0, user.getId());
        assertNull(user.getSystemRole());
        assertNull(user.getAccountStatus());
        assertNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("3. Test Full Constructor")
    void testFullConstructor() {

        LocalDateTime now = LocalDateTime.now();

        UserDTO user = new UserDTO(
                1L,
                "admin",
                "hashed_password",
                "admin@gmail.com",
                "ADMIN",
                "ACTIVE",
                now
        );

        assertEquals(1L, user.getId());
        assertEquals("admin", user.getUsername());
        assertEquals("hashed_password", user.getPassword());
        assertEquals("admin@gmail.com", user.getEmail());
        assertEquals("ADMIN", user.getSystemRole());
        assertEquals("ACTIVE", user.getAccountStatus());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    @DisplayName("4. Test Getter và Setter")
    void testGetterSetter() {

        UserDTO user = new UserDTO();

        LocalDateTime now = LocalDateTime.now();

        user.setId(10L);
        user.setUsername("testuser");
        user.setPassword("abc123");
        user.setEmail("test@gmail.com");
        user.setSystemRole("USER");
        user.setAccountStatus("ACTIVE");
        user.setCreatedAt(now);

        assertEquals(10L, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals("abc123", user.getPassword());
        assertEquals("test@gmail.com", user.getEmail());
        assertEquals("USER", user.getSystemRole());
        assertEquals("ACTIVE", user.getAccountStatus());
        assertEquals(now, user.getCreatedAt());
    }

    @Test
    @DisplayName("5. Test toString")
    void testToString() {

        UserDTO user = new UserDTO();

        user.setUsername("dung");
        user.setSystemRole("ADMIN");

        String result = user.toString();

        assertNotNull(result);

        assertTrue(result.contains("dung"));
        assertTrue(result.contains("ADMIN"));

        assertEquals(
                "UserDTO{username='dung', role='ADMIN'}",
                result
        );
    }

    @Test
    @DisplayName("6. Test null values")
    void testNullValues() {

        UserDTO user = new UserDTO();

        user.setUsername(null);
        user.setEmail(null);
        user.setPassword(null);

        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getPassword());
    }

    @Test
    @DisplayName("7. Test update field values")
    void testUpdateValues() {

        UserDTO user = new UserDTO();

        user.setUsername("oldUser");
        assertEquals("oldUser", user.getUsername());

        user.setUsername("newUser");
        assertEquals("newUser", user.getUsername());
    }
}