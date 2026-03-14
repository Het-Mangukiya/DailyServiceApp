package com.dailyserviceapp.billing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter for displaying customer-wise billing ledgers in a RecyclerView.
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-09
 */
public class BillAdapter extends RecyclerView.Adapter<BillAdapter.ViewHolder> {

    /**
     * Listener interface for bill actions.
     */
    public interface OnBillActionListener {
        void onViewDetails(CustomerLedgerSummary summary);
        void onShareBill(CustomerLedgerSummary summary);
    }

    private static final double EPSILON = 0.01;
    private final List<CustomerLedgerSummary> summaries = new ArrayList<>();
    private final OnBillActionListener listener;

    public BillAdapter(OnBillActionListener listener) {
        this.listener = Objects.requireNonNull(listener, "OnBillActionListener is required");
    }

    /**
     * Updates the adapter with customer ledger summaries using DiffUtil for smooth updates.
     * 
     * @param summaryList List of summaries to display
     */
    public void submitData(List<CustomerLedgerSummary> summaryList) {
        List<CustomerLedgerSummary> oldSummaries = new ArrayList<>(summaries);
        
        summaries.clear();
        
        if (summaryList != null) {
            summaries.addAll(summaryList);
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldSummaries.size();
            }

            @Override
            public int getNewListSize() {
                return summaries.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                CustomerLedgerSummary oldSummary = oldSummaries.get(oldItemPosition);
                CustomerLedgerSummary newSummary = summaries.get(newItemPosition);
                String oldId = oldSummary.getCustomerId();
                String newId = newSummary.getCustomerId();
                return Objects.equals(oldId, newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                CustomerLedgerSummary oldSummary = oldSummaries.get(oldItemPosition);
                CustomerLedgerSummary newSummary = summaries.get(newItemPosition);
                
                return Math.abs(oldSummary.getOutstandingAmount() - newSummary.getOutstandingAmount()) <= EPSILON
                    && Math.abs(oldSummary.getTotalPaidAmount() - newSummary.getTotalPaidAmount()) <= EPSILON
                    && oldSummary.getDeliveredEntries() == newSummary.getDeliveredEntries()
                    && safeEquals(oldSummary.getCustomerName(), newSummary.getCustomerName())
                    && sameTimestamp(oldSummary.getPaidTillDate(), newSummary.getPaidTillDate())
                    && sameTimestamp(oldSummary.getDueFromDate(), newSummary.getDueFromDate());
            }
        });
        
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_bill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomerLedgerSummary summary = summaries.get(position);
        String customerName = summary.getCustomerName() != null && !summary.getCustomerName().trim().isEmpty()
            ? summary.getCustomerName() : "Unknown";
        
        // Set customer info
        if (!customerName.isEmpty()) {
            String initial = customerName.substring(0, 1).toUpperCase(Locale.getDefault());
            holder.customerInitial.setText(initial);
        }
        holder.customerName.setText(customerName);
        
        String paidTillText = formatDate(summary.getPaidTillDate(), "Not paid yet");
        String dueFromText = formatDate(summary.getDueFromDate(), "-");
        String period;
        if (summary.getDeliveredEntries() == 0) {
            period = "No service entries yet";
        } else if (summary.getOutstandingAmount() <= EPSILON) {
            period = "Paid till: " + paidTillText + " • No dues";
        } else {
            period = "Paid till: " + paidTillText + " • Due from: " + dueFromText;
        }
        holder.billPeriod.setText(period);
        
        holder.billAmount.setText(CurrencyUtils.formatCurrency(summary.getOutstandingAmount()));
        
        if (summary.getDeliveredEntries() == 0) {
            holder.paymentStatus.setText("No Service");
            holder.paymentStatus.setChipBackgroundColorResource(R.color.md_theme_surface_variant);
            holder.paymentStatus.setTextColor(holder.itemView.getContext()
                .getColor(R.color.md_theme_on_surface_variant));
        } else if (summary.getOutstandingAmount() <= EPSILON) {
            holder.paymentStatus.setText("Clear");
            holder.paymentStatus.setChipBackgroundColorResource(R.color.md_theme_secondary);
            holder.paymentStatus.setTextColor(holder.itemView.getContext()
                .getColor(R.color.md_theme_on_secondary));
        } else if (summary.getTotalPaidAmount() > EPSILON) {
            holder.paymentStatus.setText("Partial");
            holder.paymentStatus.setChipBackgroundColorResource(R.color.color_service_entry);
            holder.paymentStatus.setTextColor(holder.itemView.getContext()
                .getColor(android.R.color.white));
        } else {
            holder.paymentStatus.setText("Pending");
            holder.paymentStatus.setChipBackgroundColorResource(R.color.color_payments);
            holder.paymentStatus.setTextColor(holder.itemView.getContext()
                .getColor(android.R.color.white));
        }

        holder.viewDetailsButton.setText("Ledger");
        holder.shareBillButton.setText("Share");
        holder.viewDetailsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetails(summary);
            }
        });
        holder.shareBillButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareBill(summary);
            }
        });
    }

    @Override
    public int getItemCount() {
        return summaries.size();
    }

    private static String formatDate(Timestamp timestamp, String fallback) {
        if (timestamp == null) return fallback;
        return DateUtils.formatShortDate(timestamp.toDate());
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private static boolean sameTimestamp(Timestamp t1, Timestamp t2) {
        if (t1 == null && t2 == null) return true;
        if (t1 == null || t2 == null) return false;
        return t1.equals(t2);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView customerInitial;
        TextView customerName;
        TextView billPeriod;
        TextView billAmount;
        Chip paymentStatus;
        MaterialButton viewDetailsButton;
        MaterialButton shareBillButton;

        ViewHolder(View itemView) {
            super(itemView);
            customerInitial = itemView.findViewById(R.id.customerInitial);
            customerName = itemView.findViewById(R.id.customerName);
            billPeriod = itemView.findViewById(R.id.billPeriod);
            billAmount = itemView.findViewById(R.id.billAmount);
            paymentStatus = itemView.findViewById(R.id.paymentStatusChip);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
            shareBillButton = itemView.findViewById(R.id.shareBillButton);
        }
    }
}
