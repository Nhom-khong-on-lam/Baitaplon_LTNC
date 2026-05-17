package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MyBidsController — Lịch sử và trạng thái đấu giá của user.
 *
 * FIX:
 *  - Thread daemon(true) trên loader
 *  - Error handling khi load thất bại
 */
public class MyBidsController {

    @FXML private Button tabAll, tabWinning, tabOutbid, tabEnded;
    @FXML private Label  bidCount;
    @FXML private VBox   bidListContainer;

    private final AuctionService auctionService = new AuctionService();
    private User          currentUser;
    private List<Auction> myBids;

    public void initData(User user) {
        this.currentUser = user;
        bidCount.setText("Loading...");

        Thread loader = new Thread(() -> {
            List<Auction> fetched;
            try {
                fetched = auctionService.getMyBids(user.getId());
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        bidCount.setText("Failed to load"));
                return;
            }

            javafx.application.Platform.runLater(() -> {
                this.myBids = fetched != null ? fetched
                        : new java.util.ArrayList<>();
                bidCount.setText(myBids.size() + " bids total");
                setActiveTab(tabAll);
                renderBids(myBids);
            });
        });
        loader.setDaemon(true);
        loader.start();
    }

    @FXML public void showAll() {
        setActiveTab(tabAll);
        renderBids(myBids);
    }

    @FXML public void showWinning() {
        setActiveTab(tabWinning);
        Long curId = currentUser.getId();
        renderBids(myBids.stream()
                .filter(a -> a.getHighestBidder() != null
                        && a.getHighestBidder().getId().equals(curId))
                .collect(Collectors.toList()));
    }

    @FXML public void showOutbid() {
        setActiveTab(tabOutbid);
        Long curId = currentUser.getId();
        renderBids(myBids.stream()
                .filter(a -> a.isLive()
                        && (a.getHighestBidder() == null
                        || !a.getHighestBidder().getId().equals(curId)))
                .collect(Collectors.toList()));
    }

    @FXML public void showEnded() {
        setActiveTab(tabEnded);
        renderBids(myBids.stream()
                .filter(a -> !a.isLive())
                .collect(Collectors.toList()));
    }

    private void setActiveTab(Button tab) {
        List.of(tabAll, tabWinning, tabOutbid, tabEnded)
                .forEach(b -> b.getStyleClass().setAll("btn-secondary"));
        tab.getStyleClass().setAll("btn-primary");
    }

    private void renderBids(List<Auction> list) {
        bidListContainer.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("No bids found in this category.");
            empty.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:13px; -fx-padding:40 0;");
            bidListContainer.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Auction a = list.get(i);
            HBox row = buildBidRow(a);
            bidListContainer.getChildren().add(row);
            int delay = i * 45;
            javafx.animation.PauseTransition p =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(delay));
            p.setOnFinished(e -> AnimationUtil.slideUp(row, 10, 200));
            p.play();
        }
    }

    private HBox buildBidRow(Auction a) {
        Long    curId   = currentUser.getId();
        boolean winning = a.isUserWinning(curId);
        boolean live    = a.isLive();
        boolean won     = a.isUserWon(curId);

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(winning && live ? "bid-row-leading" : "bid-row");
        row.setPadding(new Insets(12, 16, 12, 16));

        Label dot = new Label();
        String dotClass = live
                ? (winning ? "bid-dot-lead" : "bid-dot-outbid")
                : "bid-dot-finished";
        dot.getStyleClass().add(dotClass);

        Label thumb = new Label(a.getCategoryIcon());
        thumb.setStyle("-fx-font-size:22px; -fx-alignment:CENTER;"
                + "-fx-min-width:52px; -fx-max-width:52px;"
                + "-fx-min-height:52px; -fx-max-height:52px;"
                + "-fx-background-color:#f8fafc; -fx-background-radius:8;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(a.getTitle());
        title.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1a202c;");

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label catBadge = new Label(a.getCategory());
        catBadge.getStyleClass().addAll("badge", "badge-blue");
        String statusTxt = live
                ? (winning ? "🏆 Winning" : "⚠️ Outbid")
                : (won ? "🎉 Won" : "❌ Lost");
        Label statusBadge = new Label(statusTxt);
        statusBadge.getStyleClass().add(
                winning ? "pill-running" : live ? "pill-ending" : "pill-finished");
        metaRow.getChildren().addAll(catBadge, statusBadge);

        Label bidInfo = new Label(String.format(
                "Your bid: %,.0f  ·  Current: %,.0f",
                a.getUserBidAmount(curId), a.getCurrentPrice()));
        bidInfo.setStyle("-fx-font-size:12px; -fx-text-fill:#718096;");

        info.getChildren().addAll(title, metaRow, bidInfo);

        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setMinWidth(160);

        if (live) {
            Label timer = new Label("⏱ " + a.getTimeRemaining());
            timer.getStyleClass().add(
                    a.getRemainingSeconds() < 600 ? "timer-critical" : "timer-normal");
            Button rebidBtn = new Button(winning ? "Boost Bid" : "Re-Bid →");
            rebidBtn.getStyleClass().add(winning ? "btn-secondary" : "btn-primary");
            rebidBtn.setOnAction(e -> openDetail(a));
            right.getChildren().addAll(timer, rebidBtn);
        } else {
            String resultText = won
                    ? "🏆 Won — " + String.format("%,.0f", a.getCurrentPrice())
                    : "❌ Lost";
            Label resultLbl = new Label(resultText);
            resultLbl.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:"
                    + (won ? "#16a34a" : "#dc2626") + ";");
            right.getChildren().add(resultLbl);
        }

        row.getChildren().addAll(dot, thumb, info, right);
        return row;
    }

    private void openDetail(Auction auction) {
        try {
            javafx.scene.Node mainRootNode = bidListContainer.getScene().lookup("#mainRoot");
            MainController main = (MainController) mainRootNode.getUserData();

            // Sử dụng MainController.BASE để định vị chính xác vị trí file
            main.loadContent("/com/auction/client/auction_detail.fxml",
                    (AuctionDetailController ctrl) -> ctrl.initData(currentUser, auction));
        } catch (Exception e) {
            System.out.println("Lỗi mở chi tiết ở MyBidsController: " + e.getMessage());
            e.printStackTrace();
        }
    }
}