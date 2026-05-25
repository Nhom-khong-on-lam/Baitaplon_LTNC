

import com.auction.common.enums.SystemRole;
import com.auction.common.model.AutoBidConfig;
import com.auction.common.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AutoBidConfigTest {

    @Test
    void testGetNextBid_SuccessAndLimit() {
        User bidder = new User(1L, "bidder_auto", "hash", "auto@test.com", SystemRole.USER);
        // Đặt tối đa 500.0, mỗi lần tăng 50.0
        AutoBidConfig config = new AutoBidConfig(bidder, 500.0, 50.0);

        assertTrue(config.isActive());
        assertEquals(50.0, config.getIncrement());

        // Giá hiện tại là 300.0 -> Giá tiếp theo phải là 350.0
        assertEquals(350.0, config.getNextBid(300.0));

        // Giá hiện tại là 480.0 -> Cộng thêm 50 = 530 (Vượt quá maxPrice 500) -> Phải trả về -1
        assertEquals(-1.0, config.getNextBid(480.0));
    }

    @Test
    void testGetNextBid_WhenDeactivated() {
        User bidder = new User(1L, "bidder_auto", "hash", "auto@test.com", SystemRole.USER);
        AutoBidConfig config = new AutoBidConfig(bidder, 500.0, 50.0);

        config.deactivate();

        assertFalse(config.isActive());
        // Khi cấu hình đã bị hủy kích hoạt, getNextBid luôn trả về -1
        assertEquals(-1.0, config.getNextBid(200.0));
    }
}