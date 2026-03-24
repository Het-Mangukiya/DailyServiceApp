package com.dailyserviceapp.export.core;

import android.content.Context;
import com.dailyserviceapp.data.models.Bill;
import java.util.List;

/**
 * Central manager for coordinating Excel export operations.
 * Responsible for threading, executing the correct generator, and returning results via callbacks.
 */
public class ExportManager {

    public interface ExportCallback {
        void onSuccess(String filePath);
        void onError(Exception e);
    }

    /**
     * Trigger bill export to Excel on a background thread.
     * 
     * @param context Android context
     * @param bills List of bills to export
     * @param callback Callback returning the generated file path or an error
     */
    public void exportBillsToExcel(Context context, List<Bill> bills, ExportCallback callback) {
        // Codex: Implement background threading and use BillExcelGenerator here.
    }
    
    // Codex: Add additional export trigger methods here as needed (e.g., exportServiceHistoryToExcel)
}
