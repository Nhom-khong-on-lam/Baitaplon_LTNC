

import com.auction.common.model.Electronics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ElectronicsTest {

    @Test
    void testElectronicsConstructorAndGetters() {
        // Khởi tạo đối tượng Electronics với đầy đủ tham số
        Electronics electronics = new Electronics(
                "MacBook Pro M3",
                "Laptop Apple chính hãng 16 inch", 2499.0,
                "Apple",
                "M3 Max"
        );

        // Kiểm tra các thuộc tính chung kế thừa từ Item
        assertEquals("MacBook Pro M3", electronics.getName());
        assertEquals("Laptop Apple chính hãng 16 inch", electronics.getDescription());
        assertEquals(2499.0, electronics.getStartingPrice());
        assertEquals(2499.0, electronics.getCurrentPrice());
        assertEquals("ACTIVE", electronics.getStatus());

        // Kiểm tra các thuộc tính riêng của lớp Electronics
        assertEquals("Apple", electronics.getBrand());
        assertEquals("M3 Max", electronics.getModel());
    }

    @Test
    void testGetCategory() {
        Electronics electronics = new Electronics("S24 Ultra", "Điện thoại", 1200.0, "Samsung", "Ultra");

        // Kiểm tra danh mục trả về chính xác
        assertEquals("Electronics", electronics.getCategory());
    }
}