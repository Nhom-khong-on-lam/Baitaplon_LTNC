package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static javafx.util.Duration.millis;

/**
 * AuctionsController — Danh sách live auctions.
 *
 * FIX:
 *  - categoryFilter dùng đúng 3 loại từ CreateAuctionController.CATEGORIES
 *  - filter dùng equalsIgnoreCase để tránh mismatch
 *  - Network call chạy trên background thread (đã có), giữ nguyên
 *  - Lazy rendering với đa luồng load đầu tiên
 */
public class AuctionsController {

    @FXML private TextField       searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> sortBox;
    @FXML private Label            resultCount;
    @FXML private VBox             auctionListContainer;
    @FXML private Button           gridViewBtn;
    @FXML private Button           listViewBtn;

    private final AuctionService auctionService = new AuctionService();
    private User          currentUser;
    private List<Auction> allAuctions          = new ArrayList<>();
    private List<Auction> currentFilteredList  = new ArrayList<>();
    private int     visibleCount  = 0;
    private boolean isRendering   = false;
    private static final int LOAD_STEP = 10;


    @FXML
    public void initialize() {
        // "All Categories" + 3 loại chuẩn từ CreateAuctionController
        List<String> cats = new ArrayList<>();
        cats.add("All Categories");
        cats.addAll(CreateAuctionController.CATEGORIES);
        categoryFilter.setItems(FXCollections.observableArrayList(cats));
        categoryFilter.getSelectionModel().selectFirst();

        sortBox.setItems(FXCollections.observableArrayList(
                "Ending Soon", "Newest First",
                "Price: Low → High", "Price: High → Low", "Most Bids"));
        sortBox.getSelectionModel().selectFirst();

    }

    public void initData(User user) {
        this.currentUser = user;
        resultCount.setText("Loading auctions...");
        auctionListContainer.getChildren().clear();

        // Network call trên background thread
        Thread loader = new Thread(() -> {
            List<Auction> fetched = auctionService.getAllAuctions();

            Platform.runLater(() -> {
                this.allAuctions = fetched != null ? fetched : new ArrayList<>();
                setupScrollListener();
                applyFilters();
            });
        });
        loader.setDaemon(true);
        loader.start();

    }

