package com.auction;

import com.auction.client.controller.DashboardController;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardControllerTest {

    private DashboardController controller;

    @BeforeAll
    static void initFX() throws Exception {

        CountDownLatch latch =
                new CountDownLatch(1);

        Platform.startup(latch::countDown);

        latch.await();
    }

    @BeforeEach
    void setUp() throws Exception {

        controller = new DashboardController();

        // inject fake controls
        setField("welcomeGreeting", new Label());

        setField("welcomeSub", new Label());

        setField("welcomeBadge", new Label());

        setField("welcomeDate", new Label());

        setField("statActiveAuctions", new Label());

        setField("statMyBids", new Label());

        setField("statWinning", new Label());

        setField("statMyProducts", new Label());

        setField("statAuctionDelta", new Label());

        setField("statBidDelta", new Label());

        setField("liveAuctionList", new VBox());

        setField("activityList", new VBox());
    }

    @Test
    void testWelcomeGreetingLabel() throws Exception {

        Label label =
                getLabel("welcomeGreeting");

        label.setText("Hello");

        assertEquals(
                "Hello",
                label.getText()
        );
    }

    @Test
    void testWelcomeSubLabel() throws Exception {

        Label label =
                getLabel("welcomeSub");

        label.setText("Auction running");

        assertEquals(
                "Auction running",
                label.getText()
        );
    }

    @Test
    void testWelcomeBadgeLabel() throws Exception {

        Label label =
                getLabel("welcomeBadge");

        label.setText("LIVE");

        assertEquals(
                "LIVE",
                label.getText()
        );
    }

    @Test
    void testStatActiveAuctionsLabel()
            throws Exception {

        Label label =
                getLabel("statActiveAuctions");

        label.setText("5");

        assertEquals(
                "5",
                label.getText()
        );
    }

    @Test
    void testVBoxCreated() throws Exception {

        VBox box =
                (VBox) getField("liveAuctionList");

        assertNotNull(box);
    }

    @Test
    void testActivityVBoxCreated()
            throws Exception {

        VBox box =
                (VBox) getField("activityList");

        assertNotNull(box);
    }

    @Test
    void testAddActivityItem()
            throws Exception {

        Method method =
                controller.getClass()
                        .getDeclaredMethod(
                                "addActivityItem",
                                String.class,
                                String.class,
                                String.class,
                                String.class
                        );

        method.setAccessible(true);

        VBox activityList =
                (VBox) getField("activityList");

        int before =
                activityList.getChildren().size();

        method.invoke(
                controller,
                "🏆",
                "Winning auction",
                "Live",
                "#16a34a"
        );

        int after =
                activityList.getChildren().size();

        assertEquals(
                before + 1,
                after
        );
    }

    @Test
    void testInjectWelcomeGreeting()
            throws Exception {

        Label label = new Label();

        Field field =
                controller.getClass()
                        .getDeclaredField(
                                "welcomeGreeting"
                        );

        field.setAccessible(true);

        field.set(controller, label);

        assertNotNull(field.get(controller));
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
}