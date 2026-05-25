
import com.auction.common.dto.AutoBidDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class AutoBidDTOTest {
    @Test
    void testAutoBidDTO() {
        LocalDateTime now = LocalDateTime.now();
        AutoBidDTO dto = new AutoBidDTO(1L, 2L, 3L, 5000.0, 50.0, true, now);

        assertEquals(1L, dto.getId());
        assertEquals(2L, dto.getAuctionId());
        assertEquals(3L, dto.getBidderId());
        assertEquals(5000.0, dto.getMaxPrice());
        assertEquals(50.0, dto.getStepIncrement());
        assertTrue(dto.isActive());
        assertEquals(now, dto.getRegisteredAt());

        dto.setId(10L);
        dto.setActive(false);
        assertEquals(10L, dto.getId());
        assertFalse(dto.isActive());
    }
}