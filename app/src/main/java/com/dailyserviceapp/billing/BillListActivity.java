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
    private boolean customersLoaded = false; /**
     * Initializes the activity UI and data for viewing and managing monthly bills.
     *
     * Ensures the user is logged in (redirecting to login if not), sets the layout and toolbar,
     * restores persisted month/year and customers-loaded state when available, initializes views,
     * data, and click listeners, and starts loading bills for the selected month.
     *
     * @param savedInstanceState bundle that may contain previously saved values:
     *                           "customersLoaded" (boolean), "selectedMonth" (int, Calendar month),
     *                           and "selectedYear" (int, year)
     */
    
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
    
    /**
     * Binds the activity's UI widgets to their fields and configures the bills list layout.
     *
     * Initializes view references for month navigation controls, the bills RecyclerView,
     * the empty-state container, the generate-bills FAB, and the loading indicator, and
     * sets a LinearLayoutManager on the RecyclerView.
     */
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
    
    /**
     * Initialize core data for the activity and configure the bills RecyclerView adapter.
     *
     * Initializes the Firestore repository and provider ID, sets selectedMonth and selectedYear
     * to the current date, creates a BillAdapter with handlers for viewing and sharing bills,
     * assigns it to the RecyclerView, and updates the month display.
     */
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
    
    /**
     * Attaches click listeners to the previous/next month buttons and the generate bills FAB.
     *
     * When the previous/next buttons are tapped, adjusts selectedMonth and selectedYear as needed,
     * updates the month display, and reloads data for the new month. Tapping the FAB triggers bill generation.
     */
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
    
    /**
     * Updates the displayed month label to reflect the currently selected month and year.
     *
     * Sets the activity's month text view to the full month name (e.g., "January")
     * followed by the selected year (e.g., "2026").
     */
    private void updateMonthDisplay() {
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                               "July", "August", "September", "October", "November", "December"};
        selectedMonthText.setText(monthNames[selectedMonth] + " " + selectedYear);
    }
    
    /**
     * Load customers (if not already cached) and then load bills for the currently selected provider and month,
     * updating UI state and showing user-facing messages for authentication or network problems.
     *
     * <p>Behavior:
     * - If the provider ID is missing, shows a login prompt and displays the empty state.
     * - If there is no network, shows a connectivity toast and aborts.
     * - Shows a loading indicator while fetching data.
     * - If customers are not yet loaded, fetches and caches them, marks them as loaded, and then fetches bills.
     * - If customers are already cached, directly fetches bills for the selected month/year.</p>
     *
     * <p>Side effects: updates customerMap and customersLoaded, toggles loading/empty-state UI, and displays toasts on errors or missing authentication.</p>
     */
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
    
    /**
     * Loads bills for the current provider and selected month/year, updates local state and the UI.
     *
     * <p>On success updates the activity's bill list, hides the loading indicator, shows the empty
     * state if no bills were returned, or submits the bills and their corresponding customer names
     * to the adapter for display.</p>
     *
     * <p>On error hides the loading indicator, shows an error toast, and displays the empty state.</p>
     */
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
    
    /**
     * Initiates generation of bills for the currently selected month and year.
     *
     * Shows a progress dialog, loads all customers for the current provider, fetches service entries
     * within the selected month range, and delegates creation of bills to generateBillsFromEntries.
     * If no customers are found or an error occurs while loading customers or service entries, the
     * progress dialog is dismissed and an error or informational toast is shown.
     */
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
                                generateBillsFromEntries(customers, entries, progressDialog);
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
    
    /**
     * Creates and saves bills for customers based on provided service entries for the selected month.
     *
     * Processes the list of service entries grouped by customer, generates a Bill for each customer
     * with delivered entries whose total amount is greater than zero, saves each bill via the
     * repository, dismisses the provided progress dialog, shows a toast summarizing the result,
     * and reloads the bill list when one or more bills are generated.
     *
     * @param customers     list of customers to consider when generating bills
     * @param entries       service entries (typically filtered to the selected month) used to compute bills
     * @param progressDialog active ProgressDialog shown during generation; it will be dismissed by this method
     */
    private void generateBillsFromEntries(List<Customer> customers, List<ServiceEntry> entries,
                                          ProgressDialog progressDialog) {
        // Group entries by customer
        Map<String, List<ServiceEntry>> entriesByCustomer = new HashMap<>();
        for (ServiceEntry entry : entries) {
            if (!entriesByCustomer.containsKey(entry.getCustomerId())) {
                entriesByCustomer.put(entry.getCustomerId(), new ArrayList<>());
            }
            entriesByCustomer.get(entry.getCustomerId()).add(entry);
        }
        
        int billsGenerated = 0;
        int customersProcessed = 0;
        
        for (Customer customer : customers) {
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
            showToast("Generated " + billsGenerated + " bills");
            loadData(); // Reload the list
        } else {
            showToast("No bills to generate (no service entries found)");
        }
    }
    
    /**
     * Open the bill details screen for the given bill.
     *
     * Starts BillDetailActivity and supplies the bill's identifier as an intent extra with key "billId".
     *
     * @param bill the Bill whose details should be displayed; its `id` is passed to the detail activity
     */
    private void openBillDetails(Bill bill) {
        Intent intent = new Intent(this, BillDetailActivity.class);
        intent.putExtra("billId", bill.getId());
        startActivity(intent);
    }
    
    /**
     * Starts the process to export the specified bill as a PDF and share it via available apps.
     *
     * <p>Currently shows a placeholder toast until PDF generation and sharing are implemented.</p>
     *
     * @param bill the bill to export and share
     */
    private void shareBill(Bill bill) {
        // TODO: Implement PDF generation and sharing
        showToast("Share bill feature coming soon");
    }
    
    /**
     * Toggle visibility between the bills list and the empty-state layout.
     *
     * @param show `true` to show the empty-state layout and hide the bills list, `false` to show the bills list and hide the empty state
     */
    private void showEmptyState(boolean show) {
        if (show) {
            billsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            billsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * Toggles the loading indicator for the bills screen.
     *
     * When `show` is true, makes the loading progress visible and hides the bills list and empty-state view.
     * When `show` is false, hides the loading progress and leaves the bills list and empty-state visibility unchanged.
     *
     * @param show `true` to show the loading indicator, `false` to hide it
     */
    private void showLoading(boolean show) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            billsRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * Saves UI state required to restore the activity after recreation: the loaded-customers flag
     * and the currently selected month and year.
     *
     * @param outState bundle in which to place the saved state values (`customersLoaded`, `selectedMonth`, `selectedYear`)
     */
    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("customersLoaded", customersLoaded);
        outState.putInt("selectedMonth", selectedMonth);
        outState.putInt("selectedYear", selectedYear);
    }
}