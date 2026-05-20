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
    @FXML private Button     tabAll;
    @FXML private Button     tabLive;
    @FXML private Button     tabEnded;

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
    private int refreshTick = 0;
    /** "ALL" | "LIVE" | "ENDED" */
    private String currentTab = "ALL";

    @FXML
    public void initialize() {
        categoryFilter.setItems(FXCollections.observableArrayList("All Categories", "Electronics", "Art", "Vehicles"));
        categoryFilter.getSelectionModel().selectFirst();

        sortBox.setItems(FXCollections.observableArrayList(
                "Newest First", "Price: Low → High",
                "Price: High → Low", "Most Bids"));
        // Không chọn mặc định — hiển thị placeholder "Sort By"

        // Tạo bộ đếm chạy mỗi 1 giây để cập nhật toàn bộ Timer đang hiển thị
        countdownTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    updateAllTimers();
                    refreshTick++;
                    if (refreshTick % 5 == 0) {
                        pollLatestActiveAuctions();
                    }
                })
        );
        countdownTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        countdownTimeline.play();

        // Tự động dọn dẹp khi chuyển màn hình
        auctionListContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                if (countdownTimeline != null) {
                    countdownTimeline.stop();
                    System.out.println("Cleaned up countdownTimeline in AuctionsController.");
                }
            }
        });
    }

    public void initData(User user) {
        this.currentUser = user;
        if (auctionListContainer != null) {
            auctionListContainer.getChildren().clear();
        }

        // ⚡ BƯỚC 1: ĐỔ NGAY DỮ LIỆU TỪ RAM CACHE RA MÀN HÌNH (0ms)
        List<Auction> cached = auctionService.getActiveAuctionsCached();
        if (cached != null && !cached.isEmpty()) {
            this.allAuctions = new ArrayList<>(cached);
            setupScrollListener();
            applyFilters();
        } else {
            resultCount.setText("Loading auctions from server...");
        }

        // 🔄 BƯỚC 2: TÁCH BIỆT HOÀN TOÀN LUỒNG LẤY MẠNG VÀ RENDER
        new Thread(() -> {
            try {
                // Lấy dữ liệu ngầm từ Server qua Socket
                auctionService.refreshActiveAuctionsFromServer();
                List<Auction> freshData = auctionService.getActiveAuctionsCached();

                if (freshData != null) {
                    // Đẩy việc xử lý giao diện về luồng UI một cách an toàn
                    javafx.application.Platform.runLater(() -> {
                        this.allAuctions = new ArrayList<>(freshData);
                        setupScrollListener();

                        // Xóa bớt các animation nặng nề nếu danh sách quá dài để tránh đơ
                        applyFilters();
                    });
                }
            } catch (Exception e) {
                System.err.println("Lỗi nạp dữ liệu ngầm đấu giá: " + e.getMessage());
            }
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
        sortBox.getSelectionModel().clearSelection();
        applyFilters();
    }

    // ── Tab handlers ─────────────────────────────────────────
    @FXML public void switchTabAll()   { setActiveTab("ALL");   }
    @FXML public void switchTabLive()  { setActiveTab("LIVE");  }
    @FXML public void switchTabEnded() { setActiveTab("ENDED"); }

    private void setActiveTab(String tab) {
        currentTab = tab;
        // Cập nhật style nút tab
        tabAll.getStyleClass().removeAll("tab-btn-active");
        tabLive.getStyleClass().removeAll("tab-btn-active");
        tabEnded.getStyleClass().removeAll("tab-btn-active");
        switch (tab) {
            case "ALL"   -> tabAll.getStyleClass().add("tab-btn-active");
            case "LIVE"  -> tabLive.getStyleClass().add("tab-btn-active");
            case "ENDED" -> tabEnded.getStyleClass().add("tab-btn-active");
        }
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
        String kw   = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String cat  = categoryFilter.getValue();
        String sort = sortBox.getValue();

        List<Auction> filtered = allAuctions.stream()
                // ── Lọc theo Tab ─────────────────────────────────────
                .filter(a -> switch (currentTab) {
                    case "LIVE"  -> a.getRemainingSeconds() > 0;
                    case "ENDED" -> a.getRemainingSeconds() <= 0;
                    default      -> true; // ALL
                })
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
        }
        // sort == null (chưa chọn): giữ nguyên thứ tự từ server

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
    /** Lazy Rendering - Vẽ thêm thẻ auction card khi cuộn */
    private void loadMoreUI() {
        if (isRendering || currentFilteredList == null || visibleCount >= currentFilteredList.size()) {
            return;
        }

        isRendering = true;

        try {
            int end = Math.min(visibleCount + LOAD_STEP, currentFilteredList.size());

            if (visibleCount < 0) visibleCount = 0;

            for (int i = visibleCount; i < end; i++) {
                Auction a = currentFilteredList.get(i);
                HBox card = buildCard(a);

                // Gắn dữ liệu Auction vào card
                card.setUserData(a);

                // 🚀 SỬA TẠI ĐÂY: Chỉ giữ lại DUY NHẤT 1 dòng add này
                auctionListContainer.getChildren().add(card);

                // ĐÃ XÓA dòng add thứ hai bị trùng lặp ở đây!
            }

            visibleCount = end;

        } catch (Exception e) {
            System.err.println("Lỗi render thẻ đấu giá: " + e.getMessage());
        } finally {
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

        boolean ended = a.getRemainingSeconds() <= 0;

        Label priceLabel = new Label(ended ? "FINAL PRICE" : "CURRENT BID");
        priceLabel.getStyleClass().add("auction-card-price-label");

        Label price = new Label(String.format("%,.0f", a.getCurrentPrice()));
        price.getStyleClass().add(ended ? "auction-card-price-ended" : "auction-card-price");

        Label timerLbl = new Label();
        if (ended) {
            timerLbl.setText("🏁 Ended");
            timerLbl.getStyleClass().add("timer-ended");
        } else {
            timerLbl.setText("⏱ " + a.getTimeRemaining());
            String timerStyle = a.getRemainingSeconds() < 3600
                    ? (a.getRemainingSeconds() < 600 ? "timer-critical" : "timer-warning")
                    : "timer-normal";
            timerLbl.getStyleClass().add(timerStyle);
        }

        Button bidBtn = new Button(ended ? "View Results" : "Place Bid →");
        bidBtn.getStyleClass().add(ended ? "btn-secondary" : "btn-primary");
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
                // Skip các phiên đã ended — không cần đếm ngược
                if (a.getRemainingSeconds() <= 0) continue;

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

    private void pollLatestActiveAuctions() {
        new Thread(() -> {
            try {
                auctionService.refreshActiveAuctionsFromServer();
                List<Auction> freshData = auctionService.getActiveAuctionsCached();
                if (freshData != null) {
                    javafx.application.Platform.runLater(() -> {
                        updateUIInPlace(freshData);
                    });
                }
            } catch (Exception e) {
                System.err.println("Error polling active auctions: " + e.getMessage());
            }
        }).start();
    }

    private void updateUIInPlace(List<Auction> freshData) {
        if (freshData == null || allAuctions == null) return;

        java.util.Map<Long, Auction> freshMap = freshData.stream()
                .collect(Collectors.toMap(Auction::getId, a -> a));

        for (int i = 0; i < allAuctions.size(); i++) {
            Auction oldAuc = allAuctions.get(i);
            Auction newAuc = freshMap.get(oldAuc.getId());
            if (newAuc != null) {
                allAuctions.set(i, newAuc);
            }
        }

        for (int i = 0; i < currentFilteredList.size(); i++) {
            Auction oldAuc = currentFilteredList.get(i);
            Auction newAuc = freshMap.get(oldAuc.getId());
            if (newAuc != null) {
                currentFilteredList.set(i, newAuc);
            }
        }

        for (javafx.scene.Node node : auctionListContainer.getChildren()) {
            if (node instanceof HBox card && card.getUserData() instanceof Auction a) {
                Auction newAuc = freshMap.get(a.getId());
                if (newAuc != null) {
                    card.setUserData(newAuc);

                    try {
                        // 1. Update status badge & bid count in info VBox
                        if (card.getChildren().size() > 1 && card.getChildren().get(1) instanceof VBox info) {
                            if (info.getChildren().size() > 0 && info.getChildren().get(0) instanceof HBox titleRow) {
                                if (titleRow.getChildren().size() > 1 && titleRow.getChildren().get(1) instanceof Label statusBadge) {
                                    statusBadge.setText(newAuc.getStatusLabel());
                                    statusBadge.getStyleClass().removeAll("badge-green", "badge-gray", "badge-orange", "badge-red");
                                    statusBadge.getStyleClass().add(newAuc.getStatusStyleClass());
                                }
                            }
                            if (info.getChildren().size() > 2 && info.getChildren().get(2) instanceof Label meta) {
                                meta.setText("👤 " + newAuc.getSeller().getUsername() + "   ·   🔨 " + newAuc.getBidCount() + " bids");
                            }
                        }

                        // 2. Update price and button in right VBox
                        if (card.getChildren().size() > 2 && card.getChildren().get(2) instanceof VBox right) {
                            boolean ended = newAuc.getRemainingSeconds() <= 0;

                            if (right.getChildren().size() > 0 && right.getChildren().get(0) instanceof Label priceLabel) {
                                priceLabel.setText(ended ? "FINAL PRICE" : "CURRENT BID");
                            }

                            if (right.getChildren().size() > 1 && right.getChildren().get(1) instanceof Label price) {
                                price.setText(String.format("%,.0f", newAuc.getCurrentPrice()));
                                price.getStyleClass().removeAll("auction-card-price", "auction-card-price-ended");
                                price.getStyleClass().add(ended ? "auction-card-price-ended" : "auction-card-price");
                            }

                            if (right.getChildren().size() > 2 && right.getChildren().get(2) instanceof Label timerLbl) {
                                if (ended) {
                                    timerLbl.setText("🏁 Ended");
                                    timerLbl.getStyleClass().removeAll("timer-normal", "timer-warning", "timer-critical", "timer-ended");
                                    timerLbl.getStyleClass().add("timer-ended");
                                } else {
                                    timerLbl.setText("⏱ " + newAuc.getTimeRemaining());
                                    String timerStyle = newAuc.getRemainingSeconds() < 3600
                                            ? (newAuc.getRemainingSeconds() < 600 ? "timer-critical" : "timer-warning")
                                            : "timer-normal";
                                    timerLbl.getStyleClass().removeAll("timer-normal", "timer-warning", "timer-critical", "timer-ended");
                                    timerLbl.getStyleClass().add(timerStyle);
                                }
                            }

                            if (right.getChildren().size() > 3 && right.getChildren().get(3) instanceof Button bidBtn) {
                                bidBtn.setText(ended ? "View Results" : "Place Bid →");
                                bidBtn.getStyleClass().removeAll("btn-primary", "btn-secondary");
                                bidBtn.getStyleClass().add(ended ? "btn-secondary" : "btn-primary");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error updating card in-place: " + e.getMessage());
                    }
                }
            }
        }
    }
}
