package com.auction; // Đảm bảo package này khớp với thư mục test của bạn

import com.auction.client.model.BidTransaction;
import com.auction.client.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

class BidTransactionTest {

    private User testUser;
    private BidTransaction transaction;
    private final double BID_AMOUNT = 500.0;

    @BeforeEach
    void setUp() {
        // Khởi tạo User - Đảm bảo set username để tránh lỗi NullPointerException khi toString
        testUser = new User("bidder_01");
        // Khởi tạo một giao dịch bid thủ công (autoBid = false)
        transaction = new BidTransaction(testUser, BID_AMOUNT, false);
    }

    @Test
    @DisplayName("Kiểm tra khởi tạo đối tượng BidTransaction")
    void testConstructor() {
        assertAll("Verify initial state",
                () -> assertEquals(testUser, transaction.getBidder()),
                () -> assertEquals(BID_AMOUNT, transaction.getAmount()),
                () -> assertFalse(transaction.isAutoBid()),
                () -> assertNotNull(transaction.getBidTime())
        );
    }

    @Test
    @DisplayName("Kiểm tra logic hợp lệ của giá bid (isValid)")
    void testIsValid() {
        double currentPrice = 400.0;
        double step = 50.0;

        // Trường hợp hợp lệ: 500 >= 400 + 50 (450) -> True
        assertTrue(transaction.isValid(currentPrice, step), "Giá bid 500 phải hợp lệ khi giá hiện tại là 400 và bước giá là 50");

        // Trường hợp không hợp lệ: Giá bid thấp hơn giá tối thiểu yêu cầu
        // Ví dụ: 500 < 480 + 50 (530) -> False
        assertFalse(transaction.isValid(480.0, 50.0), "Giá bid phải không hợp lệ nếu thấp hơn currentPrice + step");
    }

    @Test
    @DisplayName("Kiểm tra định dạng thời gian (getFormattedTime)")
    void testGetFormattedTime() {
        String formattedTime = transaction.getFormattedTime();

        // Kiểm tra định dạng HH:mm:ss (độ dài chuỗi thường là 8 ký tự: 00:00:00)
        assertNotNull(formattedTime);
        assertEquals(8, formattedTime.length(), "Định dạng thời gian phải là HH:mm:ss");
        assertTrue(formattedTime.contains(":"), "Thời gian định dạng phải chứa dấu hai chấm");
    }

    @Test
    @DisplayName("Kiểm tra toString không bị lỗi Null")
    void testToString() {
        String result = transaction.toString();

        assertAll("Nội dung toString",
                () -> assertNotNull(result),
                () -> assertTrue(result.contains("BidTransaction")),
                () -> assertTrue(result.contains(String.valueOf(BID_AMOUNT)))
        );

        // Kiểm tra an toàn cho username để tránh NullPointerException nếu getUsername() bị lỗi
        if (testUser.getUsername() != null) {
            assertTrue(result.contains(testUser.getUsername()), "toString phải chứa username của người bid");
        }
    }
}