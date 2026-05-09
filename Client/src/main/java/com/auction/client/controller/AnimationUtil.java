package com.auction.client.controller;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * AnimationUtil — Tập hợp animation tái sử dụng.
 */
public final class AnimationUtil {

    private AnimationUtil() {}

    /** Fade in từ 0 → 1 */
    public static void fadeIn(Node node, int millis) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(millis), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();
    }

    /** Fade in + trượt lên từ offsetY */
    public static void slideUp(Node node, double offsetY, int millis) {
        node.setOpacity(0);
        node.setTranslateY(offsetY);
        FadeTransition ft = new FadeTransition(Duration.millis(millis), node);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(millis), node);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    /** Slide in từ trái */
    public static void slideInLeft(Node node, double offsetX, int millis) {
        node.setOpacity(0);
        node.setTranslateX(-offsetX);
        FadeTransition ft = new FadeTransition(Duration.millis(millis), node);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(millis), node);
        tt.setToX(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    /** Scale pulse (dùng cho nút bấm thành công) */
    public static void pulse(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
        st.setFromX(1); st.setFromY(1);
        st.setToX(1.06); st.setToY(1.06);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    /** Shake ngang (dùng khi validation lỗi) */
    public static void shake(Node node) {
        double ox = node.getTranslateX();
        Timeline tl = new Timeline(
                kf(node, ox,      0),
                kf(node, ox - 9, 70),
                kf(node, ox + 9,140),
                kf(node, ox - 6,210),
                kf(node, ox + 6,280),
                kf(node, ox,    350)
        );
        tl.play();
    }

    /** Đếm số từ start → end trong duration (dùng cho stat cards) */
    public static void countUp(javafx.scene.control.Label label,
                               double start, double end,
                               int millis, String prefix, String suffix) {
        Timeline tl = new Timeline();
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(millis),
                new KeyValue(new javafx.beans.property.SimpleDoubleProperty(start) {
                    @Override protected void invalidated() {
                        label.setText(prefix + String.format("%.0f", get()) + suffix);
                    }
                }, end, Interpolator.EASE_OUT)
        ));
        // Fallback đơn giản hơn:
        long steps = 40;
        double step = (end - start) / steps;
        long interval = millis / steps;
        Timeline countTl = new Timeline();
        for (int i = 0; i <= steps; i++) {
            final double val = Math.min(start + step * i, end);
            countTl.getKeyFrames().add(
                    new KeyFrame(Duration.millis(interval * i),
                            e -> label.setText(prefix + String.format("%.0f", val) + suffix)));
        }
        countTl.play();
    }

    // ── private helper ─────────────────────────────────────
    private static KeyFrame kf(Node n, double x, int ms) {
        return new KeyFrame(Duration.millis(ms),
                new KeyValue(n.translateXProperty(), x, Interpolator.EASE_BOTH));
    }
}