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
        holder.name.setText(customer.getName());

        String subtitle = (customer.getServiceType() == null ? "" : customer.getServiceType())
                + " · "
                + CurrencyUtils.formatCurrency(customer.getRatePerUnit());
        holder.subtitle.setText(subtitle);

        // Set customer initial
        if (customer.getName() != null && !customer.getName().isEmpty()) {
            String initial = customer.getName().substring(0, 1).toUpperCase();
            holder.customerInitial.setText(initial);
        }

        holder.itemView.setOnClickListener(v -> listener.onCustomerClick(customer));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView subtitle;
        final TextView customerInitial;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            subtitle = itemView.findViewById(R.id.subtitle);
            customerInitial = itemView.findViewById(R.id.customerInitial);
        }
    }
}
