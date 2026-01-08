package com.dailyserviceapp.data.models;

import java.util.Date;

/**
 * User data model representing a registered user in DailyDrop.
 * Stores basic user information including authentication details and role.
 * 
 * <p>Supports two user roles:</p>
 * <ul>
 *   <li>PROVIDER - Service providers who deliver milk, newspaper, etc.</li>
 *   <li>CUSTOMER - Customers who receive services</li>
 * </ul>
 * 
 * <p>This class is designed for Firebase Firestore serialization and requires
 * a public no-argument constructor.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class User {
    /** Unique user ID (Firebase Auth UID) */
    private String id;
    
    /** User email address */
    private String email;
    
    /** User full name */
    private String name;
    
    /** User phone number */
    private String phone;
    
    /** User role: PROVIDER or CUSTOMER */
    private String role;
    
    /** Account creation timestamp */
    private Date createdAt;
    
    /** Last update timestamp */
    private Date updatedAt;
    
    public User() {
        // Empty constructor required for Firestore
    }
    
    public User(String id, String email, String name, String phone, String role) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
