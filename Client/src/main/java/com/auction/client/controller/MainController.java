package com.auction.client.controller;

import com.auction.common.model.User;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * MainController — Shell controller dành cho USER.
 * Admin có shell riêng: AdminMainController.
 *
 * Quản lý sidebar navigation và load content pane động.
 * Giao diện mới (main.fxml v2) — logic giữ nguyên hoàn toàn.
 */
public class MainController {

    // ── Sidebar ───────────────────────────────────────────────
    @FXML private VBox   sidebar;
    @FXML private Button navDashboard, navAuctions, navMyBids,
            navMyProducts, navCreate, navProfile;
    @FXML private Label  sidebarAvatar, sidebarUserName, sidebarUserRole;

    // ── Topbar ────────────────────────────────────────────────
    @FXML private Label     topbarTitle, topbarBread, topbarAvatar;
    @FXML private TextField topbarSearch;
    @FXML private Button    btnNotifications;
    private javafx.animation.Timeline notificationPollingTimeline;

    // ── Content ───────────────────────────────────────────────
    @FXML private StackPane contentPane;

    // ── State ─────────────────────────────────────────────────
    private Button activeNavBtn;
    private User currentUser;

    private static final String BASE = "/com/auction/client/";

    // ── Init ──────────────────────────────────────────────────

    @FXML
    public void initialize() {
        AnimationUtil.slideInLeft(sidebar, 30, 450);
    }

    /**
     * Gọi từ LoginController sau khi xác nhận role = USER.
     * Thiết lập thông tin user lên sidebar / topbar,
     * sau đó load dashboard mặc định.
     */
    public void initForUser(User user) {
        this.currentUser = user;

        // Gán userData để các child controller tìm được shell này
        sidebar.getScene().getRoot().setUserData(this);

        // Tính initials cho avatar (tối đa 2 ký tự đầu)
        String name     = user.getUsername();
        String initials = name.length() >= 2
                ? name.substring(0, 2).toUpperCase()
                : name.toUpperCase();

        sidebarAvatar.setText(initials);
        sidebarUserName.setText(name);
        sidebarUserRole.setText("Member");
        topbarAvatar.setText(initials);

        // Load dashboard mặc định
        setActive(navDashboard);
        loadContent(BASE + "dashboard.fxml",
                (DashboardController ctrl) -> ctrl.initData(user));

        // Bắt đầu luồng chạy ngầm hỏi thăm (Polling) số lượng thông báo chưa đọc mỗi 3 giây
        startNotificationPolling();
    }

    private void startNotificationPolling() {
        if (notificationPollingTimeline != null) {
            notificationPollingTimeline.stop();
        }
        notificationPollingTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.seconds(3), ev -> {
                try {
                    com.auction.client.service.ServerConnection conn = com.auction.client.service.ServerConnection.getInstance();
                    com.auction.common.network.Request req = new com.auction.common.network.Request("COUNT_UNREAD_NOTIFICATIONS", currentUser.getId());
                    com.auction.common.network.Response resp = (com.auction.common.network.Response) conn.sendRequest(req);
                    if (resp.isSuccess() && resp.getData() instanceof Integer count) {
                        if (count > 0) {
                            btnNotifications.setText("🔔 (" + count + ")");
                            btnNotifications.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold;"); // Bật màu đỏ nhắc nhở
                        } else {
                            btnNotifications.setText("🔔");
                            btnNotifications.setStyle(""); // Về lại màu bình thường
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Polling notifications failed: " + e.getMessage());
                }
            })
        );
        notificationPollingTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        notificationPollingTimeline.play();
    }

    // ── Navigation handlers ───────────────────────────────────

