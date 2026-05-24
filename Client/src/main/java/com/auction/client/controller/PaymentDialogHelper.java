package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.dto.PaymentDTO;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import com.auction.common.network.Response;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class PaymentDialogHelper {

    public interface PaymentCallback {
        void onPaymentSuccess();
    }

    public static void showPaymentDialog(User currentUser, Auction auction, PaymentCallback callback) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED); // Modern borderless look

        VBox root = new VBox(20);
        root.getStyleClass().add("custom-dialog");
        root.setPadding(new Insets(24));
        root.setPrefWidth(380);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #cbd5e0; -fx-border-width: 1.5; -fx-border-radius: 12;");

        // Title
        Label titleLbl = new Label("AURUM SECURE PAYMENT");
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1a365d; -fx-letter-spacing: 1px;");
        titleLbl.setAlignment(Pos.CENTER);

        // Subtitle
        Label subLbl = new Label("Thực hiện chuyển tiền cho người bán");
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #718096;");
        subLbl.setAlignment(Pos.CENTER);

        VBox header = new VBox(4, titleLbl, subLbl);
        header.setAlignment(Pos.CENTER);

        // Divider
        Separator sep = new Separator();

        // Details Form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 10, 0));

        Label itemLabel = new Label("Sản phẩm:");
        itemLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
        Label itemVal = new Label(auction.getTitle());
        itemVal.setWrapText(true);
        itemVal.setMaxWidth(220);

        Label sellerLabel = new Label("Người bán:");
        sellerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
        Label sellerVal = new Label(auction.getSeller().getUsername());

        Label amountLabel = new Label("Số tiền:");
        amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
        Label amountVal = new Label("Đang tải...");
        amountVal.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");

        Label methodLabel = new Label("Phương thức:");
        methodLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
        Label methodVal = new Label("Chuyển khoản Ngân hàng");

        Label bankLabel = new Label("Ngân hàng nhận:");
        bankLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
        Label bankVal = new Label("Đang tải...");
        bankVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        Label accountLabel = new Label("Số tài khoản:");
        accountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
        Label accountVal = new Label("Đang tải...");
        accountVal.setStyle("-fx-font-family: monospace; -fx-font-weight: bold; -fx-text-fill: #2d3748;");

        Label cardholderLabel = new Label("Tên chủ thẻ:");
        cardholderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568;");
        Label cardholderVal = new Label("Đang tải...");
        cardholderVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748;");

        grid.add(itemLabel, 0, 0);
        grid.add(itemVal, 1, 0);
        grid.add(sellerLabel, 0, 1);
        grid.add(sellerVal, 1, 1);
        grid.add(amountLabel, 0, 2);
        grid.add(amountVal, 1, 2);
        grid.add(methodLabel, 0, 3);
        grid.add(methodVal, 1, 3);
        grid.add(bankLabel, 0, 4);
        grid.add(bankVal, 1, 4);
        grid.add(accountLabel, 0, 5);
        grid.add(accountVal, 1, 5);
        grid.add(cardholderLabel, 0, 6);
        grid.add(cardholderVal, 1, 6);

        // Buttons
        Button payBtn = new Button("Xác nhận thanh toán");
        payBtn.getStyleClass().add("btn-primary");
        payBtn.setMaxWidth(Double.MAX_VALUE);
        payBtn.setStyle("-fx-background-color: #10b981; -fx-pref-height: 40px; -fx-font-weight: bold;");

        Button cancelBtn = new Button("Hủy bỏ");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setStyle("-fx-pref-height: 40px;");

        HBox actions = new HBox(12, cancelBtn, payBtn);
        actions.setAlignment(Pos.CENTER);
        HBox.setHgrow(cancelBtn, Priority.ALWAYS);
        HBox.setHgrow(payBtn, Priority.ALWAYS);

        // Status/Error Message Label
        Label statusMsg = new Label();
        statusMsg.setStyle("-fx-font-size: 11px;");
        statusMsg.setVisible(false);
        statusMsg.setManaged(false);

        root.getChildren().addAll(header, sep, grid, statusMsg, actions);

        cancelBtn.setOnAction(e -> dialog.close());

        payBtn.setDisable(true);
        cancelBtn.setDisable(true);
        statusMsg.setText("Đang tải thông tin hóa đơn...");
        statusMsg.setStyle("-fx-text-fill: #d97706;");
        statusMsg.setVisible(true);
        statusMsg.setManaged(true);

        final PaymentDTO[] loadedPayment = new PaymentDTO[1];

        new Thread(() -> {
            try {
                AuctionService service = new AuctionService();
                PaymentDTO p = service.getPaymentByAuctionId(auction.getId());
                javafx.application.Platform.runLater(() -> {
                    if (p != null) {
                        loadedPayment[0] = p;
                        amountVal.setText(String.format("%,.0f VND", p.getAmount()));
                        bankVal.setText(p.getSellerBankName() != null ? p.getSellerBankName() : "Chưa liên kết");
                        accountVal.setText(p.getSellerAccountNumber() != null ? p.getSellerAccountNumber() : "Chưa liên kết");
                        cardholderVal.setText(p.getSellerCardholderName() != null ? p.getSellerCardholderName() : "Chưa liên kết");

                        payBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                        statusMsg.setVisible(false);
                        statusMsg.setManaged(false);
                    } else {
                        statusMsg.setText("✕ Không thể tải thông tin hóa đơn.");
                        statusMsg.setStyle("-fx-text-fill: #ef4444;");
                        cancelBtn.setDisable(false);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    statusMsg.setText("✕ Lỗi kết nối hệ thống.");
                    statusMsg.setStyle("-fx-text-fill: #ef4444;");
                    cancelBtn.setDisable(false);
                });
            }
        }).start();

        payBtn.setOnAction(e -> {
            if (loadedPayment[0] == null) return;
            payBtn.setDisable(true);
            cancelBtn.setDisable(true);
            statusMsg.setText("Đang xử lý giao dịch...");
            statusMsg.setStyle("-fx-text-fill: #d97706;");
            statusMsg.setVisible(true);
            statusMsg.setManaged(true);

            new Thread(() -> {
                try {
                    AuctionService service = new AuctionService();
                    Response res = service.createPayment(loadedPayment[0]);

                    javafx.application.Platform.runLater(() -> {
                        if (res != null && res.isSuccess()) {
                            statusMsg.setText("✓ Thanh toán thành công!");
                            statusMsg.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");

                            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(800));
                            pause.setOnFinished(evt -> {
                                dialog.close();
                                if (callback != null) {
                                    callback.onPaymentSuccess();
                                }
                            });
                            pause.play();
                        } else {
                            statusMsg.setText("✕ Lỗi: " + (res != null ? res.getMessage() : "Không thể kết nối Server"));
                            statusMsg.setStyle("-fx-text-fill: #ef4444;");
                            payBtn.setDisable(false);
                            cancelBtn.setDisable(false);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        statusMsg.setText("✕ Lỗi kết nối hệ thống.");
                        statusMsg.setStyle("-fx-text-fill: #ef4444;");
                        payBtn.setDisable(false);
                        cancelBtn.setDisable(false);
                    });
                }
            }).start();
        });

        Scene scene = new Scene(root);
        String cssPath = PaymentDialogHelper.class.getResource("/com/auction/client/global.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        dialog.setScene(scene);
        dialog.centerOnScreen();
        dialog.showAndWait();
    }
}