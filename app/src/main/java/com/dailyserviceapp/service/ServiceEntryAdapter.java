package com.dailyserviceapp.service;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified adapter for service entry marking.
 * Shows which customers already have deliveries marked today.
 * Only unmarked customers can be selected for batch delivery.
 * 
 * @author DailyDrop Team
 * @version 3.0
 * @since 2026-01-10
 */
public class ServiceEntryAdapter extends RecyclerView.Adapter<ServiceEntryAdapter.ViewHolder> {

    private final List<Customer> customers = new ArrayList<>();
    private final Map<String, Boolean> deliveryStatus = new HashMap<>(); // true = already delivered

    public ServiceEntryAdapter() {
    }

    /**
     * Updates adapter with customer list and existing service entries.
     * Marks which customers already have deliveries for the selected date.
     * 
     * @param customerList List of customers
     * @param serviceEntries Existing service entries for selected date
     */
    public void submitData(List<Customer> customerList, List<ServiceEntry> serviceEntries) {
        customers.clear();
        deliveryStatus.clear();
        
        if (customerList != null) {
            customers.addAll(customerList);
            
            // Mark which customers already have deliveries
            for (Customer customer : customerList) {
                boolean hasEntry = false;
                if (serviceEntries != null) {
                    for (ServiceEntry entry : serviceEntries) {
                        if (entry.getCustomerId().equals(customer.getId()) && entry.isDelivered()) {
                            hasEntry = true;
                            break;
                        }
                    }
                }
                deliveryStatus.put(customer.getId(), hasEntry);
            }
        }
        
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer customer = customers.get(position);
        boolean alreadyDelivered = deliveryStatus.getOrDefault(customer.getId(), false);
        
        // Set customer name
        holder.customerName.setText(customer.getName());
        
        // Set service details
        String details = String.format("%s • ₹%.0f × %.1f", 
            customer.getServiceType(),
            customer.getRatePerUnit(),
            customer.getDefaultQuantity()
        );
        holder.serviceDetails.setText(details);
        
        // Set quantity
        holder.quantityText.setText(String.format("%.1f", customer.getDefaultQuantity()));
        
        // Checkbox: checked = already delivered (read-only), unchecked = ready to mark
        holder.deliveredCheckbox.setOnCheckedChangeListener(null);
        holder.deliveredCheckbox.setChecked(alreadyDelivered);
        holder.deliveredCheckbox.setEnabled(!alreadyDelivered); // Disable if already delivered
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    /**
     * Get customers who DON'T have deliveries yet (ready for marking)
     */
    public List<DeliveryItem> getSelectedDeliveries() {
        List<DeliveryItem> deliveries = new ArrayList<>();
        for (Customer customer : customers) {
            // Only include customers who DON'T have deliveries yet
            if (!deliveryStatus.getOrDefault(customer.getId(), false)) {
                deliveries.add(new DeliveryItem(
                    customer.getId(),
                    customer.getDefaultQuantity(),
                    customer.getRatePerUnit() * customer.getDefaultQuantity()
                ));
            }
        }
        return deliveries;
    }

    /**
     * Simple data class for delivery marking
     */
    public static class DeliveryItem {
        public final String customerId;
        public final double quantity;
        public final double amount;

        public DeliveryItem(String customerId, double quantity, double amount) {
            this.customerId = customerId;
            this.quantity = quantity;
            this.amount = amount;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView customerName;
        final TextView serviceDetails;
        final TextView quantityText;
        final MaterialCheckBox deliveredCheckbox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.txtCustomerName);
            serviceDetails = itemView.findViewById(R.id.txtServiceDetails);
            quantityText = itemView.findViewById(R.id.txtQuantity);
            deliveredCheckbox = itemView.findViewById(R.id.checkboxDelivered);
        }
    }
}
