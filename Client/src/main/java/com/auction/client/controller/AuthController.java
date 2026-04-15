package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.client.service.EmailService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Random;

public class AuthController {
    // --- FXML Components (Phải khớp 100% với fx:id trong FXML) ---
    @FXML private VBox loginForm, registerForm, forgotForm;
    @FXML private TextField loginUser, regUser, regEmail, forgotEmail, otpInput, forgotOtpInput;
    @FXML private PasswordField loginPass, regPass, regConfirmPass, newPass, confirmNewPass;
    @FXML private Label loginMsg, regMsg, forgotMsg;
    @FXML private Button btnSignUp;

    // --- Services ---
    private final AuthService authService = new AuthService();
    private final EmailService emailService = new EmailService();

    // --- Logic Variables ---
    private String generatedOTP;
    private long otpExpiryTime;
    private long lastOTPSendTime;
    private final Duration ANIM_SPEED = Duration.millis(250);

    @FXML
    public void initialize() {
        setupClearMessageListeners();
    }

    // --- 1. LOGIC ĐĂNG NHẬP ---
    @FXML
    public void handleLogin() {
        String user = safeGet(loginUser);
        String pass = safeGet(loginPass);

        if (isEmpty(user, pass)) {
            showMessage(loginMsg, "Username and password are required!", false);
            return;
        }

        // Thực hiện logic login tại đây
        showMessage(loginMsg, "Processing login...", true);
    }

    // --- 2. LOGIC ĐĂNG KÝ (FIXED) ---
    @FXML
    public void handleSendOTP() {
        String email = safeGet(regEmail);

        if (!authService.isValidEmail(email)) {
            showMessage(regMsg, "Invalid email format!", false);
            return;
        }

        if (authService.isEmailExists(email)) {
            showMessage(regMsg, "This email is already registered!", false);
            return;
        }

        if (!checkCooldown(regMsg)) return;

        processOTPSending(email, regMsg, true);
    }

    @FXML
    public void handleRegister() {
        String user = safeGet(regUser);
        String email = safeGet(regEmail);
        String pass = safeGet(regPass);
        String confirm = safeGet(regConfirmPass);
        String otp = safeGet(otpInput);



        // Validation: Kiểm tra trống (đã bao gồm trim-safe)
        if (isEmpty(user, email, pass, confirm, otp)) {
            showMessage(regMsg, "Please fill in all fields!", false);
            return;
        }

        // Validation: Unique và khớp mật khẩu
        if (authService.isUsernameExists(user)) {
            showMessage(regMsg, "Username already exists!", false);
            return;
        }

        if (!pass.equals(confirm)) {
            showMessage(regMsg, "Passwords do not match!", false);
            return;
        }

        // Validation: OTP
        if (isOTPInvalid(otp)) {
            showMessage(regMsg, "Invalid or expired OTP!", false);
            return;
        }

        // Đăng ký thành công
        authService.saveUser(user, email);
        showMessage(regMsg, "Registration successful!", true);

        // Reset OTP để tránh dùng lại
        generatedOTP = null;
        autoFillAndSwitch(user, pass);
    }

    // --- 3. LOGIC QUÊN MẬT KHẨU ---
    @FXML
    public void handleSendForgotOTP() {
        String email = safeGet(forgotEmail);
        if (isEmpty(email) || !authService.isValidEmail(email)) {
            showMessage(forgotMsg, "Please enter a valid email!", false);
            return;
        }
        if (!checkCooldown(forgotMsg)) return;
        processOTPSending(email, forgotMsg, false);
    }

    @FXML
    public void handleResetPassword() {
        String email = safeGet(forgotEmail);
        String pass = safeGet(newPass);
        String confirm = safeGet(confirmNewPass);
        String otp = safeGet(forgotOtpInput);

        if (isEmpty(email, pass, confirm, otp)) {
            showMessage(forgotMsg, "All fields are required!", false);
            return;
        }

        if (isOTPInvalid(otp)) {
            showMessage(forgotMsg, "Invalid or expired OTP!", false);
            return;
        }

        if (!pass.equals(confirm)) {
            showMessage(forgotMsg, "Passwords do not match!", false);
            return;
        }

        showMessage(forgotMsg, "Password reset successful!", true);
        generatedOTP = null;
        autoFillAndSwitch(email, pass);
    }

    // --- 4. HELPER METHODS (Logic xử lý nội bộ) ---

    // Lấy text và cắt khoảng trắng (trim), tránh NullPointerException
    private String safeGet(TextInputControl field) {
        return (field == null || field.getText() == null)
                ? ""
                : field.getText().trim();
    }

