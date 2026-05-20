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
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
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
    @FXML private Label    bidMsg;
    @FXML private TextField bidAmountField;
    @FXML private Button   placeBidBtn, quickBid1, quickBid2, quickBid3;
    @FXML private Label    sellerAvatar, sellerName;
    @FXML private VBox     bidHistoryList;
    @FXML private Label    bidHistoryCount;

    private final AuctionService auctionService = new AuctionService();
    private User    currentUser;
    private Auction auction;
    private Timeline countdownTimeline;
    private String SellerName;
    private boolean isAutoBidActive = false;
    @FXML private TextField autoBidLimitField; // Thay cho autoBidMaxPriceField cũ
    @FXML private TextField autoBidStepField;  // Biến này giữ nguyên vì FXML đã đặt đúng
    @FXML private Button autoBidBtn;
    @FXML private ImageView myImageView;

    public void initData(User user, Auction auction) {
        this.currentUser = user;
        this.auction     = auction;
        this.SellerName  = auction.getSeller().getUsername();

        // Basic info
        String imgUrl = auction.getItem() != null ? auction.getItem().getImageUrl() : null;
        if (imgUrl != null && !imgUrl.isEmpty()) {
            detailImageIcon.setText(""); // Xóa chữ Icon đi

            String url = imgUrl.trim();

            // Kiểm tra xem là link mạng Cloudinary (http/https) hay file local cũ
            if (url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")) {
                // Tạo đối tượng Image JavaFX chuyên dụng, truyền tham số 'true' để TẢI ẢNH NGẦM qua Internet
                javafx.scene.image.Image cloudImage = new javafx.scene.image.Image(url, true);

                // Lắng nghe khi nào ảnh tải xong từ mây Cloudinary về máy khách thành công
                cloudImage.progressProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() == 1.0 && !cloudImage.isError()) {
                        // Khi ảnh tải xong (đạt 100%), dùng Platform.runLater nạp CSS hiển thị lên giao diện
                        javafx.application.Platform.runLater(() -> {
                            detailImageIcon.setText("");
                            detailImageIcon.setStyle(
                                    "-fx-min-width: 300px; -fx-max-width: 300px;" +
                                            "-fx-min-height: 300px; -fx-max-height: 300px;" +
                                            "-fx-background-image: url('" + url + "');" +
                                            "-fx-background-size: contain;" +
                                            "-fx-background-repeat: no-repeat;" +
                                            "-fx-background-position: center;"
                            );
                        });
                    }
                });

                // Trường hợp lỗi tải (link hỏng, mất mạng), nạp ảnh mặc định hoặc báo lỗi
                cloudImage.exceptionProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null || cloudImage.isError()) {
                        javafx.application.Platform.runLater(() -> {
                            detailImageIcon.setText("⚠️");
                            detailImageIcon.setStyle("-fx-font-size:50px; -fx-padding:20; -fx-alignment: center; -fx-text-fill: #dc2626;");
                            System.err.println("❌ Lỗi tải ảnh từ Cloudinary: " + (newValue != null ? newValue.getMessage() : "Unknown error"));
                        });
                    }
                });

            } else {
                // Nếu là đường dẫn tệp cục bộ cũ (file:///) thì nạp trực tiếp qua CSS như cũ bình thường
                detailImageIcon.setStyle(
                        "-fx-min-width: 300px; -fx-max-width: 300px;" +
                                "-fx-min-height: 300px; -fx-max-height: 300px;" +
                                "-fx-background-image: url('" + url + "');" +
                                "-fx-background-size: contain;" +
                                "-fx-background-repeat: no-repeat;" +
                                "-fx-background-position: center;"
                );
            }
        } else {
            // Khôi phục lại giao diện icon mặc định nếu sản phẩm không có ảnh up lên
            detailImageIcon.setText(auction.getCategoryIcon());
            detailImageIcon.setStyle("-fx-font-size:100px; -fx-padding:20;");
        }

        detailTitle.setText(auction.getTitle());

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

        // Quick bid increments based on current price
        double cur = auction.getCurrentPrice();
        quickBid1.setText("+10");
        quickBid2.setText("+50");
        quickBid3.setText("+100" );

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
    @FXML
    private void handleSetAutoBid() {
        if (autoBidLimitField == null || autoBidStepField == null || autoBidBtn == null) {
            System.err.println("Lỗi: Các thành phần AutoBid chưa được ánh xạ đúng từ FXML!");
            return;
        }

        // ── TRƯỜNG HỢP 1: NGƯỜI DÙNG MUỐN HỦY AUTOBID ĐANG CHẠY ──────────────────
        if (isAutoBidActive) {
            com.auction.common.dto.AutoBidDTO dto = new com.auction.common.dto.AutoBidDTO();
            dto.setAuctionId(this.auction.getId());
            dto.setBidderId(SessionManager.get().getUser().getId());

            // 🚀 BÍ QUYẾT: Đặt maxPrice = 0 để Server nhận diện 100% đây là lệnh HỦY
            dto.setMaxPrice(0.0);
            dto.setActive(false);

            new Thread(() -> {
                try {
                    com.auction.common.network.Response res = auctionService.setAutoBid(dto);

                    javafx.application.Platform.runLater(() -> {
                        // Kiểm tra chống sập nếu res bị null
                        if (res != null && res.isSuccess()) {
                            isAutoBidActive = false;

                            autoBidBtn.setText("Kích hoạt Auto Bid");
                            autoBidBtn.setStyle("-fx-background-color: #ebf8ff; -fx-text-fill: #2b6cb0; -fx-border-color: #4299e1; -fx-font-weight: bold;");
                            autoBidLimitField.setDisable(false);
                            autoBidStepField.setDisable(false);

                            if (bidMsg != null) {
                                bidMsg.setText("Đã hủy chế độ Đấu giá tự động!");
                                bidMsg.setStyle("-fx-text-fill: #eab308;");
                            }
                        } else {
                            if (bidMsg != null) {
                                bidMsg.setText(res != null ? res.getMessage() : "Lỗi: Server phản hồi trống hoặc mất kết nối!");
                                bidMsg.setStyle("-fx-text-fill: #dc2626;");
                            }
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
            return;
        }

        // ── TRƯỜNG HỢP 2: NGƯỜI DÙNG KÍCH HOẠT MỚI ─────────────────────────────
        String maxTxt = autoBidLimitField.getText().trim();
        String stepTxt = autoBidStepField.getText().trim();

        if (maxTxt.isEmpty() || stepTxt.isEmpty()) {
            if (bidMsg != null) {
                bidMsg.setText("Vui lòng nhập đầy đủ thông tin AutoBid!");
                bidMsg.setStyle("-fx-text-fill: #dc2626;");
            }
            return;
        }

        try {
            double maxPrice = Double.parseDouble(maxTxt);
            double stepIncrement = Double.parseDouble(stepTxt);

            if (maxPrice <= auction.getCurrentPrice()) {
                if (bidMsg != null) bidMsg.setText("Giá tối đa phải lớn hơn giá hiện tại!");
                return;
            }
            if (stepIncrement <= 0) {
                if (bidMsg != null) bidMsg.setText("Bước nhảy phải lớn hơn 0!");
                return;
            }

            com.auction.common.dto.AutoBidDTO dto = new com.auction.common.dto.AutoBidDTO();
            dto.setAuctionId(this.auction.getId());
            dto.setBidderId(SessionManager.get().getUser().getId());
            dto.setMaxPrice(maxPrice); // Gửi số tiền thật > 0 lên để Server biết đây là lệnh BẬT
            dto.setStepIncrement(stepIncrement);
            dto.setActive(true);

            new Thread(() -> {
                try {
                    com.auction.common.network.Response res = auctionService.setAutoBid(dto);

                    javafx.application.Platform.runLater(() -> {
                        if (res != null && res.isSuccess()) {
                            isAutoBidActive = true;

                            autoBidBtn.setText("Hủy Auto Bid");
                            autoBidBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-border-color: #f87171; -fx-font-weight: bold;");

                            autoBidLimitField.setDisable(true);
                            autoBidStepField.setDisable(true);

                            if (bidMsg != null) {
                                bidMsg.setText("Đã kích hoạt tự động đấu giá thành công!");
                                bidMsg.setStyle("-fx-text-fill: #16a34a;");
                            }
                        } else {
                            if (bidMsg != null) {
                                bidMsg.setText(res != null ? res.getMessage() : "Server từ chối yêu cầu kích hoạt!");
                                bidMsg.setStyle("-fx-text-fill: #dc2626;");
                            }
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

        } catch (NumberFormatException e) {
            if (bidMsg != null) {
                bidMsg.setText("Vui lòng chỉ nhập số hợp lệ!");
                bidMsg.setStyle("-fx-text-fill: #dc2626;");
            }
        }
    }
    // ── Place bid ─────────────────────────────────────────────
    @FXML public void handlePlaceBid() {
        if (auction.getSeller().getId().equals(currentUser.getId())) {
            // Hiển thị thông báo cảnh báo lỗi bằng Alert hoặc Label thông báo
            showAlert(Alert.AlertType.WARNING, "Lỗi đặt giá", "Bạn không thể tự đặt giá cho sản phẩm của chính mình!");
            return; // Dừng xử lý luôn, không gửi request lên Server nữa
        }
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

        if (amount <= auction.getCurrentPrice()) {
            showBidError("Bid must be > " + String.format("%,.0f", auction.getCurrentPrice()));
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
                // Tạm khóa nút và hiện thông báo chờ để tránh click 2 lần
                placeBidBtn.setDisable(true);
                bidMsg.setText("Processing bid...");
                bidMsg.setTextFill(Color.web("#d97706")); // Màu cam

                // 1. Đẩy lệnh Bid lên Server thông qua luồng chạy ngầm
                new Thread(() -> {
                    auctionService.placeBid(auction.getId(), currentUser, amount);
                    BidTransaction newBid = new BidTransaction(currentUser, amount, false);
                    boolean success = auction.addBid(newBid);

                    // 2. Nhận kết quả xong thì mở khóa UI và cập nhật màn hình
                    javafx.application.Platform.runLater(() -> {
                        if (success) {
                            // Giá tự động tăng bên trong đối tượng auction rồi
                            renderUI();
                        }

                        bidAmountField.clear();
                        updatePriceDisplay();
                        loadBidHistory();
                        showBidSuccess("🎉 Bid placed successfully!");
                        AnimationUtil.pulse(placeBidBtn);

                        placeBidBtn.setDisable(false); // Mở khóa nút
                    });
                }).start();
            }
        });
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void renderUI() {
        // 1. Cập nhật con số giá hiện tại
        detailCurrentPrice.setText(String.format("%,.0f", auction.getCurrentPrice()));

        // 2. Cập nhật số lượt đấu giá
        detailBidCount.setText(String.valueOf(auction.getBidCount()));

        // 3. Load lại danh sách lịch sử đấu giá (để hiện tên người vừa bid lên đầu)
        loadBidHistory();
    }

    // ── Quick bid handlers ────────────────────────────────────
    @FXML public void quickBid1() { setQuickBid(10); }
    @FXML public void quickBid2() { setQuickBid(50); }
    @FXML public void quickBid3() { setQuickBid(100); }

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
        // 1. Mở luồng phụ đi lấy dữ liệu mạng
        new Thread(() -> {
            List<BidTransaction> history = auctionService.getBidHistory(auction.getId());

            // 2. Có dữ liệu rồi thì nhờ luồng UI vẽ lên giao diện
            javafx.application.Platform.runLater(() -> {
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
            });
        }).start();
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
