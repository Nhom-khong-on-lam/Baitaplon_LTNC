package com.auction;

import com.auction.common.dto.AutoBidDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AutoBidDTOTest {

    @Test
    @DisplayName("Test khởi tạo bằng Constructor rỗng và dùng Setter/Getter")
    void testNoArgsConstructorAndSetters() {
        // Arrange: Chuẩn bị dữ liệu
        AutoBidDTO autoBid = new AutoBidDTO();
        LocalDateTime testTime = LocalDateTime.now();

        // Act: Gán giá trị bằng Setter
        autoBid.setId(10L);
        autoBid.setAuctionId(100L);
        autoBid.setBidderId(200L);
        autoBid.setMaxPrice(5500.0);
        autoBid.setStepIncrement(250.0);
        autoBid.setActive(true);
        autoBid.setRegisteredAt(testTime);

        // Assert: Kiểm tra lại bằng Getter xem có khớp không
        assertEquals(10L, autoBid.getId());
        assertEquals(100L, autoBid.getAuctionId());
        assertEquals(200L, autoBid.getBidderId());

        // So sánh số thực (double) luôn cần tham số delta (độ sai số cho phép)
        assertEquals(5500.0, autoBid.getMaxPrice(), 0.001);
        assertEquals(250.0, autoBid.getStepIncrement(), 0.001);

        assertTrue(autoBid.isActive());
        assertEquals(testTime, autoBid.getRegisteredAt());
    }

    @Test
    @DisplayName("Test khởi tạo bằng Constructor đầy đủ tham số")
    void testAllArgsConstructor() {
        // Arrange & Act: Khởi tạo thẳng bằng constructor
        LocalDateTime testTime = LocalDateTime.now();
        AutoBidDTO autoBid = new AutoBidDTO(5L, 150L, 300L, 10000.0, 500.0, false, testTime);

        // Assert: Kiểm tra lại dữ liệu
        assertEquals(5L, autoBid.getId());
        assertEquals(150L, autoBid.getAuctionId());
        assertEquals(300L, autoBid.getBidderId());
        assertEquals(10000.0, autoBid.getMaxPrice(), 0.001);
        assertEquals(500.0, autoBid.getStepIncrement(), 0.001);
        assertFalse(autoBid.isActive());
        assertEquals(testTime, autoBid.getRegisteredAt());
    }
}