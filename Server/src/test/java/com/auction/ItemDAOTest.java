package com.auction;

import com.auction.common.dto.ItemDTO;
import org.junit.jupiter.api.*;
import server.repository.ItemDAO;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ItemDAO")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Cho phép giữ lại biến insertedItemId xuyên suốt các hàm test
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemDAOTest {

    private ItemDAO itemDAO;
    private long insertedItemId = -1; // Lưu lại ID sinh ra ở Test 1 để Test 3 dùng

    @BeforeAll
    void setUp() {
        itemDAO = new ItemDAO();
    }

    @AfterAll
    void tearDown() {
        // Tự động dọn rác sạch sẽ sau khi kiểm thử xong tất cả
        if (insertedItemId > 0) {
            boolean isDeleted = itemDAO.delete(insertedItemId);
            assertTrue(isDeleted, "Dọn dẹp sản phẩm test thất bại!");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test thêm sản phẩm mới")
    void testInsertItem() {
        ItemDTO item = new ItemDTO();
        item.setName("iPhone 15 Pro Max"); // Đúng hàm .setName() trong DAO của bạn
        item.setDescription("Máy chính hãng, màu Titan xanh");
        item.setStartingPrice(25000000.0);
        item.setCategory("ELECTRONICS");
        item.setBrandMake("Apple");
        item.setModel("iPhone 15");
        item.setArtist(null);
        item.setProductionYear(2023);

        // Gọi hàm insert và hứng lấy ID sinh tự động từ MySQL
        insertedItemId = itemDAO.insert(item);

        // Sử dụng hàm assertTrue chuẩn của JUnit 5
        assertTrue(insertedItemId > 0, "Thêm sản phẩm thất bại, ID trả về phải lớn hơn 0");
    }

    @Test
    @Order(2)
    @DisplayName("Test lấy danh sách sản phẩm")
    void testGetAll() {
        List<ItemDTO> list = itemDAO.getAll();

        assertNotNull(list, "Danh sách sản phẩm không được phép null");
        // Vì Test 1 đã chạy trước và insert thành công, danh sách chắc chắn không được rỗng
        assertFalse(list.isEmpty(), "Danh sách sản phẩm không được trống");
        System.out.println("Tổng số sản phẩm hiện tại trong DB: " + list.size());
    }

    @Test
    @Order(3)
    @DisplayName("Test tìm sản phẩm theo ID")
    void testGetById() {
        // Đảm bảo có ID hợp lệ từ Test 1 truyền sang
        assertTrue(insertedItemId > 0, "Không có ID sản phẩm mẫu hợp lệ để thực hiện tìm kiếm!");

        ItemDTO item = itemDAO.getById(insertedItemId);

        assertNotNull(item, "Phải tìm thấy sản phẩm với ID vừa tạo!");
        assertEquals(insertedItemId, item.getId(), "ID sản phẩm lấy ra không khớp!");
        assertEquals("iPhone 15 Pro Max", item.getName(), "Tên sản phẩm lấy ra không khớp!");
        System.out.println("Tìm thấy sản phẩm: " + item.getName());
    }
}