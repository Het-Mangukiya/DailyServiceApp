package com.dailyserviceapp.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dailyserviceapp.R;
import com.dailyserviceapp.data.models.Product;

import java.util.List;

/**
 * Adapter for displaying products in a RecyclerView.
 */
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView categoryText;
        private TextView priceText;
        private TextView quantityText;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.productNameText);
            categoryText = itemView.findViewById(R.id.productCategoryText);
            priceText = itemView.findViewById(R.id.productPriceText);
            quantityText = itemView.findViewById(R.id.productQuantityText);
        }

        void bind(Product product) {
            nameText.setText(product.getName());
            categoryText.setText(product.getCategory());
            priceText.setText("₹" + String.format("%.2f", product.getPrice()));
            quantityText.setText("Qty: " + product.getQuantity());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }
    }
}

