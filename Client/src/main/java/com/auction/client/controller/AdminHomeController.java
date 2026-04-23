package com.auction.client.controller;

import com.auction.client.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminHomeController {

    @FXML private Label adminNameLabel;
    @FXML private StackPane contentArea;

    private User currentAdmin;

    public void setUser(User user) {
        this.currentAdmin = user;
        adminNameLabel.setText("Welcome, " + user.getUsername());
        showDashboard();
    }

    @FXML public void showDashboard() { loadDashboard(); }
    @FXML public void showUserManagement() { loadUserManagement(); }
    @FXML public void showProductManagement() { loadProductManagement(); }
    @FXML public void showAuctionManagement() { loadAuctionManagement(); }
    @FXML public void showReports() { loadReports(); }

    @FXML
    public void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth.fxml"));
            Parent root = loader.load();
            adminNameLabel.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Cannot logout", Alert.AlertType.ERROR);
        }
    }

    private void loadDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));
        dashboard.getStyleClass().add("card");

        Label title = new Label("📊 System Dashboard");
        title.getStyleClass().add("section-title");

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setPadding(new Insets(10, 0, 20, 0));

        statsGrid.add(createStatCard("Total Users", "156", "#d4af37"), 0, 0);
        statsGrid.add(createStatCard("Active Auctions", "12", "#d4af37"), 1, 0);
        statsGrid.add(createStatCard("Products", "324", "#d4af37"), 2, 0);
        statsGrid.add(createStatCard("Completed Auctions", "89", "#d4af37"), 3, 0);
        statsGrid.add(createStatCard("Total Bids", "1,245", "#d4af37"), 0, 1);
        statsGrid.add(createStatCard("Revenue (USD)", "$12,450", "#d4af37"), 1, 1);

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

    private void loadUserManagement() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");

        Label title = new Label("👥 User Management");
        title.getStyleClass().add("section-title");

        TableView<UserRow> table = new TableView<>();
        TableColumn<UserRow, String> colUsername = new TableColumn<>("Username");
        colUsername.setCellValueFactory(cell -> cell.getValue().usernameProperty());
        TableColumn<UserRow, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(cell -> cell.getValue().emailProperty());
        TableColumn<UserRow, String> colRole = new TableColumn<>("Role");
        colRole.setCellValueFactory(cell -> cell.getValue().roleProperty());
        TableColumn<UserRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        TableColumn<UserRow, Button> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(cell -> cell.getValue().actionProperty());

        table.getColumns().addAll(colUsername, colEmail, colRole, colStatus, colAction);

        ObservableList<UserRow> data = FXCollections.observableArrayList();
        data.add(new UserRow("admin", "admin@gmail.com", "ADMIN", "ACTIVE", this::disableUser));
        data.add(new UserRow("dung", "dung@gmail.com", "USER", "ACTIVE", this::disableUser));
        data.add(new UserRow("chau", "chau@gmail.com", "USER", "BLOCKED", this::enableUser));
        table.setItems(data);

        HBox btnBar = new HBox(10);
        Button addBtn = new Button("➕ Add New User");
        addBtn.getStyleClass().add("button-primary");
        addBtn.setOnAction(e -> showAlert("Info", "Feature will be implemented", Alert.AlertType.INFORMATION));
        btnBar.getChildren().add(addBtn);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        btnBar.getChildren().add(spacer);

        panel.getChildren().addAll(title, table, btnBar);
        setContent(panel);
    }

    private void disableUser(UserRow user) {
        user.setStatus("BLOCKED");
        showAlert("Success", "User " + user.getUsername() + " blocked", Alert.AlertType.INFORMATION);
    }

    private void enableUser(UserRow user) {
        user.setStatus("ACTIVE");
    }

    private void loadProductManagement() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");

        Label title = new Label("🏷️ Product Management");
        title.getStyleClass().add("section-title");

        TableView<ProductRow> table = new TableView<>();
        TableColumn<ProductRow, String> colName = new TableColumn<>("Product Name");
        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        TableColumn<ProductRow, String> colOwner = new TableColumn<>("Owner");
        colOwner.setCellValueFactory(cell -> cell.getValue().ownerProperty());
        TableColumn<ProductRow, String> colPrice = new TableColumn<>("Starting Price");
        colPrice.setCellValueFactory(cell -> cell.getValue().priceProperty());
        TableColumn<ProductRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        TableColumn<ProductRow, Button> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(cell -> cell.getValue().actionProperty());

        table.getColumns().addAll(colName, colOwner, colPrice, colStatus, colAction);

        ObservableList<ProductRow> data = FXCollections.observableArrayList();
        data.add(new ProductRow("Vintage Watch", "john_doe", "$250", "Pending Approval", this::approveProduct));
        data.add(new ProductRow("Painting", "jane_smith", "$1200", "Approved", this::removeProduct));
        data.add(new ProductRow("Antique Vase", "alex", "$500", "Rejected", this::removeProduct));

        table.setItems(data);

        HBox btnBar = new HBox(10);
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().add("button-primary");
        refreshBtn.setOnAction(e -> loadProductManagement());
        btnBar.getChildren().add(refreshBtn);

        panel.getChildren().addAll(title, table, btnBar);
        setContent(panel);
    }

    private void approveProduct(ProductRow product) {
        product.setStatus("Approved");
        showAlert("Approved", product.getName() + " has been approved", Alert.AlertType.INFORMATION);
    }

    private void removeProduct(ProductRow product) {
        loadProductManagement();
    }

    private void loadAuctionManagement() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");

        Label title = new Label("⏱️ Auction Sessions");
        title.getStyleClass().add("section-title");

        TableView<AuctionRow> table = new TableView<>();
        TableColumn<AuctionRow, String> colItem = new TableColumn<>("Item");
        colItem.setCellValueFactory(cell -> cell.getValue().itemProperty());
        TableColumn<AuctionRow, String> colStart = new TableColumn<>("Start Time");
        colStart.setCellValueFactory(cell -> cell.getValue().startTimeProperty());
        TableColumn<AuctionRow, String> colEnd = new TableColumn<>("End Time");
        colEnd.setCellValueFactory(cell -> cell.getValue().endTimeProperty());
        TableColumn<AuctionRow, String> colCurrentBid = new TableColumn<>("Current Bid");
        colCurrentBid.setCellValueFactory(cell -> cell.getValue().currentBidProperty());
        TableColumn<AuctionRow, Button> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(cell -> cell.getValue().actionProperty());

        table.getColumns().addAll(colItem, colStart, colEnd, colCurrentBid, colAction);

        ObservableList<AuctionRow> data = FXCollections.observableArrayList();
        data.add(new AuctionRow("Vintage Watch", "2025-03-01 10:00", "2025-03-08 20:00", "$450", this::endAuction));
        data.add(new AuctionRow("Painting", "2025-03-02 09:00", "2025-03-09 18:00", "$1,200", this::endAuction));

        table.setItems(data);

        HBox btnBar = new HBox(10);
        Button createBtn = new Button("➕ Create New Auction");
        createBtn.getStyleClass().add("button-primary");
        createBtn.setOnAction(e -> showAlert("Create", "Open dialog to create auction", Alert.AlertType.INFORMATION));
        btnBar.getChildren().add(createBtn);

        panel.getChildren().addAll(title, table, btnBar);
        setContent(panel);
    }

    private void endAuction(AuctionRow auction) {
        showAlert("End Auction", "Auction for " + auction.getItem() + " ended.", Alert.AlertType.CONFIRMATION);
        loadAuctionManagement();
    }

    private void loadReports() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");

        Label title = new Label("📈 Reports & Statistics");
        title.getStyleClass().add("section-title");

        TextArea reportArea = new TextArea();
        reportArea.setEditable(false);
        reportArea.setStyle("-fx-control-inner-background: #1a1a1a; -fx-text-fill: #f0f0f0;");
        reportArea.setText("=== Auction System Report ===\n\n" +
                "Total users: 156\n" +
                "Active auctions: 12\n" +
                "Completed auctions: 89\n" +
                "Total bids placed: 1,245\n" +
                "Revenue: $12,450\n\n" +
                "Top selling item: Painting ($1,200)\n" +
                "Most active bidder: john_doe (45 bids)\n\n" +
                "Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        reportArea.setPrefHeight(400);
        panel.getChildren().addAll(title, reportArea);
        setContent(panel);
    }

    private void setContent(Parent node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void showAlert(String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("Auction Admin");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ----- Inner classes (giữ nguyên) -----
    public static class UserRow {
        private final String username, email, role;
        private String status;
        private final java.util.function.Consumer<UserRow> actionHandler;
        public UserRow(String username, String email, String role, String status, java.util.function.Consumer<UserRow> actionHandler) {
            this.username = username; this.email = email; this.role = role; this.status = status; this.actionHandler = actionHandler;
        }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        public javafx.beans.property.StringProperty usernameProperty() { return new javafx.beans.property.SimpleStringProperty(username); }
        public javafx.beans.property.StringProperty emailProperty() { return new javafx.beans.property.SimpleStringProperty(email); }
        public javafx.beans.property.StringProperty roleProperty() { return new javafx.beans.property.SimpleStringProperty(role); }
        public javafx.beans.property.StringProperty statusProperty() { return new javafx.beans.property.SimpleStringProperty(status); }
        public javafx.beans.property.ObjectProperty<Button> actionProperty() {
            Button btn = new Button(status.equals("ACTIVE") ? "BLOCK" : "UNBLOCK");
            btn.getStyleClass().add("button-danger");
            btn.setOnAction(e -> actionHandler.accept(this));
            return new javafx.beans.property.SimpleObjectProperty<>(btn);
        }
    }

    public static class ProductRow {
        private final String name, owner, price;
        private String status;
        private final java.util.function.Consumer<ProductRow> actionHandler;
        public ProductRow(String name, String owner, String price, String status, java.util.function.Consumer<ProductRow> handler) {
            this.name = name; this.owner = owner; this.price = price; this.status = status; this.actionHandler = handler;
        }
        public String getName() { return name; }
        public String getOwner() { return owner; }
        public String getPrice() { return price; }
        public String getStatus() { return status; }
        public void setStatus(String s) { status = s; }
        public javafx.beans.property.StringProperty nameProperty() { return new javafx.beans.property.SimpleStringProperty(name); }
        public javafx.beans.property.StringProperty ownerProperty() { return new javafx.beans.property.SimpleStringProperty(owner); }
        public javafx.beans.property.StringProperty priceProperty() { return new javafx.beans.property.SimpleStringProperty(price); }
        public javafx.beans.property.StringProperty statusProperty() { return new javafx.beans.property.SimpleStringProperty(status); }
        public javafx.beans.property.ObjectProperty<Button> actionProperty() {
            Button btn = new Button(status.equals("Pending Approval") ? "Approve" : "Remove");
            btn.getStyleClass().add(status.equals("Pending Approval") ? "button-primary" : "button-danger");
            btn.setOnAction(e -> actionHandler.accept(this));
            return new javafx.beans.property.SimpleObjectProperty<>(btn);
        }
    }

    public static class AuctionRow {
        private final String item, startTime, endTime, currentBid;
        private final java.util.function.Consumer<AuctionRow> actionHandler;
        public AuctionRow(String item, String start, String end, String bid, java.util.function.Consumer<AuctionRow> handler) {
            this.item = item; this.startTime = start; this.endTime = end; this.currentBid = bid; this.actionHandler = handler;
        }
        public String getItem() { return item; }
        public javafx.beans.property.StringProperty itemProperty() { return new javafx.beans.property.SimpleStringProperty(item); }
        public javafx.beans.property.StringProperty startTimeProperty() { return new javafx.beans.property.SimpleStringProperty(startTime); }
        public javafx.beans.property.StringProperty endTimeProperty() { return new javafx.beans.property.SimpleStringProperty(endTime); }
        public javafx.beans.property.StringProperty currentBidProperty() { return new javafx.beans.property.SimpleStringProperty(currentBid); }
        public javafx.beans.property.ObjectProperty<Button> actionProperty() {
            Button btn = new Button("End Early");
            btn.getStyleClass().add("button-danger");
            btn.setOnAction(e -> actionHandler.accept(this));
            return new javafx.beans.property.SimpleObjectProperty<>(btn);
        }
    }
}