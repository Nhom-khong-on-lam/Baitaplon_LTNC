package com.auction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.auction.common.dto.ItemDTO;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ItemDTO")
class ItemDTOTest {

    @Test
    @DisplayName("Kiểm tra Getter và Setter của ItemDTO")
    void testItemDTO() {
        ItemDTO item = new ItemDTO();

        // Thiết lập dữ liệu dựa trên sơ đồ ERD
        item.setId(100L);
        item.setName("Rolex Submariner");
        item.setDescription("Đồng hồ cao cấp, tình trạng hoàn hảo.");
        item.setStartingPrice(5000.0);
        item.setCategory("VEHICLE"); // Hoặc ART, ELECTRONICS tùy Enum của bạn
        item.setBrandMake("Rolex");
        item.setModel("Submariner 2023");
        item.setArtist("Swiss Maker");
        item.setProductionYear(2023);

        // Kiểm tra tính chính xác của dữ liệu
        assertAll("Kiểm tra các thuộc tính Item",
                () -> assertEquals(100L, item.getId()),
                () -> assertEquals("Rolex Submariner", item.getName()),
                () -> assertEquals("Đồng hồ cao cấp, tình trạng hoàn hảo.", item.getDescription()),
                () -> assertEquals(5000.0, item.getStartingPrice()),
                () -> assertEquals("VEHICLE", item.getCategory()),
                () -> assertEquals("Rolex", item.getBrandMake()),
                () -> assertEquals("Submariner 2023", item.getModel()),
                () -> assertEquals("Swiss Maker", item.getArtist()),
                () -> assertEquals(2023, item.getProductionYear())
        );
    }
}