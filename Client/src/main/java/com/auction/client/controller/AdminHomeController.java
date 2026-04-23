package com.auction.client.controller;
import com.auction.client.Enum.AccountStatus;
import com.auction.client.model.*;
import com.auction.client.service.AuctionService;
import com.auction.client.service.AuthService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminHomeController {

    @FXML private Label adminNameLabel;
    @FXML private StackPane contentArea;

    private User currentAdmin;
    private final AuctionService auctionService = new AuctionService();
    private final AuthService authService = new AuthService();

    public void setUser(User user) {
        if (user == null) return;
        this.currentAdmin = user;
        adminNameLabel.setText("Welcome, " + user.getUsername());
        showDashboard();
    }

    @FXML public void showDashboard() { loadDashboard(); }
    @FXML public void showUserManagement() { loadUserManagement(); }
    @FXML public void showProductManagement() { loadProductManagement(); }
    @FXML public void showAuctionManagement() { loadAuctionManagement(); }
    @FXML public void showReports() { loadReports(); }

    @FXML public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/auth.fxml"));
            Parent root = loader.load();
            adminNameLabel.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Cannot logout: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));
        dashboard.getStyleClass().add("card");
        Label title = new Label("📊 Admin Dashboard");
        title.getStyleClass().add("section-title");

        GridPane stats = new GridPane();
        stats.setHgap(20); stats.setVgap(20);
        stats.setPadding(new Insets(10,0,20,0));

        int totalUsers = authService.getAllUsers().size();  // dùng authService
        int activeAuctions = auctionService.getActiveAuctions().size();
        int totalAuctions = auctionService.getAllAuctions().size();
        double totalRevenue = 12500; // mock

        stats.add(createStatCard("Total Users", String.valueOf(totalUsers)), 0,0);
        stats.add(createStatCard("Active Auctions", String.valueOf(activeAuctions)), 1,0);
        stats.add(createStatCard("Total Auctions", String.valueOf(totalAuctions)), 2,0);
        stats.add(createStatCard("Revenue ($)", String.valueOf(totalRevenue)), 3,0);

        dashboard.getChildren().addAll(title, stats);
        setContent(dashboard);
    }

    private VBox createStatCard(String label, String value) {
        VBox card = new VBox(5);
        card.getStyleClass().add("stat-card");
        Label lbl = new Label(label); lbl.getStyleClass().add("stat-label");
        Label val = new Label(value); val.getStyleClass().add("stat-value");
        card.getChildren().addAll(lbl, val);
        return card;
    }

    private void loadUserManagement() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");
        Label title = new Label("👥 User Management");
        title.getStyleClass().add("section-title");

        TableView<User> table = new TableView<>();
        TableColumn<User, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUsername()));
        TableColumn<User, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEmail()));
        TableColumn<User, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSystemRole().toString()));
        TableColumn<User, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAccountStatus().toString()));

        TableColumn<User, Void> colAction = new TableColumn<>("Action");
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button();
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                User u = getTableView().getItems().get(getIndex());
                boolean isActive = u.getAccountStatus() == AccountStatus.ACTIVE;
                btn.setText(isActive ? "BLOCK" : "UNBLOCK");
                btn.getStyleClass().add(isActive ? "button-danger" : "button-primary");
                btn.setOnAction(e -> {
                    AccountStatus newStatus = isActive ? AccountStatus.SUSPENDED : AccountStatus.ACTIVE;
                    authService.updateUserStatus(u.getId(), newStatus);
                    showAlert("Status Changed", "User " + u.getUsername() + " is now " + newStatus, Alert.AlertType.INFORMATION);
                    loadUserManagement(); // refresh bảng
                });
                setGraphic(btn);
            }
        });
        table.getColumns().addAll(colUser, colEmail, colRole, colStatus, colAction);
        table.setItems(FXCollections.observableArrayList(authService.getAllUsers()));
        panel.getChildren().addAll(title, table);
        setContent(panel);
    }

    private void loadProductManagement() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");
        Label title = new Label("🏷️ Product Management (All Items)");
        title.getStyleClass().add("section-title");

        List<Item> allItems = new java.util.ArrayList<>();
        for (Auction a : auctionService.getAllAuctions()) {
            if (a.getItem() != null) allItems.add(a.getItem());
        }
        TableView<Item> table = new TableView<>();
        TableColumn<Item, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        TableColumn<Item, Double> colStart = new TableColumn<>("Start Price");
        colStart.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getStartingPrice()).asObject());
        TableColumn<Item, Void> colAction = new TableColumn<>("Action");
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Remove");
            {
                btn.getStyleClass().add("button-danger");
                btn.setOnAction(e -> showAlert("Removed", "Item removed", Alert.AlertType.INFORMATION));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().addAll(colName, colStart, colAction);
        table.setItems(FXCollections.observableArrayList(allItems));
        panel.getChildren().addAll(title, table);
        setContent(panel);
    }

    private void loadAuctionManagement() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");
        Label title = new Label("⏱️ Auction Sessions");
        title.getStyleClass().add("section-title");

        TableView<Auction> table = new TableView<>();
        TableColumn<Auction, String> colItem = new TableColumn<>("Item");
        colItem.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        TableColumn<Auction, String> colSeller = new TableColumn<>("Seller");
        colSeller.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeller().getUsername()));
        TableColumn<Auction, Double> colPrice = new TableColumn<>("Current Bid");
        colPrice.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()).asObject());
        TableColumn<Auction, String> colEnd = new TableColumn<>("Ends At");
        colEnd.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        TableColumn<Auction, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().toString()));

        TableColumn<Auction, Void> colAction = new TableColumn<>("Action");
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("End Early");
            {
                btn.getStyleClass().add("button-danger");
                btn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    if (a != null) {
                        auctionService.endAuctionEarly(a.getId());
                        loadAuctionManagement();
                        showAlert("Ended", "Auction ended", Alert.AlertType.INFORMATION);
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().addAll(colItem, colSeller, colPrice, colEnd, colStatus, colAction);
        table.setItems(auctionService.getAllAuctions());
        panel.getChildren().addAll(title, table);
        setContent(panel);
    }

    private void loadReports() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");
        Label title = new Label("📈 Reports");
        title.getStyleClass().add("section-title");
        TextArea text = new TextArea();
        text.setEditable(false);
        StringBuilder sb = new StringBuilder();
        sb.append("=== Auction System Report ===\n\n");
        sb.append("Total users: ").append(authService.getAllUsers().size()).append("\n");
        sb.append("Total auctions: ").append(auctionService.getAllAuctions().size()).append("\n");
        sb.append("Active auctions: ").append(auctionService.getActiveAuctions().size()).append("\n");
        long totalBids = auctionService.getAllAuctions().stream().flatMap(a -> a.getBidHistory().stream()).count();
        sb.append("Total bids: ").append(totalBids).append("\n");
        sb.append("Generated at: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        text.setText(sb.toString());
        panel.getChildren().addAll(title, text);
        setContent(panel);
    }

    private void setContent(Parent node) {
        if (contentArea == null) return;
        contentArea.getChildren().clear();
        if (node != null) contentArea.getChildren().add(node);
    }

    private void showAlert(String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Admin Panel");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}