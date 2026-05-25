
import com.auction.common.dto.Auction_watchDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Auction_watchDTOTest {
    @Test
    void testAuctionWatchDTO() {
        Auction_watchDTO watch1 = new Auction_watchDTO(100L, 200L);
        assertEquals(100L, watch1.getUserId());
        assertEquals(200L, watch1.getAuctionId());
        assertNotNull(watch1.getWatchedAt());

        LocalDateTime now = LocalDateTime.now();
        Auction_watchDTO watch2 = new Auction_watchDTO(1L, 100L, 200L, now);
        assertEquals(1L, watch2.getId());
        assertEquals(now, watch2.getWatchedAt());

        watch2.setUserId(500L);
        watch2.setAuctionId(600L);
        assertEquals(500L, watch2.getUserId());
        assertEquals(600L, watch2.getAuctionId());
        assertNotNull(watch2.toString());
    }
}