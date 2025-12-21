package com.dailyserviceapp.data;

import androidx.annotation.NonNull;

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
import java.util.List;

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
}
