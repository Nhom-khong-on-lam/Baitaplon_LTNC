package com.auction;

import com.auction.client.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

public class CloudinaryServiceTest {

    @Test
    void testUploadImage_WithNullFile_ReturnsNull() {
        // Khi truyền vào file null, hàm phải xử lý an toàn trả về null
        String url = CloudinaryService.uploadImage(null);
        assertNull(url);
    }

    @Test
    void testUploadImage_WithNonExistentFile_ReturnsNull() {
        // Khi truyền vào file không có thực trên đĩa, hệ thống trả về null
        File nonExistentFile = new File("duong_dan_ao_kiem_tra_loi_12345.png");
        String url = CloudinaryService.uploadImage(nonExistentFile);
        assertNull(url);
    }
}