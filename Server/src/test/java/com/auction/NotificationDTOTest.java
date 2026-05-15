package com.auction;

import com.auction.common.dto.NotificationDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDTOTest {

    @Test
    void testNotificationCreationAndDefaults() {
        // Tạo một thông báo bằng Constructor đầy đủ
        NotificationDTO notif = new NotificationDTO(1L, "Có người trả giá cao hơn", "User ABC vừa trả 500k cho sản phẩm của bạn", "OUTBID", 5L, 20L);

        // Xác minh các dữ liệu thông thường
        assertEquals(1L, notif.getUserId());
        assertEquals("Có người trả giá cao hơn", notif.getTitle());
        assertEquals("OUTBID", notif.getType());
        assertEquals(5L, notif.getRelatedAuctionId());
        assertEquals(20L, notif.getRelatedBidId());

        // XÁC MINH CÁC GIÁ TRỊ MẶC ĐỊNH (Cực kỳ quan trọng)
        assertFalse(notif.isRead(), "Khi vừa khởi tạo, thông báo phải có trạng thái chưa đọc (isRead = false)");
        assertNotNull(notif.getCreatedAt(), "Thời gian tạo (createdAt) không được để trống khi dùng Constructor này");
    }
}