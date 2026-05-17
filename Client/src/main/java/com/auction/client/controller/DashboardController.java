package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * DashboardController — Màn hình tổng quan.
 *
 * FIX:
 *  - Dùng JavaFX Task chuẩn để tải network (succeeded/failed tự chạy trên UI thread)
 *  - Task thread đánh daemon(true)
 *  - Error handling khi load thất bại
 */
public class DashboardController {

    @FXML private Label welcomeGreeting, welcomeSub, welcomeBadge, welcomeDate;
    @FXML private Label statActiveAuctions, statMyBids, statWinning, statMyProducts;
    @FXML private Label statAuctionDelta, statBidDelta;
    @FXML private VBox  liveAuctionList, activityList;

    private final AuctionService auctionService = new AuctionService();
    private User currentUser;

    public void initData(User user) {
        this.currentUser = user;

        // Static UI — cập nhật ngay trên UI thread
        String hour = java.time.LocalTime.now().getHour() < 12 ? "morning"
                : java.time.LocalTime.now().getHour() < 18 ? "afternoon" : "evening";
        welcomeGreeting.setText("Good " + hour + ", " + user.getUsername() + " 👋");
        welcomeDate.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMM dd")));
        welcomeSub.setText("Loading your dashboard...");

        // JavaFX Task — network trên background, succeeded() tự chạy trên UI thread
        Task<DashboardData> loadTask = new Task<>() {
            @Override
            protected DashboardData call() {
                DashboardData data = new DashboardData();
                data.allAuctions = auctionService.getAllAuctions();
                data.myBids      = auctionService.getMyBids(user.getId());
                data.myWinning   = auctionService.getWinningBids(user.getId());
                data.myProducts  = auctionService.getAuctionsBySeller(user.getId());
                data.live        = auctionService.getActiveAuctions();
                return data;
            }

            @Override
            protected void succeeded() {
                DashboardData data = getValue();
                updateUI(data, user);
            }

            @Override
            protected void failed() {
                welcomeSub.setText("⚠ Could not load dashboard data. Please refresh.");
                System.err.println("Dashboard load failed: " + getException().getMessage());
                getException().printStackTrace();
            }
        };

        Thread t = new Thread(loadTask);
        t.setDaemon(true);
        t.start();
    }

    private void updateUI(DashboardData data, User user) {
        // Stat count-up
        AnimationUtil.countUp(statActiveAuctions, 0, data.allAuctions.size(), 800, "", "");
        AnimationUtil.countUp(statMyBids,         0, data.myBids.size(),      800, "", "");
        AnimationUtil.countUp(statWinning,        0, data.myWinning.size(),   800, "", "");
        AnimationUtil.countUp(statMyProducts,     0, data.myProducts.size(),  800, "", "");

        long previousCount = data.allAuctions.stream()
                .filter(a -> a.getEndTime() != null
                        && a.getEndTime().getMonthValue()
                        == LocalDateTime.now().minusMonths(1).getMonthValue())
                .count();

        welcomeBadge.setText("🟢  LIVE NOW — " + data.live.size() + " Auctions");
        statAuctionDelta.setText("↑ " + (data.allAuctions.size() - previousCount));
        statBidDelta.setText("↑ " + data.myBids.size());
        welcomeSub.setText("There are " + data.live.size()
                + " live auctions right now. Good luck!");

        // Live auction rows (top 5)
        liveAuctionList.getChildren().clear();
        List<Auction> preview = data.live.subList(0, Math.min(data.live.size(), 5));
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

        // Activity list
        activityList.getChildren().clear();
        buildActivity(data.allAuctions);
    }

    private HBox buildAuctionRow(Auction a) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("auction-card");
        row.setPadding(new Insets(10, 14, 10, 14));

        Label thumb = new Label(a.getCategoryIcon());
        thumb.setStyle("-fx-font-size:22px; -fx-min-width:52px; -fx-max-width:52px;"
                + "-fx-min-height:52px; -fx-max-height:52px; -fx-alignment:CENTER;"
                + "-fx-background-color:#f1f5f9; -fx-background-radius:8;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(a.getTitle());
        title.getStyleClass().add("auction-card-title");
        Label sub = new Label(a.getCategory() + " · " + a.getBidCount() + " bids");
        sub.getStyleClass().add("auction-card-sub");
        info.getChildren().addAll(title, sub);

        VBox right = new VBox(4);
        right.setAlignment(Pos.CENTER_RIGHT);
        Label price = new Label(String.format("%,.0f", a.getCurrentPrice()));
        price.getStyleClass().add("auction-card-price");
        Label timerLbl = new Label(a.getTimeRemaining());
        timerLbl.getStyleClass().add(a.isEndingSoon() ? "timer-critical" : "timer-normal");
        right.getChildren().addAll(price, timerLbl);

        row.getChildren().addAll(thumb, info, right);
        row.setOnMouseClicked(e -> navigateToAuctions());
        return row;
    }

    private void buildActivity(List<Auction> auctions) {
        activityList.getChildren().clear();
        Long curId = currentUser.getId();

        for (Auction a : auctions) {
            boolean isWinning = a.getHighestBidder() != null
                    && a.getHighestBidder().getId().equals(curId);
            boolean hasBid    = a.getBidCountByUser(curId) > 0;

            if (hasBid) {
                if (isWinning) {
                    addActivityItem("🏆", "You're winning '" + a.getTitle() + "'",
                            "Live", "#16a34a");
                } else {
                    addActivityItem("⚠️", "Outbid on '" + a.getTitle() + "'",
                            "Action needed", "#dc2626");
                }
            }

            if (a.getSeller().getId().equals(curId)) {
                addActivityItem("📦", "Your auction '" + a.getTitle()
                        + "' is " + a.getStatus(), "Owner", "#7c3aed");
            }
        }

        if (activityList.getChildren().isEmpty()) {
            Label empty = new Label("No recent activity yet.");
            empty.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:12px;");
            activityList.getChildren().add(empty);
        }
    }

    private void addActivityItem(String icon, String message,
                                 String time, String color) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label dot = new Label();
        dot.setStyle("-fx-background-color:" + color
                + "; -fx-background-radius:50;"
                + "-fx-min-width:8px; -fx-max-width:8px;"
                + "-fx-min-height:8px; -fx-max-height:8px;");

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

    @FXML public void goToAuctions() { getMainController().navAuctions(); }
    @FXML public void goToCreate()   { getMainController().navCreate(); }
    private void navigateToAuctions() { getMainController().navAuctions(); }

    private MainController getMainController() {
        return (MainController) liveAuctionList.getScene()
                .lookup("#mainRoot").getUserData();
    }

    /** Data container — tránh dùng nhiều biến instance trong Task */
    private static class DashboardData {
        List<Auction> allAuctions, myBids, myWinning, myProducts, live;
    }
}