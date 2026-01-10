# DailyServiceApp (DailyDrop) - AI Coding Agent Instructions

## Project Overview
**App Name**: DailyDrop  
**Platform**: Android (Java + XML)  
**Primary Role**: Service Provider (track deliveries, manage billing)  
**Secondary Role**: Customer (read-only views)

Android app for service providers to track daily deliveries (milk, newspaper, maid, laundry) and automate billing. Think PhonePe/GPay-inspired UI for daily service management.

**Core Flow**: Provider adds customers → marks daily deliveries → auto-generates monthly bills → tracks payments.

## UI Structure & Navigation

### Navigation Drawer (Left Menu)
**DrawerLayout + NavigationView** - Central navigation hub

**Drawer Header**:
- Provider name, email/phone
- Profile avatar
- Green gradient background (trust & daily-use feel)

**Menu Items**:
- **My Profile** → Provider Profile
- **Services** → Dashboard/Home (customer list)
- **Service Entry** → Daily delivery marking (Calendar icon shortcut)
- **Bills** → Monthly billing summary
- **Reports** → Analytics
- **Customer Requests** → (future feature)
- **Deleted History** → Deleted service entries
- **Share** / **Rate Us** / **Terms** / **Privacy** / **Language** / **Logout**

**Key Files**: [activity_dashboard.xml](app/src/main/res/layout/activity_dashboard.xml), [nav_header.xml](app/src/main/res/layout/nav_header.xml)

### Dashboard/Home Screen ("Services")
**Top App Bar**:
- Title: "Services"
- Menu icon (opens drawer)
- Notification icon
- Calendar icon (Service Entry shortcut)

**Summary Cards**:
- **Total Services** → Number of active customers
- **₹ Total Value** → Total revenue (current month)
- Material cards with rounded corners, gradients

**Search Bar**: Search customers by name (critical for large lists)

**Customer List** (RecyclerView):
- Customer name
- Service type (Milk/Newspaper/Maid)
- Rate × Quantity
- Today's status (Delivered/Pending)

**Empty State** (CRITICAL UX):
```
"No customers yet
Tap 'Add Customer' to get started"
```
**Why**: Examiners expect good empty states. Always show this when no data exists.

**FAB**: `+ Add Customer` (ExtendedFloatingActionButton) - Most-used action, one-hand access

### Add Customer Screen
**Pattern**: BottomSheetDialogFragment (NOT new Activity)

**Fields**:
- Customer photo (optional) → Image picker
- Customer Name
- Address / Notes
- Service Type (dropdown)
- Quantity (1L, 500ml, etc.)
- Rate per delivery (₹)

**Behavior**:
- Saves to Firestore immediately
- Customer appears instantly in Dashboard & Service Entry
- Validation before save (use `ValidationUtils`)

**Key Files**: [dialog_add_product.xml](app/src/main/res/layout/dialog_add_product.xml) (rename to dialog_add_customer.xml)

## Architecture & Structure

### Clean Architecture Pattern
```
Presentation (UI) → Domain (Business Logic) → Data (Firebase/Room)
```

**Package Organization**:
- `core/` - Base classes (`BaseActivity`), utilities (`DateUtils`, `ValidationUtils`, `CurrencyUtils`), constants
- `auth/` - Login, signup, password reset activities
- `dashboard/` - Main screen with customer list and analytics
- `customer/` - Customer CRUD operations
- `service/` - Daily delivery entry (`ServiceEntryActivity`)
- `billing/` - Bill generation and viewing
- `payment/` - Payment tracking
- `reports/` - Analytics and reports
- `data/` - `FirestoreRepository` (single source for all Firestore operations), models
- `ui/` - Shared components (legacy, being phased out)

### Key Base Classes
**ALL activities MUST extend `BaseActivity`** which provides:
- `preferenceManager` - User session management (`.getUserId()`, `.isLoggedIn()`, `.isProvider()`)
- `networkMonitor` - Connectivity checking (`.isNetworkAvailable()`)
- Common methods: `showToast()`, `showNetworkError()`, `getCurrentUserId()`
- Session checks: Always check `isLoggedIn()` in `onCreate()` before loading data

