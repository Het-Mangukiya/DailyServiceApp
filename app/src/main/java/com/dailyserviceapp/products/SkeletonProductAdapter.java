package com.dailyserviceapp.products;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Skeleton adapter for showing loading state in Products list.
 */
public class SkeletonProductAdapter extends RecyclerView.Adapter<SkeletonProductAdapter.SkeletonViewHolder> {

    private List<Object> items;

    public SkeletonProductAdapter(int count) {
        items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new Object());
        }
    }

    @NonNull
    @Override
    public SkeletonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SkeletonViewHolder(
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.skeleton_product_item, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull SkeletonViewHolder holder, int position) {
        // Shimmer effect is handled by the layout
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SkeletonViewHolder extends RecyclerView.ViewHolder {
        ShimmerFrameLayout shimmerFrameLayout;

        SkeletonViewHolder(android.view.View itemView) {
            super(itemView);
            shimmerFrameLayout = itemView.findViewById(R.id.shimmerLayout);
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.startShimmer();
            }
        }
    }
}

