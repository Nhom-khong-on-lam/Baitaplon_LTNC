package com.auction.client.controller;


import com.auction.client.service.AuctionService;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminAuctionsController — Quản lý toàn bộ phiên đấu giá.
 * Chức năng: xem, filter, search, kết thúc sớm, gia hạn, xóa.
 */
public class AdminAuctionsController {

    @FXML private Button tabAll, tabLive, tabEnding, tabFinished;
    @FXML private Label totalLabel;
    @FXML private TextField searchField;

    @FXML private TableView<Auction> auctionsTable;
    @FXML private TableColumn<Auction, String> colId, colTitle,
            colSeller, colCat, colPrice,
            colBids, colStatus, colEnds,
            colActions;

    private AuctionService auctionService;
    private User currentAdmin;
    private List<Auction> allAuctions;
    private String currentTab = "ALL";

    @FXML
    public void initialize() {
        setupColumns();
    }

    public void initData(User admin) {
        this.currentAdmin = admin;
        if (auctionService == null) auctionService = new AuctionService();

        // 1. Mở luồng phụ để tải dữ liệu từ Server, tránh đơ UI
        new Thread(() -> {
            List<Auction> dataFromServer = auctionService.getAllAuctions();

            // 2. Tải xong thì đưa về luồng chính để vẽ lên bảng
            javafx.application.Platform.runLater(() -> {
                this.allAuctions = new ArrayList<>(dataFromServer);
                totalLabel.setText(allAuctions.size() + " auctions");
                setActiveTab(tabAll);
                loadTable(allAuctions);
            });
        }).start();
    }

    // ── Tab filters ───────────────────────────────────────────

    @FXML
    public void showAll() {
        currentTab = "ALL";
        setActiveTab(tabAll);
        loadTable(allAuctions);
    }

