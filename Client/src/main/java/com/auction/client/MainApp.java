package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(
                MainApp.class.getResource("/com/auction/client/auth.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(),900,650);

        stage.setTitle("Auction System");
        stage.setFullScreen(true);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}