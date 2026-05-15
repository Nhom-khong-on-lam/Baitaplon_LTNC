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
        // 1. Lấy dữ liệu và kiểm tra (chạy trên UI)
        String user    = trim(regUser);
        String email   = trim(regEmail);
        String pass    = trim(regPass);
        String confirm = trim(regConfirm);

        if (user.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            setMsg(regMsg, "Please fill in all fields.", false); return;
        }
        if (!pass.equals(confirm)) {
            setMsg(regMsg, "Passwords do not match.", false); return;
        }
        if (pass.length() < 6) {
            setMsg(regMsg, "Password must be at least 6 characters.", false); return;
        }

        // Tạm khóa nút và hiện thông báo chờ
        registerBtn.setDisable(true);
        setMsg(regMsg, "Processing...", true);

        // 2. Tách luồng để đăng ký lên Server
        new Thread(() -> {
            boolean success = authService.saveUser(user, email, pass);

            // 3. Trả kết quả về luồng UI
            javafx.application.Platform.runLater(() -> {
                if (success) {
                    setMsg(regMsg, "Registration successful! Redirecting...", true);
                    javafx.animation.PauseTransition p =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(1200));
                    p.setOnFinished(e -> SceneManager.get().navigate(SceneManager.Screen.LOGIN));
                    p.play();
                } else {
                    setMsg(regMsg, "Registration failed. Username or Email already exists.", false);
                    registerBtn.setDisable(false); // Mở khóa nút nếu lỗi
                }
            });
        }).start();
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
