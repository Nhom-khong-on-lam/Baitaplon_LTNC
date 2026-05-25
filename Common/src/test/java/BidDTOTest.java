
import com.auction.common.dto.BidDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class BidDTOTest {
    @Test
    void testBidDTO() {
        LocalDateTime now = LocalDateTime.now();
        BidDTO dto = new BidDTO(1L, 2L, 3L, "Nguyen Van A", 2500.0, now, true);

        assertEquals(1L, dto.getId());
        assertEquals(2L, dto.getAuctionId());
        assertEquals(3L, dto.getBidderId());
        assertEquals("Nguyen Van A", dto.getBidderName());
        assertEquals(2500.0, dto.getAmount());
        assertEquals(now, dto.getBidTime());
        assertTrue(dto.isAutoBid());

        dto.setBidderName("Nguyen Van B");
        assertEquals("Nguyen Van B", dto.getBidderName());
    }
}