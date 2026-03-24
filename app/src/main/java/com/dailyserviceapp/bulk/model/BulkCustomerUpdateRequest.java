package com.dailyserviceapp.bulk.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Request payload for updating the status (e.g. Active, Vacation) of multiple customers.
 */
public class BulkCustomerUpdateRequest extends BulkOperationRequest {

    // The new status value to apply to all selected customers
    protected String targetStatus;

    public BulkCustomerUpdateRequest() {
        super();
    }

    public BulkCustomerUpdateRequest(@Nullable String providerId,
                                     @Nullable List<String> customerIds,
                                     @Nullable String targetStatus) {
        super(providerId, customerIds);
        this.targetStatus = targetStatus;
    }

    /**
     * Returns target status for selected customers.
     */
    @Nullable
    public String getTargetStatus() {
        return targetStatus;
    }

    /**
     * Sets target status for selected customers.
     */
    public void setTargetStatus(@Nullable String targetStatus) {
        this.targetStatus = targetStatus;
    }

    /**
     * Creates a new builder for bulk customer update request.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    @Override
    public String toString() {
        return "BulkCustomerUpdateRequest{" +
                "providerId='" + providerId + '\'' +
                ", customerIds=" + customerIds +
                ", targetStatus='" + targetStatus + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BulkCustomerUpdateRequest)) return false;
        if (!super.equals(o)) return false;
        BulkCustomerUpdateRequest that = (BulkCustomerUpdateRequest) o;
        return Objects.equals(targetStatus, that.targetStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetStatus);
    }

    public static final class Builder {
        private String providerId;
        private List<String> customerIds;
        private String targetStatus;

        private Builder() {
        }

        /**
         * Sets provider id for builder.
         */
        @NonNull
        public Builder providerId(@Nullable String providerId) {
            this.providerId = providerId;
            return this;
        }

        /**
         * Sets selected customer ids for builder.
         */
        @NonNull
        public Builder customerIds(@Nullable List<String> customerIds) {
            this.customerIds = customerIds;
            return this;
        }

        /**
         * Sets target status for builder.
         */
        @NonNull
        public Builder targetStatus(@Nullable String targetStatus) {
            this.targetStatus = targetStatus;
            return this;
        }

        /**
         * Builds immutable request instance from builder values.
         */
        @NonNull
        public BulkCustomerUpdateRequest build() {
            return new BulkCustomerUpdateRequest(providerId, customerIds, targetStatus);
        }
    }
}
