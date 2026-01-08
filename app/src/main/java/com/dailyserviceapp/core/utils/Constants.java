package com.dailyserviceapp.core.utils;

public class Constants {
    
    // Shared Preferences
    public static final String PREF_NAME = "DailyServicePrefs";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USER_ROLE = "userRole";
    public static final String KEY_USER_EMAIL = "userEmail";
    public static final String KEY_USER_NAME = "userName";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    
    // User Roles
    public static final String ROLE_PROVIDER = "PROVIDER";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    
    // Service Types
    public static final String SERVICE_MILK = "Milk";
    public static final String SERVICE_MAID = "Maid";
    public static final String SERVICE_NEWSPAPER = "Newspaper";
    public static final String SERVICE_LAUNDRY = "Laundry";
    
    // Payment Status
    public static final String PAYMENT_PENDING = "PENDING";
    public static final String PAYMENT_PARTIAL = "PARTIAL";
    public static final String PAYMENT_PAID = "PAID";
    public static final String PAYMENT_OVERDUE = "OVERDUE";
    
    // Payment Methods
    public static final String PAYMENT_CASH = "Cash";
    public static final String PAYMENT_UPI = "UPI";
    public static final String PAYMENT_BANK_TRANSFER = "Bank Transfer";
    public static final String PAYMENT_CHEQUE = "Cheque";
    public static final String PAYMENT_OTHER = "Other";
    
    // Notification Types
    public static final String NOTIF_BILL_GENERATED = "BILL_GENERATED";
    public static final String NOTIF_PAYMENT_REMINDER = "PAYMENT_REMINDER";
    public static final String NOTIF_PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    public static final String NOTIF_SERVICE_DELIVERY = "SERVICE_DELIVERY";
    
    // Firebase Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PROVIDERS = "providers";
    public static final String COLLECTION_CUSTOMERS = "customers";
    public static final String COLLECTION_SERVICE_ENTRIES = "serviceEntries";
    public static final String COLLECTION_BILLS = "bills";
    public static final String COLLECTION_PAYMENTS = "payments";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    
    // Intent Extras
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_CUSTOMER_ID = "customerId";
    public static final String EXTRA_BILL_ID = "billId";
    public static final String EXTRA_PAYMENT_ID = "paymentId";
    public static final String EXTRA_SERVICE_ENTRY_ID = "serviceEntryId";
    public static final String EXTRA_MONTH = "month";
    public static final String EXTRA_YEAR = "year";
    
    // Date Formats
    public static final String DATE_FORMAT_FULL = "dd MMM yyyy, hh:mm a";
    public static final String DATE_FORMAT_SHORT = "dd MMM yyyy";
    public static final String DATE_FORMAT_MONTH_YEAR = "MMM yyyy";
    public static final String DATE_FORMAT_DAY_MONTH = "dd MMM";
    
    // Validation
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PHONE_LENGTH = 10;
    
    // Pagination
    public static final int PAGE_SIZE = 50;
    
    // WorkManager Tags
    public static final String WORK_TAG_BILL_GENERATION = "bill_generation";
    public static final String WORK_TAG_PAYMENT_REMINDER = "payment_reminder";
    public static final String WORK_TAG_DATA_SYNC = "data_sync";
    
    // Default Values
    public static final double DEFAULT_RATE = 0.0;
    public static final int DEFAULT_QUANTITY = 1;
    
    private Constants() {
        // Private constructor to prevent instantiation
    }
}
