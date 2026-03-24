package com.dailyserviceapp.export.generator;

import android.content.Context;
import java.io.File;
import java.util.List;

/**
 * Generic interface for generating Excel documents from a list of data models.
 *
 * @param <T> The data model type (e.g., Bill, ServiceEntry)
 */
public interface ExcelDocumentGenerator<T> {
    
    /**
     * Generates an Excel file from the provided data.
     * 
     * @param context App context for file storage access
     * @param data The list of data objects to export
     * @param fileName Desired output file name (e.g., "Monthly_Bills_January.xlsx")
     * @return File pointer to the generated Excel file stored in the app's cache or external dir
     * @throws Exception If file creation or formatting fails
     */
    File generateExcel(Context context, List<T> data, String fileName) throws Exception;
}
