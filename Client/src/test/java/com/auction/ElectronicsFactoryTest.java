package com.auction;

import com.auction.client.factory.ElectronicsFactory;
import com.auction.common.model.Electronics;
import com.auction.common.model.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ElectronicsFactoryTest {

    @Test
    void testCreateItem() {

        ElectronicsFactory factory = new ElectronicsFactory(
                "Laptop",
                "Gaming laptop",
                1500,
                "Dell",
                "G15"
        );

        Item item = factory.createItem();

        assertNotNull(item);

        assertTrue(item instanceof Electronics);

        Electronics electronics = (Electronics) item;

        assertEquals("Laptop", electronics.getName());
        assertEquals("Gaming laptop", electronics.getDescription());
        assertEquals(1500, electronics.getStartingPrice());

        assertEquals("Dell", electronics.getBrand());
        assertEquals("G15", electronics.getModel());

        assertEquals("Electronics", electronics.getCategory());
    }
}