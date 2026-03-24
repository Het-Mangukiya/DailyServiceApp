package com.dailyserviceapp.data.repository;

import com.dailyserviceapp.data.models.Bill;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for all bill operations.
 * Extracted from the monolithic FirestoreRepository as part of Phase 3 decoupling.
 */
@Singleton
public class BillRepository {

    private static final String COLLECTION = "bills";
    private final FirebaseFirestore db;

    // ── Listener Interfaces ──

    public interface OnBillsLoadedListener {
        void onBillsLoaded(List<Bill> bills);
        void onError(String error);
    }

    public interface OnBillLoadedListener {
        void onBillLoaded(Bill bill);
        void onError(String error);
    }

    public interface OnSaveCompleteListener {
        void onSuccess();
        void onError(String error);
    }

    // ── Constructor ──

    @Inject
    public BillRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // ── Queries ──

    public void getBillsByProviderAndMonth(String providerId, int month, int year,
                                            OnBillsLoadedListener listener) {
        db.collection(COLLECTION)
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

    public void getBillsByCustomer(String customerId, OnBillsLoadedListener listener) {
        scopedCustomerQuery(customerId)
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

    public void getBillsByProvider(String providerId, OnBillsLoadedListener listener) {
        db.collection(COLLECTION)
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

    public void getBillById(String billId, OnBillLoadedListener listener) {
        db.collection(COLLECTION)
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

    // ── Writes ──

    public void saveBill(Bill bill, OnSaveCompleteListener listener) {
        if (bill.getId() != null && !bill.getId().isEmpty()) {
            db.collection(COLLECTION).document(bill.getId()).set(bill)
                    .addOnSuccessListener(aVoid -> listener.onSuccess())
                    .addOnFailureListener(e -> listener.onError(e.getMessage()));
        } else {
            db.collection(COLLECTION).add(bill)
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

    private String getCurrentAuthUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return "";
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }
}