    private void setupScrollListener() {
        javafx.scene.Node node = auctionListContainer.getParent();
        while (node != null && !(node instanceof ScrollPane)) {
            node = node.getParent();
        }
        if (node instanceof ScrollPane sp) {
            sp.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 0.90) loadMoreUI();
            });
        }
    }

    /** Gọi từ MainController khi search từ topbar */
    public void applySearch(String keyword) {
        searchField.setText(keyword);
        applyFilters();
    }

    @FXML public void handleSearch() { applyFilters(); }
    @FXML public void handleFilter() { applyFilters(); }
    @FXML public void handleSort()   { applyFilters(); }

    @FXML
    public void clearFilters() {
        searchField.clear();
        categoryFilter.getSelectionModel().selectFirst();
        sortBox.getSelectionModel().selectFirst();
        applyFilters();
    }

    @FXML public void switchToGrid() {
        gridViewBtn.getStyleClass().setAll("btn-primary");
        listViewBtn.getStyleClass().setAll("btn-secondary");
    }

    @FXML public void switchToList() {
        listViewBtn.getStyleClass().setAll("btn-primary");
        gridViewBtn.getStyleClass().setAll("btn-secondary");
    }

    private void applyFilters() {
        String kw   = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        String cat  = categoryFilter.getValue();
        String sort = sortBox.getValue();

        List<Auction> filtered = allAuctions.stream()
                .filter(a -> a.getRemainingSeconds() > 0)
                .filter(a -> kw.isEmpty()
                        || a.getTitle().toLowerCase().contains(kw)
                        || a.getCategory().toLowerCase().contains(kw))
                // FIX: dùng equalsIgnoreCase để tránh mismatch
                .filter(a -> cat == null || "All Categories".equals(cat)
                        || a.getCategory().equalsIgnoreCase(cat))
                .collect(Collectors.toList());

        // Sort
        switch (sort == null ? "" : sort) {
            case "Price: Low → High"  ->
                    filtered.sort((a, b) -> Double.compare(a.getCurrentPrice(), b.getCurrentPrice()));
            case "Price: High → Low"  ->
                    filtered.sort((a, b) -> Double.compare(b.getCurrentPrice(), a.getCurrentPrice()));
            case "Most Bids"          ->
                    filtered.sort((a, b) -> b.getBidCount() - a.getBidCount());
            case "Newest First"       ->
                    filtered.sort((a, b) -> Long.compare(b.getId(), a.getId()));
            default                   -> // Ending Soon
                    filtered.sort((a, b) -> Long.compare(a.getRemainingSeconds(), b.getRemainingSeconds()));
        }

        this.currentFilteredList = filtered;
        auctionListContainer.getChildren().clear();
        visibleCount = 0;

        resultCount.setText("Showing " + filtered.size()
                + " auction" + (filtered.size() != 1 ? "s" : ""));

        if (filtered.isEmpty()) {
            Label empty = new Label("No auctions found. Try adjusting your filters.");
            empty.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:13px; -fx-padding:40 0;");
            auctionListContainer.getChildren().add(empty);
            return;
        }

        loadMoreUI();
    }

    /** Lazy Rendering — vẽ thêm cards khi cuộn */
    private void loadMoreUI() {
        if (isRendering || visibleCount >= currentFilteredList.size()) return;
        isRendering = true;

        int end = Math.min(visibleCount + LOAD_STEP, currentFilteredList.size());
        List<Auction> batch = currentFilteredList.subList(visibleCount, end);

        for (int i = 0; i < batch.size(); i++) {
            Auction a = batch.get(i);
            HBox card = buildCard(a);
            auctionListContainer.getChildren().add(card);
            int delayMs = Math.min(i * 40, 300);
            javafx.animation.PauseTransition p =
                    new javafx.animation.PauseTransition(millis(delayMs));
            p.setOnFinished(e -> AnimationUtil.slideUp(card, 14, 240));
            p.play();
        }

        visibleCount = end;

        // Cooldown tránh spam scroll
        PauseTransition cooldown =
                new PauseTransition(millis(250));
        cooldown.setOnFinished(e -> isRendering = false);
        cooldown.play();
    }

    private HBox buildCard(Auction a) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("auction-card");
        card.setPadding(new Insets(14, 18, 14, 18));

        // Thumbnail
        Label thumb = new Label(a.getCategoryIcon());
        thumb.setStyle("-fx-font-size:28px; -fx-alignment:CENTER;"
                + "-fx-min-width:80px; -fx-max-width:80px;"
                + "-fx-min-height:80px; -fx-max-height:80px;"
                + "-fx-background-color:#f1f5f9; -fx-background-radius:10;");

        // Info
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(a.getTitle());
        title.getStyleClass().add("auction-card-title");

        Label statusBadge = new Label(a.getStatusLabel());
        statusBadge.getStyleClass().add(a.getStatusStyleClass());

        Label catBadge = new Label(a.getCategory());
        catBadge.getStyleClass().addAll("badge", "badge-blue");

        titleRow.getChildren().addAll(title, statusBadge, catBadge);

        Label desc = new Label(a.getItem().getDescription());
        desc.getStyleClass().add("auction-card-sub");
        desc.setWrapText(false);

        Label meta = new Label("👤 " + a.getSeller().getUsername()
                + "   ·   🔨 " + a.getBidCount() + " bids");
        meta.getStyleClass().add("auction-card-sub");

        info.getChildren().addAll(titleRow, desc, meta);

        // Right: Price + Timer + Button
        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setMinWidth(160);

        Label priceLabel = new Label("CURRENT BID");
        priceLabel.getStyleClass().add("auction-card-price-label");

        Label price = new Label(String.format("%,.0f", a.getCurrentPrice()));
        price.getStyleClass().add("auction-card-price");

        Label timerLbl = new Label("⏱ " + a.getTimeRemaining());
        String timerStyle = a.getRemainingSeconds() < 3600
                ? (a.getRemainingSeconds() < 600 ? "timer-critical" : "timer-warning")
                : "timer-normal";
        timerLbl.getStyleClass().add(timerStyle);

        Button bidBtn = new Button("Place Bid");
        bidBtn.getStyleClass().add("btn-primary");
        bidBtn.setOnAction(e -> {
            Auction freshAuction = currentFilteredList.stream()
                    .filter(auc -> auc.getId() == a.getId())
                    .findFirst()
                    .orElse(a);
            openDetail(freshAuction);
        });
        right.getChildren().addAll(priceLabel, price, timerLbl, bidBtn);

        card.getChildren().addAll(thumb, info, right);
        card.setOnMouseClicked(e -> {
            if (!(e.getTarget() instanceof Button)) {
                Auction freshAuction = currentFilteredList.stream()
                        .filter(auc -> auc.getId() == a.getId())
                        .findFirst()
                        .orElse(a);
                openDetail(freshAuction);
            }
        });
        return card;
    }

    private void openDetail(Auction auction) {
        try {
            javafx.scene.Node mainRootNode = auctionListContainer.getScene().lookup("#mainRoot");
            MainController main = (MainController) mainRootNode.getUserData();

            // Ép JavaFX lấy đúng file fxml theo biến BASE gốc của hệ thống
            main.loadContent( "/com/auction/client/auction_detail.fxml",
                    (AuctionDetailController ctrl) -> ctrl.initData(currentUser, auction));
        } catch (Exception e) {
            System.out.println("Lỗi mở chi tiết ở AuctionsController: " + e.getMessage());
            e.printStackTrace();
        }
    }
}