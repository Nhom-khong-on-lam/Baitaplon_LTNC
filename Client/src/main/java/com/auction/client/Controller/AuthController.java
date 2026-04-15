package com.auction.client.Controller;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class AuthController {

    @FXML private VBox loginForm;
    @FXML private VBox registerForm;

    // Login fields
    @FXML private TextField loginUser; // Sử dụng cho cả Username hoặc Email
    @FXML private PasswordField loginPass;

    // Register fields
    @FXML private TextField regUser;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPass;
    @FXML private PasswordField regConfirmPass;

    @FXML
    private void showRegisterForm() {
        // Animation: Login trượt trái mất đi, Register trượt từ phải vào
        playTransition(loginForm, registerForm, -500, 0);
    }

    @FXML
    private void showLoginForm() {
        // Animation: Register trượt phải mất đi, Login trượt từ trái vào
        playTransition(registerForm, loginForm, 500, 0);
    }

    private void playTransition(VBox outForm, VBox inForm, double outX, double inX) {
        inForm.setVisible(true);
        inForm.setTranslateX(-outX); // Đặt vị trí ban đầu của form đi vào

        // Hiệu ứng mờ dần và trượt cho form đi ra
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), outForm);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        TranslateTransition transOut = new TranslateTransition(Duration.millis(400), outForm);
        transOut.setToX(outX);

        // Hiệu ứng hiện dần và trượt cho form đi vào
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), inForm);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        TranslateTransition transIn = new TranslateTransition(Duration.millis(400), inForm);
        transIn.setToX(inX);

        ParallelTransition parallel = new ParallelTransition(fadeOut, transOut, fadeIn, transIn);
        parallel.setOnFinished(e -> {
            outForm.setVisible(false);
            outForm.setTranslateX(0); // Reset để dùng cho lần sau
        });
        parallel.play();
    }

    @FXML
    private void handleLogin() {
        String identifier = loginUser.getText(); // Có thể là username hoặc email
        String pass = loginPass.getText();
        System.out.println("Login attempt: " + identifier);
        // Gọi Service xử lý DB tại đây
    }

    @FXML
    private void handleRegister() {
        String user = regUser.getText();
        String email = regEmail.getText();
        String pass = regPass.getText();
        String confirm = regConfirmPass.getText();

        if (!pass.equals(confirm)) {
            System.out.println("Passwords do not match!");
            return;
        }
        System.out.println("Registering: " + user + " with email: " + email);
        // Gọi Service xử lý DB tại đây
    }
}