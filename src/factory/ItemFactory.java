package factory;

import model.Item;

public abstract class ItemFactory {
    public abstract Item createItem(String name, String description, double price);

}
