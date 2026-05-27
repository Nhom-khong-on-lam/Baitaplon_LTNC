package com.auction;

import com.auction.common.network.Request;
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
        when(mockSocket.getOutputStream()).thenReturn(bufferOut);
    }

    /**
     * Helper: Serialize một hoặc nhiều Object vào luồng RAM có Header hợp lệ của Java Serialization.
     */
    private InputStream createMockInputStream(Object... objects) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            for (Object obj : objects) {
                oos.writeObject(obj);
            }
            oos.flush();
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }

    // =========================================================================
    // TEST 1: LOGIN với payload mảng rỗng (String[]{}) — phải trả Response lỗi, KHÔNG crash
    // =========================================================================
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testRun_LoginWithInvalidPayload() throws IOException {
        // String[]{} — mảng 0 phần tử, truy cập creds[0] sẽ ném ArrayIndexOutOfBoundsException
        // nếu không có guard. ClientHandler phải xử lý an toàn và KHÔNG ném ra ngoài.
        Request malformedReq = new Request(Request.LOGIN, new String[]{});

        when(mockSocket.getInputStream()).thenReturn(createMockInputStream(malformedReq));

        ClientHandler handler = new ClientHandler(mockSocket);
        assertDoesNotThrow(() -> handler.run());
    }

    // =========================================================================
    // TEST 2: LOGIN với payload null — kiểm tra thêm nhánh null trong guard
    // =========================================================================
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testRun_LoginWithNullPayload() throws IOException {
        Request nullPayloadReq = new Request(Request.LOGIN, null);

        when(mockSocket.getInputStream()).thenReturn(createMockInputStream(nullPayloadReq));

        ClientHandler handler = new ClientHandler(mockSocket);
        assertDoesNotThrow(() -> handler.run());
    }

    // =========================================================================
    // TEST 3: Client ngắt kết nối ngay lập tức (chỉ gửi Stream Header, không có Object)
    // ClientHandler phải bắt EOFException âm thầm và đóng socket sạch sẽ.
    // =========================================================================
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testRun_ClientDisconnectsImmediately() throws IOException {
        // Tạo luồng chỉ có Header bắt tay của Java Serialization, không có Object nào.
        // readObject() sẽ ném EOFException — đây là hành vi bình thường khi client ngắt kết nối.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.flush();
        }

        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(baos.toByteArray()));

        ClientHandler handler = new ClientHandler(mockSocket);
        assertDoesNotThrow(() -> handler.run());

        // Xác nhận ClientHandler tự đóng socket sau khi xử lý xong
        verify(mockSocket, atLeastOnce()).close();
    }

    // =========================================================================
    // TEST 4: Gửi một Request với action không tồn tại — server phải không crash
    // =========================================================================
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testRun_UnknownAction_ShouldNotThrow() throws IOException {
        Request unknownReq = new Request("UNKNOWN_ACTION_XYZ", "some_data");

        when(mockSocket.getInputStream()).thenReturn(createMockInputStream(unknownReq));

        ClientHandler handler = new ClientHandler(mockSocket);
        assertDoesNotThrow(() -> handler.run());
    }
}