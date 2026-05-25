package com.auction.client.controller;

import javafx.animation.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * SceneManager — Singleton điều hướng toàn bộ màn hình.
 *
 * Dùng:
 *   SceneManager.get().navigate(Screen.LOGIN);
 *   SceneManager.get().navigate(Screen.DASHBOARD, ctrl -> ctrl.setUser(user));
 */
public class SceneManager {

    // ── Enum tất cả màn hình ───────────────────────────────
    public enum Screen {
        SPLASH   ("/com/auction/client/splash.fxml"),
        LOGIN    ("/com/auction/client/login.fxml"),
        REGISTER ("/com/auction/client/register.fxml"),
        MAIN     ("/com/auction/client/main.fxml"),   // Shell chứa sidebar + content
        ADMIN_MAIN ("/com/auction/client/admin_main.fxml");

        public final String path;
        Screen(String p) { this.path = p; }
    }

    // ── Singleton ──────────────────────────────────────────
    private static final SceneManager INSTANCE = new SceneManager();
    public  static SceneManager get() { return INSTANCE; }
    private SceneManager() {}

    // ── Fields ─────────────────────────────────────────────
    private Stage stage;
    private Scene scene;

    /** Gọi một lần duy nhất từ Main.start() */
    public void init(Stage stage, double w, double h) {
        this.stage = stage;
        this.scene = new Scene(new javafx.scene.layout.StackPane(), w, h);
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(660);
        stage.setTitle("AURUM — Auction Platform");
    }

    /** Chuyển màn hình với fade animation */
    public void navigate(Screen screen) {
        navigate(screen, null);
    }

    public <T> void navigate(Screen screen, Consumer<T> setup) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(screen.path));
            Parent next = loader.load();

            String css = getClass()
                    .getResource("/com/auction/client/global.css")
                    .toExternalForm();
            if (!next.getStylesheets().contains(css))
                next.getStylesheets().add(css);

            Parent prev = scene.getRoot();
            FadeTransition out = new FadeTransition(Duration.millis(160), prev);
            out.setToValue(0);
            out.setOnFinished(e -> {
                next.setOpacity(0);
                scene.setRoot(next);          // scene có root rồi

                if (setup != null)            // callback chạy sau
                    setup.accept(loader.getController());

                FadeTransition in = new FadeTransition(Duration.millis(240), next);
                in.setToValue(1);
                in.play();
            });
            out.play();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Stage getStage() { return stage; }
    public Scene  getScene() { return scene; }
}