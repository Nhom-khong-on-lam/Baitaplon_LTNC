package com.auction.client.controller;

import com.auction.client.Enum.AuctionStatus;
import com.auction.client.model.Auction;
import com.auction.client.model.User;
import com.auction.client.service.AuctionService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminAuctionsController — Quản lý toàn bộ phiên đấu giá.
 * Chức năng: xem, filter, search, kết thúc sớm, xóa.
 */
public class AdminAuctionsController {

    @FXML private Button    tabAll, tabLive, tabEnding, tabFinished;
    @FXML private Label     totalLabel;
    @FXML private TextField searchField;

    @FXML private TableView<Auction>              auctionsTable;
    @FXML private TableColumn<Auction, String>    colId, colIcon, colTitle,
            colSeller, colCat, colPrice,
            colBids, colStatus, colEnds,
            colActions;

    private final AuctionService auctionService = new AuctionService();

    private User           currentAdmin;
    private List<Auction>  allAuctions;

    @FXML
    public void initialize() {
        setupColumns();
    }

    public void initData(User admin) {
        this.currentAdmin = admin;
        this.allAuctions  = new java.util.ArrayList<>(auctionService.getAllAuctions());
        totalLabel.setText(allAuctions.size() + " auctions");
        setActiveTab(tabAll);
        loadTable(allAuctions);
    }

    // ── Tab filters ───────────────────────────────────────────
    @FXML
    public void showAll() {
        setActiveTab(tabAll);
        loadTable(allAuctions);
    }

    @FXML
    public void showLive() {
        setActiveTab(tabLive);
        loadTable(allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING
                        && a.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList()));
    }

    @FXML
    public void showEnding() {
        setActiveTab(tabEnding);
        // Ending = còn dưới 1 giờ
        loadTable(allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING
                        && a.getEndTime().isAfter(LocalDateTime.now())
                        && a.getEndTime().isBefore(LocalDateTime.now().plusHours(1)))
                .collect(Collectors.toList()));
    }

    @FXML
    public void showFinished() {
        setActiveTab(tabFinished);
        loadTable(allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED
                        || a.getStatus() == AuctionStatus.PAID
                        || a.getEndTime().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList()));
    }

    @FXML
    public void handleSearch() {
        String kw = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        if (kw.isEmpty()) {
            loadTable(allAuctions);
            return;
        }
        loadTable(allAuctions.stream()
                .filter(a -> a.getItem().getName().toLowerCase().contains(kw)
                        || a.getSeller().getUsername().toLowerCase().contains(kw))
                .collect(Collectors.toList()));
    }

    // ── Table setup ───────────────────────────────────────────
    private void setupColumns() {

        colId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        // Category icon
        colIcon.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getCategory()));
        colIcon.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) { setGraphic(null); return; }
                Label icon = new Label(categoryIcon(cat));
                icon.setStyle("-fx-font-size:20px;");
                setGraphic(icon); setAlignment(Pos.CENTER);
            }
        });

        colTitle.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getName()));
        colTitle.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                if (empty) { setText(null); return; }
                setText(t);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#1a202c;");
            }
        });

        colSeller.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getSeller().getUsername()));

        colCat.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getCategory()));
        colCat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) { setGraphic(null); return; }
                Label badge = new Label(cat);
                badge.getStyleClass().addAll("badge", "badge-blue");
                setGraphic(badge); setAlignment(Pos.CENTER);
            }
        });

        colPrice.setCellValueFactory(c ->
                new SimpleStringProperty(
                        "$" + String.format("%,.0f", c.getValue().getCurrentPrice())));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String p, boolean empty) {
                super.updateItem(p, empty);
                if (empty) { setText(null); return; }
                setText(p);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#2563eb;");
            }
        });

        colBids.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.valueOf(c.getValue().getBidHistory().size())));

        // Status pill
        colStatus.setCellValueFactory(c -> {
            Auction a = c.getValue();
            boolean live = a.getStatus() == AuctionStatus.RUNNING
                    && a.getEndTime().isAfter(LocalDateTime.now());
            boolean ending = live && a.getEndTime()
                    .isBefore(LocalDateTime.now().plusHours(1));
            String label = ending ? "Ending" : live ? "Live" : "Finished";
            return new SimpleStringProperty(label);
        });
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label pill = new Label(status);
                String sc = switch (status) {
                    case "Live"     -> "pill-running";
                    case "Ending"   -> "pill-ending";
                    default         -> "pill-finished";
                };
                pill.getStyleClass().add(sc);
                setGraphic(pill); setAlignment(Pos.CENTER);
            }
        });

        colEnds.setCellValueFactory(c -> {
            LocalDateTime end = c.getValue().getEndTime();
            return new SimpleStringProperty(
                    end != null ? end.format(java.time.format.DateTimeFormatter
                            .ofPattern("MMM dd, HH:mm")) : "—");
        });

        // Actions: End Early + Delete
        colActions.setCellFactory(col -> new TableCell<Auction,String>() {
            private final Button endBtn = new Button("End Early");
            private final Button delBtn = new Button("Delete");
            private final HBox   box    = new HBox(8, endBtn, delBtn);

            {
                endBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                endBtn.getStyleClass().add("btn-secondary");
                delBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                delBtn.getStyleClass().add("btn-danger");
                box.setAlignment(Pos.CENTER);

                endBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleEndEarly(a);
                });
                delBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleDelete(a);
                });
            }

            @Override
            protected void updateItem(String o, boolean empty) {
                super.updateItem(o, empty);
                if (empty) { setGraphic(null); return; }
                Auction a = getTableView().getItems().get(getIndex());
                boolean live = a.getStatus() == AuctionStatus.RUNNING
                        && a.getEndTime().isAfter(LocalDateTime.now());
                endBtn.setDisable(!live);
                setGraphic(box);
            }
        });
    }

    private void loadTable(List<Auction> list) {
        ObservableList<Auction> obs = FXCollections.observableArrayList(list);
        auctionsTable.setItems(obs);
        AnimationUtil.fadeIn(auctionsTable, 250);
        totalLabel.setText(list.size() + " auctions");
    }

    private void setActiveTab(Button tab) {
        List.of(tabAll, tabLive, tabEnding, tabFinished)
                .forEach(b -> b.getStyleClass().setAll("btn-secondary"));
        tab.getStyleClass().setAll("btn-primary");
    }

    // ── Actions ───────────────────────────────────────────────
    private void handleEndEarly(Auction auction) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("End Auction Early");
        confirm.setHeaderText("End \"" + auction.getItem().getName() + "\" now?");
        confirm.setContentText("The current highest bidder will win this auction.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                auctionService.endAuctionEarly(auction.getId());
                auctionsTable.refresh();
                showInfo("Auction ended successfully.");
            }
        });
    }

    private void handleDelete(Auction auction) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Delete Auction");
        confirm.setHeaderText("Delete \"" + auction.getItem().getName() + "\"?");
        confirm.setContentText("⚠️ This will permanently remove the auction and all bids.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                auctionService.deleteAuction(auction.getId());
                allAuctions.removeIf(a -> a.getId().equals(auction.getId()));
                loadTable(allAuctions);
                showInfo("Auction deleted.");
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

    /** Map category name → emoji icon */
    private String categoryIcon(String cat) {
        if (cat == null) return "📦";
        return switch (cat) {
            case "Art"         -> "🎨";
            case "Electronics" -> "💻";
            case "Vehicle"     -> "🚗";
            case "Jewelry"     -> "💎";
            case "Fashion"     -> "👗";
            case "Real Estate" -> "🏠";
            default            -> "📦";
        };
    }
}