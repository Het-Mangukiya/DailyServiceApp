package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for Bill model class.
 * Tests bill creation, getters/setters, and nested classes.
 */
public class BillTest {

    private Bill bill;

    @Before
    public void setUp() {
        bill = new Bill();
    }

    @Test
    public void testDefaultConstructor() {
        assertNotNull("Bill should be created", bill);
        assertNotNull("Items list should be initialized", bill.getItems());
        assertNotNull("Extras list should be initialized", bill.getExtras());
        assertNotNull("Adjustments list should be initialized", bill.getAdjustments());
        assertEquals("Items list should be empty", 0, bill.getItems().size());
        assertEquals("Extras list should be empty", 0, bill.getExtras().size());
        assertEquals("Adjustments list should be empty", 0, bill.getAdjustments().size());
    }

    @Test
    public void testParameterizedConstructor() {
        Bill paramBill = new Bill("provider123", "customer456", 5, 2026);

        assertEquals("Provider ID should match", "provider123", paramBill.getProviderId());
        assertEquals("Customer ID should match", "customer456", paramBill.getCustomerId());
        assertEquals("Month should match", 5, paramBill.getMonth());
        assertEquals("Year should match", 2026, paramBill.getYear());
        assertEquals("Payment status should be PENDING", "PENDING", paramBill.getPaymentStatus());
        assertNotNull("Created at timestamp should be set", paramBill.getCreatedAt());
    }

    @Test
    public void testSettersAndGetters() {
        bill.setId("bill123");
        bill.setProviderId("provider789");
        bill.setCustomerId("customer321");
        bill.setMonth(11);
        bill.setYear(2025);
        bill.setTotalAmount(2500.50);
        bill.setDaysServed(28);
        bill.setPaymentStatus("PAID");
        bill.setPdfUrl("https://example.com/bill.pdf");

        assertEquals("ID should match", "bill123", bill.getId());
        assertEquals("Provider ID should match", "provider789", bill.getProviderId());
        assertEquals("Customer ID should match", "customer321", bill.getCustomerId());
        assertEquals("Month should match", 11, bill.getMonth());
        assertEquals("Year should match", 2025, bill.getYear());
        assertEquals("Total amount should match", 2500.50, bill.getTotalAmount(), 0.001);
        assertEquals("Days served should match", 28, bill.getDaysServed());
        assertEquals("Payment status should match", "PAID", bill.getPaymentStatus());
        assertEquals("PDF URL should match", "https://example.com/bill.pdf", bill.getPdfUrl());
    }

    @Test
    public void testMonthValidation() {
        for (int month = 0; month < 12; month++) {
            bill.setMonth(month);
            assertEquals("Month should be set correctly", month, bill.getMonth());
        }
    }

    @Test
    public void testYearRange() {
        int[] testYears = {2020, 2025, 2026, 2030};
        for (int year : testYears) {
            bill.setYear(year);
            assertEquals("Year should be set correctly", year, bill.getYear());
        }
    }

    @Test
    public void testTotalAmountPrecision() {
        double[] amounts = {0.0, 0.01, 99.99, 1234.56, 999999.99};
        for (double amount : amounts) {
            bill.setTotalAmount(amount);
            assertEquals("Amount should maintain precision", amount, bill.getTotalAmount(), 0.001);
        }
    }

    @Test
    public void testDaysServedRange() {
        int[] days = {0, 1, 15, 28, 30, 31};
        for (int day : days) {
            bill.setDaysServed(day);
            assertEquals("Days served should be set correctly", day, bill.getDaysServed());
        }
    }

    @Test
    public void testPaymentStatusValues() {
        String[] statuses = {"PENDING", "PARTIAL", "PAID", "OVERDUE"};
        for (String status : statuses) {
            bill.setPaymentStatus(status);
            assertEquals("Payment status should match", status, bill.getPaymentStatus());
        }
    }

    @Test
    public void testSetItemsList() {
        List<Bill.BillItem> items = new ArrayList<>();
        items.add(new Bill.BillItem("Milk Delivery", 50.0, 30, 1500.0));
        items.add(new Bill.BillItem("Extra Charge", 10.0, 1, 10.0));

        bill.setItems(items);

        assertEquals("Items list should have 2 items", 2, bill.getItems().size());
        assertEquals("First item description should match", "Milk Delivery",
            bill.getItems().get(0).getDescription());
    }

    @Test
    public void testSetExtrasList() {
        List<Bill.ExtraCharge> extras = new ArrayList<>();
        extras.add(new Bill.ExtraCharge("Delivery Fee", 50.0));
        extras.add(new Bill.ExtraCharge("Late Fee", 25.0));

        bill.setExtras(extras);

        assertEquals("Extras list should have 2 items", 2, bill.getExtras().size());
        assertEquals("First extra description should match", "Delivery Fee",
            bill.getExtras().get(0).getDescription());
    }

    @Test
    public void testSetAdjustmentsList() {
        List<Bill.Adjustment> adjustments = new ArrayList<>();
        adjustments.add(new Bill.Adjustment("Discount", -100.0));
        adjustments.add(new Bill.Adjustment("Refund", -50.0));

        bill.setAdjustments(adjustments);

        assertEquals("Adjustments list should have 2 items", 2, bill.getAdjustments().size());
        assertEquals("First adjustment amount should be negative", -100.0,
            bill.getAdjustments().get(0).getAmount(), 0.001);
    }

