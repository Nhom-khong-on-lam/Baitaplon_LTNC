
import com.auction.common.dto.NotificationDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class NotificationDTOTest {
    @Test
    void testNotificationDTO() {
        NotificationDTO dto = new NotificationDTO(1L, "Thắng giải", "Bạn đã thắng cuộc", "AUCTION_WON", 10L, 20L);

        assertEquals(1L, dto.getUserId());
        assertEquals("Thắng giải", dto.getTitle());
        assertEquals("Bạn đã thắng cuộc", dto.getMessage());
        assertEquals("AUCTION_WON", dto.getType());
        assertFalse(dto.isRead());
        assertEquals(10L, dto.getRelatedAuctionId());
        assertEquals(20L, dto.getRelatedBidId());

        dto.setId(99L);
        dto.setRead(true);
        LocalDateTime expire = LocalDateTime.now().plusDays(5);
        dto.setExpiresAt(expire);

        assertEquals(99L, dto.getId());
        assertTrue(dto.isRead());
        assertEquals(expire, dto.getExpiresAt());
    }
}