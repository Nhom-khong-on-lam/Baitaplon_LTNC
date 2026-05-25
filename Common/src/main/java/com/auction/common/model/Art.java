package com.auction.common.model;

public class Art extends Item {
    private String artist;
    private int year;

    public Art(String name, String description, double startingPrice,
               String artist, int year) {
        super(name, description, startingPrice);
        this.artist = artist;
        this.year = year;
    }

    public String getArtist() {
        return artist;
    }

    public int getYear() {
        return year;
    }

    @Override
    public String getCategory() {
        return "Art";
    }
}