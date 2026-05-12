package com.auction;

import com.auction.client.model.Art;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArtTest {

    @Test
    void testConstructor() {

        Art art = new Art("Mona Lisa", "Famous painting", 1000000, "Leonardo da Vinci", 1503);

        assertEquals("Mona Lisa", art.getName());
        assertEquals("Famous painting", art.getDescription());
        assertEquals(1000000, art.getStartingPrice());

        assertEquals("Leonardo da Vinci", art.getArtist());
        assertEquals(1503, art.getYear());
    }

    @Test
    void testGetCategory() {

        Art art = new Art("Starry Night","Oil painting", 500000, "Vincent van Gogh", 1889);

        assertEquals("Art", art.getCategory());
    }

    @Test
    void testSettersFromItem() {

        Art art = new Art("Sketch", "Simple art", 2000, "Unknown", 2020);

        art.setCurrentPrice(3500);
        art.setSellerName("Chau");

        assertEquals(3500, art.getCurrentPrice());
        assertEquals("Chau", art.getSellerName());
    }
}