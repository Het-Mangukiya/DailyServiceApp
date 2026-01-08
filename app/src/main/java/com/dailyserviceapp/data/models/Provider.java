package com.dailyserviceapp.data.models;

import java.util.Date;

/**
 * Provider data model representing a service provider in DailyDrop.
 * Contains business details for providers who offer services like milk delivery,
 * newspaper delivery, maid service, or laundry service.
 * 
 * <p>Each provider is linked to a User account via userId field.
 * Providers can offer one or multiple service types to customers.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class Provider {
    /** Unique provider ID */
    private String id;
    
    /** Associated user ID (Firebase Auth UID) */
    private String userId;
    
    /** Business or trade name */
    private String businessName;
    
    /** Type of service offered (Milk, Newspaper, Maid, Laundry) */
    private String serviceType;
    
    /** Business address */
    private String address;
    
    /** GST number for tax purposes */
    private String gstNumber;
    
    /** Contact phone number */
    private String phone;
    
    /** Provider registration timestamp */
    private Date createdAt;
    
    public Provider() {
        // Empty constructor required for Firestore
    }
    
    public Provider(String id, String userId, String businessName, String serviceType) {
        this.id = id;
        this.userId = userId;
        this.businessName = businessName;
        this.serviceType = serviceType;
        this.createdAt = new Date();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getBusinessName() {
        return businessName;
    }
    
    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }
    
    public String getServiceType() {
        return serviceType;
    }
    
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getGstNumber() {
        return gstNumber;
    }
    
    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
