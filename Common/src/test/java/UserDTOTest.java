
import com.auction.common.dto.UserDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class UserDTOTest {

    @Test
    void testDefaultConstructorAndSettersGetters() {
        // 1. Test Constructor mặc định không tham số
        UserDTO dto = new UserDTO();
        LocalDateTime now = LocalDateTime.now();

        // Test toàn bộ các hàm Setter
        dto.setId(1L);
        dto.setUsername("user_test");
        dto.setPassword("hashed_password_123");
        dto.setEmail("test@gmail.com");
        dto.setSystemRole("CLIENT");
        dto.setAccountStatus("ACTIVE");
        dto.setCreatedAt(now);
        dto.setBidCount(5);
        dto.setAccountNumber("999999999");
        dto.setBankName("Vietcombank");
        dto.setCardholderName("NGUYEN VAN TEST");
        dto.setBalance(500000.0);

        // Test toàn bộ các hàm Getter xem dữ liệu trả ra có khớp không
        assertEquals(1L, dto.getId());
        assertEquals("user_test", dto.getUsername());
        assertEquals("hashed_password_123", dto.getPassword());
        assertEquals("test@gmail.com", dto.getEmail());
        assertEquals("CLIENT", dto.getSystemRole());
        assertEquals("ACTIVE", dto.getAccountStatus());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(5, dto.getBidCount());
        assertEquals("999999999", dto.getAccountNumber());
        assertEquals("Vietcombank", dto.getBankName());
        assertEquals("NGUYEN VAN TEST", dto.getCardholderName());
        assertEquals(500000.0, dto.getBalance());
    }

    @Test
    void testRegisterConstructor() {
        // 2. Test Constructor rút gọn dùng khi Đăng ký (Register)
        UserDTO dto = new UserDTO("register_user", "raw_password", "register@gmail.com");

        assertEquals("register_user", dto.getUsername());
        assertEquals("raw_password", dto.getPassword());
        assertEquals("register@gmail.com", dto.getEmail());

        // Các trường khác không truyền qua constructor này phải có giá trị mặc định (null hoặc 0)
        assertEquals(0L, dto.getId());
        assertNull(dto.getSystemRole());
    }

    @Test
    void testFullDaoConstructor() {
        // 3. Test Constructor đầy đủ thường được dùng ở tầng DAO để đổ dữ liệu từ Database lên
        LocalDateTime now = LocalDateTime.now();
        UserDTO dto = new UserDTO(99L, "dao_user", "db_pass", "dao@gmail.com", "ADMIN", "SUSPENDED", now);

        assertEquals(99L, dto.getId());
        assertEquals("dao_user", dto.getUsername());
        assertEquals("db_pass", dto.getPassword());
        assertEquals("dao@gmail.com", dto.getEmail());
        assertEquals("ADMIN", dto.getSystemRole());
        assertEquals("SUSPENDED", dto.getAccountStatus());
        assertEquals(now, dto.getCreatedAt());
    }

    @Test
    void testToStringMethod() {
        // 4. Test hàm toString() để tăng tỷ lệ Line Coverage tối đa, tránh sót dòng code nào
        UserDTO dto = new UserDTO();
        dto.setUsername("bobby");

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("UserDTO"));
    }
}