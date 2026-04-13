package com.auction.client.model;

public abstract class Item {
    private String name;
    private String description;
    private double startingPrice;
    public Item(String name,String description,double startingPrice){
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public abstract String getCategory();
}