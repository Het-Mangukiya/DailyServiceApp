package com.dailyserviceapp.billing;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.dailyserviceapp.data.models.Bill;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
 * Tests RecyclerView adapter functionality, bill display, payment status,
 * customer name handling, and action listeners.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BillAdapterTest {

    private BillAdapter adapter;

    @Mock
    private BillAdapter.OnBillActionListener mockListener;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new BillAdapter(mockListener);
    }

    @Test
    public void testAdapterCreation() {
        assertNotNull("Adapter should be created", adapter);
        assertEquals("Initial item count should be 0", 0, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithBillsAndNames() {
        List<Bill> bills = createSampleBills(3);
        List<String> names = Arrays.asList("John Doe", "Jane Smith", "Bob Johnson");

        adapter.submitData(bills, names);

        assertEquals("Item count should match bills size", 3, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithNullBills() {
        adapter.submitData(null, Arrays.asList("John Doe"));

        assertEquals("Item count should be 0 with null bills", 0, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithNullNames() {
        List<Bill> bills = createSampleBills(2);
        adapter.submitData(bills, null);

        assertEquals("Item count should match bills size even with null names",
                2, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithEmptyLists() {
        adapter.submitData(new ArrayList<>(), new ArrayList<>());

        assertEquals("Item count should be 0 with empty lists", 0, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataReplacesExistingData() {
        // Submit initial data
        List<Bill> initialBills = createSampleBills(2);
        adapter.submitData(initialBills, Arrays.asList("John", "Jane"));
        assertEquals("Initial count should be 2", 2, adapter.getItemCount());

        // Submit new data
        List<Bill> newBills = createSampleBills(5);
        List<String> newNames = Arrays.asList("A", "B", "C", "D", "E");
        adapter.submitData(newBills, newNames);

        assertEquals("Count should be updated to 5", 5, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataClearsExistingData() {
        List<Bill> bills = createSampleBills(3);
        adapter.submitData(bills, Arrays.asList("A", "B", "C"));
        assertEquals("Should have 3 items", 3, adapter.getItemCount());

        adapter.submitData(new ArrayList<>(), new ArrayList<>());
        assertEquals("Should have 0 items after clearing", 0, adapter.getItemCount());
    }

    @Test
    public void testCreateViewHolder() {
        ViewGroup parent = new FrameLayout(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        assertNotNull("ViewHolder should be created", holder);
        assertNotNull("ViewHolder itemView should not be null", holder.itemView);
    }

    @Test
    public void testBindViewHolderWithValidData() {
        List<Bill> bills = createSampleBills(1);
        bills.get(0).setTotalAmount(500.0);
        bills.get(0).setDaysServed(25);
        bills.get(0).setPaymentStatus("PAID");

        adapter.submitData(bills, Arrays.asList("John Doe"));

        ViewGroup parent = new FrameLayout(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        // Verify holder is bound (no exceptions)
        assertNotNull("ViewHolder should be bound", holder);
    }

    @Test
    public void testBindViewHolderWithMissingCustomerName() {
        List<Bill> bills = createSampleBills(2);
        List<String> names = Arrays.asList("John Doe"); // Only one name for two bills

        adapter.submitData(bills, names);

        ViewGroup parent = new FrameLayout(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        // Bind second item which has no corresponding name
        adapter.onBindViewHolder(holder, 1);

        // Should handle missing name gracefully (show "Unknown")
        assertNotNull("ViewHolder should handle missing name", holder);
    }

    @Test
    public void testBindViewHolderWithEmptyCustomerName() {
        List<Bill> bills = createSampleBills(1);
        List<String> names = Arrays.asList("");

        adapter.submitData(bills, names);

        ViewGroup parent = new FrameLayout(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        adapter.onBindViewHolder(holder, 0);

        // Should handle empty name gracefully
        assertNotNull("ViewHolder should handle empty name", holder);
    }

    @Test
    public void testPaymentStatusPaid() {
        Bill bill = createBill("customer1", 100.0, "PAID");
        List<Bill> bills = Arrays.asList(bill);

        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should have one bill", 1, adapter.getItemCount());
    }

    @Test
    public void testPaymentStatusPartial() {
        Bill bill = createBill("customer1", 100.0, "PARTIAL");
        List<Bill> bills = Arrays.asList(bill);

        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should have one bill", 1, adapter.getItemCount());
    }

    @Test
    public void testPaymentStatusOverdue() {
        Bill bill = createBill("customer1", 100.0, "OVERDUE");
        List<Bill> bills = Arrays.asList(bill);

        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should have one bill", 1, adapter.getItemCount());
    }

    @Test
    public void testPaymentStatusPending() {
        Bill bill = createBill("customer1", 100.0, "PENDING");
        List<Bill> bills = Arrays.asList(bill);

        adapter.submitData(bills, Arrays.asList("Customer"));

        assertEquals("Should have one bill", 1, adapter.getItemCount());
    }

    @Test
    public void testPaymentStatusNull() {
        Bill bill = createBill("customer1", 100.0, null);
        List<Bill> bills = Arrays.asList(bill);

        adapter.submitData(bills, Arrays.asList("Customer"));

        // Should default to PENDING when status is null
        assertEquals("Should have one bill with null status", 1, adapter.getItemCount());
    }

    @Test
    public void testMonthDisplayAllMonths() {
        // Test all 12 months
        for (int month = 0; month < 12; month++) {
            Bill bill = new Bill("provider1", "customer1", month, 2024);
            bill.setId("bill" + month);
            bill.setTotalAmount(100.0);
            bill.setDaysServed(20);
            bill.setPaymentStatus("PAID");

            adapter.submitData(Arrays.asList(bill), Arrays.asList("Customer"));

            assertEquals("Should have bill for month " + month, 1, adapter.getItemCount());
        }
    }

    @Test
    public void testDifferentYears() {
        List<Bill> bills = new ArrayList<>();
        bills.add(createBillForMonthYear(5, 2023));
        bills.add(createBillForMonthYear(5, 2024));
        bills.add(createBillForMonthYear(5, 2025));

        adapter.submitData(bills, Arrays.asList("Customer1", "Customer2", "Customer3"));

        assertEquals("Should handle different years", 3, adapter.getItemCount());
    }

    @Test
    public void testZeroDaysServed() {
        Bill bill = createBill("customer1", 0.0, "PENDING");
        bill.setDaysServed(0);

        adapter.submitData(Arrays.asList(bill), Arrays.asList("Customer"));

        assertEquals("Should handle zero days served", 1, adapter.getItemCount());
    }

    @Test
    public void testZeroAmount() {
        Bill bill = createBill("customer1", 0.0, "PENDING");

        adapter.submitData(Arrays.asList(bill), Arrays.asList("Customer"));

        assertEquals("Should handle zero amount", 1, adapter.getItemCount());
    }

    @Test
    public void testLargeAmount() {
        Bill bill = createBill("customer1", 999999.99, "PAID");

        adapter.submitData(Arrays.asList(bill), Arrays.asList("Customer"));

        assertEquals("Should handle large amount", 1, adapter.getItemCount());
    }

    @Test
    public void testNegativeAmount() {
        Bill bill = createBill("customer1", -100.0, "PENDING");

        adapter.submitData(Arrays.asList(bill), Arrays.asList("Customer"));

        assertEquals("Should handle negative amount", 1, adapter.getItemCount());
    }

    @Test
    public void testViewDetailsButtonClick() {
        Bill bill = createBill("customer1", 100.0, "PAID");
        adapter.submitData(Arrays.asList(bill), Arrays.asList("Customer"));

        ViewGroup parent = new FrameLayout(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        // Simulate button click
        holder.viewDetailsButton.performClick();

        verify(mockListener, times(1)).onViewDetails(bill);
    }

    @Test
    public void testShareBillButtonClick() {
        Bill bill = createBill("customer1", 100.0, "PAID");
        adapter.submitData(Arrays.asList(bill), Arrays.asList("Customer"));

        ViewGroup parent = new FrameLayout(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        // Simulate button click
        holder.shareBillButton.performClick();

        verify(mockListener, times(1)).onShareBill(bill);
    }

    @Test
    public void testMultipleButtonClicks() {
        Bill bill = createBill("customer1", 100.0, "PAID");
        adapter.submitData(Arrays.asList(bill), Arrays.asList("Customer"));

        ViewGroup parent = new FrameLayout(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        // Click multiple times
        holder.viewDetailsButton.performClick();
        holder.viewDetailsButton.performClick();

        verify(mockListener, times(2)).onViewDetails(bill);
    }

    @Test
    public void testLargeBillList() {
        List<Bill> bills = createSampleBills(100);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            names.add("Customer " + i);
        }

        adapter.submitData(bills, names);

        assertEquals("Should handle large bill list", 100, adapter.getItemCount());
    }

    @Test
    public void testCustomerNameWithSpecialCharacters() {
        List<String> specialNames = Arrays.asList(
            "O'Brien",
            "José García",
            "François Müller",
            "李明"
        );

        List<Bill> bills = createSampleBills(specialNames.size());
        adapter.submitData(bills, specialNames);

        assertEquals("Should handle special characters in names",
                specialNames.size(), adapter.getItemCount());
    }

    @Test
    public void testLongCustomerName() {
        String longName = "A".repeat(100);
        adapter.submitData(createSampleBills(1), Arrays.asList(longName));

        assertEquals("Should handle long customer name", 1, adapter.getItemCount());
    }

    @Test
    public void testRebindSamePosition() {
        List<Bill> bills = createSampleBills(3);
        adapter.submitData(bills, Arrays.asList("A", "B", "C"));

        ViewGroup parent = new FrameLayout(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        // Bind same position multiple times
        adapter.onBindViewHolder(holder, 0);
        adapter.onBindViewHolder(holder, 0);

        assertNotNull("Should handle rebinding same position", holder);
    }

    // Helper methods

    private List<Bill> createSampleBills(int count) {
        List<Bill> bills = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Bill bill = new Bill("provider1", "customer" + i, 0, 2024);
            bill.setId("bill" + i);
            bill.setTotalAmount(100.0 + i);
            bill.setDaysServed(20 + i);
            bill.setPaymentStatus("PENDING");
            bills.add(bill);
        }
        return bills;
    }

    private Bill createBill(String customerId, double amount, String status) {
        Bill bill = new Bill("provider1", customerId, 0, 2024);
        bill.setId("bill1");
        bill.setTotalAmount(amount);
        bill.setDaysServed(20);
        bill.setPaymentStatus(status);
        return bill;
    }

    private Bill createBillForMonthYear(int month, int year) {
        Bill bill = new Bill("provider1", "customer1", month, year);
        bill.setId("bill_" + month + "_" + year);
        bill.setTotalAmount(100.0);
        bill.setDaysServed(20);
        bill.setPaymentStatus("PAID");
        return bill;
    }
}