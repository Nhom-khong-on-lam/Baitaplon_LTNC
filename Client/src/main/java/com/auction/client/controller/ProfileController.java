package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.client.service.AuthService;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * ProfileController — Xem và chỉnh sửa thông tin cá nhân.
 */
public class ProfileController {

    // ── View mode ─────────────────────────────────────────────
    @FXML private Label  profileAvatar, profileName, profileRoleBadge,
            profileJoined;
    @FXML private Label  statBidsTotal, statWon, statListed;
    @FXML private Label  infoUsername, infoEmail, infoStatus, infoRole;
    @FXML private Label  infoBankName, infoAccountNumber;
    @FXML private VBox   viewMode, editMode;
    @FXML private Button editToggleBtn;

    // ── Edit mode ─────────────────────────────────────────────
    @FXML private TextField     editUsername, editEmail;
    @FXML private PasswordField editOldPass, editNewPass;
    @FXML private Label         editMsg;
    // banking
    @FXML private TextField     editBankName, editAccountNumber, editCardholderName;

    // ── Won list ─────────────────────────────────────────────
    @FXML private VBox wonList;

    // 🚀 BỔ SUNG THÊM 2 DÒNG NÀY:
    @FXML private Label         lblCurrentBalance; // Label hiển thị số dư ví (ví dụ đặt ở vùng View hoặc Edit)
    @FXML private TextField     txtTopUpAmount;    // Ô nhập số tiền cần nạp thêm

    private final AuthService authService    = new AuthService();
    private final AuctionService auctionService = new AuctionService();
    private User currentUser;

