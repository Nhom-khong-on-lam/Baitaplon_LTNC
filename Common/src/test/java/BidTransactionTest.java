

import com.auction.common.enums.SystemRole;
import com.auction.common.model.BidTransaction;
import com.auction.common.model.User;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class BidTransactionTest {

    @Test
    void testIsValidBidAmount() {
        User bidder = new User(2L, "buyer", "hash", "buyer@test.com", SystemRole.USER);
        // Người dùng đặt giá là 150.0
        BidTransaction transaction = new BidTransaction(bidder, 150.0, false);

        // Giá hiện tại 100.0 + bước giá tối thiểu 50.0 = 150.0 -> Hợp lệ
        assertTrue(transaction.isValid(100.0, 50.0));

        // Giá hiện tại 110.0 + bước giá tối thiểu 50.0 = 160.0 -> 150.0 không đủ điều kiện
        assertFalse(transaction.isValid(110.0, 50.0));
    }

    @Test
    void testGetFormattedTime() {
        User bidder = new User(2L, "buyer", "hash", "buyer@test.com", SystemRole.USER);
        BidTransaction transaction = new BidTransaction(bidder, 150.0, false);

        // Ép thời gian cố định vào lúc 21:45:10
        LocalDateTime customTime = LocalDateTime.of(2026, 5, 25, 21, 45, 10);
        transaction.setBidTime(customTime);

        assertEquals("21:45:10", transaction.getFormattedTime());
    }
}