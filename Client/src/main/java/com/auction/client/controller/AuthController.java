package com.auction.client.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class AuthController {

    @FXML private VBox loginForm, registerForm, forgotForm;
    @FXML private TextField loginUser, regUser, regEmail, forgotEmail, otpInput, forgotOtpInput;
    @FXML private PasswordField loginPass, regPass, regConfirmPass, newPass, confirmNewPass;
    @FXML private Label loginMsg, regMsg, forgotMsg;
    @FXML private Button btnSignUp;


    private final LoginController loginController = new LoginController();
    private final RegisterController registerController = new RegisterController();

    private final Duration ANIM_SPEED = Duration.millis(250);

    @FXML
    public void initialize() {
        loginController.inject(this, loginUser, loginPass, loginMsg);
        registerController.inject(this, regUser, regEmail, regPass, regConfirmPass,
                otpInput, forgotEmail, forgotOtpInput, newPass, confirmNewPass,
                regMsg, forgotMsg, btnSignUp);

        setupClearMessageListeners();
    }


    @FXML public void handleLogin() { loginController.handleLogin(); }
    @FXML public void handleSendOTP() { registerController.handleSendOTP(); }
    @FXML public void handleRegister() { registerController.handleRegister(); }
    @FXML public void handleSendForgotOTP() { registerController.handleSendForgotOTP(); }
    @FXML public void handleResetPassword() { registerController.handleResetPassword(); }


    @FXML public void showRegisterForm() { playSlideTransition(loginForm, registerForm, -400); }
    @FXML public void showForgotForm() { playScaleTransition(getActiveForm(), forgotForm); }
    @FXML public void showLoginForm() {
        if (forgotForm.isVisible()) playScaleTransition(forgotForm, loginForm);
        else playSlideTransition(registerForm, loginForm, 400);
    }

    public String safeGet(TextInputControl field) {
        return (field == null || field.getText() == null) ? "" : field.getText().trim();
    }

    public boolean isEmpty(String... values) {
        for (String val : values) {
            if (val == null || val.trim().isEmpty()) return true;
        }
        return false;
    }

    public void showMessage(Label lbl, String msg, boolean success) {
        if (lbl == null) return;
        lbl.setText(msg);
        lbl.setTextFill(success ? Color.web("#2ecc71") : Color.web("#e74c3c"));
    }

    public void autoFillAndSwitch(String user, String pass) {
        loginUser.setText(user);
        loginPass.setText(pass);
        PauseTransition pause = new PauseTransition(Duration.millis(1500));
        pause.setOnFinished(e -> showLoginForm());
        pause.play();
    }

    private void setupClearMessageListeners() {
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
        in.setScaleX(0.7); in.setScaleY(0.7); in.setOpacity(0);
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