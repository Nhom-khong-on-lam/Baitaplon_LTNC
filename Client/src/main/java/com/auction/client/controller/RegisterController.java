package com.auction.client.controller;

import com.auction.client.service.AuthService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;

public class RegisterController {

    @FXML private TextField     regUser, regEmail;
    @FXML private PasswordField regPass, regConfirm;
    @FXML private Label         userMsg, emailMsg, regMsg;
    @FXML private Button        registerBtn;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        regUser.textProperty().addListener( (o,v,n) -> userMsg.setText(""));
        regEmail.textProperty().addListener((o,v,n) -> emailMsg.setText(""));
        regPass.textProperty().addListener( (o,v,n) -> regMsg.setText(""));
        regConfirm.textProperty().addListener((o,v,n)->regMsg.setText(""));
    }

    @FXML
    public void handleRegister() {
        // 1. Get and trim data
        String user    = trim(regUser);
        String email   = trim(regEmail);
        String pass    = trim(regPass);
        String confirm = trim(regConfirm);

        // 2. Check for empty fields
        if (user.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            setMsg(regMsg, "Please fill in all fields.", false);
            return;
        }

        // 3. Check password confirmation
        if (!pass.equals(confirm)) {
            setMsg(regMsg, "Passwords do not match.", false);
            return;
        }

        // 4. Check password length
        if (pass.length() < 6) {
            setMsg(regMsg, "Password must be at least 6 characters.", false);
            return;
        }

        // 5. Try to save user via AuthService
        boolean success = authService.saveUser(user, email, pass);

        if (success) {
            setMsg(regMsg, "Registration successful! Redirecting...", true);

            // Navigate to Login after 1.2s
            javafx.animation.PauseTransition p =
                    new javafx.animation.PauseTransition(javafx.util.Duration.millis(1200));
            p.setOnFinished(e -> SceneManager.get().navigate(SceneManager.Screen.LOGIN));
            p.play();
        } else {
            // General error (Server down, DB error, or Duplicate user)
            setMsg(regMsg, "Registration failed. Username or Email already exists.", false);
        }
    }
    @FXML
    public void goToLogin() {
        SceneManager.get().navigate(SceneManager.Screen.LOGIN);
    }

    private void setMsg(Label lbl, String msg, boolean ok) {
        lbl.setText(msg);
        lbl.setTextFill(ok ? Color.web("#16a34a") : Color.web("#dc2626"));
    }

    private String trim(TextInputControl f) {
        return f.getText() == null ? "" : f.getText().trim();
    }
}