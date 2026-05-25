package com.auction;

import com.auction.client.service.AuctionService;
import com.auction.common.model.Auction;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test mới: Thử nghiệm gửi dữ liệu Giá dự phòng lên DB và kiểm tra kết quả trả về
 */
public class AuctionReservePriceDatabaseTest {

    private final AuctionService auctionService = new AuctionService();

    @Test
    public void testUpdateAndGetReservePriceFromDatabase() {
        System.out.println("=== BẮT ĐẦU TEST TÍCH HỢP DATABASE ===");

        // 1. Giả định ID của phiên đấu giá đang chạy trên máy bạn (Ví dụ: ID = 1)
        long testAuctionId = 1260001L;
        double expectedReservePrice = 7500000.0; // Giá cược dự phòng muốn thử thêm vào DB

        // 2. MÔ PHỎNG: Gửi gói tin yêu cầu cập nhật giá dự phòng lên Server
        // (Thay vì sửa DB bằng tay, Client dùng Socket gửi lệnh giống y như lúc chạy thật)
        try {
            System.out.println("-> Thử cập nhật giá dự phòng " + expectedReservePrice + " cho phiên ID: " + testAuctionId);

            // Thiết lập Request gửi lên Server (Khớp cấu trúc packet của dự án bạn)
            Request updateReq = new Request("UPDATE_RESERVE_PRICE", new Object[]{testAuctionId, expectedReservePrice});
            Response updateRes = (Response) com.auction.client.service.ServerConnection.getInstance().sendRequest(updateReq);

            // Kiểm tra xem Server đã chạy câu lệnh SQL thành công chưa
            if (updateRes != null && updateRes.isSuccess()) {
                System.out.println("✅ Server báo cáo: Đã thêm/cập nhật thành công vào Database!");
            } else {
                System.out.println("⚠️ Server từ chối cập nhật hoặc hàm UPDATE_RESERVE_PRICE chưa được viết trên Server.");
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối Socket đến Server khi đang thử DB: " + e.getMessage());
        }

        System.out.println("\n---------------------------------------------------");

        // 3. KIỂM TRA: Lấy dữ liệu phiên đấu giá về xem cột reserve_price đã có giá trị chưa
        try {
            System.out.println("-> Đang lấy dữ liệu phiên từ Server về để đối soát...");
            Response response = auctionService.getAuctionById(testAuctionId);

            assertNotNull(response, "Response từ hệ thống không được null");

            if (response.isSuccess() && response.getData() instanceof Auction) {
                Auction auction = (Auction) response.getData();

                System.out.println("📊 Dữ liệu thực tế lấy từ DB về:");
                System.out.println(" - Tên phiên: " + auction.getTitle());
                System.out.println(" - Giá hiện tại: " + String.format("%,.0f", auction.getCurrentPrice()));
                System.out.println(" - Giá dự phòng (Reserve Price): " + String.format("%,.0f", auction.getReservePrice()));

                // Kiểm tra xem giá lấy về có đúng bằng 7.500.000 không
                // Nếu bạn của bạn chưa sửa cột DB, hàm này sẽ trả về 0 và dòng dưới sẽ báo lý do cụ thể
                assertEquals(expectedReservePrice, auction.getReservePrice(),
                        "Thất bại: Cột reserve_price trong DB hiện tại vẫn bằng 0 (Chưa cập nhật DB thật thành công)!");

                System.out.println("🎉 HOÀN THÀNH: Kiểm thử DB thành công mỹ mãn!");
            } else {
                System.out.println("❌ Không thể lấy dữ liệu phiên đấu giá. Vui lòng bật Server trước khi chạy test!");
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi thực hiện đối soát: " + e.getMessage());
        }
    }
}