

import com.auction.common.network.Response;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ResponseTest {

    @Test
    void testResponseConstructorAndSetters() {
        // Test Constructor có tham số
        String testData = "Dữ liệu phiên đấu giá";
        Response response = new Response(true, "Thành công", testData);

        assertTrue(response.isSuccess());
        assertEquals("Thành công", response.getMessage());
        assertEquals(testData, response.getData());
        assertNull(response.getToken()); // Mặc định token phải bằng null

        // Test các hàm Setter
        response.setSuccess(false);
        response.setMessage("Thất bại");
        response.setData(null);
        response.setToken("SESSION_TOKEN_123");

        assertFalse(response.isSuccess());
        assertEquals("Thất bại", response.getMessage());
        assertNull(response.getData());
        assertEquals("SESSION_TOKEN_123", response.getToken());
    }

    @Test
    void testFactoryMethodOk() {
        String payload = "Danh sách sản phẩm";
        // Gọi helper static ok()
        Response response = Response.ok(payload);

        assertTrue(response.isSuccess());
        assertEquals("OK", response.getMessage());
        assertEquals(payload, response.getData());
    }

    @Test
    void testFactoryMethodError() {
        // Gọi helper static error()
        Response response = Response.error("Sai tài khoản hoặc mật khẩu");

        assertFalse(response.isSuccess());
        assertEquals("Sai tài khoản hoặc mật khẩu", response.getMessage());
        assertNull(response.getData());
    }
}