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
    @FXML private VBox   viewMode, editMode;
    @FXML private Button editToggleBtn;

    // ── Edit mode ─────────────────────────────────────────────
    @FXML private TextField     editUsername, editEmail;
    @FXML private PasswordField editOldPass, editNewPass;
    @FXML private Label         editMsg;

    // ── Won list ─────────────────────────────────────────────
    @FXML private VBox wonList;

    private final AuthService authService    = new AuthService();
    private final AuctionService auctionService = new AuctionService();
    private User currentUser;

    public void initData(User user) {
        this.currentUser = user;

        // Cập nhật text tĩnh ngay lập tức
        String name = user.getUsername();
        String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
        profileAvatar.setText(initials);
        profileName.setText(name);
        profileRoleBadge.setText(user.isAdmin() ? "Administrator" : "Member");
        profileJoined.setText("Joined " + user.getJoinedDate());
        infoUsername.setText(user.getUsername());
        infoEmail.setText(user.getEmail());
        infoRole.setText(user.isAdmin() ? "Administrator" : "Member");
        infoStatus.setText(user.getAccountStatus().toString());
        editUsername.setText(user.getUsername());
        editEmail.setText(user.getEmail());

        // Mở luồng phụ để tải các con số thống kê và list từ Server
        new Thread(() -> {
            List<Auction> myBids     = auctionService.getMyBids(user.getId());
            List<Auction> myWon      = auctionService.getWinningBids(user.getId());
            List<Auction> myProducts = auctionService.getAuctionsBySeller(user.getId());

            javafx.application.Platform.runLater(() -> {
                AnimationUtil.countUp(statBidsTotal, 0, myBids.size(),     700, "", "");
                AnimationUtil.countUp(statWon,       0, myWon.size(),      700, "", "");
                AnimationUtil.countUp(statListed,    0, myProducts.size(), 700, "", "");
                buildWonList(myWon);
            });
        }).start();
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

                // Gửi gói tin cập nhật User DTO lên Server qua đường ống Long-lived Socket
                authService.updateUser(currentUser);

                // 3. 🎉 [CẬP NHẬT GIAO DIỆN CHÍNH THỨC] - Đưa về Luồng UI để vẽ lại màn hình
                javafx.application.Platform.runLater(() -> {
                    // Làm mới lại bộ nhớ Session hiện tại
                    SessionManager.get().login(currentUser);

                    // Thay đổi hiển thị thông tin dạng Text trên Profile
                    profileName.setText(newUsername);
                    infoUsername.setText(newUsername);
                    infoEmail.setText(newEmail);

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
