package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class Customer {
    @DocumentId
    private String id;

    private String name;
    private String phone;
    private String address;
    private String serviceType;
    private double ratePerUnit;
    private String providerId;
    private String status; // ACTIVE, INACTIVE
    private String notes;
    private Timestamp startDate;
    private Timestamp createdAt;

    public Customer() {
        // Firestore requires a public no-arg constructor
    }

    public Customer(String name, String phone, String address, String serviceType, double ratePerUnit, Timestamp createdAt) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.serviceType = serviceType;
        this.ratePerUnit = ratePerUnit;
        this.createdAt = createdAt;
        this.status = "ACTIVE";
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

    public String getServiceType() {
        return serviceType;
    }

    public double getRatePerUnit() {
        return ratePerUnit;
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

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setRatePerUnit(double ratePerUnit) {
        this.ratePerUnit = ratePerUnit;
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
    
    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
