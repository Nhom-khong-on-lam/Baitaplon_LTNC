package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.client.service.ServerConnection;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import com.auction.common.network.Request;
import com.auction.common.network.Response;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * CreateAuctionController — Form tạo và chỉnh sửa phiên đấu giá.
 * ĐÃ KHÔI PHỤC GIỮ NGUYÊN HOÀN TOÀN CÁC HÀM BẢN CŨ & LOẠI BỎ ICON EMOJI.
 */
public class CreateAuctionController {

    // ── Danh sách category DUY NHẤT trong toàn hệ thống ──────
    public static final java.util.List<String> CATEGORIES =
            java.util.List.of("Electronics", "Art", "Vehicle");

    @FXML private Label    pageTitle;
    @FXML private TextField fieldTitle, fieldStartPrice, fieldReservePrice,
            fieldBidIncrement, fieldStartTime, fieldEndTime;
    @FXML private TextArea  fieldDesc;
    @FXML private ComboBox<String> fieldCategory, fieldCondition;
    @FXML private Label    msgTitle, msgDesc, msgCategory, msgPrice,
            msgTime, msgImage, formMsg;
    @FXML private Button   submitBtn;
    @FXML private VBox     imagePreviewBox;
    @FXML private Label    imageHint; // Label hiển thị tên file ảnh thay thế cho Icon cũ

    private final AuctionService auctionService = new AuctionService();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private User    currentUser;
    private Auction editingAuction; // null = create mode
    private String  selectedImagePath; // Đường dẫn tuyệt đối của ảnh thật

    @FXML
    public void initialize() {
        // Cấu hình dữ liệu cho ComboBox
        fieldCategory.setItems(FXCollections.observableArrayList(CATEGORIES));
        fieldCondition.setItems(FXCollections.observableArrayList(
                "New", "Like New", "Excellent", "Good", "Fair", "For Parts"));

        // Giá trị thời gian mặc định
        fieldStartTime.setText(LocalDateTime.now().plusMinutes(5).format(FMT));
        fieldEndTime.setText(LocalDateTime.now().plusDays(3).format(FMT));

        // Xóa thông báo lỗi thời gian thực
        fieldTitle.textProperty().addListener((o, v, n) -> msgTitle.setText(""));
        fieldStartPrice.textProperty().addListener((o, v, n) -> msgPrice.setText(""));
        fieldEndTime.textProperty().addListener((o, v, n) -> msgTime.setText(""));
        fieldCategory.valueProperty().addListener((o, v, n) -> msgCategory.setText(""));
    }

    // ── Create mode ───────────────────────────────────────────
    public void initData(User user) {
        this.currentUser    = user;
        this.editingAuction = null;
        pageTitle.setText("Create Auction");
        submitBtn.setText("Publish Auction →");
        clearForm();
    }

    // ── Edit mode — điền sẵn dữ liệu ─────────────────────────
    public void initEdit(User user, Auction auction) {
        this.currentUser    = user;
        this.editingAuction = auction;
        pageTitle.setText("Edit Auction");
        submitBtn.setText("Save Changes →");

        fieldTitle.setText(auction.getTitle());
        fieldDesc.setText(auction.getDescription());
        fieldStartPrice.setText(String.valueOf((int) auction.getItem().getStartingPrice()));
        fieldEndTime.setText(auction.getEndTimeFormatted());

        // Đã xóa Icon Emoji gán vào imageIcon, chỉ hiển thị thông báo text
        imageHint.setText("Image loaded from existing auction");

        setNormalizedCategory(auction.getCategory());
    }

