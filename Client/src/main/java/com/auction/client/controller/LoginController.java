package com.auction.client.controller;

import com.auction.client.Enum.AccountStatus;
import com.auction.client.model.User;
import com.auction.client.service.AuthService;
import com.auction.client.controller.AnimationUtil;
import com.auction.client.controller.SceneManager;
import com.auction.client.controller.SessionManager;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class LoginController {

    @FXML private TextField    loginUser;
    @FXML private PasswordField loginPass;
    @FXML private CheckBox     rememberCheck;
    @FXML private Label        loginMsg;
    @FXML private Button       loginBtn;
    @FXML private VBox         formPanel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        // Slide in form từ phải
        AnimationUtil.slideUp(formPanel, 24, 500);

        // Enter key = login
        loginUser.setOnAction(e -> loginPass.requestFocus());
        loginPass.setOnAction(e -> handleLogin());

        // Xóa error khi bắt đầu gõ
        loginUser.textProperty().addListener((o, ov, nv) -> clearMsg());
        loginPass.textProperty().addListener((o, ov, nv) -> clearMsg());
    }

    @FXML
    public void handleLogin() {
        String user = trim(loginUser);
        String pass = trim(loginPass);

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Please fill in all fields.");
            AnimationUtil.shake(loginPass.getText().isEmpty() ? loginPass : loginUser);
            return;
        }

        // Disable button + loading state
        loginBtn.setText("Signing in...");
        loginBtn.setDisable(true);

        // Simulate async (short delay)
        PauseTransition delay = new PauseTransition(Duration.millis(400));
        delay.setOnFinished(e -> doLogin(user, pass));
        delay.play();
    }

    private void doLogin(String user, String pass) {
        User currentUser = authService.authenticate(user, pass);
        loginBtn.setText("Sign In");
        loginBtn.setDisable(false);

        if (currentUser == null) {
            showError("Invalid username or password.");
            AnimationUtil.shake(loginPass);
            return;
        }
        if (currentUser.getAccountStatus() != AccountStatus.ACTIVE) {
            showError("Your account has been suspended. Contact support.");
            return;
        }

        showSuccess("Login successful! Redirecting...");
        SessionManager.get().login(currentUser);
        AnimationUtil.pulse(loginBtn);

        PauseTransition nav = new PauseTransition(Duration.millis(600));
        nav.setOnFinished(ev ->
                SceneManager.get().navigate(SceneManager.Screen.MAIN,
                        (MainController ctrl) -> ctrl.initForUser(currentUser))
        );
        nav.play();
    }

    @FXML
    public void handleForgotPassword() {
        // Sẽ mở dialog hoặc navigate tới forgot screen
        showInfo("Password reset link sent to your registered email.");
    }

    @FXML
    public void goToRegister() {
        SceneManager.get().navigate(SceneManager.Screen.REGISTER);
    }

    // ── Helpers ───────────────────────────────────────────
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
    private void clearMsg() { loginMsg.setText(""); }

    private String trim(TextInputControl f) {
        return f.getText() == null ? "" : f.getText().trim();
    }
}