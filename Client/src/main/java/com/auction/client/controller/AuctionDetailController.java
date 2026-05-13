package com.auction.client.controller;

import com.auction.client.model.Auction;
import com.auction.client.model.BidTransaction;
import com.auction.client.model.User;
import com.auction.client.service.AuctionService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;

/**
 * AuctionDetailController — Chi tiết 1 phiên đấu giá.
 * - Countdown timer live (cập nhật mỗi giây)
 * - Đặt bid với validation
 * - Quick bid buttons
 * - Bid history timeline
 */
public class AuctionDetailController {

    @FXML private Label    detailImageIcon, detailTitle, detailSeller, detailDesc;
    @FXML private Label    detailCatBadge, detailCondBadge, detailStatusPill;
    @FXML private Label    detailCurrentPrice, detailTimer;
    @FXML private Label    detailBidCount, detailStartPrice;
    @FXML private Label    minBidHint, bidMsg;
    @FXML private TextField bidAmountField;
    @FXML private Button   placeBidBtn, quickBid1, quickBid2, quickBid3;
    @FXML private Label    sellerAvatar, sellerName, sellerSince;
    @FXML private VBox     bidHistoryList;
    @FXML private Label    bidHistoryCount;

    private final AuctionService auctionService = new AuctionService();
    private User    currentUser;
    private Auction auction;
    private Timeline countdownTimeline;
    private double minBid;
    private String SellerName;

    public void initData(User user, Auction auction) {
        this.currentUser = user;
        this.auction     = auction;
        this.SellerName  = auction.getSeller().getUsername();

        // Basic info
        detailImageIcon.setText(auction.getCategoryIcon());
        detailTitle.setText(auction.getTitle());
        detailDesc.setText(auction.getDescription());
        detailSeller.setText("Listed by " + SellerName
                + "  ·  " + auction.getBidCount() + " bids so far");

        detailCatBadge.setText(auction.getItem().getCategory());
        detailCondBadge.setText(auction.getCondition());

        // Status pill
        detailStatusPill.setText(auction.getStatusLabel());


        // Prices
        updatePriceDisplay();
        detailStartPrice.setText(String.format("%,.0f", auction.getItem().getStartingPrice()));

        // Seller info
        String sellerInitials = SellerName.length() >= 2
                ? SellerName.substring(0, 2).toUpperCase()
                : SellerName.toUpperCase();
        sellerAvatar.setText(sellerInitials);
        sellerName.setText(SellerName);
        sellerSince.setText("Member since 2023");

        // Quick bid increments based on current price
        double cur = auction.getCurrentPrice();
        double inc = auction.getMinIncrement();
        quickBid1.setText("+" + inc);
        quickBid2.setText("+" + inc * 5);
        quickBid3.setText("+" + inc * 10);

        minBid = cur + inc;
        minBidHint.setText("Minimum bid:"+ String.format("%,.0f", minBid));

        // Disable bid if not live or is own auction
        boolean canBid = auction.isLive()
                && auction.getSeller().getId() != user.getId();
        placeBidBtn.setDisable(!canBid);
        bidAmountField.setDisable(!canBid);
        if (!canBid) {
            bidMsg.setText(auction.isLive()
                    ? "You cannot bid on your own auction."
                    : "This auction has ended.");
            bidMsg.setTextFill(Color.web("#718096"));
        }

        // Bid history
        loadBidHistory();

        // Start live countdown
        startCountdown();
    }

    // ── Countdown timer ───────────────────────────────────────
    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            // Lấy số giây thực tế còn lại từ Model (Model tự trừ dựa trên giờ hệ thống)
            long secs = auction.getRemainingSeconds();

            if (secs <= 0) {
                detailTimer.setText("00:00:00");
                // ... (các phần set style giữ nguyên)
                countdownTimeline.stop();
                placeBidBtn.setDisable(true);
                detailStatusPill.setText("Ended");
                return;
            }

            // Cập nhật text hiển thị HH:mm:ss
            detailTimer.setText(formatCountdown(secs));

