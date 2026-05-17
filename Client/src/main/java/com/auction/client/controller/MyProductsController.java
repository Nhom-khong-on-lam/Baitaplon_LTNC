package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.model.Auction;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import com.auction.common.model.User;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MyProductsController — Quản lý sản phẩm đấu giá của người bán.
 *
 * FIX:
 *  - Thread daemon(true) trên loader
 *  - Error handling khi load thất bại
 */
public class MyProductsController {

    @FXML private Button tabAll, tabLive, tabPending, tabFinished;
    @FXML private Label  productCount;

    @FXML private TableView<Auction>          productTable;
    @FXML private TableColumn<Auction,String> colThumb, colTitle, colCategory,
            colStart, colCurrent, colBids, colStatus, colEnds, colAction;

    private final AuctionService auctionService = new AuctionService();
    private User          currentUser;
    private List<Auction> myProducts;

    @FXML
    public void initialize() {
        setupColumns();
        productTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && currentUser != null) initData(currentUser);
        });
    }

    public void initData(User user) {
        this.currentUser = user;
        productCount.setText("Loading...");

        // ── BỔ SUNG 1: Xóa sạch dữ liệu cũ trên bảng hiển thị để tránh bị đơ UI ──
        productTable.setItems(FXCollections.observableArrayList());
        // ──────────────────────────────────────────────────────────────────────

        Thread loader = new Thread(() -> {
            List<Auction> fetched;
            try {
                // Đợi 250ms cho Server ổn định Transaction commit
                Thread.sleep(250);
                fetched = auctionService.getAuctionsBySeller(user.getId()); //
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> //
                        productCount.setText("⚠ Failed to load")); //
                return;
            }

            javafx.application.Platform.runLater(() -> { //
                this.myProducts = fetched != null ? fetched //
                        : new java.util.ArrayList<>(); //
                productCount.setText(myProducts.size() + " products"); //
                setActiveTab(tabAll); //

                // Nạp dữ liệu mới vào bảng
                loadTable(myProducts); //

                // ── BỔ SUNG 2: Ép JavaFX phải vẽ lại (Redraw) toàn bộ các dòng trên giao diện ──
                productTable.refresh();
                // ─────────────────────────────────────────────────────────────────────────────
            });
        });
        loader.setDaemon(true); //
        loader.start(); //
    }

    @FXML public void showAll() {
        setActiveTab(tabAll);
        loadTable(myProducts);
    }

    @FXML public void showLive() {
        setActiveTab(tabLive);
        loadTable(myProducts.stream().filter(Auction::isLive)
                .collect(Collectors.toList()));
    }

    @FXML public void showPending() {
        setActiveTab(tabPending);
        loadTable(myProducts.stream()
                .filter(a -> "Pending".equals(a.getStatus()))
                .collect(Collectors.toList()));
    }

    @FXML public void showFinished() {
        setActiveTab(tabFinished);
        loadTable(myProducts.stream().filter(a -> !a.isLive())
                .collect(Collectors.toList()));
    }

    @FXML public void goToCreate() {
        MainController main = (MainController) productTable
                .getScene().lookup("#mainRoot").getUserData();
        main.navCreate();
    }

    private void setupColumns() {
        colThumb.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategoryIcon()));
        colThumb.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String icon, boolean empty) {
                super.updateItem(icon, empty);
                if (empty || icon == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(icon);
                lbl.setStyle("-fx-font-size:20px;");
                setGraphic(lbl);
                setAlignment(Pos.CENTER);
            }
        });

        colTitle.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getTitle()));
        colTitle.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String title, boolean empty) {
                super.updateItem(title, empty);
                if (empty || title == null) { setText(null); return; }
                setText(title);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#1a202c;");
            }
        });

        colCategory.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategory()));
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

        colStart.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.format("%,.0f", c.getValue().getItem().getStartingPrice())));

        colCurrent.setCellValueFactory(c ->
                new SimpleStringProperty(
                        String.format("%,.0f", c.getValue().getCurrentPrice())));
        colCurrent.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) { setText(null); return; }
                setText(price);
                setStyle("-fx-font-weight:bold; -fx-text-fill:#2563eb;");
            }
        });

        colBids.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getBidCount())));

        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatusLabel()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                Label pill = new Label(status);
                String cls = switch (status) {
                    case "Live"     -> "pill-running";
                    case "Pending"  -> "pill-pending";
                    case "Finishing"-> "pill-ending";
                    default         -> "pill-finished";
                };
                pill.getStyleClass().add(cls);
                setGraphic(pill);
                setAlignment(Pos.CENTER);
            }
        });

        colEnds.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEndTimeFormatted()));

        colAction.setCellFactory(col -> new TableCell<Auction, String>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final HBox   box     = new HBox(6, viewBtn, editBtn);

            {
                viewBtn.getStyleClass().add("btn-ghost");
                editBtn.getStyleClass().add("btn-secondary");
                viewBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                editBtn.setStyle("-fx-padding:4 10; -fx-font-size:11px;");
                box.setAlignment(Pos.CENTER);

                viewBtn.setOnAction(e -> openDetail(
                        getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> openEdit(
                        getTableView().getItems().get(getIndex())));
            }

            @Override public void updateItem(String o, boolean empty) {
                super.updateItem(o, empty);
                if (empty) { setGraphic(null); setText(null); return; }
                setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });
    }

    private void loadTable(List<Auction> list) {
        ObservableList<Auction> obs = FXCollections.observableArrayList(list);
        productTable.setItems(obs);
        AnimationUtil.fadeIn(productTable, 300);
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