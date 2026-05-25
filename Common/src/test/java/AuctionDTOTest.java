

import com.auction.common.dto.AuctionDTO;
import com.auction.common.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionDTOTest {
    @Test
    void testGettersAndSetters() {
        AuctionDTO dto = new AuctionDTO();
        LocalDateTime now = LocalDateTime.now();

        dto.setId(1L);
        dto.setItemId(10L);
        dto.setSellerId(100L);
        dto.setHighestBidderId(200L);
        dto.setCurrentPrice(1500.0);
        dto.setStartTime(now);
        dto.setEndTime(now.plusDays(1));
        dto.setStatus("ACTIVE");
        dto.setPaymentStatus(PaymentStatus.PENDING);
        dto.setItemName("Tranh Sơn Dầu");
        dto.setCategory("ART");
        dto.setSellerUsername("seller_pro");

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getItemId());
        assertEquals(100L, dto.getSellerId());
        assertEquals(200L, dto.getHighestBidderId());
        assertEquals(1500.0, dto.getCurrentPrice());
        assertEquals(now, dto.getStartTime());
        assertEquals(now.plusDays(1), dto.getEndTime());
        assertEquals("ACTIVE", dto.getStatus());
        assertEquals(PaymentStatus.PENDING, dto.getPaymentStatus());
        assertEquals("Tranh Sơn Dầu", dto.getItemName());
        assertEquals("ART", dto.getCategory());
        assertEquals("seller_pro", dto.getSellerUsername());
    }
}