            // Logic đổi màu sắc theo thời gian còn lại
            if (secs < 600) { // Dưới 10 phút: Đỏ
                detailTimer.setStyle("-fx-text-fill:#dc2626; -fx-font-weight:bold;");
            } else if (secs < 3600) { // Dưới 1 tiếng: Cam
                detailTimer.setStyle("-fx-text-fill:#d97706; -fx-font-weight:bold;");
            }
        }));

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    // ── Place bid ─────────────────────────────────────────────
    @FXML public void handlePlaceBid() {
        String txt = bidAmountField.getText().trim();
        if (txt.isEmpty()) {
            showBidError("Please enter a bid amount.");
            AnimationUtil.shake(bidAmountField);
            return;
        }

        double amount;
        try { amount = Double.parseDouble(txt); }
        catch (NumberFormatException e) {
            showBidError("Enter a valid number.");
            AnimationUtil.shake(bidAmountField);
            return;
        }

        if (amount < minBid) {
            showBidError("Minimum bid is " + String.format("%,.0f", minBid));
            AnimationUtil.shake(bidAmountField);
            return;
        }

        // Confirm
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Bid");
        confirm.setHeaderText("Place bid of " + String.format("%,.2f", amount) + "?");
        confirm.setContentText("On: " + auction.getTitle());
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                auctionService.placeBid(auction.getId(), currentUser, amount);
                BidTransaction newBid = new BidTransaction(currentUser, amount, false);
                boolean success = auction.addBid(newBid);

                if (success) {
                    // Giá tự động tăng bên trong đối tượng auction rồi
                    // Bạn chỉ cần render lại giao diện thôi
                    renderUI();
                }
                minBid = amount + auction.getMinIncrement();
                minBidHint.setText("Minimum bid:" + String.format("%,.0f", minBid));
                bidAmountField.clear();
                updatePriceDisplay();
                loadBidHistory();
                showBidSuccess("🎉 Bid placed successfully!");
                AnimationUtil.pulse(placeBidBtn);
            }
        });
    }
    private void renderUI() {
        // 1. Cập nhật con số giá hiện tại
        detailCurrentPrice.setText(String.format("%,.0f", auction.getCurrentPrice()));

        // 2. Cập nhật số lượt đấu giá
        detailBidCount.setText(String.valueOf(auction.getBidCount()));

        // 3. Cập nhật lại gợi ý mức giá tối thiểu tiếp theo
        double minNextBid = auction.getCurrentPrice() + auction.getMinIncrement();
        minBidHint.setText("Minimum bid: " + String.format("%,.0f", minNextBid));

        // 4. Load lại danh sách lịch sử đấu giá (để hiện tên người vừa bid lên đầu)
        loadBidHistory();
    }

    // ── Quick bid handlers ────────────────────────────────────
    @FXML public void quickBid1() { setQuickBid(auction.getMinIncrement()); }
    @FXML public void quickBid2() { setQuickBid(auction.getMinIncrement() * 5); }
    @FXML public void quickBid3() { setQuickBid(auction.getMinIncrement() * 10); }

    private void setQuickBid(double add) {
        double val = auction.getCurrentPrice() + (add > 0 ? add : 10);
        bidAmountField.setText(String.format("%.0f", val));
    }

    @FXML public void goBack() {
        if (countdownTimeline != null) countdownTimeline.stop();
        MainController main = (MainController) bidAmountField
                .getScene().lookup("#mainRoot").getUserData();
        main.navAuctions();
    }

    // ── Bid history ───────────────────────────────────────────
    private void loadBidHistory() {
        List<BidTransaction> history = auctionService.getBidHistory(auction.getId());
        bidHistoryList.getChildren().clear();
        bidHistoryCount.setText(history.size() + " bids");

        if (history.isEmpty()) {
            Label empty = new Label("No bids yet. Be the first!");
            empty.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:13px;");
            bidHistoryList.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < history.size(); i++) {
            BidTransaction bid = history.get(i);
            boolean isTop = i == 0;
            HBox row = buildBidHistoryRow(bid, isTop);
            bidHistoryList.getChildren().add(row);
        }
    }

    private HBox buildBidHistoryRow(BidTransaction bid, boolean isTop) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);

        // Left: timeline dot + line
        VBox timeline = new VBox();
        timeline.setAlignment(Pos.TOP_CENTER);
        timeline.setPrefWidth(24);
        Label dot = new Label();
        dot.getStyleClass().add(isTop ? "bid-dot-lead" : "bid-dot");
        dot.setStyle("-fx-min-width:10px; -fx-max-width:10px;" +
                "-fx-min-height:10px; -fx-max-height:10px;" +
                (isTop ? "-fx-background-color:#16a34a;" : "-fx-background-color:#e2e8f0;") +
                "-fx-background-radius:50;");
        Region line = new Region();
        line.setStyle("-fx-background-color:#e2e8f0; -fx-pref-width:2px; -fx-min-width:2px;");
        VBox.setVgrow(line, Priority.ALWAYS);
        timeline.getChildren().addAll(dot, line);

        // Right: bid info
        HBox content = new HBox(14);
        content.setPadding(new Insets(0, 0, 14, 10));
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);
        String bidderName = bid.getBidder().getUsername();
        // Bidder avatar
        String init = bidderName.length() >= 2
                ? bidderName.substring(0, 2).toUpperCase()
                : bidderName.toUpperCase();
        Label avatar = new Label(init);
        avatar.setStyle("-fx-background-color:" + (isTop ? "#f0fdf4" : "#f1f5f9") + ";" +
                "-fx-text-fill:" + (isTop ? "#16a34a" : "#718096") + ";" +
                "-fx-font-weight:bold; -fx-font-size:11px; -fx-alignment:CENTER;" +
                "-fx-background-radius:50; -fx-min-width:32px; -fx-max-width:32px;" +
                "-fx-min-height:32px; -fx-max-height:32px;");

        // Bid text
        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        String bidderDisplay = bid.getBidder().getId() == currentUser.getId()
                ? "You" : bidderName;
        Label nameLabel = new Label(
                (isTop ? "🏆 " : "") + bidderDisplay + (isTop ? " (Leading)" : ""));
        nameLabel.setStyle("-fx-font-size:13px; -fx-font-weight:" + (isTop ? "bold" : "normal")
                + "; -fx-text-fill:" + (isTop ? "#15803d" : "#374151") + ";");
        Label timeLabel = new Label(bid.getFormattedTime());
        timeLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#a0aec0;");
        info.getChildren().addAll(nameLabel, timeLabel);

        // Amount
        Label amountLabel = new Label( String.format("%,.0f", bid.getAmount()));
        amountLabel.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:"
                + (isTop ? "#16a34a" : "#2563eb") + ";");

        content.getChildren().addAll(avatar, info, amountLabel);
        row.getChildren().addAll(timeline, content);
        return row;
    }

    // ── Helpers ───────────────────────────────────────────────
    private void updatePriceDisplay() {
        detailCurrentPrice.setText( String.format("%,.0f", auction.getCurrentPrice()));
        detailBidCount.setText(String.valueOf(auction.getBidCount()));
    }

    private String formatCountdown(long totalSecs) {
        long h = totalSecs / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }


    private void showBidError(String msg) {
        bidMsg.setText("✕  " + msg);
        bidMsg.setTextFill(Color.web("#dc2626"));
    }

    private void showBidSuccess(String msg) {
        bidMsg.setText(msg);
        bidMsg.setTextFill(Color.web("#16a34a"));
    }
}