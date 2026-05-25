package com.auction;

import com.auction.client.service.ItemService;
import com.auction.client.service.ServerConnection;
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

public class ItemServiceTest {

    private ItemService itemService;
    private ServerConnection mockConnection;
    private MockedStatic<ServerConnection> staticConnection;

    @BeforeEach
    void setUp() {
        mockConnection = mock(ServerConnection.class);
        staticConnection = Mockito.mockStatic(ServerConnection.class);
        staticConnection.when(ServerConnection::getInstance).thenReturn(mockConnection);

        itemService = new ItemService();
    }

    @AfterEach
    void tearDown() {
        staticConnection.close();
    }

    @Test
    void testGetMyProducts_Success() throws IOException, ClassNotFoundException {
        Response mockResponse = Response.ok("Danh sách sản phẩm của tôi");
        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        Response res = itemService.getMyProducts();

        assertTrue(res.isSuccess());
        assertEquals("Danh sách sản phẩm của tôi", res.getData());
    }

    @Test
    void testDeleteItem_Success() throws IOException, ClassNotFoundException {
        Response mockResponse = Response.ok("Đã xóa sản phẩm thành công");
        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        Response res = itemService.deleteItem(12);

        assertTrue(res.isSuccess());
        assertEquals("Đã xóa sản phẩm thành công", res.getData());
    }
}