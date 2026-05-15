package com.auction;

import com.auction.client.controller.MainController;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class MainControllerTest {

    private MainController controller;

    @BeforeAll
    static void initFX() throws Exception {

        CountDownLatch latch =
                new CountDownLatch(1);

        Platform.startup(latch::countDown);

        latch.await();
    }

    @BeforeEach
    void setUp() throws Exception {

        controller = new MainController();

        // Sidebar
        setField("sidebar", new VBox());

        setField("navDashboard", new Button());

        setField("navAuctions", new Button());

        setField("navMyBids", new Button());

        setField("navMyProducts", new Button());

        setField("navCreate", new Button());

        setField("navProfile", new Button());

        setField("sidebarAvatar", new Label());

        setField("sidebarUserName", new Label());

        setField("sidebarUserRole", new Label());

        // Topbar
        setField("topbarTitle", new Label());

        setField("topbarBread", new Label());

        setField("topbarAvatar", new Label());

        setField("topbarSearch", new TextField());

        // Content
        setField("contentPane", new StackPane());
    }

    @Test
    void testTopbarTitle() throws Exception {

        Label label =
                getLabel("topbarTitle");

        label.setText("Dashboard");

        assertEquals(
                "Dashboard",
                label.getText()
        );
    }

    @Test
    void testTopbarBreadcrumb()
            throws Exception {

        Label label =
                getLabel("topbarBread");

        label.setText("Home / Dashboard");

        assertEquals(
                "Home / Dashboard",
                label.getText()
        );
    }

    @Test
    void testSearchField() throws Exception {

        TextField field =
                getTextField("topbarSearch");

        field.setText("Laptop");

        assertEquals(
                "Laptop",
                field.getText()
        );
    }

    @Test
    void testSetTopbar()
            throws Exception {

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "setTopbar",
                                String.class,
                                String.class
                        );

        method.setAccessible(true);

        method.invoke(
                controller,
                "Auctions",
                "Home / Auctions"
        );

        Label title =
                getLabel("topbarTitle");

        Label bread =
                getLabel("topbarBread");

        assertEquals(
                "Auctions",
                title.getText()
        );

        assertEquals(
                "Home / Auctions",
                bread.getText()
        );
    }

    @Test
    void testSetActiveButton()
            throws Exception {

        Button btn =
                (Button) getField(
                        "navDashboard"
                );

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "setActive",
                                Button.class
                        );

        method.setAccessible(true);

        method.invoke(controller, btn);

        assertTrue(
                btn.getStyleClass()
                        .contains("nav-btn-active")
        );
    }

    @Test
    void testCurrentUserInitiallyNull() {

        assertNull(
                controller.getCurrentUser()
        );
    }

    @Test
    void testContentPaneExists()
            throws Exception {

        StackPane pane =
                (StackPane) getField(
                        "contentPane"
                );

        assertNotNull(pane);
    }

    @Test
    void testSidebarExists()
            throws Exception {

        VBox sidebar =
                (VBox) getField(
                        "sidebar"
                );

        assertNotNull(sidebar);
    }

    @Test
    void testInitialize()
            throws Exception {

        controller.initialize();

        assertNotNull(
                getField("sidebar")
        );
    }

    @Test
    void testUserLabels() throws Exception {

        Label userName =
                getLabel("sidebarUserName");

        Label role =
                getLabel("sidebarUserRole");

        userName.setText("chau");

        role.setText("Member");

        assertEquals(
                "chau",
                userName.getText()
        );

        assertEquals(
                "Member",
                role.getText()
        );
    }

    @Test
    void testAvatarLabel() throws Exception {

        Label avatar =
                getLabel("sidebarAvatar");

        avatar.setText("CH");

        assertEquals(
                "CH",
                avatar.getText()
        );
    }

    // =========================
    // HELPER
    // =========================

    private void setField(
            String name,
            Object value
    ) throws Exception {

        Field field =
                controller.getClass()
                        .getDeclaredField(name);

        field.setAccessible(true);

        field.set(controller, value);
    }

    private Object getField(String name)
            throws Exception {

        Field field =
                controller.getClass()
                        .getDeclaredField(name);

        field.setAccessible(true);

        return field.get(controller);
    }

    private Label getLabel(String name)
            throws Exception {

        return (Label) getField(name);
    }

    private TextField getTextField(
            String name
    ) throws Exception {

        return (TextField) getField(name);
    }
}