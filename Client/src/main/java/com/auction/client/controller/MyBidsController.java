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
 * ĐÃ ĐƯỢC THÊM CƠ CHẾ NẠP NGẦM TRẠNG THÁI PAYMENT TỪ DATABASE ĐỂ ĐỒNG BỘ VĨNH VIỄN CHO NGƯỜI MUA KHI REFRESH/SIGN OUT.
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

        // Tải danh sách ở luồng phụ (Background Thread) để chống đơ màn hình
        new Thread(() -> {
            List<Auction> fetchedBids = auctionService.getMyBids(user.getId());

            javafx.application.Platform.runLater(() -> {
                this.myBids = fetchedBids != null ? fetchedBids : new java.util.ArrayList<>();
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
                    boolean isHighestByAmount = a.getUserBidAmount(curId) > 0 && a.getUserBidAmount(curId) == a.getCurrentPrice();
                    boolean isHighestByObj = a.getHighestBidder() != null && a.getHighestBidder().getId().equals(curId);
                    boolean isActuallyLeading = isHighestByObj || isHighestByAmount;

                    return a.isLive() ? isActuallyLeading : (a.isUserWon(curId) || isActuallyLeading);
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
                    if (!a.isLive()) return false;
                    boolean isNotWinningByObj = a.getHighestBidder() == null || !a.getHighestBidder().getId().equals(curId);
                    boolean isNotWinningByAmount = a.getUserBidAmount(curId) < a.getCurrentPrice();
                    return isNotWinningByObj && isNotWinningByAmount && a.getUserBidAmount(curId) > 0;
                })
                .collect(Collectors.toList());

        renderBids(outbidList);
    }

    @FXML
    public void showEnded() {
        setActiveTab(tabEnded);
        Long curId = currentUser.getId();

        List<Auction> endedList = myBids.stream()
                .filter(a -> !a.isLive() && a.getUserBidAmount(curId) > 0)
                .collect(Collectors.toList());

        renderBids(endedList);
    }

    private void setActiveTab(Button tab) {
        List.of(tabAll, tabWinning, tabOutbid, tabEnded).forEach(b -> b.getStyleClass().setAll("btn-secondary"));
        tab.getStyleClass().setAll("btn-primary");
        activeTab = tab;
    }

    private void renderBids(List<Auction> list) {
        bidListContainer.getChildren().clear();

        if (list == null || list.isEmpty()) {
            Label empty = new Label("No bids found in this category.");
            empty.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:13px; -fx-padding:40 0;");
            bidListContainer.getChildren().add(empty);
            return;
        }

        // THẦN THÁNH: Tạo luồng ngầm kiểm tra hóa đơn gốc thực tế từ Database cho người mua
        new Thread(() -> {
            try {
                boolean needRefresh = false;
                for (Auction auction : list) {
                    // Nếu là phiên kết thúc và người dùng hiện tại thắng cuộc, tiến hành check trạng thái Payment từ DB
                    if (auction.getStatus() != null && "FINISHED".equalsIgnoreCase(auction.getStatus().name()) && auction.isUserWon(currentUser.getId())) {
                        // Tránh gọi API lặp lại nếu cờ RAM đã nhận diện từ trước
                        if (!SessionManager.get().isAuctionPaidLocally(auction.getId())) {
                            com.auction.common.dto.PaymentDTO p = auctionService.getPaymentByAuctionId(auction.getId());
                            if (p != null && "COMPLETED".equalsIgnoreCase(p.getStatus())) {
                                // Đồng bộ ngược lại cờ RAM để hàm buildBidRow biết đường hiển thị huy hiệu "Đã thanh toán"
                                SessionManager.get().markAsPaid(auction.getId());
                                needRefresh = true;
                            }
                        }
                    }
                }
                // Nếu phát hiện có dữ liệu thay đổi từ DB, ép giao diện render lại hàng loạt một cách mượt mà
                if (needRefresh) {
                    javafx.application.Platform.runLater(() -> rebuildCurrentTabContents(list));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Tiến hành render layout gốc trước, luồng ngầm cập nhật sau đè lên cực kỳ mượt mà
        for (int i = 0; i < list.size(); i++) {
            Auction a = list.get(i);
            HBox row = buildBidRow(a);
            bidListContainer.getChildren().add(row);

            // Hiệu ứng mượt mà lúc load danh sách
            int delay = i * 30;
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(delay));
            p.setOnFinished(e -> AnimationUtil.slideUp(row, 10, 180));
            p.play();
        }
    }

    // Hàm phụ trợ hỗ trợ vẽ lại giao diện tức thì khi luồng ngầm nạp xong dữ liệu DB
    private void rebuildCurrentTabContents(List<Auction> currentList) {
        bidListContainer.getChildren().clear();
        for (Auction a : currentList) {
            HBox row = buildBidRow(a);
            bidListContainer.getChildren().add(row);
        }
    }

    private HBox buildBidRow(Auction a) {
        Long curId = currentUser.getId();
        boolean live = a.isLive();

        boolean isHighestByAmount = a.getUserBidAmount(curId) > 0 && a.getUserBidAmount(curId) == a.getCurrentPrice();
        boolean isHighestByObj = a.getHighestBidder() != null && a.getHighestBidder().getId().equals(curId);

        boolean winning = live && (isHighestByObj || isHighestByAmount);
        boolean won = !live && (a.isUserWon(curId) || isHighestByAmount);

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(winning ? "bid-row-leading" : "bid-row");
        row.setPadding(new Insets(12, 16, 12, 16));

        Label dot = new Label();
        String dotClass = live ? (winning ? "bid-dot-lead" : "bid-dot-outbid") : (won ? "bid-dot-lead" : "bid-dot-finished");
        dot.getStyleClass().add(dotClass);

        Label thumb = new Label(a.getCategoryIcon());
        thumb.setStyle("-fx-font-size:22px; -fx-alignment:CENTER; -fx-min-width:52px; -fx-max-width:52px;" +
                "-fx-min-height:52px; -fx-max-height:52px; -fx-background-color:#f8fafc; -fx-background-radius:8;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label title = new Label(a.getTitle());
        title.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#1a202c;");

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label catBadge = new Label(a.getCategory());
        catBadge.getStyleClass().addAll("badge", "badge-blue");

        String statusTxt = live ? (winning ? "🏆 Winning" : "⚠️ Outbid") : (won ? "🎉 Won" : "❌ Lost");
        Label statusBadge = new Label(statusTxt);
        statusBadge.getStyleClass().add(live ? (winning ? "pill-running" : "pill-ending") : (won ? "pill-running" : "pill-finished"));
        metaRow.getChildren().addAll(catBadge, statusBadge);

        String bidInfoText = String.format("Your bid: $%,.0f  ·  Current: $%,.0f", a.getUserBidAmount(curId), a.getCurrentPrice());
        Label bidInfo = new Label(bidInfoText);
        bidInfo.setStyle("-fx-font-size:12px; -fx-text-fill:#718096;");

        info.getChildren().addAll(title, metaRow, bidInfo);

        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setMinWidth(160);

        if (live) {
            Label timer = new Label("⏱ " + a.getTimeRemaining());
            timer.getStyleClass().add(a.getRemainingSeconds() < 600 ? "timer-critical" : "timer-normal");

            Button rebidBtn = new Button(winning ? "Boost Bid" : "Re-Bid →");
            rebidBtn.getStyleClass().add(winning ? "btn-secondary" : "btn-primary");
            rebidBtn.setOnAction(e -> openDetail(a));

            right.getChildren().addAll(timer, rebidBtn);
        } else {
            String resultText = won ? "🏆 Won — $" + String.format("%,.0f", a.getCurrentPrice()) : "❌ Lost";
            Label resultLbl = new Label(resultText);
            resultLbl.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:" + (won ? "#16a34a" : "#dc2626") + ";");

            Button viewBtn = new Button("View Detail");
            viewBtn.getStyleClass().add("btn-secondary");
            viewBtn.setOnAction(e -> openDetail(a));

            right.getChildren().addAll(resultLbl, viewBtn);

            if (won) {
                String auctionStatus = a.getStatus() != null ? a.getStatus().name() : "";

                // ĐỒNG BỘ TUYỆT ĐỐI: Kiểm tra trạng thái DB HOẶC kiểm tra cờ lưu tập trung trên SessionManager
                boolean isPaid = "PAID".equalsIgnoreCase(auctionStatus)
                        || "SOLD_PAID".equalsIgnoreCase(auctionStatus)
                        || "COMPLETED".equalsIgnoreCase(auctionStatus)
                        || SessionManager.get().isAuctionPaidLocally(a.getId());

                if (isPaid) {
                    Label paidBadge = new Label("✓ Đã thanh toán");
                    paidBadge.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#065f46;" +
                            "-fx-font-size:11px; -fx-font-weight:bold; -fx-padding:4 10; -fx-background-radius:20;");
                    right.getChildren().add(paidBadge);
                } else {
                    Button payBtn = new Button("💳 Thanh toán");
                    payBtn.setStyle("-fx-background-color:#10b981; -fx-text-fill:white; -fx-font-weight:bold;" +
                            "-fx-font-size:12px; -fx-cursor:hand; -fx-background-radius:8; -fx-padding:6 14;");

                    payBtn.setOnAction(e -> {
                        PaymentDialogHelper.showPaymentDialog(currentUser, a, () -> {
                            // Cập nhật số dư tài khoản trên RAM Client tức thì
                            User sessionUser = SessionManager.get().getUser();
                            if (sessionUser != null) {
                                sessionUser.setBalance(sessionUser.getBalance() - a.getCurrentPrice());
                                SessionManager.get().login(sessionUser);
                                this.currentUser = sessionUser;
                            }

                            // ĐĂNG KÝ TRẠNG THÁI: Ghi nhận cờ thanh toán lên bộ nhớ tổng SessionManager
                            SessionManager.get().markAsPaid(a.getId());

                            // Cập nhật lại giao diện tab hiện tại ngay lập tức, cực kỳ mượt mà
                            javafx.application.Platform.runLater(() -> {
                                if (activeTab == tabAll) showAll();
                                else if (activeTab == tabWinning) showWinning();
                                else if (activeTab == tabOutbid) showOutbid();
                                else if (activeTab == tabEnded) showEnded();
                                else renderBids(myBids);
                            });
                        });
                    });
                    right.getChildren().add(payBtn);
                }
            }
        }

        row.getChildren().addAll(dot, thumb, info, right);
        return row;
    }

    private void openDetail(Auction auction) {
        MainController main = (MainController) bidListContainer.getScene().lookup("#mainRoot").getUserData();
        main.loadContent(
                "/com/auction/client/auction_detail.fxml",
                (AuctionDetailController ctrl) -> ctrl.initData(currentUser, auction)
        );
    }
}