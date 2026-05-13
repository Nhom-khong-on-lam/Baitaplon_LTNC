package com.auction.client.controller;

import com.auction.client.Enum.AccountStatus;
import com.auction.client.model.Auction;
import com.auction.client.model.User;
import com.auction.client.service.AuctionService;
import com.auction.client.service.AuthService;
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
 */
public class AdminDashboardController {

    @FXML private Label welcomeLabel, dateLabel, systemStatusBadge;
    @FXML private Label statTotalUsers, statTotalAuctions,
            statLiveAuctions, statBanned;
    @FXML private Label statUserDelta;
    @FXML private VBox  recentUsersList, recentAuctionsList;

    private AuthService authService    = new AuthService();
    private AuctionService auctionService = new AuctionService();

    public void initData(User admin) {
        // Header
        welcomeLabel.setText("Welcome back, " + admin.getUsername() );
        dateLabel.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));

        // Load data
        List<User>    allUsers    = authService.getAllUsers();
        List<Auction> allAuctions = new ArrayList<>(auctionService.getAllAuctions());
        List<Auction> liveAuctions= new ArrayList<>(auctionService.getActiveAuctions());
        long bannedCount = allUsers.stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.BANNED)
                .count();

        // Count-up animations
        AnimationUtil.countUp(statTotalUsers,    0, allUsers.size(),     800, "", "");
        AnimationUtil.countUp(statTotalAuctions, 0, allAuctions.size(),  800, "", "");
        AnimationUtil.countUp(statLiveAuctions,  0, liveAuctions.size(), 800, "", "");
        AnimationUtil.countUp(statBanned,        0, bannedCount,         800, "", "");

        statUserDelta.setText("↑ " + allUsers.size() + " total registered");

        // Recent users (last 5)
        recentUsersList.getChildren().clear();
        List<User> recent = allUsers.subList(
                Math.max(0, allUsers.size() - 5), allUsers.size());
        for (int i = recent.size() - 1; i >= 0; i--) {
            recentUsersList.getChildren().add(buildUserRow(recent.get(i)));
        }

        // Recent auctions (last 5)
        recentAuctionsList.getChildren().clear();
        List<Auction> recentAuc = allAuctions.subList(
                Math.max(0, allAuctions.size() - 5), allAuctions.size());
        for (int i = recentAuc.size() - 1; i >= 0; i--) {
            recentAuctionsList.getChildren().add(buildAuctionRow(recentAuc.get(i)));
        }
    }

    private HBox buildUserRow(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:8;" +
                "-fx-border-color:#e2e8f0; -fx-border-radius:8; -fx-border-width:1;" +
                "-fx-cursor:hand;");

        // Avatar
        String name = user.getUsername();
        String init = name.length() >= 2 ? name.substring(0, 2).toUpperCase()
                : name.toUpperCase();
        Label avatar = new Label(init);
        avatar.setStyle("-fx-background-color:#eff6ff; -fx-text-fill:#2563eb;" +
                "-fx-font-weight:bold; -fx-font-size:11px; -fx-alignment:CENTER;" +
                "-fx-background-radius:50; -fx-min-width:34px; -fx-max-width:34px;" +
                "-fx-min-height:34px; -fx-max-height:34px;");

        // Info
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLabel = new Label(user.getUsername());
        nameLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#1a202c;");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#718096;");
        info.getChildren().addAll(nameLabel, emailLabel);

        // Status badge
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
        row.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:8;" +
                "-fx-border-color:#e2e8f0; -fx-border-radius:8; -fx-border-width:1;" +
                "-fx-cursor:hand;");

        // Icon
        Label icon = new Label(auction.getItem().getCategory());
        icon.setStyle("-fx-font-size:20px; -fx-min-width:36px;");

        // Info
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label title = new Label(auction.getItem().getName());
        title.setStyle("-fx-font-weight:bold; -fx-font-size:13px; -fx-text-fill:#1a202c;");
        Label seller = new Label("by " + auction.getSeller().getUsername()
                + "  ·  " + auction.getBidHistory().size() + " bids");
        seller.setStyle("-fx-font-size:11px; -fx-text-fill:#718096;");
        info.getChildren().addAll(title, seller);

        // Price
        Label price = new Label( String.format("%,.0f", auction.getCurrentPrice()));
        price.setStyle("-fx-font-weight:bold; -fx-text-fill:#2563eb; -fx-font-size:13px;");

        row.getChildren().addAll(icon, info, price);
        return row;
    }

    @FXML public void goToUsers() {
        getShell().navUsers();
    }

    @FXML public void goToAuctions() {
        getShell().navAuctions();
    }

    private AdminMainController getShell() {
        return (AdminMainController) recentUsersList
                .getScene().getRoot().getUserData();
    }
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    public void setAuctionService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }
    public void setWelcomeLabel(Label welcomeLabel) { this.welcomeLabel = welcomeLabel; }
    public void setDateLabel(Label dateLabel) { this.dateLabel = dateLabel; }
    public void setStatTotalUsers(Label statTotalUsers) { this.statTotalUsers = statTotalUsers; }
    public void setStatTotalAuctions(Label statTotalAuctions) { this.statTotalAuctions = statTotalAuctions; }
    public void setStatLiveAuctions(Label statLiveAuctions) { this.statLiveAuctions = statLiveAuctions; }
    public void setStatBanned(Label statBanned) { this.statBanned = statBanned; }
    public void setStatUserDelta(Label statUserDelta) { this.statUserDelta = statUserDelta; }
    public void setRecentUsersList(VBox recentUsersList) { this.recentUsersList = recentUsersList; }
    public void setRecentAuctionsList(VBox recentAuctionsList) { this.recentAuctionsList = recentAuctionsList; }


    public VBox getRecentUsersList() {
        return recentUsersList;
    }

    public VBox getRecentAuctionsList() {
        return recentAuctionsList;
    }
}