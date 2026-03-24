package com.dailyserviceapp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room database entity for local caching of Customers.
 * Separated from Firestore model to enforce clean architecture and offline resilience.
 */
@Entity(tableName = "customers")
public class CustomerEntity {
    
    @PrimaryKey
    @NonNull
    public String id = "";
    
    public String name;
    public String phone;
    public String address;
    public String serviceType;
    public double ratePerUnit;
    public double defaultQuantity;
    public double lentAmount;
    public String providerId;
    public String status;
    public long createdAtMillis;
    public boolean onVacation;
}
