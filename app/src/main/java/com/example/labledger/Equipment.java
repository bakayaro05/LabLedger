
package com.example.labledger;

public class Equipment {

    private final int id;
    private final String name;
    private final String category;
    private final String location;
    private final int totalQuantity;
    private final int availableQuantity;

    public Equipment(int id, String name, String category, String location,
                     int totalQuantity, int availableQuantity) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.location = location;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getBorrowedQuantity() { return totalQuantity - availableQuantity; }
}