    private void setNormalizedCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            fieldCategory.setValue(null);
            return;
        }
        String matched = fieldCategory.getItems().stream()
                .filter(item -> item.equalsIgnoreCase(rawCategory.trim()))
                .findFirst()
                .orElse(CATEGORIES.get(0));
        fieldCategory.setValue(matched);
    }

    // ── Quick duration buttons ────────────────────────────────
    @FXML public void set1Day()  { setEndFromNow(1); }
    @FXML public void set3Days() { setEndFromNow(3); }
    @FXML public void set7Days() { setEndFromNow(7); }

    private void setEndFromNow(int days) {
        fieldEndTime.setText(LocalDateTime.now().plusDays(days).format(FMT));
        msgTime.setText("");
    }

    // ── Image upload (Xử lý chọn file ảnh thật) ──────────────────
    @FXML public void handleImageUpload() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Product Image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        File file = fc.showOpenDialog(imagePreviewBox.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();

            // Chỉ cập nhật nhãn tên file, ĐÃ BỎ hoàn toàn dòng gán "imageIcon.setText" cũ
            imageHint.setText("Selected: " + file.getName());

            imagePreviewBox.setStyle(imagePreviewBox.getStyle()
                    .replace("border-style:dashed", "border-style:solid")
                    + " -fx-border-color:#2563eb;");
            msgImage.setText("");
        }
    }

    // ── Save draft ────────────────────────────────────────────
    @FXML public void saveDraft() {
        if (!validateBasic()) return;
        formMsg.setText("✓ Draft saved successfully.");
        formMsg.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
        AnimationUtil.pulse(submitBtn);
    }

    // ── Submit (GIỮ NGUYÊN TÊN HÀM VÀ THAM SỐ GỐC 100%) ───────
    @FXML
    public void handleSubmit() {
        if (!validateAll()) return;

        // 1. Lấy dữ liệu trên luồng UI trước khi mở luồng phụ
        final String        title      = fieldTitle.getText().trim();
        final String        desc       = fieldDesc.getText().trim();
        final String        category   = fieldCategory.getValue() != null ? fieldCategory.getValue() : CATEGORIES.get(0);
        final String        condition  = fieldCondition.getValue() == null ? "Good" : fieldCondition.getValue();
        final double        startPrice = Double.parseDouble(fieldStartPrice.getText().trim());
        final double        reserve    = parseOptionalDouble(fieldReservePrice.getText());
        final double        increment  = parseOptionalDouble(fieldBidIncrement.getText());
        final LocalDateTime start      = parseDateTime(fieldStartTime.getText());
        final LocalDateTime end        = parseDateTime(fieldEndTime.getText());

        // Đường dẫn ảnh thật, nếu chưa chọn ảnh thì fallback về chuỗi mặc định
        final String        imagePath  = (selectedImagePath != null) ? selectedImagePath : "default.png";

        final boolean       isEdit     = (editingAuction != null);
        final long          editId     = isEdit ? editingAuction.getId() : -1;

        // 2. Khóa UI
        submitBtn.setDisable(true);
        formMsg.setText("⏳ Processing...");
        formMsg.setStyle("-fx-text-fill:#d97706;");

        // 3. Chạy tác vụ mạng trên Thread phụ
        Thread worker = new Thread(() -> {
            boolean success;
            try {
                if (!isEdit) {
                    Object[] data = new Object[8];
                    data[0] = currentUser;                                         // Vị trí 0
                    data[1] = title;                                               // Vị trí 1
                    data[2] = desc;                                                // Vị trí 2
                    data[3] = category;                                            // Vị trí 3
                    data[4] = condition;                                           // Vị trí 4
                    data[5] = startPrice;                                          // Vị trí 5
                    data[6] = (start != null) ? start.toString() : LocalDateTime.now().toString(); // Vị trí 6 (String)
                    data[7] = (end != null) ? end.toString() : "";                 // Vị trí 7 (String)
                    data[8] = imagePath;
                    // Tạo request với mảng chuẩn hóa mới gửi thẳng lên Server
                    Request req = new Request(Request.CREATE_AUCTION, data);

                    // Gửi request thông qua Service mạng gốc
                   Response res = (Response) ServerConnection.getInstance().sendRequest(req);
                    // Lưu ý: Nếu hàm createAuction(req) báo đỏ, bạn có thể gọi thẳng:
                    // com.auction.common.network.Response res = (com.auction.common.network.Response) com.auction.client.network.ServerConnection.getInstance().sendRequest(req);

                    success = res != null && res.isSuccess();
                } else {
                    // 🚨 GIỮ NGUYÊN GỐC 100% CÁC THAM SỐ CỦA HÀM UPDATE BẢN CŨ
                    com.auction.common.network.Response res =
                            auctionService.updateAuction(editId, title, desc,
                                    category, startPrice, end);
                    success = res != null && res.isSuccess();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                success = false;
            }

            final boolean ok = success;

            // 4. Trả kết quả về UI thread
            Platform.runLater(() -> {
                submitBtn.setDisable(false);
                if (ok) {
                    showSuccess(isEdit ? " Auction updated successfully!" : " Auction published successfully!");
                    AnimationUtil.pulse(submitBtn);

                    javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1200));
                    p.setOnFinished(e -> {
                        MainController main = (MainController) submitBtn.getScene().lookup("#mainRoot").getUserData();

                        try {
                            Node myProductsNode = submitBtn.getScene().lookup("#productTable");
                            if (myProductsNode != null && myProductsNode.getUserData() instanceof MyProductsController) {
                                MyProductsController myProductsCtrl = (MyProductsController) myProductsNode.getUserData();
                                myProductsCtrl.initData(main.getCurrentUser());
                            }
                        } catch (Exception ex) {
                            System.out.println("Refreshing products list view...");
                        }

                        main.navMyProducts();
                    });
                    p.play();
                } else {
                    formMsg.setText("✕ An error occurred. Please try again.");
                    formMsg.setStyle("-fx-text-fill:#dc2626;");
                }
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    @FXML public void goBack() {
        MainController main = (MainController) submitBtn.getScene().lookup("#mainRoot").getUserData();
        main.navMyProducts();
    }

    // ── Validation ────────────────────────────────────────────
    private boolean validateBasic() {
        boolean ok = true;
        if (fieldTitle.getText().trim().isEmpty()) {
            msgTitle.setText("Title is required.");
            AnimationUtil.shake(fieldTitle);
            ok = false;
        }
        if (fieldStartPrice.getText().trim().isEmpty()) {
            msgPrice.setText("Start price is required.");
            AnimationUtil.shake(fieldStartPrice);
            ok = false;
        } else {
            try {
                double val = Double.parseDouble(fieldStartPrice.getText().trim());
                if (val <= 0) {
                    msgPrice.setText("Price must be greater than 0.");
                    AnimationUtil.shake(fieldStartPrice);
                    ok = false;
                }
            } catch (NumberFormatException e) {
                msgPrice.setText("Enter a valid number.");
                AnimationUtil.shake(fieldStartPrice);
                ok = false;
            }
        }
        return ok;
    }

    private boolean validateAll() {
        if (!validateBasic()) return false;
        boolean ok = true;

        if (fieldDesc.getText().trim().isEmpty()) {
            msgDesc.setText("Description is required.");
            ok = false;
        }
        if (fieldCategory.getValue() == null) {
            msgCategory.setText("Select a category.");
            AnimationUtil.shake(fieldCategory);
            ok = false;
        }
        // Thêm bắt buộc chọn ảnh thật khi tạo mới
        if (selectedImagePath == null && editingAuction == null) {
            msgImage.setText("Please upload a product image.");
            ok = false;
        }
        LocalDateTime end = parseDateTime(fieldEndTime.getText());
        if (end == null) {
            msgTime.setText("End time format: YYYY-MM-DD HH:mm");
            AnimationUtil.shake(fieldEndTime);
            ok = false;
        } else if (end.isBefore(LocalDateTime.now().plusMinutes(30))) {
            msgTime.setText("End time must be at least 30 min from now.");
            ok = false;
        }
        if (!ok) {
            formMsg.setText("Please fix the errors above.");
            formMsg.setStyle("-fx-text-fill:#dc2626;");
            AnimationUtil.shake(submitBtn);
        }
        return ok;
    }

    private void showSuccess(String msg) {
        formMsg.setText(msg);
        formMsg.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDateTime.parse(text.trim(), FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private double parseOptionalDouble(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void clearForm() {
        fieldTitle.clear();
        fieldDesc.clear();
        fieldStartPrice.clear();
        fieldBidIncrement.clear();
        if (fieldReservePrice != null) fieldReservePrice.clear();
        selectedImagePath = null;
        imageHint.setText("No file selected (.png, .jpg, .webp)");
        imagePreviewBox.setStyle("-fx-border-style: dashed; -fx-border-color: #cbd5e1; -fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-color: #f8fafc;");
    }
}