package com.auction.client.factory;

import com.auction.client.model.Item;
import com.auction.client.model.Art;

public class ArtFactory extends ItemFactory {
    private String name;
    private String description;
    private double price;
    private String artist;
    private int year;
    public ArtFactory(String name, String description, double price,
                      String artist, int year) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.artist = artist;
        this.year = year;
    }
    @Override
    public Item createItem() {
        return new Art(name, description, price, artist, year);
    }
}