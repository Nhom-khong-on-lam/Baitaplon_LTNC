package com.auction.client.factory;

import com.auction.client.model.Item;
import com.auction.client.model.Vehicle;

public class VehicleFactory extends ItemFactory {
    private String name;
    private String description;
    private double price;
    private String make;
    private String model;
    private int year;
    public VehicleFactory(String name, String description, double price,
                          String make, String model, int year) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.make = make;
        this.model = model;
        this.year = year;
    }

    @Override
    public Item createItem() {
        return new Vehicle(name, description, price, make, model, year);
    }
}