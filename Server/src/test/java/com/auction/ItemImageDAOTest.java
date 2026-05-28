package com.auction;

import com.auction.common.dto.ItemDTO;
import com.auction.common.dto.ItemImageDTO;
import org.junit.jupiter.api.*;
import server.repository.ItemDAO;
import server.repository.ItemImageDAO; // 1. IMPORT CẢ 2 DAO ĐỂ PHỐI HỢP TEST

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ItemImageDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Giúp chia sẻ ID mồi xuyên suốt các hàm test
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemImageDAOTest {

    private ItemImageDAO itemImageDAO;
    private ItemDAO itemDAO;

    private long testItemId = -1; // ID của Item mồi dùng để gán ảnh

    @BeforeAll
    void initAll() {
        itemImageDAO = new ItemImageDAO();
        itemDAO = new ItemDAO();

        // 2. CHỦ ĐỘNG TẠO SẢN PHẨM MỒI ĐỂ TRÁNH LỖI KHÓA NGOẠI (FOREIGN KEY)
        try {
            ItemDTO mockItem = new ItemDTO();
            mockItem.setName("Sản phẩm test ảnh");
            mockItem.setDescription("Dùng để mồi data test ItemImageDAO");
            mockItem.setStartingPrice(50000.0);
            mockItem.setCategory("ELECTRONICS");

            // Lấy ID tự sinh từ MySQL gán vào biến testItemId
            testItemId = itemDAO.insert(mockItem);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Đảm bảo phải mồi được sản phẩm thành công mới cho chạy test ảnh
        assertTrue(testItemId > 0, "Không thể tạo sản phẩm mồi, hủy test gán ảnh!");
    }

    @AfterAll
    void tearDownAll() {
        // 3. DỌN SẠCH DỮ LIỆU THEO THỨ TỰ NGƯỢC (REVERSE ORDER)
        try {
            if (testItemId > 0) {
                // Xóa hết ảnh của sản phẩm trước
                itemImageDAO.deleteByItemId(testItemId);
                // Xóa sản phẩm gốc sau
                itemDAO.delete(testItemId);
            }
        } catch (Exception e) {
            System.out.println("Lỗi dọn dẹp data test ảnh: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test thêm ảnh mới cho sản phẩm")
    void testInsertItemImage() {
        ItemImageDTO img = new ItemImageDTO();
        img.setItemId(testItemId); // Dùng chính ID của sản phẩm mồi vừa tạo
        img.setImageUrl("https://uet.edu.vn/images/iphone15_blue.png");

        // Gọi hàm insert (hàm này trong DAO của bạn trả về boolean)
        boolean isInserted = itemImageDAO.insert(img);

        // Sử dụng hàm assertTrue của JUnit 5 kiểm tra kết quả trả về
        assertTrue(isInserted, "Thêm ảnh thất bại! Hãy chắc chắn Item ID hợp lệ.");
    }

    @Test
    @Order(2)
    @DisplayName("Test lấy danh sách ảnh theo Item ID")
    void testGetImagesByItemId() {
        List<ItemImageDTO> images = itemImageDAO.getImagesByItemId(testItemId);

        assertNotNull(images, "Danh sách ảnh trả về không được null");
        assertFalse(images.isEmpty(), "Danh sách ảnh không được trống sau khi đã insert");
        assertEquals(testItemId, images.get(0).getItemId(), "Item ID của ảnh lấy ra không khớp!");
        assertEquals("https://uet.edu.vn/images/iphone15_blue.png", images.get(0).getImageUrl());
    }

    @Test
    @Order(3)
    @DisplayName("Test lấy tất cả ảnh trong hệ thống")
    void testGetAll() {
        List<ItemImageDTO> allImages = itemImageDAO.getAll();

        assertNotNull(allImages, "Danh sách toàn bộ ảnh không được null");
        assertFalse(allImages.isEmpty(), "Hệ thống phải chứa ít nhất 1 ảnh vừa được tạo");
    }

    @Test
    @Order(4)
    @DisplayName("Test xóa toàn bộ ảnh của một sản phẩm")
    void testDeleteByItemId() {
        boolean isDeleted = itemImageDAO.deleteByItemId(testItemId);
        assertTrue(isDeleted, "Xóa toàn bộ ảnh của sản phẩm thất bại!");

        // Kiểm tra lại xem danh sách ảnh của sản phẩm này thực sự đã trống rỗng chưa
        List<ItemImageDTO> emptyList = itemImageDAO.getImagesByItemId(testItemId);
        assertTrue(emptyList.isEmpty(), "Danh sách ảnh phải trống rỗng sau khi đã xóa!");
    }
}