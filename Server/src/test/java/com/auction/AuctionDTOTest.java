package com.auction;

import org.junit.jupiter.api.Test;
import server.common.model.AuctionDTO;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AuctionDTOTest {
    @Test
    void testAuctionDTO() {
        AuctionDTO auction = new AuctionDTO();

        // 1. Set các giá trị Long trực tiếp (vì DTO của bạn không có setSeller(Object))
        auction.setId(1L);
        auction.setItemId(10L);
        auction.setSellerId(2L); // Sử dụng setSellerId thay vì setSeller
        auction.setHighestBidderId(3L);

        auction.setCurrentPrice(2000.0);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(LocalDateTime.now().plusDays(1));

        // 2. Set status bằng String (vì DTO của bạn đang để String status)
        auction.setStatus("OPEN");

        assertAll("Kiểm tra AuctionDTO",
                () -> assertEquals(1L, auction.getId(), "ID không khớp"),
                () -> assertEquals(10L, auction.getItemId(), "Item ID không khớp"),
                () -> assertEquals(2L, auction.getSellerId(), "Seller ID không khớp"),
                () -> assertEquals("OPEN", auction.getStatus(), "Status không khớp")
        );
    }
}