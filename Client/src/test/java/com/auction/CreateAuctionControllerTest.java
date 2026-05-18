package com.auction;

import com.auction.client.controller.CreateAuctionController;
import javafx.application.Platform;
import javafx.scene.control.*;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class CreateAuctionControllerTest {

    private CreateAuctionController controller;

    @BeforeAll
    static void initFX() throws Exception {

        CountDownLatch latch =
                new CountDownLatch(1);

        Platform.startup(latch::countDown);

        latch.await();
    }

    @BeforeEach
    void setUp() throws Exception {

        controller = new CreateAuctionController();

        // inject fake controls
        setField("fieldTitle", new TextField());

        setField("fieldDesc", new TextArea());

        setField("fieldStartPrice", new TextField());

        setField("fieldEndTime", new TextField());

        setField("fieldCategory",
                new ComboBox<String>());

        setField("msgTitle", new Label());

        setField("msgDesc", new Label());

        setField("msgPrice", new Label());

        setField("msgTime", new Label());

        setField("msgCategory", new Label());

        setField("formMsg", new Label());

        setField("submitBtn", new Button());
    }

    @Test
    void testTitleField() throws Exception {

        TextField title =
                getTextField("fieldTitle");

        title.setText("Laptop");

        assertEquals(
                "Laptop",
                title.getText()
        );
    }

    @Test
    void testDescriptionField() throws Exception {

        TextArea desc =
                getTextArea("fieldDesc");

        desc.setText("Gaming laptop");

        assertEquals(
                "Gaming laptop",
                desc.getText()
        );
    }

    @Test
    void testPriceField() throws Exception {

        TextField price =
                getTextField("fieldStartPrice");

        price.setText("1000");

        assertEquals(
                "1000",
                price.getText()
        );
    }

    @Test
    void testCategorySelection() throws Exception {

        ComboBox<String> category =
                getComboBox("fieldCategory");

        category.getItems().add("Electronics");

        category.setValue("Electronics");

        assertEquals(
                "Electronics",
                category.getValue()
        );
    }

    @Test
    void testValidateBasic_EmptyTitle()
            throws Exception {

        getTextField("fieldTitle")
                .setText("");

        getTextField("fieldStartPrice")
                .setText("100");

        boolean result =
                callValidateBasic();

        assertFalse(result);
    }

    @Test
    void testValidateBasic_InvalidPrice()
            throws Exception {

        getTextField("fieldTitle")
                .setText("Laptop");

        getTextField("fieldStartPrice")
                .setText("abc");

        boolean result =
                callValidateBasic();

        assertFalse(result);
    }

    @Test
    void testValidateBasic_Valid()
            throws Exception {

        getTextField("fieldTitle")
                .setText("Laptop");

        getTextField("fieldStartPrice")
                .setText("1000");

        boolean result =
                callValidateBasic();

        assertTrue(result);
    }

    @Test
    void testParseDateTime_Valid()
            throws Exception {

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "parseDateTime",
                                String.class
                        );

        method.setAccessible(true);

        Object result =
                method.invoke(
                        controller,
                        "2026-05-11 20:30"
                );

        assertNotNull(result);
    }

    @Test
    void testParseDateTime_Invalid()
            throws Exception {

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "parseDateTime",
                                String.class
                        );

        method.setAccessible(true);

        Object result =
                method.invoke(
                        controller,
                        "abcxyz"
                );

        assertNull(result);
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

    private TextField getTextField(String name)
            throws Exception {

        return (TextField) getField(name);
    }

    private TextArea getTextArea(String name)
            throws Exception {

        return (TextArea) getField(name);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> getComboBox(
            String name
    ) throws Exception {

        return (ComboBox<String>)
                getField(name);
    }

    private boolean callValidateBasic()
            throws Exception {

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "validateBasic"
                        );

        method.setAccessible(true);

        return (boolean)
                method.invoke(controller);
    }
}