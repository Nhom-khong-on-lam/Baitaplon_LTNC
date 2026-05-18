package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
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
    private List<Auction> currentFilteredList = new ArrayList<>();
    private int visibleCount = 0;
    private final int LOAD_STEP = 10; // Số lượng hiển thị thêm mỗi lần cuộn
    private boolean isRendering = false;
    private int currentPage = 1;
    private final int PAGE_SIZE = 12; // Số lượng tải mỗi đợt từ Server
    private boolean isFull = false;   // Đã tải hết dữ liệu trên Server chưa
    private boolean isLoading = false;
    private javafx.animation.Timeline countdownTimeline;

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

        // Tạo bộ đếm chạy mỗi 1 giây để cập nhật toàn bộ Timer đang hiển thị
        countdownTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> updateAllTimers())
        );
        countdownTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    public void initData(User user) {
        this.currentUser = user;
        auctionListContainer.getChildren().clear();

        // ⚡ BƯỚC 1: ĐỔ NGAY DỮ LIỆU TỪ RAM CACHE RA MÀN HÌNH (0ms)
        // Lấy danh sách đã được gom ngầm từ Dashboard đem ra xài luôn
        List<Auction> cached = auctionService.getActiveAuctionsCached();
        if (cached != null && !cached.isEmpty()) {
            this.allAuctions = cached;
            setupScrollListener(); // Bật bộ bắt cuộn màn hình
            applyFilters();        // Lọc và vẽ lứa 10 sản phẩm đầu tiên ngay lập tức!
        } else {
            resultCount.setText("Loading auctions...");
        }

        // 🔄 BƯỚC 2: CHẠY LUỒNG NGẦM ĐỒNG BỘ DỮ LIỆU MỚI NHẤT TỪ SERVER (CHỐNG ĐƠ UI)
        new Thread(() -> {
            // Âm thầm gửi Socket lên Server đồng bộ dữ liệu mới về RAM Client
            auctionService.refreshActiveAuctionsFromServer();
            List<Auction> freshData = auctionService.getActiveAuctionsCached();

            javafx.application.Platform.runLater(() -> {
                // Chỉ render lại giao diện nếu dữ liệu thực tế trên Server có sự thay đổi
                if (freshData != null && (this.allAuctions == null || this.allAuctions.size() != freshData.size())) {
                    this.allAuctions = freshData;
                    setupScrollListener();
                    applyFilters(); // Cập nhật lại danh sách phòng live mới nhất lên màn hình
                }
            });
        }).start();
    }

    private void setupScrollListener() {
        // Tự động tìm ScrollPane bọc bên ngoài VBox auctionListContainer
        javafx.scene.Node node = auctionListContainer.getParent();
        while (node != null && !(node instanceof javafx.scene.control.ScrollPane)) {
            node = node.getParent();
        }

        if (node instanceof javafx.scene.control.ScrollPane) {
            javafx.scene.control.ScrollPane scrollPane = (javafx.scene.control.ScrollPane) node;
            // Bắt sự kiện khi thanh cuộn thay đổi
            scrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                // Nếu cuộn xuống đạt 90% trang -> Tự động tải thêm
                if (newVal.doubleValue() >= 0.90) {
                    loadMoreUI();
                }
            });
        }
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

        applyFilters();
    }

    @FXML public void switchToGrid() {
        gridViewBtn.getStyleClass().setAll("btn-primary");
        listViewBtn.getStyleClass().setAll("btn-secondary");

        // Đổi VBox thành FlowPane hoặc dùng GridPane để hiện 2 cột
        // Cách nhanh nhất: Chỉnh thuộc tính của auctionListContainer trong FXML
        // Hoặc tạm thời thông báo tính năng đang phát triển UI:
        System.out.println("Switching to Grid Layout...");
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
                .filter(a -> a.getRemainingSeconds() > 0)

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

        this.currentFilteredList = filtered;
        auctionListContainer.getChildren().clear();
        visibleCount = 0;
        resultCount.setText("Showing " + currentFilteredList.size() + " auction" + (currentFilteredList.size() != 1 ? "s" : ""));

        // Kiểm tra trống ngay tại đây thay vì để trong loadMoreUI
        if (currentFilteredList.isEmpty()) {
            Label empty = new Label("No auctions found. Try adjusting your filters.");
            empty.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:13px; -fx-padding:40 0;");
            auctionListContainer.getChildren().add(empty);
            return;
        }

        // Nếu có dữ liệu mới bắt đầu vẽ lứa đầu tiên
        loadMoreUI();
    }

    /** Lazy Rendering - Vẽ thêm thẻ auction card khi cuộn */
    private void loadMoreUI() {
        // 1. Chốt chặn bảo vệ: Nếu đang render hoặc đã vẽ hết danh sách thì dừng
        if (isRendering || currentFilteredList == null || visibleCount >= currentFilteredList.size()) {
            return;
        }

        isRendering = true; // Khóa đầu vào để tránh trùng lặp khi cuộn chuột nhanh

        try {
            int end = Math.min(visibleCount + LOAD_STEP, currentFilteredList.size());

            // Kiểm tra an toàn chỉ số index trước khi lặp
            if (visibleCount < 0) visibleCount = 0;

            for (int i = visibleCount; i < end; i++) {
                Auction a = currentFilteredList.get(i);
                HBox card = buildCard(a);

                // Gắn dữ liệu Auction vào card để hàm updateAllTimers có thể đọc được
                card.setUserData(a);

                auctionListContainer.getChildren().add(card);

                // Hiệu ứng trượt mượt mà khi xuất hiện
                int delayMs = Math.min((i - visibleCount) * 40, 300);
                javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(delayMs));
                p.setOnFinished(e -> AnimationUtil.slideUp(card, 14, 240));
                p.play();
            }

            visibleCount = end; // Cập nhật lại mốc sản phẩm đã vẽ thành công

        } catch (Exception e) {
            System.err.println("Lỗi render thẻ đấu giá: " + e.getMessage());
        } finally {
            // 🚀 CHÌA KHÓA VÀNG: Đảm bảo cờ hiệu luôn được trả về false NGAY LẬP TỨC
            // sau khi vòng lặp chạy xong, phá bỏ hoàn toàn việc bị kẹt luồng do hoán đổi trang!
            isRendering = false;
        }
    }

    /** Tạo auction card hoàn chỉnh */
    private HBox buildCard(Auction a) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("auction-card");
        card.setPadding(new Insets(14, 18, 14, 18));

        // ── Thumbnail ──
        Label thumb = new Label();
        String imgUrl = a.getItem() != null ? a.getItem().getImageUrl() : null;

        // Nếu sản phẩm có ảnh, dùng CSS để set ảnh làm background
        if (imgUrl != null && !imgUrl.isEmpty()) {
            thumb.setStyle(
                    "-fx-min-width:80px; -fx-max-width:80px;" +
                            "-fx-min-height:80px; -fx-max-height:80px;" +
                            "-fx-background-radius:10;" +
                            "-fx-background-image: url('" + imgUrl + "');" + // Gán URL ảnh
                            "-fx-background-size: cover;" +                  // Phủ kín vùng 80x80
                            "-fx-background-position: center;"
            );
        } else {
            // Nếu không có ảnh, dùng Emoji mặc định của danh mục
            thumb.setText(a.getCategoryIcon());
            thumb.setStyle(
                    "-fx-font-size:28px; -fx-alignment:CENTER;" +
                            "-fx-min-width:80px; -fx-max-width:80px;" +
                            "-fx-min-height:80px; -fx-max-height:80px;" +
                            "-fx-background-color:#f1f5f9; -fx-background-radius:10;");
        }

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
                "/com/auction/client/auction_detail.fxml",
                (AuctionDetailController ctrl) -> ctrl.initData(currentUser, auction)
        );
    }

    private void updateAllTimers() {
        // Duyệt qua tất cả các thẻ AuctionCard đang hiện trên màn hình
        for (javafx.scene.Node node : auctionListContainer.getChildren()) {
            if (node instanceof HBox card && card.getUserData() instanceof Auction a) {
                // Tìm Label chứa Timer trong VBox 'right' (vị trí cuối cùng của HBox)
                VBox right = (VBox) card.getChildren().get(card.getChildren().size() - 1);
                for (javafx.scene.Node n : right.getChildren()) {

                    if (n instanceof Label lbl) {
                        if (lbl.getStyleClass().contains("timer-normal") ||
                                lbl.getStyleClass().contains("timer-warning") ||
                                lbl.getStyleClass().contains("timer-critical")) {

                            lbl.setText("⏱ " + a.getTimeRemaining());

                            // Cập nhật lại màu sắc nếu thời gian sắp hết
                            lbl.getStyleClass().removeAll("timer-normal", "timer-warning", "timer-critical");
                            lbl.getStyleClass().add(a.getRemainingSeconds() < 3600
                                    ? (a.getRemainingSeconds() < 600 ? "timer-critical" : "timer-warning")
                                    : "timer-normal");
                        }
                    }
                }
            }
        }
    }
}