**Example Pattern**:
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_example);
    
    if (!isLoggedIn()) {
        navigateToLogin();
        return;
    }
    
    String userId = getCurrentUserId();  // From BaseActivity
    // Continue initialization...
}
```

## Critical Patterns

### Firebase Firestore Data Model
**Collection Structure** (see [REQUIREMENTS_AND_ARCHITECTURE.md](REQUIREMENTS_AND_ARCHITECTURE.md)):
```
customers/{customerId}
  ├─ fields: name, phone, address, serviceType, ratePerUnit, createdAt
  └─ subcollections:
      ├─ deliveries/{yyyyMMdd} - daily entries (delivered: boolean, quantity: double)
      ├─ bills/{yyyyMM} - monthly bills (totalAmount, items[], status)
      └─ payments/{yyyyMM} - payment records (paid: boolean, paidAmount, paidOn)
```

**Always use `FirestoreRepository`** for database operations - never call Firestore directly in activities:
```java
FirestoreRepository repository = new FirestoreRepository();
repository.loadCustomers(providerId, 
    customers -> { /* success */ },
    error -> { /* failure */ }
);
```

### Date Handling
- Use `DateUtils` for all date operations (formatting, parsing, key generation)
- Firestore stores `Timestamp` objects (not `Date` or `LocalDateTime`)
- Date keys format: `yyyyMMdd` for deliveries, `yyyyMM` for bills/payments
- Example: `DateUtils.getDateKey(new Date())` → "20260110"

### UI Design System
Follow [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) strictly:
- **Colors**: Primary `#0891B2` (Trust Blue), Success `#10B981`, Error `#EF4444`
- **Typography**: 32sp (hero) → 24sp (screen titles) → 16sp (body)
- **Spacing**: 8dp grid (4dp, 8dp, 16dp, 24dp, 32dp)
- **Corners**: 12dp buttons, 16dp cards, 20dp stat cards
- **Touch targets**: Minimum 48dp height for buttons/clickable elements
- **NO** all-caps text on buttons (Material 3 style)

### Modern UI Enhancements
Recently modernized with (see [MODERNIZATION_SUMMARY.md](MODERNIZATION_SUMMARY.md)):
- Shimmer loading screens (`SkeletonProductAdapter`)
- Haptic feedback on interactions (`performHapticFeedback(HapticFeedbackConstants.CLICK)`)
- Gradient headers and cards
- Smooth animations (`fade_in.xml`, `slide_up.xml`, `scale_in.xml`)
- Animated number counting for stats

**When adding new screens**: Include skeleton loading, haptic feedback, and smooth transitions.

## Build & Development

### Build Commands
```bash
# Clean build (always run after dependency changes)
./gradlew clean assembleDebug

# Install to device
./gradlew installDebug
# OR: adb install -r app/build/outputs/apk/debug/app-debug.apk

# Check for errors
./gradlew lint
```

### Dependencies (app/build.gradle)
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 24 (Android 7.0)
- **Java**: Version 17
- **ViewBinding**: Enabled (use binding instead of `findViewById`)
- **Key Libraries**: Firebase (Auth, Firestore, Storage, Messaging), Room, WorkManager, iText7 (PDF), MPAndroidChart, Glide, Shimmer

### Firebase Setup
**CRITICAL**: App won't work without proper Firebase configuration.
1. Download `google-services.json` from Firebase Console
2. Place at `app/google-services.json`
3. Enable: Authentication (Email/Password), Firestore Database, Cloud Messaging
4. See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for troubleshooting

## Common Tasks

### Adding a New Activity
1. Extend `BaseActivity` (NOT `AppCompatActivity`)
2. Add to `AndroidManifest.xml`
3. Check `isLoggedIn()` in `onCreate()`
4. Use `FirestoreRepository` for data operations
5. Follow design system colors/spacing
6. Add skeleton loading state
7. Include haptic feedback on buttons

### Adding New Firestore Operations
**Always add to `FirestoreRepository.java`** - keep all DB logic centralized:
```java
public void loadCustomers(String providerId, 
                         OnCustomersLoadedListener onSuccess, 
                         OnFailureListener onFailure) {
    db.collection("customers")
      .whereEqualTo("providerId", providerId)
      .get()
      .addOnSuccessListener(snapshot -> {
          List<Customer> customers = snapshot.toObjects(Customer.class);
          onSuccess.onCustomersLoaded(customers);
      })
      .addOnFailureListener(onFailure);
}
```

### Service Entry Flow
**ServiceEntryActivity** is the core daily workflow:

**Access**: Calendar icon OR drawer menu item

**Date Rules**:
- ✅ Today & Past only
- ❌ Future dates disabled (validation required)

**UI**:
1. Shows all customers for selected date (default: today)
2. Inline quantity controls (+ / - buttons)
3. Checkboxes for delivery status
4. "Mark Delivered" button for bulk operations
5. **Auto-saves** to Firestore on every change
6. Supports undo for bulk operations (Snackbar with undo action)

