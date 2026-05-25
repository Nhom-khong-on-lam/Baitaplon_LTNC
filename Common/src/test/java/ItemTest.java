

import com.auction.common.model.Art;
import com.auction.common.model.Electronics;
import com.auction.common.model.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemTest {

    @Test
    void testArtItem() {
        Art art = new Art("Mona Lisa", "Bức tranh nổi tiếng", 1000.0, "Leonardo da Vinci", 1503);

        assertEquals("Art", art.getCategory());
        assertEquals("Leonardo da Vinci", art.getArtist());
        assertEquals(1503, art.getYear());
        assertEquals("Mona Lisa", art.getName());
        assertEquals(1000.0, art.getStartingPrice());
    }

    @Test
    void testElectronicsItem() {
        Electronics electronics = new Electronics("iPhone 15", "Điện thoại Apple", 999.0, "Apple", "Pro Max");

        assertEquals("Electronics", electronics.getCategory());
        assertEquals("Apple", electronics.getBrand());
        assertEquals("Pro Max", electronics.getModel());
        assertEquals(999.0, electronics.getCurrentPrice());
    }

    @Test
    void testVehicleItem() {
        Vehicle vehicle = new Vehicle("Civic", "Xe sedan", 25000.0, "Honda", "Civic RS", 2024);

        assertEquals("Vehicle", vehicle.getCategory());
        assertEquals("Honda", vehicle.getMake());
        assertEquals("Civic RS", vehicle.getModel());
        assertEquals(2024, vehicle.getYear());
    }
}