    @Test
    public void testBillItemConstructor() {
        Bill.BillItem item = new Bill.BillItem("Newspaper", 5.0, 30, 150.0);

        assertEquals("Description should match", "Newspaper", item.getDescription());
        assertEquals("Rate should match", 5.0, item.getRate(), 0.001);
        assertEquals("Quantity should match", 30, item.getQuantity());
        assertEquals("Amount should match", 150.0, item.getAmount(), 0.001);
    }

    @Test
    public void testBillItemSettersAndGetters() {
        Bill.BillItem item = new Bill.BillItem();
        item.setDescription("Test Item");
        item.setRate(25.5);
        item.setQuantity(10);
        item.setAmount(255.0);

        assertEquals("Description should match", "Test Item", item.getDescription());
        assertEquals("Rate should match", 25.5, item.getRate(), 0.001);
        assertEquals("Quantity should match", 10, item.getQuantity());
        assertEquals("Amount should match", 255.0, item.getAmount(), 0.001);
    }

    @Test
    public void testExtraChargeConstructor() {
        Bill.ExtraCharge extra = new Bill.ExtraCharge("Service Charge", 100.0);

        assertEquals("Description should match", "Service Charge", extra.getDescription());
        assertEquals("Amount should match", 100.0, extra.getAmount(), 0.001);
    }

    @Test
    public void testExtraChargeSettersAndGetters() {
        Bill.ExtraCharge extra = new Bill.ExtraCharge();
        extra.setDescription("Test Charge");
        extra.setAmount(75.5);

        assertEquals("Description should match", "Test Charge", extra.getDescription());
        assertEquals("Amount should match", 75.5, extra.getAmount(), 0.001);
    }

    @Test
    public void testAdjustmentConstructor() {
        Bill.Adjustment adjustment = new Bill.Adjustment("Early Payment Discount", -50.0);

        assertEquals("Description should match", "Early Payment Discount",
            adjustment.getDescription());
        assertEquals("Amount should be negative", -50.0, adjustment.getAmount(), 0.001);
    }

    @Test
    public void testAdjustmentSettersAndGetters() {
        Bill.Adjustment adjustment = new Bill.Adjustment();
        adjustment.setDescription("Test Adjustment");
        adjustment.setAmount(-25.0);

        assertEquals("Description should match", "Test Adjustment", adjustment.getDescription());
        assertEquals("Amount should match", -25.0, adjustment.getAmount(), 0.001);
    }

    @Test
    public void testPositiveAdjustment() {
        Bill.Adjustment adjustment = new Bill.Adjustment("Additional Charge", 100.0);

        assertTrue("Positive adjustment should be allowed", adjustment.getAmount() > 0);
    }

    @Test
    public void testSetCreatedAtTimestamp() {
        Timestamp now = Timestamp.now();
        bill.setCreatedAt(now);

        assertEquals("Created at timestamp should match", now, bill.getCreatedAt());
    }

    @Test
    public void testSetDueDateTimestamp() {
        Timestamp dueDate = Timestamp.now();
        bill.setDueDate(dueDate);

        assertEquals("Due date timestamp should match", dueDate, bill.getDueDate());
    }

    @Test
    public void testNullPdfUrl() {
        bill.setPdfUrl(null);
        assertNull("PDF URL can be null", bill.getPdfUrl());
    }

    @Test
    public void testEmptyCollections() {
        bill.setItems(new ArrayList<>());
        bill.setExtras(new ArrayList<>());
        bill.setAdjustments(new ArrayList<>());

        assertEquals("Items should be empty", 0, bill.getItems().size());
        assertEquals("Extras should be empty", 0, bill.getExtras().size());
        assertEquals("Adjustments should be empty", 0, bill.getAdjustments().size());
    }

    @Test
    public void testBillWithMultipleItems() {
        Bill completeBill = new Bill("provider1", "customer1", 6, 2026);

        List<Bill.BillItem> items = new ArrayList<>();
        items.add(new Bill.BillItem("Milk", 50.0, 30, 1500.0));
        items.add(new Bill.BillItem("Curd", 20.0, 15, 300.0));
        completeBill.setItems(items);

        List<Bill.ExtraCharge> extras = new ArrayList<>();
        extras.add(new Bill.ExtraCharge("Delivery", 50.0));
        completeBill.setExtras(extras);

        List<Bill.Adjustment> adjustments = new ArrayList<>();
        adjustments.add(new Bill.Adjustment("Discount", -100.0));
        completeBill.setAdjustments(adjustments);

        completeBill.setTotalAmount(1750.0);
        completeBill.setDaysServed(30);
        completeBill.setPaymentStatus("PENDING");

        assertEquals("Should have 2 items", 2, completeBill.getItems().size());
        assertEquals("Should have 1 extra", 1, completeBill.getExtras().size());
        assertEquals("Should have 1 adjustment", 1, completeBill.getAdjustments().size());
        assertEquals("Total amount should be calculated", 1750.0,
            completeBill.getTotalAmount(), 0.001);
    }

    @Test
    public void testZeroQuantityBillItem() {
        Bill.BillItem item = new Bill.BillItem("Test", 10.0, 0, 0.0);
        assertEquals("Zero quantity should be allowed", 0, item.getQuantity());
    }

    @Test
    public void testNegativeAmountAdjustment() {
        Bill.Adjustment discount = new Bill.Adjustment("Discount", -200.0);
        assertTrue("Negative adjustments should be allowed for discounts",
            discount.getAmount() < 0);
    }
}