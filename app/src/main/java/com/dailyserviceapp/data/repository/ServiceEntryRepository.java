package com.dailyserviceapp.data.repository;

import android.util.Log;

import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for all service entry (delivery) operations.
 * Extracted from the monolithic FirestoreRepository as part of Phase 3 decoupling.
 */
@Singleton
public class ServiceEntryRepository {

    private static final String TAG = "ServiceEntryRepo";
    private static final String COLLECTION = "serviceEntries";

    private final FirebaseFirestore db;

    // ── Listener Interfaces ──

    public interface OnServiceEntriesLoadedListener {
        void onServiceEntriesLoaded(List<ServiceEntry> entries);
        void onError(String error);
    }

    public interface OnSaveCompleteListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnBatchSaveResultListener {
        void onSuccess(int savedCount, int skippedCount);
        void onError(String error);
    }

    // ── Value Object ──

    public static class DeliveryWriteRequest {
        public final String customerId;
        public final double quantity;
        public final double rate;
        public final double amount;

        public DeliveryWriteRequest(String customerId, double quantity, double rate, double amount) {
            this.customerId = customerId;
            this.quantity = quantity;
            this.rate = rate;
            this.amount = amount;
        }
    }

    // ── Constructor ──

    @Inject
    public ServiceEntryRepository(FirebaseFirestore db) {
        this.db = db;
    }

    private CollectionReference customers() {
        return db.collection("customers");
    }

    // ── Queries ──

