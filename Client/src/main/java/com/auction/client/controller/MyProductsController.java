package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.PaymentStatus;
import com.auction.common.model.*;
import com.auction.common.observer.Observer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MyProductsController — Quản lý sản phẩm đấu giá của người bán.
 * Dùng TableView với các cột đầy đủ, filter theo trạng thái độc lập.
 */
public class MyProductsController {

    @FXML private Button tabAll, tabLive, tabPending, tabFinished;
    @FXML private Label  productCount;

    @FXML private TableView<Auction>         productTable;
    @FXML private TableColumn<Auction,String>  colTitle, colCategory,
            colStart, colCurrent, colBids, colStatus, colEnds, colAction;

    private final AuctionService auctionService = new AuctionService();
    private User currentUser;
    private List<Auction> myProducts;
    private TableColumn<Auction, com.auction.common.enums.PaymentStatus> colPaymentStatus;

    @FXML
    public void initialize() {
        // 1. Khởi tạo cột độc lập bằng code hiển thị "PAYMENT STATUS"
        colPaymentStatus = new TableColumn<>("PAYMENT STATUS");
        colPaymentStatus.setPrefWidth(140);

        // 2. Định nghĩa lấy dữ liệu chính xác từ Enum paymentStatus của Auction
        colPaymentStatus.setCellValueFactory(c -> {
            if (c.getValue() == null) return null;
            return new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getPaymentStatus());
        });
        // 🌟 THAY THẾ ĐOẠN CELL FACTORY CỦA colPaymentStatus TRONG MyProductsController.java

        colPaymentStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(com.auction.common.enums.PaymentStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }

                Auction auction = getTableView().getItems().get(getIndex());
                if (auction == null) { setGraphic(null); return; }

                Label badge = new Label();
                badge.setPadding(new javafx.geometry.Insets(4, 8, 4, 8));
                badge.setStyle("-fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");

