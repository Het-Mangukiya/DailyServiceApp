package com.dailyserviceapp.data;

import com.google.firebase.Timestamp;

public class DeliveryEntry {
    private String dateKey; // yyyyMMdd
    private boolean delivered;
    private Timestamp updatedAt;

    public DeliveryEntry() {
    }

    public DeliveryEntry(String dateKey, boolean delivered, Timestamp updatedAt) {
        this.dateKey = dateKey;
        this.delivered = delivered;
        this.updatedAt = updatedAt;
    }

    public String getDateKey() {
        return dateKey;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setDateKey(String dateKey) {
        this.dateKey = dateKey;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
