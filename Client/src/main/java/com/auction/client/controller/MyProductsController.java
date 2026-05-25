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
 * ĐÃ ĐƯỢC THÊM CƠ CHẾ NẠP NGẦM TRẠNG THÁI PAYMENT TỪ DATABASE ĐỂ ĐỒNG BỘ VĨNH VIỄN KHI REFRESH/SIGN OUT.
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

        // 🌟 ĐỒNG BỘ: Kiểm tra cờ RAM tập trung từ SessionManager để đổi màu COMPLETED lập tức
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

                // 🚀 ƯU TIÊN KIỂM TRA TRẠNG THÁI: Nếu cờ RAM cục bộ HOẶC data thuộc tính Payment từ DB trả về là COMPLETED
                if (SessionManager.get().isAuctionPaidLocally(auction.getId())
                        || item == com.auction.common.enums.PaymentStatus.COMPLETED) {
                    badge.setText("COMPLETED");
                    badge.setStyle("-fx-background-color: #e6f4ea; -fx-text-fill: #137333;");
                }
                // Nếu DB trả về trạng thái PENDING rõ ràng
                else if (item == com.auction.common.enums.PaymentStatus.PENDING) {
                    badge.setText("PENDING");
                    badge.setStyle("-fx-background-color: #fef7e0; -fx-text-fill: #b06000;");
                }
                // Xử lý các trường hợp còn lại dựa theo trạng thái của Auction
                else {
                    String actStatus = auction.getStatus() != null ? auction.getStatus().name() : "";

                    if ("FINISHED".equalsIgnoreCase(actStatus)) {
                        if (auction.getBidCount() == 0) {
                            badge.setText("No Buyers");
                            badge.setStyle("-fx-background-color: #f1f3f4; -fx-text-fill: #70757a;");
                        } else {
                            // Nếu đã kết thúc, có người mua mà chưa có thông tin COMPLETED thì hiển thị PENDING tạm thời trước khi luồng ngầm nạp xong
                            badge.setText("PENDING");
                            badge.setStyle("-fx-background-color: #fef7e0; -fx-text-fill: #b06000;");
                        }
                    } else {
                        // Đang diễn ra (Live) thì để dấu gạch ngang
                        badge.setText("—");
                        badge.setStyle("-fx-text-fill: #70757a; -fx-background-color: transparent;");
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

    // ── Tab handlers — Ép refresh giao diện khi đổi tab nhằm quét lại dữ liệu mới nhất ──
    @FXML public void showAll() {
        setActiveTab(tabAll);
        loadTable(myProducts);
    }

    @FXML public void showLive() {
        setActiveTab(tabLive);
        loadTable(myProducts.stream().filter(Auction::isLive).collect(Collectors.toList()));
    }

    @FXML public void showPending() {
        setActiveTab(tabPending);
        loadTable(myProducts.stream()
                .filter(a -> a.getStatus() != null && "PENDING_APPROVAL".equalsIgnoreCase(a.getStatus().name()))
                .collect(Collectors.toList()));
    }

    @FXML public void showFinished() {
        setActiveTab(tabFinished);
        loadTable(myProducts.stream().filter(a -> !a.isLive()).collect(Collectors.toList()));
    }

    @FXML public void goToCreate() {
        MainController main = (MainController) productTable
                .getScene().lookup("#mainRoot").getUserData();
        main.navCreate();
    }

    // ── Setup các cột hiển thị ─────────────────────────────────────────
    private void setupColumns() {
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colTitle.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String title, boolean empty) {
                super.updateItem(title, empty);
                if (empty || title == null) { setText(null); return; }
                setText(title);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#1a202c;");
            }
        });

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

        colStart.setCellValueFactory(c -> {
            if (c.getValue() != null && c.getValue().getItem() != null) {
                return new SimpleStringProperty("$" + String.format("%,.0f", c.getValue().getItem().getStartingPrice()));
            }
            return new SimpleStringProperty("$0");
        });

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

        colBids.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getBidCount())));

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatusLabel()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }

                Label pill = new Label(status);
                String styleClass = switch (status) {
                    case "Live"               -> "pill-running";
                    case "Pending"            -> "pill-pending";
                    case "Finished"           -> "pill-finished";
                    case "Cancelled", "Rejected" -> "pill-ending";
                    default                   -> "pill-finished";
                };

                pill.getStyleClass().add(styleClass);
                setGraphic(pill);
                setAlignment(Pos.CENTER);
            }
        });

        colEnds.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEndTimeFormatted()));

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
                    return;
                }

                Auction a = getTableView().getItems().get(getIndex());
                String status = a.getStatus() != null ? a.getStatus().name() : "";

                boolean isRejected       = "REJECTED".equalsIgnoreCase(status);
                boolean isPending        = "PENDING_APPROVAL".equalsIgnoreCase(status);
                boolean isRunningOrDone  = !isPending && !isRejected; // RUNNING, FINISHED, LIVE...

                // ── Nút View ──────────────────────────────────────────────
                viewBtn.setVisible(!isRejected);
                viewBtn.setManaged(!isRejected);

                viewBtn.setOnAction(e -> {
                    if (isPending) {
                        new Alert(Alert.AlertType.INFORMATION,
                                "Auction chưa được admin duyệt, vui lòng chờ.")
                                .showAndWait();
                    } else {
                        openDetail(a);
                    }
                });

                // ── Nút Edit ──────────────────────────────────────────────
                editBtn.setVisible(!isRejected);
                editBtn.setManaged(!isRejected);

                editBtn.setOnAction(e -> {
                    if (isRunningOrDone) {
                        new Alert(Alert.AlertType.WARNING,
                                "Không thể chỉnh sửa: Auction đã được duyệt hoặc đã kết thúc.")
                                .showAndWait();
                    } else {
                        openEdit(a);
                    }
                });

                setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });
    }

    private void loadTable(List<Auction> list) {
        if (list == null) return;

        // 🚀 THẦN THÁNH: Tạo luồng ngầm tự động liên hệ Server quét trạng thái hóa đơn gốc thực tế từ Database
        new Thread(() -> {
            try {
                for (Auction auction : list) {
                    // Nếu là phiên kết thúc FINISHED và có lượt đấu giá, tiến hành check trạng thái Payment từ DB
                    if (auction.getStatus() != null && "FINISHED".equalsIgnoreCase(auction.getStatus().name()) && auction.getBidCount() > 0) {
                        com.auction.common.dto.PaymentDTO p = auctionService.getPaymentByAuctionId(auction.getId());
                        if (p != null && "COMPLETED".equalsIgnoreCase(p.getStatus())) {
                            // Gán thẳng trạng thái COMPLETED vào model để hiển thị màu xanh chuẩn chỉ vĩnh viễn
                            auction.setPaymentStatus(com.auction.common.enums.PaymentStatus.COMPLETED);
                        }
                    }
                }
                // Đồng bộ cập nhật lại đồ họa UI một lần duy nhất sau khi nạp xong dữ liệu DB thực
                javafx.application.Platform.runLater(() -> productTable.refresh());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        ObservableList<Auction> obs = FXCollections.observableArrayList(list);
        productTable.setItems(obs);
        productTable.refresh(); // Ép TableView xóa cache đồ họa cũ
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