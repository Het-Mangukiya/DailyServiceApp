package com.dailyserviceapp.data.repository;

import com.dailyserviceapp.data.models.Customer;
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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CustomerRepository {

    private final FirebaseFirestore db;

    @Inject
    public CustomerRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public interface OnCustomersLoadedListener {
        void onCustomersLoaded(List<Customer> customers);
        void onError(String error);
    }

    public interface OnSaveCompleteListener {
        void onSuccess();
        void onError(String error);
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
                                    deleteDocuments(query, listener);
                                } else {
                                    listener.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> listener.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

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
}
