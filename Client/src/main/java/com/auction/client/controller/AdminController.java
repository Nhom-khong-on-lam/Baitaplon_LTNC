package com.auction.client.controller;

import com.auction.client.Enum.AccountStatus;
import com.auction.client.model.Auction;
import com.auction.client.model.User;
import com.auction.client.service.AuctionService;
import com.auction.client.service.AuthService;
import com.auction.client.controller.AnimationUtil;
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
 * AdminController — Quản lý toàn bộ users và auctions.
 * Chỉ admin mới có quyền truy cập màn hình này.
 */
public class AdminController {

    // ── Stats ─────────────────────────────────────────────────
    @FXML private Label adminStatUsers, adminStatAuctions,
            adminStatActive, adminStatRevenue;
    @FXML private Label adminBadge;
    @FXML private Label adminStatUsersDelta;

    // ── Tabs ──────────────────────────────────────────────────
    @FXML private Button tabUsers, tabAuctions;
    @FXML private TextField adminSearch;
    @FXML private VBox usersPanel, auctionsPanel;

    // ── Users table ───────────────────────────────────────────
    @FXML private TableView<User>               usersTable;
    @FXML private TableColumn<User, String>     colUserId, colUserAvatar, colUserName,
            colUserEmail, colUserRole, colUserStatus,
            colUserBids, colUserJoined, colUserAction;

    // ── Auctions table ────────────────────────────────────────
    @FXML private TableView<Auction>            auctionsTable;
    @FXML private TableColumn<Auction, String>  colAucId, colAucTitle, colAucSeller,
            colAucCat, colAucPrice, colAucBids,
            colAucStatus, colAucEnds, colAucAction;

    private final AuthService    authService    = new AuthService();
    private final AuctionService auctionService = new AuctionService();
    private User currentAdmin;
    private List<User>    allUsers;
    private List<Auction> allAuctions;

    @FXML
    public void initialize() {
        setupUserColumns();
        setupAuctionColumns();
    }

    public void initData(User admin) {
        this.currentAdmin = admin;
        this.allUsers    = authService.getAllUsers();
        this.allAuctions = auctionService.getAllAuctions();

        // Stat cards
        List<Auction> live = auctionService.getActiveAuctions();
        double revenue = allAuctions.stream()
                .filter(a -> !a.isLive())
                .mapToDouble(Auction::getCurrentPrice)
                .sum();

        AnimationUtil.countUp(adminStatUsers,    0, allUsers.size(),    800, "", "");
        AnimationUtil.countUp(adminStatAuctions, 0, allAuctions.size(), 800, "", "");
        AnimationUtil.countUp(adminStatActive,   0, live.size(),        800, "", "");
        AnimationUtil.countUp(adminStatRevenue,  0, revenue,            800, "", "");
        adminBadge.setText("⚙  " + admin.getUsername());          // hiện tên admin
        adminStatUsersDelta.setText("↑ " + allUsers.size() + " total"); // hoặc tính số mới
        // Load default tab
        setActiveTab(tabUsers);
        usersTable.setItems(FXCollections.observableArrayList(allUsers));
        AnimationUtil.fadeIn(usersTable, 350);
    }

    // ── Tab switching ─────────────────────────────────────────
    @FXML public void showUsers(javafx.event.ActionEvent event) {
        setActiveTab(tabUsers);
        usersPanel.setVisible(true);   usersPanel.setManaged(true);
        auctionsPanel.setVisible(false); auctionsPanel.setManaged(false);
        adminSearch.clear();
        usersTable.setItems(FXCollections.observableArrayList(allUsers));
        AnimationUtil.fadeIn(usersPanel, 250);
    }

    @FXML public void showAuctions(javafx.event.ActionEvent event) {
        setActiveTab(tabAuctions);
        auctionsPanel.setVisible(true);  auctionsPanel.setManaged(true);
        usersPanel.setVisible(false);    usersPanel.setManaged(false);
        adminSearch.clear();
        auctionsTable.setItems(FXCollections.observableArrayList(allAuctions));
        AnimationUtil.fadeIn(auctionsPanel, 250);
    }

