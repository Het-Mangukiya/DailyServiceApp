package com.dailyserviceapp.utils;

import android.content.Context;

import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Test Data Generator for creating sample data in the app.
 * Use this to populate the database with test customers, service entries, and bills.
 */
public class TestDataGenerator {

    private static final int TEST_CUSTOMER_COUNT = 30;
    
    private final String providerId;
    private final List<String> createdCustomerIds = new ArrayList<>();
    private final List<Customer> createdCustomers = new ArrayList<>();
    private final FirebaseFirestore db;
    
    public TestDataGenerator(Context context, String providerId) {
        this.providerId = providerId;
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Generate complete test dataset: customers + service entries for January 2026
     */
    public void generateCompleteTestData(OnTestDataGeneratedListener listener) {
        generateTestCustomers(new OnCustomersGeneratedListener() {
            @Override
            public void onCustomersGenerated(List<String> customerIds) {
                createdCustomerIds.addAll(customerIds);
                generateServiceEntriesForJanuary(customerIds, listener);
            }
            
            @Override
            public void onError(String error) {
                listener.onError("Failed to create customers: " + error);
            }
        });
    }
    
    /**
     * Create test customers with different service types.
     */
    private void generateTestCustomers(OnCustomersGeneratedListener listener) {
        int customerCount = TEST_CUSTOMER_COUNT;
        String[] serviceTypes = {"Milk", "Newspaper", "Water", "Tiffin"};
        String[] areas = {
            "MG Road",
            "Koramangala",
            "Whitefield",
            "Indiranagar",
            "Jayanagar",
            "HSR Layout",
            "BTM Layout",
            "Banashankari",
            "Rajajinagar",
            "Electronic City"
        };

        List<String> customerIds = new ArrayList<>();
        WriteBatch batch = db.batch();

        for (int i = 0; i < customerCount; i++) {
            String name = "Test Customer " + (i + 1);
            String phone = "9876543" + String.format(Locale.US, "%03d", i);
            String address = "House " + (100 + i) + ", " + areas[i % areas.length] + ", Bangalore";
                String area = areas[i % areas.length];
            String serviceType = serviceTypes[i % serviceTypes.length];
            double rate = 25 + (i % 6) * 5;

            Customer customer = new Customer(
                    name,
                    phone,
                    address,
                    serviceType,
                    rate,
                    Timestamp.now()
            );
            customer.setProviderId(providerId);
            customer.setStatus("ACTIVE");
            customer.setArea(area);
            customer.setOnVacation(false);
            customer.setDefaultQuantity(1.0 + (i % 3) * 0.5);
            createdCustomers.add(customer);

            DocumentReference ref = db.collection("customers").document();
            customerIds.add(ref.getId());
            batch.set(ref, customer);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> listener.onCustomersGenerated(customerIds))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Create service entries for each customer starting Jan 2026.
     * Generates a larger dataset with mixed delivery patterns.
     */
    private void generateServiceEntriesForJanuary(List<String> customerIds, OnTestDataGeneratedListener listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.JANUARY, 1, 8, 0, 0); // Start from Jan 1, 2026
        
        List<ServiceEntry> allEntries = new ArrayList<>();

        int daysToGenerate = 60;
        int customerCount = customerIds.size();
        for (int customerIndex = 0; customerIndex < customerCount; customerIndex++) {
            for (int day = 1; day <= daysToGenerate; day++) {
                calendar.set(2026, Calendar.JANUARY, 1, 8, 0, 0);
                calendar.add(Calendar.DAY_OF_YEAR, day - 1);

                boolean shouldDeliver;
                if (customerIndex % 4 == 0) {
                    shouldDeliver = day % 6 != 0; // skip every 6th day
                } else if (customerIndex % 4 == 1) {
                    shouldDeliver = day % 2 != 0; // alternate days
                } else if (customerIndex % 4 == 2) {
                    shouldDeliver = day % 7 != 0; // skip weekly
                } else {
                    shouldDeliver = true; // daily
                }

                if (shouldDeliver) {
                    double qty = 1.0 + (customerIndex % 3) * 0.5;
                    allEntries.add(createServiceEntry(customerIds.get(customerIndex), calendar.getTime(), qty));
                }
            }
        }
        
        // Save all entries in one batch (fast)
        WriteBatch batch = db.batch();
        for (ServiceEntry entry : allEntries) {
            DocumentReference ref = db.collection("serviceEntries").document();
            batch.set(ref, entry);
        }

        batch.commit()
                .addOnSuccessListener(aVoid ->
                        generateSupportTicketsForCustomers(createdCustomerIds, createdCustomers, allEntries.size(), listener))
                .addOnFailureListener(e -> listener.onError("Failed to save service entries: " + e.getMessage()));
    }

    private void generateSupportTicketsForCustomers(
            List<String> customerIds,
            List<Customer> customers,
            int entriesCount,
            OnTestDataGeneratedListener listener
    ) {
        if (customerIds == null || customerIds.isEmpty() || customers == null || customers.isEmpty()) {
            listener.onTestDataGenerated(createdCustomerIds, entriesCount);
            return;
        }

        String[] categories = {"Delivery Issue", "Billing Issue", "Payment Issue", "App Issue", "Other"};
        String[] subjects = {
            "Missed delivery",
            "Incorrect bill total",
            "Payment not updated",
            "Need schedule change",
            "General query"
        };

        WriteBatch batch = db.batch();
        int ticketCount = Math.min(200, customerIds.size() * 6);
        int customerCount = Math.min(customerIds.size(), customers.size());

        for (int i = 0; i < ticketCount; i++) {
            int index = i % customerCount;
            String customerId = customerIds.get(index);
            Customer customer = customers.get(index);

            DocumentReference ref = db.collection("supportTickets").document();
            batch.set(ref, buildSupportTicket(customerId, customer, categories[i % categories.length],
                    subjects[i % subjects.length], i));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> listener.onTestDataGenerated(createdCustomerIds, entriesCount))
                .addOnFailureListener(e -> listener.onError("Failed to save support tickets: " + e.getMessage()));
    }

    private java.util.Map<String, Object> buildSupportTicket(
            String customerId,
            Customer customer,
            String category,
            String subject,
            int index
    ) {
        java.util.Map<String, Object> ticket = new java.util.HashMap<>();
        ticket.put("customerId", customerId);
        ticket.put("providerId", providerId);
        ticket.put("customerName", customer != null ? customer.getName() : "");
        ticket.put("customerEmail", "");
        ticket.put("providerName", "Test Provider");
        ticket.put("providerEmail", "");
        ticket.put("category", category);
        ticket.put("subject", subject);
        ticket.put("message", "Sample complaint message #" + (index + 1));
        ticket.put("status", index % 3 == 0 ? "IN_PROGRESS" : (index % 4 == 0 ? "RESOLVED" : "OPEN"));
        ticket.put("createdAt", Timestamp.now());
        ticket.put("updatedAt", Timestamp.now());
        if (index % 4 == 0) {
            ticket.put("resolvedAt", Timestamp.now());
        }
        return ticket;
    }
    
    private ServiceEntry createServiceEntry(String customerId, Date date, double quantity) {
        ServiceEntry entry = new ServiceEntry();
        entry.setProviderId(providerId);
        entry.setCustomerId(customerId);
        entry.setDate(new Timestamp(date));
        entry.setQuantity(quantity);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        entry.setNotes("Test delivery - " + sdf.format(date));
        entry.setCreatedAt(Timestamp.now());
        return entry;
    }
    
    public interface OnCustomersGeneratedListener {
        void onCustomersGenerated(List<String> customerIds);
        void onError(String error);
    }
    
    public interface OnTestDataGeneratedListener {
        void onTestDataGenerated(List<String> customerIds, int entriesCount);
        void onError(String error);
    }
}
