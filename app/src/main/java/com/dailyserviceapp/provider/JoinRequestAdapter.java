package com.dailyserviceapp.provider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.ViewHolder> {

    public interface OnActionListener {
        void onApprove(JoinRequestItem item);
        void onReject(JoinRequestItem item);
    }

    private final List<JoinRequestItem> items = new ArrayList<>();
    private final OnActionListener listener;

    public JoinRequestAdapter(OnActionListener listener) {
        this.listener = Objects.requireNonNull(listener, "OnActionListener cannot be null");
    }

    public void submit(List<JoinRequestItem> requests) {
        items.clear();
        if (requests != null) {
            items.addAll(requests);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.row_join_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JoinRequestItem item = items.get(position);
        String fallbackCustomer = holder.itemView.getContext().getString(R.string.customer_default);
        String fallbackEmail = holder.itemView.getContext().getString(R.string.no_email);
        String fallbackPhone = holder.itemView.getContext().getString(R.string.no_phone);

        String name = valueOrDefault(item.getCustomerName(), fallbackCustomer);
        holder.txtName.setText(name);

        String email = valueOrDefault(item.getCustomerEmail(), fallbackEmail);
        holder.txtEmail.setText(email);

        String phone = valueOrDefault(item.getCustomerPhone(), fallbackPhone);
        holder.txtPhone.setText(phone);

        if (item.getRequestedAt() != null) {
            SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            String when = format.format(item.getRequestedAt().toDate());
            holder.txtRequestedAt.setText(
                holder.itemView.getContext().getString(R.string.requested_at_format, when)
            );
        } else {
            holder.txtRequestedAt.setText(holder.itemView.getContext().getString(R.string.requested_just_now));
        }

        holder.btnApprove.setOnClickListener(v -> listener.onApprove(item));
        holder.btnReject.setOnClickListener(v -> listener.onReject(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String valueOrDefault(String input, String fallback) {
        if (input == null || input.trim().isEmpty()) {
            return fallback;
        }
        return input.trim();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView txtName;
        final TextView txtEmail;
        final TextView txtPhone;
        final TextView txtRequestedAt;
        final MaterialButton btnApprove;
        final MaterialButton btnReject;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtRequestCustomerName);
            txtEmail = itemView.findViewById(R.id.txtRequestCustomerEmail);
            txtPhone = itemView.findViewById(R.id.txtRequestCustomerPhone);
            txtRequestedAt = itemView.findViewById(R.id.txtRequestTime);
            btnApprove = itemView.findViewById(R.id.btnApproveRequest);
            btnReject = itemView.findViewById(R.id.btnRejectRequest);
        }
    }
}
