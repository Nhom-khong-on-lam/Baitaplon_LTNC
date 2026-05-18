package com.auction;

import com.auction.common.dto.ItemImageDTO;
import org.junit.jupiter.api.*;
import server.repository.ItemImageDAO;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ItemImageDAO - Quản lý ảnh sản phẩm")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemImageDAOTest {

    private ItemImageDAO itemImageDAO;

    @BeforeEach
    void setUp() {
        itemImageDAO = new ItemImageDAO();
    }

    @Test
    @Order(1)
    @DisplayName("Test thêm ảnh mới bằng Constructor đầy đủ tham số")
    void testInsertItemImage() {
        // Sử dụng Constructor bạn vừa tạo: ItemImageDTO(Long itemId, String imageUrl)
        ItemImageDTO img = new ItemImageDTO(1L, "https://auction-system.com/images/macbook_v1.jpg");

        boolean result = itemImageDAO.insert(img);

        // Lưu ý: Sẽ xanh (Pass) nếu bảng item đã có ID 1
        assertTrue(result, "Thêm ảnh thất bại! Hãy chắc chắn Item ID 1 đã tồn tại trong DB.");
    }

    @Test
    @Order(2)
    @DisplayName("Test lấy danh sách ảnh của một sản phẩm")
    void testGetImagesByItemId() {
        long itemId = 1L;
        List<ItemImageDTO> images = itemImageDAO.getImagesByItemId(itemId);

        assertNotNull(images, "Danh sách ảnh không được null");
        System.out.println("Sản phẩm " + itemId + " hiện có " + images.size() + " ảnh.");
    }

    @Test
    @Order(3)
    @DisplayName("Test xóa ảnh theo Item ID")
    void testDeleteImages() {
        long itemId = 1L;
        // Hàm này sẽ trả về true nếu có ít nhất 1 dòng bị xóa
        boolean result = itemImageDAO.deleteByItemId(itemId);

        if (result) {
            System.out.println("Đã dọn dẹp sạch ảnh của sản phẩm " + itemId);
        } else {
            System.out.println("Không có ảnh nào để xóa hoặc ID không tồn tại.");
        }
    }
}