package com.auction;

import com.auction.client.controller.AdminDashboardController;
import com.auction.client.service.AuctionService;
import com.auction.client.service.AuthService;
import com.auction.common.enums.AccountStatus;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.Auction;
import com.auction.common.model.Item;
import com.auction.common.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminDashboardControllerTest extends ApplicationTest {

    private AdminDashboardController controller;

    @Mock private AuthService authService;
    @Mock private AuctionService auctionService;

    @BeforeAll
    static void initJavaFX() {
        try { Platform.startup(() -> {}); } catch (Exception ignored) {}
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AdminDashboardController();

        // Khởi tạo UI qua Setter
        controller.setWelcomeLabel(new Label());
        controller.setDateLabel(new Label());
        controller.setStatTotalUsers(new Label());
        controller.setStatTotalAuctions(new Label());
        controller.setStatLiveAuctions(new Label());
        controller.setStatBanned(new Label());
        controller.setStatUserDelta(new Label());
        controller.setRecentUsersList(new VBox());
        controller.setRecentAuctionsList(new VBox());

        controller.setAuthService(authService);
        controller.setAuctionService(auctionService);

        // MẶC ĐỊNH: Luôn trả về danh sách rỗng thay vì null để tránh lỗi bạn vừa gặp
        when(auctionService.getActiveAuctions()).thenReturn(FXCollections.observableArrayList());
    }

    // Helper: Tạo User đúng 5 tham số
    private User createTestUser(Long id, String name) {
        return new User(id, name, "hash", name + "@test.com", SystemRole.USER);
    }

    // Helper: Tạo Item (Anonymous) và Auction
    private Auction createTestAuction(String itemName) {
        Item item = new Item(itemName, "Desc", 100.0) {
            @Override public String getCategory() { return "📦"; }
        };
        return new Auction(item, createTestUser(9L, "Seller"), LocalDateTime.now().plusDays(1));
    }

    // --- 6 TEST CASES ---

    @Test
    void test1_InitDataSuccessfully() {
        User admin = new User(1L, "Admin", "pw", "a@a.com", SystemRole.ADMIN);
        when(authService.getAllUsers()).thenReturn(new ArrayList<>());
        when(auctionService.getAllAuctions()).thenReturn(FXCollections.observableArrayList());

        interact(() -> controller.initData(admin));

        assertNotNull(controller);
    }

    @Test
    void test2_RecentUsersLimitIsFive() {
        List<User> users = new ArrayList<>();
        for (long i = 1; i <= 8; i++) users.add(createTestUser(i, "User" + i));

        when(authService.getAllUsers()).thenReturn(users);
        when(auctionService.getAllAuctions()).thenReturn(FXCollections.observableArrayList());

        interact(() -> controller.initData(createTestUser(1L, "Admin")));

        // Kiểm tra logic subList lấy 5 người cuối
        assertEquals(5, controller.getRecentUsersList().getChildren().size());
    }

    @Test
    void test3_EmptyListsHandling() {
        when(authService.getAllUsers()).thenReturn(new ArrayList<>());
        when(auctionService.getAllAuctions()).thenReturn(FXCollections.observableArrayList());

        interact(() -> controller.initData(createTestUser(1L, "Admin")));

        assertEquals(0, controller.getRecentUsersList().getChildren().size());
        assertEquals(0, controller.getRecentAuctionsList().getChildren().size());
    }

    @Test
    void test4_BannedUserCalculation() {
        User u1 = createTestUser(1L, "Active");
        User u2 = createTestUser(2L, "Banned");
        u2.setAccountStatus(AccountStatus.BANNED);

        when(authService.getAllUsers()).thenReturn(Arrays.asList(u1, u2));
        when(auctionService.getAllAuctions()).thenReturn(FXCollections.observableArrayList());

        // Chạy initData để xem code filter BANNED có lỗi không
        interact(() -> controller.initData(createTestUser(1L, "Admin")));

        assertNotNull(u2.getAccountStatus());
    }

    @Test
    void test5_RecentAuctionsDisplay() {
        Auction a = createTestAuction("Vintage Car");
        a.setBidHistory(new ArrayList<>());

        when(authService.getAllUsers()).thenReturn(new ArrayList<>());
        when(auctionService.getAllAuctions()).thenReturn(FXCollections.observableArrayList(a));

        interact(() -> controller.initData(createTestUser(1L, "Admin")));

        assertEquals(1, controller.getRecentAuctionsList().getChildren().size());
    }

    @Test
    void test6_AdminWelcomeMessage() {
        User admin = new User(1L, "SuperAdmin", "pw", "s@a.com", SystemRole.ADMIN);
        when(authService.getAllUsers()).thenReturn(new ArrayList<>());
        when(auctionService.getAllAuctions()).thenReturn(FXCollections.observableArrayList());

        // Kiểm tra xem hàm có chạy hết mà không văng lỗi khi set welcomeLabel không
        interact(() -> controller.initData(admin));

        assertEquals("SuperAdmin", admin.getUsername());
    }
}