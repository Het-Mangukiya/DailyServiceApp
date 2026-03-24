package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Model representing a customer's request for extra quantity before delivery.
 * 
 * <p>Flow: Customer submits request (PENDING) → Provider approves/rejects from dashboard.</p>
 * 
 * <p>Firestore collection: quantityRequests</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-03-23
 */
public class QuantityRequest {

    /** Status constants */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @DocumentId
    private String id;

    /** Auth UID of the customer who made the request */
    private String customerId;

    /** Auth UID of the service provider */
    private String providerId;

    /** Display name for provider's dashboard */
    private String customerName;

    /** Service type: Milk, Newspaper, Water, Tiffin, etc. */
    private String serviceType;

    /** Customer's default daily quantity */
    private double currentQuantity;

    /** Requested quantity for the delivery date */
    private double requestedQuantity;

    /** The delivery date this request applies to */
    private Timestamp requestDate;

    /** PENDING, APPROVED, or REJECTED */
    private String status;

    /** Optional message from customer */
    private String note;

    /** When the provider responded */
    private Timestamp respondedAt;

    /** When the customer submitted the request */
    private Timestamp createdAt;

    /** Empty constructor required for Firestore deserialization */
    public QuantityRequest() {
        this.status = STATUS_PENDING;
    }

    public QuantityRequest(String customerId, String providerId, String customerName,
                           String serviceType, double currentQuantity, double requestedQuantity,
                           Timestamp requestDate) {
        this.customerId = customerId;
        this.providerId = providerId;
        this.customerName = customerName;
        this.serviceType = serviceType;
        this.currentQuantity = currentQuantity;
        this.requestedQuantity = requestedQuantity;
        this.requestDate = requestDate;
        this.status = STATUS_PENDING;
        this.createdAt = Timestamp.now();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public double getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(double currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public double getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(double requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Timestamp getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Timestamp requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /** Check if this request is still actionable */
    public boolean isPending() {
        return STATUS_PENDING.equals(status);
    }

    /** Check if this request was approved */
    public boolean isApproved() {
        return STATUS_APPROVED.equals(status);
    }

    /** Check if this request was rejected */
    public boolean isRejected() {
        return STATUS_REJECTED.equals(status);
    }

    /** Get the extra quantity being requested (difference) */
    public double getExtraQuantity() {
        return Math.max(0, requestedQuantity - currentQuantity);
    }
}
