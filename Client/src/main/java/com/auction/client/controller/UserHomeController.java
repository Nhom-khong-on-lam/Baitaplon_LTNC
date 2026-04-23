package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.BidTransaction;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import com.auction.client.service.AuctionService;
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
import java.util.Optional;

public class UserHomeController {

    @FXML private Label userNameLabel;
    @FXML private StackPane contentArea;

    private User currentUser;
    private final AuctionService auctionService = new AuctionService();

    public void setUser(User user) {
        this.currentUser = user;
        userNameLabel.setText("Welcome, " + user.getUsername());
        showDashboard();
    }

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
            showAlert("Error", "Cannot logout", Alert.AlertType.ERROR);
        }
    }

    private void loadDashboard() {
        VBox dashboard = new VBox(20);
        dashboard.setPadding(new Insets(20));
        dashboard.getStyleClass().add("card");
        Label title = new Label("📊 User Dashboard");
        title.getStyleClass().add("section-title");

        GridPane stats = new GridPane();
        stats.setHgap(20); stats.setVgap(20);
        stats.setPadding(new Insets(10,0,20,0));

        int activeAuctions = auctionService.getActiveAuctions().size();
        List<BidTransaction> myBids = auctionService.getBidsByUser(currentUser.getId());
        long myBidCount = myBids.size();
        double highestBid = myBids.stream().mapToDouble(BidTransaction::getAmount).max().orElse(0);
        double totalSpent = myBids.stream().mapToDouble(BidTransaction::getAmount).sum();

        stats.add(createStatCard("Active Auctions", String.valueOf(activeAuctions)), 0,0);
        stats.add(createStatCard("My Bids", String.valueOf(myBidCount)), 1,0);
        stats.add(createStatCard("Highest Bid", "$" + highestBid), 2,0);
        stats.add(createStatCard("Total Spent", "$" + totalSpent), 3,0);

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

    private void loadActiveAuctions() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");
        Label title = new Label("🔨 Active Auctions");
        title.getStyleClass().add("section-title");

        TableView<Auction> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Auction, String> colName = new TableColumn<>("Item");
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        TableColumn<Auction, String> colSeller = new TableColumn<>("Seller");
        colSeller.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSeller().getUsername()));
        TableColumn<Auction, Double> colPrice = new TableColumn<>("Current Bid ($)");
        colPrice.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()).asObject());
        TableColumn<Auction, String> colEnd = new TableColumn<>("Ends At");
        colEnd.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        TableColumn<Auction, Void> colAction = new TableColumn<>("Place Bid");
        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Bid");
            { btn.getStyleClass().add("button-primary");
                btn.setOnAction(e -> showBidDialog(getTableView().getItems().get(getIndex()))); }
            @Override protected void updateItem(Void item, boolean empty) {
                setGraphic(empty ? null : btn);
            }
        });
        table.getColumns().addAll(colName, colSeller, colPrice, colEnd, colAction);
        table.setItems(auctionService.getActiveAuctions());
        panel.getChildren().addAll(title, table);
        setContent(panel);
    }

    private void showBidDialog(Auction auction) {
        TextInputDialog dlg = new TextInputDialog(String.valueOf(auction.getCurrentPrice() + 10));
        dlg.setTitle("Place Bid");
        dlg.setHeaderText("Auction: " + auction.getItem().getName() + "\nCurrent: $" + auction.getCurrentPrice());
        dlg.setContentText("Your bid amount:");
        Optional<String> result = dlg.showAndWait();
        result.ifPresent(input -> {
            try {
                double amount = Double.parseDouble(input);
                if (auctionService.placeBid(auction.getId(), currentUser, amount)) {
                    showAlert("Success", "Bid placed: $" + amount, Alert.AlertType.INFORMATION);
                    showAuctions();
                } else {
                    showAlert("Failed", "Bid must be higher than current price.", Alert.AlertType.WARNING);
                }
            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid number", Alert.AlertType.ERROR);
            }
        });
    }

    private void loadMyBids() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");
        Label title = new Label("💰 My Bids");
        title.getStyleClass().add("section-title");

        TableView<BidTransaction> table = new TableView<>();
        TableColumn<BidTransaction, String> colItem = new TableColumn<>("Item");
        colItem.setCellValueFactory(cell -> {
            for (Auction a : auctionService.getAllAuctions()) {
                if (a.getBidHistory().contains(cell.getValue()))
                    return new SimpleStringProperty(a.getItem().getName());
            }
            return new SimpleStringProperty("Unknown");
        });
        TableColumn<BidTransaction, Double> colAmount = new TableColumn<>("Amount ($)");
        colAmount.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getAmount()).asObject());
        TableColumn<BidTransaction, String> colTime = new TableColumn<>("Time");
        colTime.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBidTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        table.getColumns().addAll(colItem, colAmount, colTime);
        table.setItems(FXCollections.observableArrayList(auctionService.getBidsByUser(currentUser.getId())));
        panel.getChildren().addAll(title, table);
        setContent(panel);
    }

    private void loadMyProducts() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");
        Label title = new Label("📦 My Products on Auction");
        title.getStyleClass().add("section-title");

        TableView<Auction> table = new TableView<>();
        TableColumn<Auction, String> colName = new TableColumn<>("Product");
        colName.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getItem().getName()));
        TableColumn<Auction, Double> colStart = new TableColumn<>("Start Price");
        colStart.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getItem().getStartingPrice()).asObject());
        TableColumn<Auction, Double> colCurrent = new TableColumn<>("Current Bid");
        colCurrent.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getCurrentPrice()).asObject());
        TableColumn<Auction, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().toString()));
        table.getColumns().addAll(colName, colStart, colCurrent, colStatus);
        table.setItems(FXCollections.observableArrayList(auctionService.getAuctionsBySeller(currentUser.getId())));

        Button createBtn = new Button("➕ Create New Auction");
        createBtn.getStyleClass().add("button-primary");
        createBtn.setOnAction(e -> showCreateAuctionDialog());

        panel.getChildren().addAll(title, table, createBtn);
        setContent(panel);
    }


    private void showCreateAuctionDialog() {
        // Nhập tên sản phẩm
        TextInputDialog nameDlg = new TextInputDialog();
        nameDlg.setHeaderText("Product Name");
        nameDlg.setTitle("New Product");
        Optional<String> nameRes = nameDlg.showAndWait();
        if (!nameRes.isPresent()) return;

        TextInputDialog descDlg = new TextInputDialog();
        descDlg.setHeaderText("Description");
        Optional<String> descRes = descDlg.showAndWait();
        if (!descRes.isPresent()) return;

        TextInputDialog priceDlg = new TextInputDialog();
        priceDlg.setHeaderText("Starting Price");
        Optional<String> priceRes = priceDlg.showAndWait();
        if (!priceRes.isPresent()) return;
        double startPrice;
        try {
            startPrice = Double.parseDouble(priceRes.get());
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid price", Alert.AlertType.ERROR);
            return;
        }

        TextInputDialog daysDlg = new TextInputDialog("3");
        daysDlg.setHeaderText("Duration (days)");
        Optional<String> daysRes = daysDlg.showAndWait();
        if (!daysRes.isPresent()) return;
        int days;
        try {
            days = Integer.parseInt(daysRes.get());
        } catch (NumberFormatException e) {
            showAlert("Error", "Invalid days", Alert.AlertType.ERROR);
            return;
        }

        Item newItem = new Item();
        newItem.setName(nameRes.get());
        newItem.setDescription(descRes.get());
        newItem.setStartingPrice(startPrice);
        newItem.setCurrentPrice(startPrice);
        newItem.setSellerId(currentUser.getId());
        newItem.setSellerName(currentUser.getUsername());
        newItem.setId(System.currentTimeMillis()); // tạm thời dùng timestamp làm id

        LocalDateTime endTime = LocalDateTime.now().plusDays(days);
        Auction newAuction = auctionService.createAuction(newItem, currentUser, endTime);
        if (newAuction != null) {
            showAlert("Created", "Auction created: " + newItem.getName(), Alert.AlertType.INFORMATION);
            showMyProducts(); // refresh danh sách sản phẩm của user
        } else {
            showAlert("Failed", "Cannot create auction", Alert.AlertType.ERROR);
        }
    }

    private void loadProfile() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));
        panel.getStyleClass().add("card");
        Label title = new Label("👤 Profile");
        title.getStyleClass().add("section-title");
        GridPane grid = new GridPane(); grid.setHgap(20); grid.setVgap(15); grid.setPadding(new Insets(10));
        grid.add(new Label("Username:"), 0,0); grid.add(new Label(currentUser.getUsername()), 1,0);
        grid.add(new Label("Email:"), 0,1); grid.add(new Label(currentUser.getEmail()), 1,1);
        grid.add(new Label("Role:"), 0,2); grid.add(new Label(currentUser.getSystemRole().toString()), 1,2);
        Button changePass = new Button("Change Password"); changePass.getStyleClass().add("button-primary");
        changePass.setOnAction(e -> showAlert("Change Password", "Not implemented yet", Alert.AlertType.INFORMATION));
        panel.getChildren().addAll(title, grid, changePass);
        setContent(panel);
    }

    private void setContent(Parent node) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
    }

    private void showAlert(String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("User Panel");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}