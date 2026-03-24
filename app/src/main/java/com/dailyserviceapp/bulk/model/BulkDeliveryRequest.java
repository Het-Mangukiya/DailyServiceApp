package com.dailyserviceapp.bulk.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Request payload for marking multiple deliveries at once.
 */
public class BulkDeliveryRequest extends BulkOperationRequest {

    // Typically bulk deliveries apply to a single date/timestamp.
    protected long targetDateMillis;

    public BulkDeliveryRequest() {
        super();
    }

    public BulkDeliveryRequest(@Nullable String providerId,
                               @Nullable List<String> customerIds,
                               long targetDateMillis) {
        super(providerId, customerIds);
        this.targetDateMillis = targetDateMillis;
    }

    /**
     * Returns target date in epoch millis.
     */
    public long getTargetDateMillis() {
        return targetDateMillis;
    }

    /**
     * Sets target date in epoch millis.
     */
    public void setTargetDateMillis(long targetDateMillis) {
        this.targetDateMillis = targetDateMillis;
    }

    /**
     * Creates a new builder for bulk delivery request.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    @Override
    public String toString() {
        return "BulkDeliveryRequest{" +
                "providerId='" + providerId + '\'' +
                ", customerIds=" + customerIds +
                ", targetDateMillis=" + targetDateMillis +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BulkDeliveryRequest)) return false;
        if (!super.equals(o)) return false;
        BulkDeliveryRequest that = (BulkDeliveryRequest) o;
        return targetDateMillis == that.targetDateMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), targetDateMillis);
    }

    public static final class Builder {
        private String providerId;
        private List<String> customerIds;
        private long targetDateMillis;

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
         * Sets target date in epoch millis for builder.
         */
        @NonNull
        public Builder targetDateMillis(long targetDateMillis) {
            this.targetDateMillis = targetDateMillis;
            return this;
        }

        /**
         * Builds immutable request instance from builder values.
         */
        @NonNull
        public BulkDeliveryRequest build() {
            return new BulkDeliveryRequest(providerId, customerIds, targetDateMillis);
        }
    }
}
