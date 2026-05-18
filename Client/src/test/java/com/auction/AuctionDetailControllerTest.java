package com.auction;


import com.auction.client.controller.AuctionDetailController;
import com.auction.common.enums.SystemRole;
import com.auction.common.model.Auction;
import com.auction.common.model.Item;
import com.auction.common.model.User;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionDetailControllerTest {

    private AuctionDetailController controller;

    @BeforeAll
    static void initFX() throws Exception {

        CountDownLatch latch = new CountDownLatch(1);

        Platform.startup(latch::countDown);

        latch.await();
    }

    @BeforeEach
    void setUp() throws Exception {

        controller = new AuctionDetailController();

        // fake data
        User user = new User(1L, "test", "123", "test@gmail.com", SystemRole.USER);

        Item item = new Item() {
            @Override
            public String getCategory() {
                return "Test";
            }
        };

        item.setStartingPrice(1000.0);

        Auction auction = new Auction(
                item,
                user,
                LocalDateTime.now().plusDays(1)
        );

        // inject data
        setField("auction", auction);

        // inject UI
        setField("bidAmountField", new TextField());
        setField("bidMsg", new Label());
    }

    @Test
    void testEmptyInput() throws Exception {

        TextField field = getTextField();

        field.setText("");

        controller.handlePlaceBid();

        assertTrue(
                getLabel().getText()
                        .contains("Please")
        );
    }

    @Test
    void testWrongFormat() throws Exception {

        TextField field = getTextField();

        field.setText("abc");

        controller.handlePlaceBid();

        assertTrue(
                getLabel().getText()
                        .toLowerCase()
                        .contains("number")
        );
    }

    // ================= HELPER =================

    private void setField(String name, Object value)
            throws Exception {

        Field f = controller.getClass()
                .getDeclaredField(name);

        f.setAccessible(true);

        f.set(controller, value);
    }

    private TextField getTextField()
            throws Exception {

        Field f = controller.getClass()
                .getDeclaredField("bidAmountField");

        f.setAccessible(true);

        return (TextField) f.get(controller);
    }

    private Label getLabel()
            throws Exception {

        Field f = controller.getClass()
                .getDeclaredField("bidMsg");

        f.setAccessible(true);

        return (Label) f.get(controller);
    }
    @Test
    void testLowPriceLogic() {

        double minBid = 1000;

        double input = 500;

        assertTrue(input < minBid);
    }

    @Test
    void testValidNumberInput() {

        String txt = "2000";

        assertDoesNotThrow(() -> {
            Double.parseDouble(txt);
        });
    }

}

