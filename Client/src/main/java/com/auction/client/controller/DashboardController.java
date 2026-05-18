package com.auction.client.controller;


import com.auction.client.service.AuctionService;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import javafx.concurrent.Task;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * DashboardController — Màn hình tổng quan.
 * Hiển thị stat cards, live auctions, recent activity.
 */
public class DashboardController {

    @FXML private Label welcomeGreeting;
    @FXML private Label welcomeSub;
    @FXML private Label welcomeBadge;
    @FXML private Label welcomeDate;
    @FXML private Label statActiveAuctions;
    @FXML private Label statMyBids;
    @FXML private Label statWinning;
    @FXML private Label statMyProducts;
    @FXML private Label statAuctionDelta;
    @FXML private Label statBidDelta;

    @FXML private VBox liveAuctionList;
    @FXML private VBox activityList;

    private final AuctionService auctionService = new AuctionService();
    private User currentUser;

    /** Gọi từ MainController sau khi load FXML */
    public void initData(User user) {
        this.currentUser = user;

        // Welcome greeting (chạy trên UI thread — an toàn)
        String hour = java.time.LocalTime.now().getHour() < 12 ? "morning"
                : java.time.LocalTime.now().getHour() < 18 ? "afternoon" : "evening";
        welcomeGreeting.setText("Good " + hour + ", " + user.getUsername());
        welcomeDate.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMM dd")));

        // ⚡ BƯỚC 1: HIỂN THỊ NGAY TỨC THÌ DỮ LIỆU TỪ RAM CACHE (0ms - KHÔNG ĐỢI MẠNG)
        com.auction.common.model.DashboardData cachedData = auctionService.getDashboardDataCached();
        if (cachedData != null) {
            System.out.println("Dashboard bốc từ RAM Cache: Mở phát lên luôn!");
            renderDashboardUI(cachedData);
        } else {
            welcomeSub.setText("Loading dashboard data...");
        }

        // 🔄 BƯỚC 2: CHẠY NGẦM 1 REQUEST DUY NHẤT ĐỂ ĐỒNG BỘ SỐ LIỆU MỚI TỪ SERVER
        Task<com.auction.common.model.DashboardData> loadTask = new Task<>() {
            @Override
            protected com.auction.common.model.DashboardData call() throws Exception {
                // Gọi Server lấy cục dữ liệu Dashboard tổng hợp (Hàm này tự động ghi đè vào RAM Cache luôn)
                return auctionService.getDashboardData(user.getId());
            }

            @Override
            protected void succeeded() {
                com.auction.common.model.DashboardData freshData = getValue();
                if (freshData != null) {
                    // Đổ số liệu mới tinh từ Server lên giao diện
                    renderDashboardUI(freshData);
                }
            }

            @Override
            protected void failed() {
                System.err.println("Dashboard background refresh failed: " + getException().getMessage());
                if (auctionService.getDashboardDataCached() == null) {
                    welcomeSub.setText("Mất kết nối hoặc không thể đồng bộ dữ liệu Dashboard.");
                }
            }
        };

        // Kích hoạt tiến trình chạy ngầm
        new Thread(loadTask).start();

        // Vẫn giữ lại luồng ngầm này của bạn để gom sẵn củi đuốc cho trang Live Auctions nhé!
        new Thread(() -> {
            auctionService.refreshActiveAuctionsFromServer();
        }).start();
    }

    /**
     * 🛠️ HÀM PHỤ TÁCH RIÊNG LOGIC VẼ UI - Giúp tái sử dụng code cho cả dữ liệu Cache và Server
     */
    private void renderDashboardUI(com.auction.common.model.DashboardData data) {
        List<Auction> allAuctions = data.getAllAuctions() != null ? data.getAllAuctions() : java.util.Collections.emptyList();
        List<Auction> myBids      = data.getMyBids() != null ? data.getMyBids() : java.util.Collections.emptyList();
        List<Auction> myWinning   = data.getMyWinning() != null ? data.getMyWinning() : java.util.Collections.emptyList();
        List<Auction> myProducts  = data.getMyProducts() != null ? data.getMyProducts() : java.util.Collections.emptyList();
        List<Auction> live        = data.getLiveAuctions() != null ? data.getLiveAuctions() : java.util.Collections.emptyList();

        // Hiển thị số liệu kèm hiệu ứng countUp mượt mà
        AnimationUtil.countUp(statActiveAuctions, 0, allAuctions.size(), 400, "", "");
        AnimationUtil.countUp(statMyBids,         0, myBids.size(),       400, "", "");
        AnimationUtil.countUp(statWinning,        0, myWinning.size(),    400, "", "");
        AnimationUtil.countUp(statMyProducts,     0, myProducts.size(),   400, "", "");

        long previousCount = allAuctions.stream()
                .filter(a -> a.getEndTime().getMonthValue() == LocalDateTime.now().minusMonths(1).getMonthValue())
                .count();

        welcomeBadge.setText("LIVE NOW — " + live.size() + " Auctions");
        statAuctionDelta.setText("↑ " + (allAuctions.size() - previousCount));
        statBidDelta.setText("↑ " + myBids.size());
        welcomeSub.setText("There are " + live.size() + " live auctions right now. Good luck!");

        // Build các dòng live auction (Tối đa 5 phòng)
        liveAuctionList.getChildren().clear();
        List<Auction> preview = live.subList(0, Math.min(live.size(), 5));
        for (int i = 0; i < preview.size(); i++) {
            Auction a = preview.get(i);
            HBox row = buildAuctionRow(a);
            liveAuctionList.getChildren().add(row);

            final HBox finalRow = row;
            javafx.application.Platform.runLater(() -> AnimationUtil.slideUp(finalRow, 8, 150));
        }

        // Build danh sách hoạt động
        activityList.getChildren().clear();
        buildActivity(allAuctions);
    }

