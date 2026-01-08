package com.dailyserviceapp.data;

import androidx.annotation.NonNull;

import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for Firestore database operations.
 * Handles CRUD operations for customers, service entries, and payments.
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
                .whereEqualTo("status", "ACTIVE")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Customer> customerList = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Customer customer = doc.toObject(Customer.class);
                        if (customer != null) {
                            customer.setId(doc.getId());
                            customerList.add(customer);
                        }
                    }
                    listener.onCustomersLoaded(customerList);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Gets service entries for a provider within a date range.
     * 
     * @param providerId The provider's ID
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param listener Callback for results
     */
    public void getServiceEntriesByProviderAndDate(String providerId, Timestamp startDate, 
                                                    Timestamp endDate, OnServiceEntriesLoadedListener listener) {
        db.collection("serviceEntries")
                .whereEqualTo("providerId", providerId)
                .orderBy("date")
                .startAt(startDate)
                .endAt(endDate)
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
}
