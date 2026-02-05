package com.dailyserviceapp.billing;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.data.FirestoreRepository;
import com.dailyserviceapp.data.models.Bill;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bill List Activity for viewing and managing monthly bills.
 * Displays bills for the selected month and allows bill generation from service entries.
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Month selector for viewing different billing periods</li>
 *   <li>Bill list with customer names, amounts, and payment status</li>
 *   <li>Generate bills button - creates bills from service entries</li>
 *   <li>View bill details</li>
 *   <li>Share bills (PDF)</li>
 * </ul>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-09
 */
public class BillListActivity extends BaseActivity {
    
    private TextView selectedMonthText;
    private MaterialButton previousMonthButton;
    private MaterialButton nextMonthButton;
    private RecyclerView billsRecyclerView;
    private LinearLayout emptyStateLayout;
    private ExtendedFloatingActionButton generateBillsFab;
    private android.widget.ProgressBar loadingProgress;
    
    private FirestoreRepository repository;
    private BillAdapter adapter;
    private String providerId;
    private int selectedMonth;
    private int selectedYear;
    
    private List<Bill> currentBills = new ArrayList<>();
    private Map<String, Customer> customerMap = new HashMap<>();
    private boolean customersLoaded = false; // Track if customers have been loaded
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_list_new);
        
        // CRITICAL: Check session first
        if (!isLoggedIn()) {
            showToast("Please login first");
            navigateToLogin();
            return;
        }
        
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, "Bills", true);
        
        // Restore state if available
        if (savedInstanceState != null) {
            customersLoaded = savedInstanceState.getBoolean("customersLoaded", false);
            selectedMonth = savedInstanceState.getInt("selectedMonth", Calendar.getInstance().get(Calendar.MONTH));
            selectedYear = savedInstanceState.getInt("selectedYear", Calendar.getInstance().get(Calendar.YEAR));
        }
        
        initializeViews();
        initializeData();
        setupClickListeners();
        loadData();
    }
    
    private void initializeViews() {
        selectedMonthText = findViewById(R.id.selectedMonthText);
        previousMonthButton = findViewById(R.id.previousMonthButton);
        nextMonthButton = findViewById(R.id.nextMonthButton);
        billsRecyclerView = findViewById(R.id.billsRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyState);
        generateBillsFab = findViewById(R.id.generateBillsFab);
        loadingProgress = findViewById(R.id.loadingProgress);
        
        billsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void initializeData() {
        repository = new FirestoreRepository();
        providerId = getCurrentUserId();
        
        // Set current month
        Calendar calendar = Calendar.getInstance();
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedYear = calendar.get(Calendar.YEAR);
        
        adapter = new BillAdapter(new BillAdapter.OnBillActionListener() {
            @Override
            public void onViewDetails(Bill bill) {
                openBillDetails(bill);
            }

            @Override
            public void onShareBill(Bill bill) {
                shareBill(bill);
            }
        });
        billsRecyclerView.setAdapter(adapter);
        
        updateMonthDisplay();
    }
    
    private void setupClickListeners() {
        previousMonthButton.setOnClickListener(v -> {
            selectedMonth--;
            if (selectedMonth < 0) {
                selectedMonth = 11;
                selectedYear--;
            }
            updateMonthDisplay();
            loadData();
        });
        
        nextMonthButton.setOnClickListener(v -> {
            selectedMonth++;
            if (selectedMonth > 11) {
                selectedMonth = 0;
                selectedYear++;
            }
            updateMonthDisplay();
            loadData();
        });
        
        generateBillsFab.setOnClickListener(v -> generateBills());
    }
    
    private void updateMonthDisplay() {
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                               "July", "August", "September", "October", "November", "December"};
        selectedMonthText.setText(monthNames[selectedMonth] + " " + selectedYear);
    }
    
    private void loadData() {
        if (providerId == null || providerId.isEmpty()) {
            showToast("Please login again");
            showEmptyState(true);
            return;
        }
        
        if (!isNetworkAvailable()) {
            showToast("No internet connection");
            return;
        }
        
        // Show loading instead of empty state
        showLoading(true);
        
        // Load customers only if not already loaded (prevents redundant Firestore reads)
        if (!customersLoaded) {
            repository.getCustomersByProvider(providerId, new FirestoreRepository.OnCustomersLoadedListener() {
                @Override
                public void onCustomersLoaded(List<Customer> customers) {
                    customerMap.clear();
                    for (Customer customer : customers) {
                        customerMap.put(customer.getId(), customer);
                    }
                    customersLoaded = true; // Mark as loaded
                    
                    // Then load bills
                    loadBills();
                }

                @Override
                public void onError(String error) {
                    showToast("Error loading customers: " + error);
                    showEmptyState(true);
                }
            });
        } else {
            // Customers already loaded, just load bills
            loadBills();
        }
    }
    
    private void loadBills() {
        repository.getBillsByProviderAndMonth(providerId, selectedMonth, selectedYear,
                new FirestoreRepository.OnBillsLoadedListener() {
                    @Override
                    public void onBillsLoaded(List<Bill> bills) {
                        showLoading(false);
                        currentBills = bills;
                        if (bills == null || bills.isEmpty()) {
                            showEmptyState(true);
                            return;
                        }
                        
                        showEmptyState(false);
                        
                        // Extract customer names
                        List<String> customerNames = new ArrayList<>();
                        for (Bill bill : bills) {
                            Customer customer = customerMap.get(bill.getCustomerId());
                            customerNames.add(customer != null ? customer.getName() : "Unknown");
                        }
                        
                        adapter.submitData(bills, customerNames);
                    }

                    @Override
                    public void onError(String error) {
                        showLoading(false);
                        showToast("Error loading bills: " + error);
                        showEmptyState(true);
                    }
                });
    }
    
    private void generateBills() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Generating bills...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Get all customers first
        repository.getCustomersByProvider(providerId, new FirestoreRepository.OnCustomersLoadedListener() {
            @Override
            public void onCustomersLoaded(List<Customer> customers) {
                if (customers == null || customers.isEmpty()) {
                    progressDialog.dismiss();
                    showToast("No customers found");
                    return;
                }
                
                // Get service entries for the selected month
                Calendar startCal = Calendar.getInstance();
                startCal.set(selectedYear, selectedMonth, 1, 0, 0, 0);
                startCal.set(Calendar.MILLISECOND, 0);
                
                Calendar endCal = Calendar.getInstance();
                endCal.set(selectedYear, selectedMonth, startCal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
                endCal.set(Calendar.MILLISECOND, 999);
                
                repository.getServiceEntriesByProviderAndDate(
                        providerId,
                        new Timestamp(startCal.getTime()),
                        new Timestamp(endCal.getTime()),
                        new FirestoreRepository.OnServiceEntriesLoadedListener() {
                            @Override
                            public void onServiceEntriesLoaded(List<ServiceEntry> entries) {
                                // Load existing bills to prevent duplicates
                                repository.getBillsByProviderAndMonth(providerId, selectedMonth, selectedYear,
                                        new FirestoreRepository.OnBillsLoadedListener() {
                                            @Override
                                            public void onBillsLoaded(List<Bill> bills) {
                                                generateBillsFromEntries(customers, entries, bills, progressDialog);
                                            }

                                            @Override
                                            public void onError(String error) {
                                                // If we can't load existing bills, proceed but warn
                                                showToast("Warning: Could not check existing bills. " + error);
                                                generateBillsFromEntries(customers, entries, new ArrayList<>(), progressDialog);
                                            }
                                        });
                            }

                            @Override
                            public void onError(String error) {
                                progressDialog.dismiss();
                                showToast("Error loading service entries: " + error);
                            }
                        }
                );
            }

            @Override
            public void onError(String error) {
                progressDialog.dismiss();
                showToast("Error loading customers: " + error);
            }
        });
    }
    
    private void generateBillsFromEntries(List<Customer> customers, List<ServiceEntry> entries,
                                          List<Bill> existingBills, ProgressDialog progressDialog) {
        // Group entries by customer
        Map<String, List<ServiceEntry>> entriesByCustomer = new HashMap<>();
        for (ServiceEntry entry : entries) {
            if (!entriesByCustomer.containsKey(entry.getCustomerId())) {
                entriesByCustomer.put(entry.getCustomerId(), new ArrayList<>());
            }
            entriesByCustomer.get(entry.getCustomerId()).add(entry);
        }
        
        java.util.Set<String> billedCustomerIds = new java.util.HashSet<>();
        if (existingBills != null) {
            for (Bill bill : existingBills) {
                if (bill.getCustomerId() != null) {
                    billedCustomerIds.add(bill.getCustomerId());
                }
            }
        }
        
        int billsGenerated = 0;
        int customersProcessed = 0;
        int billsSkipped = 0;
        
        for (Customer customer : customers) {
            if (billedCustomerIds.contains(customer.getId())) {
                billsSkipped++;
                customersProcessed++;
                continue;
            }
            
            List<ServiceEntry> customerEntries = entriesByCustomer.get(customer.getId());
            
            if (customerEntries != null && !customerEntries.isEmpty()) {
                // Calculate bill
                double totalAmount = 0;
                int daysServed = 0;
                
                for (ServiceEntry entry : customerEntries) {
                    if (entry.isDelivered()) {
                        totalAmount += entry.getQuantity() * customer.getRatePerUnit();
                        daysServed++;
                    }
                }
                
                if (totalAmount > 0) {
                    // Create bill
                    Bill bill = new Bill(providerId, customer.getId(), selectedMonth, selectedYear);
                    bill.setTotalAmount(totalAmount);
                    bill.setDaysServed(daysServed);
                    bill.setPaymentStatus("PENDING");
                    
                    // Add bill item
                    Bill.BillItem item = new Bill.BillItem(
                            customer.getServiceType(),
                            customer.getRatePerUnit(),
                            daysServed,
                            totalAmount
                    );
                    List<Bill.BillItem> items = new ArrayList<>();
                    items.add(item);
                    bill.setItems(items);
                    
                    // Set due date (15 days from now)
                    Calendar dueCal = Calendar.getInstance();
                    dueCal.add(Calendar.DAY_OF_MONTH, 15);
                    bill.setDueDate(new Timestamp(dueCal.getTime()));
                    
                    // Save bill
                    final int currentCount = billsGenerated;
                    repository.saveBill(bill, new FirestoreRepository.OnSaveCompleteListener() {
                        @Override
                        public void onSuccess() {
                            // Bill saved
                        }

                        @Override
                        public void onError(String error) {
                            showToast("Error saving bill for " + customer.getName());
                        }
                    });
                    
                    billsGenerated++;
                }
            }
            
            customersProcessed++;
        }
        
        progressDialog.dismiss();
        
        if (billsGenerated > 0) {
            String message = "Generated " + billsGenerated + " bills";
            if (billsSkipped > 0) {
                message += " (" + billsSkipped + " already billed)";
            }
            showToast(message);
            loadData(); // Reload the list
        } else {
            if (billsSkipped > 0) {
                showToast("No new bills generated (" + billsSkipped + " already billed)");
            } else {
                showToast("No bills to generate (no service entries found)");
            }
        }
    }
    
    private void openBillDetails(Bill bill) {
        Intent intent = new Intent(this, BillDetailActivity.class);
        intent.putExtra("billId", bill.getId());
        startActivity(intent);
    }
    
    private void shareBill(Bill bill) {
        // TODO: Implement PDF generation and sharing
        showToast("Share bill feature coming soon");
    }
    
    private void showEmptyState(boolean show) {
        if (show) {
            billsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            billsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private void showLoading(boolean show) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            billsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("customersLoaded", customersLoaded);
        outState.putInt("selectedMonth", selectedMonth);
        outState.putInt("selectedYear", selectedYear);
    }
}
