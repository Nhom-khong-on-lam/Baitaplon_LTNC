package com.auction;

import com.auction.client.controller.AdminMainController;


import com.auction.client.controller.SceneManager;
import com.auction.client.controller.SessionManager;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.User;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AdminMainControllerTest {

    private AdminMainController controller;
    private User mockAdmin;
    private static MockedStatic<SessionManager> mockedSession;
    private static MockedStatic<SceneManager> mockedScene;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        // XÓA BỎ các dòng System.setProperty("glass.platform", "Monocle")...
        // Chỉ giữ lại việc khởi tạo Platform mặc định
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            // Nếu Toolkit đã chạy rồi thì thôi
            latch.countDown();
        }
        latch.await();

        // Khởi tạo Mock Static một lần ở đây để dùng chung (tránh leak)
        mockedSession = mockStatic(SessionManager.class);
        mockedScene = mockStatic(SceneManager.class);

        SessionManager sess = mock(SessionManager.class);
        SceneManager sce = mock(SceneManager.class);

        mockedSession.when(SessionManager::get).thenReturn(sess);
        mockedScene.when(SceneManager::get).thenReturn(sce);
    }

    @AfterAll
    static void closeMocks() {
        if (mockedSession != null) mockedSession.close();
        if (mockedScene != null) mockedScene.close();
    }

    @BeforeEach
    void setUp() {
        controller = new AdminMainController();
        mockAdmin = new User(1L, "admin_test", "hash", "email@test.com", SystemRole.ADMIN);

        // Inject các thành phần thật (Real Objects)
        injectFields();
    }

    private void injectFields() {
        // Sử dụng đối tượng thật để tránh lỗi "Mockito cannot mock this class"
        setPrivateField(controller, "sidebar", new VBox());
        setPrivateField(controller, "navDashboard", new Button());
        setPrivateField(controller, "navUsers", new Button());
        setPrivateField(controller, "navAuctions", new Button());
        setPrivateField(controller, "sidebarAvatar", new Label());
        setPrivateField(controller, "sidebarUserName", new Label());
        setPrivateField(controller, "topbarTitle", new Label());
        setPrivateField(controller, "topbarBread", new Label());
        setPrivateField(controller, "topbarAvatar", new Label());
        setPrivateField(controller, "topbarSearch", new TextField());
        setPrivateField(controller, "contentPane", new StackPane());
    }

    @Test
    void testNavUsers_Logic() {
        setPrivateField(controller, "currentAdmin", mockAdmin);
        controller.navUsers();

        Label title = (Label) getPrivateField(controller, "topbarTitle");
        assertEquals("Manage Users", title.getText());
    }

    @Test
    void testHandleLogout_Logic() {
        controller.handleLogout();
        // Kiểm tra xem logout có được gọi thông qua Singleton không
        verify(SessionManager.get()).logout();
        verify(SceneManager.get()).navigate(SceneManager.Screen.LOGIN);
    }

    @Test
    void testHandleSearch_TriggersNavigation() {
        TextField search = (TextField) getPrivateField(controller, "topbarSearch");
        search.setText("Gia Bao");
        setPrivateField(controller, "currentAdmin", mockAdmin);

        controller.handleSearch();

        Label title = (Label) getPrivateField(controller, "topbarTitle");
        assertEquals("Search: Gia Bao", title.getText());
    }

    // --- Reflection Helpers ---
    private void setPrivateField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Object getPrivateField(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) { return null; }
    }
}