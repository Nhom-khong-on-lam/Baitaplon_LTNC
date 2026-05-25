
import com.auction.common.dto.PaymentDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentDTOTest {
    @Test
    void testPaymentDTO() {
        PaymentDTO dto = new PaymentDTO();
        LocalDateTime now = LocalDateTime.now();

        dto.setId(1L);
        dto.setAuctionId(2L);
        dto.setBuyerId(3L);
        dto.setSellerId(4L);
        dto.setAmount(150000.0);
        dto.setStatus("PAID");
        dto.setCreatedAt(now);
        dto.setSellerBankName("MB Bank");
        dto.setSellerAccountNumber("123456789");
        dto.setSellerCardholderName("NGUYEN VAN SELLER");

        assertEquals(1L, dto.getId());
        assertEquals(2L, dto.getAuctionId());
        assertEquals(3L, dto.getBuyerId());
        assertEquals(4L, dto.getSellerId());
        assertEquals(150000.0, dto.getAmount());
        assertEquals("PAID", dto.getStatus());
        assertEquals(now, dto.getCreatedAt());
        assertEquals("MB Bank", dto.getSellerBankName());
        assertEquals("123456789", dto.getSellerAccountNumber());
        assertEquals("NGUYEN VAN SELLER", dto.getSellerCardholderName());
    }
}