    /** Tạo 1 hàng auction nhỏ trong live section */
    private HBox buildAuctionRow(Auction a) {
        HBox row = new HBox(14);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("auction-card");
        row.setPadding(new Insets(10, 14, 10, 14));

        // Thumbnail placeholder
        Label thumb = new Label(a.getCategoryIcon());
        thumb.getStyleClass().add("auction-card-thumb");
        thumb.setStyle("-fx-font-size:22px; -fx-min-width:52px; -fx-max-width:52px;" +
                "-fx-min-height:52px; -fx-max-height:52px; -fx-alignment:CENTER;" +
                "-fx-background-color:#f1f5f9; -fx-background-radius:8;");

        // Info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(a.getTitle());
        title.getStyleClass().add("auction-card-title");
        Label sub = new Label(a.getCategory() + " · " + a.getBidCount() + " bids");
        sub.getStyleClass().add("auction-card-sub");
        info.getChildren().addAll(title, sub);

        // Right side
        VBox right = new VBox(4);
        right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label price = new Label(String.format("%,.0f", a.getCurrentPrice()));
        price.getStyleClass().add("auction-card-price");
        Label timerLbl = new Label(a.getTimeRemaining());
        timerLbl.getStyleClass().add(a.isEndingSoon() ? "timer-critical" : "timer-normal");
        right.getChildren().addAll(price, timerLbl);

        row.getChildren().addAll(thumb, info, right);

        // Click → navigate to auctions
        row.setOnMouseClicked(e -> navigateToAuctions());
        return row;
    }

    /** Xây dựng recent activity giả */
    // HÀM CHÍNH: Duyệt dữ liệu thật
    // Truyền danh sách vào qua tham số
    /** Lọc và dựng tối đa 5 hoạt động mới nhất để tránh quá tải Render giao diện */
    private void buildActivity(List<Auction> auctions) {
        activityList.getChildren().clear(); // Xóa sạch giao diện cũ

        if (auctions == null || auctions.isEmpty()) return;

        Long curId = currentUser.getId();
        int count = 0; // Biến đếm kiểm soát số lượng phần tử render

        for (Auction a : auctions) {
            if (count >= 5) break; // 🚀 ĐẠT GIỚI HẠN 5 HOẠT ĐỘNG THÌ DỪNG LẠI, KHÔNG RENDER RÁC!

            boolean isWinning = (a.getHighestBidder() != null && a.getHighestBidder().getId().equals(curId));
            boolean hasBid = a.getBidCountByUser(curId) > 0;
            boolean isAdded = false;

            if (hasBid) {
                if (isWinning) {
                    addActivityItem("🏆", "You're winning '" + a.getTitle() + "'", "Live", "#16a34a");
                } else {
                    addActivityItem("⚠️", "Outbid on '" + a.getTitle() + "'", "Action needed", "#dc2626");
                }
                isAdded = true;
            }

            // Nếu mình là người bán
            if (a.getSeller() != null && a.getSeller().getId().equals(curId)) {
                addActivityItem("📦", "Your auction '" + a.getTitle() + "' is " + a.getStatus(), "Owner", "#7c3aed");
                isAdded = true;
            }

            if (isAdded) {
                count++; // Chỉ tăng biến đếm khi có item thực sự được thêm vào giao diện
            }
        }
    }

    // HÀM PHỤ: Giữ lại logic tạo UI của bạn nhưng truyền tham số vào
    private void addActivityItem(String icon, String message, String time, String color) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label dot = new Label();
        dot.setStyle("-fx-background-color:" + color + "; -fx-background-radius:50; -fx-min-width:8px; -fx-max-width:8px; -fx-min-height:8px; -fx-max-height:8px;");

        VBox txt = new VBox(1);
        Label msgLabel = new Label(icon + " " + message);
        msgLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#374151; -fx-wrap-text:true;");

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#a0aec0;");

        txt.getChildren().addAll(msgLabel, timeLabel);
        HBox.setHgrow(txt, Priority.ALWAYS);

        row.getChildren().addAll(dot, txt);
        activityList.getChildren().add(row);
    }

    @FXML public void goToAuctions() {
        // Tìm MainController qua scene và trigger navAuctions
        getMainController().navAuctions();
    }

    @FXML public void goToCreate() {
        getMainController().navCreate();
    }

    private void navigateToAuctions() {
        getMainController().navAuctions();
    }

    /** Lấy MainController từ scene graph */
    private MainController getMainController() {
        return (MainController) liveAuctionList.getScene()
                .lookup("#mainRoot")
                .getUserData();
    }
}
