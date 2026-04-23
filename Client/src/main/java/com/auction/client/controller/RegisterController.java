package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.client.service.EmailService;
import javafx.application.Platform;
import javafx.scene.control.*;
import java.util.Random;

public class RegisterController {
    private AuthController parent;
    private TextField regUser, regEmail, otpInput, forgotEmail, forgotOtpInput;
    private PasswordField regPass, regConfirmPass, newPass, confirmNewPass;
    private Label regMsg, forgotMsg;
    private Button btnSignUp;

    private final AuthService authService = new AuthService();
    private final EmailService emailService = new EmailService();
    private String generatedOTP;
    private long otpExpiryTime;
    private long lastOTPSendTime;

    public void inject(AuthController parent,
                       TextField regUser, TextField regEmail, PasswordField regPass, PasswordField regConfirmPass,
                       TextField otpInput, TextField forgotEmail, TextField forgotOtpInput,
                       PasswordField newPass, PasswordField confirmNewPass,
                       Label regMsg, Label forgotMsg, Button btnSignUp) {
        this.parent = parent;
        this.regUser = regUser;
        this.regEmail = regEmail;
        this.regPass = regPass;
        this.regConfirmPass = regConfirmPass;
        this.otpInput = otpInput;
        this.forgotEmail = forgotEmail;
        this.forgotOtpInput = forgotOtpInput;
        this.newPass = newPass;
        this.confirmNewPass = confirmNewPass;
        this.regMsg = regMsg;
        this.forgotMsg = forgotMsg;
        this.btnSignUp = btnSignUp;
    }


    public void handleSendOTP() {
        String email = parent.safeGet(regEmail);

        if (!authService.isValidEmail(email)) {
            parent.showMessage(regMsg, "Invalid email format!", false);
            return;
        }

        if (authService.isEmailExists(email)) {
            parent.showMessage(regMsg, "This email is already registered!", false);
            return;
        }

        if (!checkCooldown(regMsg)) return;

        processOTPSending(email, regMsg, true);
    }

    public void handleRegister() {
        String user = parent.safeGet(regUser);
        String email = parent.safeGet(regEmail);
        String pass = parent.safeGet(regPass);
        String confirm = parent.safeGet(regConfirmPass);
        String otp = parent.safeGet(otpInput);

        if (parent.isEmpty(user, email, pass, confirm, otp)) {
            parent.showMessage(regMsg, "Please fill in all fields!", false);
            return;
        }

        if (authService.isUsernameExists(user)) {
            parent.showMessage(regMsg, "Username already exists!", false);
            return;
        }

        if (!pass.equals(confirm)) {
            parent.showMessage(regMsg, "Passwords do not match!", false);
            return;
        }

        if (isOTPInvalid(otp)) {
            parent.showMessage(regMsg, "Invalid or expired OTP!", false);
            return;
        }

        authService.saveUser(user, email);
        parent.showMessage(regMsg, "Registration successful!", true);

        generatedOTP = null;
        parent.autoFillAndSwitch(user, pass);
    }


    public void handleSendForgotOTP() {
        String email = parent.safeGet(forgotEmail);
        if (parent.isEmpty(email) || !authService.isValidEmail(email)) {
            parent.showMessage(forgotMsg, "Please enter a valid email!", false);
            return;
        }
        if (!checkCooldown(forgotMsg)) return;
        processOTPSending(email, forgotMsg, false);
    }

    public void handleResetPassword() {
        String email = parent.safeGet(forgotEmail);
        String pass = parent.safeGet(newPass);
        String confirm = parent.safeGet(confirmNewPass);
        String otp = parent.safeGet(forgotOtpInput);

        if (parent.isEmpty(email, pass, confirm, otp)) {
            parent.showMessage(forgotMsg, "All fields are required!", false);
            return;
        }

        if (isOTPInvalid(otp)) {
            parent.showMessage(forgotMsg, "Invalid or expired OTP!", false);
            return;
        }

        if (!pass.equals(confirm)) {
            parent.showMessage(forgotMsg, "Passwords do not match!", false);
            return;
        }

        parent.showMessage(forgotMsg, "Password reset successful!", true);
        generatedOTP = null;
        parent.autoFillAndSwitch(email, pass);
    }

    private boolean checkCooldown(Label msgLabel) {
        long now = System.currentTimeMillis();
        if (now - lastOTPSendTime < 30000) {
            long remaining = 30 - (now - lastOTPSendTime) / 1000;
            parent.showMessage(msgLabel, "Resend available in " + remaining + "s", false);
            return false;
        }
        return true;
    }

    private void processOTPSending(String email, Label targetLabel, boolean isReg) {
        generatedOTP = String.format("%06d", new Random().nextInt(999999));
        otpExpiryTime = System.currentTimeMillis() + (5 * 60 * 1000);
        lastOTPSendTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                emailService.sendOTP(email, generatedOTP);
                Platform.runLater(() -> {
                    parent.showMessage(targetLabel, "OTP sent successfully to " + email, true);
                    if (isReg && btnSignUp != null) btnSignUp.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> parent.showMessage(targetLabel, "Email delivery failed!", false));
            }
        }).start();
    }

    private boolean isOTPInvalid(String inputOTP) {
        if (generatedOTP == null || inputOTP.isEmpty()) return true;
        return System.currentTimeMillis() > otpExpiryTime || !inputOTP.equals(generatedOTP);
    }
}