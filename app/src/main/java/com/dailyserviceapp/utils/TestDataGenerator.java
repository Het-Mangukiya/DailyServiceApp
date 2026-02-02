package com.dailyserviceapp.utils;

import android.content.Context;

import com.dailyserviceapp.data.FirestoreRepository;
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
    
    private final FirestoreRepository repository;
    private final String providerId;
    private final Context context;
    private final List<String> createdCustomerIds = new ArrayList<>();
    private final FirebaseFirestore db;
    
    /**
     * Creates a TestDataGenerator bound to the given Android context and provider.
     *
     * Initializes internal Firestore repository and Firestore instance used to write test data.
     *
     * @param context    the Android Context used for resource access and any context-bound operations
     * @param providerId identifier of the provider for which test data will be generated
     */
    public TestDataGenerator(Context context, String providerId) {
        this.context = context;
        this.providerId = providerId;
        this.repository = new FirestoreRepository();
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Orchestrates generation of test customers and associated service entries for January 2026.
     *
     * <p>Creates a set of test customers, then generates service entries for those customers for
     * January 2026. Any errors encountered during creation are reported through the provided listener.</p>
     *
     * @param listener callback invoked with the generated customer IDs and number of created service entries,
     *                 or called with an error message if generation fails
     */
    public void generateCompleteTestData(OnTestDataGeneratedListener listener) {
        generateTestCustomers(new OnCustomersGeneratedListener() {
            /**
             * Handles newly generated customer IDs by recording them and initiating generation of service entries for January.
             *
             * @param customerIds list of created customer document IDs
             */
            @Override
            public void onCustomersGenerated(List<String> customerIds) {
                createdCustomerIds.addAll(customerIds);
                generateServiceEntriesForJanuary(customerIds, listener);
            }
            
            /**
             * Forwards a customer-generation error to the parent listener, prefixed with "Failed to create customers: ".
             *
             * @param error the original error message describing the failure
             */
            @Override
            public void onError(String error) {
                listener.onError("Failed to create customers: " + error);
            }
        });
    }
    
    /**
         * Generates five predefined test customers and writes them to Firestore.
         *
         * Writes five sample Customer records into the "customers" collection and, when the batch commit completes,
         * invokes listener.onCustomersGenerated with the created document IDs; on failure invokes listener.onError
         * with the error message.
         *
         * @param listener callback that receives the list of created customer IDs on success or an error message on failure
         */
    private void generateTestCustomers(OnCustomersGeneratedListener listener) {
        String[][] customerData = {
            {"Rajesh Kumar", "9876543210", "House #123, MG Road, Bangalore", "Milk", "50"},
            {"Priya Sharma", "9876543211", "Flat 45, Koramangala, Bangalore", "Newspaper", "30"},
            {"Amit Patel", "9876543212", "Villa 12, Whitefield, Bangalore", "Milk", "60"},
            {"Sneha Reddy", "9876543213", "Apartment 78, Indiranagar, Bangalore", "Milk", "45"},
            {"Vijay Singh", "9876543214", "House 90, Jayanagar, Bangalore", "Newspaper", "25"}
        };
        
        List<String> customerIds = new ArrayList<>();
        WriteBatch batch = db.batch();

        for (String[] data : customerData) {
            Customer customer = new Customer(
                    data[0], // name
                    data[1], // phone
                    data[2], // address
                    data[3], // serviceType
                    Double.parseDouble(data[4]), // ratePerUnit
                    Timestamp.now() // createdAt
            );
            customer.setProviderId(providerId);
            customer.setStatus("ACTIVE");

            DocumentReference ref = db.collection("customers").document();
            customerIds.add(ref.getId());
            batch.set(ref, customer);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> listener.onCustomersGenerated(customerIds))
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Generate service entries for each provided customer for January 2026 following predefined patterns.
     *
     * <p>Creates a set of ServiceEntry objects for January 1–28, 2026 using the following per-customer patterns:
     * <ul>
     *   <li>customerIds[0]: daily entries (28 days)</li>
     *   <li>customerIds[1]: 25 entries (skips days 7, 15, 23)</li>
     *   <li>customerIds[2]: 26 entries (skips days 10, 20)</li>
     *   <li>customerIds[3]: alternate days (every other day)</li>
     *   <li>customerIds[4]: 24 entries (skips every 7th day)</li>
     * </ul>
     * </p>
     *
     * <p>All entries are written in a single Firestore batch to the "serviceEntries" collection.
     * On success invokes listener.onTestDataGenerated(createdCustomerIds, entriesCount).
     * On failure invokes listener.onError(...) with the error message.</p>
     *
     * @param customerIds list of customer IDs to generate entries for; expected to contain at least five IDs
     * @param listener callback to receive success or error notifications
     */
    private void generateServiceEntriesForJanuary(List<String> customerIds, OnTestDataGeneratedListener listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.JANUARY, 1, 8, 0, 0); // Start from Jan 1, 2026
        
        List<ServiceEntry> allEntries = new ArrayList<>();
        
        // Customer 0 (Rajesh): Daily entries (28 days)
        for (int day = 1; day <= 28; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            allEntries.add(createServiceEntry(customerIds.get(0), calendar.getTime(), 2.0));
        }
        
        // Customer 1 (Priya): 25 days (missed 3 days)
        for (int day = 1; day <= 28; day++) {
            if (day != 7 && day != 15 && day != 23) { // Skip 3 days
                calendar.set(Calendar.DAY_OF_MONTH, day);
                allEntries.add(createServiceEntry(customerIds.get(1), calendar.getTime(), 1.0));
            }
        }
        
        // Customer 2 (Amit): 26 days (missed 2 days)
        for (int day = 1; day <= 28; day++) {
            if (day != 10 && day != 20) { // Skip 2 days
                calendar.set(Calendar.DAY_OF_MONTH, day);
                allEntries.add(createServiceEntry(customerIds.get(2), calendar.getTime(), 2.5));
            }
        }
        
        // Customer 3 (Sneha): 20 days (alternate + some gaps)
        for (int day = 1; day <= 28; day += 2) { // Every alternate day
            calendar.set(Calendar.DAY_OF_MONTH, day);
            allEntries.add(createServiceEntry(customerIds.get(3), calendar.getTime(), 1.5));
        }
        
        // Customer 4 (Vijay): 24 days
        for (int day = 1; day <= 28; day++) {
            if (day % 7 != 0) { // Skip every 7th day (Sundays)
                calendar.set(Calendar.DAY_OF_MONTH, day);
                allEntries.add(createServiceEntry(customerIds.get(4), calendar.getTime(), 1.0));
            }
        }
        
        // Save all entries in one batch (fast)
        WriteBatch batch = db.batch();
        for (ServiceEntry entry : allEntries) {
            DocumentReference ref = db.collection("serviceEntries").document();
            batch.set(ref, entry);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> listener.onTestDataGenerated(createdCustomerIds, allEntries.size()))
                .addOnFailureListener(e -> listener.onError("Failed to save service entries: " + e.getMessage()));
    }
    
    /**
     * Creates a ServiceEntry pre-populated for a test delivery.
     *
     * @param customerId the customer identifier for the entry
     * @param date       the service date for the entry
     * @param quantity   the delivered quantity for the entry
     * @return           a ServiceEntry populated with providerId, customerId, date, quantity, a "Test delivery - {date}" note, and a creation timestamp
     */
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
        /**
 * Callback invoked when test customers have been successfully created.
 *
 * @param customerIds the list of generated customer document IDs corresponding to the created customers
 */
void onCustomersGenerated(List<String> customerIds);
        /**
 * Notifies the caller that an error occurred during the operation.
 *
 * @param error a human-readable error message describing the failure
 */
void onError(String error);
    }
    
    public interface OnTestDataGeneratedListener {
        /**
 * Invoked after test data generation finishes, providing created customer IDs and the total number of service entries created.
 *
 * @param customerIds the list of generated customer document IDs
 * @param entriesCount the total number of service entries created
 */
void onTestDataGenerated(List<String> customerIds, int entriesCount);
        /**
 * Notifies the caller that an error occurred during the operation.
 *
 * @param error a human-readable error message describing the failure
 */
void onError(String error);
    }
}