package com.dailyserviceapp.customer;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.dailyserviceapp.R;
import com.dailyserviceapp.billing.MonthlyBillPdfGenerator;
import com.dailyserviceapp.billing.CustomerLedgerCalculator;
import com.dailyserviceapp.billing.CustomerLedgerSummary;
import com.dailyserviceapp.core.base.BaseActivity;
import com.dailyserviceapp.core.utils.Constants;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.dailyserviceapp.databinding.ActivityCustomerServiceDashboardBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Customer dashboard after provider link approval.
 */
public class CustomerServiceDashboardActivity extends BaseActivity {

    private ActivityCustomerServiceDashboardBinding binding;

    private FirebaseFirestore firestore;
    private String customerId;
    private String providerId;
    private String providerName;
    private String providerServiceType;

    private Customer customer;
    private final List<ServiceEntry> serviceEntries = new ArrayList<>();
    private final List<Payment> payments = new ArrayList<>();

    private final AtomicInteger pendingLoads = new AtomicInteger(0);
    private boolean skipNextResumeRefresh = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCustomerServiceDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!isLoggedIn()) {
            navigateToLogin();
            return;
        }

        if (!isCustomer()) {
            showToast("Customer dashboard is only for customers");
            finish();
            return;
        }

        customerId = getCurrentUserId();
        if (customerId == null || customerId.trim().isEmpty()) {
            showToast("Session expired. Please login again.");
            navigateToLogin();
            return;
        }

        firestore = FirebaseFirestore.getInstance();

        setupToolbar(binding.toolbar, "My Service Dashboard", true);
        binding.btnRefreshData.setOnClickListener(v -> loadActiveLinkAndDashboard());
        binding.btnComplaintSupport.setOnClickListener(v -> openComplaintSupport());
        binding.btnShareCustomerSummary.setOnClickListener(v -> shareCustomerSummary());
        binding.btnDownloadAndSharePdf.setOnClickListener(v -> showMonthPickerAndGenerate(true));

        loadActiveLinkAndDashboard();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.customer_home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_more) {
            showMoreMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showMoreMenu() {
        PopupMenu popupMenu = new PopupMenu(this, binding.toolbar, Gravity.END);
        popupMenu.getMenuInflater().inflate(R.menu.customer_home_more_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this::handleToolbarMenuClick);
        popupMenu.show();
    }

    private boolean handleToolbarMenuClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_support) {
            openComplaintSupport();
            return true;
        }
        if (itemId == R.id.action_logout) {
            performLogout();
            return true;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (skipNextResumeRefresh) {
            skipNextResumeRefresh = false;
            return;
        }
        loadActiveLinkAndDashboard();
    }

    private void loadActiveLinkAndDashboard() {
        setLoading(true);
        firestore.collection(Constants.COLLECTION_CUSTOMER_LINKS)
            .document(customerId)
            .get()
            .addOnSuccessListener(linkDoc -> {
                if (!isUiActive()) return;
                if (linkDoc == null || !linkDoc.exists()) {
                    setLoading(false);
                    showToast("No provider link found. Please connect first.");
                    openCustomerHome();
                    return;
                }

                String status = safeTrim(linkDoc.getString("status")).toUpperCase(Locale.US);
                if (!"ACTIVE".equals(status)) {
                    setLoading(false);
                    showToast("Provider link is " + (status.isEmpty() ? "PENDING" : status) + ".");
                    openCustomerHome();
                    return;
                }

                providerId = safeTrim(linkDoc.getString("providerId"));
                providerName = safeTrim(linkDoc.getString("providerName"));
                if (providerId.isEmpty()) {
                    setLoading(false);
                    showToast("Invalid provider link. Please reconnect.");
                    openCustomerHome();
                    return;
                }

                loadAllDashboardData();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                setLoading(false);
                Log.e("CustomerServiceDashboard", "Failed to load provider link", e);
                showToast("Failed to load provider link. Please try again.");
            });
    }

    private void loadAllDashboardData() {
        serviceEntries.clear();
        payments.clear();
        pendingLoads.set(4);

        loadCustomerDocument();
        loadProviderDocument();
        loadServiceEntries();
        loadPayments();
    }

    private void loadCustomerDocument() {
        firestore.collection(Constants.COLLECTION_CUSTOMERS)
            .document(customerId)
            .get()
            .addOnSuccessListener(doc -> {
                if (!isUiActive()) return;
                customer = doc != null ? doc.toObject(Customer.class) : null;
                if (customer == null) {
                    customer = new Customer();
                    customer.setId(customerId);
                    customer.setName(preferenceManager.getUserName());
                } else {
                    customer.setId(customerId);
                }
                markLoadComplete();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                customer = new Customer();
                customer.setId(customerId);
                customer.setName(preferenceManager.getUserName());
                markLoadComplete();
            });
    }

    private void loadProviderDocument() {
        firestore.collection(Constants.COLLECTION_PROVIDERS)
            .document(providerId)
            .get()
            .addOnSuccessListener(doc -> {
                if (!isUiActive()) return;
                if (doc != null && doc.exists()) {
                    String businessName = safeTrim(doc.getString("businessName"));
                    String ownerName = safeTrim(doc.getString("name"));

                    if (providerName == null || providerName.trim().isEmpty()) {
                        providerName = !businessName.isEmpty() ? businessName : ownerName;
                    }

                    providerServiceType = safeTrim(doc.getString("serviceType"));
                    if (providerServiceType.isEmpty()) {
                        Object servicesObj = doc.get("services");
                        if (servicesObj instanceof List) {
                            List<?> services = (List<?>) servicesObj;
                            if (!services.isEmpty() && services.get(0) instanceof String) {
                                providerServiceType = ((String) services.get(0)).trim();
                            }
                        }
                    }
                }
                markLoadComplete();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                markLoadComplete();
            });
    }

    private void loadServiceEntries() {
        if (providerId == null || providerId.trim().isEmpty()) {
            markLoadComplete();
            return;
        }
        firestore.collection(Constants.COLLECTION_SERVICE_ENTRIES)
            .whereEqualTo("customerId", customerId)
            .whereEqualTo("providerId", providerId)
            .limit(1000)
            .get()
            .addOnSuccessListener(query -> {
                if (!isUiActive()) return;
                if (query != null) {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        ServiceEntry entry = doc.toObject(ServiceEntry.class);
                        if (entry == null) continue;
                        entry.setId(doc.getId());
                        serviceEntries.add(entry);
                    }
                }

                serviceEntries.sort((e1, e2) -> {
                    Date d1 = e1.getDate() != null ? e1.getDate().toDate() : new Date(0);
                    Date d2 = e2.getDate() != null ? e2.getDate().toDate() : new Date(0);
                    return d2.compareTo(d1);
                });
                markLoadComplete();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                markLoadComplete();
            });
    }

    private void loadPayments() {
        if (providerId == null || providerId.trim().isEmpty()) {
            markLoadComplete();
            return;
        }
        firestore.collection(Constants.COLLECTION_PAYMENTS)
            .whereEqualTo("customerId", customerId)
            .whereEqualTo("providerId", providerId)
            .limit(1000)
            .get()
            .addOnSuccessListener(query -> {
                if (!isUiActive()) return;
                if (query != null) {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment == null) continue;
                        payment.setId(doc.getId());
                        payments.add(payment);
                    }
                }

                payments.sort((p1, p2) -> {
                    Date d1 = p1.getPaymentDate() != null ? p1.getPaymentDate().toDate() : new Date(0);
                    Date d2 = p2.getPaymentDate() != null ? p2.getPaymentDate().toDate() : new Date(0);
                    return d2.compareTo(d1);
                });
                markLoadComplete();
            })
            .addOnFailureListener(e -> {
                if (!isUiActive()) return;
                markLoadComplete();
            });
    }

    private void markLoadComplete() {
        if (!isUiActive()) return;
        if (pendingLoads.decrementAndGet() <= 0) {
            setLoading(false);
            renderDashboard();
        }
    }

    private void renderDashboard() {
        if (!isUiActive()) return;
        String displayProviderName = safeTrim(providerName);
        if (displayProviderName.isEmpty()) {
            displayProviderName = "Service Provider";
        }
        binding.txtProviderName.setText(displayProviderName);
        binding.txtProviderCode.setText("ID: " + shortProviderId(providerId));

        String serviceType = safeTrim(providerServiceType);
        if (serviceType.isEmpty() && customer != null) {
            serviceType = safeTrim(customer.getServiceType());
        }
        if (serviceType.isEmpty()) serviceType = "Service";
        binding.txtServiceType.setText(serviceType);

        renderTodaySummary();
        renderBillingSummary();
        renderServiceHistory();
        renderPaymentHistory();
    }

    private void renderTodaySummary() {
        int todayDeliveries = 0;
        double todayQuantity = 0.0;
        double todayAmount = 0.0;

        double fallbackRate = customer != null ? customer.getRatePerUnit() : 0.0;

        for (ServiceEntry entry : serviceEntries) {
            if (entry == null || entry.getDate() == null || !entry.isDelivered()) continue;
            Date date = entry.getDate().toDate();
            if (!DateUtils.isToday(date)) continue;

            todayDeliveries++;
            double quantity = entry.getQuantity() > 0 ? entry.getQuantity() : 1.0;
            double rate = entry.getRate() > 0 ? entry.getRate() : fallbackRate;
            todayQuantity += quantity;
            todayAmount += (quantity * rate);
        }

        if (todayDeliveries > 0) {
            binding.txtTodayStatus.setText("Delivered Today");
            binding.txtTodayDeliveredEntries.setText(String.valueOf(todayDeliveries));
            binding.txtTodayQuantity.setText(String.format(Locale.US, "%.2f", todayQuantity));
            binding.txtTodayAmount.setText(CurrencyUtils.formatCurrency(todayAmount));
        } else {
            binding.txtTodayStatus.setText("Not Delivered Yet");
            binding.txtTodayDeliveredEntries.setText("0");
            binding.txtTodayQuantity.setText("0.00");
            binding.txtTodayAmount.setText(CurrencyUtils.formatCurrency(0.0));
        }
    }

    private void renderBillingSummary() {
        Customer safeCustomer = customer != null ? customer : new Customer();
        if (safeCustomer.getId() == null) {
            safeCustomer.setId(customerId);
        }

        CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(safeCustomer, serviceEntries, payments);

        binding.txtTotalServiceValue.setText(CurrencyUtils.formatCurrency(summary.getTotalServiceAmount()));
        binding.txtTotalPaid.setText(CurrencyUtils.formatCurrency(summary.getTotalPaidAmount()));
        binding.txtOutstanding.setText(CurrencyUtils.formatCurrency(summary.getOutstandingAmount()));

        String lastPaid = "Not paid yet";
        if (!payments.isEmpty() && payments.get(0).getPaymentDate() != null) {
            lastPaid = DateUtils.formatShortDate(payments.get(0).getPaymentDate().toDate());
        }
        binding.txtLastPaidDate.setText(lastPaid);

        double monthPaid = getCurrentMonthPaidAmount();
        binding.txtMonthPaid.setText(CurrencyUtils.formatCurrency(monthPaid));
    }

    private double getCurrentMonthPaidAmount() {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        long monthStart = start.getTimeInMillis();
        double total = 0.0;
        for (Payment payment : payments) {
            if (payment == null || payment.getPaymentDate() == null) continue;
            long time = payment.getPaymentDate().toDate().getTime();
            if (time >= monthStart) {
                total += Math.max(0.0, payment.getAmount());
            }
        }
        return total;
    }

    private void renderServiceHistory() {
        binding.serviceHistoryContainer.removeAllViews();

        if (serviceEntries.isEmpty()) {
            binding.txtNoServiceHistory.setVisibility(View.VISIBLE);
            return;
        }

        binding.txtNoServiceHistory.setVisibility(View.GONE);
        int limit = Math.min(8, serviceEntries.size());
        double fallbackRate = customer != null ? customer.getRatePerUnit() : 0.0;

        for (int i = 0; i < limit; i++) {
            ServiceEntry entry = serviceEntries.get(i);
            if (entry == null) continue;

            double quantity = Math.max(entry.getQuantity(), 1.0);
            double normalizedFallbackRate = Math.max(fallbackRate, 0.0);
            double rate = entry.getRate() > 0 ? entry.getRate() : normalizedFallbackRate;
            double amount = quantity * rate;

            String title = (entry.isDelivered() ? "Delivered" : "Not delivered") + " • "
                + (entry.getDate() != null ? DateUtils.formatShortDate(entry.getDate().toDate()) : "-");
            String subtitle = String.format(Locale.US, "Qty: %.2f, Rate: %s", quantity, CurrencyUtils.formatCurrency(rate));
            String value = CurrencyUtils.formatCurrency(amount);
            addHistoryRow(binding.serviceHistoryContainer, title, subtitle, value);
        }
    }

    private void renderPaymentHistory() {
        binding.paymentHistoryContainer.removeAllViews();

        if (payments.isEmpty()) {
            binding.txtNoPaymentHistory.setVisibility(View.VISIBLE);
            return;
        }

        binding.txtNoPaymentHistory.setVisibility(View.GONE);
        int limit = Math.min(8, payments.size());

        for (int i = 0; i < limit; i++) {
            Payment payment = payments.get(i);
            if (payment == null) continue;

            String title = "Paid • " + (payment.getPaymentDate() != null
                ? DateUtils.formatShortDate(payment.getPaymentDate().toDate())
                : "-");
            String method = safeTrim(payment.getPaymentMethod());
            if (method.isEmpty()) method = "Payment";
            String subtitle = method;
            if (!safeTrim(payment.getNotes()).isEmpty()) {
                subtitle += " • " + payment.getNotes().trim();
            }
            String value = CurrencyUtils.formatCurrency(Math.max(0.0, payment.getAmount()));
            addHistoryRow(binding.paymentHistoryContainer, title, subtitle, value);
        }
    }

    private void addHistoryRow(LinearLayout container, String title, String subtitle, String value) {
        View row = getLayoutInflater().inflate(R.layout.row_customer_history, container, false);

        TextView txtTitle = row.findViewById(R.id.txtHistoryTitle);
        TextView txtSubtitle = row.findViewById(R.id.txtHistorySubtitle);
        TextView txtValue = row.findViewById(R.id.txtHistoryValue);

        txtTitle.setText(title);
        txtSubtitle.setText(subtitle);
        txtValue.setText(value);

        container.addView(row);
    }

    private String shortProviderId(String id) {
        if (id == null || id.trim().isEmpty()) return "-";
        String trimmed = id.trim();
        if (trimmed.length() <= 8) return trimmed.toUpperCase(Locale.US);
        return trimmed.substring(0, 8).toUpperCase(Locale.US);
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void setLoading(boolean loading) {
        if (!isUiActive()) return;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnRefreshData.setEnabled(!loading);
    }

    private void openCustomerHome() {
        if (!isUiActive()) return;
        Intent intent = new Intent(this, CustomerHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private boolean isUiActive() {
        return binding != null && !isFinishing() && !isDestroyed();
    }

    private void openComplaintSupport() {
        Intent intent = new Intent(this, ComplaintSupportActivity.class);
        startActivity(intent);
    }

    private void shareCustomerSummary() {
        if (customer == null) {
            showToast("Dashboard is still loading. Please try again.");
            return;
        }

        CustomerLedgerSummary summary = CustomerLedgerCalculator.calculate(customer, serviceEntries, payments);
        String text = "Customer: " + safeTrim(customer.getName()) + "\n"
            + "Provider: " + (safeTrim(providerName).isEmpty() ? "Service Provider" : providerName) + "\n"
            + "Total Service Value: " + CurrencyUtils.formatCurrency(summary.getTotalServiceAmount()) + "\n"
            + "Total Paid: " + CurrencyUtils.formatCurrency(summary.getTotalPaidAmount()) + "\n"
            + "Outstanding: " + CurrencyUtils.formatCurrency(summary.getOutstandingAmount()) + "\n"
            + "Delivered Entries: " + summary.getDeliveredEntries();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Service Summary");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share summary"));
    }

    private void showMonthPickerAndGenerate(boolean shareAfterDownload) {
        List<MonthYearItem> monthOptions = collectAvailableMonths();
        if (monthOptions.isEmpty()) {
            Calendar now = Calendar.getInstance();
            monthOptions.add(new MonthYearItem(now.get(Calendar.MONTH), now.get(Calendar.YEAR)));
        }

        String[] labels = new String[monthOptions.size()];
        for (int i = 0; i < monthOptions.size(); i++) {
            labels[i] = monthOptions.get(i).label();
        }

        androidx.appcompat.app.AlertDialog.Builder builder =
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select bill month")
                .setItems(labels, (DialogInterface dialog, int which) -> {
                    if (which < 0 || which >= monthOptions.size()) return;
                    MonthYearItem selected = monthOptions.get(which);
                    generateMonthlyBillPdf(selected.month, selected.year, shareAfterDownload);
                });
        builder.show();
    }

    private List<MonthYearItem> collectAvailableMonths() {
        Set<String> unique = new LinkedHashSet<>();
        List<MonthYearItem> items = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        for (ServiceEntry entry : serviceEntries) {
            if (entry == null || entry.getDate() == null) continue;
            cal.setTime(entry.getDate().toDate());
            int month = cal.get(Calendar.MONTH);
            int year = cal.get(Calendar.YEAR);
            String key = year + "_" + month;
            if (unique.add(key)) {
                items.add(new MonthYearItem(month, year));
            }
        }

        for (Payment payment : payments) {
            if (payment == null || payment.getPaymentDate() == null) continue;
            cal.setTime(payment.getPaymentDate().toDate());
            int month = cal.get(Calendar.MONTH);
            int year = cal.get(Calendar.YEAR);
            String key = year + "_" + month;
            if (unique.add(key)) {
                items.add(new MonthYearItem(month, year));
            }
        }

        items.sort((i1, i2) -> {
            if (i1.year != i2.year) return Integer.compare(i2.year, i1.year);
            return Integer.compare(i2.month, i1.month);
        });
        return items;
    }

    private void generateMonthlyBillPdf(int month, int year, boolean shareAfterDownload) {
        if (providerId == null || providerId.trim().isEmpty()) {
            showToast("Provider details unavailable. Please refresh.");
            return;
        }

        Customer safeCustomer = customer != null ? customer : new Customer();
        if (safeCustomer.getName() == null || safeCustomer.getName().trim().isEmpty()) {
            safeCustomer.setName(safeTrim(preferenceManager.getUserName()).isEmpty()
                ? "Customer" : preferenceManager.getUserName());
        }
        if (safeCustomer.getId() == null || safeCustomer.getId().trim().isEmpty()) {
            safeCustomer.setId(customerId);
        }

        setLoading(true);
        new Thread(() -> {
            try {
                MonthlyBillPdfGenerator.Result result = MonthlyBillPdfGenerator.generate(
                    CustomerServiceDashboardActivity.this,
                    safeCustomer,
                    providerName,
                    providerServiceType,
                    month,
                    year,
                    serviceEntries,
                    payments
                );

                runOnUiThread(() -> {
                    if (!isUiActive()) return;
                    setLoading(false);
                    showToast("Bill PDF saved: " + result.pdfFile.getName());

                    if (shareAfterDownload) {
                        sharePdfFile(result.pdfFile, month, year);
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    if (!isUiActive()) return;
                    setLoading(false);
                    showToast("Failed to generate PDF: " + e.getMessage());
                });
            }
        }).start();
    }

    private void sharePdfFile(File pdfFile, int month, int year) {
        if (pdfFile == null || !pdfFile.exists()) {
            showToast("PDF file not found");
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                pdfFile
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Monthly Bill - " + monthYearLabel(month, year));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.setClipData(ClipData.newUri(getContentResolver(), "Monthly Bill PDF", contentUri));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share bill PDF"));
        } catch (ActivityNotFoundException ex) {
            showToast("No app available to share PDF");
        } catch (Exception ex) {
            showToast("Unable to share PDF: " + ex.getMessage());
        }
    }

    private String monthYearLabel(int month, int year) {
        java.text.DateFormatSymbols symbols = new java.text.DateFormatSymbols(Locale.getDefault());
        String[] months = symbols.getMonths();
        String monthName = (month >= 0 && month < months.length) ? months[month] : "Month";
        return monthName + " " + year;
    }

    private static final class MonthYearItem {
        final int month;
        final int year;

        MonthYearItem(int month, int year) {
            this.month = month;
            this.year = year;
        }

        String label() {
            java.text.DateFormatSymbols symbols = new java.text.DateFormatSymbols(Locale.getDefault());
            String[] months = symbols.getMonths();
            String monthName = (month >= 0 && month < months.length) ? months[month] : "Month";
            return monthName + " " + year;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
