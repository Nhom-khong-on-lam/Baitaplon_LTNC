package com.auction;


import com.auction.common.enums.AccountStatus;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.BaseEntity;
import com.auction.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UserTest {
    private User user;
    private final Long userId = 1L;
    private final String username = "testuser";
    private final String password = "hashed_password";
    private final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        // Khởi tạo đối tượng User mặc định trước mỗi test case
        user = new User(userId, username, password, email, SystemRole.USER);
    }

    @Test
    @DisplayName("Nên khởi tạo User với các giá trị mặc định chính xác")
    void shouldCreateUserWithCorrectInitialValues() {
        assertThat(user.getId()).isEqualTo(userId);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getSystemRole()).isEqualTo(SystemRole.USER);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE); // Default trong constructor
        assertThat(user.getCreatedAt()).isNotNull(); // Kế thừa từ BaseEntity
    }

    @Test
    @DisplayName("Nên kiểm tra đúng quyền Admin")
    void shouldVerifyAdminRole() {
        User admin = new User(2L, "admin", "pass", "admin@test.com", SystemRole.ADMIN);

        assertThat(admin.isAdmin()).isTrue();
        assertThat(user.isAdmin()).isFalse(); // user role là SystemRole.USER
    }

    @Test
    @DisplayName("Nên trả về định dạng ngày tham gia dd/MM/yyyy")
    void shouldReturnFormattedJoinedDate() {
        String expectedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        assertThat(user.getJoinedDate()).isEqualTo(expectedDate);
    }

    @Test
    @DisplayName("Nên cập nhật được thông tin qua Setter")
    void shouldUpdateUserInformationViaSetters() {
        user.setUsername("new_name");
        user.setEmail("new@test.com");

        user.setAccountStatus(AccountStatus.SUSPENDED);

        assertThat(user.getUsername()).isEqualTo("new_name");
        assertThat(user.getEmail()).isEqualTo("new@test.com");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Nên kế thừa đúng các trường từ BaseEntity")
    void shouldInheritFromBaseEntity() {
        // Kiểm tra xem User có các thuộc tính của BaseEntity không
        assertThat(user).isInstanceOf(BaseEntity.class);

        LocalDateTime manualUpdate = LocalDateTime.now().plusDays(1);
        user.setUpdatedAt(manualUpdate);

        assertThat(user.getUpdatedAt()).isEqualTo(manualUpdate);
    }
}
