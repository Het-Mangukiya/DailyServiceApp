package com.dailyserviceapp.bulk.repository;

import com.dailyserviceapp.bulk.model.BulkCustomerUpdateRequest;
import com.dailyserviceapp.bulk.model.BulkDeliveryRequest;
import com.google.android.gms.tasks.Task;

/**
 * Interface definition for handling bulk operations.
 * Must be implemented using Firestore WriteBatch to ensure atomicity
 * and to minimize network calls.
 */
public interface BulkOperationRepository {

    /**
     * Executes a batch of delivery operations (e.g., marking multiple customers as delivered).
     * 
     * @param request The bulk delivery payload containing provider ID and selected customers.
     * @return A Task representing the asynchronous batch operation.
     */
    Task<Void> executeBulkDeliveries(BulkDeliveryRequest request);

    /**
     * Executes a batch update on customer statuses (e.g., setting multiple to Vacation/Active).
     * 
     * @param request The bulk update payload containing target status and selected customers.
     * @return A Task representing the asynchronous batch operation.
     */
    Task<Void> executeBulkCustomerUpdates(BulkCustomerUpdateRequest request);
}
