package com.auction;


import com.auction.common.model.Item;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemTest {

    // =========================
    // TEST constructor đầy đủ
    // =========================
    @Test
    void testFullConstructor() {

        LocalDateTime endTime = LocalDateTime.now().plusDays(1);

        Item item = new Item(1L, "Laptop", "Gaming laptop", 1000, 10L, "chau", endTime) {
            @Override
            public String getCategory() {
                return "Electronics";
            }
        };

        assertEquals(1L, item.getId());

        assertEquals("Laptop", item.getName());

        assertEquals("Gaming laptop", item.getDescription());

        assertEquals(1000, item.getStartingPrice());

        assertEquals(1000, item.getCurrentPrice());

        assertEquals(10L, item.getSellerId());

        assertEquals("chau", item.getSellerName());

        assertEquals(endTime, item.getEndTime());

        assertEquals("ACTIVE", item.getStatus());
    }

    // =========================
    // TEST constructor đơn giản
    // =========================
    @Test
    void testSimpleConstructor() {

        Item item = new Item("Phone", "Iphone", 500) {
            @Override
            public String getCategory() {
                return "Electronics";
            }
        };

        assertEquals("Phone", item.getName());

        assertEquals("Iphone", item.getDescription());

        assertEquals(500, item.getStartingPrice());

        assertEquals(500, item.getCurrentPrice());

        assertEquals("ACTIVE", item.getStatus());
    }

    @Test
    void testNegativeStartingPrice() {

        Item item = new Item(1L, "Camera", "Canon", -100, 1L, "chau",LocalDateTime.now().plusDays(1)) {
            @Override
            public String getCategory() {
                return "Electronics";
            }
        };

        assertEquals(0, item.getStartingPrice());

        assertEquals(0, item.getCurrentPrice());
    }

    @Test
    void testIdGetterSetter() {

        Item item = createItem();

        item.setId(5L);

        assertEquals(5L, item.getId());
    }

    @Test
    void testNameGetterSetter() {

        Item item = createItem();

        item.setName("Laptop");

        assertEquals("Laptop", item.getName());
    }

    @Test
    void testDescriptionGetterSetter() {

        Item item = createItem();

        item.setDescription("Gaming");

        assertEquals("Gaming", item.getDescription());
    }

    @Test
    void testPriceGetterSetter() {

        Item item = createItem();

        item.setStartingPrice(2000);

        item.setCurrentPrice(2500);

        assertEquals(2000, item.getStartingPrice());

        assertEquals(2500, item.getCurrentPrice());
    }

    @Test
    void testSellerGetterSetter() {

        Item item = createItem();

        item.setSellerId(99L);

        item.setSellerName("admin");

        assertEquals(99L, item.getSellerId());

        assertEquals("admin", item.getSellerName());
    }

    @Test
    void testStatusGetterSetter() {

        Item item = createItem();

        item.setStatus("FINISHED");

        assertEquals("FINISHED", item.getStatus());
    }

    @Test
    void testGetCategory() {

        Item item = createItem();

        assertEquals("Electronics", item.getCategory());
    }

    private Item createItem() {

        return new Item() {
            @Override
            public String getCategory() {
                return "Electronics";
            }
        };
    }
}