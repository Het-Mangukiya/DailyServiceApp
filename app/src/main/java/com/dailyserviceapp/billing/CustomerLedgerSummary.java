package com.dailyserviceapp.billing;

import com.google.firebase.Timestamp;

/**
 * Computed billing ledger summary for one customer.
 */
public class CustomerLedgerSummary {
    private String customerId;
    private String customerName;
    private int deliveredEntries;
    private double totalServiceAmount;
    private double totalPaidAmount;
    private double outstandingAmount;
    private Timestamp firstServiceDate;
    private Timestamp lastServiceDate;
    private Timestamp paidTillDate;
    private Timestamp dueFromDate;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getDeliveredEntries() {
        return deliveredEntries;
    }

    public void setDeliveredEntries(int deliveredEntries) {
        this.deliveredEntries = deliveredEntries;
    }

    public double getTotalServiceAmount() {
        return totalServiceAmount;
    }

    public void setTotalServiceAmount(double totalServiceAmount) {
        this.totalServiceAmount = totalServiceAmount;
    }

    public double getTotalPaidAmount() {
        return totalPaidAmount;
    }

    public void setTotalPaidAmount(double totalPaidAmount) {
        this.totalPaidAmount = totalPaidAmount;
    }

    public double getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(double outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public Timestamp getFirstServiceDate() {
        return firstServiceDate;
    }

    public void setFirstServiceDate(Timestamp firstServiceDate) {
        this.firstServiceDate = firstServiceDate;
    }

    public Timestamp getLastServiceDate() {
        return lastServiceDate;
    }

    public void setLastServiceDate(Timestamp lastServiceDate) {
        this.lastServiceDate = lastServiceDate;
    }

    public Timestamp getPaidTillDate() {
        return paidTillDate;
    }

    public void setPaidTillDate(Timestamp paidTillDate) {
        this.paidTillDate = paidTillDate;
    }

    public Timestamp getDueFromDate() {
        return dueFromDate;
    }

    public void setDueFromDate(Timestamp dueFromDate) {
        this.dueFromDate = dueFromDate;
    }
}
