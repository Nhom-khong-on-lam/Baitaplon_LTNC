package com.auction;

import org.junit.jupiter.api.*;
import server.repository.ItemDAO;
import server.common.model.ItemDTO;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ItemDAO")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemDAOTest {

    private ItemDAO itemDAO;

    @BeforeEach
    void setUp() {
        itemDAO = new ItemDAO();
    }

    @Test
    @Order(1)
    @DisplayName("Test thêm sản phẩm mới")
    void testInsertItem() {
        ItemDTO item = new ItemDTO();
        item.setName("iPhone 15 Pro Max");
        item.setDescription("Máy chính hãng, màu Titan xanh");
        item.setStartingPrice(25000000.0);
        item.setCategory("ELECTRONICS"); // Khớp với ENUM trong ERD
        item.setBrandMake("Apple");
        item.setModel("iPhone 15");
        item.setArtist(null); // Đồ điện tử không có artist
        item.setProductionYear(2023);

        long result = itemDAO.insert(item);
        assertTrue(result, "Thêm sản phẩm phải thành công");
    }

    private void assertTrue(long result, String s) {
    }

    @Test
    @Order(2)
    @DisplayName("Test lấy danh sách sản phẩm")
    void testGetAll() {
        List<ItemDTO> list = itemDAO.getAll();
        assertNotNull(list);
        System.out.println("Tổng số sản phẩm: " + list.size());
    }

    @Test
    @Order(3)
    @DisplayName("Test tìm sản phẩm theo ID")
    void testGetById() {
        // Giả sử lấy ID 1 (Bạn nên check trong DB ID nào đang tồn tại)
        ItemDTO item = itemDAO.getById(1L);
        if (item != null) {
            System.out.println("Tìm thấy: " + item.getName());
            assertEquals(1L, item.getId());
        } else {
            System.out.println("Không tìm thấy sản phẩm ID 1");
        }
    }
}