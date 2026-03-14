package com.dailyserviceapp.provider;

import com.google.firebase.Timestamp;

/**
 * UI model for provider-side customer join requests.
 */
public class JoinRequestItem {

    private final String linkId;
    private final String customerId;
    private final String customerName;
    private final String customerEmail;
    private final String customerPhone;
    private final Timestamp requestedAt;

    public JoinRequestItem(String linkId, String customerId, String customerName,
                           String customerEmail, String customerPhone, Timestamp requestedAt) {
        this.linkId = linkId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.requestedAt = requestedAt;
    }

    public String getLinkId() {
        return linkId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public Timestamp getRequestedAt() {
        return requestedAt;
    }
}
