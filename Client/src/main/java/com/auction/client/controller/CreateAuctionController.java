package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * CreateAuctionController — Form tạo và chỉnh sửa phiên đấu giá.
 * Hỗ trợ 2 chế độ: Create mới hoặc Edit auction có sẵn.
 */
public class CreateAuctionController {

    @FXML private Label    pageTitle;
    @FXML private TextField fieldTitle, fieldStartPrice, fieldReservePrice,
            fieldStartTime, fieldEndTime;
    @FXML private TextArea  fieldDesc;
    @FXML private ComboBox<String> fieldCategory, fieldCondition;
    @FXML private Label    msgTitle, msgDesc, msgCategory, msgPrice,
            msgTime, msgImage, formMsg;
    @FXML private Button   submitBtn;
    @FXML private VBox     imagePreviewBox;
    @FXML private Label    imageIcon, imageHint;

    private final AuctionService auctionService = new AuctionService();
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private User currentUser;
    private Auction editingAuction; // null = create mode
    private File selectedImageFile;

    @FXML
    public void initialize() {
        fieldCategory.setItems(FXCollections.observableArrayList(
                "Electronics", "Art", "Vehicle"));

        fieldCondition.setItems(FXCollections.observableArrayList(
                "New", "Like New", "Excellent", "Good", "Fair", "For Parts"));

        // Default start time = now + 5 min
        fieldStartTime.setText(LocalDateTime.now().plusMinutes(5)
                .format(FMT));
        // Default end = now + 3 days
        fieldEndTime.setText(LocalDateTime.now().plusDays(3)
                .format(FMT));

        // Real-time clear errors
        fieldTitle.textProperty().addListener((o, v, n) -> msgTitle.setText(""));
        fieldStartPrice.textProperty().addListener((o, v, n) -> msgPrice.setText(""));
        fieldEndTime.textProperty().addListener((o, v, n) -> msgTime.setText(""));
    }

    /** Create mode */
    public void initData(User user) {
        this.currentUser = user;
        this.editingAuction = null;
        pageTitle.setText("Create Auction");
        submitBtn.setText("Publish Auction →");
    }

    /** Edit mode — điền sẵn dữ liệu */
    public void initEdit(User user, Auction auction) {
        this.currentUser    = user;
        this.editingAuction = auction;
        pageTitle.setText("Edit Auction");
        submitBtn.setText("Save Changes →");

        fieldTitle.setText(auction.getTitle());
        fieldDesc.setText(auction.getDescription());
        fieldCategory.setValue(auction.getCategory());
        fieldStartPrice.setText(String.valueOf((int) auction.getItem().getStartingPrice()));
        fieldEndTime.setText(auction.getEndTimeFormatted());
        imageHint.setText("Image uploaded ✓");
        imageIcon.setText("🖼");
    }

    // ── Quick duration buttons ────────────────────────────────
    @FXML public void set1Day()  { setEndFromNow(1); }
    @FXML public void set3Days() { setEndFromNow(3); }
    @FXML public void set7Days() { setEndFromNow(7); }

    private void setEndFromNow(int days) {
        fieldEndTime.setText(LocalDateTime.now().plusDays(days).format(FMT));
    }

