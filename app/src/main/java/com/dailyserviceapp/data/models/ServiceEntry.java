package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class ServiceEntry {
    @DocumentId
    private String id;
    
    private String providerId;
    private String customerId;
    private Timestamp date;
    private double quantity;
    private boolean delivered;
    private String notes;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    public ServiceEntry() {
        // Empty constructor required for Firestore
    }
    
    public ServiceEntry(String providerId, String customerId, Timestamp date, double quantity, boolean delivered) {
        this.providerId = providerId;
        this.customerId = customerId;
        this.date = date;
        this.quantity = quantity;
        this.delivered = delivered;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public Timestamp getDate() {
        return date;
    }
    
    public void setDate(Timestamp date) {
        this.date = date;
    }
    
    public double getQuantity() {
        return quantity;
    }
    
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }
    
    public boolean isDelivered() {
        return delivered;
    }
    
    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
