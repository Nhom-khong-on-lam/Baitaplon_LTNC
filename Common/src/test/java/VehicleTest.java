

import com.auction.common.model.Vehicle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VehicleTest {

    @Test
    void testVehicleConstructorAndGetters() {
        // Khởi tạo đối tượng Vehicle với đầy đủ tham số
        Vehicle vehicle = new Vehicle(
                "VinFast VF8",
                "Xe điện thông minh, màu đen",
                50000.0,
                "VinFast",
                "VF8 Plus",
                2024
        );

        // Kiểm tra các thuộc tính chung kế thừa từ Item
        assertEquals("VinFast VF8", vehicle.getName());
        assertEquals("Xe điện thông minh, màu đen", vehicle.getDescription());
        assertEquals(50000.0, vehicle.getStartingPrice());
        assertEquals(50000.0, vehicle.getCurrentPrice()); // Mặc định currentPrice bằng startingPrice
        assertEquals("ACTIVE", vehicle.getStatus()); // Trạng thái mặc định khi tạo mới

        // Kiểm tra các thuộc tính riêng của lớp Vehicle
        assertEquals("VinFast", vehicle.getMake());
        assertEquals("VF8 Plus", vehicle.getModel());
        assertEquals(2024, vehicle.getYear());
    }

    @Test
    void testGetCategory() {
        Vehicle vehicle = new Vehicle("Civic", "Xe cũ", 15000.0, "Honda", "Civic", 2020);

        // Kiểm tra danh mục trả về chính xác
        assertEquals("Vehicle", vehicle.getCategory());
    }
}