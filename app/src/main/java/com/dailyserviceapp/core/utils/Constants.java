package com.dailyserviceapp.core.utils;

/**
 * Application-wide constants for DailyDrop.
 */
public class Constants {
    
    // Shared Preferences
    public static final String PREF_NAME = "DailyServicePrefs";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USER_ROLE = "userRole";
    public static final String KEY_USER_EMAIL = "userEmail";
    public static final String KEY_USER_NAME = "userName";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    // Dashboard Cache (SharedPreferences)
    public static final String PREF_DASHBOARD_TOTAL_CUSTOMERS = "dash_total_customers";
    public static final String PREF_DASHBOARD_TODAY_DELIVERED = "dash_today_delivered";
    public static final String PREF_DASHBOARD_TODAY_AMOUNT = "dash_today_amount";
    public static final String PREF_DASHBOARD_MONTHLY_REVENUE = "dash_monthly_revenue";

    public static final String PREF_PROVIDER_TODAY_DELIVERED = "provider_today_delivered";
    public static final String PREF_PROVIDER_TODAY_EARNINGS = "provider_today_earnings";
    public static final String PREF_PROVIDER_TOTAL_LENT = "provider_total_lent";
    public static final String PREF_PROVIDER_TOTAL_RECEIVED = "provider_total_received";
    public static final String PREF_PROVIDER_PENDING_AMOUNT = "provider_pending_amount";
    public static final String PREF_PROVIDER_MONTHLY_EARNINGS = "provider_monthly_earnings";
    public static final String PREF_PROVIDER_MONTHLY_DELIVERIES = "provider_monthly_deliveries";
    
    // User Roles
    public static final String ROLE_PROVIDER = "PROVIDER";
    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_ADMIN = "ADMIN";
    
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
    
    // Admin Notification Types (Push Notifications)
    public static final String NOTIF_JOIN_REQUEST = "JOIN_REQUEST";
    public static final String NOTIF_JOIN_REQUEST_STATUS = "JOIN_REQUEST_STATUS";
    public static final String NOTIF_BULK_ORDER = "BULK_ORDER";
    public static final String NOTIF_QUANTITY_REQUEST = "QUANTITY_REQUEST";
    public static final String NOTIF_QUANTITY_RESPONSE = "QUANTITY_RESPONSE";
    public static final String NOTIF_SUPPORT_TICKET = "SUPPORT_TICKET";
    public static final String NOTIF_SUPPORT_UPDATE = "SUPPORT_UPDATE";
    
    // Firebase Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PROVIDERS = "providers";
    public static final String COLLECTION_CUSTOMERS = "customers";
    public static final String COLLECTION_CUSTOMER_LINKS = "customerLinks";
    public static final String COLLECTION_SERVICE_ENTRIES = "serviceEntries";
    public static final String COLLECTION_BILLS = "bills";
    public static final String COLLECTION_PAYMENTS = "payments";
    public static final String COLLECTION_SUPPORT_TICKETS = "supportTickets";
    public static final String COLLECTION_CUSTOMER_INVITES = "customerInvites";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_QUANTITY_REQUESTS = "quantityRequests";
    public static final String COLLECTION_JOIN_REQUESTS = "joinRequests";
    public static final String COLLECTION_BULK_ORDERS = "bulkOrders";
    
    // Date Formats
    public static final String DATE_FORMAT_FULL = "dd MMM yyyy, hh:mm a";
    public static final String DATE_FORMAT_SHORT = "dd MMM yyyy";
    public static final String DATE_FORMAT_MONTH_YEAR = "MMM yyyy";
    public static final String DATE_FORMAT_DAY_MONTH = "dd MMM";
    
    // Validation
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PHONE_LENGTH = 10;
    
    // Intent Extras
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_CUSTOMER_ID = "customerId";
    public static final String EXTRA_BILL_ID = "billId";
    public static final String EXTRA_PAYMENT_ID = "paymentId";
    public static final String EXTRA_SERVICE_ENTRY_ID = "serviceEntryId";
    public static final String EXTRA_ORDER_ID = "orderId";
    public static final String EXTRA_MONTH = "month";
    public static final String EXTRA_YEAR = "year";
    public static final String EXTRA_FORCE_PROFILE_SETUP = "forceProfileSetup";
    public static final String EXTRA_SKIP_SPLASH_DELAY = "skip_splash_delay";

    // Invite flow cache keys
    public static final String KEY_PENDING_INVITE_TOKEN = "pendingInviteToken";
    public static final String KEY_PENDING_INVITE_CUSTOMER_ID = "pendingInviteCustomerId";
    public static final String KEY_LAST_NOTIFICATION_ALERT_PREFIX = "lastNotificationAlert_";
    
    // Pagination
    public static final int PAGE_SIZE = 50;
    
    // WorkManager Tags
    public static final String WORK_TAG_BILL_GENERATION = "bill_generation";
    public static final String WORK_TAG_PAYMENT_REMINDER = "payment_reminder";
    public static final String WORK_TAG_DATA_SYNC = "data_sync";
    
    // Default Values
    public static final double DEFAULT_RATE = 0.0;
    public static final int DEFAULT_QUANTITY = 1;
    
    private Constants() {}
}
