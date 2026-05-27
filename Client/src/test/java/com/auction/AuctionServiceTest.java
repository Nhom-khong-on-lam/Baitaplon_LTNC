package com.auction;

import com.auction.client.service.AuctionService;
import com.auction.client.service.ServerConnection;
import com.auction.common.dto.PaymentDTO;
import com.auction.common.model.Auction;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuctionServiceTest {

    private AuctionService auctionService;
    private ServerConnection mockConnection;
    private MockedStatic<ServerConnection> staticConnectionMock;

    @BeforeEach
    void setUp() {
        // 1. Tạo instance giả lập (mock) cho ServerConnection
        mockConnection = mock(ServerConnection.class);

        // 2. Chặn đầu phương thức tĩnh ServerConnection.getInstance() bằng MockedStatic
        staticConnectionMock = mockStatic(ServerConnection.class);
        staticConnectionMock.when(ServerConnection::getInstance).thenReturn(mockConnection);

        // 3. Khởi tạo service chạy test
        auctionService = new AuctionService();
    }

    @AfterEach
    void tearDown() {
        // Bắt buộc phải đóng static mock để tránh lỗi chồng chéo luồng test (Thread)
        if (staticConnectionMock != null) {
            staticConnectionMock.close();
        }
    }

    // ── TEST: getAllAuctions() ───────────────────────────────────────────────
    @Test
    void testGetAllAuctions_Success() throws Exception {
        // Tạo dữ liệu giả lập danh sách đấu giá mẫu
        List<Auction> expectedAuctions = new ArrayList<>();
        Auction auction = new Auction();
        auction.setId(1L);
        expectedAuctions.add(auction);

        // Sử dụng chính xác hàm Response.ok(Object) từ class của bạn
        Response mockResponse = Response.ok(expectedAuctions);

        // Khi Service gọi mạng, bắt ServerConnection trả về mockResponse vừa tạo
        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        List<Auction> result = auctionService.getAllAuctions();

        // Kiểm định tính đúng đắn của dữ liệu nhận về
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void testGetAllAuctions_FailureOrEmpty() throws Exception {
        // Sử dụng hàm Response.error(String) từ class của bạn
        Response mockResponse = Response.error("Server internal error");
        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        List<Auction> result = auctionService.getAllAuctions();

        // Kiểm tra xem hệ thống có tự trả về danh sách rỗng (để tránh NullPointerException) không
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── TEST: createPayment() ────────────────────────────────────────────────
    @Test
    void testCreatePayment_Success() throws Exception {
        PaymentDTO paymentDTO = new PaymentDTO();
        Response mockResponse = Response.ok("Payment processed successfully");

        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        Response response = auctionService.createPayment(paymentDTO);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Payment processed successfully", response.getData());
    }

    @Test
    void testCreatePayment_Exception() throws Exception {
        PaymentDTO paymentDTO = new PaymentDTO();

        // Giả lập mạng bị đứt đột ngột ném ra lỗi hệ thống
        when(mockConnection.sendRequest(any(Request.class))).thenThrow(new RuntimeException("Socket connection lost"));

        Response response = auctionService.createPayment(paymentDTO);

        // Đảm bảo khối catch của AuctionService hoạt động ổn định và bọc lỗi lại thành chuỗi thông báo
        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Connection error during payment"));
    }

    // ── TEST: approveAuction() ───────────────────────────────────────────────
    @Test
    void testApproveAuction_Success() throws Exception {
        Response mockResponse = Response.ok(null);
        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        boolean isApproved = auctionService.approveAuction(55L);

        assertTrue(isApproved);
    }

    @Test
    void testApproveAuction_Fail() throws Exception {
        Response mockResponse = Response.error("Unauthorized action");
        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        boolean isApproved = auctionService.approveAuction(55L);

        assertFalse(isApproved);
    }
}