    public void getServiceEntriesByProviderAndDate(String providerId, Timestamp startDate,
                                                    Timestamp endDate, OnServiceEntriesLoadedListener listener) {
        final long startMillis = startDate != null ? startDate.toDate().getTime() : Long.MIN_VALUE;
        final long endMillis = endDate != null ? endDate.toDate().getTime() : Long.MAX_VALUE;

        Query optimizedQuery = db.collection(COLLECTION).whereEqualTo("providerId", providerId);
        if (startDate != null) {
            optimizedQuery = optimizedQuery.whereGreaterThanOrEqualTo("date", startDate);
        }
        if (endDate != null) {
            optimizedQuery = optimizedQuery.whereLessThan("date", endDate);
        }

        optimizedQuery.get()
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
                        listener.onError(e.getMessage());
                        return;
                    }
                    db.collection(COLLECTION)
                            .whereEqualTo("providerId", providerId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<ServiceEntry> entries = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    ServiceEntry entry = doc.toObject(ServiceEntry.class);
                                    if (entry == null || entry.getDate() == null) continue;
                                    long entryMillis = entry.getDate().toDate().getTime();
                                    if (entryMillis < startMillis || entryMillis >= endMillis) continue;
                                    entry.setId(doc.getId());
                                    entries.add(entry);
                                }
                                listener.onServiceEntriesLoaded(entries);
                            })
                            .addOnFailureListener(inner -> listener.onError(inner.getMessage()));
                });
    }

    public void getDeliveredServiceEntriesByProvider(String providerId, OnServiceEntriesLoadedListener listener) {
        db.collection(COLLECTION)
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
                        Log.e(TAG, "Failed to load delivered entries for provider: " + providerId, e);
                        listener.onError(e.getMessage());
                        return;
                    }
                    Log.w(TAG, "Missing index for delivered entries query; using in-memory filter", e);
                    db.collection(COLLECTION)
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

    public void getDeliveredServiceEntriesByProviderInRange(String providerId, Timestamp startDate,
                                                            Timestamp endDate, OnServiceEntriesLoadedListener listener) {
        db.collection(COLLECTION)
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
                        Log.e(TAG, "Failed to load delivered entries in range for provider: " + providerId, e);
                        listener.onError(e.getMessage());
                        return;
                    }
                    Log.w(TAG, "Missing index for delivered range query; using in-memory filter", e);
                    final long startMillis = startDate != null ? startDate.toDate().getTime() : Long.MIN_VALUE;
                    final long endMillis = endDate != null ? endDate.toDate().getTime() : Long.MAX_VALUE;
                    db.collection(COLLECTION)
                            .whereEqualTo("providerId", providerId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<ServiceEntry> entries = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    ServiceEntry entry = doc.toObject(ServiceEntry.class);
                                    if (entry == null || !entry.isDelivered() || entry.getDate() == null) continue;
                                    long entryMillis = entry.getDate().toDate().getTime();
                                    if (entryMillis < startMillis || entryMillis >= endMillis) continue;
                                    entry.setId(doc.getId());
                                    entries.add(entry);
                                }
                                listener.onServiceEntriesLoaded(entries);
                            })
                            .addOnFailureListener(inner -> listener.onError(inner.getMessage()));
                });
    }

    public void getServiceEntriesByCustomerAndDate(String customerId, Timestamp startDate,
                                                   Timestamp endDate, OnServiceEntriesLoadedListener listener) {
        final long startMillis = startDate != null ? startDate.toDate().getTime() : Long.MIN_VALUE;
        final long endMillis = endDate != null ? endDate.toDate().getTime() : Long.MAX_VALUE;
        Query optimizedQuery = scopedCustomerQuery(customerId);
        if (startDate != null) {
            optimizedQuery = optimizedQuery.whereGreaterThanOrEqualTo("date", startDate);
        }
        if (endDate != null) {
            optimizedQuery = optimizedQuery.whereLessThan("date", endDate);
        }

        optimizedQuery.get()
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
                        listener.onError(e.getMessage());
                        return;
                    }
                    scopedCustomerQuery(customerId)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<ServiceEntry> entries = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    ServiceEntry entry = doc.toObject(ServiceEntry.class);
                                    if (entry == null || entry.getDate() == null) continue;
                                    long entryMillis = entry.getDate().toDate().getTime();
                                    if (entryMillis < startMillis || entryMillis >= endMillis) continue;
                                    entry.setId(doc.getId());
                                    entries.add(entry);
                                }
                                listener.onServiceEntriesLoaded(entries);
                            })
                            .addOnFailureListener(inner -> listener.onError(inner.getMessage()));
                });
    }

    // ── Writes ──

    public void saveServiceEntry(ServiceEntry entry, OnSaveCompleteListener listener) {
        if (entry.getId() != null && !entry.getId().isEmpty()) {
            db.collection(COLLECTION).document(entry.getId()).set(entry)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            db.collection(COLLECTION).add(entry)
                    .addOnSuccessListener(documentReference -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }
    }

    public void saveServiceEntryWithTransaction(ServiceEntry entry, String customerId,
                                                 double deliveryCost, OnSaveCompleteListener listener) {
        if (entry == null) {
            listener.onError("Service entry is required");
            return;
        }

        String normalizedProviderId = safeTrim(entry.getProviderId());
        if (normalizedProviderId.isEmpty()) {
            normalizedProviderId = safeTrim(getCurrentAuthUid());
            entry.setProviderId(normalizedProviderId);
        }

        List<DeliveryWriteRequest> requests = new ArrayList<>();
        requests.add(new DeliveryWriteRequest(customerId, entry.getQuantity(), entry.getRate(), deliveryCost));

        saveServiceEntriesBatchWithTransaction(
                normalizedProviderId, entry.getDate(), requests,
                new OnBatchSaveResultListener() {
                    @Override
                    public void onSuccess(int savedCount, int skippedCount) {
                        if (savedCount > 0) { listener.onSuccess(); return; }
                        listener.onError("Delivery already marked for this customer today");
                    }
                    @Override
                    public void onError(String error) { listener.onError(error); }
                }
        );
    }

    public void saveServiceEntriesBatchWithTransaction(String providerId, Timestamp targetDate,
                                                       List<DeliveryWriteRequest> requests,
                                                       OnBatchSaveResultListener listener) {
        if (listener == null) return;
        final String normalizedProviderId = safeTrim(providerId);
        if (normalizedProviderId.isEmpty()) { listener.onError("Provider ID is required"); return; }
        if (targetDate == null) { listener.onError("Delivery date is required"); return; }
        if (requests == null || requests.isEmpty()) { listener.onError("No deliveries selected"); return; }

        final List<DeliveryWriteRequest> validRequests = new ArrayList<>();
        for (DeliveryWriteRequest request : requests) {
            if (request == null) continue;
            String cId = safeTrim(request.customerId);
            if (cId.isEmpty()) continue;
            if (request.quantity <= 0 || request.amount < 0) continue;
            validRequests.add(new DeliveryWriteRequest(cId, request.quantity, request.rate, request.amount));
        }
        if (validRequests.isEmpty()) { listener.onError("No valid deliveries to save"); return; }

        loadExistingDeliveredCustomerIdsForDate(normalizedProviderId, targetDate,
                new OnExistingCustomerIdsLoadedListener() {
                    @Override
                    public void onLoaded(Set<String> deliveredCustomerIds) {
                        List<DeliveryWriteRequest> toPersist = new ArrayList<>();
                        Map<String, Double> amountByCustomer = new HashMap<>();
                        Set<String> seenCustomerIds = new HashSet<>();
                        int skippedCount = 0;

                        for (DeliveryWriteRequest request : validRequests) {
                            if (deliveredCustomerIds.contains(request.customerId)) { skippedCount++; continue; }
                            if (!seenCustomerIds.add(request.customerId)) { skippedCount++; continue; }
                            toPersist.add(request);
                            double running = amountByCustomer.containsKey(request.customerId)
                                    ? amountByCustomer.get(request.customerId) : 0.0;
                            amountByCustomer.put(request.customerId, running + request.amount);
                        }

                        if (toPersist.isEmpty()) { listener.onSuccess(0, skippedCount); return; }
                        if (toPersist.size() > 200) {
                            listener.onError("Too many deliveries selected. Please save up to 200 at once.");
                            return;
                        }

                        final int finalSkippedCount = skippedCount;
                        db.runTransaction(transaction -> {
                            Map<String, Double> updatedLent = new HashMap<>();
                            Map<String, DocumentReference> entryRefs = new HashMap<>();

                            for (Map.Entry<String, Double> ae : amountByCustomer.entrySet()) {
                                DocumentReference customerRef = customers().document(ae.getKey());
                                DocumentSnapshot cs = transaction.get(customerRef);
                                if (!cs.exists()) throw new RuntimeException("Customer not found");
                                Double currentLent = cs.getDouble("lentAmount");
                                if (currentLent == null) currentLent = 0.0;
                                updatedLent.put(ae.getKey(), currentLent + ae.getValue());
                            }

                            for (DeliveryWriteRequest request : toPersist) {
                                String entryDocId = buildServiceEntryDocumentId(normalizedProviderId, request.customerId, targetDate);
                                DocumentReference entryRef = db.collection(COLLECTION).document(entryDocId);
                                entryRefs.put(request.customerId, entryRef);
                                DocumentSnapshot existing = transaction.get(entryRef);
                                if (existing != null && existing.exists())
                                    throw new IllegalStateException("Delivery already marked for one or more customers today");
                            }

                            for (Map.Entry<String, Double> le : updatedLent.entrySet()) {
                                transaction.update(customers().document(le.getKey()), "lentAmount", le.getValue());
                            }

                            Timestamp now = Timestamp.now();
                            for (DeliveryWriteRequest request : toPersist) {
                                ServiceEntry entry = new ServiceEntry(normalizedProviderId, request.customerId, targetDate, request.quantity, true);
                                entry.setRate(request.rate);
                                entry.setCreatedAt(now);
                                entry.setUpdatedAt(now);
                                DocumentReference entryRef = entryRefs.get(request.customerId);
                                if (entryRef == null) throw new IllegalStateException("Failed to create entry reference");
                                transaction.set(entryRef, entry);
                            }
                            return null;
                        }).addOnSuccessListener(aVoid -> listener.onSuccess(toPersist.size(), finalSkippedCount))
                          .addOnFailureListener(e -> listener.onError(e.getMessage()));
                    }

                    @Override
                    public void onError(String error) { listener.onError(error); }
                });
    }

    // ── Private Helpers ──

    private interface OnExistingCustomerIdsLoadedListener {
        void onLoaded(Set<String> customerIds);
        void onError(String error);
    }

    private void loadExistingDeliveredCustomerIdsForDate(String providerId, Timestamp targetDate,
                                                         OnExistingCustomerIdsLoadedListener listener) {
        final Calendar startCal = Calendar.getInstance();
        startCal.setTime(targetDate.toDate());
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        final Calendar endCal = (Calendar) startCal.clone();
        endCal.add(Calendar.DAY_OF_YEAR, 1);

        final Timestamp startOfDay = new Timestamp(startCal.getTime());
        final Timestamp endExclusive = new Timestamp(endCal.getTime());
        final long startMillis = startOfDay.toDate().getTime();
        final long endMillis = endExclusive.toDate().getTime();

        db.collection(COLLECTION)
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("delivered", true)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThan("date", endExclusive)
                .get()
                .addOnSuccessListener(snapshot -> listener.onLoaded(collectDeliveredCustomerIds(snapshot, startMillis, endMillis, false)))
                .addOnFailureListener(e -> {
                    if (!isMissingIndexError(e)) { listener.onError(e.getMessage()); return; }
                    db.collection(COLLECTION).whereEqualTo("providerId", providerId)
                            .whereGreaterThanOrEqualTo("date", startOfDay).whereLessThan("date", endExclusive)
                            .get()
                            .addOnSuccessListener(snapshot -> listener.onLoaded(collectDeliveredCustomerIds(snapshot, startMillis, endMillis, false)))
                            .addOnFailureListener(inner -> {
                                if (!isMissingIndexError(inner)) { listener.onError(inner.getMessage()); return; }
                                db.collection(COLLECTION).whereEqualTo("providerId", providerId).get()
                                        .addOnSuccessListener(snapshot -> listener.onLoaded(collectDeliveredCustomerIds(snapshot, startMillis, endMillis, true)))
                                        .addOnFailureListener(last -> listener.onError(last.getMessage()));
                            });
                });
    }

    private Set<String> collectDeliveredCustomerIds(QuerySnapshot snapshot, long startMillis, long endMillis,
                                                    boolean enforceDateFilterInMemory) {
        Set<String> deliveredCustomerIds = new HashSet<>();
        if (snapshot == null) return deliveredCustomerIds;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Boolean delivered = doc.getBoolean("delivered");
            if (delivered != null && !delivered) continue;
            if (enforceDateFilterInMemory) {
                Timestamp date = doc.getTimestamp("date");
                if (date == null) continue;
                long entryMillis = date.toDate().getTime();
                if (entryMillis < startMillis || entryMillis >= endMillis) continue;
            }
            String customerId = safeTrim(doc.getString("customerId"));
            if (!customerId.isEmpty()) deliveredCustomerIds.add(customerId);
        }
        return deliveredCustomerIds;
    }

    private String buildServiceEntryDocumentId(String providerId, String customerId, Timestamp targetDate) {
        return safeTrim(providerId) + "_" + safeTrim(customerId) + "_" + buildDayKey(targetDate);
    }

    private String buildDayKey(Timestamp timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp.toDate());
        return String.format(Locale.US, "%04d%02d%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    private Query scopedCustomerQuery(String customerId) {
        Query query = db.collection(COLLECTION).whereEqualTo("customerId", customerId);
        String authUid = safeTrim(getCurrentAuthUid());
        if (!authUid.isEmpty() && !authUid.equals(customerId)) {
            query = query.whereEqualTo("providerId", authUid);
        }
        return query;
    }

    private boolean isMissingIndexError(Exception e) {
        if (!(e instanceof FirebaseFirestoreException)) return false;
        FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
        if (fe.getCode() != FirebaseFirestoreException.Code.FAILED_PRECONDITION) return false;
        String message = fe.getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains("index");
    }

    private String getCurrentAuthUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return "";
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
