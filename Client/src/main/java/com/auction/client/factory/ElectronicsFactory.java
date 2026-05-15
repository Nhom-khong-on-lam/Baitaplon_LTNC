package com.auction.client.factory;


import com.auction.common.model.Electronics;
import com.auction.common.model.Item;

public class ElectronicsFactory extends ItemFactory {
    private String name;
    private String description;
    private double price;
    private String brand;
    private String model;

    public ElectronicsFactory(String name, String description, double price, String brand, String model) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.brand = brand;
        this.model = model;
    }

    @Override
    public Item createItem() {
        return new Electronics(name, description, price, brand, model);
    }
}