    @FXML
    public void initialize() {
        // Trì hoãn một chút để cấu trúc FXML dựng xong hoàn toàn
        javafx.animation.PauseTransition stableUIWithDelay =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(150));
        stableUIWithDelay.setOnFinished(e -> {
            User sessionUser = SessionManager.get().getUser();
            if (sessionUser != null) {
                this.currentUser = sessionUser;

                // 🌟 THỦ THUẬT DU KÍCH: Gọi lệnh nạp thêm "0 VND" lên Server để ép Server
                // đọc trực tiếp số dư thực tế trong Database rồi trả về cho Client.
                new Thread(() -> {
                    try {
                        com.auction.common.network.Response res = auctionService.topUpBalance(currentUser.getId(), 0);
                        if (res != null && res.isSuccess()) {
                            // Ép kiểu lấy số dư thật từ DB do Server trả về
                            double realBalance = ((Number) res.getData()).doubleValue();

                            // Ghi đè số dư thật này vào RAM cục bộ để đồng bộ hệ thống
                            currentUser.setBalance(realBalance);
                            SessionManager.get().login(currentUser);

                            // Đẩy lên UI Thread để vẽ lại số tiền thật lên màn hình!
                            javafx.application.Platform.runLater(() -> {
                                if (lblCurrentBalance != null) {
                                    lblCurrentBalance.setText(String.format("%,.0f VND", realBalance));
                                }
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });
        stableUIWithDelay.play();
    }

    public void initData(User user) {
        // 🚀 ĐỒNG BỘ: Bốc ngay dữ liệu mới nhất, đầy đủ nhất từ SessionManager toàn cục ra dùng
        User sessionUser = SessionManager.get().getUser();
        if (sessionUser != null) {
            this.currentUser = sessionUser;
        } else {
            this.currentUser = user;
        }

        // Cập nhật text tĩnh ngay lập tức bằng đối tượng currentUser đã được đồng bộ
        String name = currentUser.getUsername();
        String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
        profileAvatar.setText(initials);
        profileName.setText(name);
        profileRoleBadge.setText(currentUser.isAdmin() ? "Administrator" : "Member");
        profileJoined.setText("Joined " + currentUser.getJoinedDate());
        infoUsername.setText(currentUser.getUsername());
        infoEmail.setText(currentUser.getEmail());
        infoRole.setText(currentUser.isAdmin() ? "Administrator" : "Member");
        infoStatus.setText(currentUser.getAccountStatus().toString());
        editUsername.setText(currentUser.getUsername());
        editEmail.setText(currentUser.getEmail());
        if (editBankName != null)      editBankName.setText(currentUser.getBankName() != null ? currentUser.getBankName() : "");
        if (editAccountNumber != null) editAccountNumber.setText(currentUser.getAccountNumber() != null ? currentUser.getAccountNumber() : "");
        if (editCardholderName != null) editCardholderName.setText(currentUser.getCardholderName() != null ? currentUser.getCardholderName() : "");

        // Mở luồng phụ để tải các con số thống kê và list từ Server
        new Thread(() -> {
            List<Auction> myBids     = auctionService.getMyBids(currentUser.getId());
            List<Auction> myWon      = auctionService.getWinningBids(currentUser.getId());
            List<Auction> myProducts = auctionService.getAuctionsBySeller(currentUser.getId());

            javafx.application.Platform.runLater(() -> {
                AnimationUtil.countUp(statBidsTotal, 0, myBids.size(),     700, "", "");
                AnimationUtil.countUp(statWon,       0, myWon.size(),      700, "", "");
                AnimationUtil.countUp(statListed,    0, myProducts.size(), 700, "", "");
                buildWonList(myWon);
            });
        }).start();

        if (editCardholderName != null) editCardholderName.setText(currentUser.getCardholderName() != null ? currentUser.getCardholderName() : "");

        // 🚀 CẬP NHẬT CHUẨN XÁC: Vẽ số dư lấy từ luồng SessionManager lên màn hình
        if (lblCurrentBalance != null) {
            lblCurrentBalance.setText(String.format("%,.0f VND", currentUser.getBalance()));
        }
        if (infoBankName != null) {
            infoBankName.setText(currentUser.getBankName() != null && !currentUser.getBankName().isEmpty()
                    ? currentUser.getBankName() : "Chưa liên kết");
        }
        if (infoAccountNumber != null) {
            infoAccountNumber.setText(currentUser.getAccountNumber() != null && !currentUser.getAccountNumber().isEmpty()
                    ? currentUser.getAccountNumber() : "Chưa liên kết");
        }
    }

    // ── Toggle edit mode ──────────────────────────────────────
    @FXML
    public void toggleEditMode() {
        boolean isEditing = editMode.isVisible();
        if (isEditing) {
            cancelEdit();
        } else {
            viewMode.setVisible(false); viewMode.setManaged(false);
            editMode.setVisible(true);  editMode.setManaged(true);
            editToggleBtn.setText("← Cancel");
            AnimationUtil.fadeIn(editMode, 300);
        }
    }

    @FXML
    public void cancelEdit() {
        editMode.setVisible(false);  editMode.setManaged(false);
        viewMode.setVisible(true);   viewMode.setManaged(true);
        editToggleBtn.setText("✏ Edit Profile");
        editMsg.setText("");
    }

    @FXML
    public void handleSave() {
        String newUsername = editUsername.getText().trim();
        String newEmail    = editEmail.getText().trim();
        String oldPass     = editOldPass.getText();
        String newPass     = editNewPass.getText();

        // 1. [Chạy trên UI Thread] - Kiểm tra dữ liệu nhanh ngay tại Client
        if (newUsername.isEmpty()) {
            setMsg("Username cannot be empty.", false);
            AnimationUtil.shake(editUsername);
            return;
        }
        if (!authService.isValidEmail(newEmail)) {
            setMsg("Invalid email format.", false);
            AnimationUtil.shake(editEmail);
            return;
        }
        if (!oldPass.isEmpty() && newPass.length() < 6) {
            setMsg("New password must be at least 6 characters.", false);
            AnimationUtil.shake(editNewPass);
            return;
        }

        // Hiển thị trạng thái đang xử lý và KHÓA nút Save/Cancel lại để chống người dùng ấn liên tục gây lỗi
        setMsg("Saving changes...", true);
        editUsername.setDisable(true);
        editEmail.setDisable(true);
        editOldPass.setDisable(true);
        editNewPass.setDisable(true);

        // Lưu lại Email gốc để phục vụ đổi mật khẩu trước khi bị ghi đè thông tin mới
        final String originalEmail = currentUser.getEmail();

        // 2. 🚀 [TẠO LUỒNG PHỤ NGẦM] - Đẩy việc gọi mạng qua Socket xuống đây để app không bị đơ
        new Thread(() -> {
            try {
                // Kiểm tra và đổi mật khẩu (nếu có nhập)
                if (!oldPass.isEmpty()) {
                    boolean isPassCorrect = authService.checkPassword(currentUser, oldPass);

                    if (!isPassCorrect) {
                        // Trả lại lệnh hiển thị lỗi về cho Luồng UI xử lý
                        javafx.application.Platform.runLater(() -> {
                            setMsg("Current password is incorrect.", false);
                            AnimationUtil.shake(editOldPass);
                            // Mở khóa lại các ô nhập liệu để người dùng sửa đổi
                            enableInputFields();
                        });
                        return; // Ngắt luồng phụ ngầm, không lưu dữ liệu nữa
                    }

                    // Gửi lệnh đổi pass lên Server
                    authService.updatePassword(originalEmail, newPass);
                }

                // Gán thông tin mới vào đối tượng cục bộ
                currentUser.setUsername(newUsername);
                currentUser.setEmail(newEmail);

                // Cập nhật thông tin ngân hàng nếu có
                String newBankName     = editBankName.getText().trim();
                String newAccount      = editAccountNumber.getText().trim();
                String newCardholder   = editCardholderName.getText().trim();

                // 2. Gán trực tiếp vào Object currentUser để đồng bộ RAM cục bộ của Client
                currentUser.setBankName(newBankName);
                currentUser.setAccountNumber(newAccount);
                currentUser.setCardholderName(newCardholder);

                // 🚀 CHUYỂN DỮ LIỆU ĐI: Chỉ giữ lại duy nhất 2 dòng này
                auctionService.updateBankInfo(currentUser.getId(), newBankName, newAccount, newCardholder);
                authService.updateUser(currentUser);
                // 3. 🎉 [CẬP NHẬT GIAO DIỆN CHÍNH THỨC] - Đưa về Luồng UI để vẽ lại màn hình
                javafx.application.Platform.runLater(() -> {
                    // Làm mới lại bộ nhớ Session hiện tại
                    SessionManager.get().login(currentUser);

                    // 🚀 ĐỔI CHỮ HIỂN THỊ TRÊN PROFILE NGAY LẬP TỨC KHÔNG CẦN CHUYỂN TAB
                    profileName.setText(newUsername);
                    infoUsername.setText(newUsername);
                    infoEmail.setText(newEmail);

                    // Đẩy trực tiếp giá trị chuỗi vừa gõ từ ô nhập liệu ra nhãn hiển thị tĩnh
                    if (infoBankName != null) {
                        infoBankName.setText(newBankName.isEmpty() ? "Chưa liên kết" : newBankName);
                    }
                    if (infoAccountNumber != null) {
                        infoAccountNumber.setText(newAccount.isEmpty() ? "Chưa liên kết" : newAccount);
                    }

                    // Dọn sạch 2 ô nhập mật khẩu
                    editOldPass.clear();
                    editNewPass.clear();

                    setMsg("✓ Profile updated successfully!", true);

                    // Mở khóa lại các ô nhập liệu cho lần sửa sau
                    enableInputFields();

                    // Tạo một quãng nghỉ ngắn 1 giây để người dùng kịp nhìn thấy thông báo thành công trước khi đóng tab Edit
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(1000));
                    pause.setOnFinished(e -> cancelEdit());
                    pause.play();
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                // Nếu xảy ra lỗi mất kết nối, hiển thị lên UI thông báo cho người dùng biết
                javafx.application.Platform.runLater(() -> {
                    setMsg("Error: Server communication failed.", false);
                    enableInputFields();
                });
            }
        }).start(); // Kích hoạt luồng phụ chạy song song với luồng giao diện
    }
    private void enableInputFields() {
        editUsername.setDisable(false);
        editEmail.setDisable(false);
        editOldPass.setDisable(false);
        editNewPass.setDisable(false);
        if (editBankName != null)       editBankName.setDisable(false);
        if (editAccountNumber != null)  editAccountNumber.setDisable(false);
        if (editCardholderName != null) editCardholderName.setDisable(false);
    }
    @FXML
    public void handleTopUp() {
        if (txtTopUpAmount == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Chưa kết nối text field nạp tiền!");
            return;
        }

        String amountStr = txtTopUpAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập số tiền cần nạp!");
            AnimationUtil.shake(txtTopUpAmount);
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Lỗi số tiền", "Số tiền nạp vào phải lớn hơn 0 VND!");
                AnimationUtil.shake(txtTopUpAmount);
                return;
            }

            txtTopUpAmount.setDisable(true);

            // Chạy luồng phụ ngầm gửi gói tin Socket lên Server
            new Thread(() -> {
                try {
                    com.auction.common.network.Response res = auctionService.topUpBalance(currentUser.getId(), amount);

                    javafx.application.Platform.runLater(() -> {
                        txtTopUpAmount.setDisable(false);
                        if (res != null && res.isSuccess()) {
                            // 1. Nhận số dư mới tinh mà Server đã cộng dồn thành công từ DB
                            double newBalance = ((Number) res.getData()).doubleValue();

                            // 2. Cập nhật vào đối tượng User hiện tại trong RAM cục bộ
                            currentUser.setBalance(newBalance);

                            // 3. Cập nhật lại Session để các màn hình khác (như Đặt giá) cùng biết số dư mới
                            SessionManager.get().login(currentUser);

                            // 4. Ép giao diện vẽ lại thay số 0 cũ thành số tiền mới tinh vừa nạp
                            if (lblCurrentBalance != null) {
                                lblCurrentBalance.setText(String.format("%,.0f VND", newBalance));
                            }

                            txtTopUpAmount.clear(); // Xóa trống ô nhập
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Nạp tiền thành công! Số dư mới: " + String.format("%,.0f VND", newBalance));
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Thất bại", res != null ? res.getMessage() : "Lỗi kết nối Server.");
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        txtTopUpAmount.setDisable(false);
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối mạng tới Server.");
                    });
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Sai định dạng", "Số tiền nhập vào bắt buộc phải là ký tự số!");
            txtTopUpAmount.setDisable(false);
            AnimationUtil.shake(txtTopUpAmount);
        }
    }

    // Hàm phụ hiển thị thông báo Pop-up trực quan cho người dùng thấy kết quả luôn
    private void showAlert(Alert.AlertType type, String title, String content) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    /** Build danh sách phiên đấu giá thắng */
    private void buildWonList(List<Auction> wonAuctions) {
        wonList.getChildren().clear();

        if (wonAuctions.isEmpty()) {
            Label empty = new Label("No won auctions yet. Keep bidding!");
            empty.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:12px;");
            wonList.getChildren().add(empty);
            return;
        }

        for (Auction a : wonAuctions) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 10, 8, 10));
            row.setStyle("-fx-background-color:#f0fdf4; -fx-background-radius:8;" +
                    "-fx-border-color:#bbf7d0; -fx-border-radius:8; -fx-border-width:1;");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label title = new Label(a.getTitle());
            title.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#15803d;");

            title.setWrapText(true);
            Label price = new Label("$" + String.format("%,.0f", a.getCurrentPrice()));
            price.setStyle("-fx-font-size:11px; -fx-text-fill:#16a34a;");
            info.getChildren().addAll(title, price);

            row.getChildren().addAll(info);
            wonList.getChildren().add(row);
        }
    }

    private void setMsg(String msg, boolean ok) {
        editMsg.setText(msg);
        editMsg.setTextFill(ok ? Color.web("#16a34a") : Color.web("#dc2626"));
    }
}
