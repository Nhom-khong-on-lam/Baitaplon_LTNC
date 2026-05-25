

import com.auction.common.network.Request;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RequestTest {

    @Test
    void testRequestCreationAndToken() {
        String testPayload = "Thông tin phiên đấu giá";

        // Khởi tạo một Request với hành động "PLACE_BID" kèm dữ liệu mẫu
        Request request = new Request(Request.PLACE_BID, testPayload);

        assertEquals(Request.PLACE_BID, request.getAction());
        assertEquals("PLACE_BID", request.getAction());
        assertEquals(testPayload, request.getData());

        // Mặc định token lúc đầu phải bằng null
        assertNull(request.getToken());

        // Giả lập sau khi đăng nhập xong, hệ thống đính kèm Token xác thực vào
        String sessionToken = "JWT_TOKEN_ABC_123";
        request.setToken(sessionToken);

        assertEquals(sessionToken, request.getToken());
    }

    @Test
    void testRequestActionOnly() {
        // Test trường hợp gửi gói tin chỉ chứa lệnh điều hướng không cần payload (ví dụ LOGOUT)
        Request request = new Request(Request.LOGOUT);

        assertEquals(Request.LOGOUT, request.getAction());
        assertNull(request.getData());
    }
}