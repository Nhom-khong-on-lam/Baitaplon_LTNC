package com.auction.client;

import com.auction.client.controller.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        // 1. Khởi tạo SceneManager (Singleton của bạn)
        // Truyền Stage vào để SceneManager quản lý việc chuyển màn hình
        SceneManager.get().init(stage, 1000, 600);

        // 2. Hiện cửa sổ lên (Vẫn sẽ hiện hình con Duke mặc định)
        stage.show();

        SceneManager.get().navigate(SceneManager.Screen.ADMIN_MAIN);
    }

    public static void main(String[] args) {
        // Gọi hàm launch để bắt đầu vòng đời JavaFX
        launch(args);
    }
}