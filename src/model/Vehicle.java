package model;

public class Vehicle extends Item{
    private String make;
    private String model;
    private int year;

    public Vehicle(String name, String description, double startingPrice,
                   String make, String model, int year) {
        super(name, description, startingPrice);
        this.make = make;
        this.model = model;
        this.year = year;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    @Override
    public String getCategory() {
        return "Vehicle";
    }
}
