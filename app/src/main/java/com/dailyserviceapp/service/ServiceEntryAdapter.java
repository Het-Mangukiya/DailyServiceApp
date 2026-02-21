package com.dailyserviceapp.service;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
public class ServiceEntryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CUSTOMER = 1;

    private final List<Object> items = new ArrayList<>(); // Mix of String (headers) and Customer objects
    private final List<Customer> customers = new ArrayList<>();
    private final Map<String, Boolean> deliveryStatus = new HashMap<>(); // true = already delivered
    private final Map<String, Double> quantityOverrides = new HashMap<>(); // custom quantities
    private final Map<String, Boolean> selectionState = new HashMap<>(); // true = selected for marking

    public ServiceEntryAdapter() {
    }

    /**
     * Updates adapter with customer list and existing service entries using DiffUtil.
     * Marks which customers already have deliveries for the selected date.
     * Groups customers by area for route planning.
     * 
     * @param customerList List of customers
     * @param serviceEntries Existing service entries for selected date
     */
    public void submitData(List<Customer> customerList, List<ServiceEntry> serviceEntries) {
        List<Object> oldItems = new ArrayList<>(items);
        
        customers.clear();
        deliveryStatus.clear();
        selectionState.clear();
        items.clear();
        
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
                selectionState.put(customer.getId(), false);
            }
            
            // Group customers by area for route planning
            Map<String, List<Customer>> groupedByArea = new LinkedHashMap<>();
            for (Customer customer : customerList) {
                String area = customer.getArea();
                if (area == null || area.trim().isEmpty()) {
                    area = "Other Areas";
                }
                if (!groupedByArea.containsKey(area)) {
                    groupedByArea.put(area, new ArrayList<>());
                }
                groupedByArea.get(area).add(customer);
            }
            
            // Build items list with headers
            for (Map.Entry<String, List<Customer>> entry : groupedByArea.entrySet()) {
                items.add(entry.getKey()); // Add header
                items.addAll(entry.getValue()); // Add customers in this area
            }
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldItems.size();
            }

            @Override
            public int getNewListSize() {
                return items.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Object oldItem = oldItems.get(oldItemPosition);
                Object newItem = items.get(newItemPosition);
                
                if (oldItem instanceof String && newItem instanceof String) {
                    return oldItem.equals(newItem);
                } else if (oldItem instanceof Customer && newItem instanceof Customer) {
                    Customer oldCustomer = (Customer) oldItem;
                    Customer newCustomer = (Customer) newItem;
                    return oldCustomer.getId() != null && oldCustomer.getId().equals(newCustomer.getId());
                }
                return false;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Object oldItem = oldItems.get(oldItemPosition);
                Object newItem = items.get(newItemPosition);
                
                if (oldItem instanceof String && newItem instanceof String) {
                    return oldItem.equals(newItem);
                } else if (oldItem instanceof Customer && newItem instanceof Customer) {
                    Customer oldCustomer = (Customer) oldItem;
                    Customer newCustomer = (Customer) newItem;
                    return oldCustomer.getName().equals(newCustomer.getName()) &&
                           oldCustomer.getServiceType().equals(newCustomer.getServiceType()) &&
                           oldCustomer.getRatePerUnit() == newCustomer.getRatePerUnit() &&
                           oldCustomer.getDefaultQuantity() == newCustomer.getDefaultQuantity();
                }
                return false;
            }
        });
        
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? VIEW_TYPE_HEADER : VIEW_TYPE_CUSTOMER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_area_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_service_entry, parent, false);
            return new CustomerViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            String areaName = (String) items.get(position);
            ((HeaderViewHolder) holder).areaName.setText(areaName);
        } else if (holder instanceof CustomerViewHolder) {
            Customer customer = (Customer) items.get(position);
            bindCustomer((CustomerViewHolder) holder, customer, position);
        }
    }
    
    private void bindCustomer(@NonNull CustomerViewHolder holder, Customer customer, int position) {
        boolean alreadyDelivered = deliveryStatus.getOrDefault(customer.getId(), false);
        
        // Set customer name
        holder.customerName.setText(customer.getName());
        
        // Get current quantity (custom override or default)
        double currentQuantity = quantityOverrides.getOrDefault(customer.getId(), customer.getDefaultQuantity());
        
        // Set service details
        String details = String.format(Locale.getDefault(), "%s • ₹%.0f × %.1f", 
            customer.getServiceType(),
            customer.getRatePerUnit(),
            currentQuantity
        );
        holder.serviceDetails.setText(details);
        
        // Show address if available
        if (customer.getAddress() != null && !customer.getAddress().trim().isEmpty()) {
            holder.addressText.setText(customer.getAddress());
            holder.addressText.setVisibility(View.VISIBLE);
        } else {
            holder.addressText.setVisibility(View.GONE);
        }
        
        // Set quantity
        holder.quantityText.setText(String.format(Locale.getDefault(), "%.1f", currentQuantity));
        
        // Checkbox: checked = already delivered (read-only), unchecked = ready to mark
        holder.deliveredCheckbox.setOnCheckedChangeListener(null);
        if (alreadyDelivered) {
            holder.deliveredCheckbox.setChecked(true);
            holder.deliveredCheckbox.setEnabled(false);
            holder.itemView.setOnClickListener(null);
        } else {
            boolean isSelected = selectionState.getOrDefault(customer.getId(), false);
            holder.deliveredCheckbox.setChecked(isSelected);
            holder.deliveredCheckbox.setEnabled(true);
            holder.deliveredCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectionState.put(customer.getId(), isChecked);
            });
            holder.itemView.setOnClickListener(v ->
                holder.deliveredCheckbox.setChecked(!holder.deliveredCheckbox.isChecked())
            );
        }
        
        // Quantity controls - only enabled if not already delivered
        boolean enableControls = !alreadyDelivered;
        holder.btnDecreaseQty.setEnabled(enableControls && currentQuantity > 0.5);
        holder.btnIncreaseQty.setEnabled(enableControls && currentQuantity < 10.0);
        
        // Decrease quantity
        holder.btnDecreaseQty.setOnClickListener(v -> {
            double newQty = Math.max(0.5, currentQuantity - 0.5);
            quantityOverrides.put(customer.getId(), newQty);
            notifyItemChanged(position);
        });
        
        // Increase quantity
        holder.btnIncreaseQty.setOnClickListener(v -> {
            double newQty = Math.min(10.0, currentQuantity + 0.5);
            quantityOverrides.put(customer.getId(), newQty);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Get customers who DON'T have deliveries yet (ready for marking)
     */
    public List<DeliveryItem> getSelectedDeliveries() {
        List<DeliveryItem> deliveries = new ArrayList<>();
        for (Customer customer : customers) {
            // Only include customers who DON'T have deliveries yet
            boolean alreadyDelivered = deliveryStatus.getOrDefault(customer.getId(), false);
            boolean isSelected = selectionState.getOrDefault(customer.getId(), false);
            if (!alreadyDelivered && isSelected) {
                double quantity = quantityOverrides.getOrDefault(customer.getId(), customer.getDefaultQuantity());
                deliveries.add(new DeliveryItem(
                    customer.getId(),
                    quantity,
                    customer.getRatePerUnit(),
                    customer.getRatePerUnit() * quantity
                ));
            }
        }
        return deliveries;
    }

    /**
     * Selects all customers that are eligible for delivery marking.
     *
     * @return count of customers selected
     */
    public int selectAllAvailable() {
        int selectedCount = 0;
        for (Customer customer : customers) {
            String customerId = customer.getId();
            if (customerId == null) continue;
            boolean alreadyDelivered = deliveryStatus.getOrDefault(customerId, false);
            if (!alreadyDelivered) {
                selectionState.put(customerId, true);
                selectedCount++;
            }
        }
        notifyDataSetChanged();
        return selectedCount;
    }

    /**
     * Clears all current selections.
     */
    public void clearSelection() {
        for (Customer customer : customers) {
            String customerId = customer.getId();
            if (customerId != null) {
                selectionState.put(customerId, false);
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Simple data class for delivery marking.
     * Immutable value object to ensure data integrity.
     *
     * @since 2.0
     */
    public static class DeliveryItem {
        public final String customerId;
        public final double quantity;
        public final double rate;
        public final double amount;

        /**
         * Creates a new delivery item.
         *
         * @param customerId The unique identifier of the customer
         * @param quantity The quantity delivered
         * @param rate The rate per unit
         * @param amount The total amount (quantity * rate)
         * @throws IllegalArgumentException if customerId is null/empty or values are negative
         */
        public DeliveryItem(String customerId, double quantity, double rate, double amount) {
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID cannot be null or empty");
            }
            if (quantity < 0 || rate < 0 || amount < 0) {
                throw new IllegalArgumentException("Quantity, rate, and amount must be non-negative");
            }
            this.customerId = customerId;
            this.quantity = quantity;
            this.rate = rate;
            this.amount = amount;
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView areaName;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            areaName = itemView.findViewById(R.id.txtAreaName);
        }
    }

    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        final TextView customerName;
        final TextView serviceDetails;
        final TextView addressText;
        final TextView quantityText;
        final MaterialCheckBox deliveredCheckbox;
        final MaterialButton btnDecreaseQty;
        final MaterialButton btnIncreaseQty;

        CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.txtCustomerName);
            serviceDetails = itemView.findViewById(R.id.txtServiceDetails);
            addressText = itemView.findViewById(R.id.txtAddress);
            quantityText = itemView.findViewById(R.id.txtQuantity);
            deliveredCheckbox = itemView.findViewById(R.id.checkboxDelivered);
            btnDecreaseQty = itemView.findViewById(R.id.btnDecreaseQty);
            btnIncreaseQty = itemView.findViewById(R.id.btnIncreaseQty);
        }
    }
}
