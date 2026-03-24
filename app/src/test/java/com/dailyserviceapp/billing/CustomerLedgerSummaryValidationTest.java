package com.dailyserviceapp.billing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CustomerLedgerSummaryValidationTest {

    @Test(expected = IllegalArgumentException.class)
    public void setDeliveredEntries_negative_throws() {
        CustomerLedgerSummary summary = new CustomerLedgerSummary();
        summary.setDeliveredEntries(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTotalServiceAmount_negative_throws() {
        CustomerLedgerSummary summary = new CustomerLedgerSummary();
        summary.setTotalServiceAmount(-0.01);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setTotalPaidAmount_negative_throws() {
        CustomerLedgerSummary summary = new CustomerLedgerSummary();
        summary.setTotalPaidAmount(-10.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setOutstandingAmount_negative_throws() {
        CustomerLedgerSummary summary = new CustomerLedgerSummary();
        summary.setOutstandingAmount(-5.0);
    }

    @Test
    public void setters_validValues_areStored() {
        CustomerLedgerSummary summary = new CustomerLedgerSummary();

        summary.setDeliveredEntries(3);
        summary.setTotalServiceAmount(250.0);
        summary.setTotalPaidAmount(100.0);
        summary.setOutstandingAmount(150.0);

        assertEquals(3, summary.getDeliveredEntries());
        assertEquals(250.0, summary.getTotalServiceAmount(), 0.0001);
        assertEquals(100.0, summary.getTotalPaidAmount(), 0.0001);
        assertEquals(150.0, summary.getOutstandingAmount(), 0.0001);
    }
}
