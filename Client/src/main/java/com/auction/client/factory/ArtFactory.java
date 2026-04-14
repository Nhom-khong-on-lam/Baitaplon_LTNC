package com.auction.client.factory;

import com.auction.client.model.Item;
import com.auction.client.model.Art;

public class ArtFactory extends ItemFactory{
    @Override
    public Item createItem(String name, String description, double price) {
        return new Art(name, description, price, "Unknown Artist",2020);
}

}
