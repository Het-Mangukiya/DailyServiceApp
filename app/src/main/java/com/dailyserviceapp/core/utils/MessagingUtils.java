package com.dailyserviceapp.core.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dailyserviceapp.data.models.Customer;

import java.util.List;
import java.util.Locale;

/**
 * MessagingUtils — helper for:
 *  • WhatsApp message (direct app or web fallback)
 *  • SMS sending via SmsManager
 *  • Pre-built reminder templates
 *
 * Manifest permission required:
 *   <uses-permission android:name="android.permission.SEND_SMS" />
 */
public final class MessagingUtils {

    private static final String TAG = "MessagingUtils";

    private static final String WA_PACKAGE = "com.whatsapp";
    private static final String WA_BIZ_PACKAGE = "com.whatsapp.w4b";

    private MessagingUtils() { }

    // ────────────────────────────────────────────────────────────────────────
    // WhatsApp
    // ────────────────────────────────────────────────────────────────────────

    public static void sendWhatsApp(@NonNull Context context,
                                    @NonNull String phone,
                                    @NonNull String message) {
        String cleanPhone = sanitizePhone(phone);
        if (cleanPhone.isEmpty()) {
            Log.w(TAG, "sendWhatsApp: invalid phone number");
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + cleanPhone + "?text=" + Uri.encode(message)));
            intent.setPackage(WA_PACKAGE);
            context.startActivity(intent);
            return;
        } catch (ActivityNotFoundException ignored) { }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + cleanPhone + "?text=" + Uri.encode(message)));
            intent.setPackage(WA_BIZ_PACKAGE);
            context.startActivity(intent);
            return;
        } catch (ActivityNotFoundException ignored) { }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + cleanPhone + "?text=" + Uri.encode(message)));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No browser available to open WhatsApp Web", e);
        }
    }

    public static void sendWhatsAppPaymentReminder(@NonNull Context context,
                                                   @NonNull Customer customer,
                                                   double pendingAmount) {
        String msg = buildPaymentReminderMessage(customer.getName(), pendingAmount);
        sendWhatsApp(context, customer.getPhone(), msg);
    }

    public static void sendWhatsAppBillReady(@NonNull Context context,
                                             @NonNull Customer customer,
                                             @NonNull String monthLabel,
                                             double totalAmount) {
        String msg = buildBillReadyMessage(customer.getName(), monthLabel, totalAmount);
        sendWhatsApp(context, customer.getPhone(), msg);
    }

    public static void sendWhatsAppPaymentReceived(@NonNull Context context,
                                                   @NonNull Customer customer,
                                                   double amountPaid) {
        String msg = buildPaymentReceivedMessage(customer.getName(), amountPaid);
        sendWhatsApp(context, customer.getPhone(), msg);
    }

    // ────────────────────────────────────────────────────────────────────────
    // SMS
    // ────────────────────────────────────────────────────────────────────────

    public static void sendSms(@NonNull Context context,
                               @NonNull String phone,
                               @NonNull String message) {
        String cleanPhone = sanitizePhoneForSms(phone);
        if (cleanPhone.isEmpty()) {
            Log.w(TAG, "sendSms: invalid phone number");
            return;
        }
        try {
            SmsManager smsManager = SmsManager.getDefault();
            if (message.length() > 160) {
                smsManager.sendMultipartTextMessage(
                    cleanPhone, null,
                    smsManager.divideMessage(message),
                    null, null);
            } else {
                smsManager.sendTextMessage(cleanPhone, null, message, null, null);
            }
            Log.d(TAG, "SMS sent to " + cleanPhone);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS to " + cleanPhone, e);
        }
    }

    public static void sendSmsPaymentReminder(@NonNull Context context,
                                              @NonNull Customer customer,
                                              double pendingAmount) {
        sendSms(context, customer.getPhone(),
            buildPaymentReminderMessage(customer.getName(), pendingAmount));
    }

    public static void sendSmsBillReady(@NonNull Context context,
                                        @NonNull Customer customer,
                                        @NonNull String monthLabel,
                                        double totalAmount) {
        sendSms(context, customer.getPhone(),
            buildBillReadyMessage(customer.getName(), monthLabel, totalAmount));
    }

    public static void sendBulkSms(@NonNull Context context,
                                   @NonNull List<Customer> customers,
                                   @NonNull String message) {
        for (Customer c : customers) {
            if (c.getPhone() != null && !c.getPhone().isEmpty()) {
                sendSms(context, c.getPhone(), message);
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Message templates
    // ────────────────────────────────────────────────────────────────────────

    public static String buildPaymentReminderMessage(String customerName, double amount) {
        return String.format(Locale.getDefault(),
            "Hi %s, this is a friendly reminder that your payment of ₹%.0f is pending. " +
            "Please clear it at your earliest convenience. Thank you! — DailyService",
            customerName, amount);
    }

    public static String buildBillReadyMessage(String customerName,
                                               String monthLabel,
                                               double totalAmount) {
        return String.format(Locale.getDefault(),
            "Hi %s, your %s bill of ₹%.0f is ready. " +
            "Contact your service provider to view the details. Thank you! — DailyService",
            customerName, monthLabel, totalAmount);
    }

    public static String buildPaymentReceivedMessage(String customerName, double amountPaid) {
        return String.format(Locale.getDefault(),
            "Hi %s, we received your payment of ₹%.0f. Thank you! — DailyService",
            customerName, amountPaid);
    }

    public static String buildVacationMessage(String customerName, String startDate, String endDate) {
        return String.format(
            "Hi %s, your service delivery has been paused from %s to %s. " +
            "It will resume automatically after that. — DailyService",
            customerName, startDate, endDate);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────────────────

    private static String sanitizePhone(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String digits = raw.replaceAll("[^+\\d]", "");
        if (digits.length() == 10 && !digits.startsWith("+")) {
            digits = "+91" + digits;
        }
        return digits;
    }

    private static String sanitizePhoneForSms(@Nullable String raw) {
        String phone = sanitizePhone(raw);
        return phone;
    }
}
