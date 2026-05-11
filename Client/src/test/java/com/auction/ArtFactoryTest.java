package com.auction;

import com.auction.client.factory.ArtFactory;
import com.auction.client.model.Art;
import com.auction.client.model.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArtFactoryTest {

    @Test
    void testCreateItem() {

        ArtFactory factory = new ArtFactory("Mona Lisa", "Famous painting", 1000000, "Leonardo da Vinci", 1503);

        Item item = factory.createItem();

        assertNotNull(item);

        assertTrue(item instanceof Art);

        Art art = (Art) item;

        assertEquals("Mona Lisa", art.getName());
        assertEquals("Famous painting", art.getDescription());
        assertEquals(1000000, art.getStartingPrice());

        assertEquals("Leonardo da Vinci", art.getArtist());
        assertEquals(1503, art.getYear());

        assertEquals("Art", art.getCategory());
    }
}