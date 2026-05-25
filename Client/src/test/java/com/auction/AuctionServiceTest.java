package com.auction.client.service;

import com.auction.common.dto.PaymentDTO;
import com.auction.common.model.Auction;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuctionServiceTest {

    private AuctionService auctionService;
    private ServerConnection mockConnection;
    private MockedStatic<ServerConnection> staticConnection;

    @BeforeEach
    void setUp() {
        mockConnection = mock(ServerConnection.class);
        staticConnection = Mockito.mockStatic(ServerConnection.class);
        staticConnection.when(ServerConnection::getInstance).thenReturn(mockConnection);

        auctionService = new AuctionService();
    }

    @AfterEach
    void tearDown() {
        staticConnection.close();
    }

    @Test
    void testGetAllAuctions_ReturnsList() throws IOException, ClassNotFoundException {
        List<Auction> expectedList = new ArrayList<>();
        Response mockResponse = Response.ok(expectedList);

        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        List<Auction> result = auctionService.getAllAuctions();

        assertNotNull(result);
        assertEquals(expectedList, result);
    }

    @Test
    void testCreatePayment_Success() throws IOException, ClassNotFoundException {
        PaymentDTO payment = new PaymentDTO();
        Response mockResponse = Response.ok(null);

        when(mockConnection.sendRequest(any(Request.class))).thenReturn(mockResponse);

        Response res = auctionService.createPayment(payment);

        assertTrue(res.isSuccess());
        assertEquals("OK", res.getMessage());
    }
}