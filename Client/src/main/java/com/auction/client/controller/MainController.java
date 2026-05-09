package com.auction.client.controller;

import com.auction.client.model.User;
import com.auction.client.controller.AnimationUtil;
import com.auction.client.controller.SceneManager;
import com.auction.client.controller.SessionManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.IOException;

/**
 * MainController — Shell controller.
 * Quản lý sidebar navigation và load content pane động.
 */
public class MainController {

    // ── Sidebar ───────────────────────────────────────────
    @FXML private VBox      sidebar;
    @FXML private Button    navDashboard, navAuctions, navMyBids,
            navMyProducts, navCreate, navAdmin, navProfile;
    @FXML private Label     adminSection;
    @FXML private Label     sidebarAvatar, sidebarUserName, sidebarUserRole;

    // ── Topbar ────────────────────────────────────────────
    @FXML private Label     topbarTitle, topbarBread, topbarAvatar;
    @FXML private TextField topbarSearch;

    // ── Content ───────────────────────────────────────────
    @FXML private StackPane contentPane;

    // ── Internal state ────────────────────────────────────
    private Button activeNavBtn;
    private User   currentUser;

    // ── FXML paths for content panels ─────────────────────
    private static final String BASE = "/com/auction/client/fxml/";

    // ── Init ──────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Sidebar slide-in animation
        AnimationUtil.slideInLeft(sidebar, 30, 450);
    }

    /** Gọi sau khi FXML load xong (từ LoginController) */
    public void initForUser(User user) {
        this.currentUser = user;

        // Thiết lập user info trên sidebar
        String name = user.getUsername();
        String initials = name.length() >= 2
                ? name.substring(0, 2).toUpperCase()
                : name.toUpperCase();
        sidebarAvatar.setText(initials);
        sidebarUserName.setText(name);
        sidebarUserRole.setText(user.isAdmin() ? "Administrator" : "Member");
        topbarAvatar.setText(initials);

        // Hiện admin menu nếu là admin
        if (user.isAdmin()) {
            adminSection.setVisible(true); adminSection.setManaged(true);
            navAdmin.setVisible(true);     navAdmin.setManaged(true);
        }

        // Load dashboard mặc định
        setActive(navDashboard);
        loadContent(BASE + "dashboard.fxml",
                (DashboardController ctrl) -> ctrl.initData(user));
    }

    // ── Navigation handlers ───────────────────────────────

    @FXML public void navDashboard() {
        setActive(navDashboard);
        setTopbar("Dashboard", "Home / Dashboard");
        loadContent(BASE + "dashboard.fxml",
                (DashboardController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML public void navAuctions() {
        setActive(navAuctions);
        setTopbar("Live Auctions", "Home / Auctions");
        loadContent(BASE + "auctions.fxml",
                (AuctionsController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML public void navMyBids() {
        setActive(navMyBids);
        setTopbar("My Bids", "Home / My Bids");
        loadContent(BASE + "my_bids.fxml",
                (MyBidsController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML public void navMyProducts() {
        setActive(navMyProducts);
        setTopbar("My Products", "Home / My Products");
        loadContent(BASE + "my_products.fxml",
                (MyProductsController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML public void navCreate() {
        setActive(navCreate);
        setTopbar("Create Auction", "Home / Create Auction");
        loadContent(BASE + "create_auction.fxml",
                (CreateAuctionController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML public void navProfile() {
        setActive(navProfile);
        setTopbar("Profile", "Home / Profile");
        loadContent(BASE + "profile.fxml",
                (ProfileController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML public void navAdmin() {
        setActive(navAdmin);
        setTopbar("Admin Panel", "Home / Admin");
        loadContent(BASE + "admin.fxml",
                (AdminController ctrl) -> ctrl.initData(currentUser));
    }

    @FXML public void handleLogout() {
        SessionManager.get().logout();
        SceneManager.get().navigate(SceneManager.Screen.LOGIN);
    }

    @FXML public void handleSearch() {
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

    @FXML public void handleNotifications() {
        // TODO: show notification panel
    }

    @FXML public void handleAvatarClick(MouseEvent e) {
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
        menu.show(topbarAvatar,
                topbarAvatar.localToScreen(0, topbarAvatar.getHeight() + 6).getX(),
                topbarAvatar.localToScreen(0, topbarAvatar.getHeight() + 6).getY());
    }

    // ── Core: load content với animation ──────────────────
    public <T> void loadContent(String fxmlPath, java.util.function.Consumer<T> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent panel = loader.load();

            if (setup != null) setup.accept(loader.getController());

            // Fade out cũ → fade in mới
            if (!contentPane.getChildren().isEmpty()) {
                Node old = contentPane.getChildren().get(0);
                FadeTransition out = new FadeTransition(Duration.millis(120), old);
                out.setToValue(0);
                out.setOnFinished(e -> swapContent(panel));
                out.play();
            } else {
                swapContent(panel);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            // Hiện error placeholder
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

    // ── Sidebar active state ───────────────────────────────
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
}