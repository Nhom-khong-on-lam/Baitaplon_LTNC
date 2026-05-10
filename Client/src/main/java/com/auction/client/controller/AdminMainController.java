package com.auction.client.controller;

import com.auction.client.Enum.SystemRole;
import com.auction.client.model.User;
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
 * AdminMainController — Shell riêng cho Admin.
 * Giống MainController nhưng chỉ có menu admin:
 *   Dashboard → AdminDashboardController
 *   Users     → AdminUsersController
 *   Auctions  → AdminAuctionsController
 */
public class AdminMainController {

    // ── Sidebar ───────────────────────────────────────────
    @FXML private VBox   sidebar;
    @FXML private Button navDashboard, navUsers, navAuctions;
    @FXML private Label  sidebarAvatar, sidebarUserName;

    // ── Topbar ────────────────────────────────────────────
    @FXML private Label     topbarTitle, topbarBread, topbarAvatar;
    @FXML private TextField topbarSearch;

    // ── Content ───────────────────────────────────────────
    @FXML private StackPane contentPane;

    // ── State ─────────────────────────────────────────────
    private Button activeBtn;
    private User   currentAdmin;

    private static final String BASE = "/com/auction/client/fxml/";

    // ── Init ──────────────────────────────────────────────
    @FXML
    public void initialize() {
        AnimationUtil.slideInLeft(sidebar, 30, 450);
    }

    /**
     * Gọi từ LoginController sau khi xác nhận role = ADMIN.
     */
    public void initForUser(User admin) {
        this.currentAdmin = admin;

        // Gán userData cho root node để các child controller tìm được shell
        sidebar.getScene().getRoot().setUserData(this);

        // Avatar initials
        String name = admin.getUsername();
        String initials = name.length() >= 2
                ? name.substring(0, 2).toUpperCase()
                : name.toUpperCase();
        sidebarAvatar.setText(initials);
        sidebarUserName.setText(name);
        topbarAvatar.setText(initials);

        // Load trang mặc định = Dashboard
        setActive(navDashboard);
        loadContent(BASE + "admin_dashboard.fxml",
                (AdminDashboardController ctrl) -> ctrl.initData(admin));
    }

    // ── Navigation handlers ───────────────────────────────
    @FXML public void navDashboard() {
        setActive(navDashboard);
        setTopbar("Admin Dashboard", "Admin / Dashboard");
        loadContent(BASE + "admin_dashboard.fxml",
                (AdminDashboardController ctrl) -> ctrl.initData(currentAdmin));
    }

    @FXML public void navUsers() {
        setActive(navUsers);
        setTopbar("Manage Users", "Admin / Users");
        loadContent(BASE + "admin_users.fxml",
                (AdminUsersController ctrl) -> ctrl.initData(currentAdmin));
    }

    @FXML public void navAuctions() {
        setActive(navAuctions);
        setTopbar("Manage Auctions", "Admin / Auctions");
        loadContent(BASE + "admin_auctions.fxml",
                (AdminAuctionsController ctrl) -> ctrl.initData(currentAdmin));
    }

    @FXML public void handleLogout() {
        SessionManager.get().logout();
        SceneManager.get().navigate(SceneManager.Screen.LOGIN);
    }

    @FXML public void handleSearch() {
        String kw = topbarSearch.getText().trim();
        if (!kw.isEmpty()) {
            // Default: search trong Users
            setActive(navUsers);
            setTopbar("Search: " + kw, "Admin / Search");
            loadContent(BASE + "admin_users.fxml",
                    (AdminUsersController ctrl) -> {
                        ctrl.initData(currentAdmin);
                        ctrl.applySearch(kw);
                    });
        }
    }

    @FXML public void handleAvatarClick(MouseEvent e) {
        ContextMenu menu = new ContextMenu();
        menu.getStyleClass().add("context-menu");

        MenuItem logout = new MenuItem("🚪  Sign Out");
        logout.setOnAction(ev -> handleLogout());

        menu.getItems().add(logout);
        menu.show(topbarAvatar,
                topbarAvatar.localToScreen(0, topbarAvatar.getHeight() + 6).getX(),
                topbarAvatar.localToScreen(0, topbarAvatar.getHeight() + 6).getY());
    }

    // ── Core: load content với animation ──────────────────
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
            Label err = new Label("Failed to load: " + fxmlPath);
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
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("nav-btn-active");
            if (!activeBtn.getStyleClass().contains("nav-btn"))
                activeBtn.getStyleClass().add("nav-btn");
        }
        btn.getStyleClass().remove("nav-btn");
        if (!btn.getStyleClass().contains("nav-btn-active"))
            btn.getStyleClass().add("nav-btn-active");
        activeBtn = btn;
    }

    private void setTopbar(String title, String breadcrumb) {
        topbarTitle.setText(title);
        topbarBread.setText(breadcrumb);
    }

    public User getCurrentAdmin() { return currentAdmin; }
}