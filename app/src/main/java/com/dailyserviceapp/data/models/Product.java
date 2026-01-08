package com.dailyserviceapp.data.models;

import java.io.Serializable;

/**
 * Product model representing an inventory item.
 * 
 * @author DailyDrop Team
 * @version 1.0
 */
public class Product implements Serializable {
    private String id;
    private String name;
    private String category;
    private double price;
    private int quantity;
    private String description;
    private String providerId;
    private long createdAt;
    private long updatedAt;

    // Default constructor required for Firestore
    public Product() {
    }

    public Product(String name, String category, double price, int quantity, String description, String providerId) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.description = description;
        this.providerId = providerId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Calculate total value of this product (price * quantity)
     */
    public double getTotalValue() {
        return price * quantity;
    }
}

