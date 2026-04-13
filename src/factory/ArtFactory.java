package factory;

import model.Art;
import model.Item;

public class ArtFactory extends ItemFactory{    @Override
public Item createItem(String name, String description, double price) {
    return new Art(name, description, price, "Unknown Artist",2020);
    }

}
