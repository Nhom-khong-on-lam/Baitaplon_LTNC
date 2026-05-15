package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.client.service.AuthService;
import com.auction.common.enums.AccountStatus;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminUsersController — Quản lý toàn bộ tài khoản người dùng.
 * Chức năng: xem, tìm kiếm, filter, ban/unban, delete.
 */
public class AdminUsersController {

    @FXML private Button    tabAll, tabActive, tabBanned, tabAdmin;
    @FXML private Label     totalLabel;
    @FXML private TextField searchField;

    @FXML private TableView<User>              usersTable;
    @FXML private TableColumn<User, String>    colId, colAvatar, colUsername,
            colEmail, colRole, colStatus,
            colBids, colActions;

    private final AuthService authService    = new AuthService();
    private final AuctionService auctionService = new AuctionService();

    private User       currentAdmin;
    private List<User> allUsers;

    @FXML
    public void initialize() {
        setupColumns();
    }

    public void initData(User admin) {
        this.currentAdmin = admin;
        totalLabel.setText("Loading users...");

        // Tách việc gọi mạng ra luồng phụ
        new Thread(() -> {
            List<User> fetchedUsers = authService.getAllUsers();

            // Tải xong đưa về luồng UI để vẽ
            javafx.application.Platform.runLater(() -> {
                this.allUsers = fetchedUsers != null ? fetchedUsers : new java.util.ArrayList<>();
                totalLabel.setText(this.allUsers.size() + " users total");
                setActiveTab(tabAll);
                loadTable(this.allUsers);
            });
        }).start();
    }

    /** Gọi từ AdminMainController khi search từ topbar */
    public void applySearch(String keyword) {
        searchField.setText(keyword);
        handleSearch();
    }

    // ── Tab filters ───────────────────────────────────────────
    @FXML public void showAll() {
        setActiveTab(tabAll);
        loadTable(allUsers);
    }

    @FXML public void showActive() {
        setActiveTab(tabActive);
        loadTable(allUsers.stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                .collect(Collectors.toList()));
    }

