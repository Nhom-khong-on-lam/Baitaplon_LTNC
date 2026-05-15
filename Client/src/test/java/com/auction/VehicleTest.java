package com.auction;


import com.auction.common.model.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VehicleTest {

    @Test
    void testConstructor() {

        Vehicle v = new Vehicle("Car", "Sports car", 50000, "Toyota", "Supra", 2022);
        assertEquals("Car", v.getName());
        assertEquals("Sports car", v.getDescription());
        assertEquals(50000, v.getStartingPrice());

        assertEquals("Toyota", v.getMake());
        assertEquals("Supra", v.getModel());
        assertEquals(2022, v.getYear());
    }

    @Test
    void testGetCategory() {

        Vehicle v = new Vehicle("Bike", "Racing bike", 12000, "Yamaha", "R1", 2021);

        assertEquals("Vehicle", v.getCategory());
    }

    @Test
    void testSettersFromItem() {

        Vehicle v = new Vehicle("Old Car", "Description", 10000, "Honda", "Civic", 2018);

        v.setCurrentPrice(15000);
        v.setSellerName("Chau");

        assertEquals(15000, v.getCurrentPrice());
        assertEquals("Chau", v.getSellerName());
    }
}