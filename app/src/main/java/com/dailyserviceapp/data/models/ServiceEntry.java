package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Service Entry data model representing a daily service delivery record.
 * Tracks individual service transactions including quantity delivered,
 * delivery status, and timestamps.
 * 
 * <p>Used for recording daily deliveries such as:</p>
 * <ul>
 *   <li>Milk delivery (quantity in liters)</li>
 *   <li>Newspaper delivery (number of copies)</li>
 *   <li>Maid service (hours worked)</li>
 *   <li>Laundry service (number of items)</li>
 * </ul>
 * 
 * <p>Service entries are aggregated monthly to generate bills.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class ServiceEntry {
    /** Firestore document ID */
    @DocumentId
    private String id;
    
    /** Provider ID who delivered the service */
    private String providerId;
    
    /** Customer ID who received the service */
    private String customerId;
    
    /** Service delivery date and time */
    private Timestamp date;
    
    /** Quantity of service delivered (liters, items, hours, etc.) */
    private double quantity;
    
    /** Rate per unit for this service delivery */
    private double rate;
    
    /** Delivery status flag */
    private boolean delivered;
    
    /** Additional notes about this service entry */
    private String notes;
    
    /** Record creation timestamp */
    private Timestamp createdAt;
    
    /** Last update timestamp */
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
    
    public double getRate() {
        return rate;
    }
    
    public void setRate(double rate) {
        this.rate = rate;
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
