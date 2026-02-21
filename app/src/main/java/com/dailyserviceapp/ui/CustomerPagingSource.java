package com.dailyserviceapp.ui;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;

import com.dailyserviceapp.data.models.Customer;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kotlin.coroutines.Continuation;

/**
 * PagingSource for loading provider customers page-by-page from Firestore.
 */
public class CustomerPagingSource extends PagingSource<DocumentSnapshot, Customer> {

    private final FirebaseFirestore firestore;
    private final String providerId;
    private final int pageSize;
    private final String sortField;

    public CustomerPagingSource(FirebaseFirestore firestore, String providerId, int pageSize, String sortField) {
        this.firestore = firestore;
        this.providerId = providerId;
        this.pageSize = pageSize;
        this.sortField = sortField;
    }

    @Override
    public Object load(@NonNull LoadParams<DocumentSnapshot> params,
                       @NonNull Continuation<? super LoadResult<DocumentSnapshot, Customer>> continuation) {
        try {
            // Defensive guard: never allow blocking Firestore await() on main thread.
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return new LoadResult.Error<>(
                    new IllegalStateException("Paging load attempted on main thread")
                );
            }

            Query query = firestore.collection("customers")
                .whereEqualTo("providerId", providerId)
                .orderBy(sortField, Query.Direction.ASCENDING)
                .limit(Math.max(params.getLoadSize(), pageSize));

            if (params.getKey() != null) {
                query = query.startAfter(params.getKey());
            }

            QuerySnapshot querySnapshot = Tasks.await(query.get(), 20, TimeUnit.SECONDS);
            List<Customer> customers = new ArrayList<>();
            List<DocumentSnapshot> docs = querySnapshot.getDocuments();

            for (DocumentSnapshot doc : docs) {
                String status = doc.getString("status");
                if (status != null && !status.trim().isEmpty() && !"ACTIVE".equalsIgnoreCase(status)) {
                    continue;
                }
                Customer customer = doc.toObject(Customer.class);
                if (customer != null) {
                    customer.setId(doc.getId());
                    customers.add(customer);
                }
            }

            DocumentSnapshot nextKey = docs.isEmpty() ? null : docs.get(docs.size() - 1);
            return new LoadResult.Page<>(customers, null, nextKey);
        } catch (Exception e) {
            return new LoadResult.Error<>(e);
        }
    }

    @Override
    public DocumentSnapshot getRefreshKey(@NonNull PagingState<DocumentSnapshot, Customer> state) {
        return null;
    }
}