    @FXML public void showBanned() {
        setActiveTab(tabBanned);
        loadTable(allUsers.stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.BANNED)
                .collect(Collectors.toList()));
    }

    @FXML public void showAdmins() {
        setActiveTab(tabAdmin);
        loadTable(allUsers.stream()
                .filter(u -> u.getSystemRole() == SystemRole.ADMIN)
                .collect(Collectors.toList()));
    }

    @FXML public void handleSearch() {
        String kw = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        if (kw.isEmpty()) {
            loadTable(allUsers);
            return;
        }
        loadTable(allUsers.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(kw)
                        || u.getEmail().toLowerCase().contains(kw))
                .collect(Collectors.toList()));
    }

    // ── Table setup ───────────────────────────────────────────
    private void setupColumns() {
        usersTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        colId.setMinWidth(70);
        colUsername.setMinWidth(150);
        colEmail.setMinWidth(120);
        colActions.setMinWidth(150);
        colId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        // Avatar initials
        colAvatar.setCellValueFactory(c -> {
            String n = c.getValue().getUsername();
            return new SimpleStringProperty(
                    n.length() >= 2 ? n.substring(0, 2).toUpperCase() : n.toUpperCase());
        });
        colAvatar.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String init, boolean empty) {
                super.updateItem(init, empty);
                if (empty || init == null) { setGraphic(null); return; }
                Label av = new Label(init);
                av.setStyle("-fx-background-color:#eff6ff; -fx-text-fill:#2563eb;" +
                        "-fx-font-weight:bold; -fx-font-size:11px; -fx-alignment:CENTER;" +
                        "-fx-background-radius:50; -fx-min-width:32px; -fx-max-width:32px;" +
                        "-fx-min-height:32px; -fx-max-height:32px;");
                setGraphic(av); setAlignment(Pos.CENTER);
            }
        });

        colUsername.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsername()));
        colUsername.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty) { setText(null); return; }
                setText(name);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#1a202c;");
            }
        });

        colEmail.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEmail()));

        // Role badge
        colRole.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getSystemRole() == SystemRole.ADMIN ? "Admin" : "User"));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); return; }
                Label badge = new Label(role);
                badge.getStyleClass().addAll("badge",
                        "Admin".equals(role) ? "badge-amber" : "badge-blue");
                setGraphic(badge); setAlignment(Pos.CENTER);
            }
        });

        // Status pill
        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getAccountStatus().name()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label pill = new Label(status);
                pill.getStyleClass().add(
                        "ACTIVE".equals(status) ? "pill-running" : "pill-ending");
                setGraphic(pill); setAlignment(Pos.CENTER);
            }
        });

        // Bids count
        colBids.setCellValueFactory(c -> {
            int count = c.getValue().getBidCount();
            return new SimpleStringProperty(String.valueOf(count));
        });

        // Actions: Ban/Unban + Delete
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button banBtn = new Button("Ban");
            private final Button delBtn = new Button("Delete");
            private final HBox   box    = new HBox(8, banBtn, delBtn);

            {
                banBtn.setStyle("-fx-padding:4 12; -fx-font-size:11px;");
                delBtn.setStyle("-fx-padding:4 12; -fx-font-size:11px;");
                delBtn.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER);

                banBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    handleToggleBan(u);
                });
                delBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    handleDelete(u);
                });
            }

            @Override protected void updateItem(String o, boolean empty) {
                super.updateItem(o, empty);
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                // Sửa dòng 199 thành:
                boolean isSelf = java.util.Objects.equals(u.getId(), currentAdmin.getId());
                boolean banned = u.getAccountStatus() == AccountStatus.BANNED;

                // Điều chỉnh label và style nút Ban/Unban
                banBtn.setText(banned ? "Unban" : "Ban");
                banBtn.getStyleClass().setAll(banned ? "btn-secondary" : "btn-primary");
                banBtn.setDisable(isSelf);
                delBtn.setDisable(isSelf);

                setGraphic(box);
            }
        });
    }

    private void loadTable(List<User> list) {
        usersTable.setItems(FXCollections.observableArrayList(list));
        AnimationUtil.fadeIn(usersTable, 250);
        totalLabel.setText(list.size() + " users");
    }

    private void setActiveTab(Button tab) {
        List.of(tabAll, tabActive, tabBanned, tabAdmin)
                .forEach(b -> b.getStyleClass().setAll("btn-secondary"));
        tab.getStyleClass().setAll("btn-primary");
    }

    // ── Actions ───────────────────────────────────────────────
    private void handleToggleBan(User user) {
        boolean isBanned = user.getAccountStatus() == AccountStatus.BANNED;
        String action    = isBanned ? "Unban" : "Ban";
        String msg       = isBanned
                ? "Restore access for " + user.getUsername() + "?"
                : "Suspend " + user.getUsername() + "'s account?";

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle(action + " User");
        confirm.setHeaderText(action + " — " + user.getUsername());
        confirm.setContentText(msg);
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                AccountStatus newStatus = isBanned ? AccountStatus.ACTIVE : AccountStatus.BANNED;

                new Thread(() -> {
                    authService.updateUserStatus(user.getId(), newStatus);

                    javafx.application.Platform.runLater(() -> {
                        user.setAccountStatus(newStatus);
                        usersTable.refresh();
                        showInfo(user.getUsername() + " has been " + action.toLowerCase() + "ned.");
                    });
                }).start();
            }
        });
    }

    private void handleDelete(User user) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete " + user.getUsername() + "?");
        confirm.setContentText("⚠️ This action cannot be undone. All data will be removed.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                new Thread(() -> {
                    authService.deleteUser(user.getId());

                    javafx.application.Platform.runLater(() -> {
                        allUsers.removeIf(u -> u.getId().equals(user.getId()));
                        loadTable(allUsers);
                        showInfo("User \"" + user.getUsername() + "\" deleted.");
                    });
                }).start();
            }
        });
    }

    private void showInfo(String msg) {
        Alert info = new Alert(AlertType.INFORMATION);
        info.setTitle("Admin Action");
        info.setHeaderText(null);
        info.setContentText(msg);
        info.show();
    }
}
