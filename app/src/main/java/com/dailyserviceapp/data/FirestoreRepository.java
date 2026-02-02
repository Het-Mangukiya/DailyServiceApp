package com.dailyserviceapp.data;

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

    /**
     * Mark a customer's payment for a specific month as paid and persist the payment status.
     *
     * Writes a PaymentStatus document (with paid=true, the paid amount, and current timestamp) to
     * the customer's "payments" subcollection keyed by the given monthKey, replacing any existing document.
     *
     * @param customerId the ID of the customer document to update
     * @param monthKey   a month identifier used as the payment document ID (e.g., "202601" for Jan 2026)
     * @param paidAmount the amount recorded as paid for the month
     * @param onSuccess  callback invoked when the write completes successfully
     * @param onFailure  callback invoked if the write fails
     */
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
     * Deletes the Firestore customer document with the given ID.
     *
     * Invokes the provided listener's onSuccess() when the delete completes successfully,
     * or onError(String) with the failure message if the delete fails.
     *
     * @param customerId the ID of the customer document to delete
     * @param listener callback invoked on success or error
     */
    public void deleteCustomer(String customerId, OnSaveCompleteListener listener) {
        customers()
                .document(customerId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess())
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
     * Retrieve active customers belonging to the specified provider.
     *
     * Results are delivered to the provided listener: onCustomersLoaded receives the list of matching Customer objects (with IDs populated), and onError receives an error message if the query fails.
     *
     * @param providerId the provider's document ID to filter customers by
     * @param listener callback that receives the loaded customers or an error message
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
     * Subscribe to real-time updates of ACTIVE customers for the given provider.
     *
     * The provided listener is invoked with the latest list of customers whenever the data changes,
     * or with an error message if the listener encounters an error.
     *
     * @param providerId the provider's ID
     * @param listener callback invoked on updates or errors
     * @return a ListenerRegistration that can be used to remove the snapshot listener
     */
    public ListenerRegistration listenToCustomers(String providerId, OnCustomersLoadedListener listener) {
        return customers()
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("status", "ACTIVE")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        listener.onError(error.getMessage());
                        return;
                    }
                    
                    if (querySnapshot != null) {
                        List<Customer> customerList = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Customer customer = doc.toObject(Customer.class);
                            if (customer != null) {
                                customer.setId(doc.getId());
                                customerList.add(customer);
                            }
                        }
                        listener.onCustomersLoaded(customerList);
                    }
                });
    }
    
    /**
         * Retrieve service entries for a provider that fall within the inclusive date range.
         *
         * <p>Performs a provider-scoped query and filters results in memory to include entries whose
         * date is greater than or equal to {@code startDate} and less than or equal to {@code endDate},
         * avoiding the need for composite Firestore indexes.</p>
         *
         * @param providerId the provider's document ID to filter service entries
         * @param startDate the start of the date range (inclusive)
         * @param endDate the end of the date range (inclusive)
         * @param listener callback that receives the matching entries via {@code onServiceEntriesLoaded}
         *                 or an error message via {@code onError}
         */
    public void getServiceEntriesByProviderAndDate(String providerId, Timestamp startDate, 
                                                    Timestamp endDate, OnServiceEntriesLoadedListener listener) {
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
                            if (entryDate != null && 
                                !entryDate.toDate().before(startDate.toDate()) && 
                                !entryDate.toDate().after(endDate.toDate())) {
                                entries.add(entry);
                            }
                        }
                    }
                    listener.onServiceEntriesLoaded(entries);
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }
    
    /**
     * Persists a ServiceEntry to Firestore, updating an existing document when the entry has an ID or creating a new document otherwise.
     *
     * If the entry contains a non-empty ID the corresponding document in the "serviceEntries" collection is overwritten; if the ID is absent or empty a new document is created. Completion and error results are reported through the provided listener.
     *
     * @param entry    the ServiceEntry to save or update
     * @param listener callback invoked on success or with an error message on failure
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
     * Save a service entry and atomically update the customer's lent amount.
     *
     * Performs a duplicate check to prevent more than one delivery for the same customer on the same day,
     * and, if none is found, runs a Firestore transaction that updates the customer's lentAmount and creates the service entry together.
     *
     * @param entry       the ServiceEntry to persist
     * @param customerId  the ID of the customer whose lent amount will be updated
     * @param deliveryCost the amount to add to the customer's lentAmount (e.g., rate × quantity)
     * @param listener    callback invoked on success or with an error message on failure
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
        /**
 * Called when the repository has retrieved the list of bills.
 *
 * @param bills the retrieved list of Bill objects; may be empty if no bills were found
 */
void onBillsLoaded(List<Bill> bills);
        /**
 * Called when an operation fails.
 *
 * @param error a human-readable message describing the failure
 */
void onError(String error);
    }
    
    /**
     * Retrieves all bills for the given provider within the specified month and year and delivers them to the listener.
     *
     * The month parameter uses 0-11 indexing (January = 0).
     *
     * @param providerId the provider's identifier
     * @param month the month index (0-11, January = 0)
     * @param year the calendar year
     * @param listener callback invoked with the loaded bills or an error message
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
     * Creates a new Bill document or updates an existing one in the "bills" collection.
     *
     * If the provided Bill has a non-empty id, the corresponding document is overwritten; otherwise a new document is created.
     *
     * @param bill     the Bill to persist; when `bill.getId()` is non-empty the existing document is updated
     * @param listener callback invoked on successful completion or with an error message on failure
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
         * Retrieve a bill document by its ID and deliver the result via the provided listener.
         *
         * If the document exists it is converted to a Bill (its ID is set) and delivered via
         * OnBillLoadedListener.onBillLoaded. If the document does not exist or a fetch error
         * occurs, OnBillLoadedListener.onError is invoked with an error message.
         *
         * @param billId  the Firestore document ID of the bill
         * @param listener callback receiving the loaded Bill or an error message
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
        /**
 * Called when a bill has been retrieved successfully.
 *
 * @param bill the retrieved Bill matching the requested identifier
 */
void onBillLoaded(Bill bill);
        /**
 * Called when an operation fails.
 *
 * @param error a human-readable message describing the failure
 */
void onError(String error);
    }
    
    // ========== Payment Methods ==========
    
    /**
     * Persists the given payment in Firestore by creating a new document or updating an existing one.
     *
     * <p>If the payment has a non-empty `id`, the corresponding document is overwritten; otherwise a
     * new document is created.</p>
     *
     * @param payment  the Payment to persist; its `id` (if present) selects update mode
     * @param listener callback invoked on successful completion or with an error message on failure
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
     * Loads all payments associated with the specified bill and notifies the listener.
     *
     * Results are delivered to the listener ordered by `paymentDate` in descending order.
     *
     * @param billId   the ID of the bill whose payments should be retrieved
     * @param listener callback that receives the list of payments or an error message
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
     * Listener interface for loading payments.
     */
    public interface OnPaymentsLoadedListener {
        /**
 * Called when payments for a bill have been loaded.
 *
 * @param payments the list of payments associated with the bill; items are ordered by payment date in descending order (most recent first). This list may be empty if there are no payments.
 */
void onPaymentsLoaded(List<Payment> payments);
        /**
 * Called when an operation fails.
 *
 * @param error a human-readable message describing the failure
 */
void onError(String error);
    }
}
