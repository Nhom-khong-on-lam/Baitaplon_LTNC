package com.auction.client.controller;

import com.auction.client.service.AuthService;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.User;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class LoginController {

    @FXML private TextField     loginUser;
    @FXML private PasswordField loginPass;
    @FXML private Label         loginMsg;
    @FXML private Button        loginBtn;
    @FXML private VBox          formPanel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        AnimationUtil.slideUp(formPanel, 24, 500);

        loginUser.setOnAction(e -> loginPass.requestFocus());
        loginPass.setOnAction(e -> handleLogin());

        loginUser.textProperty().addListener((o, ov, nv) -> clearMsg());
        loginPass.textProperty().addListener((o, ov, nv) -> clearMsg());
    }

    @FXML
    public void handleLogin() {
        String user = trim(loginUser);
        String pass = trim(loginPass);

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Please fill in all fields.");
            AnimationUtil.shake(pass.isEmpty() ? loginPass : loginUser);
            return;
        }

        loginBtn.setText("Signing in...");
        loginBtn.setDisable(true);

        PauseTransition delay = new PauseTransition(Duration.millis(400));
        delay.setOnFinished(e -> doLogin(user, pass));
        delay.play();
    }

    private void doLogin(String user, String pass) {
        // Chạy network call trên background thread để tránh đơ UI
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() {
                return authService.authenticate(user, pass);
            }
        };

        loginTask.setOnSucceeded(ev -> {
            User currentUser = loginTask.getValue();

            loginBtn.setText("Sign In");
            loginBtn.setDisable(false);
            System.out.println("DEBUG currentUser = " + currentUser);

            if (currentUser == null) {
                showError("Invalid username or password.");
                AnimationUtil.shake(loginPass);
                return;
            }

            Object status = currentUser.getAccountStatus();
            boolean isActive = (status instanceof String)
                    ? "ACTIVE".equalsIgnoreCase((String) status)
                    : status != null && status.toString().equalsIgnoreCase("ACTIVE");

            if (!isActive) {
                showError("Your account has been suspended. Contact support.");
                return;
            }

            showSuccess("Login successful! Redirecting...");
            SessionManager.get().login(currentUser);
            AnimationUtil.pulse(loginBtn);

            PauseTransition nav = new PauseTransition(Duration.millis(600));
            nav.setOnFinished(e2 -> {
                Object role = currentUser.getSystemRole();
                boolean isAdmin = (role instanceof SystemRole)
                        ? role == SystemRole.ADMIN
                        : role != null && role.toString().equalsIgnoreCase("ADMIN");

                if (isAdmin) {
                    SceneManager.get().navigate(SceneManager.Screen.ADMIN_MAIN,
                            (AdminMainController ctrl) -> ctrl.initForUser(currentUser));
                } else {
                    SceneManager.get().navigate(SceneManager.Screen.MAIN,
                            (MainController ctrl) -> ctrl.initForUser(currentUser));
                }
            });
            nav.play();
        });

        loginTask.setOnFailed(ev -> {
            loginBtn.setText("Sign In");
            loginBtn.setDisable(false);
            showError("Cannot connect to server. Please try again.");
            AnimationUtil.shake(loginPass);
        });

        new Thread(loginTask).start();
    }

    @FXML
    public void goToRegister() {
        SceneManager.get().navigate(SceneManager.Screen.REGISTER);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void showError(String msg) {
        loginMsg.setText("✕  " + msg);
        loginMsg.setTextFill(Color.web("#dc2626"));
    }

    private void showSuccess(String msg) {
        loginMsg.setText("✓  " + msg);
        loginMsg.setTextFill(Color.web("#16a34a"));
    }

    private void showInfo(String msg) {
        loginMsg.setText("ℹ  " + msg);
        loginMsg.setTextFill(Color.web("#2563eb"));
    }

    private void clearMsg() {
        loginMsg.setText("");
    }

    private String trim(TextInputControl f) {
        return f.getText() == null ? "" : f.getText().trim();
    }
}