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

        // Validate cơ bản trên UI
        if (newUsername.isEmpty()) {
            setMsg("Username cannot be empty.", false);
            AnimationUtil.shake(editUsername); return;
        }
        if (!authService.isValidEmail(newEmail)) {
            setMsg("Invalid email format.", false);
            AnimationUtil.shake(editEmail); return;
        }
        if (!oldPass.isEmpty() && newPass.length() < 6) {
            setMsg("New password must be at least 6 characters.", false);
            AnimationUtil.shake(editNewPass); return;
        }

        setMsg("Saving changes...", true);

        // Chạy ngầm gọi mạng kiểm tra & update
        new Thread(() -> {
            if (!oldPass.isEmpty()) {
                if (!authService.checkPassword(currentUser, oldPass)) {
                    javafx.application.Platform.runLater(() -> {
                        setMsg("Current password is incorrect.", false);
                        AnimationUtil.shake(editOldPass);
                    });
                    return; // Dừng luồng nếu sai pass
                }
                authService.updatePassword(currentUser.getEmail(), newPass);
            }

            currentUser.setUsername(newUsername);
            currentUser.setEmail(newEmail);
            authService.updateUser(currentUser);

            // Vẽ lại giao diện sau khi thành công
            javafx.application.Platform.runLater(() -> {
                SessionManager.get().login(currentUser); // refresh session
                profileName.setText(newUsername);
                infoUsername.setText(newUsername);
                infoEmail.setText(newEmail);

                setMsg("✓ Profile updated successfully!", true);

                javafx.animation.PauseTransition p =
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(1000));
                p.setOnFinished(e -> cancelEdit());
                p.play();
            });
        }).start();
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

            Label icon = new Label(a.getCategoryIcon());
            icon.setStyle("-fx-font-size:20px;");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label title = new Label(a.getTitle());
            title.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#15803d;");
            Label price = new Label("$" + String.format("%,.0f", a.getCurrentPrice()));
            price.setStyle("-fx-font-size:11px; -fx-text-fill:#16a34a;");
            info.getChildren().addAll(title, price);

            row.getChildren().addAll(icon, info);
            wonList.getChildren().add(row);
        }
    }

    private void setMsg(String msg, boolean ok) {
        editMsg.setText(msg);
        editMsg.setTextFill(ok ? Color.web("#16a34a") : Color.web("#dc2626"));
    }
}