    @FXML public void handleAdminSearch(javafx.event.ActionEvent event) {
        String kw = adminSearch.getText().trim().toLowerCase();
        if (usersPanel.isVisible()) {
            List<User> filtered = allUsers.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(kw)
                            || u.getEmail().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
            usersTable.setItems(FXCollections.observableArrayList(filtered));
        } else {
            List<Auction> filtered = allAuctions.stream()
                    .filter(a -> a.getTitle().toLowerCase().contains(kw)
                            || a.getSeller().getUsername().toLowerCase().contains(kw))
                    .collect(Collectors.toList());
            auctionsTable.setItems(FXCollections.observableArrayList(filtered));
        }
    }

    // ── Users table columns ───────────────────────────────────
    private void setupUserColumns() {
        colUserId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colUserId.setStyle("-fx-alignment:CENTER;");

        // Avatar initials
        colUserAvatar.setCellValueFactory(c -> {
            String name = c.getValue().getUsername();
            return new SimpleStringProperty(name.length() >= 2
                    ? name.substring(0, 2).toUpperCase()
                    : name.toUpperCase());
        });
        colUserAvatar.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String init, boolean empty) {
                super.updateItem(init, empty);
                if (empty || init == null) { setGraphic(null); return; }
                Label av = new Label(init);
                av.setStyle("-fx-background-color:#eff6ff; -fx-text-fill:#2563eb;" +
                        "-fx-font-weight:bold; -fx-font-size:11px; -fx-alignment:CENTER;" +
                        "-fx-background-radius:50; -fx-min-width:30px; -fx-max-width:30px;" +
                        "-fx-min-height:30px; -fx-max-height:30px;");
                setGraphic(av);
                setAlignment(Pos.CENTER);
            }
        });

        colUserName.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUsername()));
        colUserName.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty) { setText(null); return; }
                setText(name);
                setStyle("-fx-font-weight:bold;");
            }
        });

        colUserEmail.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEmail()));

        colUserRole.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isAdmin() ? "Admin" : "Member"));
        colUserRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) { setGraphic(null); return; }
                Label badge = new Label(role);
                badge.getStyleClass().addAll("badge",
                        "Admin".equals(role) ? "badge-amber" : "badge-blue");
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        colUserStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getAccountStatus().toString()));
        colUserStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label pill = new Label(status);
                pill.getStyleClass().add("ACTIVE".equals(status)
                        ? "pill-running" : "pill-ending");
                setGraphic(pill);
                setAlignment(Pos.CENTER);
            }
        });

        colUserBids.setCellValueFactory(c -> {
            User userInRow = c.getValue(); // Lấy User của dòng này

            // Bạn cần dùng Service để đếm xem User này đã bid bao nhiêu lần trên toàn hệ thống
            // Hoặc trong một ngữ cảnh cụ thể nào đó
            long count = auctionService.getBidsByUser(userInRow.getId()).size();

            return new SimpleStringProperty(String.valueOf(count));
        });

        colUserJoined.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getJoinedDate()));

        // Action buttons: Ban / Delete
        colUserAction.setCellFactory(col -> new TableCell<User,String>() {
            private final Button banBtn = new Button("Ban");
            private final Button delBtn = new Button("Delete");
            private final HBox   box    = new HBox(6, banBtn, delBtn);

            {
                banBtn.getStyleClass().add("btn-secondary");
                delBtn.getStyleClass().add("btn-danger");
                banBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                delBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                box.setAlignment(Pos.CENTER);

                banBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    handleBanUser(u);
                });
                delBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    handleDeleteUser(u);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                User u = getTableView().getItems().get(getIndex());
                // Can't ban/delete yourself
                if (u != null && currentAdmin != null) {
                    // Nên dùng .equals() nếu ID là kiểu Long (Object) để tránh lỗi so sánh địa chỉ
                    boolean isSelf = u.getId().equals(currentAdmin.getId());
                    banBtn.setDisable(isSelf);
                    delBtn.setDisable(isSelf);
                }
                setGraphic(box);
            }
        });
    }

    // ── Auctions table columns ────────────────────────────────
    private void setupAuctionColumns() {
        colAucId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colAucTitle.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTitle()));
        colAucTitle.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                if (empty) { setText(null); return; }
                setText(t);
                setStyle("-fx-font-weight:bold;");
            }
        });

        colAucSeller.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getSeller().getUsername()));
        colAucCat.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getCategory()));
        colAucCat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) { setGraphic(null); return; }
                Label badge = new Label(cat);
                badge.getStyleClass().addAll("badge", "badge-blue");
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        colAucPrice.setCellValueFactory(c ->
                new SimpleStringProperty( String.format("%,.0f", c.getValue().getCurrentPrice())));
        colAucPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String p, boolean empty) {
                super.updateItem(p, empty);
                if (empty) { setText(null); return; }
                setText(p);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#2563eb;");
            }
        });

        colAucBids.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getBidCount())));
        colAucStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatusLabel()));
        colAucStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label pill = new Label(status);
                String sc = switch (status) {
                    case "Live"     -> "pill-running";
                    case "Pending"  -> "pill-pending";
                    case "Ending"   -> "pill-ending";
                    default         -> "pill-finished";
                };
                pill.getStyleClass().add(sc);
                setGraphic(pill);
                setAlignment(Pos.CENTER);
            }
        });

        colAucEnds.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEndTimeFormatted()));

        // Action: Delete auction
        colAucAction.setCellFactory(col -> new TableCell<Auction,String>() {
            private final Button viewBtn = new Button("View");
            private final Button delBtn  = new Button("Delete");
            private final HBox   box     = new HBox(6, viewBtn, delBtn);

            {
                viewBtn.getStyleClass().add("btn-ghost");
                delBtn.getStyleClass().add("btn-danger");
                viewBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                delBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                box.setAlignment(Pos.CENTER);

                viewBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    openAuctionDetail(a);
                });
                delBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleDeleteAuction(a);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Căn giữa các nút trong ô
                    setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }

    // ── Actions ───────────────────────────────────────────────
    private void handleBanUser(User user) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Ban User");
        confirm.setHeaderText("Ban " + user.getUsername() + "?");
        confirm.setContentText("This will suspend the user's account.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // 1. Gọi service (hết đỏ sau khi bạn làm bước 1)
                authService.banUser(user.getId());

                // 2. Cập nhật status cho đối tượng user hiện tại
                // Giả sử class User của bạn có hàm setStatus hoặc tương tự
                user.setAccountStatus(AccountStatus.BANNED);

                // 3. Lúc này refresh mới có tác dụng thực sự
                usersTable.refresh();

                showInfo("User " + user.getUsername() + " has been banned.");
            }
        });

    }

    private void handleDeleteUser(User user) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete " + user.getUsername() + "?");
        confirm.setContentText("⚠️ This action cannot be undone.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                authService.deleteUser(user.getId());
                allUsers.removeIf(u -> u.getId() == user.getId());
                usersTable.setItems(FXCollections.observableArrayList(allUsers));
                showInfo("User deleted.");
            }
        });
    }

    private void handleDeleteAuction(Auction auction) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Delete Auction");
        confirm.setHeaderText("Delete \"" + auction.getTitle() + "\"?");
        confirm.setContentText("This will remove the auction permanently.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                auctionService.deleteAuction(auction.getId());
                allAuctions.removeIf(a -> a.getId() == auction.getId());
                auctionsTable.setItems(FXCollections.observableArrayList(allAuctions));
                showInfo("Auction deleted.");
            }
        });
    }

    private void openAuctionDetail(Auction a) {
        MainController main = (MainController) usersTable
                .getScene().lookup("#mainRoot").getUserData();
        main.loadContent("/com/auction/client/fxml/auction_detail.fxml",
                (AuctionDetailController ctrl) -> ctrl.initData(currentAdmin, a));
    }

    private void setActiveTab(Button tab) {
        List.of(tabUsers, tabAuctions)
                .forEach(b -> b.getStyleClass().setAll("btn-secondary"));
        tab.getStyleClass().setAll("btn-primary");
    }

    private void showInfo(String msg) {
        Alert info = new Alert(AlertType.INFORMATION);
        info.setTitle("Admin Action");
        info.setHeaderText(null);
        info.setContentText(msg);
        info.show();
    }
}