    // ── Image upload ─────────────────────────────────────────
    @FXML public void handleImageUpload() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Product Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File file = fc.showOpenDialog(imagePreviewBox.getScene().getWindow());
        if (file != null) {
            // Chuyển đổi File thành chuẩn URI (file:///) trước khi gán
            this.selectedImageFile = file;

            imageIcon.setText("🖼");
            imageHint.setText(file.getName());
            imagePreviewBox.setStyle(imagePreviewBox.getStyle()
                    .replace("border-style:dashed", "border-style:solid")
                    + " -fx-border-color:#2563eb;");
            msgImage.setText("");
        }
    }

    // ── Save draft ───────────────────────────────────────────
    @FXML public void saveDraft() {
        if (!validateBasic()) return;
        formMsg.setText("✓ Draft saved successfully.");
        formMsg.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
        AnimationUtil.pulse(submitBtn);
    }

    // ── Submit ────────────────────────────────────────────────
    @FXML public void handleSubmit() {
        if (!validateAll()) return;

        // 1. Lấy TẤT CẢ dữ liệu từ các ô giao diện (Bắt buộc làm ở luồng UI)
        String title       = fieldTitle.getText().trim();
        String desc        = fieldDesc.getText().trim();
        String category    = fieldCategory.getValue();
        String condition   = fieldCondition.getValue() == null ? "Good" : fieldCondition.getValue();
        double startPrice  = Double.parseDouble(fieldStartPrice.getText().trim());
        double reserve     = parseOptionalDouble(fieldReservePrice.getText());
        double increment   = 0;
        LocalDateTime start= parseDateTime(fieldStartTime.getText());
        LocalDateTime end  = parseDateTime(fieldEndTime.getText());

        // 2. Khóa nút bấm và báo đang xử lý
        submitBtn.setDisable(true);
        formMsg.setText("Processing...");
        formMsg.setStyle("-fx-text-fill:#d97706;"); // Màu cam

        // 3. Mở luồng phụ để gửi dữ liệu lên Server
        new Thread(() -> {
            String finalImagePath = null;
            if (editingAuction == null) {
                if (selectedImageFile != null) {
                    // Gọi dịch vụ đẩy ảnh lên mây lấy link trực tuyến
                    finalImagePath = com.auction.client.service.CloudinaryService.uploadImage(selectedImageFile);
                }
                // Create new
                auctionService.createAuction(currentUser, title, desc,
                        category, condition, startPrice, reserve, increment,
                        start, end, finalImagePath);
            } else {
                // Edit existing — upload ảnh mới nếu người dùng có chọn file
                if (selectedImageFile != null) {
                    finalImagePath = com.auction.client.service.CloudinaryService.uploadImage(selectedImageFile);
                }
                auctionService.updateAuction(editingAuction.getId(), title, desc,
                        category, startPrice, end, finalImagePath); // thêm finalImagePath
            }

            // 4. Server chạy xong, báo luồng chính cập nhật lại UI
            javafx.application.Platform.runLater(() -> {
                if (editingAuction == null) {
                    showSuccess("Auction published successfully!");
                } else {
                    showSuccess("Auction updated successfully! ✓");
                }
                this.selectedImageFile = null;

                AnimationUtil.pulse(submitBtn);
                submitBtn.setDisable(false); // Mở khóa nút bấm

                // Navigate to my products after short delay
                javafx.animation.PauseTransition p =
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(1200));
                p.setOnFinished(e -> {
                    MainController main = (MainController) submitBtn
                            .getScene().lookup("#mainRoot").getUserData();
                    main.navMyProducts();
                });
                p.play();
            });
        }).start();
    }

    @FXML public void goBack() {
        MainController main = (MainController) submitBtn
                .getScene().lookup("#mainRoot").getUserData();
        main.navMyProducts();
    }

    // ── Validation ────────────────────────────────────────────
    private boolean validateBasic() {
        boolean ok = true;
        if (fieldTitle.getText().trim().isEmpty()) {
            msgTitle.setText("Title is required.");
            AnimationUtil.shake(fieldTitle); ok = false;
        }
        if (fieldStartPrice.getText().trim().isEmpty()) {
            msgPrice.setText("Start price is required.");
            AnimationUtil.shake(fieldStartPrice); ok = false;
        } else {
            try { Double.parseDouble(fieldStartPrice.getText().trim()); }
            catch (NumberFormatException e) {
                msgPrice.setText("Enter a valid number.");
                AnimationUtil.shake(fieldStartPrice); ok = false;
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
            ok = false;
        }
        LocalDateTime end = parseDateTime(fieldEndTime.getText());
        if (end == null) {
            msgTime.setText("End time format: YYYY-MM-DD HH:mm");
            AnimationUtil.shake(fieldEndTime); ok = false;
        } else if (end.isBefore(LocalDateTime.now().plusMinutes(5))) {
            msgTime.setText("End time must be at least 5 min from now.");
            ok = false;
        }
        if (!ok) {
            formMsg.setText("Please fix the errors above.");
            formMsg.setStyle("-fx-text-fill:#dc2626;");
            AnimationUtil.shake(submitBtn);
        }
        return ok;
    }

    // ── Helpers ───────────────────────────────────────────────
    private void showSuccess(String msg) {
        formMsg.setText(msg);
        formMsg.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.isBlank()) return null;
        try { return LocalDateTime.parse(text.trim(), FMT); }
        catch (DateTimeParseException e) { return null; }
    }

    private double parseOptionalDouble(String text) {
        if (text == null || text.isBlank()) return 0;
        try { return Double.parseDouble(text.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
