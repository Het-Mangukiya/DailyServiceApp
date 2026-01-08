package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

public class Bill {
    @DocumentId
    private String id;
    
    private String providerId;
    private String customerId;
    private int month; // 0-11
    private int year;
    private double totalAmount;
    private int daysServed;
    private String paymentStatus; // PENDING, PARTIAL, PAID, OVERDUE
    private String pdfUrl;
    private List<BillItem> items;
    private List<ExtraCharge> extras;
    private List<Adjustment> adjustments;
    private Timestamp createdAt;
    private Timestamp dueDate;
    
    public Bill() {
        // Empty constructor required for Firestore
        this.items = new ArrayList<>();
        this.extras = new ArrayList<>();
        this.adjustments = new ArrayList<>();
    }
    
    public Bill(String providerId, String customerId, int month, int year) {
        this.providerId = providerId;
        this.customerId = customerId;
        this.month = month;
        this.year = year;
        this.paymentStatus = "PENDING";
        this.items = new ArrayList<>();
        this.extras = new ArrayList<>();
        this.adjustments = new ArrayList<>();
        this.createdAt = Timestamp.now();
    }
    
    // Getters and Setters
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
    
    public int getMonth() {
        return month;
    }
    
    public void setMonth(int month) {
        this.month = month;
    }
    
    public int getYear() {
        return year;
    }
    
    public void setYear(int year) {
        this.year = year;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public int getDaysServed() {
        return daysServed;
    }
    
    public void setDaysServed(int daysServed) {
        this.daysServed = daysServed;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getPdfUrl() {
        return pdfUrl;
    }
    
    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }
    
    public List<BillItem> getItems() {
        return items;
    }
    
    public void setItems(List<BillItem> items) {
        this.items = items;
    }
    
    public List<ExtraCharge> getExtras() {
        return extras;
    }
    
    public void setExtras(List<ExtraCharge> extras) {
        this.extras = extras;
    }
    
    public List<Adjustment> getAdjustments() {
        return adjustments;
    }
    
    public void setAdjustments(List<Adjustment> adjustments) {
        this.adjustments = adjustments;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }
    
    // Inner classes for nested objects
    public static class BillItem {
        private String description;
        private double rate;
        private int quantity;
        private double amount;
        
        public BillItem() {}
        
        public BillItem(String description, double rate, int quantity, double amount) {
            this.description = description;
            this.rate = rate;
            this.quantity = quantity;
            this.amount = amount;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public double getRate() {
            return rate;
        }
        
        public void setRate(double rate) {
            this.rate = rate;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
        
        public double getAmount() {
            return amount;
        }
        
        public void setAmount(double amount) {
            this.amount = amount;
        }
    }
    
    public static class ExtraCharge {
        private String description;
        private double amount;
        
        public ExtraCharge() {}
        
        public ExtraCharge(String description, double amount) {
            this.description = description;
            this.amount = amount;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public double getAmount() {
            return amount;
        }
        
        public void setAmount(double amount) {
            this.amount = amount;
        }
    }
    
    public static class Adjustment {
        private String description;
        private double amount; // Positive for additions, negative for discounts
        
        public Adjustment() {}
        
        public Adjustment(String description, double amount) {
            this.description = description;
            this.amount = amount;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public double getAmount() {
            return amount;
        }
        
        public void setAmount(double amount) {
            this.amount = amount;
        }
    }
}
