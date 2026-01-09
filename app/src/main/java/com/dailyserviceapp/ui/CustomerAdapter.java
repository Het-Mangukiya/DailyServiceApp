package com.dailyserviceapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.models.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder> {

    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer);
    }

    private final List<Customer> items = new ArrayList<>();
    private final OnCustomerClickListener listener;

    public CustomerAdapter(OnCustomerClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<Customer> customers) {
        items.clear();
        if (customers != null) {
            items.addAll(customers);
        }
        notifyDataSetChanged();
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
        String serviceInfo = (customer.getServiceType() == null ? "" : customer.getServiceType())
                + " · "
                + CurrencyUtils.formatCurrency(customer.getRatePerUnit());
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

        holder.itemView.setOnClickListener(v -> listener.onCustomerClick(customer));
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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customerName);
            customerService = itemView.findViewById(R.id.customerService);
            customerPhone = itemView.findViewById(R.id.customerPhone);
            customerInitial = itemView.findViewById(R.id.customerInitial);
            customerStatus = itemView.findViewById(R.id.customerStatus);
        }
    }
}
