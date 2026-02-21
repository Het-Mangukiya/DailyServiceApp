package com.dailyserviceapp.data;

import android.util.Log;

import androidx.annotation.NonNull;

import com.dailyserviceapp.data.models.Bill;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for Firestore database operations.
 * Handles CRUD operations for customers, service entries, bills, and payments.
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-09
 */
public class FirestoreRepository {
    private static final DateTimeFormatter DATE_KEY = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    private CollectionReference customers() {
        return db.collection("customers");
    }

    public void addCustomer(Customer customer, OnSuccessListener<DocumentReference> onSuccess, OnFailureListener onFailure) {
        if (customer.getCreatedAt() == null) {
            customer.setCreatedAt(Timestamp.now());
        }
        customers().add(customer).addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    public void getCustomer(String customerId, OnSuccessListener<DocumentSnapshot> onSuccess, OnFailureListener onFailure) {
        customers().document(customerId).get().addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
    }

    public void listCustomers(OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        customers()
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Legacy method using per-customer subcollections.
     * Prefer serviceEntries collection instead.
     */
    @Deprecated
    public void markDeliveredToday(String customerId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        LocalDate today = LocalDate.now();
        String dateKey = today.format(DATE_KEY);

        DeliveryEntry entry = new DeliveryEntry(dateKey, true, Timestamp.now());
        customers()
                .document(customerId)
                .collection("deliveries")
                .document(dateKey)
                .set(entry)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Legacy method using per-customer subcollections.
     * Prefer serviceEntries collection instead.
     */
    @Deprecated
    public void countDeliveredInMonth(String customerId, String monthKey, OnSuccessListener<Integer> onSuccess, OnFailureListener onFailure) {
        // monthKey: yyyyMM, deliveries stored as yyyyMMdd in doc id, so range query on doc id.
        String start = monthKey + "01";
        String endExclusive = monthKey + "32";

        customers()
                .document(customerId)
                .collection("deliveries")
                .whereEqualTo("delivered", true)
                .orderBy("dateKey")
                .startAt(start)
                .endAt(endExclusive)
                .get()
                .addOnSuccessListener(snapshot -> onSuccess.onSuccess(snapshot.size()))
                .addOnFailureListener(onFailure);
    }

    /**
     * Legacy method using per-customer subcollections.
     * Prefer bills/payments collections instead.
     */
    @Deprecated
    public void getPaymentStatus(String customerId, String monthKey, OnSuccessListener<PaymentStatus> onSuccess, OnFailureListener onFailure) {
        customers()
                .document(customerId)
                .collection("payments")
                .document(monthKey)
                .get()
                .addOnSuccessListener(doc -> {
                    PaymentStatus status = doc.toObject(PaymentStatus.class);
                    if (status == null) {
                        status = new PaymentStatus(monthKey, false, 0.0, null);
                    }
                    onSuccess.onSuccess(status);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Legacy method using per-customer subcollections.
     * Prefer bills/payments collections instead.
     */
    @Deprecated
    public void setPaymentPaid(String customerId, String monthKey, double paidAmount, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        PaymentStatus status = new PaymentStatus(monthKey, true, paidAmount, Timestamp.now());
        customers()
                .document(customerId)
                .collection("payments")
                .document(monthKey)
                .set(status)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /**
     * Updates an existing customer.
     * 
     * @param customer Customer with updated fields (must have ID set)
     * @param listener Callback for completion
     */
    public void updateCustomer(Customer customer, OnSaveCompleteListener listener) {
        if (customer.getId() == null || customer.getId().isEmpty()) {
            listener.onError("Customer ID is required for update");
            return;
        }

        customers()
                .document(customer.getId())
                .set(customer)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Deletes a customer and all associated data.
     * WARNING: This also deletes service entries, bills, and payments.
     * 
     * @param customerId Customer ID to delete
     * @param listener Callback for completion
     */
    public void deleteCustomer(String customerId, OnSaveCompleteListener listener) {
        customers()
                .document(customerId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        listener.onError("Customer not found");
                        return;
                    }

                    // Cascade delete in sequence to avoid orphaned data
                    deleteDocuments(db.collection("serviceEntries")
                                    .whereEqualTo("customerId", customerId),
                            new OnSaveCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    deleteDocuments(db.collection("payments")
                                                    .whereEqualTo("customerId", customerId),
                                            new OnSaveCompleteListener() {
                                                @Override
                                                public void onSuccess() {
                                                    deleteDocuments(db.collection("bills")
                                                                    .whereEqualTo("customerId", customerId),
                                                            new OnSaveCompleteListener() {
                                                                @Override
                                                                public void onSuccess() {
                                                                    // Legacy subcollections cleanup
                                                                    deleteDocuments(customers()
                                                                                    .document(customerId)
                                                                                    .collection("deliveries"),
                                                                            new OnSaveCompleteListener() {
                                                                                @Override
                                                                                public void onSuccess() {
                                                                                    deleteDocuments(customers()
                                                                                                    .document(customerId)
                                                                                                    .collection("payments"),
                                                                                            new OnSaveCompleteListener() {
                                                                                                @Override
                                                                                                public void onSuccess() {
                                                                                                    // Finally delete customer doc
                                                                                                    customers()
                                                                                                            .document(customerId)
                                                                                                            .delete()
                                                                                                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                                                                                                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                                                                                                }

                                                                                                @Override
                                                                                                public void onError(String error) {
                                                                                                    listener.onError(error);
                                                                                                }
                                                                                            });
                                                                                }

                                                                                @Override
                                                                                public void onError(String error) {
                                                                                    listener.onError(error);
                                                                                }
                                                                            });
                                                                }

                                                                @Override
                                                                public void onError(String error) {
                                                                    listener.onError(error);
                                                                }
                                                            });
                                                }

                                                @Override
                                                public void onError(String error) {
                                                    listener.onError(error);
                                                }
                                            });
                                }

                                @Override
                                public void onError(String error) {
                                    listener.onError(error);
                                }
                            });
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Deletes all documents from a query in batches.
     */
    private void deleteDocuments(Query query, OnSaveCompleteListener listener) {
        final int batchSize = 400;

        query.limit(batchSize)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        listener.onSuccess();
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (snapshot.size() >= batchSize) {
                                    // There may be more documents; repeat
                                    deleteDocuments(query, listener);
                                } else {
                                    listener.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    // ========== Service Entry Methods ==========
    
    /**
     * Listener interface for loading customers.
     */
    public interface OnCustomersLoadedListener {
        void onCustomersLoaded(List<Customer> customers);
        void onError(String error);
    }
    
    /**
     * Listener interface for loading service entries.
     */
    public interface OnServiceEntriesLoadedListener {
        void onServiceEntriesLoaded(List<ServiceEntry> entries);
        void onError(String error);
    }
    
    /**
     * Listener interface for save operations.
     */
    public interface OnSaveCompleteListener {
        void onSuccess();
        void onError(String error);
    }
    
    /**
     * Gets all customers for a specific provider.
     * 
     * @param providerId The provider's ID
     * @param listener Callback for results
     */
    public void getCustomersByProvider(String providerId, OnCustomersLoadedListener listener) {
        customers()
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Customer> customerList = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Customer customer = doc.toObject(Customer.class);
                        if (customer != null && isActiveCustomer(doc)) {
                            customer.setId(doc.getId());
                            customerList.add(customer);
                        }
                    }
                    listener.onCustomersLoaded(customerList);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Listens to real-time updates for customers of a specific provider.
     * Returns a ListenerRegistration that should be removed when done.
     * 
     * @param providerId The provider's ID
     * @param listener Callback for real-time updates
     * @return ListenerRegistration to remove listener
     */
    public ListenerRegistration listenToCustomers(String providerId, OnCustomersLoadedListener listener) {
        return customers()
                .whereEqualTo("providerId", providerId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    
                    if (querySnapshot != null) {
                        List<Customer> customerList = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Customer customer = doc.toObject(Customer.class);
                            if (customer != null && isActiveCustomer(doc)) {
                                customer.setId(doc.getId());
                                customerList.add(customer);
                            }
                        }
                        listener.onCustomersLoaded(customerList);
                    }
                });
    }

    private boolean isActiveCustomer(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot == null) return false;
        String status = documentSnapshot.getString("status");
        return status == null || status.trim().isEmpty() || "ACTIVE".equalsIgnoreCase(status);
    }
    
    /**
     * Gets service entries for a provider within a date range.
     * Filters in memory to avoid complex Firestore index requirements.
     * 
     * @param providerId The provider's ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param listener Callback for results
     */
    public void getServiceEntriesByProviderAndDate(String providerId, Timestamp startDate, 
                                                    Timestamp endDate, OnServiceEntriesLoadedListener listener) {
        final long startMillis = startDate != null ? startDate.toDate().getTime() : Long.MIN_VALUE;
        final long endMillis = endDate != null ? endDate.toDate().getTime() : Long.MAX_VALUE;

        db.collection("serviceEntries")
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ServiceEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ServiceEntry entry = doc.toObject(ServiceEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            
                            // Filter by date range in memory
                            Timestamp entryDate = entry.getDate();
                            if (entryDate != null) {
                                long entryMillis = entryDate.toDate().getTime();
                                // Keep [start, end) semantics consistent with Firestore whereLessThan(end).
                                if (entryMillis < startMillis || entryMillis >= endMillis) {
                                    continue;
                                }
                                entries.add(entry);
                            } else {
                                Log.w("FirestoreRepository",
                                    "Skipping service entry with null date: " + doc.getId());
                            }
                        }
                    }
                    listener.onServiceEntriesLoaded(entries);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Gets all delivered service entries for a provider.
     * Falls back to in-memory filtering when a composite index is unavailable.
     */
    public void getDeliveredServiceEntriesByProvider(String providerId, OnServiceEntriesLoadedListener listener) {
        db.collection("serviceEntries")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("delivered", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ServiceEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ServiceEntry entry = doc.toObject(ServiceEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    listener.onServiceEntriesLoaded(entries);
                })
                .addOnFailureListener(e -> {
                    if (!isMissingIndexError(e)) {
                        Log.e("FirestoreRepository",
                            "Failed to load delivered entries for provider: " + providerId, e);
                        listener.onError(e.getMessage());
                        return;
                    }
                    // Fallback for environments without composite index support.
                    Log.w("FirestoreRepository",
                        "Missing index for delivered entries query; using in-memory filter", e);
                    db.collection("serviceEntries")
                            .whereEqualTo("providerId", providerId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<ServiceEntry> entries = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    ServiceEntry entry = doc.toObject(ServiceEntry.class);
                                    if (entry != null && entry.isDelivered()) {
                                        entry.setId(doc.getId());
                                        entries.add(entry);
                                    }
                                }
                                listener.onServiceEntriesLoaded(entries);
                            })
                            .addOnFailureListener(inner -> listener.onError(inner.getMessage()));
                });
    }

    /**
     * Gets delivered service entries for a provider within a date range.
     * Falls back to in-memory filtering if index is missing.
     */
    public void getDeliveredServiceEntriesByProviderInRange(String providerId, Timestamp startDate,
                                                            Timestamp endDate, OnServiceEntriesLoadedListener listener) {
        db.collection("serviceEntries")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("delivered", true)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThan("date", endDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ServiceEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ServiceEntry entry = doc.toObject(ServiceEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            entries.add(entry);
                        }
                    }
                    listener.onServiceEntriesLoaded(entries);
                })
                .addOnFailureListener(e -> {
                    if (!isMissingIndexError(e)) {
                        Log.e("FirestoreRepository",
                            "Failed to load delivered entries in range for provider: " + providerId, e);
                        listener.onError(e.getMessage());
                        return;
                    }
                    // Fallback: use broader query and filter in memory while preserving delivered=true.
                    Log.w("FirestoreRepository",
                        "Missing index for delivered range query; using in-memory filter", e);
                    final long startMillis = startDate != null ? startDate.toDate().getTime() : Long.MIN_VALUE;
                    final long endMillis = endDate != null ? endDate.toDate().getTime() : Long.MAX_VALUE;

                    db.collection("serviceEntries")
                            .whereEqualTo("providerId", providerId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<ServiceEntry> entries = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    ServiceEntry entry = doc.toObject(ServiceEntry.class);
                                    if (entry == null || !entry.isDelivered() || entry.getDate() == null) {
                                        continue;
                                    }

                                    long entryMillis = entry.getDate().toDate().getTime();
                                    if (entryMillis < startMillis || entryMillis >= endMillis) {
                                        continue;
                                    }

                                    entry.setId(doc.getId());
                                    entries.add(entry);
                                }
                                listener.onServiceEntriesLoaded(entries);
                            })
                            .addOnFailureListener(inner -> listener.onError(inner.getMessage()));
                });
    }

