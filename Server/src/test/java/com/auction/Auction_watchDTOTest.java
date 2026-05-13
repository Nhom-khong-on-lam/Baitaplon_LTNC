package com.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.common.model.Auction_watchDTO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class Auction_watchDTOTest {

    @Test
    @DisplayName("1. Test Constructor mặc định")
    void testDefaultConstructor() {

        Auction_watchDTO watchDTO = new Auction_watchDTO();

        assertNotNull(watchDTO);

        // Giá trị mặc định
        assertEquals(0, watchDTO.getId());
        assertEquals(0, watchDTO.getUserId());
        assertEquals(0, watchDTO.getAuctionId());
        assertNull(watchDTO.getCreatedAt());
        assertNull(watchDTO.getAuctionTitle());
    }

    @Test
    @DisplayName("2. Test Constructor Theo Dõi Auction")
    void testWatchConstructor() {

        Auction_watchDTO watchDTO =
                new Auction_watchDTO(100L, 200L);

        assertEquals(100L, watchDTO.getUserId());
        assertEquals(200L, watchDTO.getAuctionId());

        // createdAt phải tự động tạo
        assertNotNull(watchDTO.getCreatedAt());

        // id chưa được set
        assertEquals(0, watchDTO.getId());
    }

    @Test
    @DisplayName("3. Test Full Constructor")
    void testFullConstructor() {

        LocalDateTime now = LocalDateTime.now();

        Auction_watchDTO watchDTO =
                new Auction_watchDTO(
                        1L,
                        10L,
                        20L,
                        now
                );

        assertEquals(1L, watchDTO.getId());
        assertEquals(10L, watchDTO.getUserId());
        assertEquals(20L, watchDTO.getAuctionId());
        assertEquals(now, watchDTO.getCreatedAt());
    }

    @Test
    @DisplayName("4. Test Getter và Setter")
    void testGetterSetter() {

        Auction_watchDTO watchDTO =
                new Auction_watchDTO();

        LocalDateTime now = LocalDateTime.now();

        watchDTO.setId(99L);
        watchDTO.setUserId(5L);
        watchDTO.setAuctionId(10L);
        watchDTO.setCreatedAt(now);
        watchDTO.setAuctionTitle("Laptop Gaming Auction");

        assertEquals(99L, watchDTO.getId());
        assertEquals(5L, watchDTO.getUserId());
        assertEquals(10L, watchDTO.getAuctionId());
        assertEquals(now, watchDTO.getCreatedAt());

        assertEquals(
                "Laptop Gaming Auction",
                watchDTO.getAuctionTitle()
        );
    }

    @Test
    @DisplayName("5. Test toString")
    void testToString() {

        Auction_watchDTO watchDTO =
                new Auction_watchDTO();

        watchDTO.setUserId(11L);
        watchDTO.setAuctionId(22L);

        String result = watchDTO.toString();

        assertNotNull(result);

        assertTrue(result.contains("11"));
        assertTrue(result.contains("22"));

        assertEquals(
                "AuctionWatchDTO{userId=11, auctionId=22}",
                result
        );
    }

    @Test
    @DisplayName("6. Test null Auction Title")
    void testNullAuctionTitle() {

        Auction_watchDTO watchDTO =
                new Auction_watchDTO();

        watchDTO.setAuctionTitle(null);

        assertNull(watchDTO.getAuctionTitle());
    }

    @Test
    @DisplayName("7. Test update values")
    void testUpdateValues() {

        Auction_watchDTO watchDTO =
                new Auction_watchDTO();

        watchDTO.setAuctionTitle("Old Auction");
        assertEquals(
                "Old Auction",
                watchDTO.getAuctionTitle()
        );

        watchDTO.setAuctionTitle("New Auction");

        assertEquals(
                "New Auction",
                watchDTO.getAuctionTitle()
        );
    }

    @Test
    @DisplayName("8. Test createdAt auto generate")
    void testCreatedAtAutoGenerate() {

        Auction_watchDTO watchDTO =
                new Auction_watchDTO(1L, 2L);

        LocalDateTime now = LocalDateTime.now();

        assertNotNull(watchDTO.getCreatedAt());

        // createdAt không được lớn hơn thời điểm hiện tại
        assertTrue(
                watchDTO.getCreatedAt().isBefore(now)
                        || watchDTO.getCreatedAt().isEqual(now)
        );
    }
}