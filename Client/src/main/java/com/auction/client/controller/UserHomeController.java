package com.auction.client.controller;

import com.auction.client.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserHomeController {

    @FXML private Label userNameLabel;
    @FXML private StackPane contentArea;

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        userNameLabel.setText("Welcome, " + user.getUsername());
        showDashboard(); // mặc định hiển thị dashboard
    }

    // ========== MENU ACTIONS ==========
    @FXML public void showDashboard() { loadDashboard(); }
    @FXML public void showAuctions() { loadActiveAuctions(); }
    @FXML public void showMyBids() { loadMyBids(); }
    @FXML public void showMyProducts() { loadMyProducts(); }
    @FXML public void showProfile() { loadProfile(); }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/auth.fxml"));
            Parent root = loader.load();
            userNameLabel.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Cannot logout", Alert.AlertType.ERROR);
        }
    }

    // ========== DASHBOARD ==========
    private void loadDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));
        dashboard.getStyleClass().add("card");

        Label title = new Label("📊 User Dashboard");
        title.getStyleClass().add("section-title");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setPadding(new Insets(10, 0, 20, 0));

        statsGrid.add(createStatCard("Active Auctions", "8", "#d4af37"), 0, 0);
        statsGrid.add(createStatCard("My Active Bids", "3", "#d4af37"), 1, 0);
        statsGrid.add(createStatCard("Won Auctions", "2", "#d4af37"), 2, 0);
        statsGrid.add(createStatCard("Total Spent", "$450", "#d4af37"), 3, 0);
        statsGrid.add(createStatCard("My Products Listed", "5", "#d4af37"), 0, 1);
        statsGrid.add(createStatCard("Pending Payments", "$120", "#d4af37"), 1, 1);

        dashboard.getChildren().addAll(title, statsGrid);
        setContent(dashboard);
    }

    private VBox createStatCard(String label, String value, String color) {
        VBox card = new VBox(5);
        card.getStyleClass().add("stat-card");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");
        Label val = new Label(value);
        val.getStyleClass().add("stat-value");
        card.getChildren().addAll(lbl, val);
        return card;
    }

    // ========== ACTIVE AUCTIONS (xem danh sách đấu giá đang diễn ra) ==========
    private void loadActiveAuctions() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");

        Label title = new Label("🔨 Active Auctions");
        title.getStyleClass().add("section-title");

        TableView<AuctionItem> table = new TableView<>();
        TableColumn<AuctionItem, String> colItem = new TableColumn<>("Item");
        colItem.setCellValueFactory(cell -> cell.getValue().nameProperty());
        TableColumn<AuctionItem, String> colSeller = new TableColumn<>("Seller");
        colSeller.setCellValueFactory(cell -> cell.getValue().sellerProperty());
        TableColumn<AuctionItem, String> colCurrentBid = new TableColumn<>("Current Bid");
        colCurrentBid.setCellValueFactory(cell -> cell.getValue().currentBidProperty());
        TableColumn<AuctionItem, String> colEndTime = new TableColumn<>("Ends At");
        colEndTime.setCellValueFactory(cell -> cell.getValue().endTimeProperty());
        TableColumn<AuctionItem, Button> colAction = new TableColumn<>("Place Bid");
        colAction.setCellValueFactory(cell -> cell.getValue().bidButtonProperty());

        table.getColumns().addAll(colItem, colSeller, colCurrentBid, colEndTime, colAction);

        ObservableList<AuctionItem> data = FXCollections.observableArrayList();
        data.add(new AuctionItem("Vintage Watch", "seller1", "$250", "2025-03-08 20:00", this::placeBid));
        data.add(new AuctionItem("Painting", "artist99", "$1,200", "2025-03-09 18:00", this::placeBid));
        data.add(new AuctionItem("Antique Vase", "collectorX", "$500", "2025-03-10 12:00", this::placeBid));

        table.setItems(data);
        panel.getChildren().addAll(title, table);
        setContent(panel);
    }

    private void placeBid(AuctionItem item) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Place Bid");
        dialog.setHeaderText("Bid on: " + item.getName());
        dialog.setContentText("Your bid amount (must be higher than current bid):");
        dialog.showAndWait().ifPresent(amount -> {
            showAlert("Bid Placed", "You placed $" + amount + " on " + item.getName(), Alert.AlertType.INFORMATION);
        });
    }

    // ========== MY BIDS ==========
    private void loadMyBids() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");

        Label title = new Label("💰 My Bids");
        title.getStyleClass().add("section-title");

        TableView<BidHistory> table = new TableView<>();
        TableColumn<BidHistory, String> colItem = new TableColumn<>("Item");
        colItem.setCellValueFactory(cell -> cell.getValue().itemProperty());
        TableColumn<BidHistory, String> colAmount = new TableColumn<>("Bid Amount");
        colAmount.setCellValueFactory(cell -> cell.getValue().amountProperty());
        TableColumn<BidHistory, String> colTime = new TableColumn<>("Time");
        colTime.setCellValueFactory(cell -> cell.getValue().timeProperty());
        TableColumn<BidHistory, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());

        table.getColumns().addAll(colItem, colAmount, colTime, colStatus);

        ObservableList<BidHistory> data = FXCollections.observableArrayList();
        data.add(new BidHistory("Vintage Watch", "$260", "2025-03-01 14:23", "Leading"));
        data.add(new BidHistory("Painting", "$1,250", "2025-03-02 09:15", "Outbid"));
        data.add(new BidHistory("Antique Vase", "$510", "2025-03-03 18:45", "Won"));

        table.setItems(data);
        panel.getChildren().addAll(title, table);
        setContent(panel);
    }

    // ========== MY PRODUCTS (sản phẩm user đăng bán) ==========
    private void loadMyProducts() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");

        Label title = new Label("📦 My Products");
        title.getStyleClass().add("section-title");

        TableView<MyProduct> table = new TableView<>();
        TableColumn<MyProduct, String> colName = new TableColumn<>("Product");
        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        TableColumn<MyProduct, String> colPrice = new TableColumn<>("Starting Price");
        colPrice.setCellValueFactory(cell -> cell.getValue().startingPriceProperty());
        TableColumn<MyProduct, String> colStatus = new TableColumn<>("Auction Status");
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        TableColumn<MyProduct, Button> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(cell -> cell.getValue().actionProperty());

        table.getColumns().addAll(colName, colPrice, colStatus, colAction);

        ObservableList<MyProduct> data = FXCollections.observableArrayList();
        data.add(new MyProduct("Old Camera", "$100", "Active", this::editProduct));
        data.add(new MyProduct("Comic Book", "$30", "Ended", this::viewWinner));

        table.setItems(data);

        Button addBtn = new Button("➕ Sell New Product");
        addBtn.getStyleClass().add("button-primary");
        addBtn.setOnAction(e -> showAlert("New Product", "Form to create product (to be implemented)", Alert.AlertType.INFORMATION));

        panel.getChildren().addAll(title, table, addBtn);
        setContent(panel);
    }

    private void editProduct(MyProduct product) {
        showAlert("Edit", "Edit product: " + product.getName(), Alert.AlertType.INFORMATION);
    }

    private void viewWinner(MyProduct product) {
        showAlert("Winner", "Winner of " + product.getName() + " is userX", Alert.AlertType.INFORMATION);
    }

    // ========== PROFILE ==========
    private void loadProfile() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");

        Label title = new Label("👤 My Profile");
        title.getStyleClass().add("section-title");

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(15);
        form.setPadding(new Insets(10));

        Label userLbl = new Label("Username:");
        userLbl.getStyleClass().add("stat-label");
        Label userVal = new Label(currentUser.getUsername());
        userVal.getStyleClass().add("stat-value");

        Label emailLbl = new Label("Email:");
        Label emailVal = new Label(currentUser.getEmail());

        Label roleLbl = new Label("Role:");
        Label roleVal = new Label(currentUser.getSystemRole().toString());

        Label memberSince = new Label("Member since:");
        Label dateVal = new Label("2025-01-15"); // mock

        form.add(userLbl, 0, 0);
        form.add(userVal, 1, 0);
        form.add(emailLbl, 0, 1);
        form.add(emailVal, 1, 1);
        form.add(roleLbl, 0, 2);
        form.add(roleVal, 1, 2);
        form.add(memberSince, 0, 3);
        form.add(dateVal, 1, 3);

        Button changePassBtn = new Button("Change Password");
        changePassBtn.getStyleClass().add("button-primary");
        changePassBtn.setOnAction(e -> showAlert("Change Password", "Feature: change password dialog", Alert.AlertType.INFORMATION));

        panel.getChildren().addAll(title, form, changePassBtn);
        setContent(panel);
    }

    // ========== UTILITIES ==========
    private void setContent(Parent node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void showAlert(String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Auction User");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ---------- Inner classes for table data (property binding) ----------
    public static class AuctionItem {
        private final String name, seller, currentBid, endTime;
        private final java.util.function.Consumer<AuctionItem> bidHandler;
        public AuctionItem(String name, String seller, String currentBid, String endTime, java.util.function.Consumer<AuctionItem> handler) {
            this.name = name; this.seller = seller; this.currentBid = currentBid; this.endTime = endTime; this.bidHandler = handler;
        }
        public String getName() { return name; }
        public javafx.beans.property.StringProperty nameProperty() { return new javafx.beans.property.SimpleStringProperty(name); }
        public javafx.beans.property.StringProperty sellerProperty() { return new javafx.beans.property.SimpleStringProperty(seller); }
        public javafx.beans.property.StringProperty currentBidProperty() { return new javafx.beans.property.SimpleStringProperty(currentBid); }
        public javafx.beans.property.StringProperty endTimeProperty() { return new javafx.beans.property.SimpleStringProperty(endTime); }
        public javafx.beans.property.ObjectProperty<Button> bidButtonProperty() {
            Button btn = new Button("Bid Now");
            btn.getStyleClass().add("button-primary");
            btn.setOnAction(e -> bidHandler.accept(this));
            return new javafx.beans.property.SimpleObjectProperty<>(btn);
        }
    }

    public static class BidHistory {
        private final String item, amount, time, status;
        public BidHistory(String item, String amount, String time, String status) {
            this.item = item; this.amount = amount; this.time = time; this.status = status;
        }
        public javafx.beans.property.StringProperty itemProperty() { return new javafx.beans.property.SimpleStringProperty(item); }
        public javafx.beans.property.StringProperty amountProperty() { return new javafx.beans.property.SimpleStringProperty(amount); }
        public javafx.beans.property.StringProperty timeProperty() { return new javafx.beans.property.SimpleStringProperty(time); }
        public javafx.beans.property.StringProperty statusProperty() { return new javafx.beans.property.SimpleStringProperty(status); }
    }

    public static class MyProduct {
        private final String name, startingPrice, status;
        private final java.util.function.Consumer<MyProduct> action;
        public MyProduct(String name, String price, String status, java.util.function.Consumer<MyProduct> action) {
            this.name = name; this.startingPrice = price; this.status = status; this.action = action;
        }
        public String getName() { return name; }
        public javafx.beans.property.StringProperty nameProperty() { return new javafx.beans.property.SimpleStringProperty(name); }
        public javafx.beans.property.StringProperty startingPriceProperty() { return new javafx.beans.property.SimpleStringProperty(startingPrice); }
        public javafx.beans.property.StringProperty statusProperty() { return new javafx.beans.property.SimpleStringProperty(status); }
        public javafx.beans.property.ObjectProperty<Button> actionProperty() {
            Button btn = new Button(status.equals("Active") ? "Edit" : "View Winner");
            btn.getStyleClass().add("button-primary");
            btn.setOnAction(e -> action.accept(this));
            return new javafx.beans.property.SimpleObjectProperty<>(btn);
        }
    }
}