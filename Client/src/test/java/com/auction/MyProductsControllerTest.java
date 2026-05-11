package com.auction;

import com.auction.client.Enum.SystemRole;
import com.auction.client.controller.MyProductsController;
import com.auction.client.model.Auction;
import com.auction.client.model.Item;
import com.auction.client.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MyProductsControllerTest {

    private MyProductsController controller;

    @BeforeEach
    void setup() {

        controller = new MyProductsController();
    }

    // =========================================
    // TEST currentUser
    // =========================================
    @Test
    void testSetCurrentUser() throws Exception {

        User user = new User(1L, "chau", "123", "chau@gmail.com", SystemRole.USER);

        setField("currentUser", user);

        User result = (User) getField("currentUser");

        assertEquals("chau", result.getUsername());
    }

    // =========================================
    // TEST myProducts
    // =========================================
    @Test
    void testSetMyProducts() throws Exception {

        List<Auction> auctions = List.of(createAuction(true), createAuction(false));

        setField("myProducts", auctions);

        List<Auction> result = (List<Auction>) getField("myProducts");

        assertEquals(2, result.size());
    }

    // =========================================
    // TEST live auction
    // =========================================
    @Test
    void testLiveAuction() {

        Auction auction = createAuction(true);

        assertTrue(auction.isLive());
    }

    // =========================================
    // TEST finished auction
    // =========================================
    @Test
    void testFinishedAuction() {

        Auction auction = createAuction(false);

        assertFalse(auction.isLive());
    }

    // =========================================
    // TEST current price
    // =========================================
    @Test
    void testCurrentPrice() {

        Auction auction = createAuction(true);

        auction.setCurrentPrice(2000);

        assertEquals(2000, auction.getCurrentPrice());
    }

    // =========================================
    // TEST reflection method
    // =========================================
    @Test
    void testPrivateMethodExists() throws Exception {

        Method method = MyProductsController.class.getDeclaredMethod("loadTable", List.class);

        assertNotNull(method);
    }

    // =========================================
    // CREATE AUCTION
    // =========================================
    private Auction createAuction(boolean live) {

        User seller = new User(1L, "chau", "123", "chau@gmail.com", SystemRole.USER);

        // Item abstract -> anonymous class
        Item item = new Item() {

            @Override
            public String getCategory() {
                return "Electronics";
            }
        };

        item.setName("Laptop");

        LocalDateTime endTime;

        if (live) {
            endTime = LocalDateTime.now().plusDays(1);
        } else {
            endTime = LocalDateTime.now().minusDays(1);
        }

        Auction auction =
                new Auction(item, seller, endTime);

        auction.setCurrentPrice(1500);

        return auction;
    }

    // =========================================
    // REFLECTION HELPERS
    // =========================================
    private void setField(String fieldName, Object value)
            throws Exception {

        Field field =
                MyProductsController.class.getDeclaredField(fieldName);

        field.setAccessible(true);

        field.set(controller, value);
    }

    private Object getField(String fieldName)
            throws Exception {

        Field field = MyProductsController.class.getDeclaredField(fieldName);

        field.setAccessible(true);

        return field.get(controller);
    }
}