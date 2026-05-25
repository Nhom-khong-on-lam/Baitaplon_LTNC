package com.auction;

import com.auction.common.network.Request;
import com.auction.common.network.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import server.network.ClientHandler;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    private Socket mockSocket;
    private ByteArrayOutputStream bufferOut;

    @BeforeEach
    void setUp() throws IOException {
        mockSocket = mock(Socket.class);
        bufferOut = new ByteArrayOutputStream();

        // Cấu hình Socket trả về luồng ghi thực tế trên bộ nhớ RAM để tránh lỗi ObjectOutputStream nghẽn
        when(mockSocket.getOutputStream()).thenReturn(bufferOut);
    }

    /**
     * Hàm helper: Tạo luồng đọc thực tế trên RAM chứa sẵn mã định danh Header Handshake của Java
     * và danh sách các Object Request. Đọc xong sẽ tự động kết thúc luồng (-1) an toàn.
     */
    private InputStream createMockInputStream(Object... objects) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Ghi dữ liệu Object thật kèm Header Handshake hợp lệ của Java vào RAM
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            for (Object obj : objects) {
                oos.writeObject(obj);
            }
            oos.flush();
        }

        byte[] data = baos.toByteArray();
        return new ByteArrayInputStream(data);
    }

    // =========================================================================
    // TEST CASE 1: ĐĂNG NHẬP VỚI PAYLOAD SAI ĐỊNH DẠNG DỮ LIỆU
    // =========================================================================
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testRun_LoginWithInvalidPayload() throws IOException {
        // SỬA LỖI: Thay 'new Object()' không có Serializable bằng một String[] trống (đã có Serializable)
        Request malformedReq = new Request(Request.LOGIN, new String[]{});

        // Nạp request vào luồng bộ nhớ thực tế trên RAM
        InputStream mockIn = createMockInputStream(malformedReq);
        when(mockSocket.getInputStream()).thenReturn(mockIn);

        ClientHandler clientHandler = new ClientHandler(mockSocket);

        // Chạy hàm run(). Đi qua logic kiểm tra an toàn và thoát ra mượt mà trong vài mili-giây!
        assertDoesNotThrow(() -> clientHandler.run());
    }

    // =========================================================================
    // TEST CASE 2: KHÁCH HÀNG NGẮT KẾT NỐI (Chỉ truyền Header bắt tay, không gửi dữ liệu)
    // =========================================================================
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testRun_ClientDisconnectsImmediately() throws IOException {
        // SỬA LỖI: Tạo luồng chỉ chứa duy nhất mã bắt tay (Stream Header) của Java Serialization,
        // ngay sau đó luồng sẽ kết thúc trả về -1 (Giả lập ngắt kết nối an toàn mà không ném Runtime Exception làm crash JUnit)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.flush();
        }

        InputStream immediateEofIn = new ByteArrayInputStream(baos.toByteArray());
        when(mockSocket.getInputStream()).thenReturn(immediateEofIn);

        ClientHandler clientHandler = new ClientHandler(mockSocket);

        // Khối catch(EOFException) hoặc catch(IOException) trong ClientHandler.java sẽ tự dọn dẹp âm thầm
        assertDoesNotThrow(() -> clientHandler.run());

        // Đảm bảo hệ thống tự động đóng socket giải phóng tài nguyên
        verify(mockSocket, atLeastOnce()).close();
    }
}