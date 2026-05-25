

import com.auction.common.enums.SystemRole;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import com.auction.common.model.Vehicle;
import com.auction.common.observer.AuctionObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuctionObserverTest {

    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        // Cấu hình để hứng luồng ghi dữ liệu từ System.out vào outputStreamCaptor
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        // Trả luồng System.out về trạng thái gốc của hệ thống sau khi test xong
        System.setOut(originalOut);
    }

    @Test
    void testUpdate_PrintsCorrectMessage() {
        // 1. Tạo đối tượng AuctionObserver cần kiểm thử
        AuctionObserver observer = new AuctionObserver("Client-01");

        // 2. Chuẩn bị các tham số giả lập để tạo đối tượng Auction trống
        User seller = new User(1L, "seller", "hash", "seller@test.com", SystemRole.USER);
        Vehicle item = new Vehicle("Laptop", "Mô tả", 100.0, "Brand", "Model", 2026);
        Auction auction = new Auction(item, seller, LocalDateTime.now().plusDays(1));

        // 3. Kích hoạt gọi hàm update trực tiếp từ Observer
        observer.update(auction);

        // 4. Lấy dữ liệu dạng chuỗi xuất hiện trên màn hình Console
        String consoleOutput = outputStreamCaptor.toString().trim();

        // 5. Kiểm tra chuỗi in ra xem có đúng với khuôn mẫu cấu trúc trong mã nguồn hay không
        // Khuôn mẫu mong muốn: "Observer Client-01 notified: Auction updated!"
        assertTrue(consoleOutput.contains("Observer Client-01 notified: Auction updated!"));
    }
}