**On Mark Delivery**:
```java
deliveryCost = rate × quantity;
lentAmount += deliveryCost;  // Track monthly revenue
```

**Validations**:
- One entry per customer per day
- No future marking
- No manual bill editing (bills are auto-calculated)

**Key Files**: [ServiceEntryActivity.java](app/src/main/java/com/dailyserviceapp/service/ServiceEntryActivity.java), [ServiceEntryAdapter.java](app/src/main/java/com/dailyserviceapp/service/ServiceEntryAdapter.java), [activity_service_entry.xml](app/src/main/res/layout/activity_service_entry.xml)

### Billing & Summary Screen
**Shows**:
- Monthly total (auto-calculated)
- Paid amount
- Pending amount
- Per-customer breakdown

**Pattern**: Read-only for accuracy - prevents manual tampering

**Key Files**: [BillListActivity.java](app/src/main/java/com/dailyserviceapp/billing/BillListActivity.java)

### Validation
Use `ValidationUtils` for all input validation:
- `.isValidEmail(email)` - Email format
- `.isValidPhone(phone)` - Indian phone numbers
- `.isValidAmount(amount)` - Positive numbers
- Always validate BEFORE calling repository methods

### Offline Support
- Check `isNetworkAvailable()` before network operations
- Show offline banner when disconnected
- Firebase handles offline caching automatically
- Use `WorkManager` for background sync tasks

## Project-Specific Conventions

### Naming
- Activities: `<Feature>Activity` (e.g., `ServiceEntryActivity`)
- Adapters: `<Feature>Adapter` (e.g., `CustomerAdapter`)
- Layouts: `activity_<feature>.xml`, `row_<item>.xml`, `dialog_<name>.xml`
- Models: Plain nouns (e.g., `Customer`, `Bill`, `ServiceEntry`)

### Error Handling
```java
repository.someOperation(
    result -> { /* Success */ },
    error -> {
        Log.e(TAG, "Operation failed", error);
        showToast(error.getMessage());
        hideLoading();
    }
);
```

### Loading States
Always show loading indicators for async operations:
```java
showLoading();  // Show progress bar
repository.loadData(
    data -> {
        hideLoading();
        // Update UI
    },
    error -> {
        hideLoading();
        showToast("Error: " + error.getMessage());
    }
);
```

## Testing & Debugging

### Common Issues
1. **Firebase errors**: Check `google-services.json` exists and Firebase services enabled
2. **Build failures**: Run `./gradlew clean` first
3. **Null user ID**: User not logged in, check `isLoggedIn()` first
4. **Date formatting**: Always use `DateUtils`, never manual formatting

### Debug Logging
Use consistent tag pattern:
```java
private static final String TAG = "ServiceEntryActivity";
Log.d(TAG, "Loading entries for date: " + dateKey);
```

## Important Files
- [REQUIREMENTS_AND_ARCHITECTURE.md](REQUIREMENTS_AND_ARCHITECTURE.md) - Full feature spec, data models, NFRs
- [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) - UI standards, colors, typography
- [MODERNIZATION_SUMMARY.md](MODERNIZATION_SUMMARY.md) - Recent UI improvements
- [FIREBASE_SETUP.md](FIREBASE_SETUP.md) - Firebase configuration guide

## Implementation Order (CRITICAL)

**❌ Do NOT build everything at once**  
**✅ Build in this order**:

1. **Drawer + Home UI** - Navigation structure and dashboard layout
2. **Customer List + Empty State** - RecyclerView with proper empty state handling
3. **Add Customer Bottom Sheet** - BottomSheetDialogFragment for adding customers
4. **Service Entry Logic** - Core daily delivery marking (most important)
5. **Billing Summary** - Auto-calculated monthly bills (read-only)
6. **Customer Role** - Read-only views for customers (optional/later phase)

This order ensures:
- Each phase is testable independently
- Critical features (service entry) are prioritized
- Foundation is solid before adding complexity

## When Making Changes
1. Maintain consistency with existing patterns (always check similar existing code first)
2. Use `BaseActivity` methods instead of duplicating logic
3. Keep all Firestore operations in `FirestoreRepository`
4. Follow Material 3 design system (no custom colors/sizes without checking DESIGN_SYSTEM.md)
5. Test both online and offline scenarios
6. Add proper error handling and loading states
7. Include haptic feedback and animations for modern feel