    /**
     * Gets service entries for a specific customer within a date range.
     * Filters in memory to avoid complex Firestore index requirements.
     *
     * @param customerId Customer ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param listener Callback for results
     */
    public void getServiceEntriesByCustomerAndDate(String customerId, Timestamp startDate,
                                                   Timestamp endDate, OnServiceEntriesLoadedListener listener) {
        final long startMillis = startDate != null ? startDate.toDate().getTime() : Long.MIN_VALUE;
        final long endMillis = endDate != null ? endDate.toDate().getTime() : Long.MAX_VALUE;
        db.collection("serviceEntries")
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ServiceEntry> entries = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ServiceEntry entry = doc.toObject(ServiceEntry.class);
                        if (entry != null) {
                            entry.setId(doc.getId());
                            
                            // Filter by date range in memory
                            Timestamp entryDate = entry.getDate();
                            if (entryDate != null) {
                                long entryMillis = entryDate.toDate().getTime();
                                if (entryMillis < startMillis || entryMillis >= endMillis) {
                                    continue;
                                }
                                entries.add(entry);
                            } else {
                                Log.w("FirestoreRepository",
                                    "Skipping service entry with null date: " + doc.getId());
                            }
                        }
                    }
                    listener.onServiceEntriesLoaded(entries);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Saves or updates a service entry.
     * 
     * @param entry The service entry to save
     * @param listener Callback for completion
     */
    public void saveServiceEntry(ServiceEntry entry, OnSaveCompleteListener listener) {
        if (entry.getId() != null && !entry.getId().isEmpty()) {
            // Update existing entry
            db.collection("serviceEntries")
                    .document(entry.getId())
                    .set(entry)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            // Create new entry
            db.collection("serviceEntries")
                    .add(entry)
                    .addOnSuccessListener(documentReference -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }
    }
    
    /**
     * Saves a service entry and atomically updates customer's lent amount.
     * Uses Firestore transaction to ensure atomicity (both succeed or both fail).
     * Also checks for duplicate entry (one delivery per customer per day).
     * 
     * @param entry The service entry to save
     * @param customerId Customer ID
     * @param deliveryCost Cost to add to lent amount (rate × quantity)
     * @param listener Callback for completion
     */
    public void saveServiceEntryWithTransaction(ServiceEntry entry, String customerId, 
                                                 double deliveryCost, OnSaveCompleteListener listener) {
        // First check for duplicate entry (same customer + same day)
        // We'll query all entries for this customer and check dates in memory
        db.collection("serviceEntries")
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Check if any entry matches the same day
                    Timestamp entryDate = entry.getDate();
                    java.util.Calendar entryCal = java.util.Calendar.getInstance();
                    entryCal.setTime(entryDate.toDate());
                    int entryYear = entryCal.get(java.util.Calendar.YEAR);
                    int entryMonth = entryCal.get(java.util.Calendar.MONTH);
                    int entryDay = entryCal.get(java.util.Calendar.DAY_OF_MONTH);
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Timestamp existingDate = doc.getTimestamp("date");
                        if (existingDate != null) {
                            java.util.Calendar existingCal = java.util.Calendar.getInstance();
                            existingCal.setTime(existingDate.toDate());
                            
                            if (existingCal.get(java.util.Calendar.YEAR) == entryYear &&
                                existingCal.get(java.util.Calendar.MONTH) == entryMonth &&
                                existingCal.get(java.util.Calendar.DAY_OF_MONTH) == entryDay) {
                                // Duplicate found - entry already exists for this customer on this day
                                listener.onError("Delivery already marked for this customer today");
                                return;
                            }
                        }
                    }
                    
                    // No duplicate - proceed with transaction
                    DocumentReference customerRef = customers().document(customerId);
                    
                    db.runTransaction(transaction -> {
                        // Read customer document
                        DocumentSnapshot customerSnapshot = transaction.get(customerRef);
                        
                        if (!customerSnapshot.exists()) {
                            throw new RuntimeException("Customer not found");
                        }
                        
                        // Get current lent amount (default to 0 if not set)
                        Double currentLent = customerSnapshot.getDouble("lentAmount");
                        if (currentLent == null) {
                            currentLent = 0.0;
                        }
                        
                        // Calculate new lent amount
                        double newLent = currentLent + deliveryCost;
                        
                        // Update customer's lent amount
                        transaction.update(customerRef, "lentAmount", newLent);
                        
                        // Add service entry
                        DocumentReference entryRef = db.collection("serviceEntries").document();
                        transaction.set(entryRef, entry);
                        
                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        listener.onSuccess();
                    }).addOnFailureListener(e -> {
                        listener.onError(e.getMessage());
                    });
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    // ========== Bill Methods ==========
    
    /**
     * Listener interface for loading bills.
     */
    public interface OnBillsLoadedListener {
        void onBillsLoaded(List<Bill> bills);
        void onError(String error);
    }
    
    /**
     * Gets bills for a provider in a specific month and year.
     * 
     * @param providerId The provider's ID
     * @param month The month (0-11, January=0)
     * @param year The year
     * @param listener Callback for results
     */
    public void getBillsByProviderAndMonth(String providerId, int month, int year,
                                            OnBillsLoadedListener listener) {
        db.collection("bills")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("month", month)
                .whereEqualTo("year", year)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Bill> bills = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Bill bill = doc.toObject(Bill.class);
                        if (bill != null) {
                            bill.setId(doc.getId());
                            bills.add(bill);
                        }
                    }
                    listener.onBillsLoaded(bills);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Gets all bills for a specific customer.
     * Filters in memory for month/year on the caller side if needed.
     *
     * @param customerId Customer ID
     * @param listener Callback for results
     */
    public void getBillsByCustomer(String customerId, OnBillsLoadedListener listener) {
        db.collection("bills")
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Bill> bills = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Bill bill = doc.toObject(Bill.class);
                        if (bill != null) {
                            bill.setId(doc.getId());
                            bills.add(bill);
                        }
                    }
                    listener.onBillsLoaded(bills);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Saves or updates a bill.
     * 
     * @param bill The bill to save
     * @param listener Callback for completion
     */
    public void saveBill(Bill bill, OnSaveCompleteListener listener) {
        if (bill.getId() != null && !bill.getId().isEmpty()) {
            // Update existing bill
            db.collection("bills")
                    .document(bill.getId())
                    .set(bill)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            // Create new bill
            db.collection("bills")
                    .add(bill)
                    .addOnSuccessListener(documentReference -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }
    }
    
    /**
     * Gets a single bill by ID.
     * 
     * @param billId The bill's ID
     * @param listener Callback for results
     */
    public void getBillById(String billId, OnBillLoadedListener listener) {
        db.collection("bills")
                .document(billId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Bill bill = documentSnapshot.toObject(Bill.class);
                    if (bill != null) {
                        bill.setId(documentSnapshot.getId());
                        listener.onBillLoaded(bill);
                    } else {
                        listener.onError("Bill not found");
                    }
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Listener interface for loading a single bill.
     */
    public interface OnBillLoadedListener {
        void onBillLoaded(Bill bill);
        void onError(String error);
    }
    
    // ========== Payment Methods ==========
    
    /**
     * Saves or updates a payment.
     * 
     * @param payment The payment to save
     * @param listener Callback for completion
     */
    public void savePayment(Payment payment, OnSaveCompleteListener listener) {
        if (payment.getId() != null && !payment.getId().isEmpty()) {
            // Update existing payment
            db.collection("payments")
                    .document(payment.getId())
                    .set(payment)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            // Create new payment
            db.collection("payments")
                    .add(payment)
                    .addOnSuccessListener(documentReference -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }
    }
    
    /**
     * Gets all payments for a specific bill.
     * 
     * @param billId The bill's ID
     * @param listener Callback for results
     */
    public void getPaymentsByBill(String billId, OnPaymentsLoadedListener listener) {
        db.collection("payments")
                .whereEqualTo("billId", billId)
                .orderBy("paymentDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Payment> payments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            payment.setId(doc.getId());
                            payments.add(payment);
                        }
                    }
                    listener.onPaymentsLoaded(payments);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Gets all payments for a specific customer.
     */
    public void getPaymentsByCustomer(String customerId, OnPaymentsLoadedListener listener) {
        db.collection("payments")
                .whereEqualTo("customerId", customerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Payment> payments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            payment.setId(doc.getId());
                            payments.add(payment);
                        }
                    }
                    listener.onPaymentsLoaded(payments);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Gets payments for a provider within a date range.
     */
    public void getPaymentsByProviderAndDate(String providerId, Timestamp startDate, Timestamp endDate,
                                             OnPaymentsLoadedListener listener) {
        final long startMillis = startDate != null ? startDate.toDate().getTime() : Long.MIN_VALUE;
        final long endMillis = endDate != null ? endDate.toDate().getTime() : Long.MAX_VALUE;

        // Note: Using single where clause to avoid composite index requirement
        // Filter by date in code instead
        db.collection("payments")
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Payment> payments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            payment.setId(doc.getId());
                            // Filter by date in code
                            Timestamp paymentDate = payment.getPaymentDate();
                            if (paymentDate != null) {
                                long paymentMillis = paymentDate.toDate().getTime();
                                if (paymentMillis < startMillis || paymentMillis >= endMillis) {
                                    continue;
                                }
                                payments.add(payment);
                            } else {
                                Log.w("FirestoreRepository",
                                    "Skipping payment with null date: " + doc.getId());
                            }
                        }
                    }
                    listener.onPaymentsLoaded(payments);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Gets all payments for a provider.
     */
    public void getPaymentsByProvider(String providerId, OnPaymentsLoadedListener listener) {
        db.collection("payments")
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Payment> payments = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            payment.setId(doc.getId());
                            payments.add(payment);
                        }
                    }
                    listener.onPaymentsLoaded(payments);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Gets all bills for a provider.
     */
    public void getBillsByProvider(String providerId, OnBillsLoadedListener listener) {
        db.collection("bills")
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Bill> bills = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Bill bill = doc.toObject(Bill.class);
                        if (bill != null) {
                            bill.setId(doc.getId());
                            bills.add(bill);
                        }
                    }
                    listener.onBillsLoaded(bills);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    private boolean isMissingIndexError(Exception e) {
        if (!(e instanceof FirebaseFirestoreException)) {
            return false;
        }
        FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
        if (firestoreException.getCode() != FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
            return false;
        }
        String message = firestoreException.getMessage();
        return message != null && message.toLowerCase().contains("index");
    }
    
    /**
     * Listener interface for loading payments.
     */
    public interface OnPaymentsLoadedListener {
        void onPaymentsLoaded(List<Payment> payments);
        void onError(String error);
    }
}
