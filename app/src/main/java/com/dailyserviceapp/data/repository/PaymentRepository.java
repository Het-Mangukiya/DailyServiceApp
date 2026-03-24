package com.dailyserviceapp.data.repository;

import com.dailyserviceapp.data.models.Payment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for all payment operations.
 * Extracted from the monolithic FirestoreRepository as part of Phase 3 decoupling.
 */
@Singleton
public class PaymentRepository {

    private static final String COLLECTION = "payments";
    private final FirebaseFirestore db;

    // ── Listener Interfaces ──

    public interface OnPaymentsLoadedListener {
        void onPaymentsLoaded(List<Payment> payments);
        void onError(String error);
    }

    public interface OnSaveCompleteListener {
        void onSuccess();
        void onError(String error);
    }

    // ── Constructor ──

    @Inject
    public PaymentRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // ── Queries ──

    public void getPaymentsByBill(String billId, OnPaymentsLoadedListener listener) {
        db.collection(COLLECTION)
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

    public void getPaymentsByCustomer(String customerId, OnPaymentsLoadedListener listener) {
        scopedCustomerQuery(customerId)
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

    public void getPaymentsByProviderAndDate(String providerId, Timestamp startDate, Timestamp endDate,
                                             OnPaymentsLoadedListener listener) {
        final long startMillis = startDate != null ? startDate.toDate().getTime() : Long.MIN_VALUE;
        final long endMillis = endDate != null ? endDate.toDate().getTime() : Long.MAX_VALUE;

        Query optimizedQuery = db.collection(COLLECTION).whereEqualTo("providerId", providerId);
        if (startDate != null) {
            optimizedQuery = optimizedQuery.whereGreaterThanOrEqualTo("paymentDate", startDate);
        }
        if (endDate != null) {
            optimizedQuery = optimizedQuery.whereLessThan("paymentDate", endDate);
        }

        optimizedQuery.get()
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
                .addOnFailureListener(e -> {
                    if (!isMissingIndexError(e)) {
                        listener.onError(e.getMessage());
                        return;
                    }
                    db.collection(COLLECTION).whereEqualTo("providerId", providerId).get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<Payment> payments = new ArrayList<>();
                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    Payment payment = doc.toObject(Payment.class);
                                    if (payment == null || payment.getPaymentDate() == null) continue;
                                    long paymentMillis = payment.getPaymentDate().toDate().getTime();
                                    if (paymentMillis < startMillis || paymentMillis >= endMillis) continue;
                                    payment.setId(doc.getId());
                                    payments.add(payment);
                                }
                                listener.onPaymentsLoaded(payments);
                            })
                            .addOnFailureListener(inner -> listener.onError(inner.getMessage()));
                });
    }

    public void getPaymentsByProvider(String providerId, OnPaymentsLoadedListener listener) {
        db.collection(COLLECTION)
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

    // ── Writes ──

    public void savePayment(Payment payment, OnSaveCompleteListener listener) {
        if (payment.getId() != null && !payment.getId().isEmpty()) {
            db.collection(COLLECTION).document(payment.getId()).set(payment)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            db.collection(COLLECTION).add(payment)
                    .addOnSuccessListener(documentReference -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        }
    }

    // ── Private Helpers ──

    private Query scopedCustomerQuery(String customerId) {
        Query query = db.collection(COLLECTION).whereEqualTo("customerId", customerId);
        String authUid = getCurrentAuthUid();
        if (authUid != null && !authUid.isEmpty() && !authUid.equals(customerId)) {
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
}
