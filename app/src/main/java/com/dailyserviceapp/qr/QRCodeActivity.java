package com.dailyserviceapp.qr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.dailyserviceapp.R;
import com.dailyserviceapp.core.base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QRCodeActivity extends BaseActivity {

    private ImageView qrCodeImage;
    private TextView providerName;
    private TextView providerId;
    private MaterialButton shareButton;
    private MaterialButton saveButton;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private String currentProviderId;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);

        initializeViews();
        setupToolbar();
        setupFirebase();
        loadProviderData();
    }

    private void initializeViews() {
        qrCodeImage = findViewById(R.id.qrCodeImage);
        providerName = findViewById(R.id.providerName);
        providerId = findViewById(R.id.providerId);
        shareButton = findViewById(R.id.shareButton);
        saveButton = findViewById(R.id.saveButton);

        shareButton.setOnClickListener(v -> shareQRCode());
        saveButton.setOnClickListener(v -> saveQRCode());
    }

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void loadProviderData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showToast("Please login first");
            finish();
            return;
        }

        currentProviderId = user.getUid();

        // Load provider details from Firestore
        firestore.collection("providers")
                .document(currentProviderId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String businessName = document.getString("businessName");
                        
                        String displayName = businessName != null && !businessName.isEmpty() 
                                ? businessName 
                                : (name != null ? name : "Service Provider");
                        
                        providerName.setText(displayName);
                        providerId.setText("ID: " + currentProviderId.substring(0, 8).toUpperCase());
                        ensureProviderRecord(displayName);
                        
                        // Generate QR Code
                        generateQRCode(currentProviderId);
                    } else {
                        // Use basic user info if provider document doesn't exist
                        String displayName = user.getDisplayName() != null 
                                ? user.getDisplayName() 
                                : "Service Provider";
                        providerName.setText(displayName);
                        providerId.setText("ID: " + currentProviderId.substring(0, 8).toUpperCase());
                        ensureProviderRecord(displayName);
                        
                        // Generate QR Code
                        generateQRCode(currentProviderId);
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load provider data");
                    // Still generate QR code with basic info
                    String displayName = user.getDisplayName() != null 
                            ? user.getDisplayName() 
                            : "Service Provider";
                    providerName.setText(displayName);
                    providerId.setText("ID: " + currentProviderId.substring(0, 8).toUpperCase());
                    ensureProviderRecord(displayName);
                    generateQRCode(currentProviderId);
                });
    }

    private void ensureProviderRecord(String displayName) {
        if (currentProviderId == null || currentProviderId.trim().isEmpty()) {
            return;
        }

        Map<String, Object> providerData = new HashMap<>();
        providerData.put("id", currentProviderId);
        providerData.put("userId", currentProviderId);
        providerData.put("providerCode", shortProviderCode(currentProviderId));
        providerData.put("updatedAt", System.currentTimeMillis());

        if (displayName != null && !displayName.trim().isEmpty()) {
            providerData.put("name", displayName.trim());
        }

        firestore.collection("providers")
            .document(currentProviderId)
            .set(providerData, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener(e -> {
                Log.e("QRCodeActivity", "Failed to ensure provider record for " + currentProviderId, e);
                showToast("Failed to sync provider code");
            });
    }

    private String shortProviderCode(String id) {
        if (id == null || id.trim().isEmpty()) return "";
        String trimmed = id.trim();
        if (trimmed.length() <= 8) return trimmed.toUpperCase(Locale.US);
        return trimmed.substring(0, 8).toUpperCase(Locale.US);
    }

    private void generateQRCode(String data) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            
            qrCodeImage.setImageBitmap(qrBitmap);
        } catch (WriterException e) {
            showToast("Failed to generate QR code");
            e.printStackTrace();
        }
    }

    private void shareQRCode() {
        if (qrBitmap == null) {
            showToast("QR code not generated yet");
            return;
        }

        try {
            // Save to cache directory
            File cachePath = new File(getCacheDir(), "qr_codes");
            cachePath.mkdirs();
            File qrFile = new File(cachePath, "my_qr_code.png");
            
            FileOutputStream stream = new FileOutputStream(qrFile);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, 
                    getPackageName() + ".fileprovider", qrFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                    "Scan this QR code to join my service!\nProvider ID: " + currentProviderId);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share QR Code"));
        } catch (IOException e) {
            showToast("Failed to share QR code");
            e.printStackTrace();
        }
    }

    private void saveQRCode() {
        if (qrBitmap == null) {
            showToast("QR code not generated yet");
            return;
        }

        try {
            String fileName = "QRCode_" + System.currentTimeMillis() + ".png";
            String savedImageURL = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    qrBitmap,
                    fileName,
                    "My QR Code"
            );

            if (savedImageURL != null) {
                showToast("QR code saved to gallery");
            } else {
                showToast("Failed to save QR code");
            }
        } catch (Exception e) {
            showToast("Failed to save QR code");
            e.printStackTrace();
        }
    }
}
