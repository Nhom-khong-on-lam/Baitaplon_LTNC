
import com.auction.common.dto.ItemDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemDTOTest {
    @Test
    void testItemDTO() {
        ItemDTO dto = new ItemDTO();
        LocalDateTime now = LocalDateTime.now();

        dto.setId(100L);
        dto.setName("iPhone 15");
        dto.setDescription("Like new 99%");
        dto.setStartingPrice(999.0);
        dto.setCategory("ELECTRONICS");
        dto.setBrandMake("Apple");
        dto.setModel("Pro Max");
        dto.setArtist("Foxconn");
        dto.setProductionYear(2023);
        dto.setCreatedAt(now);

        assertEquals(100L, dto.getId());
        assertEquals("iPhone 15", dto.getName());
        assertEquals("Like new 99%", dto.getDescription());
        assertEquals(999.0, dto.getStartingPrice());
        assertEquals("ELECTRONICS", dto.getCategory());
        assertEquals("Apple", dto.getBrandMake());
        assertEquals("Pro Max", dto.getModel());
        assertEquals("Foxconn", dto.getArtist());
        assertEquals(2023, dto.getProductionYear());
        assertEquals(now, dto.getCreatedAt());
    }
}