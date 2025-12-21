package com.dailyserviceapp.data;

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

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
