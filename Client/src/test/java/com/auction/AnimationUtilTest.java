package com.auction;

import com.auction.client.controller.AnimationUtil;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AnimationUtilTest {

    private Node mockNode;

    @BeforeAll
    static void initJFX() throws InterruptedException {
        // Khởi chạy JavaFX Toolkit để có thể tạo các đối tượng đồ họa
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await(5, TimeUnit.SECONDS);
    }

    @BeforeEach
    void setUp() {
        // Tạo một đối tượng Pane đơn giản để làm vật thí nghiệm cho các hiệu ứng
        mockNode = new Pane();
    }

    @Test
    @DisplayName("Test: Hiệu ứng Fade In (Mờ dần đến hiện rõ)")
    void testFadeIn() {
        // Thực thi: Gọi hàm hiệu ứng
        AnimationUtil.fadeIn(mockNode, 100);

        // Kiểm tra: Thuộc tính Opacity phải được đặt về 0 để bắt đầu hiệu ứng
        assertEquals(0, mockNode.getOpacity(), "Opacity phải bắt đầu từ 0 khi gọi fadeIn");
    }

    @Test
    @DisplayName("Test: Hiệu ứng Slide Up (Trượt từ dưới lên)")
    void testSlideUp() {
        double startY = 50.0;

        // Thực thi
        AnimationUtil.slideUp(mockNode, startY, 100);

        // Kiểm tra: Node phải được đẩy xuống dưới startY trước khi trượt lên
        assertEquals(startY, mockNode.getTranslateY(), "Vị trí Y ban đầu phải khớp với offsetY truyền vào");
        assertEquals(0, mockNode.getOpacity(), "Độ mờ ban đầu phải là 0");
    }

    @Test
    @DisplayName("Test: Hiệu ứng Slide In Left (Trượt từ trái sang)")
    void testSlideInLeft() {
        double startX = 30.0;

        // Thực thi
        AnimationUtil.slideInLeft(mockNode, startX, 100);

        // Kiểm tra: Node phải nằm ở phía bên trái (giá trị âm)
        assertEquals(-startX, mockNode.getTranslateX(), "Vị trí X phải là số âm để bắt đầu trượt từ trái");
    }

    @Test
    @DisplayName("Test: Hiệu ứng Count Up (Đếm số tăng dần)")
    void testCountUp() throws InterruptedException {
        Label label = new Label("0");
        double startValue = 0;
        double endValue = 100;

        // Thực thi: Đếm từ 0 đến 100
        // Vì hàm này sử dụng Timeline chạy bất đồng bộ, chúng ta chạy trên UI Thread
        Platform.runLater(() -> {
            AnimationUtil.countUp(label, startValue, endValue, 50, "", "");
        });

        // Đợi một chút để hiệu ứng diễn ra
        Thread.sleep(200);
    }
}

        // Kiểm tra: Sau thời gian chạy, label phải hiển thị số cuối cùng [cite: Animation