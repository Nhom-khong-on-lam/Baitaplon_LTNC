
import com.auction.common.dto.ItemImageDTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ItemImageDTOTest {
    @Test
    void testItemImageDTO_Valid() {
        ItemImageDTO dto = new ItemImageDTO(5L, "http://image.com/1.png");
        assertEquals(5L, dto.getItemId());
        assertEquals("http://image.com/1.png", dto.getImageUrl());

        dto.setId(1L);
        assertEquals(1L, dto.getId());
    }

    @Test
    void testItemImageDTO_Invalid_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ItemImageDTO(null, "http://image.com/1.png");
        });
    }
}