package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Customer data model representing a service customer in DailyDrop.
 * Stores customer information, service details, and pricing for a provider's customer.
 * 
 * <p>Each customer is associated with a specific provider and service type.
 * Tracks customer status (ACTIVE/INACTIVE), service rate, and subscription details.</p>
 * 
 * <p>Uses Firebase Firestore annotations for document ID mapping.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class Customer {
    /** Firestore document ID */
    @DocumentId
    private String id;

    /** Customer full name */
    private String name;
    
    /** Customer phone number */
    private String phone;
    
    /** Customer address for service delivery */
    private String address;
    
    /** Area/Locality for route planning (e.g., Sector 12, Downtown, etc.) */
    private String area;
    
    /** Type of service subscribed (Milk, Newspaper, Maid, Laundry) */
    private String serviceType;
    
    /** Rate per unit of service */
    private double ratePerUnit;
    
    /** Default quantity for daily delivery (everyday quantity) */
    private double defaultQuantity;
    
    /** Total amount lent to customer (pending payments) */
    private double lentAmount;
    
    /** Provider ID who manages this customer */
    private String providerId;
    
    /** Customer status: ACTIVE or INACTIVE */
    private String status;
    
    /** Additional notes about the customer */
    private String notes;
    
    /** Vacation mode - if true, customer is on vacation and service deliveries are paused */
    private boolean onVacation;
    
    /** Service subscription start date */
    private Timestamp startDate;
    
    /** Customer record creation timestamp */
    private Timestamp createdAt;

    public Customer() {
        // Firestore requires a public no-arg constructor
        this.defaultQuantity = 1.0; // Default to 1 unit per day
        this.lentAmount = 0.0; // Default to no pending amount
        this.onVacation = false; // Default to not on vacation
    }

    public Customer(String name, String phone, String address, String serviceType, double ratePerUnit, Timestamp createdAt) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.serviceType = serviceType;
        this.ratePerUnit = ratePerUnit;
        this.createdAt = createdAt;
        this.status = "ACTIVE";
        this.defaultQuantity = 1.0; // Default to 1 unit per day
        this.lentAmount = 0.0; // Default to no pending amount
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }
    
    public String getArea() {
        return area;
    }

    public String getServiceType() {
        return serviceType;
    }

    public double getRatePerUnit() {
        return ratePerUnit;
    }
    
    public double getDefaultQuantity() {
        return defaultQuantity;
    }
    
    public double getLentAmount() {
        return lentAmount;
    }
    
    public String getProviderId() {
        return providerId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public boolean isOnVacation() {
        return onVacation;
    }
    
    public Timestamp getStartDate() {
        return startDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
    public void setArea(String area) {
        this.area = area;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setRatePerUnit(double ratePerUnit) {
        this.ratePerUnit = ratePerUnit;
    }
    
    public void setDefaultQuantity(double defaultQuantity) {
        this.defaultQuantity = defaultQuantity;
    }
    
    public void setLentAmount(double lentAmount) {
        this.lentAmount = lentAmount;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public void setOnVacation(boolean onVacation) {
        this.onVacation = onVacation;
    }
    
    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
