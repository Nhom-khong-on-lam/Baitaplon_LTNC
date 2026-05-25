

import com.auction.common.enums.AccountStatus;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        // Khởi tạo các đối tượng User dựa trên cấu trúc Constructor chính xác của mã nguồn
        adminUser = new User(1L, "admin_boss", "hashed_password_123", "admin@auction.com", SystemRole.ADMIN);
        normalUser = new User(2L, "john_doe", "hashed_password_456", "john@auction.com", SystemRole.USER);
    }

    @Test
    void testUserConstructorAndGetters() {
        // Kiểm tra tính chính xác của các giá trị truyền vào qua Constructor chính
        assertEquals(1L, adminUser.getId());
        assertEquals("admin_boss", adminUser.getUsername());
        assertEquals("hashed_password_123", adminUser.getPasswordHash());
        assertEquals("admin@auction.com", adminUser.getEmail());
        assertEquals(SystemRole.ADMIN, adminUser.getSystemRole());

        // Trạng thái mặc định khi tạo mới phải là ACTIVE
        assertEquals(AccountStatus.ACTIVE, adminUser.getAccountStatus());

        // Số dư mặc định ban đầu phải bằng 0.0
        assertEquals(0.0, adminUser.getBalance());
    }

    @Test
    void testAlternativeConstructor() {
        // Kiểm tra Constructor phụ nhận vào một chuỗi String
        User testUser = new User("testUser");
        assertNotNull(testUser);
        assertNull(testUser.getUsername()); // Các thuộc tính khác mặc định chưa được gán
    }

    @Test
    void testIsAdmin() {
        // Kiểm tra logic phân quyền tài khoản Quản trị viên
        assertTrue(adminUser.isAdmin(), "Tài khoản với vai trò ADMIN thì isAdmin() phải trả về true");
        assertFalse(normalUser.isAdmin(), "Tài khoản với vai trò USER thì isAdmin() phải trả về false");
    }

    @Test
    void testGetJoinedDate_Formatted() {
        // 1. Trường hợp joinedDate bằng null, hệ thống phải trả về "N/A"
        assertEquals("N/A", normalUser.getJoinedDate());

        // 2. Trường hợp joinedDate có giá trị cụ thể
        // Đặt ngày cụ thể: 25 tháng 12 năm 2026
        LocalDateTime testDate = LocalDateTime.of(2026, 12, 25, 10, 30);
        normalUser.setJoinedDate(testDate);

        // Định dạng mong muốn là dd/MM/yyyy -> "25/12/2026"
        assertEquals("25/12/2026", normalUser.getJoinedDate());
    }

    @Test
    void testSettersAndGettersForProfileAndBank() {
        // Kiểm tra tất cả các hàm cập nhật thông tin cá nhân và thông tin ngân hàng
        normalUser.setUsername("new_john");
        normalUser.setPasswordHash("new_hash_999");
        normalUser.setEmail("new_john@auction.com");
        normalUser.setbidCount(5);
        normalUser.setAccountNumber("999999999");
        normalUser.setBankName("Vietcombank");
        normalUser.setCardholderName("NGUYEN VAN A");
        normalUser.setBalance(1500.75);

        assertEquals("new_john", normalUser.getUsername());
        assertEquals("new_hash_999", normalUser.getPasswordHash());
        assertEquals("new_john@auction.com", normalUser.getEmail());
        assertEquals(5, normalUser.getBidCount());
        assertEquals("999999999", normalUser.getAccountNumber());
        assertEquals("Vietcombank", normalUser.getBankName());
        assertEquals("NGUYEN VAN A", normalUser.getCardholderName());
        assertEquals(1500.75, normalUser.getBalance());
    }

    @Test
    void testSetAccountStatus() {
        // Kiểm tra việc cập nhật trạng thái hoạt động của tài khoản
        normalUser.setAccountStatus(AccountStatus.BANNED);
        assertEquals(AccountStatus.BANNED, normalUser.getAccountStatus());
    }

    @Test
    void testLoginLogoutEmptyMethods() {
        // Kiểm tra các phương thức trống không gây ra lỗi ngoại lệ (Exception) khi thực thi
        assertDoesNotThrow(() -> normalUser.login());
        assertDoesNotThrow(() -> normalUser.logout());
    }
}