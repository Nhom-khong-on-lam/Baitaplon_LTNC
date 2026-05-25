

import com.auction.common.model.Art;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArtTest {

    @Test
    void testArtConstructorAndGetters() {
        // Khởi tạo đối tượng Art với đầy đủ tham số
        Art art = new Art(
                "Mona Lisa",
                "Bức chân dung thế kỷ 16 kiệt tác của Leonardo da Vinci",
                1000000.0,
                "Leonardo da Vinci",
                1503
        );

        // Kiểm tra các thuộc tính chung kế thừa từ Item
        assertEquals("Mona Lisa", art.getName());
        assertEquals("Bức chân dung thế kỷ 16 kiệt tác của Leonardo da Vinci", art.getDescription());
        assertEquals(1000000.0, art.getStartingPrice());
        assertEquals(1000000.0, art.getCurrentPrice());
        assertEquals("ACTIVE", art.getStatus());

        // Kiểm tra các thuộc tính riêng của lớp Art
        assertEquals("Leonardo da Vinci", art.getArtist());
        assertEquals(1503, art.getYear());
    }

    @Test
    void testGetCategory() {
        Art art = new Art("Em Thúy", "Tranh sơn dầu", 5000.0, "Trần Văn Cẩn", 1943);

        // Kiểm tra danh mục trả về chính xác
        assertEquals("Art", art.getCategory());
    }
}