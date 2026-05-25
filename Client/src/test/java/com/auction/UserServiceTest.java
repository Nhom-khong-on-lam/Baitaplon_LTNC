package com.auction;

import com.auction.client.service.ServerConnection;
import com.auction.client.service.UserService;
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

public class UserServiceTest {

    private UserService userService;
    private ServerConnection mockConnection;
    private MockedStatic<ServerConnection> staticConnection;

    @BeforeEach
    void setUp() {
        mockConnection = mock(ServerConnection.class);
        staticConnection = Mockito.mockStatic(ServerConnection.class);
        staticConnection.when(ServerConnection::getInstance).thenReturn(mockConnection);

        userService = new UserService();
    }

    @AfterEach
    void tearDown() {
        staticConnection.close();
    }

    @Test
    void testGetProfile_Success() throws IOException, ClassNotFoundException {
        Response mockResponse = Response.ok("Thông tin Profile");
        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        Response res = userService.getProfile();

        assertTrue(res.isSuccess());
        assertEquals("Thông tin Profile", res.getData());
    }

    @Test
    void testAdminGetAllUsers_Success() throws IOException, ClassNotFoundException {
        Response mockResponse = Response.ok("Danh sách toàn bộ User");
        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        Response res = userService.adminGetAllUsers();

        assertTrue(res.isSuccess());
        assertEquals("Danh sách toàn bộ User", res.getData());
    }
}