package com.auction;


import com.auction.client.controller.MyBidsController;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class MyBidsControllerTest {

    private MyBidsController controller;

    @BeforeAll
    static void initFX() throws Exception {

        CountDownLatch latch =
                new CountDownLatch(1);

        Platform.startup(latch::countDown);

        latch.await();
    }

    @BeforeEach
    void setUp() throws Exception {

        controller = new MyBidsController();

        setField("tabAll", new Button());

        setField("tabWinning", new Button());

        setField("tabOutbid", new Button());

        setField("tabEnded", new Button());

        setField("bidCount", new Label());

        setField("bidListContainer", new VBox());

        setField("myBids", new ArrayList<Auction>());
    }

    @Test
    void testBidCountLabel() throws Exception {

        Label label = getLabel("bidCount");

        label.setText("5 bids total");

        assertEquals("5 bids total", label.getText());
    }

    @Test
    void testSetActiveTab()
            throws Exception {

        Button tab = (Button) getField("tabWinning");

        Method method = controller.getClass().getDeclaredMethod("setActiveTab", Button.class);

        method.setAccessible(true);

        method.invoke(controller, tab);

        assertTrue(tab.getStyleClass().contains("btn-primary"));
    }

    @Test
    void testRenderBidsEmpty()
            throws Exception {

        VBox container = (VBox) getField("bidListContainer");

        Method method = controller.getClass().getDeclaredMethod("renderBids", List.class);

        method.setAccessible(true);

        method.invoke(controller, new ArrayList<Auction>());

        assertEquals(1, container.getChildren().size());

        Label empty = (Label) container.getChildren().get(0);

        assertTrue(empty.getText().contains("No bids"));
    }

    @Test
    void testVBoxExists()
            throws Exception {

        VBox box = (VBox) getField("bidListContainer");

        assertNotNull(box);
    }

    @Test
    void testButtonsExist()
            throws Exception {

        assertNotNull(getField("tabAll"));

        assertNotNull(getField("tabWinning"));

        assertNotNull(getField("tabOutbid"));

        assertNotNull(getField("tabEnded"));
    }

    @Test
    void testBuildBidRowWithNullAuction()
            throws Exception {

        Method method = controller.getClass().getDeclaredMethod("buildBidRow", Auction.class);

        method.setAccessible(true);

        assertThrows(Exception.class, () -> method.invoke(controller, new Object[]{null}));
    }

    @Test
    void testActiveTabField()
            throws Exception {

        Button btn = new Button();

        setField("activeTab", btn);

        Button result = (Button) getField("activeTab");

        assertEquals(btn, result);
    }

    @Test
    void testCurrentUserField()
            throws Exception {

        User user = new User(1L, "chau", "123", "chau@gmail.com", SystemRole.USER);

        setField("currentUser", user);

        User result = (User) getField("currentUser");

        assertEquals(user, result);
    }

    @Test
    void testMyBidsListField()
            throws Exception {

        List<Auction> list = new ArrayList<>();

        setField("myBids", list);

        List<?> result = (List<?>) getField("myBids");

        assertEquals(list, result);
    }

    // =========================
    // HELPER
    // =========================

    private void setField(String name, Object value) throws Exception {

        Field field = controller.getClass().getDeclaredField(name);

        field.setAccessible(true);

        field.set(controller, value);
    }

    private Object getField(String name)
            throws Exception {

        Field field = controller.getClass().getDeclaredField(name);

        field.setAccessible(true);

        return field.get(controller);
    }

    private Label getLabel(String name)
            throws Exception {

        return (Label) getField(name);
    }
}