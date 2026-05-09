package com.auction.client.controller;

import com.auction.client.controller.AnimationUtil;
import com.auction.client.controller.SceneManager;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SplashController {

    @FXML private VBox      logoBox;
    @FXML private VBox      progressBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label     statusLabel;

    private final String[] STATUS_MSGS = {
            "Initializing core...",
            "Loading auction data...",
            "Connecting to services...",
            "Preparing dashboard...",
            "Ready!"
    };

    @FXML
    public void initialize() {
        // Bước 1: Logo fade in + slide up
        AnimationUtil.slideUp(logoBox, 30, 600);

        // Bước 2: Progress bar xuất hiện sau 500ms
        PauseTransition delay = new PauseTransition(Duration.millis(500));
        delay.setOnFinished(e -> {
            AnimationUtil.fadeIn(progressBox, 400);
            startProgress();
        });
        delay.play();
    }

    private void startProgress() {
        // Cứ 400ms tăng progress một bước
        Timeline timeline = new Timeline();
        int steps = STATUS_MSGS.length;
        for (int i = 0; i < steps; i++) {
            final int idx = i;
            final double prog = (double)(i + 1) / steps;
            timeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(400 * (i + 1)),
                    ev -> {
                        progressBar.setProgress(prog);
                        statusLabel.setText(STATUS_MSGS[idx]);
                    }
            ));
        }
        // Chuyển sang Login sau khi xong
        timeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(400 * steps + 500),
                ev -> SceneManager.get().navigate(SceneManager.Screen.LOGIN)
        ));
        timeline.play();
    }
}