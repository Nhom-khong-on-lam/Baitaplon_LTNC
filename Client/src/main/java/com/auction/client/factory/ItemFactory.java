package com.auction.client.factory;

import com.auction.client.model.Item;

public abstract class ItemFactory {
    public abstract Item createItem(String name, String description, double price);
}
