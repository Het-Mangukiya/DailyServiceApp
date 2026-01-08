package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class Payment {
    @DocumentId
    private String id;
    
    private String billId;
    private String providerId;
    private String customerId;
    private double amount;
    private String paymentMethod; // Cash, UPI, Bank Transfer, Cheque, Other
    private Timestamp paymentDate;
    private String notes;
    private Timestamp createdAt;
    
    public Payment() {
        // Empty constructor required for Firestore
    }
    
    public Payment(String billId, String providerId, String customerId, double amount, String paymentMethod, Timestamp paymentDate) {
        this.billId = billId;
        this.providerId = providerId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
        this.createdAt = Timestamp.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getBillId() {
        return billId;
    }
    
    public void setBillId(String billId) {
        this.billId = billId;
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
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public Timestamp getPaymentDate() {
        return paymentDate;
    }
    
    public void setPaymentDate(Timestamp paymentDate) {
        this.paymentDate = paymentDate;
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
}
