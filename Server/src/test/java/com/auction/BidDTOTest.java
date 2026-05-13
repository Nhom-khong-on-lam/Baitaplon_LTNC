package com.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.common.model.BidDTO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BidDTOTest {

    @Test
    @DisplayName("Test khởi tạo và dùng Setter/Getter cho BidDTO")
    void testSettersAndGetters() {
        BidDTO bid = new BidDTO();
        LocalDateTime now = LocalDateTime.now();

        bid.setId(1L);
        bid.setAuctionId(10L);
        bid.setBidderId(100L);
        bid.setBidderName("nhuanh_test");
        bid.setAmount(5500.0);
        bid.setBidTime(now);
        bid.setAutoBid(true);

        assertEquals(1L, bid.getId());
        assertEquals(10L, bid.getAuctionId());
        assertEquals(100L, bid.getBidderId());
        assertEquals("nhuanh_test", bid.getBidderName());
        assertEquals(5500.0, bid.getAmount(), 0.001);
        assertEquals(now, bid.getBidTime());
        assertTrue(bid.isAutoBid());
    }

    @Test
    @DisplayName("Test Constructor đầy đủ tham số")
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        BidDTO bid = new BidDTO(5L, 20L, 200L, "tester", 1000.0, now, false);

        assertEquals(5L, bid.getId());
        assertEquals(20L, bid.getAuctionId());
        assertEquals(200L, bid.getBidderId());
        assertEquals("tester", bid.getBidderName());
        assertEquals(1000.0, bid.getAmount(), 0.001);
        assertEquals(now, bid.getBidTime());
        assertFalse(bid.isAutoBid());
    }
}