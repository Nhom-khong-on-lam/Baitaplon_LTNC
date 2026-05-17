package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.model.User;
import com.auction.common.enums.AuctionStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuctionDetailController — Displays details of a single auction.
 * Fully synchronized with your existing Auction and MainController structures.
 */
public class AuctionDetailController {

    @FXML private javafx.scene.image.ImageView detailImageView;
    @FXML private Label    detailTitle, detailSeller, detailDesc;

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
    private double  minBid;
    private Timeline countdownTimeline;
    private long    remainingSeconds = 0;

    public void initData(User user, Auction auction) {

        this.currentUser = user;
        this.auction     = auction;
        if (placeBidBtn != null) {
            placeBidBtn.setDisable(false);      // Mở khóa nút bấm mặc định
            placeBidBtn.setText("Place Bid");   // Trả lại chữ đặt cược mặc định
        }
        if (bidAmountField != null) {
            bidAmountField.setDisable(false);
            bidAmountField.clear();// Mở khóa ô nhập tiền mặc định
        }
        if (bidMsg != null) {
            bidMsg.setVisible(false); // Ẩn thông báo lỗi phòng cũ
        }
        if (auction.getSeller() != null && auction.getSeller().getId().equals(currentUser.getId())) {
            if (placeBidBtn != null) {
                placeBidBtn.setDisable(true);          // Vô hiệu hóa nút đặt cược
                placeBidBtn.setText("Your Product");   // Đổi chữ hiển thị để thông báo
            }
        }

        if (auction == null || user == null) return;
        String sellerNameStr = auction.getSeller() != null ? auction.getSeller().getUsername() : "Unknown";

        if (detailImageView != null) {
            setDefaultImage();
        }

        if (detailTitle != null) {
            detailTitle.setText(auction.getItem() != null ? auction.getItem().getName() : "No Title");
        }
        if (detailDesc != null) {
            detailDesc.setText(auction.getItem() != null ? auction.getItem().getDescription() : "");
        }

        if (detailSeller != null) {
            detailSeller.setText("Listed by " + sellerNameStr + "  ·  " + auction.getBidCount() + " bids so far");
        }

        double startPrice = 0;
        if (auction.getItem() != null) {
            startPrice = auction.getItem().getStartingPrice();
            if (detailCatBadge != null) detailCatBadge.setText(auction.getItem().getCategory());
        } else {
            if (detailCatBadge != null) detailCatBadge.setText("General");
        }

        if (detailStartPrice != null) {
            detailStartPrice.setText(String.format("%,.0f", startPrice));
        }

        if (detailCondBadge != null) {
            detailCondBadge.setText("Standard");
        }

        if (detailStatusPill != null) {
            detailStatusPill.setText(auction.getStatus() != null ? auction.getStatus().name() : "UNKNOWN");
        }

        if (sellerAvatar != null) {
            String init = sellerNameStr.length() >= 2 ? sellerNameStr.substring(0, 2).toUpperCase() : sellerNameStr.toUpperCase();
            sellerAvatar.setText(init);
        }
        if (sellerName != null) sellerName.setText(sellerNameStr);
        if (sellerSince != null) sellerSince.setText("Member since 2023");

        updatePriceDisplay();

        double cur = auction.getCurrentPrice();
        double increment = auction.getMinIncrement();

        if (quickBid1 != null) quickBid1.setText("+" + String.format("%.0f", increment));
        if (quickBid2 != null) quickBid2.setText("+" + String.format("%.0f", increment * 5));
        if (quickBid3 != null) quickBid3.setText("+" + String.format("%.0f", increment * 10));

        minBid = cur + increment;
        if (minBidHint != null) {
            minBidHint.setText("Minimum bid: " + String.format("%,.0f", minBid));
        }

        if (auction.getEndTime() != null) {
            java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), auction.getEndTime());
            remainingSeconds = duration.isNegative() ? 0 : duration.getSeconds();
        } else {
            remainingSeconds = 0;
        }

        boolean isOwnAuction = auction.getSeller() != null && java.util.Objects.equals(auction.getSeller().getId(), user.getId());
        boolean isLiveAuction = remainingSeconds > 0 && auction.getStatus() == AuctionStatus.RUNNING;
        boolean canBid = isLiveAuction && !isOwnAuction;

        if (placeBidBtn != null) placeBidBtn.setDisable(!canBid);
        if (bidAmountField != null) bidAmountField.setDisable(!canBid);

        if (bidMsg != null && !canBid) {
            bidMsg.setText(isOwnAuction ? "You cannot bid on your own auction." : "This auction has ended.");
            bidMsg.setTextFill(Color.web("#718096"));
            bidMsg.setVisible(true);
        }

        loadBidHistory();
        startCountdown();
    }

    private void setDefaultImage() {
        try {
            // Sửa đường dẫn nạp ảnh placeholder tương thích với cây thư mục của bạn
            detailImageView.setImage(new javafx.scene.image.Image(getClass().getResourceAsStream("/com/auction/client/images/default-placeholder.png")));
        } catch (Exception e) {
            System.out.println("Could not load default placeholder image.");
        }
    }

    @FXML
    public void handlePlaceBid() {
        if (bidMsg != null) {
            bidMsg.setVisible(false);
            bidMsg.setTextFill(Color.web("#dc2626"));
        }

        String amountText = bidAmountField.getText().trim();
        if (amountText.isEmpty()) {
            showBidError("Please enter an amount.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            showBidError("Enter a valid number.");
            return;
        }

        if (amount < minBid) {
            showBidError("Bid must be at least " + String.format("%,.0f", minBid));
            return;
        }

        placeBidBtn.setDisable(true);
        if (bidMsg != null) {
            bidMsg.setText("Submitting your bid...");
            bidMsg.setTextFill(Color.web("#d97706"));
            bidMsg.setVisible(true);
        }

        Thread t = new Thread(() -> {
            com.auction.common.network.Response res = auctionService.placeBid(auction.getId(), currentUser, amount);
            javafx.application.Platform.runLater(() -> {
                placeBidBtn.setDisable(false);
                if (res != null && res.isSuccess()) {
                    if (bidMsg != null) {
                        bidMsg.setText("Bid placed successfully!");
                        bidMsg.setTextFill(Color.web("#16a34a"));
                    }
                    bidAmountField.clear();

                    if (res.getData() instanceof Auction) {
                        this.auction = (Auction) res.getData();
                        updatePriceDisplay();
                        minBid = auction.getCurrentPrice() + auction.getMinIncrement();
                        if (minBidHint != null) {
                            minBidHint.setText("Minimum bid: " + String.format("%,.0f", minBid));
                        }
                    }
                    loadBidHistory();
                } else {
                    showBidError(res != null ? res.getMessage() : "Server communication error.");
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML public void handleQuickBid1() { fillQuickBid(1); }
    @FXML public void handleQuickBid2() { fillQuickBid(5); }
    @FXML public void handleQuickBid3() { fillQuickBid(10); }

    private void fillQuickBid(int multiplier) {
        double amount = auction.getCurrentPrice() + (auction.getMinIncrement() * multiplier);
        bidAmountField.setText(String.format("%.0f", amount));
        if (bidMsg != null) bidMsg.setVisible(false);
    }

    @FXML
    public void goBack() {
        if (countdownTimeline != null) countdownTimeline.stop();
        // Gọi hàm navAuctions() điều hướng chuẩn xác tới com/auction/client/auctions.fxml
        MainController main = (MainController) placeBidBtn.getScene().lookup("#mainRoot").getUserData();
        if (main != null) {
            main.navAuctions();
        }
    }

    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (remainingSeconds <= 0) {
                if (detailTimer != null) {
                    detailTimer.setText("ENDED");
                    detailTimer.setStyle("-fx-text-fill: #718096;");
                }
                if (placeBidBtn != null) placeBidBtn.setDisable(true);
                if (bidAmountField != null) bidAmountField.setDisable(true);
                countdownTimeline.stop();
            } else {
                remainingSeconds--;
                if (detailTimer != null) detailTimer.setText(formatCountdown(remainingSeconds));
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void loadBidHistory() {
        if (bidHistoryList == null) return;

        Thread worker = new Thread(() -> {
            List<BidTransaction> list = auctionService.getBidHistory(auction.getId());
            javafx.application.Platform.runLater(() -> {
                bidHistoryList.getChildren().clear();
                if (bidHistoryCount != null) bidHistoryCount.setText(list.size() + " bids");

                if (list.isEmpty()) {
                    Label emptyLabel = new Label("No bids yet. Be the first to place a bid!");
                    emptyLabel.setStyle("-fx-text-fill: #a0aec0; -fx-font-style: italic; -fx-padding: 10;");
                    bidHistoryList.getChildren().add(emptyLabel);
                    return;
                }

                for (int i = 0; i < list.size(); i++) {
                    bidHistoryList.getChildren().add(createBidRow(list.get(i), i == 0));
                }
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    private HBox createBidRow(BidTransaction bid, boolean isTop) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle("-fx-border-color: #f1f5f9; -fx-border-width: 0 0 1 0; " +
                (isTop ? "-fx-background-color: #f0fdf4;" : "-fx-background-color: transparent;"));

        VBox timeline = new VBox();
        timeline.setAlignment(Pos.CENTER);
        timeline.setPrefWidth(24);
        Label dot = new Label(isTop ? "👑" : "•");
        dot.setStyle("-fx-font-size: " + (isTop ? "14px" : "18px") + "; -fx-text-fill: " + (isTop ? "#16a34a" : "#cbd5e1") + ";");
        timeline.getChildren().add(dot);

        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);

        String name = bid.getBidder() != null ? bid.getBidder().getUsername() : "User";
        Label avatar = new Label(name.substring(0, Math.min(name.length(), 2)).toUpperCase());
        avatar.setStyle("-fx-background-color: " + (isTop ? "#dcfce7;" : "#f1f5f9;") +
                "-fx-text-fill: " + (isTop ? "#15803d;" : "#475569;") +
                "-fx-alignment: center; -fx-background-radius: 50; -fx-font-weight: bold; -fx-font-size: 11px; " +
                "-fx-min-width: 28px; -fx-max-width: 28px; -fx-min-height: 28px; -fx-max-height: 28px;");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nameLabel = new Label(name + (isTop ? " (Highest Bidder)" : ""));
        nameLabel.setStyle("-fx-font-weight: " + (isTop ? "bold" : "normal") + "; -fx-text-fill: #1e293b; -fx-font-size: 13px;");

        String timeStr = bid.getFormattedTime() != null ? bid.getFormattedTime() : "Just now";
        Label timeLabel = new Label(timeStr);
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        info.getChildren().addAll(nameLabel, timeLabel);

        Label amountLabel = new Label(String.format("%,.0f", bid.getAmount()));
        amountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight:bold; -fx-text-fill: " + (isTop ? "#16a34a" : "#2563eb") + ";");

        content.getChildren().addAll(avatar, info, amountLabel);
        row.getChildren().addAll(timeline, content);
        return row;
    }

    private void updatePriceDisplay() {
        if (auction != null) {
            if (detailCurrentPrice != null) detailCurrentPrice.setText(String.format("%,.0f", auction.getCurrentPrice()));
            if (detailBidCount != null) detailBidCount.setText(String.valueOf(auction.getBidCount()));
        }
    }

    private String formatCountdown(long totalSecs) {
        long h = totalSecs / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void showBidError(String msg) {
        if (bidMsg != null) {
            bidMsg.setText("✕  " + msg);
            bidMsg.setTextFill(Color.web("#dc2626"));
            bidMsg.setVisible(true);
        }
    }
}