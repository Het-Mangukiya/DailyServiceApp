package com.dailyserviceapp.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class CustomerLedgerCalculatorEdgeCaseTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void calculate_ignoresUndeliveredEntriesAndNullDates() {
        Customer customer = buildCustomer("cust_edge_1", 30.0);

        ServiceEntry delivered = buildEntry("cust_edge_1", ts(2026, 3, 1), 2.0, 10.0, true);
        ServiceEntry undelivered = buildEntry("cust_edge_1", ts(2026, 3, 2), 5.0, 10.0, false);
        ServiceEntry nullDate = buildEntry("cust_edge_1", null, 10.0, 10.0, true);

        CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(
            customer,
            Arrays.asList(delivered, undelivered, nullDate),
            Collections.emptyList()
        );

        assertEquals(1, summary.getDeliveredEntries());
        assertEquals(20.0, summary.getTotalServiceAmount(), EPSILON);
        assertEquals(20.0, summary.getOutstandingAmount(), EPSILON);
        assertEquals(ts(2026, 3, 1), summary.getDueFromDate());
    }

    @Test
    public void calculate_treatsNegativePaymentAsZeroContribution() {
        Customer customer = buildCustomer("cust_edge_2", 50.0);
        ServiceEntry entry = buildEntry("cust_edge_2", ts(2026, 3, 5), 1.0, 100.0, true);

        Payment invalidPayment = new Payment();
        invalidPayment.setCustomerId("cust_edge_2");
        invalidPayment.setAmount(-500.0);
        invalidPayment.setPaymentDate(ts(2026, 3, 6));

        CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(
            customer,
            Collections.singletonList(entry),
            Collections.singletonList(invalidPayment)
        );

        assertEquals(100.0, summary.getTotalServiceAmount(), EPSILON);
        assertEquals(0.0, summary.getTotalPaidAmount(), EPSILON);
        assertEquals(100.0, summary.getOutstandingAmount(), EPSILON);
    }

    @Test
    public void calculateEntryAmount_handlesNullAndFallbacks() {
        Customer customer = buildCustomer("cust_edge_3", 42.0);

        assertEquals(0.0, CustomerLedgerCalculator.calculateEntryAmount(null, customer), EPSILON);

        ServiceEntry entry = new ServiceEntry();
        entry.setQuantity(0.0);
        entry.setRate(-1.0);

        assertEquals(42.0, CustomerLedgerCalculator.calculateEntryAmount(entry, customer), EPSILON);
    }

    @Test
    public void calculate_withNoServiceEntries_keepsOutstandingZero() {
        Customer customer = buildCustomer("cust_edge_4", 60.0);

        CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(
            customer,
            Collections.emptyList(),
            Collections.emptyList()
        );

        assertEquals(0, summary.getDeliveredEntries());
        assertEquals(0.0, summary.getTotalServiceAmount(), EPSILON);
        assertEquals(0.0, summary.getOutstandingAmount(), EPSILON);
        assertNull(summary.getDueFromDate());
    }

    private Customer buildCustomer(String id, double ratePerUnit) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("Edge Customer");
        customer.setRatePerUnit(ratePerUnit);
        return customer;
    }

    private ServiceEntry buildEntry(String customerId, Timestamp date, double qty, double rate, boolean delivered) {
        ServiceEntry entry = new ServiceEntry();
        entry.setCustomerId(customerId);
        entry.setDate(date);
        entry.setQuantity(qty);
        entry.setRate(rate);
        entry.setDelivered(delivered);
        return entry;
    }

    private Timestamp ts(int year, int month, int day) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.YEAR, year);
        calendar.set(java.util.Calendar.MONTH, month - 1);
        calendar.set(java.util.Calendar.DAY_OF_MONTH, day);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return new Timestamp(new Date(calendar.getTimeInMillis()));
    }
}
