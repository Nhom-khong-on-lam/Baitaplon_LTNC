package factory;

import model.Electronics;
import model.Item;

public class ElectronicsFactory extends ItemFactory {
    @Override
    public Item createItem(String name, String description, double price) {
        return new Electronics(name, description, price, "Asus", "ROG");
    }
}
