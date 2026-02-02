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
        /**
 * Handle a user action to view details for the given bill.
 *
 * @param bill the bill whose detailed information should be presented
 */
void onViewDetails(Bill bill);
        /**
 * Share the specified bill with external apps or services.
 *
 * @param bill the bill to share
 */
void onShareBill(Bill bill);
    }

    private final List<Bill> bills = new ArrayList<>();
    private final List<String> customerNames = new ArrayList<>();
    private final OnBillActionListener listener;

    /**
     * Creates a BillAdapter and registers a listener to receive bill action events.
     *
     * @param listener callback invoked when a bill's "view details" or "share" action is triggered
     */
    public BillAdapter(OnBillActionListener listener) {
        this.listener = listener;
    }

    /**
     * Replace the adapter's data with the provided bills and corresponding customer names.
     *
     * <p>This clears any existing items, adds all entries from {@code billList} and {@code names}
     * when non-null, and refreshes the RecyclerView.</p>
     *
     * @param billList list of bills to display; if {@code null} the adapter's bill list is cleared
     * @param names list of customer names aligned by index with {@code billList}; if {@code null} the adapter's name list is cleared
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

    /**
     * Inflates the bill row layout and creates a ViewHolder for it.
     *
     * @param parent   the parent ViewGroup to which the new view will eventually be attached
     * @param viewType the view type of the new view
     * @return         a ViewHolder bound to the inflated row_bill layout
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_bill, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds bill data and UI interactions into the provided ViewHolder for the item at the given position.
     *
     * Sets the customer's initial and name (falls back to "Unknown" when a name is missing), constructs and displays
     * the bill period from month/year and days served, formats and displays the total amount, maps the payment status
     * (defaults to "PENDING") to a user-facing label and chip color, and attaches click listeners for view and share actions.
     *
     * @param holder   the ViewHolder whose views will be populated
     * @param position the adapter position of the bill to bind; used to select the Bill and the corresponding customer name
     */
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

    /**
     * Return the number of bills currently held by the adapter.
     *
     * @return the number of bills in the adapter
     */
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

        /**
         * Creates a ViewHolder and binds its child view references from the provided item view.
         *
         * @param itemView the root view of a bill row used to locate child UI elements
         */
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