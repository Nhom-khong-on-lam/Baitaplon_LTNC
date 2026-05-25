
import com.auction.common.dto.Auction_extension_logDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Auction_extension_logDTOTest {
    @Test
    void testAuctionExtensionLogDTO() {
        LocalDateTime original = LocalDateTime.now();
        LocalDateTime extended = original.plusMinutes(10);

        Auction_extension_logDTO log1 = new Auction_extension_logDTO(5L, original, extended);
        assertEquals(5L, log1.getAuctionId());
        assertEquals(original, log1.getOriginalEndTime());
        assertEquals(extended, log1.getNewEndTime());

        Auction_extension_logDTO log2 = new Auction_extension_logDTO(1L, 5L, original, extended);
        assertEquals(1L, log2.getId());

        log2.setId(10L);
        assertEquals(10L, log2.getId());
        assertNotNull(log2.toString());
    }
}