package com.dailyserviceapp.billing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.core.utils.DateUtils;
import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Generates a simple monthly bill PDF for a customer.
 */
public final class MonthlyBillPdfGenerator {

    public static class Result {
        public final File pdfFile;
        public final double serviceTotal;
        public final double paidTotal;
        public final double monthOutstanding;

        Result(File pdfFile, double serviceTotal, double paidTotal, double monthOutstanding) {
            this.pdfFile = pdfFile;
            this.serviceTotal = serviceTotal;
            this.paidTotal = paidTotal;
            this.monthOutstanding = monthOutstanding;
        }
    }

    private MonthlyBillPdfGenerator() {
    }

    public static Result generate(
        Context context,
        Customer customer,
        String providerName,
        String serviceType,
        int month,
        int year,
        List<ServiceEntry> allEntries,
        List<Payment> allPayments
    ) throws IOException {
        if (context == null) {
            throw new IOException("Invalid app context");
        }

        String customerName = customer != null && customer.getName() != null
            ? customer.getName().trim() : "Customer";
        double defaultRate = customer != null ? customer.getRatePerUnit() : 0.0;

        List<ServiceEntry> monthEntries = filterEntriesForMonth(allEntries, month, year);
        List<Payment> monthPayments = filterPaymentsForMonth(allPayments, month, year);

        double serviceTotal = 0.0;
        for (ServiceEntry entry : monthEntries) {
            if (entry == null || !entry.isDelivered()) continue;
            double qty = Math.max(0.0, entry.getQuantity());
            double rate = entry.getRate() > 0 ? entry.getRate() : Math.max(0.0, defaultRate);
            serviceTotal += qty * rate;
        }

        double paidTotal = 0.0;
        for (Payment payment : monthPayments) {
            if (payment == null) continue;
            paidTotal += Math.max(0.0, payment.getAmount());
        }

        double outstanding = Math.max(0.0, serviceTotal - paidTotal);

        String monthLabel = monthYearLabel(month, year);
        String safeProvider = sanitizeFilenamePart(providerName == null ? "Provider" : providerName);
        String safeCustomer = sanitizeFilenamePart(customerName);
        String fileName = "bill_" + safeCustomer + "_" + monthLabel.replace(' ', '_') + ".pdf";

        File docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (docsDir == null) {
            throw new IOException("Unable to access app documents directory");
        }
        if (!docsDir.exists() && !docsDir.mkdirs()) {
            throw new IOException("Unable to create documents directory");
        }

        File outFile = new File(docsDir, fileName);

        PdfDocument document = new PdfDocument();
        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 @72dpi
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint titlePaint = new Paint();
            titlePaint.setColor(Color.BLACK);
            titlePaint.setTextSize(20f);
            titlePaint.setFakeBoldText(true);

            Paint headingPaint = new Paint();
            headingPaint.setColor(Color.BLACK);
            headingPaint.setTextSize(13f);
            headingPaint.setFakeBoldText(true);

            Paint textPaint = new Paint();
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(11f);

            Paint linePaint = new Paint();
            linePaint.setColor(Color.GRAY);
            linePaint.setStrokeWidth(1f);

            int left = 40;
            int right = 555;
            int y = 50;

            canvas.drawText("Monthly Bill", left, y, titlePaint);
            y += 24;
            canvas.drawText(monthLabel, left, y, headingPaint);

            y += 24;
            canvas.drawLine(left, y, right, y, linePaint);

            y += 22;
            canvas.drawText("Provider: " + (providerName == null || providerName.trim().isEmpty() ? "Service Provider" : providerName), left, y, textPaint);
            y += 18;
            canvas.drawText("Customer: " + customerName, left, y, textPaint);
            y += 18;
            canvas.drawText("Service: " + (serviceType == null || serviceType.trim().isEmpty() ? "Service" : serviceType), left, y, textPaint);
            y += 18;
            canvas.drawText("Generated: " + DateUtils.formatShortDate(new Date()), left, y, textPaint);

            y += 20;
            canvas.drawLine(left, y, right, y, linePaint);

            y += 20;
            canvas.drawText("Date", left, y, headingPaint);
            canvas.drawText("Qty", 240, y, headingPaint);
            canvas.drawText("Rate", 320, y, headingPaint);
            canvas.drawText("Amount", 430, y, headingPaint);

            y += 8;
            canvas.drawLine(left, y, right, y, linePaint);
            y += 18;

            int rowsRendered = 0;
            for (ServiceEntry entry : monthEntries) {
                if (entry == null || !entry.isDelivered()) continue;

                String dateStr = entry.getDate() != null ? DateUtils.formatShortDate(entry.getDate().toDate()) : "-";
                double qty = Math.max(0.0, entry.getQuantity());
                double rate = entry.getRate() > 0 ? entry.getRate() : Math.max(0.0, defaultRate);
                double amount = qty * rate;

                canvas.drawText(dateStr, left, y, textPaint);
                canvas.drawText(String.format(Locale.US, "%.2f", qty), 240, y, textPaint);
                canvas.drawText(CurrencyUtils.formatCurrency(rate), 320, y, textPaint);
                canvas.drawText(CurrencyUtils.formatCurrency(amount), 430, y, textPaint);

                y += 16;
                rowsRendered++;
                if (y > 730) {
                    canvas.drawText("(More entries omitted for single-page export)", left, y, textPaint);
                    break;
                }
            }

            if (rowsRendered == 0) {
                canvas.drawText("No delivered entries for this month.", left, y, textPaint);
                y += 16;
            }

            y += 10;
            canvas.drawLine(left, y, right, y, linePaint);

            y += 22;
            canvas.drawText("Service Total: " + CurrencyUtils.formatCurrency(serviceTotal), left, y, headingPaint);
            y += 18;
            canvas.drawText("Payments Received: " + CurrencyUtils.formatCurrency(paidTotal), left, y, headingPaint);
            y += 18;
            canvas.drawText("Outstanding: " + CurrencyUtils.formatCurrency(outstanding), left, y, headingPaint);

            y += 24;
            canvas.drawText("Powered by DailyDrop", left, y, textPaint);

            document.finishPage(page);

            FileOutputStream outputStream = new FileOutputStream(outFile);
            document.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();
        } finally {
            document.close();
        }

