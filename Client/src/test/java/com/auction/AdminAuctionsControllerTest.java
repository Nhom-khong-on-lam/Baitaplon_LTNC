package com.auction;


import com.auction.client.controller.AdminAuctionsController;
import com.auction.client.service.AuctionService;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.Auction;
import com.auction.common.model.Item;
import com.auction.common.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminAuctionsControllerTest extends ApplicationTest {

    private AdminAuctionsController controller;

    @Mock private AuctionService auctionService;

    private Auction aRunning, aEnding, aFinished;

    @BeforeAll
    static void initJavaFX() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AdminAuctionsController();
        controller.setAuctionsTable(new TableView<>());
        controller.setSearchField(new TextField());
        controller.setTotalLabel(new Label());
        controller.setTabAll(new Button());
        controller.setTabLive(new Button());
        controller.setTabEnding(new Button());
        controller.setTabFinished(new Button());
        controller.setAuctionService(auctionService);

        aRunning  = auction(1L, "Laptop Gaming", AuctionStatus.RUNNING,  LocalDateTime.now().plusDays(2));
        aEnding   = auction(2L, "iPhone 16",     AuctionStatus.RUNNING,  LocalDateTime.now().plusMinutes(30));
        aFinished = auction(3L, "Vintage Vase",  AuctionStatus.FINISHED, LocalDateTime.now().minusDays(1));

        when(auctionService.getAllAuctions())
                .thenReturn(FXCollections.observableArrayList(aRunning, aEnding, aFinished));

        interact(() -> controller.initData(new User(1L, "admin", "hash", "a@b.com", SystemRole.ADMIN)));
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test void testInitData_LoadsAll() {
        assertEquals(3, controller.getAuctionsTable().getItems().size());
    }

    @Test void testShowAll() {
        interact(() -> controller.showAll());
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(3, controller.getAuctionsTable().getItems().size());
    }

    @Test void testShowLive_OnlyRunning() {
        interact(() -> controller.showLive());
        WaitForAsyncUtils.waitForFxEvents();
        // aRunning + aEnding đều RUNNING và còn hạn
        assertEquals(2, controller.getAuctionsTable().getItems().size());
        controller.getAuctionsTable().getItems()
                .forEach(a -> assertEquals(AuctionStatus.RUNNING, a.getStatus()));
    }

    @Test void testShowEnding_Within1Hour() {
        interact(() -> controller.showEnding());
        WaitForAsyncUtils.waitForFxEvents();
        // Chỉ aEnding (30 phút) thoả điều kiện
        assertEquals(1, controller.getAuctionsTable().getItems().size());
        assertEquals(aEnding.getId(), controller.getAuctionsTable().getItems().get(0).getId());
    }

    @Test void testShowFinished() {
        interact(() -> controller.showFinished());
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, controller.getAuctionsTable().getItems().size());
        assertEquals(AuctionStatus.FINISHED, controller.getAuctionsTable().getItems().get(0).getStatus());
    }

    @Test void testHandleSearch_Found() {
        interact(() -> { controller.getSearchField().setText("laptop"); controller.handleSearch(); });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(1, controller.getAuctionsTable().getItems().size());
        assertTrue(controller.getAuctionsTable().getItems().get(0).getItem().getName().toLowerCase().contains("laptop"));
    }

    @Test void testHandleSearch_NotFound() {
        interact(() -> { controller.getSearchField().setText("xyz_nothing"); controller.handleSearch(); });
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(controller.getAuctionsTable().getItems().isEmpty());
    }

    @Test void testHandleSearch_EmptyRestoresAll() {
        interact(() -> { controller.getSearchField().setText(""); controller.handleSearch(); });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(3, controller.getAuctionsTable().getItems().size());
    }

    @Test void testDeleteAuction_ServiceCalled() {
        auctionService.deleteAuction(aFinished.getId());
        verify(auctionService, times(1)).deleteAuction(3L);
    }

    @Test void testExtendAuction_ServiceCalled() {
        when(auctionService.extendAuction(aRunning.getId(), 1)).thenReturn(true);
        assertTrue(auctionService.extendAuction(aRunning.getId(), 1));
        verify(auctionService).extendAuction(1L, 1);
    }

    // ── Helper ───────────────────────────────────────────────
    private Auction auction(Long id, String name, AuctionStatus status, LocalDateTime end) {
        Item item = new Item() { @Override public String getCategory() { return "Electronics"; } };
        item.setName(name);
        User seller = new User(id + 10, "seller_" + id, "pass", "s@mail.com", SystemRole.USER);
        Auction a = new Auction(item, seller, end);
        a.setId(id);
        a.setStatus(status);
        a.setBidHistory(new ArrayList<>());
        return a;
    }
}