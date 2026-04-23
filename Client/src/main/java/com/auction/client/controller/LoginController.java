package com.auction.client.controller;

import com.auction.client.model.User;
import com.auction.client.service.AuthService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import java.io.IOException;

public class LoginController {
    private AuthController parent;
    private TextField loginUser;
    private PasswordField loginPass;
    private Label loginMsg;
    private final AuthService authService = new AuthService();

    public void inject(AuthController parent, TextField user, PasswordField pass, Label msg) {
        this.parent = parent;
        this.loginUser = user;
        this.loginPass = pass;
        this.loginMsg = msg;
    }

    public void handleLogin() {
        String userInput = parent.safeGet(loginUser);
        String passInput = parent.safeGet(loginPass);

        if (parent.isEmpty(userInput, passInput)) {
            parent.showMessage(loginMsg, "Username and password are required!", false);
            return;
        }

        // Xác thực
        User currentUser = authService.authenticate(userInput, passInput);
        if (currentUser == null) {
            parent.showMessage(loginMsg, "Invalid username or password!", false);
            return;
        }

        parent.showMessage(loginMsg, "Login successful! Redirecting...", true);

        // Chuyển scene dựa trên role
        String fxmlPath;
        if (currentUser.isAdmin()) {
            fxmlPath = "/com/auction/client/adminHome.fxml";
        } else {
            fxmlPath = "/com/auction/client/userHome.fxml";
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof UserHomeController) {
                ((UserHomeController) controller).setUser(currentUser);
            } else if (controller instanceof AdminHomeController) {
                ((AdminHomeController) controller).setUser(currentUser);
            }

            Scene scene = loginUser.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            parent.showMessage(loginMsg, "Cannot load home page!", false);
        }
    }
}