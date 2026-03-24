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
 * MessagingUtils — one-stop helper for:
 *
 *   • Sending a WhatsApp message to a customer (opens WhatsApp directly if installed,
 *     falls back to WhatsApp Web).
 *   • Sending a device SMS via SmsManager (no user interaction needed once SEND_SMS
 *     permission is granted).
 *   • Composing pre-built payment-reminder and bill-ready messages.
 *
 * MANIFEST PERMISSIONS REQUIRED:
 *   <uses-permission android:name="android.permission.SEND_SMS" />
 *
 * For SMS on Android 6+, request the permission at runtime before calling sendSms().
 *
 * USAGE EXAMPLES:
 *   // WhatsApp payment reminder
 *   MessagingUtils.sendWhatsAppPaymentReminder(context, customer, 750.0);
 *
 *   // SMS bill ready
 *   MessagingUtils.sendSmsBillReady(context, customer, "March 2026", 1200.0);
 *
 *   // Custom WhatsApp message
 *   MessagingUtils.sendWhatsApp(context, "+919876543210", "Hello from DailyService!");
 *
 *   // Bulk SMS to multiple customers
 *   MessagingUtils.sendBulkSms(context, customerList, "Holiday notice: no delivery tomorrow.");
 */
public final class MessagingUtils {

    private static final String TAG = "MessagingUtils";

    // WhatsApp package — works for both consumer and Business editions
    private static final String WA_PACKAGE       = "com.whatsapp";
    private static final String WA_BIZ_PACKAGE   = "com.whatsapp.w4b";
    private static final String WA_SHARE_ACTION  = "com.whatsapp.intent.action.SEND_TO";

    private MessagingUtils() { /* utility class — do not instantiate */ }

    // ══════════════════════════════════════════════════════════════════════════
    //  WhatsApp
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Opens WhatsApp with a pre-filled message to the given phone number.
     *
     * @param context   Activity context
     * @param phone     Phone number in international format, e.g. "+919876543210"
     *                  (digits only — any spaces or dashes are stripped automatically)
     * @param message   Message to pre-fill in WhatsApp
     */
    public static void sendWhatsApp(@NonNull Context context,
                                     @NonNull String phone,
                                     @NonNull String message) {
        String cleanPhone = sanitizePhone(phone);
        if (cleanPhone.isEmpty()) {
            Log.w(TAG, "sendWhatsApp: invalid phone number");
            return;
        }

        // Primary: direct WhatsApp URI
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + cleanPhone +
                              "?text=" + Uri.encode(message)));
            intent.setPackage(WA_PACKAGE);
            context.startActivity(intent);
            return;
        } catch (ActivityNotFoundException ignored) { /* WhatsApp not installed */ }

        // Fallback: try WhatsApp Business
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + cleanPhone +
                              "?text=" + Uri.encode(message)));
            intent.setPackage(WA_BIZ_PACKAGE);
            context.startActivity(intent);
            return;
        } catch (ActivityNotFoundException ignored) { /* WA Business not installed */ }

        // Final fallback: WhatsApp Web in browser
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + cleanPhone +
                              "?text=" + Uri.encode(message)));
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No browser available to open WhatsApp Web", e);
        }
    }

    /**
     * Sends a WhatsApp payment-reminder message to a customer.
     *
     * @param pendingAmount  Amount still due (e.g. 750.0)
     */
    public static void sendWhatsAppPaymentReminder(@NonNull Context context,
                                                    @NonNull Customer customer,
                                                    double pendingAmount) {
        String msg = buildPaymentReminderMessage(customer.getName(), pendingAmount);
        sendWhatsApp(context, customer.getPhone(), msg);
    }

    /**
     * Sends a WhatsApp message informing the customer that their bill is ready.
     *
     * @param monthLabel  e.g. "March 2026"
     * @param totalAmount Total bill amount
     */
    public static void sendWhatsAppBillReady(@NonNull Context context,
                                              @NonNull Customer customer,
                                              @NonNull String monthLabel,
                                              double totalAmount) {
        String msg = buildBillReadyMessage(customer.getName(), monthLabel, totalAmount);
        sendWhatsApp(context, customer.getPhone(), msg);
    }

    /**
     * Sends a WhatsApp thank-you message after a payment is recorded.
     *
     * @param amountPaid  Amount received
     */
    public static void sendWhatsAppPaymentReceived(@NonNull Context context,
                                                    @NonNull Customer customer,
                                                    double amountPaid) {
        String msg = buildPaymentReceivedMessage(customer.getName(), amountPaid);
        sendWhatsApp(context, customer.getPhone(), msg);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SMS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Sends an SMS directly via SmsManager (silent, no user interaction).
     * Requires SEND_SMS permission — request it at runtime before calling this.
     *
     * Messages longer than 160 characters are split and sent as multi-part SMS.
     */
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
                // Multi-part SMS
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

    /**
     * Sends a payment-reminder SMS to a customer.
     */
    public static void sendSmsPaymentReminder(@NonNull Context context,
                                               @NonNull Customer customer,
                                               double pendingAmount) {
        sendSms(context, customer.getPhone(),
                buildPaymentReminderMessage(customer.getName(), pendingAmount));
    }

    /**
     * Sends a bill-ready SMS to a customer.
     */
    public static void sendSmsBillReady(@NonNull Context context,
                                         @NonNull Customer customer,
                                         @NonNull String monthLabel,
                                         double totalAmount) {
        sendSms(context, customer.getPhone(),
                buildBillReadyMessage(customer.getName(), monthLabel, totalAmount));
    }

    /**
     * Sends the same SMS message to multiple customers in one call.
     * Long messages are split automatically by SmsManager.
     *
     * @param customers  List of customers — those with a null/empty phone are skipped.
     * @param message    Message to send to everyone
     */
    public static void sendBulkSms(@NonNull Context context,
                                    @NonNull List<Customer> customers,
                                    @NonNull String message) {
        for (Customer c : customers) {
            if (c.getPhone() != null && !c.getPhone().isEmpty()) {
                sendSms(context, c.getPhone(), message);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Message templates
    // ══════════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Strips everything except digits and leading '+'.
     * Ensures Indian numbers without country code get +91 prepended.
     */
    private static String sanitizePhone(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String digits = raw.replaceAll("[^+\\d]", "");
        // If local 10-digit Indian number, prepend country code
        if (digits.length() == 10 && !digits.startsWith("+")) {
            digits = "+91" + digits;
        }
        return digits;
    }

    /** For SmsManager, the number should not have '+' on some devices. */
    private static String sanitizePhoneForSms(@Nullable String raw) {
        String phone = sanitizePhone(raw);
        // Keep the '+' — SmsManager handles international format on modern Android
        return phone;
    }
}
