package com.auction;

import com.auction.client.controller.LoginController;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class LoginControllerTest {

    private LoginController controller;

    @BeforeAll
    static void initFX() throws Exception {

        CountDownLatch latch =
                new CountDownLatch(1);

        Platform.startup(latch::countDown);

        latch.await();
    }

    @BeforeEach
    void setUp() throws Exception {

        controller = new LoginController();

        // inject fake controls
        setField("loginUser", new TextField());

        setField("loginPass", new PasswordField());

        setField("rememberCheck", new CheckBox());

        setField("loginMsg", new Label());

        setField("loginBtn", new Button());

        setField("formPanel", new VBox());
    }

    @Test
    void testLoginUserField() throws Exception {

        TextField field =
                getTextField("loginUser");

        field.setText("admin");

        assertEquals(
                "admin",
                field.getText()
        );
    }

    @Test
    void testLoginPasswordField()
            throws Exception {

        PasswordField field =
                getPasswordField("loginPass");

        field.setText("123456");

        assertEquals(
                "123456",
                field.getText()
        );
    }

    @Test
    void testRememberCheckBox()
            throws Exception {

        CheckBox checkBox =
                (CheckBox) getField(
                        "rememberCheck"
                );

        checkBox.setSelected(true);

        assertTrue(checkBox.isSelected());
    }

    @Test
    void testClearMessage()
            throws Exception {

        Label label =
                getLabel("loginMsg");

        label.setText("Error");

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "clearMsg"
                        );

        method.setAccessible(true);

        method.invoke(controller);

        assertEquals(
                "",
                label.getText()
        );
    }

    @Test
    void testTrimText()
            throws Exception {

        TextField field = new TextField();

        field.setText("   admin   ");

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "trim",
                                TextInputControl.class
                        );

        method.setAccessible(true);

        String result =
                (String) method.invoke(
                        controller,
                        field
                );

        assertEquals(
                "admin",
                result
        );
    }

    @Test
    void testTrimNull()
            throws Exception {

        TextField field = new TextField();

        field.setText(null);

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "trim",
                                TextInputControl.class
                        );

        method.setAccessible(true);

        String result =
                (String) method.invoke(
                        controller,
                        field
                );

        assertEquals(
                "",
                result
        );
    }

    @Test
    void testShowError()
            throws Exception {

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "showError",
                                String.class
                        );

        method.setAccessible(true);

        method.invoke(
                controller,
                "Invalid login"
        );

        Label label =
                getLabel("loginMsg");

        assertTrue(
                label.getText()
                        .contains("Invalid login")
        );
    }

    @Test
    void testShowSuccess()
            throws Exception {

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "showSuccess",
                                String.class
                        );

        method.setAccessible(true);

        method.invoke(
                controller,
                "Success"
        );

        Label label =
                getLabel("loginMsg");

        assertTrue(
                label.getText()
                        .contains("Success")
        );
    }

    @Test
    void testShowInfo()
            throws Exception {

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "showInfo",
                                String.class
                        );

        method.setAccessible(true);

        method.invoke(
                controller,
                "Reset email sent"
        );

        Label label =
                getLabel("loginMsg");

        assertTrue(
                label.getText()
                        .contains("Reset email sent")
        );
    }

    @Test
    void testHandleLogin_EmptyFields()
            throws Exception {

        getTextField("loginUser")
                .setText("");

        getPasswordField("loginPass")
                .setText("");

        controller.handleLogin();

        Label label =
                getLabel("loginMsg");

        assertTrue(
                label.getText()
                        .contains("Please fill")
        );
    }

    @Test
    void testInitialize()
            throws Exception {

        controller.initialize();

        assertNotNull(
                getField("formPanel")
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

    private TextField getTextField(
            String name
    ) throws Exception {

        return (TextField) getField(name);
    }

    private PasswordField getPasswordField(
            String name
    ) throws Exception {

        return (PasswordField) getField(name);
    }

    private Label getLabel(String name)
            throws Exception {

        return (Label) getField(name);
    }
}