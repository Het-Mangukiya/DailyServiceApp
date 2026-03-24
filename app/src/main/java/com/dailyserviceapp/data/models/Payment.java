package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Payment data model representing a payment transaction.
 * Records payment details including amount, method, and associated bill.
 * 
 * <p>Supports multiple payment methods:</p>
 * <ul>
 *   <li>Cash</li>
 *   <li>UPI (Google Pay, PhonePe, Paytm, etc.)</li>
 *   <li>Bank Transfer</li>
 *   <li>Cheque</li>
 *   <li>Other</li>
 * </ul>
 * 
 * <p>Payments are linked to bills and update bill payment status.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class Payment {
    /** Firestore document ID */
    @DocumentId
    private String id;
    
    /** Associated bill ID */
    private String billId;
    
    /** Provider ID who received the payment */
    private String providerId;
    
    /** Customer ID who made the payment */
    private String customerId;
    
    /** Payment amount */
    private double amount;
    
    /** Payment method: Cash, UPI, Bank Transfer, Cheque, or Other */
    private String paymentMethod;
    
    /** Date and time payment was received */
    private Timestamp paymentDate;
    
    /** Additional notes about the payment */
    private String notes;
    
    /** Payment record creation timestamp */
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
