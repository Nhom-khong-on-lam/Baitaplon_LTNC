package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.dto.AutoBidDTO;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.model.User;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.List;

public class AuctionDetailController {

    // ── FXML Elements ───────────────────────────────────────────────────────
    @FXML private LineChart<String, Number> priceChart;
    @FXML private Label detailImageIcon, detailTitle, detailSeller, detailDesc;
    @FXML private Label detailCatBadge, detailCondBadge, detailStatusPill;
    @FXML private Label detailCurrentPrice, detailTimer, detailBidCount, detailStartPrice;
    @FXML private Label bidMsg, sellerAvatar, sellerName;
    @FXML private TextField bidAmountField, autoBidLimitField, autoBidStepField;
    @FXML private Button placeBidBtn, autoBidBtn, quickBid1, quickBid2, quickBid3;
    @FXML private VBox bidHistoryList;
    @FXML private Label bidHistoryCount;
    @FXML private VBox resultBox;
    @FXML private Label resultTitle, resultMsg;
    @FXML private Label detailReserveStatus;

    // ── Core Fields ─────────────────────────────────────────────────────────
    private final AuctionService auctionService = new AuctionService();
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private User currentUser;
    private Auction auction;
    private Timeline countdownTimeline;
    private boolean isAutoBidActive = false;
    private javafx.concurrent.ScheduledService<Response> pollingService;

    // Thread-safe state tracking to break infinite layout rendering loops
    private int lastRenderedBidCount = -1;

