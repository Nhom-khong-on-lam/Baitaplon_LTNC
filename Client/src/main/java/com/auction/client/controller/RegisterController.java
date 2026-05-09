package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.client.service.EmailService;
import com.auction.client.controller.AnimationUtil;
import com.auction.client.controller.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

import java.util.Random;

public class RegisterController {

    @FXML private TextField     regUser, regEmail, otpField;
    @FXML private PasswordField regPass, regConfirm;
    @FXML private Label         userMsg, emailMsg, regMsg;
    @FXML private Button        sendOtpBtn, registerBtn;

    private final AuthService  authService  = new AuthService();
    private final EmailService emailService = new EmailService();

    private String  generatedOtp;
    private long    otpExpiry;
    private long    lastOtpSent = 0;

    @FXML
    public void initialize() {
        AnimationUtil.fadeIn(registerBtn, 400);

        // Clear errors on type
        regUser.textProperty().addListener( (o,v,n) -> userMsg.setText(""));
        regEmail.textProperty().addListener((o,v,n) -> emailMsg.setText(""));
        regPass.textProperty().addListener( (o,v,n) -> regMsg.setText(""));
        regConfirm.textProperty().addListener((o,v,n)->regMsg.setText(""));
    }

    // ── Send OTP ──────────────────────────────────────────
    @FXML
    public void handleSendOTP() {
        String email = trim(regEmail);
        if (!authService.isValidEmail(email)) {
            setMsg(emailMsg, "Invalid email format.", false); return;
        }
        if (authService.isEmailExists(email)) {
            setMsg(emailMsg, "Email already registered.", false); return;
        }
        long now = System.currentTimeMillis();
        if (now - lastOtpSent < 30_000) {
            long sec = 30 - (now - lastOtpSent) / 1000;
            setMsg(emailMsg, "Resend available in " + sec + "s", false); return;
        }

        generatedOtp = String.format("%06d", new Random().nextInt(999999));
        otpExpiry    = now + 5 * 60_000;
        lastOtpSent  = now;
        sendOtpBtn.setText("Sending...");
        sendOtpBtn.setDisable(true);

        new Thread(() -> {
            try {
                emailService.sendOTP(email, generatedOtp);
                Platform.runLater(() -> {
                    sendOtpBtn.setText("Resend OTP");
                    sendOtpBtn.setDisable(false);
                    setMsg(emailMsg, "✓ OTP sent to " + email, true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    sendOtpBtn.setText("Send OTP");
                    sendOtpBtn.setDisable(false);
                    setMsg(emailMsg, "Email delivery failed.", false);
                });
            }
        }, "otp-thread").start();
    }

    // ── Register ──────────────────────────────────────────
    @FXML
    public void handleRegister() {
        String user    = trim(regUser);
        String email   = trim(regEmail);
        String otp     = trim(otpField);
        String pass    = trim(regPass);
        String confirm = trim(regConfirm);

        // Validate
        if (user.isEmpty() || email.isEmpty() || otp.isEmpty()
                || pass.isEmpty() || confirm.isEmpty()) {
            setMsg(regMsg, "Please fill in all fields.", false);
            AnimationUtil.shake(registerBtn); return;
        }
        if (authService.isUsernameExists(user)) {
            setMsg(userMsg, "Username already taken.", false);
            AnimationUtil.shake(regUser); return;
        }
        if (!pass.equals(confirm)) {
            setMsg(regMsg, "Passwords do not match.", false);
            AnimationUtil.shake(regConfirm); return;
        }
        if (pass.length() < 6) {
            setMsg(regMsg, "Password must be at least 6 characters.", false);
            AnimationUtil.shake(regPass); return;
        }
        if (generatedOtp == null || System.currentTimeMillis() > otpExpiry
                || !otp.equals(generatedOtp)) {
            setMsg(regMsg, "Invalid or expired OTP.", false);
            AnimationUtil.shake(otpField); return;
        }

        authService.saveUser(user, email, pass);
        setMsg(regMsg, "✓ Account created! Redirecting to login...", true);
        generatedOtp = null;

        javafx.animation.PauseTransition p =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(1200));
        p.setOnFinished(e -> SceneManager.get().navigate(SceneManager.Screen.LOGIN));
        p.play();
    }

    @FXML
    public void goToLogin() {
        SceneManager.get().navigate(SceneManager.Screen.LOGIN);
    }

    // ── Helpers ───────────────────────────────────────────
    private void setMsg(Label lbl, String msg, boolean ok) {
        lbl.setText(msg);
        lbl.setTextFill(ok ? Color.web("#16a34a") : Color.web("#dc2626"));
    }
    private String trim(TextInputControl f) {
        return f.getText() == null ? "" : f.getText().trim();
    }
}