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
        Auction_watchDTO dto = new Auction_watchDTO();
        assertNotNull(dto);
        assertEquals(0, dto.getId());
        assertEquals(0, dto.getUserId());
        assertEquals(0, dto.getAuctionId());
        assertNull(dto.getWatchedAt());
    }

    @Test
    @DisplayName("2. Test Constructor cho việc thêm mới")
    void testAddConstructor() {
        Auction_watchDTO dto = new Auction_watchDTO(100L, 200L);

        assertEquals(100L, dto.getUserId());
        assertEquals(200L, dto.getAuctionId());
        assertNotNull(dto.getWatchedAt()); // watchedAt được khởi tạo LocalDateTime.now()
    }

    @Test
    @DisplayName("3. Test Constructor đầy đủ từ DAO")
    void testFullConstructor() {
        LocalDateTime time = LocalDateTime.now();
        Auction_watchDTO dto = new Auction_watchDTO(1L, 500L, 600L, time);

        assertEquals(1L, dto.getId());
        assertEquals(500L, dto.getUserId());
        assertEquals(600L, dto.getAuctionId());
        assertEquals(time, dto.getWatchedAt());
    }

    @Test
    @DisplayName("4. Test Getter và Setter")
    void testGetterSetter() {
        Auction_watchDTO dto = new Auction_watchDTO();
        LocalDateTime time = LocalDateTime.now();

        dto.setId(99L);
        dto.setUserId(88L);
        dto.setAuctionId(77L);
        dto.setWatchedAt(time);

        assertEquals(99L, dto.getId());
        assertEquals(88L, dto.getUserId());
        assertEquals(77L, dto.getAuctionId());
        assertEquals(time, dto.getWatchedAt());
    }

    @Test
    @DisplayName("5. Test toString")
    void testToString() {
        Auction_watchDTO dto = new Auction_watchDTO(10L, 20L);
        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("userId=10"));
        assertTrue(result.contains("auctionId=20"));
    }
}