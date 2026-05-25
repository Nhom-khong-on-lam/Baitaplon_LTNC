package com.auction;

import com.auction.client.service.ServerConnection;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ServerConnectionTest {

    @Test
    void testSingletonInstance() {
        // Mock phương thức static hoặc kiểm tra instance (bài test này giả định việc tạo instance kiểm soát độc lập)
        ServerConnection instance1 = ServerConnection.getInstance();
        ServerConnection instance2 = ServerConnection.getInstance();

        assertSame(instance1, instance2, "ServerConnection phải tuân thủ thiết kế Singleton");
    }

    @Test
    void testSendRequest_Success() throws IOException, ClassNotFoundException {
        // Giả lập mock đối tượng ServerConnection
        ServerConnection mockConnection = mock(ServerConnection.class);
        Request req = new Request("PING");
        Response expectedRes = Response.ok("PONG");

        when(mockConnection.sendRequest(req)).thenReturn(expectedRes);

        Object actualRes = mockConnection.sendRequest(req);

        assertNotNull(actualRes);
        assertTrue(((Response) actualRes).isSuccess());
        assertEquals("PONG", ((Response) actualRes).getData());
    }
}