        return new Result(outFile, serviceTotal, paidTotal, outstanding);
    }

    private static List<ServiceEntry> filterEntriesForMonth(List<ServiceEntry> entries, int month, int year) {
        List<ServiceEntry> out = new ArrayList<>();
        if (entries == null) return out;
        Calendar cal = Calendar.getInstance();
        for (ServiceEntry entry : entries) {
            if (entry == null || entry.getDate() == null) continue;
            cal.setTime(entry.getDate().toDate());
            if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                out.add(entry);
            }
        }
        return out;
    }

    private static List<Payment> filterPaymentsForMonth(List<Payment> payments, int month, int year) {
        List<Payment> out = new ArrayList<>();
        if (payments == null) return out;
        Calendar cal = Calendar.getInstance();
        for (Payment payment : payments) {
            if (payment == null || payment.getPaymentDate() == null) continue;
            cal.setTime(payment.getPaymentDate().toDate());
            if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                out.add(payment);
            }
        }
        return out;
    }

    private static String monthYearLabel(int month, int year) {
        String[] months = new DateFormatSymbols(Locale.getDefault()).getMonths();
        String monthName = (month >= 0 && month < months.length) ? months[month] : "Month";
        return monthName + " " + year;
    }

    private static String sanitizeFilenamePart(String value) {
        String cleaned = value == null ? "unknown" : value.trim();
        if (cleaned.isEmpty()) cleaned = "unknown";
        return cleaned.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
