package com.dailyserviceapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.models.Customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {

    public interface OnCustomerActionListener {
        void onViewProfile(Customer customer);
        void onEditCustomer(Customer customer);
        void onToggleVacation(Customer customer);
    }

    private final List<Customer> items = new ArrayList<>();
    private final OnCustomerActionListener listener;
    private String expandedCustomerId;

    public CustomerAdapter(OnCustomerActionListener listener) {
        this.listener = Objects.requireNonNull(listener, "OnCustomerActionListener cannot be null");
    }

    public void submit(List<Customer> customers) {
        List<Customer> oldList = new ArrayList<>(items);
        items.clear();
        if (customers != null) {
            items.addAll(customers);
        }

        if (expandedCustomerId != null && findPositionById(expandedCustomerId) < 0) {
            expandedCustomerId = null;
        }
        
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return items.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Customer oldItem = oldList.get(oldItemPosition);
                Customer newItem = items.get(newItemPosition);
                return Objects.equals(oldItem.getId(), newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Customer oldItem = oldList.get(oldItemPosition);
                Customer newItem = items.get(newItemPosition);
                return safeEquals(oldItem.getName(), newItem.getName()) &&
                       safeEquals(oldItem.getServiceType(), newItem.getServiceType()) &&
                       Double.compare(oldItem.getRatePerUnit(), newItem.getRatePerUnit()) == 0 &&
                       oldItem.isOnVacation() == newItem.isOnVacation() &&
                       safeEquals(oldItem.getStatus(), newItem.getStatus());
            }
        });
        
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer customer = items.get(position);
        
        // Set customer name
        holder.customerName.setText(customer.getName());
        
        // Set service info
        String serviceType = customer.getServiceType() == null ? "" : customer.getServiceType().trim();
        String rateText = CurrencyUtils.formatCurrency(customer.getRatePerUnit());
        String serviceInfo = serviceType.isEmpty() ? rateText : serviceType + " · " + rateText;
        holder.customerService.setText(serviceInfo);
        
        // Set phone number (optional)
        if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
            holder.customerPhone.setText(customer.getPhone());
            holder.customerPhone.setVisibility(View.VISIBLE);
        } else {
            holder.customerPhone.setVisibility(View.GONE);
        }
        
        // Set customer initial
        if (customer.getName() != null && !customer.getName().isEmpty()) {
            String initial = customer.getName().substring(0, 1).toUpperCase();
            holder.customerInitial.setText(initial);
        }
        
        // Set status chip
        holder.customerStatus.setText(customer.getStatus() != null ? customer.getStatus() : "ACTIVE");
        
        // Show vacation badge if customer is on vacation
        if (customer.isOnVacation()) {
            holder.vacationBadge.setVisibility(View.VISIBLE);
            holder.customerName.setAlpha(0.6f);
        } else {
            holder.vacationBadge.setVisibility(View.GONE);
            holder.customerName.setAlpha(1.0f);
        }

        boolean isExpanded = isExpanded(customer);
        holder.expandableActions.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.customerMenuButton.setIconResource(
            isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more
        );

        String vacationText = customer.isOnVacation() ? "Remove from Vacation" : "Mark as On Vacation";
        holder.vacationActionButton.setText(vacationText);

        holder.customerMenuButton.setOnClickListener(v -> toggleExpanded(customer));
        holder.viewProfileButton.setOnClickListener(v -> listener.onViewProfile(customer));
        holder.editCustomerButton.setOnClickListener(v -> listener.onEditCustomer(customer));
        holder.vacationActionButton.setOnClickListener(v -> listener.onToggleVacation(customer));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView customerName;
        final TextView customerService;
        final TextView customerPhone;
        final TextView customerInitial;
        final com.google.android.material.chip.Chip customerStatus;
        final TextView vacationBadge;
        final com.google.android.material.button.MaterialButton customerMenuButton;
        final View expandableActions;
        final com.google.android.material.button.MaterialButton viewProfileButton;
        final com.google.android.material.button.MaterialButton editCustomerButton;
        final com.google.android.material.button.MaterialButton vacationActionButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customerName);
            customerService = itemView.findViewById(R.id.customerService);
            customerPhone = itemView.findViewById(R.id.customerPhone);
            customerInitial = itemView.findViewById(R.id.customerInitial);
            customerStatus = itemView.findViewById(R.id.customerStatus);
            vacationBadge = itemView.findViewById(R.id.vacationBadge);
            customerMenuButton = itemView.findViewById(R.id.customerMenuButton);
            expandableActions = itemView.findViewById(R.id.expandableActions);
            viewProfileButton = itemView.findViewById(R.id.viewProfileButton);
            editCustomerButton = itemView.findViewById(R.id.editCustomerButton);
            vacationActionButton = itemView.findViewById(R.id.vacationActionButton);
        }
    }

    private boolean isExpanded(Customer customer) {
        return customer != null
            && customer.getId() != null
            && customer.getId().equals(expandedCustomerId);
    }

    private void toggleExpanded(Customer customer) {
        if (customer == null || customer.getId() == null) return;

        String previousExpanded = expandedCustomerId;
        if (customer.getId().equals(expandedCustomerId)) {
            expandedCustomerId = null;
        } else {
            expandedCustomerId = customer.getId();
        }

        notifyChangedById(previousExpanded);
        notifyChangedById(expandedCustomerId);
    }

    private void notifyChangedById(String customerId) {
        int position = findPositionById(customerId);
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    private int findPositionById(String customerId) {
        if (customerId == null) return -1;
        for (int i = 0; i < items.size(); i++) {
            Customer item = items.get(i);
            if (item != null && customerId.equals(item.getId())) {
                return i;
            }
        }
        return -1;
    }

    private boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
