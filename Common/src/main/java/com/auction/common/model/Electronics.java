package com.auction.common.model;

public class Electronics extends Item{
    private String brand;
    private String model;

    public Electronics(String name, String description, double startingPrice,String brand, String model) {
        super(name, description, startingPrice);
        this.brand = brand;
        this.model = model;
    }
    public String getBrand(){
        return brand;
    }
    public String getModel(){
        return model;
    }
    @Override
    public String getCategory() {
        return "Electronics";
    }
}
