package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.client.service.AuthService;
import com.auction.common.enums.AccountStatus;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AdminDashboardController — Tổng quan thống kê hệ thống.
 *
 * FIX:
 *  - Thread đánh daemon(true) — tránh block JVM shutdown
 *  - Error handling khi network thất bại
 *  - Category icon hiển thị từ getCategoryIcon() thay vì getCategory()
 */
public class AdminDashboardController {

    @FXML private Label welcomeLabel, dateLabel, systemStatusBadge;
    @FXML private Label statTotalUsers, statTotalAuctions,
            statLiveAuctions, statBanned;
    @FXML private Label statUserDelta;
    @FXML private VBox  recentUsersList, recentAuctionsList;

    private AuthService    authService    = new AuthService();
    private AuctionService auctionService = new AuctionService();

    public void initData(User admin) {
        // Header — cập nhật ngay trên UI thread
        welcomeLabel.setText("Welcome back, " + admin.getUsername());
        dateLabel.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
        if (systemStatusBadge != null) systemStatusBadge.setText("● Online");

        // Background thread — tải dữ liệu nặng
        Thread loader = new Thread(() -> {
            List<User>    allUsers;
            List<Auction> allAuctions;
            List<Auction> liveAuctions;

            try {
                allUsers     = authService.getAllUsers();
                allAuctions  = new ArrayList<>(auctionService.getAllAuctions());
                liveAuctions = new ArrayList<>(auctionService.getActiveAuctions());
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    welcomeLabel.setText("Welcome back, " + admin.getUsername()
                            + "  ⚠ Could not load stats");
                });
                return;
            }

            long bannedCount = allUsers.stream()
                    .filter(u -> u.getAccountStatus() == AccountStatus.BANNED)
                    .count();

            final List<User>    finalUsers     = allUsers;
            final List<Auction> finalAuctions  = allAuctions;
            final List<Auction> finalLive      = liveAuctions;
            final long          finalBanned    = bannedCount;

            javafx.application.Platform.runLater(() -> {
                // Count-up animations
                AnimationUtil.countUp(statTotalUsers,    0, finalUsers.size(),    800, "", "");
                AnimationUtil.countUp(statTotalAuctions, 0, finalAuctions.size(), 800, "", "");
                AnimationUtil.countUp(statLiveAuctions,  0, finalLive.size(),     800, "", "");
                AnimationUtil.countUp(statBanned,        0, finalBanned,          800, "", "");
                statUserDelta.setText("↑ " + finalUsers.size() + " total registered");

                // Recent users (last 5, mới nhất lên đầu)
                recentUsersList.getChildren().clear();
                int uStart = Math.max(0, finalUsers.size() - 5);
                List<User> recentUsers = finalUsers.subList(uStart, finalUsers.size());
                for (int i = recentUsers.size() - 1; i >= 0; i--) {
                    recentUsersList.getChildren().add(buildUserRow(recentUsers.get(i)));
                }

                // Recent auctions (last 5, mới nhất lên đầu)
                recentAuctionsList.getChildren().clear();
                int aStart = Math.max(0, finalAuctions.size() - 5);
                List<Auction> recentAuc = finalAuctions.subList(aStart, finalAuctions.size());
                for (int i = recentAuc.size() - 1; i >= 0; i--) {
                    recentAuctionsList.getChildren().add(buildAuctionRow(recentAuc.get(i)));
                }
            });
        });
        loader.setDaemon(true);
        loader.start();
    }

    private HBox buildUserRow(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:8;"
                + "-fx-border-color:#e2e8f0; -fx-border-radius:8; -fx-border-width:1;"
                + "-fx-cursor:hand;");

        String name = user.getUsername();
        String init = name.length() >= 2 ? name.substring(0, 2).toUpperCase()
                : name.toUpperCase();
        Label avatar = new Label(init);
        avatar.setStyle("-fx-background-color:#eff6ff; -fx-text-fill:#2563eb;"
                + "-fx-font-weight:bold; -fx-font-size:11px; -fx-alignment:CENTER;"
                + "-fx-background-radius:50; -fx-min-width:34px; -fx-max-width:34px;"
                + "-fx-min-height:34px; -fx-max-height:34px;");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLabel  = new Label(user.getUsername());
        nameLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#1a202c;");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#718096;");
        info.getChildren().addAll(nameLabel, emailLabel);

        boolean active = user.getAccountStatus() == AccountStatus.ACTIVE;
        Label badge = new Label(active ? "Active" : "Banned");
        badge.getStyleClass().add(active ? "pill-running" : "pill-ending");

        row.getChildren().addAll(avatar, info, badge);
        return row;
    }

    private HBox buildAuctionRow(Auction auction) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:8;"
                + "-fx-border-color:#e2e8f0; -fx-border-radius:8; -fx-border-width:1;"
                + "-fx-cursor:hand;");

        // FIX: dùng getCategoryIcon() thay vì getCategory() — tránh hiện tên thay vì icon
        Label icon = new Label(auction.getCategoryIcon());
        icon.setStyle("-fx-font-size:20px; -fx-min-width:36px;");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(auction.getItem().getName());
        title.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#1a202c;");
        Label seller = new Label("by " + auction.getSeller().getUsername()
                + "  ·  " + auction.getBidHistory().size() + " bids");
        seller.setStyle("-fx-font-size:11px; -fx-text-fill:#718096;");
        info.getChildren().addAll(title, seller);

        Label price = new Label(String.format("%,.0f", auction.getCurrentPrice()));
        price.setStyle("-fx-font-weight:bold; -fx-text-fill:#2563eb; -fx-font-size:13px;");

        row.getChildren().addAll(icon, info, price);
        return row;
    }

    @FXML public void goToUsers()    { getShell().navUsers(); }
    @FXML public void goToAuctions() { getShell().navAuctions(); }

    private AdminMainController getShell() {
        return (AdminMainController) recentUsersList
                .getScene().getRoot().getUserData();
    }

    // ── Setters (cho unit test) ───────────────────────────────
    public void setAuthService(AuthService s)       { this.authService = s; }
    public void setAuctionService(AuctionService s) { this.auctionService = s; }
    public void setWelcomeLabel(Label l)            { this.welcomeLabel = l; }
    public void setDateLabel(Label l)               { this.dateLabel = l; }
    public void setStatTotalUsers(Label l)          { this.statTotalUsers = l; }
    public void setStatTotalAuctions(Label l)       { this.statTotalAuctions = l; }
    public void setStatLiveAuctions(Label l)        { this.statLiveAuctions = l; }
    public void setStatBanned(Label l)              { this.statBanned = l; }
    public void setStatUserDelta(Label l)           { this.statUserDelta = l; }
    public void setRecentUsersList(VBox v)          { this.recentUsersList = v; }
    public void setRecentAuctionsList(VBox v)       { this.recentAuctionsList = v; }
    public VBox getRecentUsersList()                { return recentUsersList; }
    public VBox getRecentAuctionsList()             { return recentAuctionsList; }
}