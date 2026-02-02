package com.dailyserviceapp.billing;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.data.models.Bill;
import com.google.android.material.chip.Chip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BillAdapter.
 * Tests bill list display and user interactions.
 */
@RunWith(RobolectricTestRunner.class)
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
    }

    @Test
    public void testInitialItemCountIsZero() {
        assertEquals("Initial item count should be 0", 0, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithNullLists() {
        adapter.submitData(null, null);
        assertEquals("Item count should be 0 with null data", 0, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithEmptyLists() {
        adapter.submitData(new ArrayList<>(), new ArrayList<>());
        assertEquals("Item count should be 0 with empty lists", 0, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithBills() {
        List<Bill> bills = createTestBills(3);
        List<String> names = Arrays.asList("Customer A", "Customer B", "Customer C");

        adapter.submitData(bills, names);

        assertEquals("Item count should match bills count", 3, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataReplacesOldData() {
        List<Bill> bills1 = createTestBills(2);
        List<String> names1 = Arrays.asList("Customer A", "Customer B");

        adapter.submitData(bills1, names1);
        assertEquals("Initial count should be 2", 2, adapter.getItemCount());

        List<Bill> bills2 = createTestBills(5);
        List<String> names2 = Arrays.asList("A", "B", "C", "D", "E");

        adapter.submitData(bills2, names2);
        assertEquals("Updated count should be 5", 5, adapter.getItemCount());
    }

    @Test
    public void testSubmitDataWithMismatchedLists() {
        List<Bill> bills = createTestBills(3);
        List<String> names = Arrays.asList("Customer A"); // Only 1 name

        adapter.submitData(bills, names);

        // Should handle gracefully - bill count takes precedence
        assertEquals("Item count should be 3", 3, adapter.getItemCount());
    }

    @Test
    public void testViewHolderInitialization() {
        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        RecyclerView.ViewHolder viewHolder = adapter.onCreateViewHolder(recyclerView, 0);

        assertNotNull("ViewHolder should be created", viewHolder);
        assertTrue("ViewHolder should be correct type",
            viewHolder instanceof BillAdapter.ViewHolder);
    }

    @Test
    public void testViewHolderBindsCorrectData() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setMonth(0); // January
        bills.get(0).setYear(2026);
        bills.get(0).setDaysServed(25);
        bills.get(0).setTotalAmount(500.0);
        bills.get(0).setPaymentStatus("PAID");

        List<String> names = Arrays.asList("Test Customer");

        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        // Verify data is bound
        assertEquals("Customer name should be set", "Test Customer",
            holder.customerName.getText().toString());
    }

    @Test
    public void testPaymentStatusChipForPaid() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setPaymentStatus("PAID");

        List<String> names = Arrays.asList("Customer");
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Status should be 'Paid'", "Paid", holder.paymentStatus.getText().toString());
    }

    @Test
    public void testPaymentStatusChipForPending() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setPaymentStatus("PENDING");

        List<String> names = Arrays.asList("Customer");
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Status should be 'Pending'", "Pending",
            holder.paymentStatus.getText().toString());
    }

    @Test
    public void testPaymentStatusChipForPartial() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setPaymentStatus("PARTIAL");

        List<String> names = Arrays.asList("Customer");
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Status should be 'Partial'", "Partial",
            holder.paymentStatus.getText().toString());
    }

    @Test
    public void testPaymentStatusChipForOverdue() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setPaymentStatus("OVERDUE");

        List<String> names = Arrays.asList("Customer");
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Status should be 'Overdue'", "Overdue",
            holder.paymentStatus.getText().toString());
    }

    @Test
    public void testPaymentStatusChipForNull() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setPaymentStatus(null);

        List<String> names = Arrays.asList("Customer");
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Null status should default to 'Pending'", "Pending",
            holder.paymentStatus.getText().toString());
    }

    @Test
    public void testViewDetailsButtonClick() {
        List<Bill> bills = createTestBills(1);
        List<String> names = Arrays.asList("Customer");
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        holder.viewDetailsButton.performClick();

        verify(mockListener, times(1)).onViewDetails(any(Bill.class));
    }

    @Test
    public void testShareBillButtonClick() {
        List<Bill> bills = createTestBills(1);
        List<String> names = Arrays.asList("Customer");
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        holder.shareBillButton.performClick();

        verify(mockListener, times(1)).onShareBill(any(Bill.class));
    }

    @Test
    public void testCustomerInitialExtraction() {
        List<Bill> bills = createTestBills(1);
        List<String> names = Arrays.asList("Alice");
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Customer initial should be 'A'", "A",
            holder.customerInitial.getText().toString());
    }

    @Test
    public void testCustomerNameUnknownWhenMissing() {
        List<Bill> bills = createTestBills(1);
        List<String> names = new ArrayList<>(); // No names provided
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        assertEquals("Customer name should be 'Unknown'", "Unknown",
            holder.customerName.getText().toString());
    }

    @Test
    public void testBillPeriodFormatting() {
        List<Bill> bills = createTestBills(1);
        bills.get(0).setMonth(5); // June
        bills.get(0).setYear(2026);
        bills.get(0).setDaysServed(30);

        List<String> names = Arrays.asList("Customer");
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        String billPeriod = holder.billPeriod.getText().toString();
        assertTrue("Bill period should contain month", billPeriod.contains("June"));
        assertTrue("Bill period should contain year", billPeriod.contains("2026"));
        assertTrue("Bill period should contain days", billPeriod.contains("30"));
    }

    @Test
    public void testMultipleBillsBinding() {
        List<Bill> bills = createTestBills(3);
        bills.get(0).setPaymentStatus("PAID");
        bills.get(1).setPaymentStatus("PENDING");
        bills.get(2).setPaymentStatus("OVERDUE");

        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
        adapter.submitData(bills, names);

        assertEquals("Should have 3 bills", 3, adapter.getItemCount());

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());

        for (int i = 0; i < 3; i++) {
            BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
                adapter.onCreateViewHolder(recyclerView, 0);
            adapter.onBindViewHolder(holder, i);

            assertNotNull("Holder should be bound for position " + i, holder.customerName);
        }
    }

    @Test
    public void testEmptyCustomerNameHandling() {
        List<Bill> bills = createTestBills(1);
        List<String> names = Arrays.asList(""); // Empty name
        adapter.submitData(bills, names);

        RecyclerView recyclerView = new RecyclerView(RuntimeEnvironment.getApplication());
        BillAdapter.ViewHolder holder = (BillAdapter.ViewHolder)
            adapter.onCreateViewHolder(recyclerView, 0);

        adapter.onBindViewHolder(holder, 0);

        // Should handle empty name gracefully
        assertNotNull("Customer name should be set", holder.customerName.getText());
    }

    @Test
    public void testAdapterHandlesLargeDataset() {
        List<Bill> bills = createTestBills(100);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            names.add("Customer " + i);
        }

        adapter.submitData(bills, names);

        assertEquals("Should handle 100 bills", 100, adapter.getItemCount());
    }

    @Test
    public void testClearDataBySubmittingEmpty() {
        List<Bill> bills = createTestBills(5);
        List<String> names = Arrays.asList("A", "B", "C", "D", "E");
        adapter.submitData(bills, names);

        assertEquals("Should have 5 bills initially", 5, adapter.getItemCount());

        adapter.submitData(new ArrayList<>(), new ArrayList<>());

        assertEquals("Should have 0 bills after clearing", 0, adapter.getItemCount());
    }

    // Helper method to create test bills
    private List<Bill> createTestBills(int count) {
        List<Bill> bills = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Bill bill = new Bill("provider" + i, "customer" + i, 0, 2026);
            bill.setId("bill" + i);
            bill.setTotalAmount(100.0 * (i + 1));
            bill.setDaysServed(20 + i);
            bill.setPaymentStatus("PENDING");
            bills.add(bill);
        }
        return bills;
    }
}