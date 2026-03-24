package com.dailyserviceapp.bulk.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Base abstract class for all bulk operation requests.
 * Contains shared fields required for batch processing.
 */
public abstract class BulkOperationRequest {

    protected String providerId;
    protected List<String> customerIds;

    public BulkOperationRequest() {
    }

    public BulkOperationRequest(@Nullable String providerId, @Nullable List<String> customerIds) {
        this.providerId = providerId;
        this.customerIds = customerIds;
    }

    /**
     * Returns provider id for the bulk request.
     */
    @Nullable
    public String getProviderId() {
        return providerId;
    }

    /**
     * Sets provider id for the bulk request.
     */
    public void setProviderId(@Nullable String providerId) {
        this.providerId = providerId;
    }

    /**
     * Returns selected customer ids for this request.
     */
    @Nullable
    public List<String> getCustomerIds() {
        return customerIds;
    }

    /**
     * Sets selected customer ids for this request.
     */
    public void setCustomerIds(@Nullable List<String> customerIds) {
        this.customerIds = customerIds;
    }

    @NonNull
    @Override
    public String toString() {
        return "BulkOperationRequest{" +
                "providerId='" + providerId + '\'' +
                ", customerIds=" + customerIds +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BulkOperationRequest that = (BulkOperationRequest) o;
        return Objects.equals(providerId, that.providerId)
                && Objects.equals(customerIds, that.customerIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, customerIds);
    }
}
