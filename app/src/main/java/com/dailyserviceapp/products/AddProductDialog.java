package com.dailyserviceapp.products;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dailyserviceapp.R;

/**
 * Dialog for adding a new product.
 */
public class AddProductDialog extends Dialog {

    public interface OnProductAddedListener {
        void onProductAdded(String name, String category, double price, int quantity, String description);
    }

    private OnProductAddedListener listener;
    private EditText nameEditText;
    private EditText categoryEditText;
    private EditText priceEditText;
    private EditText quantityEditText;
    private EditText descriptionEditText;
    private Button addButton;
    private Button cancelButton;

    public AddProductDialog(Context context, OnProductAddedListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_add_product);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.nameEditText);
        categoryEditText = findViewById(R.id.categoryEditText);
        priceEditText = findViewById(R.id.priceEditText);
        quantityEditText = findViewById(R.id.quantityEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        addButton = findViewById(R.id.addButton);
        cancelButton = findViewById(R.id.cancelButton);
    }

    private void setupClickListeners() {
        addButton.setOnClickListener(v -> {
            if (validateInput()) {
                String name = nameEditText.getText().toString().trim();
                String category = categoryEditText.getText().toString().trim();
                double price = Double.parseDouble(priceEditText.getText().toString().trim());
                int quantity = Integer.parseInt(quantityEditText.getText().toString().trim());
                String description = descriptionEditText.getText().toString().trim();

                if (listener != null) {
                    listener.onProductAdded(name, category, price, quantity, description);
                }
                dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private boolean validateInput() {
        if (TextUtils.isEmpty(nameEditText.getText().toString().trim())) {
            nameEditText.setError("Product name is required");
            return false;
        }

        if (TextUtils.isEmpty(categoryEditText.getText().toString().trim())) {
            categoryEditText.setError("Category is required");
            return false;
        }

        if (TextUtils.isEmpty(priceEditText.getText().toString().trim())) {
            priceEditText.setError("Price is required");
            return false;
        }

        try {
            double price = Double.parseDouble(priceEditText.getText().toString().trim());
            if (price < 0) {
                priceEditText.setError("Price must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            priceEditText.setError("Invalid price");
            return false;
        }

        if (TextUtils.isEmpty(quantityEditText.getText().toString().trim())) {
            quantityEditText.setError("Quantity is required");
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityEditText.getText().toString().trim());
            if (quantity < 0) {
                quantityEditText.setError("Quantity must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            quantityEditText.setError("Invalid quantity");
            return false;
        }

        return true;
    }
}

