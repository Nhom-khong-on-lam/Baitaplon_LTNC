package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.User;
import com.auction.client.service.AuctionService;
import com.auction.client.controller.AnimationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AuctionsController — Danh sách live auctions.
 * Hỗ trợ search, filter theo category, sort, và navigate to detail.
 */
public class AuctionsController {

    @FXML private TextField  searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> sortBox;
    @FXML private Label      resultCount;
    @FXML private VBox       auctionListContainer;
    @FXML private Button     gridViewBtn;
    @FXML private Button     listViewBtn;

    private final AuctionService auctionService = new AuctionService();
    private User currentUser;
    private List<Auction> allAuctions;

    @FXML
    public void initialize() {
        categoryFilter.setItems(FXCollections.observableArrayList(
                "All Categories", "Electronics", "Art", "Jewelry",
                "Vehicles", "Real Estate", "Fashion", "Collectibles"));
        categoryFilter.getSelectionModel().selectFirst();

        sortBox.setItems(FXCollections.observableArrayList(
                "Ending Soon", "Newest First", "Price: Low → High",
                "Price: High → Low", "Most Bids"));
        sortBox.getSelectionModel().selectFirst();
    }

    public void initData(User user) {
        this.currentUser = user;
        this.allAuctions = auctionService.getAllAuctions();
        renderAuctions(allAuctions);
    }

    /** Gọi từ MainController khi search từ topbar */
    public void applySearch(String keyword) {
        searchField.setText(keyword);
        handleSearch();
    }

    @FXML
    public void handleSearch() {
        applyFilters();
    }

    @FXML
    public void handleFilter() {
        applyFilters();
    }

    @FXML
    public void handleSort() {
        applyFilters();
    }

    @FXML
    public void clearFilters() {
        searchField.clear();
        categoryFilter.getSelectionModel().selectFirst();
        sortBox.getSelectionModel().selectFirst();
        renderAuctions(allAuctions);
    }

    @FXML public void switchToGrid() {
        // TODO: implement grid view
        gridViewBtn.getStyleClass().setAll("btn-primary");
        listViewBtn.getStyleClass().setAll("btn-secondary");
    }

    @FXML public void switchToList() {
        listViewBtn.getStyleClass().setAll("btn-primary");
        gridViewBtn.getStyleClass().setAll("btn-secondary");
    }

    /** Lọc và sắp xếp danh sách */
    private void applyFilters() {
        String kw  = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String cat = categoryFilter.getValue();
        String sort = sortBox.getValue();

        List<Auction> filtered = allAuctions.stream()
                .filter(a -> kw.isEmpty()
                        || a.getTitle().toLowerCase().contains(kw)
                        || a.getCategory().toLowerCase().contains(kw))
                .filter(a -> cat == null || cat.equals("All Categories")
                        || a.getCategory().equals(cat))
                .collect(Collectors.toList());

        // Sort
        if ("Price: Low → High".equals(sort)) {
            filtered.sort((a, b) -> Double.compare(a.getCurrentPrice(), b.getCurrentPrice()));
        } else if ("Price: High → Low".equals(sort)) {
            filtered.sort((a, b) -> Double.compare(b.getCurrentPrice(), a.getCurrentPrice()));
        } else if ("Most Bids".equals(sort)) {
            filtered.sort((a, b) -> b.getBidCount() - a.getBidCount());
        } else if ("Newest First".equals(sort)) {
            filtered.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        } else { // Ending Soon
            filtered.sort((a, b) -> Long.compare(a.getRemainingSeconds(), b.getRemainingSeconds()));
        }

        renderAuctions(filtered);
    }

    /** Render danh sách auction cards */
    private void renderAuctions(List<Auction> list) {
        auctionListContainer.getChildren().clear();
        resultCount.setText("Showing " + list.size() + " auction" + (list.size() != 1 ? "s" : ""));

        for (int i = 0; i < list.size(); i++) {
            Auction a = list.get(i);
            HBox card = buildCard(a);
            auctionListContainer.getChildren().add(card);

            // Staggered animation
            int delayMs = Math.min(i * 40, 300);
            javafx.animation.PauseTransition p =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(delayMs));
            p.setOnFinished(e -> AnimationUtil.slideUp(card, 14, 240));
            p.play();
        }

        if (list.isEmpty()) {
            Label empty = new Label("No auctions found. Try adjusting your filters.");
            empty.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:13px; -fx-padding:40 0;");
            auctionListContainer.getChildren().add(empty);
        }
    }

    /** Tạo auction card hoàn chỉnh */
    private HBox buildCard(Auction a) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("auction-card");
        card.setPadding(new Insets(14, 18, 14, 18));

        // ── Thumbnail ──
        Label thumb = new Label(a.getCategoryIcon());
        thumb.setStyle(
                "-fx-font-size:28px; -fx-alignment:CENTER;" +
                        "-fx-min-width:80px; -fx-max-width:80px;" +
                        "-fx-min-height:80px; -fx-max-height:80px;" +
                        "-fx-background-color:#f1f5f9; -fx-background-radius:10;");

        // ── Info ──
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Title + badge row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(a.getTitle());
        title.getStyleClass().add("auction-card-title");

        // Status badge
        Label statusBadge = new Label(a.getStatusLabel());
        statusBadge.getStyleClass().add(a.getStatusStyleClass());

        // Category badge
        Label catBadge = new Label(a.getCategory());
        catBadge.getStyleClass().addAll("badge", "badge-blue");

        titleRow.getChildren().addAll(title, statusBadge, catBadge);

        Label desc = new Label(a.getItem().getDescription());
        desc.getStyleClass().add("auction-card-sub");
        desc.setWrapText(false);

        Label meta = new Label("👤 " + a.getSeller().getUsername() + "   ·   🔨 " + a.getBidCount() + " bids");
        meta.getStyleClass().add("auction-card-sub");

        info.getChildren().addAll(titleRow, desc, meta);

        // ── Right: Price + Timer + Bid Btn ──
        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setMinWidth(160);

        Label priceLabel = new Label("CURRENT BID");
        priceLabel.getStyleClass().add("auction-card-price-label");

        Label price = new Label( String.format("%,.0f", a.getCurrentPrice()));
        price.getStyleClass().add("auction-card-price");

        Label timerLbl = new Label("⏱ " + a.getTimeRemaining());
        String timerStyle = a.getRemainingSeconds() < 3600
                ? (a.getRemainingSeconds() < 600 ? "timer-critical" : "timer-warning")
                : "timer-normal";
        timerLbl.getStyleClass().add(timerStyle);

        Button bidBtn = new Button("Place Bid →");
        bidBtn.getStyleClass().add("btn-primary");
        bidBtn.setOnAction(e -> openDetail(a));

        right.getChildren().addAll(priceLabel, price, timerLbl, bidBtn);

        card.getChildren().addAll(thumb, info, right);
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Button)) openDetail(a);
        });

        return card;
    }

    /** Mở chi tiết auction — load AuctionDetailController vào contentPane */
    private void openDetail(Auction auction) {
        MainController main = (MainController) auctionListContainer
                .getScene().lookup("#mainRoot").getUserData();
        main.loadContent(
                "/com/auction/client/fxml/auction_detail.fxml",
                (AuctionDetailController ctrl) -> ctrl.initData(currentUser, auction)
        );
    }
}