    @FXML
    public void navDashboard() {
        setActive(navDashboard);
        setTopbar("Dashboard", "Home / Dashboard");
        loadContent(BASE + "dashboard.fxml",
                (DashboardController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML
    public void navAuctions() {
        setActive(navAuctions);
        setTopbar("Live Auctions", "Home / Auctions");
        loadContent(BASE + "auctions.fxml",
                (AuctionsController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML
    public void navMyBids() {
        setActive(navMyBids);
        setTopbar("My Bids", "Home / My Bids");
        loadContent(BASE + "my_bids.fxml",
                (MyBidsController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML
    public void navMyProducts() {
        setActive(navMyProducts);
        setTopbar("My Products", "Home / My Products");
        loadContent(BASE + "my_products.fxml",
                (MyProductsController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML
    public void navCreate() {
        setActive(navCreate);
        setTopbar("Create Auction", "Home / Create Auction");
        loadContent(BASE + "create_auction.fxml",
                (CreateAuctionController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML
    public void navProfile() {
        setActive(navProfile);
        setTopbar("Profile", "Home / Profile");
        loadContent(BASE + "profile.fxml",
                (ProfileController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML
    public void handleLogout() {
        if (notificationPollingTimeline != null) {
            notificationPollingTimeline.stop();
        }
        SessionManager.get().logout();
        SceneManager.get().navigate(SceneManager.Screen.LOGIN);
    }

    @FXML
    public void handleSearch() {
        String kw = topbarSearch.getText().trim();
        if (!kw.isEmpty()) {
            setActive(navAuctions);
            setTopbar("Search: " + kw, "Home / Search");
            loadContent(BASE + "auctions.fxml",
                    (AuctionsController ctrl) -> {
                        ctrl.initData(currentUser);
                        ctrl.applySearch(kw);
                    });
        }
    }

    @FXML
    public void handleNotifications() {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("context-menu");

        // 1. Ép cố định chiều rộng của ContextMenu bên ngoài là 280px (Không bị phình to bề ngang)
        menu.setStyle("-fx-max-width: 280px; -fx-pref-width: 280px;");

        com.auction.client.service.AuctionService auctionService = new com.auction.client.service.AuctionService();
        java.util.List<com.auction.common.dto.NotificationDTO> notifs = auctionService.getNotifications(SessionManager.get().getUser().getId());
        if (notifs != null && notifs.size() > 15) {
            notifs = notifs.subList(0, 15); // Chỉ lấy 15 thông báo gần nhất
        }

        // Sau khi hiển thị, gọi lệnh đánh dấu đã đọc tất cả
        try {
            com.auction.client.service.ServerConnection.getInstance().sendRequest(
                new com.auction.common.network.Request("MARK_ALL_NOTIF_READ", SessionManager.get().getUser().getId())
            );
        } catch (Exception ex) {
            System.err.println("Failed to mark notifications as read: " + ex.getMessage());
        }

        if (notifs == null || notifs.isEmpty()) {
            MenuItem emptyItem = new MenuItem("You have no notifications.");
            emptyItem.setDisable(true);
            menu.getItems().add(emptyItem);
        } else {
            // Tiêu đề nhỏ gọn đặt ở trên cùng bảng thông báo
            MenuItem titleItem = new MenuItem("Recent Notifications");
            titleItem.setStyle("-fx-font-weight: bold; -fx-text-fill: #2b6cb0; -fx-font-size: 11px;");
            titleItem.setDisable(true);
            menu.getItems().add(titleItem);
            menu.getItems().add(new SeparatorMenuItem());

            // 2. TẠO KHUNG CHỨA THÔNG BÁO GỌN GÀNG (Sử dụng VBox)
            javafx.scene.layout.VBox listContainer = new javafx.scene.layout.VBox(4);
            // Thiết lập padding và màu nền trắng cho danh sách
            listContainer.setStyle("-fx-padding: 4; -fx-background-color: white;");

            for (com.auction.common.dto.NotificationDTO notif : notifs) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM");
                String timeStr = notif.getCreatedAt() != null ? notif.getCreatedAt().format(formatter) : "";

                // Nhãn hiển thị thời gian bằng chữ nhỏ xám
                javafx.scene.control.Label timeLabel = new javafx.scene.control.Label(timeStr);
                timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #a0aec0;");

                // Nhãn hiển thị nội dung tin nhắn, ép chữ thu nhỏ (11px) và tự động ngắt dòng
                String prefix = !notif.isRead() ? "🔴 " : "";
                javafx.scene.control.Label msgLabel = new javafx.scene.control.Label(prefix + notif.getMessage());
                msgLabel.setWrapText(true); // Tự động ngắt dòng đi xuống khi chạm biên vùng chữ
                msgLabel.setPrefWidth(235); // Giới hạn chiều rộng tối đa của cột chữ
                msgLabel.setMaxWidth(235);

                if (!notif.isRead()) {
                    msgLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1a202c;");
                } else {
                    msgLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4a5568;");
                }

                // Gom thời gian và nội dung xếp dọc vào 1 hàng dòng thông báo
                javafx.scene.layout.VBox row = new javafx.scene.layout.VBox(1, timeLabel, msgLabel);
                // Khoảng cách đệm khít lại và tạo đường gạch mờ mỏng phân cách giữa các thông báo
                row.setStyle("-fx-padding: 4 2 5 2; -fx-border-color: #f7fafc; -fx-border-width: 0 0 1 0;");
                listContainer.getChildren().add(row);
            }

            // 3. TẠO THANH CUỘN (ScrollPane): Bọc listContainer vào trong thanh cuộn
            javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(listContainer);
            scrollPane.setFitToWidth(true);

            // Ép chiều cao bảng thông báo luôn cố định ở mức 240px (Nhiều thông báo tự bật thanh cuộn lên để lăn chuột)
            scrollPane.setPrefHeight(240);
            scrollPane.setMaxHeight(240);
            scrollPane.setPrefWidth(265);
            scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

            // Nhúng nguyên khối ScrollPane này vào một CustomMenuItem duy nhất để đưa vào ContextMenu
            javafx.scene.control.CustomMenuItem scrollableLayout = new javafx.scene.control.CustomMenuItem(scrollPane);
            scrollableLayout.setHideOnClick(false); // Khi người dùng kéo thanh cuộn hoặc lăn chuột, bảng không bị ẩn đột ngột

            menu.getItems().add(scrollableLayout);
        }

        if (topbarAvatar.getScene() == null) return;
        double x = topbarAvatar.localToScreen(0, topbarAvatar.getHeight() + 6).getX() - 200;
        double y = topbarAvatar.localToScreen(0, topbarAvatar.getHeight() + 6).getY();
        menu.show(topbarAvatar, x, y);
    }

    @FXML
    public void handleAvatarClick(MouseEvent e) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("context-menu");

        MenuItem profile  = new MenuItem("👤  My Profile");
        MenuItem settings = new MenuItem("⚙  Settings");
        MenuItem logout   = new MenuItem("🚪  Sign Out");

        profile.setOnAction(ev  -> navProfile());
        settings.setOnAction(ev -> navProfile());
        logout.setOnAction(ev   -> handleLogout());

        menu.getItems().addAll(profile, settings,
                new SeparatorMenuItem(), logout);

        double x = topbarAvatar.localToScreen(0, topbarAvatar.getHeight() + 6).getX();
        double y = topbarAvatar.localToScreen(0, topbarAvatar.getHeight() + 6).getY();
        menu.show(topbarAvatar, x, y);
    }

    // ── Core: load content với fade + slide animation ─────────

    /**
     * Load một FXML vào contentPane với animation.
     * Nếu pane đang có nội dung, fade out trước rồi swap.
     *
     * @param fxmlPath đường dẫn resource tới file FXML
     * @param setup    Consumer nhận controller của panel vừa load (có thể null)
     * @param <T>      kiểu controller
     */
    public <T> void loadContent(String fxmlPath, Consumer<T> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent panel = loader.load();

            if (setup != null) setup.accept(loader.getController());

            if (!contentPane.getChildren().isEmpty()) {
                Node old = contentPane.getChildren().get(0);
                FadeTransition out = new FadeTransition(Duration.millis(120), old);
                out.setToValue(0);
                out.setOnFinished(ev -> swapContent(panel));
                out.play();
            } else {
                swapContent(panel);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            Label err = new Label("Failed to load panel: " + fxmlPath);
            err.setStyle("-fx-text-fill:#dc2626; -fx-font-size:13px;");
            contentPane.getChildren().setAll(err);
        }
    }

    private void swapContent(Parent panel) {
        panel.setOpacity(0);
        contentPane.getChildren().setAll(panel);
        AnimationUtil.slideUp(panel, 16, 280);
    }

    // ── Sidebar active state ──────────────────────────────────

    private void setActive(Button btn) {
        if (activeNavBtn != null) {
            activeNavBtn.getStyleClass().remove("nav-btn-active");
            if (!activeNavBtn.getStyleClass().contains("nav-btn"))
                activeNavBtn.getStyleClass().add("nav-btn");
        }
        btn.getStyleClass().remove("nav-btn");
        if (!btn.getStyleClass().contains("nav-btn-active"))
            btn.getStyleClass().add("nav-btn-active");
        activeNavBtn = btn;
    }

    private void setTopbar(String title, String breadcrumb) {
        topbarTitle.setText(title);
        topbarBread.setText(breadcrumb);
    }

    // ── Getter ────────────────────────────────────────────────

    public User getCurrentUser() { return currentUser; }
}