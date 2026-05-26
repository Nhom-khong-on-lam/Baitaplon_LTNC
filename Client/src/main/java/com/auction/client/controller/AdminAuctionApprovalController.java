package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminAuctionApprovalController — Review and approve/reject new auctions submitted by users.
 */
public class AdminAuctionApprovalController {

    // ── FXML fields ───────────────────────────────────────────────────────────
    @FXML private Button    tabPending, tabApproved, tabRejected;
    @FXML private Label     totalLabel, pendingBadge;
    @FXML private TextField searchField;

    @FXML private TableView<Auction>           approvalTable;
    @FXML private TableColumn<Auction, String> colId, colTitle, colSeller,
            colCategory, colStartPrice,
            colDescription, colStatus,
            colSubmittedAt, colActions;

    // ── State ─────────────────────────────────────────────────────────────────
    private AuctionService auctionService = new AuctionService();
    private User           currentAdmin;
    private List<Auction>  allPending  = new ArrayList<>();
    private List<Auction>  allApproved = new ArrayList<>();
    private List<Auction>  allRejected = new ArrayList<>();
    private String         activeTab   = "PENDING";

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ── Init ──────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        setupColumns();
    }

    public void initData(User admin) {
        this.currentAdmin = admin;
        totalLabel.setText("Loading...");
        pendingBadge.setVisible(false);
        refreshAllData();
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void refreshAllData() {
        // 1. Đặt trạng thái thông báo để Admin biết dữ liệu đang tải
        if (totalLabel != null) {
            totalLabel.setText("⌛ Syncing data with server...");
        }

        new Thread(() -> {
            // CHỈ GỌI MẠNG ĐÚNG 1 LẦN DUY NHẤT để lấy tất cả phòng về
            List<Auction> allAuctions = auctionService.getAllApprovalAuctions();

            Platform.runLater(() -> {
                if (allAuctions != null) {
                    // 2. Tự lọc cực nhanh trên RAM Client bằng cách so sánh Trạng thái (AuctionStatus)
                    // Phân loại phòng CHỜ DUYỆT
                    allPending = allAuctions.stream()
                            .filter(a -> a.getStatus() == AuctionStatus.PENDING_APPROVAL)
                            .collect(Collectors.toCollection(ArrayList::new));

                    // Phân loại phòng ĐÃ DUYỆT (Có thể trạng thái là RUNNING hoặc các trạng thái live khác tùy logic của bạn)
                    allApproved = allAuctions.stream()
                            .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                            .collect(Collectors.toCollection(ArrayList::new));

                    // Phân loại phòng BỊ TỪ CHỐI
                    allRejected = allAuctions.stream()
                            .filter(a -> a.getStatus() == AuctionStatus.REJECTED)
                            .collect(Collectors.toCollection(ArrayList::new));
                } else {
                    allPending = new ArrayList<>();
                    allApproved = new ArrayList<>();
                    allRejected = new ArrayList<>();
                }

                // 3. Cập nhật Badge đếm số lượng thông báo và chuyển giao diện về Tab đang đứng
                updatePendingBadge();

                switch (activeTab) {
                    case "APPROVED" -> showApproved();
                    case "REJECTED" -> showRejected();
                    default         -> showPending();
                }
            });
        }).start();
    }

    @FXML
    public void showPending() {
        activeTab = "PENDING";
        setActiveTab(tabPending);
        // Không gọi lại service.getPendingAuctions() ở đây nữa để tránh lag UI
        // Sử dụng luôn danh sách allPending đã được nạp từ refreshAllData()
        loadTable(filterBySearch(allPending));
        updatePendingBadge();
    }

    @FXML
    public void showApproved() {
        activeTab = "APPROVED";
        setActiveTab(tabApproved);
        // Sử dụng trực tiếp danh sách đã có trong bộ nhớ RAM Client
        loadTable(filterBySearch(allApproved));
    }

    @FXML
    public void showRejected() {
        activeTab = "REJECTED";
        setActiveTab(tabRejected);
        // Sử dụng trực tiếp danh sách đã có trong bộ nhớ RAM Client
        loadTable(filterBySearch(allRejected));
    }

    @FXML public void handleSearch() {
        switch (activeTab) {
            case "APPROVED" -> loadTable(filterBySearch(allApproved));
            case "REJECTED" -> loadTable(filterBySearch(allRejected));
            default         -> loadTable(filterBySearch(allPending));
        }
    }

    private List<Auction> filterBySearch(List<Auction> source) {
        if (source == null) return new ArrayList<>();
        String kw = searchField.getText() == null ? ""
                : searchField.getText().trim().toLowerCase();
        if (kw.isEmpty()) return source;
        return source.stream()
                .filter(a -> (a.getItem() != null && a.getItem().getName().toLowerCase().contains(kw))
                        || (a.getSeller() != null && a.getSeller().getUsername().toLowerCase().contains(kw))
                        || (a.getItem() != null && a.getItem().getCategory() != null && a.getItem().getCategory().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
    }

    // ── Table columns ──────────────────────────────────────────────────────────
    private void setupColumns() {
        approvalTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        colId.setMinWidth(55);
        colTitle.setMinWidth(160);
        colSeller.setMinWidth(130);
        colCategory.setMinWidth(100);
        colStartPrice.setMinWidth(110);
        colDescription.setMinWidth(200);
        colStatus.setMinWidth(110);
        colSubmittedAt.setMinWidth(140);
        colActions.setMinWidth(200);

        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));

        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItem() != null ? c.getValue().getItem().getName() : "N/A"));
        colTitle.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String t, boolean empty) {
                super.updateItem(t, empty);
                if (empty) { setText(null); return; }
                setText(t);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#1a202c;");
                setWrapText(true);
            }
        });

        colSeller.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSeller() != null ? c.getValue().getSeller().getUsername() : "Unknown"));
        colSeller.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);
                if (empty || name == null) { setGraphic(null); return; }
                String init = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
                Label av = new Label(init);
                av.setStyle("-fx-background-color:#eff6ff; -fx-text-fill:#2563eb;" +
                        "-fx-font-weight:bold; -fx-font-size:10px; -fx-alignment:CENTER;" +
                        "-fx-background-radius:50; -fx-min-width:28px; -fx-max-width:28px;" +
                        "-fx-min-height:28px; -fx-max-height:28px;");
                Label nameLbl = new Label(name);
                nameLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#374151;");
                HBox box = new HBox(6, av, nameLbl);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });

        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getItem() != null ? c.getValue().getItem().getCategory() : "General"));
        colCategory.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) { setGraphic(null); return; }
                Label badge = new Label(categoryIcon(cat) + " " + cat);
                badge.getStyleClass().addAll("badge", "badge-blue");
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        colStartPrice.setCellValueFactory(c -> new SimpleStringProperty(String.format("%,.0f ", c.getValue().getItem() != null ? c.getValue().getItem().getStartingPrice() : 0.0)));
        colStartPrice.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String p, boolean empty) {
                super.updateItem(p, empty);
                if (empty) { setText(null); return; }
                setText(p);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#2563eb;");
            }
        });

        colDescription.setCellValueFactory(c -> {
            String desc = c.getValue().getItem() != null ? c.getValue().getItem().getDescription() : "";
            if (desc == null || desc.isBlank()) desc = "(No description)";
            return new SimpleStringProperty(desc.length() > 90 ? desc.substring(0, 90) + "…" : desc);
        });
        colDescription.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String d, boolean empty) {
                super.updateItem(d, empty);
                if (empty) { setText(null); return; }
                setText(d);
                setStyle("-fx-text-fill:#6b7280; -fx-font-size:11px;");
                setWrapText(true);
            }
        });

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus() != null ? c.getValue().getStatus().name() : ""));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                String display;
                String sc;
                switch (status) {
                    case "PENDING_APPROVAL" -> { display = "Pending";  sc = "pill-ending";  }
                    case "RUNNING"          -> { display = "Approved"; sc = "pill-running"; }
                    case "REJECTED"         -> { display = "Rejected"; sc = "pill-finished";}
                    default                 -> { display = status;        sc = "pill-finished";}
                }
                Label pill = new Label(display);
                pill.getStyleClass().add(sc);
                setGraphic(pill);
                setAlignment(Pos.CENTER);
            }
        });

        colSubmittedAt.setCellValueFactory(c -> {
            String ts = c.getValue().getStartTime() != null ? c.getValue().getStartTime().format(FMT) : "—";
            return new SimpleStringProperty(ts);
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn  = new Button("Reject");
            private final Button detailBtn  = new Button("👁");
            private final HBox   box        = new HBox(6, approveBtn, rejectBtn, detailBtn);

            {
                approveBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                approveBtn.getStyleClass().add("btn-primary");

                rejectBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                rejectBtn.getStyleClass().add("btn-danger");

                detailBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                detailBtn.getStyleClass().add("btn-secondary");

                box.setAlignment(Pos.CENTER);

                approveBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleApprove(a);
                });
                rejectBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleReject(a);
                });
                detailBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    handleViewDetail(a);
                });
            }

            @Override protected void updateItem(String o, boolean empty) {
                super.updateItem(o, empty);
                if (empty) { setGraphic(null); return; }
                Auction a = getTableView().getItems().get(getIndex());
                boolean isPending = a.getStatus() == AuctionStatus.PENDING_APPROVAL;
                approveBtn.setDisable(!isPending);
                rejectBtn.setDisable(!isPending);
                setGraphic(box);
            }
        });
    }

    private void loadTable(List<Auction> list) {
        approvalTable.setItems(FXCollections.observableArrayList(list));
        totalLabel.setText(list.size() + " auctions");
    }

    private void setActiveTab(Button tab) {
        List.of(tabPending, tabApproved, tabRejected)
                .forEach(b -> b.getStyleClass().setAll("btn-secondary"));
        tab.getStyleClass().setAll("btn-primary");
    }

    private void handleApprove(Auction auction) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Approve Auction");
        confirm.setHeaderText("Approve \"" + (auction.getItem() != null ? auction.getItem().getName() : "Auction") + "\"?");
        confirm.setContentText("The auction will go live immediately and the seller will be notified.");

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            new Thread(() -> {
                // Ép kiểu tường minh sang Long trước khi đóng gói gửi đi nhằm tránh lỗi ClassCastException trên Server
                Long idToSend = Long.valueOf(auction.getId());
                boolean success = auctionService.approveAuction(idToSend);

                Platform.runLater(() -> {
                    if (success) {
                        // Xóa đối tượng khỏi danh sách chờ duyệt
                        allPending.removeIf(a -> a.getId().equals(auction.getId()));
                        auction.setStatus(AuctionStatus.RUNNING);

                        // Thêm lên đầu danh sách đã duyệt để lưu vết tạm thời
                        allApproved.add(0, auction);

                        updatePendingBadge();
                        showPending(); // Vẽ lại bảng dữ liệu mới
                        showInfo("✅ Auction \"" + (auction.getItem() != null ? auction.getItem().getName() : "") + "\" has been approved.");
                    } else {
                        showError("Failed to approve auction. Please try again.");
                    }
                });
            }).start();
        });
    }

    private void handleReject(Auction auction) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Reject Auction");
        dialog.setHeaderText("Reject \"" + (auction.getItem() != null ? auction.getItem().getName() : "Auction") + "\"");

        ButtonType rejectType = new ButtonType("Reject", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(rejectType, cancelType);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));

        Label reasonLabel = new Label("Rejection reason (will be sent to the seller):");
        reasonLabel.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("e.g. Insufficient item description, poor image quality...");
        reasonArea.setPrefRowCount(4);
        reasonArea.setWrapText(true);

        Label quickLabel = new Label("Quick reasons:");
        quickLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7280;");
        FlowPane quickBtns = new FlowPane(8, 6);
        String[] quickReasons = {
                "Insufficient description",
                "Unreasonable starting price",
                "Poor image quality",
                "Violates terms of service",
                "Prohibited item"
        };
        for (String r : quickReasons) {
            Button chip = new Button(r);
            chip.setStyle("-fx-padding:3 9; -fx-font-size:11px; -fx-cursor:hand;");
            chip.getStyleClass().add("btn-secondary");
            chip.setOnAction(e -> reasonArea.setText(r));
            quickBtns.getChildren().add(chip);
        }

        content.getChildren().addAll(reasonLabel, reasonArea, quickLabel, quickBtns);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(480);

        javafx.scene.Node rejectNode = dialog.getDialogPane().lookupButton(rejectType);
        rejectNode.setDisable(true);
        reasonArea.textProperty().addListener((obs, o, n) -> rejectNode.setDisable(n.trim().isEmpty()));

        dialog.setResultConverter(btn -> btn == rejectType ? reasonArea.getText().trim() : null);

        dialog.showAndWait().ifPresent(reason -> {
            if (reason == null || reason.isEmpty()) return;

            new Thread(() -> {
                // Đảm bảo truyền đủ cả ID (Long) và Lý do (String) trùng khớp với mảng Object[] Server đang đợi
                Long idToSend = Long.valueOf(auction.getId());
                boolean success = auctionService.rejectAuction(idToSend, reason);

                Platform.runLater(() -> {
                    if (success) {
                        allPending.removeIf(a -> a.getId().equals(auction.getId()));
                        auction.setStatus(AuctionStatus.REJECTED);
                        allRejected.add(0, auction);

                        updatePendingBadge();
                        showPending(); // Cập nhật lại giao diện bảng ngay lập tức
                        showInfo("❌ Auction \"" + (auction.getItem() != null ? auction.getItem().getName() : "") + "\" has been rejected.");
                    } else {
                        showError("Failed to reject auction. Please try again.");
                    }
                });
            }).start();
        });
    }

    private void handleViewDetail(Auction auction) {
        Alert detail = new Alert(AlertType.INFORMATION);
        detail.setTitle("Auction Detail");
        detail.setHeaderText(auction.getItem() != null ? auction.getItem().getName() : "Auction Detail");
        detail.getDialogPane().setMinWidth(500);

        String body = String.format("""
                👤  Seller          : %s
                📦  Category        : %s  %s
                💰  Starting Price  : %,.0f
                📅  Start Time      : %s
                ⏰  End Time        : %s
                
                📝  Description:
                %s
                """,
                auction.getSeller() != null ? auction.getSeller().getUsername() : "N/A",
                auction.getItem() != null ? categoryIcon(auction.getItem().getCategory()) : "📦",
                auction.getItem() != null ? auction.getItem().getCategory() : "N/A",
                auction.getItem() != null ? auction.getItem().getStartingPrice() : 0.0,
                auction.getStartTime() != null ? auction.getStartTime().format(FMT) : "—",
                auction.getEndTime()   != null ? auction.getEndTime().format(FMT)   : "—",
                (auction.getItem() != null && auction.getItem().getDescription() != null)
                        ? auction.getItem().getDescription() : "(No description provided)"
        );
        detail.setContentText(body);
        detail.show();
    }

    private void updatePendingBadge() {
        int count = allPending.size();
        pendingBadge.setText(String.valueOf(count));
        pendingBadge.setVisible(count > 0);
    }

    private void showInfo(String msg) {
        Alert info = new Alert(AlertType.INFORMATION);
        info.setTitle("Admin Action");
        info.setHeaderText(null);
        info.setContentText(msg);
        info.show();
    }

    private void showError(String msg) {
        Alert err = new Alert(AlertType.ERROR);
        err.setTitle("Error");
        err.setHeaderText(null);
        err.setContentText(msg);
        err.show();
    }

    private String categoryIcon(String cat) {
        if (cat == null) return "📦";
        return switch (cat) {
            case "Art"         -> "🎨";
            case "Electronics" -> "💻";
            case "Vehicles"    -> "🚗";
            default            -> "📦";
        };
    }

    public void setAuctionService(AuctionService service) {
        this.auctionService = service;
    }
}