    // ── 1. Initialize Data (initData) ───────────────────────────────────────
    public void initData(User user, Auction auction) {
        this.currentUser = user;
        this.auction = auction;

        // Chart Optimization: Bind Series once and disable animation to eliminate high CPU usage
        priceSeries.setName("Price Trend");
        priceChart.setAnimated(false);
        if (!priceChart.getData().contains(priceSeries)) {
            priceChart.getData().add(priceSeries);
        }

        // Asynchronously load the product image layout
        initImageLoader();

        // Populate static visual fields
        detailTitle.setText(auction.getTitle());
        detailDesc.setText(auction.getItem() != null ? auction.getItem().getDescription() : "");
        detailCatBadge.setText(auction.getItem() != null ? auction.getItem().getCategory() : "General");

        // FIXED CRASH: Your Item class does not have getCondition(). Fallback to standard text.
        detailCondBadge.setText("Standard");

        detailStatusPill.setText(auction.getStatusLabel());
        detailStartPrice.setText(String.format("%,.0f", auction.getItem().getStartingPrice()));

        String sellerNameStr = auction.getSeller().getUsername();
        String sellerInitials = sellerNameStr.length() >= 2 ? sellerNameStr.substring(0, 2).toUpperCase() : sellerNameStr.toUpperCase();
        sellerAvatar.setText(sellerInitials);
        sellerName.setText(sellerNameStr);

        // Run baseline render engine
        renderUI();
        checkInitialAutoBidStatus();
        startCountdown();
        initLivePolling();

        // Enforce contextual interaction guards
        boolean canBid = auction.isLive() && auction.getSeller().getId() != user.getId();
        placeBidBtn.setDisable(!canBid);
        bidAmountField.setDisable(!canBid);

        if (!canBid) {
            if (!auction.isLive()) {
                handleAuctionEndedUI();
            } else {
                bidMsg.setText("You cannot bid on your own auction.");
                bidMsg.setTextFill(Color.web("#718096"));
            }
        }

        // Auto clean threads on window close to avoid background memory leaks
        detailTimer.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                shutdownAllThreads();
            }
        });
    }

    // ── 2. Asynchronous Image Loader Engine ─────────────────────────────────
    private void initImageLoader() {
        String imgUrl = auction.getItem() != null ? auction.getItem().getImageUrl() : null;
        if (imgUrl != null && !imgUrl.isEmpty()) {
            detailImageIcon.setText("");
            String url = imgUrl.trim();

            if (url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")) {
                Image cloudImage = new Image(url, true);
                cloudImage.progressProperty().addListener((obs, oldProg, newProg) -> {
                    if (newProg.doubleValue() == 1.0 && !cloudImage.isError()) {
                        Platform.runLater(() -> detailImageIcon.setStyle(
                                "-fx-min-width: 300px; -fx-max-width: 300px; -fx-min-height: 300px; -fx-max-height: 300px;" +
                                        "-fx-background-image: url('" + url + "'); -fx-background-size: contain;" +
                                        "-fx-background-repeat: no-repeat; -fx-background-position: center;"
                        ));
                    }
                });
            } else {
                detailImageIcon.setStyle(
                        "-fx-min-width: 300px; -fx-max-width: 300px; -fx-min-height: 300px; -fx-max-height: 300px;" +
                                "-fx-background-image: url('" + url + "'); -fx-background-size: contain;" +
                                "-fx-background-repeat: no-repeat; -fx-background-position: center;"
                );
            }
        } else {
            detailImageIcon.setText(auction.getCategoryIcon());
            detailImageIcon.setStyle("-fx-font-size:100px; -fx-padding:20;");
        }
    }

    // ── 3. Countdown Clock Engine ───────────────────────────────────────────
    private void startCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long secs = auction.getRemainingSeconds();
            if (secs <= 0) {
                detailTimer.setText("00:00:00");
                countdownTimeline.stop();
                placeBidBtn.setDisable(true);
                detailStatusPill.setText("Ended");
                handleAuctionEndedUI();
                return;
            }
            detailTimer.setText(formatCountdown(secs));
            if (secs < 600) {
                detailTimer.setStyle("-fx-text-fill:#dc2626; -fx-font-weight:bold;");
            } else if (secs < 3600) {
                detailTimer.setStyle("-fx-text-fill:#d97706; -fx-font-weight:bold;");
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    // ── 4. Fixed Live Polling Service (Prevents UI Freezing Deadlocks) ───────
    private void initLivePolling() {
        pollingService = new javafx.concurrent.ScheduledService<>() {
            @Override
            protected javafx.concurrent.Task<Response> createTask() {
                return new javafx.concurrent.Task<>() {
                    @Override
                    protected Response call() throws Exception {
                        return auctionService.getAuctionById(auction.getId());
                    }
                };
            }
        };
        pollingService.setPeriod(Duration.seconds(3));

        pollingService.setOnSucceeded(event -> {
            Response res = pollingService.getValue();
            if (res != null && res.isSuccess() && res.getData() instanceof Auction updatedAuction) {

                // Keep background data sync operations outside the FX Application UI Thread
                this.auction.setCurrentPrice(updatedAuction.getCurrentPrice());
                this.auction.setBidCount(updatedAuction.getBidCount());
                this.auction.setHighestBidder(updatedAuction.getHighestBidder());
                this.auction.setStatus(updatedAuction.getStatus());
                this.auction.setEndTime(updatedAuction.getEndTime());

                Platform.runLater(() -> {
                    updatePriceDisplay();
                    renderReservePriceLogic();
                    detailStatusPill.setText(this.auction.getStatusLabel());

                    // FIXED DEADLOCK: Only reload transaction layout history if the bid size actually changed
                    if (this.auction.getBidCount() != lastRenderedBidCount) {
                        loadBidHistory();
                    }
                });
            }
        });
        pollingService.start();
    }

    // ── 5. Manual Transaction Manager (handlePlaceBid) ──────────────────────
    @FXML
    public void handlePlaceBid() {
        if (auction.getSeller().getId().equals(currentUser.getId())) {
            showAlert(Alert.AlertType.WARNING, "Bidding Error", "You cannot bid on your own product!");
            return;
        }

        String txt = bidAmountField.getText().trim();
        if (txt.isEmpty()) { showBidError("Please enter a bid amount."); return; }

        double amount;
        try { amount = Double.parseDouble(txt); }
        catch (NumberFormatException e) { showBidError("Enter a valid number."); return; }

        if (amount <= auction.getCurrentPrice()) {
            showBidError("Bid must be > " + String.format("%,.0f", auction.getCurrentPrice()));
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Bid");
        confirm.setHeaderText("Place bid of " + String.format("%,.0f", amount) + "?");
        confirm.setContentText("Product: " + auction.getTitle());
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                new Thread(() -> {
                    Response res = auctionService.placeBid(auction.getId(), currentUser, amount);
                    Platform.runLater(() -> {
                        if (res != null && res.isSuccess()) {
                            bidAmountField.clear();
                            updatePriceDisplay();

                            // Instantly refresh the timeline pipeline
                            if (pollingService != null) pollingService.restart();

                            showBidSuccess("🎉 Bid placed successfully!");
                        } else {
                            showBidError(res != null ? res.getMessage() : "Connection timeout!");
                        }
                    });
                }).start();
            }
        });
    }

    // ── 6. Temporal Extensions ──────────────────────────────────────────────
    @FXML
    private void handleExtendAuctionClick() {
        if (this.auction == null) return;

        int hoursToExtend = 2;
        Object[] payload = new Object[] { this.auction.getId(), hoursToExtend };

        new Thread(() -> {
            try {
                Request req = new Request(Request.EXTEND_AUCTION, payload);
                Response res = (Response) com.auction.client.service.ServerConnection.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (res != null && res.isSuccess()) {
                        showBidSuccess("⏳ Auction time successfully extended by " + hoursToExtend + " hours!");
                        System.out.println("🎉 [CLIENT SUCCESS] Auction " + this.auction.getId() + " reset to LIVE.");
                    } else {
                        showBidError(res != null ? res.getMessage() : "Extension failed: Connection lost!");
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // ── 7. Auto-Bid Pipeline Engine ──────────────────────────────────────────
    @FXML
    private void handleSetAutoBid() {
        if (isAutoBidActive) {
            AutoBidDTO dto = new AutoBidDTO();
            dto.setAuctionId(this.auction.getId());
            dto.setBidderId(currentUser.getId());
            dto.setMaxPrice(0.0);
            dto.setStepIncrement(0.0);
            dto.setActive(false);

            new Thread(() -> {
                try {
                    Request req = new Request(Request.SET_AUTO_BID, dto);
                    Response res = (Response) com.auction.client.service.ServerConnection.getInstance().sendRequest(req);
                    Platform.runLater(() -> {
                        if (res != null && res.isSuccess()) {
                            isAutoBidActive = false;
                            autoBidBtn.setText("Turn On Auto Bid");
                            autoBidBtn.setStyle("-fx-background-color: #ebf8ff; -fx-text-fill: #2b6cb0; -fx-border-color: #4299e1; -fx-font-weight: bold;");
                            autoBidLimitField.setDisable(false);
                            autoBidStepField.setDisable(false);
                            autoBidLimitField.clear();
                            autoBidStepField.clear();
                            showBidSuccess("🎉 Auto-bid has been canceled.");
                        } else {
                            showBidError(res != null ? res.getMessage() : "Lost connection!");
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
            return;
        }

        String maxTxt = autoBidLimitField.getText().trim();
        String stepTxt = autoBidStepField.getText().trim();
        if (maxTxt.isEmpty() || stepTxt.isEmpty()) { showBidError("Please fill in all AutoBid fields!"); return; }

        try {
            double maxPrice = Double.parseDouble(maxTxt);
            double stepIncrement = Double.parseDouble(stepTxt);

            if (maxPrice <= auction.getCurrentPrice()) { showBidError("Max price must be greater than current price!"); return; }
            if (stepIncrement <= 0) { showBidError("Increment must be > 0!"); return; }

            AutoBidDTO dto = new AutoBidDTO();
            dto.setAuctionId(this.auction.getId());
            dto.setBidderId(currentUser.getId());
            dto.setMaxPrice(maxPrice);
            dto.setStepIncrement(stepIncrement);
            dto.setActive(true);

            new Thread(() -> {
                try {
                    Request req = new Request(Request.SET_AUTO_BID, dto);
                    Response res = (Response) com.auction.client.service.ServerConnection.getInstance().sendRequest(req);
                    Platform.runLater(() -> {
                        if (res != null && res.isSuccess()) {
                            isAutoBidActive = true;
                            autoBidBtn.setText("Turn Off Auto Bid");
                            autoBidBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-border-color: #f87171; -fx-font-weight: bold;");
                            autoBidLimitField.setDisable(true);
                            autoBidStepField.setDisable(true);
                            showBidSuccess("🚀 Auto-bid has been successfully activated!");
                        } else {
                            showBidError(res != null ? res.getMessage() : "Refused by server.");
                        }
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();

        } catch (NumberFormatException e) { showBidError("Enter valid configurations!"); }
    }

    private void checkInitialAutoBidStatus() {
        new Thread(() -> {
            try {
                Request checkReq = new Request("CHECK_AUTOBID_STATUS", new Object[]{auction.getId(), currentUser.getId()});
                Response checkRes = (Response) com.auction.client.service.ServerConnection.getInstance().sendRequest(checkReq);
                if (checkRes != null && checkRes.isSuccess() && checkRes.getData() != null) {
                    Object data = checkRes.getData();
                    boolean activeInDB = false;
                    String maxStr = "0"; String stepStr = "0";

                    if (data instanceof Boolean) { activeInDB = (Boolean) data; }
                    else if (data instanceof AutoBidDTO config) {
                        activeInDB = config.isActive();
                        maxStr = String.format("%.0f", config.getMaxPrice());
                        stepStr = String.format("%.0f", config.getStepIncrement());
                    }

                    final boolean finalActive = activeInDB;
                    final String finalMax = maxStr; final String finalStep = stepStr;

                    Platform.runLater(() -> {
                        if (finalActive) {
                            isAutoBidActive = true;
                            autoBidBtn.setText("Turn Off Auto Bid");
                            autoBidBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-border-color: #f87171; -fx-font-weight: bold;");
                            autoBidLimitField.setDisable(true); autoBidStepField.setDisable(true);
                            autoBidLimitField.setText(finalMax); autoBidStepField.setText(finalStep);
                        }
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    // ── 8. Dialog Triggers ──────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void renderUI() {
        Platform.runLater(() -> {
            detailCurrentPrice.setText(String.format("%,.0f", auction.getCurrentPrice()));
            detailBidCount.setText(String.valueOf(auction.getBidCount()));
        });
        loadBidHistory();
    }

    // ── 9. Fast Value Delta Adjustments ─────────────────────────────────────
    @FXML public void quickBid1() { setQuickBid(10); }
    @FXML public void quickBid2() { setQuickBid(50); }
    @FXML public void quickBid3() { setQuickBid(100); }

    private void setQuickBid(double add) {
        double val = auction.getCurrentPrice() + (add > 0 ? add : 10);
        bidAmountField.setText(String.format("%.0f", val));
    }

    @FXML
    public void goBack() {
        shutdownAllThreads();
        MainController main = (MainController) bidAmountField.getScene().lookup("#mainRoot").getUserData();
        main.navAuctions();
    }

    // ── 10. Ledger Render Optimization (SetAll & AddAll Batching) ───────────
    private void loadBidHistory() {
        new Thread(() -> {
            List<BidTransaction> history = auctionService.getBidHistory(auction.getId());
            Platform.runLater(() -> renderBidHistory(history));
        }).start();
    }

    private void renderBidHistory(List<BidTransaction> history) {
        bidHistoryList.getChildren().clear();

        // Save current history size to state tracker to avoid redundant canvas refreshes
        lastRenderedBidCount = history.size();
        bidHistoryCount.setText(lastRenderedBidCount + " bids");

        // Batch graph vector coordinates inside a single list to keep CPU usage low
        List<XYChart.Data<String, Number>> updatedPoints = new ArrayList<>();
        XYChart.Data<String, Number> startPoint = new XYChart.Data<>("Start", auction.getItem().getStartingPrice());
        updatedPoints.add(startPoint);

        for (int i = history.size() - 1; i >= 0; i--) {
            BidTransaction bid = history.get(i);
            XYChart.Data<String, Number> bidPoint = new XYChart.Data<>(bid.getFormattedTime(), bid.getAmount());
            updatedPoints.add(bidPoint);
        }
        priceSeries.getData().setAll(updatedPoints);

        if (history.isEmpty()) {
            Label empty = new Label("No bids yet. Be the first!");
            empty.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:13px;");
            bidHistoryList.getChildren().add(empty);
            return;
        }

        // Render HBox structures into a virtual batch array to bypass multi-phase layout calculations
        List<HBox> historyRows = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            BidTransaction bid = history.get(i);
            boolean isTop = (i == 0);
            HBox row = buildBidHistoryRow(bid, isTop);
            historyRows.add(row);
        }
        bidHistoryList.getChildren().addAll(historyRows);
    }

    private HBox buildBidHistoryRow(BidTransaction bid, boolean isTop) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);

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

        HBox content = new HBox(14);
        content.setPadding(new Insets(0, 0, 14, 10));
        content.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);
        String bidderName = bid.getBidder().getUsername();

        String init = bidderName.length() >= 2 ? bidderName.substring(0, 2).toUpperCase() : bidderName.toUpperCase();
        Label avatar = new Label(init);
        avatar.setStyle("-fx-background-color:" + (isTop ? "#f0fdf4" : "#f1f5f9") + ";" +
                "-fx-text-fill:" + (isTop ? "#16a34a" : "#718096") + ";" +
                "-fx-font-weight:bold; -fx-font-size:11px; -fx-alignment:CENTER;" +
                "-fx-background-radius:50; -fx-min-width:32px; -fx-max-width:32px;" +
                "-fx-min-height:32px; -fx-max-height:32px;");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        String bidderDisplay = bid.getBidder().getId() == currentUser.getId() ? "You" : bidderName;
        Label nameLabel = new Label((isTop ? "🏆 " : "") + bidderDisplay + (isTop ? " (Leading)" : ""));
        nameLabel.setStyle("-fx-font-size:13px; -fx-font-weight:" + (isTop ? "bold" : "normal") + "; -fx-text-fill:" + (isTop ? "#15803d" : "#374151") + ";");
        Label timeLabel = new Label(bid.getFormattedTime());
        timeLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#a0aec0;");
        info.getChildren().addAll(nameLabel, timeLabel);

        Label amountLabel = new Label(String.format("%,.0f", bid.getAmount()));
        amountLabel.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + (isTop ? "#16a34a" : "#2563eb") + ";");

        content.getChildren().addAll(avatar, info, amountLabel);
        row.getChildren().addAll(timeline, content);
        return row;
    }

    // ── 11. Support Utility Functions ───────────────────────────────────────
    private void updatePriceDisplay() {
        Platform.runLater(() -> {
            detailCurrentPrice.setText(String.format("%,.0f", auction.getCurrentPrice()));
            detailBidCount.setText(String.valueOf(auction.getBidCount()));
        });
    }

    private void renderReservePriceLogic() {
        Platform.runLater(() -> {
            try {
                if (detailReserveStatus == null || auction == null) return;

                double currentPrice = auction.getCurrentPrice();
                double reservePrice = auction.getReservePrice();

                detailReserveStatus.setVisible(true);

                if (reservePrice > 0) {
                    if (currentPrice < reservePrice) {
                        detailReserveStatus.setText("❌ Reserve price not met");
                        detailReserveStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 13px;");
                    } else {
                        detailReserveStatus.setText("✔ Reserve price has been met");
                        detailReserveStatus.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 13px;");
                    }
                } else {
                    detailReserveStatus.setText("ℹ No reserve price configured for this auction");
                    detailReserveStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-style: italic; -fx-font-size: 13px;");
                }
            } catch (Exception e) {
                System.err.println("⚠ Layout render error on reserve engine: " + e.getMessage());
            }
        });
    }

    private void handleAuctionEndedUI() {
        Long curId = currentUser.getId();
        String curName = currentUser.getUsername();
        String winName = (auction.getHighestBidder() != null) ? auction.getHighestBidder().getUsername() : "";

        if (auction.getSeller() != null && auction.getSeller().getId().equals(curId)) {
            String msg = !winName.isEmpty() ? "🎉 SUCCESSFULLY SOLD!\nWinner: " + winName : "Your auction has ended.\nNo buyers.";
            showAuctionResult(msg, "#1e40af", "#dbeafe");
        } else if (!winName.isEmpty() && winName.equals(curName)) {
            showAuctionResult("🎉 Congratulations! You have WON this auction!", "#16a34a", "#f0fdf4");
        } else if (auction.getUserBidAmount(curId) > 0) {
            showAuctionResult("You did not win this auction.\nWinner: " + (winName.isEmpty() ? "None" : winName), "#dc2626", "#fef2f2");
        } else {
            showAuctionResult("The auction has ended.\nWinner: " + (winName.isEmpty() ? "None" : winName), "#1e40af", "#dbeafe");
        }
    }

    private void shutdownAllThreads() {
        if (countdownTimeline != null) { countdownTimeline.stop(); countdownTimeline = null; }
        if (pollingService != null) { pollingService.cancel(); pollingService = null; }
        System.out.println("🛑 [SHUTDOWN] Background worker clusters stopped.");
    }

    private String formatCountdown(long totalSecs) {
        long h = totalSecs / 3600;
        long m = (totalSecs % 3600) / 60;
        long s = totalSecs % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void showBidError(String msg) {
        Platform.runLater(() -> {
            bidMsg.setText("✕  " + msg);
            bidMsg.setTextFill(Color.web("#dc2626"));
        });
    }

    private void showBidSuccess(String msg) {
        Platform.runLater(() -> {
            bidMsg.setText(msg);
            bidMsg.setTextFill(Color.web("#16a34a"));
        });
    }

    private void showAuctionResult(String status, String colorHex, String bgColorHex) {
        Platform.runLater(() -> {
            resultBox.setVisible(true);
            resultBox.setManaged(true);
            resultBox.setStyle("-fx-border-color: " + colorHex + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-color: " + bgColorHex + "; -fx-padding: 15px;");
            resultTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: " + colorHex + ";");
            resultMsg.setText(status);
            resultMsg.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
            bidMsg.setText("");
        });
    }
}