    @FXML
    public void showLive() {
        currentTab = "LIVE";
        setActiveTab(tabLive);
        loadTable(allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING
                        && a.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList()));
    }

    @FXML
    public void showEnding() {
        currentTab = "ENDING";
        setActiveTab(tabEnding);
        loadTable(allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING
                        && a.getEndTime().isAfter(LocalDateTime.now())
                        && a.getEndTime().isBefore(LocalDateTime.now().plusHours(1)))
                .collect(Collectors.toList()));
    }

    @FXML
    public void showFinished() {
        currentTab = "FINISHED";
        setActiveTab(tabFinished);
        loadTable(allAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.FINISHED
                        || a.getStatus() == AuctionStatus.PAID
                        || a.getEndTime().isBefore(LocalDateTime.now()))
                .collect(Collectors.toList()));
    }

    private void refreshCurrentTab() {
        switch (currentTab) {
            case "LIVE"     -> showLive();
            case "ENDING"   -> showEnding();
            case "FINISHED" -> showFinished();
            default         -> showAll();
        }
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
        auctionsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        colId.setMinWidth(70);
        colTitle.setMinWidth(100);
        colSeller.setMinWidth(150);
        colCat.setMinWidth(120);
        colPrice.setMinWidth(120);
        colStatus.setMinWidth(120);
        colActions.setMinWidth(200);

        colId.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        colTitle.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getItem().getName()));
        colTitle.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String t, boolean empty) {
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
            @Override
            protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) { setGraphic(null); return; }
                Label badge = new Label(cat);
                badge.getStyleClass().addAll("badge", "badge-blue");
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        colPrice.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.format("%,.0f", c.getValue().getCurrentPrice())));
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String p, boolean empty) {
                super.updateItem(p, empty);
                if (empty) { setText(null); return; }
                setText(p);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#2563eb;");
            }
        });

        colBids.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.valueOf(c.getValue().getBidHistory().size())));

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
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label pill = new Label(status);
                String sc = switch (status) {
                    case "Live"   -> "pill-running";
                    case "Ending" -> "pill-ending";
                    default       -> "pill-finished";
                };
                pill.getStyleClass().add(sc);
                setGraphic(pill);
                setAlignment(Pos.CENTER);
            }
        });

        colEnds.setCellValueFactory(c -> {
            LocalDateTime end = c.getValue().getEndTime();
            String text = end == null ? "—"
                    : end.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
            return new SimpleStringProperty(text);
        });

        // ── Actions: End Early + Extend + Delete ──────────────
        colActions.setCellFactory(col -> new TableCell<Auction, String>() {
            private final Button endBtn    = new Button("End Early");
            private final Button extendBtn = new Button("Extend");
            private final Button delBtn    = new Button("Delete");
            private final HBox box         = new HBox(6, endBtn, extendBtn, delBtn);

            {
                endBtn.setStyle("-fx-padding:4 8; -fx-font-size:11px;");
                endBtn.getStyleClass().add("btn-secondary");

                extendBtn.setStyle("-fx-padding:4 8; -fx-font-size:11px;");
                extendBtn.getStyleClass().add("btn-primary");

                delBtn.setStyle("-fx-padding:4 8; -fx-font-size:11px;");
                delBtn.getStyleClass().add("btn-danger");

                box.setAlignment(Pos.CENTER);

                endBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleEndEarly(a);
                });
                extendBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleExtend(a);
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
                extendBtn.setDisable(!live);
                setGraphic(box);
            }
        });
    }

    private void loadTable(List<Auction> list) {
        ObservableList<Auction> obs = FXCollections.observableArrayList(list);
        javafx.application.Platform.runLater(() -> {
            if (auctionsTable != null) {
                auctionsTable.setItems(obs);
                if (auctionsTable.getScene() != null)
                    AnimationUtil.fadeIn(auctionsTable, 250);
            }
            if (totalLabel != null)
                totalLabel.setText(list.size() + " auctions");
        });
    }

    private void setActiveTab(Button tab) {
        List.of(tabAll, tabLive, tabEnding, tabFinished)
                .forEach(b -> b.getStyleClass().setAll("btn-secondary"));
        tab.getStyleClass().setAll("btn-primary");
    }

    // ── Actions ───────────────────────────────────────────────

    private void handleExtend(Auction auction) {
        boolean isLive = auction.getStatus() == AuctionStatus.RUNNING
                && auction.getEndTime().isAfter(LocalDateTime.now());
        if (!isLive) {
            showError("This auction is not active and cannot be extended.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                "1 hour", "1 hour", "3 hours", "6 hours", "12 hours", "24 hours");
        dialog.setTitle("Extend Auction");
        dialog.setHeaderText("Extend \"" + auction.getItem().getName() + "\"");
        dialog.setContentText("Current end time: "
                + auction.getEndTime().format(
                java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));

        dialog.showAndWait().ifPresent(choice -> {
            int hours = switch (choice) {
                case "3 hours"  -> 3;
                case "6 hours"  -> 6;
                case "12 hours" -> 12;
                case "24 hours" -> 24;
                default         -> 1;
            };

            // Chạy ngầm việc gửi yêu cầu gia hạn lên Server
            new Thread(() -> {
                boolean success = auctionService.extendAuction(auction.getId(), hours);

                // Cập nhật lại thông báo trên UI
                javafx.application.Platform.runLater(() -> {
                    if (success) {
                        refreshCurrentTab();
                        showInfo("✓ Auction extended successfully.");
                    } else {
                        showError("Failed to extend auction.");
                    }
                });
            }).start();
        });
    }

    private void handleEndEarly(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("End Auction Early");
        confirm.setHeaderText("End \"" + auction.getItem().getName() + "\" now?");
        confirm.setContentText("The current highest bidder will win this auction.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {

                // Chạy ngầm gọi mạng
                new Thread(() -> {
                    auctionService.endAuctionEarly(auction.getId());

                    javafx.application.Platform.runLater(() -> {
                        refreshCurrentTab();
                        showInfo("Auction ended successfully.");
                    });
                }).start();
            }
        });
    }

    private void handleDelete(Auction auction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Auction");
        confirm.setHeaderText("Delete \"" + auction.getItem().getName() + "\"?");
        confirm.setContentText("⚠️ This will permanently remove the auction and all bids.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {

                new Thread(() -> {
                    auctionService.deleteAuction(auction.getId());

                    javafx.application.Platform.runLater(() -> {
                        allAuctions.removeIf(a -> a.getId().equals(auction.getId()));
                        refreshCurrentTab();
                        showInfo("Auction deleted.");
                    });
                }).start();
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────

    private void showInfo(String msg) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Admin Action");
        info.setHeaderText(null);
        info.setContentText(msg);
        info.show();
    }

    private void showError(String msg) {
        Alert error = new Alert(Alert.AlertType.ERROR);
        error.setTitle("Error");
        error.setHeaderText(null);
        error.setContentText(msg);
        error.show();
    }

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

    // ── Getters & Setters ─────────────────────────────────────

    public void setAuctionService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public TableView<Auction> getAuctionsTable()  { return auctionsTable; }
    public TextField getSearchField()             { return searchField; }

    public void setAuctionsTable(TableView<Auction> auctionsTable) {
        this.auctionsTable = auctionsTable;
    }

    public void setSearchField(TextField searchField) {
        this.searchField = searchField;
    }

    public void setTotalLabel(Label label)   { this.totalLabel = label; }
    public void setTabAll(Button button)     { this.tabAll = button; }
    public void setTabLive(Button button)    { this.tabLive = button; }
    public void setTabEnding(Button button)  { this.tabEnding = button; }
    public void setTabFinished(Button button){ this.tabFinished = button; }
}
