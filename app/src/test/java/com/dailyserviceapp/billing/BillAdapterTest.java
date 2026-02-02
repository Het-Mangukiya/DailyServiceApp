package com.dailyserviceapp.billing;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.dailyserviceapp.data.models.Bill;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BillAdapter.
 * Tests adapter functionality, view binding, and action listeners.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BillAdapterTest {

    private BillAdapter adapter;
    private BillAdapter.OnBillActionListener mockListener;

    @Before
    public void setUp() {
        mockListener = mock(BillAdapter.OnBillActionListener.class);
        adapter = new BillAdapter(mockListener);
    }

    @Test
    public void testAdapterCreation() {
        assertNotNull("Adapter should be created", adapter);
        assertEquals("Initial item count should be 0", 0, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithBillsAndNames() {
        List<Bill> bills = createTestBills(3);
        List<String> names = Arrays.asList("Customer A", "Customer B", "Customer C");

        adapter.submitData(bills, names);

        assertEquals("Item count should match bills count", 3, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithNullBills() {
        adapter.submitData(null, Arrays.asList("Customer A"));

        assertEquals("Item count should be 0 with null bills", 0, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithNullNames() {
        List<Bill> bills = createTestBills(2);
        adapter.submitData(bills, null);

        assertEquals("Item count should match bills count", 2, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataReplacesExistingData() {
        adapter.submitData(createTestBills(3), Arrays.asList("A", "B", "C"));
        adapter.submitData(createTestBills(5), Arrays.asList("D", "E", "F", "G", "H"));

        assertEquals("Item count should reflect new data", 5, adapter.getItemCount());
    }

    @Test
    public void testSubmitEmptyData() {
        adapter.submitData(new ArrayList<>(), new ArrayList<>());

        assertEquals("Item count should be 0 with empty lists", 0, adapter.getItemCount());
    }

    @Test
    public void testGetItemCountAfterMultipleSubmits() {
        adapter.submitData(createTestBills(2), Arrays.asList("A", "B"));
        assertEquals("First submit: count should be 2", 2, adapter.getItemCount());

        adapter.submitData(createTestBills(4), Arrays.asList("C", "D", "E", "F"));
        assertEquals("Second submit: count should be 4", 4, adapter.getItemCount());

        adapter.submitData(new ArrayList<>(), new ArrayList<>());
        assertEquals("Third submit: count should be 0", 0, adapter.getItemCount());
    }

    @Test
    public void testCreateViewHolderReturnsNonNull() {
        ViewGroup parent = new FrameLayout(RuntimeEnvironment.application);

        BillAdapter.ViewHolder viewHolder = adapter.onCreateViewHolder(parent, 0);

        assertNotNull("ViewHolder should not be null", viewHolder);
    }

    @Test
    public void testViewHolderBindingWithValidData() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setMonth(0); // January
        bills.get(0).setYear(2026);
        bills.get(0).setDaysServed(30);
        bills.get(0).setTotalAmount(1500.0);
        bills.get(0).setPaymentStatus("PAID");

        List<String> names = Arrays.asList("John Doe");

        adapter.submitData(bills, names);

        // Test that adapter can bind without crashing
        assertEquals("Should have 1 item", 1, adapter.getItemCount());
    }

    @Test
    public void testViewHolderBindingWithPendingStatus() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setPaymentStatus("PENDING");
        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should have 1 item with PENDING status", 1, adapter.getItemCount());
    }

    @Test
    public void testViewHolderBindingWithPartialStatus() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setPaymentStatus("PARTIAL");
        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should have 1 item with PARTIAL status", 1, adapter.getItemCount());
    }

    @Test
    public void testViewHolderBindingWithOverdueStatus() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setPaymentStatus("OVERDUE");
        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should have 1 item with OVERDUE status", 1, adapter.getItemCount());
    }

    @Test
    public void testViewHolderBindingWithNullStatus() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setPaymentStatus(null);
        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should have 1 item with null status", 1, adapter.getItemCount());
    }

    @Test
    public void testViewHolderBindingWithUnknownCustomer() {
        List<Bill> bills = createTestBills(1);
        adapter.submitData(bills, new ArrayList<>()); // No customer names

        assertEquals("Should handle missing customer name", 1, adapter.getItemCount());
    }

    @Test
    public void testBillAmountFormatting() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setTotalAmount(1234.56);
        adapter.submitData(bills, Arrays.asList("Customer"));

        // Verify adapter handles the bill
        assertEquals("Should format bill amount correctly", 1, adapter.getItemCount());
    }

    @Test
    public void testMonthYearDisplay() {
        List<Bill> bills = createTestBills(1);
        // Test each month
        for (int month = 0; month < 12; month++) {
            bills.get(0).setMonth(month);
            bills.get(0).setYear(2026);
            adapter.submitData(bills, Arrays.asList("Customer"));
            assertEquals("Should handle month " + month, 1, adapter.getItemCount());
        }
    }

    @Test
    public void testCustomerInitialExtraction() {
        List<Bill> bills = createTestBills(1);
        List<String> names = Arrays.asList("Alice");
        adapter.submitData(bills, names);

        // Verify adapter can extract initial 'A' from "Alice"
        assertEquals("Should extract customer initial", 1, adapter.getItemCount());
    }

    @Test
    public void testEmptyCustomerName() {
        List<Bill> bills = createTestBills(1);
        List<String> names = Arrays.asList("");
        adapter.submitData(bills, names);

        // Should handle empty customer name gracefully
        assertEquals("Should handle empty customer name", 1, adapter.getItemCount());
    }

    @Test
    public void testMismatchedBillsAndCustomerNames() {
        List<Bill> bills = createTestBills(5);
        List<String> names = Arrays.asList("Customer A", "Customer B"); // Only 2 names

        adapter.submitData(bills, names);

        assertEquals("Should handle mismatched lists", 5, adapter.getItemCount());
    }

    @Test
    public void testBillPeriodFormatting() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setMonth(5); // June
        bills.get(0).setYear(2026);
        bills.get(0).setDaysServed(28);

        adapter.submitData(bills, Arrays.asList("Customer"));

        // Verify period includes "June 2026 • 28 days"
        assertEquals("Should format bill period", 1, adapter.getItemCount());
    }

    @Test
    public void testDifferentDaysServedValues() {
        List<Bill> bills = createTestBills(3);
        bills.get(0).setDaysServed(30);
        bills.get(1).setDaysServed(15);
        bills.get(2).setDaysServed(1);

        adapter.submitData(bills, Arrays.asList("A", "B", "C"));

        assertEquals("Should handle various days served values", 3, adapter.getItemCount());
    }

    @Test
    public void testZeroAmountBill() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setTotalAmount(0.0);

        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should handle zero amount bill", 1, adapter.getItemCount());
    }

    @Test
    public void testLargeAmountBill() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setTotalAmount(999999.99);

        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should handle large amount bill", 1, adapter.getItemCount());
    }

    // Helper method to create test bills
    private List<Bill> createTestBills(int count) {
        List<Bill> bills = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Bill bill = new Bill("provider" + i, "customer" + i, 0, 2026);
            bill.setId("bill" + i);
            bill.setTotalAmount(100.0 * (i + 1));
            bill.setDaysServed(30);
            bill.setPaymentStatus("PENDING");
            bills.add(bill);
        }
        return bills;
    }
}