package com.dailyserviceapp.billing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.models.Bill;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Adapter for displaying bills in a RecyclerView.
 * Shows customer name, bill amount, payment status, and action buttons.
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
        void onViewDetails(Bill bill);
        void onShareBill(Bill bill);
    }

    private final List<Bill> bills = new ArrayList<>();
    private final List<String> customerNames = new ArrayList<>();
    private final OnBillActionListener listener;

    public BillAdapter(OnBillActionListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the adapter with bills and customer names.
     * 
     * @param billList List of bills to display
     * @param names List of customer names corresponding to bills
     */
    public void submitData(List<Bill> billList, List<String> names) {
        bills.clear();
        customerNames.clear();
        
        if (billList != null) {
            bills.addAll(billList);
        }
        
        if (names != null) {
            customerNames.addAll(names);
        }
        
        notifyDataSetChanged();
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
        Bill bill = bills.get(position);
        String customerName = position < customerNames.size() ? customerNames.get(position) : "Unknown";
        
        // Set customer info
        if (customerName != null && !customerName.isEmpty()) {
            String initial = customerName.substring(0, 1).toUpperCase();
            holder.customerInitial.setText(initial);
        }
        holder.customerName.setText(customerName);
        
        // Set bill period
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                               "July", "August", "September", "October", "November", "December"};
        String period = monthNames[bill.getMonth()] + " " + bill.getYear() + " • " + bill.getDaysServed() + " days";
        holder.billPeriod.setText(period);
        
        // Set bill amount
        holder.billAmount.setText(CurrencyUtils.formatCurrency(bill.getTotalAmount()));
        
        // Set payment status
        String status = bill.getPaymentStatus();
        if (status == null) status = "PENDING";
        
        switch (status) {
            case "PAID":
                holder.paymentStatus.setText("Paid");
                holder.paymentStatus.setChipBackgroundColorResource(R.color.md_theme_secondary);
                holder.paymentStatus.setTextColor(holder.itemView.getContext()
                        .getColor(R.color.md_theme_on_secondary));
                break;
            case "PARTIAL":
                holder.paymentStatus.setText("Partial");
                holder.paymentStatus.setChipBackgroundColorResource(R.color.color_service_entry);
                holder.paymentStatus.setTextColor(holder.itemView.getContext()
                        .getColor(android.R.color.white));
                break;
            case "OVERDUE":
                holder.paymentStatus.setText("Overdue");
                holder.paymentStatus.setChipBackgroundColorResource(R.color.color_payments);
                holder.paymentStatus.setTextColor(holder.itemView.getContext()
                        .getColor(android.R.color.white));
                break;
            default:
                holder.paymentStatus.setText("Pending");
                holder.paymentStatus.setChipBackgroundColorResource(R.color.color_payments);
                holder.paymentStatus.setTextColor(holder.itemView.getContext()
                        .getColor(android.R.color.white));
                break;
        }
        
        // Set click listeners
        holder.viewDetailsButton.setOnClickListener(v -> listener.onViewDetails(bill));
        holder.shareBillButton.setOnClickListener(v -> listener.onShareBill(bill));
    }

    @Override
    public int getItemCount() {
        return bills.size();
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