    // Kiểm tra danh sách các chuỗi có cái nào trống không
    private boolean isEmpty(String... values) {
        for (String val : values) {
            if (val == null || val.trim().isEmpty()) return true;
        }
        return false;
    }

    private boolean checkCooldown(Label msgLabel) {
        long now = System.currentTimeMillis();
        if (now - lastOTPSendTime < 30000) {
            long remaining = 30 - (now - lastOTPSendTime) / 1000;
            showMessage(msgLabel, "Resend available in " + remaining + "s", false);
            return false;
        }
        return true;
    }

    private void processOTPSending(String email, Label targetLabel, boolean isReg) {
        generatedOTP = String.format("%06d", new Random().nextInt(999999));
        otpExpiryTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 phút
        lastOTPSendTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                emailService.sendOTP(email, generatedOTP);
                Platform.runLater(() -> {
                    showMessage(targetLabel, "OTP sent successfully to " + email, true);
                    if (isReg && btnSignUp != null) btnSignUp.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> showMessage(targetLabel, "Email delivery failed!", false));
            }
        }).start();
    }

    private boolean isOTPInvalid(String inputOTP) {
        if (generatedOTP == null || inputOTP.isEmpty()) return true;
        return System.currentTimeMillis() > otpExpiryTime || !inputOTP.equals(generatedOTP);
    }

    private void showMessage(Label lbl, String msg, boolean success) {
        if (lbl == null) return;
        lbl.setText(msg);
        lbl.setTextFill(success ? Color.web("#2ecc71") : Color.web("#e74c3c"));
    }

    private void autoFillAndSwitch(String user, String pass) {
        loginUser.setText(user);
        loginPass.setText(pass);
        PauseTransition pause = new PauseTransition(Duration.millis(1500));
        pause.setOnFinished(e -> showLoginForm());
        pause.play();
    }

    private void setupClearMessageListeners() {
        // Clear message khi user nhập liệu vào bất kỳ field nào
        TextInputControl[] fields = {loginUser, loginPass, regUser, regEmail, regPass, otpInput, forgotEmail};
        for (TextInputControl field : fields) {
            if (field != null) {
                field.textProperty().addListener((obs, oldVal, newVal) -> {
                    loginMsg.setText("");
                    regMsg.setText("");
                    forgotMsg.setText("");
                });
            }
        }
    }

    // --- 5. ANIMATIONS (Giữ nguyên logic chuyển cảnh) ---
    @FXML private void showRegisterForm() { playSlideTransition(loginForm, registerForm, -400); }
    @FXML private void showForgotForm() { playScaleTransition(getActiveForm(), forgotForm); }

    @FXML
    public void showLoginForm() {
        if (forgotForm.isVisible()) playScaleTransition(forgotForm, loginForm);
        else playSlideTransition(registerForm, loginForm, 400);
    }

    private void playSlideTransition(VBox out, VBox in, double x) {
        in.setVisible(true);
        in.setTranslateX(-x);
        TranslateTransition tOut = new TranslateTransition(ANIM_SPEED, out); tOut.setByX(x);
        TranslateTransition tIn = new TranslateTransition(ANIM_SPEED, in); tIn.setToX(0);
        FadeTransition fOut = new FadeTransition(ANIM_SPEED, out); fOut.setToValue(0);
        FadeTransition fIn = new FadeTransition(ANIM_SPEED, in); fIn.setToValue(1);

        ParallelTransition pt = new ParallelTransition(tOut, tIn, fOut, fIn);
        pt.setOnFinished(e -> { out.setVisible(false); out.setTranslateX(0); out.setOpacity(1); });
        pt.play();
    }

    private void playScaleTransition(VBox out, VBox in) {
        in.setVisible(true);
        in.setScaleX(0.7); in.setScaleY(0.7);
        in.setOpacity(0);
        FadeTransition fOut = new FadeTransition(ANIM_SPEED, out); fOut.setToValue(0);
        FadeTransition fIn = new FadeTransition(ANIM_SPEED, in); fIn.setToValue(1);
        ScaleTransition sIn = new ScaleTransition(ANIM_SPEED, in); sIn.setToX(1); sIn.setToY(1);

        ParallelTransition pt = new ParallelTransition(fOut, fIn, sIn);
        pt.setOnFinished(e -> { out.setVisible(false); out.setOpacity(1); });
        pt.play();
    }

    private VBox getActiveForm() {
        if (loginForm.isVisible()) return loginForm;
        if (registerForm.isVisible()) return registerForm;
        return forgotForm;
    }
}