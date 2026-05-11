package com.auction;

import com.auction.client.factory.VehicleFactory;
import com.auction.client.model.Item;
import com.auction.client.model.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VehicleFactoryTest {

    @Test
    void testCreateItem() {

        VehicleFactory factory = new VehicleFactory("Car", "Sports car", 50000, "Toyota", "Supra", 2022);

        Item item = factory.createItem();

        assertNotNull(item);

        assertTrue(item instanceof Vehicle);

        Vehicle vehicle = (Vehicle) item;

        assertEquals("Car", vehicle.getName());
        assertEquals("Sports car", vehicle.getDescription());
        assertEquals(50000, vehicle.getStartingPrice());

        assertEquals("Toyota", vehicle.getMake());
        assertEquals("Supra", vehicle.getModel());
        assertEquals(2022, vehicle.getYear());

        assertEquals("Vehicle", vehicle.getCategory());
    }
}