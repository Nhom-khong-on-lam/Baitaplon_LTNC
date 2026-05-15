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
        welcomeGreeting.setText("Good " + hour + ", " + user.getUsername() + " 👋");
        welcomeDate.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMM dd")));

        // Chạy tất cả network call trên background thread để tránh đơ UI
        Task<Void> loadTask = new Task<>() {
            List<Auction> allAuctions, myBids, myWinning, myProducts, live;

            @Override
            protected Void call() {
                allAuctions = auctionService.getAllAuctions();
                myBids      = auctionService.getMyBids(user.getId());
                myWinning   = auctionService.getWinningBids(user.getId());
                myProducts  = auctionService.getAuctionsBySeller(user.getId());
                live        = auctionService.getActiveAuctions();
                return null;
            }

            @Override
            protected void succeeded() {
                // Cập nhật UI — phải chạy trên JavaFX thread (succeeded() tự động chạy trên UI thread)
                AnimationUtil.countUp(statActiveAuctions, 0, allAuctions.size(), 800, "", "");
                AnimationUtil.countUp(statMyBids,         0, myBids.size(),       800, "", "");
                AnimationUtil.countUp(statWinning,        0, myWinning.size(),    800, "", "");
                AnimationUtil.countUp(statMyProducts,     0, myProducts.size(),   800, "", "");

                long previousCount = allAuctions.stream()
                        .filter(a -> a.getEndTime().getMonthValue() == LocalDateTime.now().minusMonths(1).getMonthValue())
                        .count();

                welcomeBadge.setText("🟢  LIVE NOW — " + live.size() + " Auctions");
                statAuctionDelta.setText("↑ " + (allAuctions.size() - previousCount));
                statBidDelta.setText("↑ 1");
                welcomeSub.setText("There are " + live.size() + " live auctions right now. Good luck!");

                // Build live auction rows
                liveAuctionList.getChildren().clear();
                List<Auction> preview = live.subList(0, Math.min(live.size(), 5));
                for (int i = 0; i < preview.size(); i++) {
                    Auction a = preview.get(i);
                    HBox row = buildAuctionRow(a);
                    liveAuctionList.getChildren().add(row);
                    int delay = i * 60;
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(delay));
                    pause.setOnFinished(e -> AnimationUtil.slideUp(row, 12, 220));
                    pause.play();
                }

                // Build activity list
                activityList.getChildren().clear();
                buildActivity(allAuctions);
            }

            @Override
            protected void failed() {
                System.err.println("Dashboard load failed: " + getException().getMessage());
            }
        };

        new Thread(loadTask).start();
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
        Label price = new Label("$" + String.format("%,.0f", a.getCurrentPrice()));
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
    private void buildActivity(List<Auction> auctions) {
        activityList.getChildren().clear(); // Xóa sạch giao diện cũ

        Long curId = currentUser.getId();

        // XÓA DÒNG NÀY ĐỂ TRÁNH GỌI MẠNG LÀM ĐƠ UI:
        // List<Auction> auctions = auctionService.getAllAuctions();

        for (Auction a : auctions) {
            // Kiểm tra nếu mình là người đặt giá cao nhất
            boolean isWinning = (a.getHighestBidder() != null && a.getHighestBidder().getId().equals(curId));
            // Kiểm tra nếu mình có tham gia đấu giá phiên này
            boolean hasBid = a.getBidCountByUser(curId) > 0;

            if (hasBid) {
                if (isWinning) {
                    addActivityItem("🏆", "You're winning '" + a.getTitle() + "'", "Live", "#16a34a");
                } else {
                    addActivityItem("⚠️", "Outbid on '" + a.getTitle() + "'", "Action needed", "#dc2626");
                }
            }

            // Nếu mình là người bán
            if (a.getSeller().getId().equals(curId)) {
                addActivityItem("📦", "Your auction '" + a.getTitle() + "' is " + a.getStatus(), "Owner", "#7c3aed");
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
