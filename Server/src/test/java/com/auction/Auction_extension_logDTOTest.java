package com.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.common.model.Auction_extension_logDTO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class Auction_extension_logDTOTest {

    @Test
    @DisplayName("1. Test Constructor mặc định")
    void testDefaultConstructor() {

        Auction_extension_logDTO logDTO =
                new Auction_extension_logDTO();

        assertNotNull(logDTO);

        // Giá trị mặc định
        assertEquals(0, logDTO.getId());
        assertEquals(0, logDTO.getAuctionId());
        assertNull(logDTO.getOriginalEndTime());
        assertNull(logDTO.getNewEndTime());
    }

    @Test
    @DisplayName("2. Test Constructor gia hạn auction")
    void testExtensionConstructor() {

        LocalDateTime oldTime =
                LocalDateTime.of(2026, 5, 1, 20, 0);

        LocalDateTime newTime =
                LocalDateTime.of(2026, 5, 1, 20, 15);

        Auction_extension_logDTO logDTO =
                new Auction_extension_logDTO(
                        100L,
                        oldTime,
                        newTime
                );

        assertEquals(100L, logDTO.getAuctionId());

        assertEquals(
                oldTime,
                logDTO.getOriginalEndTime()
        );

        assertEquals(
                newTime,
                logDTO.getNewEndTime()
        );

        // id chưa được set
        assertEquals(0, logDTO.getId());
    }

    @Test
    @DisplayName("3. Test Full Constructor")
    void testFullConstructor() {

        LocalDateTime oldTime =
                LocalDateTime.now();

        LocalDateTime newTime =
                oldTime.plusMinutes(10);

        Auction_extension_logDTO logDTO =
                new Auction_extension_logDTO(
                        1L,
                        200L,
                        oldTime,
                        newTime
                );

        assertEquals(1L, logDTO.getId());
        assertEquals(200L, logDTO.getAuctionId());

        assertEquals(
                oldTime,
                logDTO.getOriginalEndTime()
        );

        assertEquals(
                newTime,
                logDTO.getNewEndTime()
        );
    }

    @Test
    @DisplayName("4. Test Getter và Setter")
    void testGetterSetter() {

        Auction_extension_logDTO logDTO =
                new Auction_extension_logDTO();

        LocalDateTime oldTime =
                LocalDateTime.now();

        LocalDateTime newTime =
                oldTime.plusMinutes(5);

        logDTO.setId(10L);
        logDTO.setAuctionId(99L);
        logDTO.setOriginalEndTime(oldTime);
        logDTO.setNewEndTime(newTime);

        assertEquals(10L, logDTO.getId());
        assertEquals(99L, logDTO.getAuctionId());

        assertEquals(
                oldTime,
                logDTO.getOriginalEndTime()
        );

        assertEquals(
                newTime,
                logDTO.getNewEndTime()
        );
    }

    @Test
    @DisplayName("5. Test toString")
    void testToString() {

        LocalDateTime oldTime =
                LocalDateTime.of(2026, 1, 1, 10, 0);

        LocalDateTime newTime =
                LocalDateTime.of(2026, 1, 1, 10, 10);

        Auction_extension_logDTO logDTO =
                new Auction_extension_logDTO();

        logDTO.setAuctionId(55L);
        logDTO.setOriginalEndTime(oldTime);
        logDTO.setNewEndTime(newTime);

        String result = logDTO.toString();

        assertNotNull(result);

        assertTrue(result.contains("55"));
        assertTrue(result.contains(oldTime.toString()));
        assertTrue(result.contains(newTime.toString()));

        assertEquals(
                "AuctionExtensionLogDTO{" +
                        "auctionId=55" +
                        ", originalEndTime=" + oldTime +
                        ", newEndTime=" + newTime +
                        '}',
                result
        );
    }

    @Test
    @DisplayName("6. Test null values")
    void testNullValues() {

        Auction_extension_logDTO logDTO =
                new Auction_extension_logDTO();

        logDTO.setOriginalEndTime(null);
        logDTO.setNewEndTime(null);

        assertNull(logDTO.getOriginalEndTime());
        assertNull(logDTO.getNewEndTime());
    }

    @Test
    @DisplayName("7. Test update values")
    void testUpdateValues() {

        Auction_extension_logDTO logDTO =
                new Auction_extension_logDTO();

        LocalDateTime firstTime =
                LocalDateTime.now();

        LocalDateTime secondTime =
                firstTime.plusMinutes(20);

        logDTO.setNewEndTime(firstTime);

        assertEquals(
                firstTime,
                logDTO.getNewEndTime()
        );

        logDTO.setNewEndTime(secondTime);

        assertEquals(
                secondTime,
                logDTO.getNewEndTime()
        );
    }

    @Test
    @DisplayName("8. Test new end time after original end time")
    void testNewEndTimeAfterOriginal() {

        LocalDateTime oldTime =
                LocalDateTime.now();

        LocalDateTime newTime =
                oldTime.plusMinutes(15);

        Auction_extension_logDTO logDTO =
                new Auction_extension_logDTO(
                        1L,
                        oldTime,
                        newTime
                );

        assertTrue(
                logDTO.getNewEndTime()
                        .isAfter(logDTO.getOriginalEndTime())
        );
    }
}