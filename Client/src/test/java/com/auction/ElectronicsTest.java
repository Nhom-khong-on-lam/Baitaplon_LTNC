package com.auction;

import com.auction.client.model.Electronics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ElectronicsTest {

    @Test
    void testConstructor() {

        Electronics e = new Electronics("Laptop", "Gaming laptop", 1500, "Dell", "G15");

        assertEquals("Laptop", e.getName());
        assertEquals("Gaming laptop", e.getDescription());
        assertEquals(1500, e.getStartingPrice());
        assertEquals("Dell", e.getBrand());
        assertEquals("G15", e.getModel());
    }

    @Test
    void testGetCategory() {

        Electronics e = new Electronics("Phone", "iPhone", 1000, "Apple", "15 Pro");

        assertEquals("Electronics", e.getCategory());
    }
}