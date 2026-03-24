package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.Objects;

/**
 * Complaint/support ticket model for customer-provider communication.
 * Captures the request details, status, and lifecycle timestamps.
 *
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-03-23
 */
public class Complaint {

    /** Firestore document ID */
    @DocumentId
    private String id;

    /** Provider ID who receives the complaint */
    private String providerId;

    /** Customer ID who submitted the complaint */
    private String customerId;

    /** Provider display name */
    private String providerName;

    /** Provider contact email */
    private String providerEmail;

    /** Customer display name */
    private String customerName;

    /** Customer contact email */
    private String customerEmail;

    /** Complaint category (Delivery Issue, Billing Issue, etc.) */
    private String category;

    /** Short subject line */
    private String subject;

    /** Full complaint description */
    private String message;

    /** Status: OPEN, IN_PROGRESS, RESOLVED */
    private String status;

    /** Complaint creation timestamp */
    private Timestamp createdAt;

    /** Complaint last update timestamp */
    private Timestamp updatedAt;

    /** Complaint resolution timestamp */
    private Timestamp resolvedAt;

    public Complaint() {
        // Firestore requires a public no-arg constructor
    }

    public Complaint(String providerId, String customerId, String subject, String message) {
        this.providerId = providerId;
        this.customerId = customerId;
        this.subject = subject;
        this.message = message;
        this.status = "OPEN";
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

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

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public void setProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    @Override
    public String toString() {
        return "Complaint{" +
            "id='" + id + '\'' +
            ", providerId='" + providerId + '\'' +
            ", customerId='" + customerId + '\'' +
            ", category='" + category + '\'' +
            ", subject='" + subject + '\'' +
            ", status='" + status + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Complaint complaint = (Complaint) o;
        return Objects.equals(id, complaint.id)
            && Objects.equals(providerId, complaint.providerId)
            && Objects.equals(customerId, complaint.customerId)
            && Objects.equals(category, complaint.category)
            && Objects.equals(subject, complaint.subject)
            && Objects.equals(status, complaint.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, providerId, customerId, category, subject, status);
    }
}
