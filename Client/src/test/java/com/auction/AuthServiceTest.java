package com.auction;

import com.auction.client.service.AuthService;
import com.auction.client.service.ServerConnection;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.User;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private AuthService authService;
    private ServerConnection mockConnection;
    private MockedStatic<ServerConnection> staticConnection;

    @BeforeEach
    void setUp() {
        mockConnection = mock(ServerConnection.class);
        // Mock phương thức tĩnh getInstance() để trả về mockConnection
        staticConnection = Mockito.mockStatic(ServerConnection.class);
        staticConnection.when(ServerConnection::getInstance).thenReturn(mockConnection);

        authService = new AuthService();
    }

    @AfterEach
    void tearDown() {
        staticConnection.close();
    }

    @Test
    void testLogin_Success() throws IOException, ClassNotFoundException {
        User user = new User(1L, "user_test", "hash", "test@test.com", SystemRole.USER);
        Response mockResponse = new Response(true, "OK", user);
        mockResponse.setToken("MOCK_JWT_TOKEN");

        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        Response response = authService.login("user_test", "password123");

        assertTrue(response.isSuccess());
        assertEquals("MOCK_JWT_TOKEN", response.getToken());
        assertEquals(user, response.getData());
    }

    @Test
    void testSaveUser_Success() throws IOException, ClassNotFoundException {
        Response mockResponse = Response.ok(null);
        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        boolean result = authService.saveUser("new_user", "new@test.com", "raw_password");

        assertTrue(result, "Hàm saveUser phải trả về true khi Server phản hồi thành công");
    }
}