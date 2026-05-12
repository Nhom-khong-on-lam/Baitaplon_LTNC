package com.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.common.model.PaymentDTO;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử PaymentDTO")
class PaymentDTOTest {

    @Test
    @DisplayName("Kiểm tra Getter và Setter của PaymentDTO")
    void testGetterSetter() {
        PaymentDTO p = new PaymentDTO();
        LocalDateTime now = LocalDateTime.now();

        // Thiết lập dữ liệu qua Setter
        p.setId(10L);
        p.setAuctionId(1L);
        p.setBuyerId(2L);
        p.setSellerId(3L);
        p.setAmount(500000.0);
        p.setStatus("COMPLETED");
        p.setCreatedAt(now);

        // Kiểm tra dữ liệu qua Getter
        assertAll("Kiểm tra tất cả các trường dữ liệu",
                () -> assertEquals(10L, p.getId()),
                () -> assertEquals(1L, p.getAuctionId()),
                () -> assertEquals(2L, p.getBuyerId()),
                () -> assertEquals(3L, p.getSellerId()),
                () -> assertEquals(500000.0, p.getAmount()),
                () -> assertEquals("COMPLETED", p.getStatus()),
                () -> assertEquals(now, p.getCreatedAt())
        );
    }
}