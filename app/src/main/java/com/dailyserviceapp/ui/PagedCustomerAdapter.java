package com.dailyserviceapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.models.Customer;

import java.util.Objects;

/**
 * PagingDataAdapter for customer list.
 */
public class PagedCustomerAdapter extends PagingDataAdapter<Customer, PagedCustomerAdapter.ViewHolder> {

    public interface OnCustomerActionListener {
        void onViewProfile(Customer customer);
        void onEditCustomer(Customer customer);
        void onToggleVacation(Customer customer);
    }

    private final OnCustomerActionListener listener;
    private String expandedCustomerId;

    public PagedCustomerAdapter(OnCustomerActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = Objects.requireNonNull(listener, "OnCustomerActionListener cannot be null");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer customer = getItem(position);
        if (customer == null) {
            holder.customerMenuButton.setOnClickListener(null);
            holder.expandableActions.setVisibility(View.GONE);
            return;
        }

        holder.customerName.setText(customer.getName());

        String serviceType = customer.getServiceType() == null ? "" : customer.getServiceType().trim();
        String rateText = CurrencyUtils.formatCurrency(customer.getRatePerUnit());
        String serviceInfo = serviceType.isEmpty() ? rateText : serviceType + " · " + rateText;
        holder.customerService.setText(serviceInfo);

        if (customer.getPhone() != null && !customer.getPhone().isEmpty()) {
            holder.customerPhone.setText(customer.getPhone());
            holder.customerPhone.setVisibility(View.VISIBLE);
        } else {
            holder.customerPhone.setVisibility(View.GONE);
        }

        if (customer.getName() != null && !customer.getName().isEmpty()) {
            holder.customerInitial.setText(customer.getName().substring(0, 1).toUpperCase());
        } else {
            holder.customerInitial.setText("");
        }

        holder.customerStatus.setText(customer.getStatus() != null ? customer.getStatus() : "ACTIVE");

        if (customer.isOnVacation()) {
            holder.vacationBadge.setVisibility(View.VISIBLE);
            holder.customerName.setAlpha(0.6f);
        } else {
            holder.vacationBadge.setVisibility(View.GONE);
            holder.customerName.setAlpha(1.0f);
        }

        boolean isExpanded = customer.getId() != null && customer.getId().equals(expandedCustomerId);
        holder.expandableActions.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.customerMenuButton.setIconResource(
            isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more
        );

        String vacationText = customer.isOnVacation()
            ? holder.itemView.getContext().getString(R.string.remove_from_vacation)
            : holder.itemView.getContext().getString(R.string.mark_on_vacation);
        holder.vacationActionButton.setText(vacationText);

        holder.customerMenuButton.setOnClickListener(v -> toggleExpanded(customer));
        holder.viewProfileButton.setOnClickListener(v -> listener.onViewProfile(customer));
        holder.editCustomerButton.setOnClickListener(v -> listener.onEditCustomer(customer));
        holder.vacationActionButton.setOnClickListener(v -> listener.onToggleVacation(customer));
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
        for (int i = 0; i < getItemCount(); i++) {
            Customer item = getItem(i);
            if (item != null && customerId.equals(item.getId())) {
                return i;
            }
        }
        return -1;
    }

    private static final DiffUtil.ItemCallback<Customer> DIFF_CALLBACK = new DiffUtil.ItemCallback<Customer>() {
        @Override
        public boolean areItemsTheSame(@NonNull Customer oldItem, @NonNull Customer newItem) {
            if (oldItem.getId() == null || newItem.getId() == null) {
                return false;
            }
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Customer oldItem, @NonNull Customer newItem) {
            return safeEquals(oldItem.getName(), newItem.getName())
                && safeEquals(oldItem.getServiceType(), newItem.getServiceType())
                && safeEquals(oldItem.getPhone(), newItem.getPhone())
                && safeEquals(oldItem.getStatus(), newItem.getStatus())
                && oldItem.isOnVacation() == newItem.isOnVacation()
                && Double.compare(oldItem.getRatePerUnit(), newItem.getRatePerUnit()) == 0;
        }

        private boolean safeEquals(@Nullable String a, @Nullable String b) {
            return a == null ? b == null : a.equals(b);
        }
    };
}
