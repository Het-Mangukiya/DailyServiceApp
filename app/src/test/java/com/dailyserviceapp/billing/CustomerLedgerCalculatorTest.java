package com.dailyserviceapp.billing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class CustomerLedgerCalculatorTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void calculate_withPartialPayment_setsOutstandingAndDueDates() {
        Customer customer = buildCustomer("cust_1", "Het", 40.0);

        ServiceEntry entryOne = buildEntry("cust_1", ts(2026, 1, 1), 2.0, 0.0);
        ServiceEntry entryTwo = buildEntry("cust_1", ts(2026, 1, 5), 1.5, 50.0);

        Payment payment = buildPayment("cust_1", 80.0, ts(2026, 1, 3));

        CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(
            customer,
            Arrays.asList(entryOne, entryTwo),
            Collections.singletonList(payment)
        );

        assertEquals(2, summary.getDeliveredEntries());
        assertEquals(155.0, summary.getTotalServiceAmount(), EPSILON);
        assertEquals(80.0, summary.getTotalPaidAmount(), EPSILON);
        assertEquals(75.0, summary.getOutstandingAmount(), EPSILON);
        assertEquals(ts(2026, 1, 3), summary.getPaidTillDate());
        assertEquals(ts(2026, 1, 5), summary.getDueFromDate());
    }

    @Test
    public void calculate_withOverpayment_clampsOutstandingToZero() {
        Customer customer = buildCustomer("cust_2", "Harsh", 60.0);

        ServiceEntry entry = buildEntry("cust_2", ts(2026, 2, 10), 1.0, 100.0);
        Payment payment = buildPayment("cust_2", 120.0, ts(2026, 2, 11));

        CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(
            customer,
            Collections.singletonList(entry),
            Collections.singletonList(payment)
        );

        assertEquals(100.0, summary.getTotalServiceAmount(), EPSILON);
        assertEquals(120.0, summary.getTotalPaidAmount(), EPSILON);
        assertEquals(0.0, summary.getOutstandingAmount(), EPSILON);
        assertEquals(ts(2026, 2, 10), summary.getPaidTillDate());
        assertNull(summary.getDueFromDate());
    }

    @Test
    public void calculate_withNoPayments_setsDueFromAsFirstServiceDate() {
        Customer customer = buildCustomer("cust_3", "Aarav", 35.0);

        ServiceEntry entryOne = buildEntry("cust_3", ts(2026, 3, 1), 1.0, 0.0);
        ServiceEntry entryTwo = buildEntry("cust_3", ts(2026, 3, 2), 1.0, 0.0);

        CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(
            customer,
            Arrays.asList(entryOne, entryTwo),
            Collections.emptyList()
        );

        assertEquals(2, summary.getDeliveredEntries());
        assertEquals(70.0, summary.getOutstandingAmount(), EPSILON);
        assertNull(summary.getPaidTillDate());
        assertEquals(ts(2026, 3, 1), summary.getDueFromDate());
    }

    @Test
    public void calculateEntryAmount_usesFallbackRateAndDefaultQuantity() {
        Customer customer = buildCustomer("cust_4", "Mira", 45.0);

        ServiceEntry entry = new ServiceEntry();
        entry.setQuantity(0.0); // Should default to 1.0
        entry.setRate(0.0); // Should fallback to customer rate
        entry.setDelivered(true);
        entry.setDate(ts(2026, 1, 1));

        double amount = CustomerLedgerCalculator.calculateEntryAmount(entry, customer);

        assertEquals(45.0, amount, EPSILON);
    }

    private Customer buildCustomer(String id, String name, double ratePerUnit) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName(name);
        customer.setRatePerUnit(ratePerUnit);
        return customer;
    }

    private ServiceEntry buildEntry(String customerId, Timestamp date, double quantity, double rate) {
        ServiceEntry entry = new ServiceEntry();
        entry.setCustomerId(customerId);
        entry.setDate(date);
        entry.setQuantity(quantity);
        entry.setRate(rate);
        entry.setDelivered(true);
        return entry;
    }

    private Payment buildPayment(String customerId, double amount, Timestamp date) {
        Payment payment = new Payment();
        payment.setCustomerId(customerId);
        payment.setAmount(amount);
        payment.setPaymentDate(date);
        return payment;
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
        Timestamp timestamp = new Timestamp(new Date(calendar.getTimeInMillis()));
        assertNotNull(timestamp);
        return timestamp;
    }
}
