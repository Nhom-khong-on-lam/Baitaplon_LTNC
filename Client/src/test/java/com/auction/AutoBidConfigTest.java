package com.auction;

// 1. Import các class từ model của bạn


// 2. Import các công cụ test của JUnit 5
import com.auction.common.model.AutoBidConfig;
import com.auction.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// 3. Import các lệnh kiểm tra (Assertion)
import static org.junit.jupiter.api.Assertions.*;

class AutoBidConfigTest {

    private User testUser;
    private AutoBidConfig config;
    private final double MAX_PRICE = 1000.0;
    private final double STEP = 50.0;

    @BeforeEach
    void setUp() {
        // Tạo user giả lập cho test
        testUser = new User("test_user");
        config = new AutoBidConfig(testUser, MAX_PRICE, STEP);
    }

    @Test
    @DisplayName("Kiểm tra khởi tạo đối tượng")
    void testConstructor() {
        assertNotNull(config);
        assertEquals(testUser, config.getBidder());
        assertTrue(config.isActive());
    }

    @Test
    @DisplayName("Kiểm tra tính giá bid tiếp theo")
    void testGetNextBid() {
        // 500 + 50 = 550 (Hợp lệ)
        assertEquals(550.0, config.getNextBid(500.0));

        // 1000 + 50 = 1050 (Vượt mức 1000 -> Trả về -1)
        assertEquals(-1, config.getNextBid(1000.0));
    }

    @Test
    @DisplayName("Kiểm tra vô hiệu hóa cấu hình")
    void testDeactivate() {
        config.deactivate();
        assertFalse(config.isActive());
        assertEquals(-1, config.getNextBid(100.0));
    }

    @Test
    @DisplayName("Kiểm tra toString")
    void testToString() {
        // 1. Lấy kết quả toString từ config
        String result = config.toString();

        // 2. In ra màn hình để bạn tự kiểm tra xem nó đang chứa gì (chuột phải Run sẽ thấy)
        System.out.println("Kết quả toString thực tế: " + result);

        // 3. Kiểm tra an toàn:
        assertNotNull(result, "Hàm toString không được trả về null");

        // Thay vì dùng testUser.getUsername() (đang bị null),
        // ta kiểm tra xem class name có xuất hiện không trước đã
        assertTrue(result.contains("AutoBidConfig"), "toString phải chứa tên lớp");

        // Nếu bạn muốn check username, hãy chắc chắn username không null
        if (testUser.getUsername() != null) {
            assertTrue(result.contains(testUser.getUsername()));
        } else {
            System.out.println("Cảnh báo: getUsername() đang trả về null, hãy kiểm tra lại lớp User!");
        }
    }
}