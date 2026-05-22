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
 * MyBidsController — Hiển thị lịch sử và trạng thái đấu giá của user.
 */
public class MyBidsController {

    @FXML private Button tabAll, tabWinning, tabOutbid, tabEnded;
    @FXML private Label  bidCount;
    @FXML private VBox   bidListContainer;

    private final AuctionService auctionService = new AuctionService();
    private User currentUser;
    private List<Auction> myBids;
    private Button activeTab;

    public void initData(User user) {
        this.currentUser = user;

        // 1. Mở luồng phụ để tải danh sách các phiên đã đặt giá
        new Thread(() -> {
            List<Auction> fetchedBids = auctionService.getMyBids(user.getId());

            // 2. Tải xong thì đưa về luồng giao diện để vẽ lên màn hình
            javafx.application.Platform.runLater(() -> {
                this.myBids = fetchedBids;

                // Tránh lỗi NullPointerException nếu Server trả về null
                if (this.myBids == null) {
                    this.myBids = new java.util.ArrayList<>();
                }

                bidCount.setText(this.myBids.size() + " bids total");
                setActiveTab(tabAll);
                renderBids(this.myBids);
            });
        }).start();
    }

    @FXML public void showAll()     { setActiveTab(tabAll);     renderBids(myBids); }
    @FXML
    public void showWinning() {
        setActiveTab(tabWinning);
        Long curId = currentUser.getId();

        List<Auction> winningList = myBids.stream()
                .filter(a -> {
                    if (a.isLive()) {
                        // Nếu đang diễn ra: Mình phải là người ra giá cao nhất
                        return a.getHighestBidder() != null && a.getHighestBidder().getId().equals(curId);
                    } else {
                        // Nếu đã kết thúc: Biến isUserWon(id) của class Auction phải trả về true
                        return a.isUserWon(curId);
                    }
                })
                .collect(Collectors.toList());

        renderBids(winningList);
    }
    @FXML
    public void showOutbid() {
        setActiveTab(tabOutbid);

        Long curId = currentUser.getId();

        List<Auction> outbidList = myBids.stream()
                .filter(a -> {
                    // Điều kiện 1: Phiên đấu giá vẫn đang diễn ra
                    boolean isLive = a.isLive();

                    // Điều kiện 2: Người cao nhất KHÔNG PHẢI là mình
                    boolean isNotWinning = a.getHighestBidder() == null ||
                            !a.getHighestBidder().getId().equals(curId);

                    // ⭐ Điều kiện 3 (QUYẾT ĐỊNH): Mình PHẢI TỪNG ĐẶT GIÁ cho phiên này rồi
                    // Kiểm tra xem số tiền mình từng bid có lớn hơn 0 hay không
                    boolean iHaveBid = a.getUserBidAmount(curId) > 0;

                    // Thỏa mãn cả 3 mới được coi là bị người khác vượt mặt
                    return isLive && isNotWinning && iHaveBid;
                })
                .collect(Collectors.toList());

        renderBids(outbidList);
    }
    @FXML
    public void showEnded() {
        setActiveTab(tabEnded);

        Long curId = currentUser.getId();

        List<Auction> endedList = myBids.stream()
                .filter(a -> {
                    // Điều kiện 1: Phiên đấu giá đã kết thúc
                    boolean isEnded = !a.isLive();

                    // Điều kiện 2: Mình PHẢI TỪNG ĐẶT GIÁ cho phiên này
                    boolean iHaveBid = a.getUserBidAmount(curId) > 0;

                    return isEnded && iHaveBid;
                })
                .collect(Collectors.toList());

        renderBids(endedList);
    }

