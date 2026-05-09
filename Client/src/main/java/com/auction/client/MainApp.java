package com.auction.client;

import com.auction.client.controller.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        // Khởi tạo SceneManager với stage và kích thước mặc định
        SceneManager.get().init(stage, 1100, 700);

        // Hiện stage trước khi navigate
        stage.show();

        // Bắt đầu từ Splash Screen
        SceneManager.get().navigate(SceneManager.Screen.SPLASH);
    }

    public static void main(String[] args) {
        launch();
    }
}