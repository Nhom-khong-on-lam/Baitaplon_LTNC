package com.auction.client.factory;

import com.auction.client.model.Item;
import com.auction.client.model.Vehicle;

public class VehicleFactory extends ItemFactory{
    @Override
    public Item createItem(String name, String description, double price) {
        return new Vehicle(name,description,price,"Toyota", "Camry",2022);
    }
}