                // 🌟 TRƯỜNG HỢP 1: BẢNG payment ĐÃ CÓ DỮ LIỆU (Được load lên qua LEFT JOIN chuẩn)
                if (item != null) {
                    switch (item) {
                        case PENDING:
                            badge.setText("PENDING");
                            badge.setStyle(badge.getStyle() + "-fx-background-color: #fef7e0; -fx-text-fill: #b06000;");
                            break;
                        case COMPLETED:
                            badge.setText("COMPLETED");
                            badge.setStyle(badge.getStyle() + "-fx-background-color: #e6f4ea; -fx-text-fill: #137333;");
                            break;
                        case FAILED:
                            badge.setText("FAILED");
                            badge.setStyle(badge.getStyle() + "-fx-background-color: #fce8e6; -fx-text-fill: #c5221f;");
                            break;
                        default:
                            badge.setText(item.toString());
                            badge.setStyle(badge.getStyle() + "-fx-background-color: #f1f3f4; -fx-text-fill: #3c4043;");
                            break;
                    }
                }
                // 🌟 TRƯỜNG HỢP 2: BẢNG payment TRẢ VỀ NULL (Do database cũ hoặc chưa kịp cập nhật)
                else {
                    String actStatus = auction.getStatus() != null ? auction.getStatus().name() : "";

                    // Kiểm tra xem phiên đấu giá thực sự đã khép lại chưa (bao gồm cả trạng thái FINISHED hoặc PAID)
                    if ("FINISHED".equalsIgnoreCase(actStatus) || "PAID".equalsIgnoreCase(actStatus)) {
                        if (auction.getBidCount() == 0) {
                            badge.setText("No Buyers"); // Thực sự kết thúc mà không có ai đặt giá
                            badge.setStyle(badge.getStyle() + "-fx-background-color: #f1f3f4; -fx-text-fill: #70757a;");
                        } else {
                            // Nếu trạng thái của phiên đấu giá vốn dĩ đã là PAID, hiển thị luôn chữ COMPLETED màu xanh
                            if ("PAID".equalsIgnoreCase(actStatus)) {
                                badge.setText("COMPLETED");
                                badge.setStyle(badge.getStyle() + "-fx-background-color: #e6f4ea; -fx-text-fill: #137333;");
                            } else {
                                // Ngược lại nếu mới chỉ FINISHED và có người mua, chắc chắn là đang chờ tiền PENDING
                                badge.setText("PENDING");
                                badge.setStyle(badge.getStyle() + "-fx-background-color: #fef7e0; -fx-text-fill: #b06000;");
                            }
                        }
                    } else {
                        // Phiên đấu giá đang diễn ra (Live, Pending_Approval...) thì không cần hiển thị trạng thái tiền bạc
                        badge.setText("—");
                        badge.setStyle(badge.getStyle() + "-fx-text-fill: #70757a; -fx-background-color: transparent;");
                    }
                }

                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });


        // 4. Cấu hình định dạng cho toàn bộ cột cũ
        setupColumns();

        // 5. Đẩy cột Payment Status vào đứng trước cột Action
        int indexAction = productTable.getColumns().indexOf(colAction);
        if (indexAction >= 0) {
            productTable.getColumns().add(indexAction, colPaymentStatus);
        } else {
            productTable.getColumns().add(colPaymentStatus);
        }
    }

    public void initData(User user) {
        this.currentUser = user;

        new Thread(() -> {
            List<Auction> fetchedProducts = auctionService.getAuctionsBySeller(user.getId());

            javafx.application.Platform.runLater(() -> {
                this.myProducts = fetchedProducts;
                if (this.myProducts == null) {
                    this.myProducts = new java.util.ArrayList<>();
                }
                productCount.setText(this.myProducts.size() + " products");
                setActiveTab(tabAll);
                loadTable(this.myProducts);
            });
        }).start();
    }

    // ── Tab handlers ──────────────────────────────────────────
    @FXML public void showAll()      { setActiveTab(tabAll);      loadTable(myProducts); }
    @FXML public void showLive()     { setActiveTab(tabLive);
        loadTable(myProducts.stream().filter(Auction::isLive).collect(Collectors.toList())); }
    @FXML public void showPending()  {
        setActiveTab(tabPending);
        loadTable(myProducts.stream()
                .filter(a -> a.getStatus() != null && "PENDING_APPROVAL".equalsIgnoreCase(a.getStatus().name()))
                .collect(Collectors.toList()));
    }
    @FXML public void showFinished() {
        setActiveTab(tabFinished);
        // Lọc tất cả các sản phẩm không còn Live (Bao gồm cả FINISHED và PAID)
        loadTable(myProducts.stream().filter(a -> !a.isLive()).collect(Collectors.toList()));
    }
    @FXML public void goToCreate() {
        MainController main = (MainController) productTable
                .getScene().lookup("#mainRoot").getUserData();
        main.navCreate();
    }

    // ── Setup columns ─────────────────────────────────────────
    private void setupColumns() {
        // Title column
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colTitle.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String title, boolean empty) {
                super.updateItem(title, empty);
                if (empty || title == null) { setText(null); return; }
                setText(title);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#1a202c;");
            }
        });

        // Category
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCategory()));
        colCategory.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) { setGraphic(null); return; }
                Label badge = new Label(cat);
                badge.getStyleClass().addAll("badge", "badge-blue");
                setGraphic(badge);
                setAlignment(Pos.CENTER);
            }
        });

        // Start price
        colStart.setCellValueFactory(c -> {
            if (c.getValue() != null && c.getValue().getItem() != null) {
                return new SimpleStringProperty("$" + String.format("%,.0f", c.getValue().getItem().getStartingPrice()));
            }
            return new SimpleStringProperty("$0");
        });

        // Current bid
        colCurrent.setCellValueFactory(c ->
                new SimpleStringProperty("$" + String.format("%,.0f", c.getValue().getCurrentPrice())));
        colCurrent.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) { setText(null); return; }
                setText(price);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#2563eb;");
            }
        });

        // Bids count
        colBids.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getBidCount())));

        // Status badge - Sửa đổi các case đồng bộ khít với hàm getStatusLabel() của class Auction
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatusLabel()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }

                Label pill = new Label(status);
                String styleClass = switch (status) {
                    case "Live"               -> "pill-running";   // Màu xanh lá
                    case "Pending"            -> "pill-pending";   // Màu vàng cam
                    case "Finished"           -> "pill-finished";  // Màu xám (Cho cả phiên đã trả tiền hoặc hết giờ)
                    case "Cancelled", "Rejected" -> "pill-ending"; // Màu đỏ
                    default                   -> "pill-finished";
                };

                pill.getStyleClass().add(styleClass);
                setGraphic(pill);
                setAlignment(Pos.CENTER);
            }
        });

        // End time
        colEnds.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEndTimeFormatted()));

        // Action buttons
        colAction.setCellFactory(col -> new TableCell<Auction,String>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final HBox   box     = new HBox(6, viewBtn, editBtn);

            {
                viewBtn.getStyleClass().add("btn-ghost");
                editBtn.getStyleClass().add("btn-secondary");
                viewBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                editBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                box.setAlignment(Pos.CENTER);

                viewBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    openDetail(a);
                });
                editBtn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    openEdit(a);
                });
            }

            @Override
            public void updateItem(String o, boolean empty) {
                super.updateItem(o, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });
    }

    private void loadTable(List<Auction> list) {
        ObservableList<Auction> obs = FXCollections.observableArrayList(list);
        productTable.setItems(obs);
    }

    private void setActiveTab(Button tab) {
        List.of(tabAll, tabLive, tabPending, tabFinished)
                .forEach(b -> b.getStyleClass().setAll("btn-secondary"));
        tab.getStyleClass().setAll("btn-primary");
    }

    private void openDetail(Auction a) {
        MainController main = (MainController) productTable
                .getScene().lookup("#mainRoot").getUserData();
        main.loadContent("/com/auction/client/auction_detail.fxml",
                (AuctionDetailController ctrl) -> ctrl.initData(currentUser, a));
    }

    private void openEdit(Auction a) {
        MainController main = (MainController) productTable
                .getScene().lookup("#mainRoot").getUserData();
        main.loadContent("/com/auction/client/create_auction.fxml",
                (CreateAuctionController ctrl) -> ctrl.initEdit(currentUser, a));
    }
}