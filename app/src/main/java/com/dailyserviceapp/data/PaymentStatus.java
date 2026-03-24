package com.dailyserviceapp.data;

import com.google.firebase.Timestamp;

public class PaymentStatus {
    private String monthKey; // yyyyMM
    private boolean paid;
    private double paidAmount;
    private Timestamp paidOn;

    public PaymentStatus() {
    }

    public PaymentStatus(String monthKey, boolean paid, double paidAmount, Timestamp paidOn) {
        this.monthKey = monthKey;
        this.paid = paid;
        this.paidAmount = paidAmount;
        this.paidOn = paidOn;
    }

    public String getMonthKey() {
        return monthKey;
    }

    public boolean isPaid() {
        return paid;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public Timestamp getPaidOn() {
        return paidOn;
    }

    public void setMonthKey(String monthKey) {
        this.monthKey = monthKey;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public void setPaidOn(Timestamp paidOn) {
        this.paidOn = paidOn;
    }
}
