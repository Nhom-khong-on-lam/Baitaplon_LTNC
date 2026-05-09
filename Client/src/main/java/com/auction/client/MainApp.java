package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Kiểm tra URL xem file có tồn tại không
        var resource = MainApp.class.getResource("/com/auction/client/auth.fxml");
        if (resource == null) {
            System.err.println("LỖI: Không tìm thấy file auth.fxml tại đường dẫn đã cho!");
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(resource);

        try {
            // 2. Load giao diện
            Scene scene = new Scene(fxmlLoader.load(), 900, 650);

            stage.setTitle("Auction System");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            // In ra chi tiết lỗi bên trong file FXML
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}