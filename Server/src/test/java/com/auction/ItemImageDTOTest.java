package com.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import server.common.model.ItemImageDTO;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ItemImageDTO")
class ItemImageDTOTest {

    @Test
    @DisplayName("Kiểm tra Getter và Setter của ItemImageDTO")
    void testItemImageDTO() {
        ItemImageDTO itemImage = new ItemImageDTO();

        // Thiết lập dữ liệu dựa trên sơ đồ ERD
        itemImage.setId(500L);
        itemImage.setItemId(10L);
        itemImage.setImageUrl("https://example.com/images/iphone15.jpg");

        // Kiểm tra tính chính xác của dữ liệu
        assertAll("Kiểm tra các thuộc tính ItemImage",
                () -> assertEquals(500L, itemImage.getId(), "ID không khớp"),
                () -> assertEquals(10L, itemImage.getItemId(), "Item ID không khớp"),
                () -> assertEquals("https://example.com/images/iphone15.jpg", itemImage.getImageUrl(), "Image URL không khớp")
        );
    }

    @Test
    @DisplayName("Kiểm tra Constructor có tham số (nếu có)")
    void testConstructor() {
        // Kiểm tra constructor nhanh nếu bạn có định nghĩa trong DTO
        ItemImageDTO itemImage = new ItemImageDTO(10L, "https://example.com/test.png");

        assertAll("Kiểm tra Constructor",
                () -> assertEquals(10L, itemImage.getItemId()),
                () -> assertEquals("https://example.com/test.png", itemImage.getImageUrl())
        );
    }
}