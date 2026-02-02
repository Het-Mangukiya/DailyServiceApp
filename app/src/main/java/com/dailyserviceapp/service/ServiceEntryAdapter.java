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
    private final Map<String, Double> quantityOverrides = new HashMap<>(); /**
     * Creates a ServiceEntryAdapter configured to display customers grouped by area,
     * track per-customer delivery status, and manage per-customer quantity overrides.
     */

    public ServiceEntryAdapter() {
    }

    /**
     * Refreshes the adapter's data using the provided customers and service entries.
     *
     * Rebuilds the internal customer list, records which customers already have a delivered
     * service entry for the selected date, groups customers by area (empty area -> "Other Areas"),
     * and constructs the mixed items list consisting of area header strings followed by their customers.
     *
     * @param customerList   list of customers to display; if null the adapter will be cleared
     * @param serviceEntries existing service entries for the selected date used to mark delivered customers
     */
    public void submitData(List<Customer> customerList, List<ServiceEntry> serviceEntries) {
        customers.clear();
        deliveryStatus.clear();
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
        
        notifyDataSetChanged();
    }

    /**
     * Selects the view type for the item at the given adapter position.
     *
     * @return VIEW_TYPE_HEADER if the item at the position is an area header (a String), VIEW_TYPE_CUSTOMER otherwise.
     */
    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof String) ? VIEW_TYPE_HEADER : VIEW_TYPE_CUSTOMER;
    }

    /**
     * Create a ViewHolder instance matching the requested view type.
     *
     * @param parent   the parent ViewGroup used to inflate the item view
     * @param viewType either {@code VIEW_TYPE_HEADER} for area headers or {@code VIEW_TYPE_CUSTOMER} for customer rows
     * @return the created {@link RecyclerView.ViewHolder} appropriate for {@code viewType}
     */
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

    /**
     * Binds the adapter item at the given position to the provided view holder, handling both area headers and customer rows.
     *
     * @param holder the view holder, either {@link HeaderViewHolder} for area headers or {@link CustomerViewHolder} for customer rows
     * @param position adapter position of the item to bind
     */
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
    
    /**
     * Bind a customer's data to the given CustomerViewHolder and wire quantity and delivery controls.
     *
     * This sets the customer's name, service details, address visibility, displayed quantity, and the
     * delivered checkbox state based on the adapter's deliveryStatus. It also enables/disables the
     * decrease/increase buttons according to delivery state and quantity limits, and installs click
     * listeners that update quantityOverrides (clamped to 0.5–10.0) and call notifyItemChanged for the
     * provided position.
     *
     * @param holder   the CustomerViewHolder to populate
     * @param customer the Customer whose data will be displayed
     * @param position the adapter position of this item (used when notifying item changes)
     */
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
        holder.deliveredCheckbox.setChecked(alreadyDelivered);
        holder.deliveredCheckbox.setEnabled(!alreadyDelivered); // Disable if already delivered
        
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

    /**
     * Gets the total number of display items in the adapter.
     *
     * @return the number of items, including area header strings and customer entries
     */
    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Collects delivery records for customers who have not yet been delivered.
     *
     * Each DeliveryItem uses a per-customer quantity override when present; otherwise it uses the customer's default quantity.
     *
     * @return a list of DeliveryItem for every customer without a recorded delivery; each item contains the customerId, quantity, rate per unit, and amount (rate × quantity)
     */
    public List<DeliveryItem> getSelectedDeliveries() {
        List<DeliveryItem> deliveries = new ArrayList<>();
        for (Customer customer : customers) {
            // Only include customers who DON'T have deliveries yet
            if (!deliveryStatus.getOrDefault(customer.getId(), false)) {
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
         * Create a DeliveryItem with the given identifiers and amounts.
         *
         * @param customerId the non-null, non-empty customer identifier
         * @param quantity the delivered quantity (must be >= 0)
         * @param rate the rate per unit (must be >= 0)
         * @param amount the total amount (quantity * rate, must be >= 0)
         * @throws IllegalArgumentException if {@code customerId} is null or empty, or if {@code quantity}, {@code rate}, or {@code amount} is negative
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

        /**
         * Creates a HeaderViewHolder and binds the area name TextView for an area header row.
         *
         * @param itemView the root view of the header item layout
         */
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

        /**
         * Initializes the view holder and binds its child views used to display a customer row.
         *
         * @param itemView the root view containing the customer row layout (must include views with IDs
         *                 txtCustomerName, txtServiceDetails, txtAddress, txtQuantity,
         *                 checkboxDelivered, btnDecreaseQty, and btnIncreaseQty)
         */
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