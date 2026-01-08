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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying and managing service entries in a RecyclerView.
 * Provides inline editing capabilities for quantity and delivery status.
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-09
 */
public class ServiceEntryAdapter extends RecyclerView.Adapter<ServiceEntryAdapter.ViewHolder> {

    /**
     * Listener interface for service entry changes.
     */
    public interface OnEntryChangeListener {
        void onQuantityChanged(Customer customer, double quantity, boolean delivered);
    }

    private final List<Customer> customers = new ArrayList<>();
    private final Map<String, ServiceEntry> entries = new HashMap<>();
    private final OnEntryChangeListener listener;

    public ServiceEntryAdapter(OnEntryChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the adapter with customer list and existing service entries.
     * 
     * @param customerList List of customers to display
     * @param serviceEntries Existing service entries for the selected date
     */
    public void submitData(List<Customer> customerList, List<ServiceEntry> serviceEntries) {
        customers.clear();
        entries.clear();
        
        if (customerList != null) {
            customers.addAll(customerList);
        }
        
        if (serviceEntries != null) {
            for (ServiceEntry entry : serviceEntries) {
                entries.put(entry.getCustomerId(), entry);
            }
        }
        
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_service_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer customer = customers.get(position);
        ServiceEntry entry = entries.get(customer.getId());
        
        // Set customer info
        if (customer.getName() != null && !customer.getName().isEmpty()) {
            String initial = customer.getName().substring(0, 1).toUpperCase();
            holder.customerInitial.setText(initial);
        }
        holder.customerName.setText(customer.getName());
        
        String serviceInfo = customer.getServiceType() + " · " + customer.getRatePerUnit() + "/unit";
        holder.serviceInfo.setText(serviceInfo);
        
        // Set quantity and delivery status
        double quantity = entry != null ? entry.getQuantity() : 0.0;
        boolean delivered = entry != null && entry.isDelivered();
        
        holder.quantityText.setText(String.valueOf(quantity));
        holder.deliveredCheckbox.setChecked(delivered);
        
        // Decrease button
        holder.decreaseButton.setOnClickListener(v -> {
            double currentQty = Double.parseDouble(holder.quantityText.getText().toString());
            if (currentQty > 0) {
                double newQty = currentQty - 0.5;
                holder.quantityText.setText(String.valueOf(newQty));
                listener.onQuantityChanged(customer, newQty, holder.deliveredCheckbox.isChecked());
            }
        });
        
        // Increase button
        holder.increaseButton.setOnClickListener(v -> {
            double currentQty = Double.parseDouble(holder.quantityText.getText().toString());
            double newQty = currentQty + 0.5;
            holder.quantityText.setText(String.valueOf(newQty));
            listener.onQuantityChanged(customer, newQty, holder.deliveredCheckbox.isChecked());
        });
        
        // Delivered checkbox
        holder.deliveredCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            double currentQty = Double.parseDouble(holder.quantityText.getText().toString());
            listener.onQuantityChanged(customer, currentQty, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView customerInitial;
        final TextView customerName;
        final TextView serviceInfo;
        final MaterialButton decreaseButton;
        final TextView quantityText;
        final MaterialButton increaseButton;
        final MaterialCheckBox deliveredCheckbox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            customerInitial = itemView.findViewById(R.id.customerInitial);
            customerName = itemView.findViewById(R.id.customerName);
            serviceInfo = itemView.findViewById(R.id.serviceInfo);
            decreaseButton = itemView.findViewById(R.id.decreaseButton);
            quantityText = itemView.findViewById(R.id.quantityText);
            increaseButton = itemView.findViewById(R.id.increaseButton);
            deliveredCheckbox = itemView.findViewById(R.id.deliveryCheckbox);
        }
    }
}
