package com.dailyserviceapp.core.utils;

/**
 * Application-wide constants for DailyDrop.
 * Contains shared preferences keys, user roles, service types,
 * payment statuses and methods, notification types, and Firebase collection names.
 * 
 * <p>This class should not be instantiated. All members are static final constants.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class Constants {
    
    // Shared Preferences
    /** SharedPreferences file name */
    public static final String PREF_NAME = "DailyServicePrefs";
    
    /** Key for storing user ID */
    public static final String KEY_USER_ID = "userId";
    
    /** Key for storing user role */
    public static final String KEY_USER_ROLE = "userRole";
    
    /** Key for storing user email */
    public static final String KEY_USER_EMAIL = "userEmail";
    
    /** Key for storing user name */
    public static final String KEY_USER_NAME = "userName";
    
    /** Key for storing login status */
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    
    // User Roles
    /** Service provider role constant */
    public static final String ROLE_PROVIDER = "PROVIDER";
    
    /** Customer role constant */
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    
    // Service Types
    /** Milk delivery service */
    public static final String SERVICE_MILK = "Milk";
    
    /** Maid service */
    public static final String SERVICE_MAID = "Maid";
    
    /** Newspaper delivery service */
    public static final String SERVICE_NEWSPAPER = "Newspaper";
    
    /** Laundry service */
    public static final String SERVICE_LAUNDRY = "Laundry";
    
    // Payment Status
    /** Payment pending status */
    public static final String PAYMENT_PENDING = "PENDING";
    
    /** Partial payment received */
    public static final String PAYMENT_PARTIAL = "PARTIAL";
    
    /** Full payment received */
    public static final String PAYMENT_PAID = "PAID";
    
    /** Payment overdue status */
    public static final String PAYMENT_OVERDUE = "OVERDUE";
    
    // Payment Methods
    /** Cash payment method */
    public static final String PAYMENT_CASH = "Cash";
    
    /** UPI payment method */
    public static final String PAYMENT_UPI = "UPI";
    
    /** Bank transfer payment method */
    public static final String PAYMENT_BANK_TRANSFER = "Bank Transfer";
    
    /** Cheque payment method */
    public static final String PAYMENT_CHEQUE = "Cheque";
    
    /** Other payment method */
    public static final String PAYMENT_OTHER = "Other";
    
    // Notification Types
    /** Bill generated notification */
    public static final String NOTIF_BILL_GENERATED = "BILL_GENERATED";
    
    /** Payment reminder notification */
    public static final String NOTIF_PAYMENT_REMINDER = "PAYMENT_REMINDER";
    
    /** Payment received notification */
    public static final String NOTIF_PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    
    /** Service delivery notification */
    public static final String NOTIF_SERVICE_DELIVERY = "SERVICE_DELIVERY";
    
    // Firebase Collections
    /** Users collection in Firestore */
    public static final String COLLECTION_USERS = "users";
    
    /** Providers collection in Firestore */
    public static final String COLLECTION_PROVIDERS = "providers";
    
    /** Customers collection in Firestore */
    public static final String COLLECTION_CUSTOMERS = "customers";
    
    /** Service entries collection in Firestore */
    public static final String COLLECTION_SERVICE_ENTRIES = "serviceEntries";
    
    /** Bills collection in Firestore */
    public static final String COLLECTION_BILLS = "bills";
    
    /** Payments collection in Firestore */
    public static final String COLLECTION_PAYMENTS = "payments";
    
    /** Notifications collection in Firestore */
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    
    // Date Formats
    /** Full date-time format (dd MMM yyyy, hh:mm a) */
    public static final String DATE_FORMAT_FULL = "dd MMM yyyy, hh:mm a";
    
    /** Short date format (dd MMM yyyy) */
    public static final String DATE_FORMAT_SHORT = "dd MMM yyyy";
    
    /** Month and year format (MMM yyyy) */
    public static final String DATE_FORMAT_MONTH_YEAR = "MMM yyyy";
    
    /** Day and month format (dd MMM) */
    public static final String DATE_FORMAT_DAY_MONTH = "dd MMM";
    
    // Validation
    /** Minimum password length */
    public static final int MIN_PASSWORD_LENGTH = 8;
    
    /** Maximum phone number length (Indian standard) */
    public static final int MAX_PHONE_LENGTH = 10;
    
    // Intent Extras
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_CUSTOMER_ID = "customerId";
    public static final String EXTRA_BILL_ID = "billId";
    public static final String EXTRA_PAYMENT_ID = "paymentId";
    public static final String EXTRA_SERVICE_ENTRY_ID = "serviceEntryId";
    public static final String EXTRA_MONTH = "month";
    public static final String EXTRA_YEAR = "year";
    
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