    private void setActiveTab(Button tab) {
        List.of(tabAll, tabWinning, tabOutbid, tabEnded).forEach(b -> {
            b.getStyleClass().setAll("btn-secondary");
        });
        tab.getStyleClass().setAll("btn-primary");
        activeTab = tab;
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
        // 1. Lấy ID và trạng thái quan trọng
        Long curId = currentUser.getId();
        boolean winning = a.isUserWinning(curId);
        boolean live    = a.isLive();
        boolean won     = a.isUserWon(curId);

        // 2. Container chính cho dòng
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(winning && live ? "bid-row-leading" : "bid-row");
        row.setPadding(new Insets(12, 16, 12, 16));

        // 3. Status Dot (Chấm tròn trạng thái)
        Label dot = new Label();
        String dotClass = live ? (winning ? "bid-dot-lead" : "bid-dot-outbid") : "bid-dot-finished";
        dot.getStyleClass().add(dotClass);

        // 4. Thumbnail (Icon danh mục)
        Label thumb = new Label(a.getCategoryIcon());
        thumb.setStyle("-fx-font-size:22px; -fx-alignment:CENTER; -fx-min-width:52px; -fx-max-width:52px;" +
                "-fx-min-height:52px; -fx-max-height:52px; -fx-background-color:#f8fafc; -fx-background-radius:8;");

        // 5. Thông tin chi tiết (VBox giữa)
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(a.getTitle());
        title.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1a202c;");

        // Dòng badge (thể loại và trạng thái)
        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label catBadge = new Label(a.getCategory());
        catBadge.getStyleClass().addAll("badge", "badge-blue");

        String statusTxt = live ? (winning ? "🏆 Winning" : "⚠️ Outbid") : (won ? "🎉 Won" : "❌ Lost");
        Label statusBadge = new Label(statusTxt);
        if (live) {
            statusBadge.getStyleClass().add(winning ? "pill-running" : "pill-ending");
        } else {
            // Nếu thắng cuộc thì cho màu xanh (pill-running), nếu thua cho màu xám/đỏ (pill-finished)
            statusBadge.getStyleClass().add(won ? "pill-running" : "pill-finished");
        }

        metaRow.getChildren().addAll(catBadge, statusBadge);

        // Thông tin giá tiền
        String bidInfoText = String.format("Your bid: $%,.0f  ·  Current: $%,.0f",
                a.getUserBidAmount(curId), a.getCurrentPrice());
        Label bidInfo = new Label(bidInfoText);
        bidInfo.setStyle("-fx-font-size:12px; -fx-text-fill:#718096;");

        info.getChildren().addAll(title, metaRow, bidInfo);

        // 6. Phần bên phải (Timer và Nút bấm)
        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setMinWidth(160);

        if (live) {
            // Hiển thị đếm ngược
            Label timer = new Label("⏱ " + a.getTimeRemaining());
            timer.getStyleClass().add(a.getRemainingSeconds() < 600 ? "timer-critical" : "timer-normal");

            // Nút đặt giá lại
            Button rebidBtn = new Button(winning ? "Boost Bid" : "Re-Bid →");
            rebidBtn.getStyleClass().add(winning ? "btn-secondary" : "btn-primary");
            rebidBtn.setOnAction(e -> openDetail(a)); // Mở chi tiết

            right.getChildren().addAll(timer, rebidBtn);

        } else {
            // Hiển thị kết quả thắng/thua
            String resultText = won ? "🏆 Won — $" + String.format("%,.0f", a.getCurrentPrice()) : "❌ Lost";
            Label resultLbl = new Label(resultText);
            String color = won ? "#16a34a" : "#dc2626";
            resultLbl.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:" + color + ";");

            //  THÊM NÚT XEM CHI TIẾT DÀNH CHO PHIÊN ĐÃ KẾT THÚC
            Button viewBtn = new Button("View Detail");
            viewBtn.getStyleClass().add("btn-secondary"); // Dùng nút phụ (viền mờ) cho phiên đã kết thúc
            viewBtn.setOnAction(e -> openDetail(a)); // Vẫn gọi hàm mở phòng chi tiết bình thường

            right.getChildren().addAll(resultLbl, viewBtn);
        }

        row.getChildren().addAll(dot, thumb, info, right);
        return row;
    }

    private void openDetail(Auction auction) {
        MainController main = (MainController) bidListContainer
                .getScene().lookup("#mainRoot").getUserData();
        main.loadContent(
                "/com/auction/client/auction_detail.fxml",
                (AuctionDetailController ctrl) -> ctrl.initData(currentUser, auction)
        );
    }
}