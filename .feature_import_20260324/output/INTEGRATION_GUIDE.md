# DailyServiceApp — Feature Integration Guide
# 5 Features: FCM Notifications · Notification Bell · SMS · WhatsApp · Google Maps Route

---

## OVERVIEW OF FILES

```
output/
├── notifications/
│   ├── FCMService.java                    → replaces existing FCMService.java
│   ├── NotificationListActivity.java      → replaces existing NotificationListActivity.java
│   ├── DashboardActivity_NotificationPatch.java  → instructions to patch DashboardActivity
│   ├── activity_notification_list.xml     → replaces existing layout
│   ├── row_notification.xml               → NEW row layout
│   └── menu_notifications.xml             → NEW menu resource
│
├── whatsapp/
│   └── MessagingUtils.java                → NEW utility class (drop-in)
│
└── maps/
    ├── RouteOptimizationActivity.java     → NEW activity
    ├── activity_route_optimization.xml    → NEW layout
    ├── row_route_stop.xml                 → NEW row layout
    └── menu_route.xml                     → NEW menu resource
```

---

## FEATURE 1 — Fix FCM Notifications
### File: FCMService.java
**Destination:** `app/src/main/java/com/dailyserviceapp/notifications/FCMService.java`
**Action:** Replace the existing file entirely.

**What it now does:**
- Creates 3 notification channels (General, Billing, Delivery) on startup
- Receives push notification data payload AND notification payload from Firebase
- Shows a heads-up system notification with the correct channel
- Saves every received notification to Firestore (`notifications` collection)
  so it appears in the in-app list
- On token refresh, saves the new FCM token to the user's Firestore document

**After replacing, also update LoginActivity.java:**
After a successful sign-in, add this call so the token is saved even if it
refreshed before the user logged in:

```java
// In LoginActivity, after FirebaseAuth sign-in succeeds:
com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
    .addOnSuccessListener(token ->
        com.dailyserviceapp.notifications.FCMService.saveTokenToFirestore(token));
```

---

## FEATURE 2 — Fix Notification Bell Tap + Unread Badge
### File: DashboardActivity_NotificationPatch.java
**Action:** Apply the 6 patch steps described inside the file to DashboardActivity.java

**Summary of changes:**
1. Import `NotificationListActivity` and `ListenerRegistration`
2. Add two fields: `notifListener` and `unreadNotifCount`
3. Call `loadUnreadNotificationCount()` at end of `onCreate()`
4. Handle `R.id.action_notifications` in `onOptionsItemSelected` → opens NotificationListActivity
5. Add `loadUnreadNotificationCount()` method that listens to unread count in real-time
6. Add `updateNotificationBadge()` method that shows a numeric badge on the bell icon
7. Remove listener in `onDestroy()`

---

## FEATURE 3 — Full Notification List Screen
### Files: NotificationListActivity.java + layouts

**Destinations:**
- `app/src/main/java/com/dailyserviceapp/notifications/NotificationListActivity.java`
- `app/src/main/res/layout/activity_notification_list.xml`
- `app/src/main/res/layout/row_notification.xml`   ← NEW file
- `app/src/main/res/menu/menu_notifications.xml`   ← NEW file

**What it now does:**
- Real-time Firestore listener — notifications appear instantly
- Unread items shown at full opacity with a blue dot; read ones dimmed
- Tap any notification → marks it as read in Firestore
- Overflow menu → "Mark all as read" (batched Firestore write)
- Empty state with icon when no notifications exist
- Icons change based on notification type (billing vs delivery vs general)

**AndroidManifest.xml** — the activity is already declared, no change needed.

---

## FEATURE 4 — WhatsApp & SMS Alerts
### File: MessagingUtils.java
**Destination:** `app/src/main/java/com/dailyserviceapp/core/utils/MessagingUtils.java`
**Action:** Drop this new file in — no existing file to replace.

**AndroidManifest.xml — add SMS permission:**
```xml
<uses-permission android:name="android.permission.SEND_SMS" />
```

**Runtime SMS permission (add to any Activity that calls sendSms):**
```java
if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.SEND_SMS}, 101);
}
```

**Usage examples — paste anywhere in your existing activities:**

```java
// WhatsApp: payment reminder to one customer
MessagingUtils.sendWhatsAppPaymentReminder(context, customer, 750.0);

// WhatsApp: bill is ready
MessagingUtils.sendWhatsAppBillReady(context, customer, "March 2026", 1200.0);

// WhatsApp: payment received thank-you
MessagingUtils.sendWhatsAppPaymentReceived(context, customer, 500.0);

// SMS: payment reminder
MessagingUtils.sendSmsPaymentReminder(context, customer, 750.0);

// SMS: bill ready
MessagingUtils.sendSmsBillReady(context, customer, "March 2026", 1200.0);

// SMS: bulk message to all customers
MessagingUtils.sendBulkSms(context, customerList, "Holiday notice: no delivery tomorrow.");
```

**Suggested integration points:**
- `BillDetailActivity` → add a "Send via WhatsApp" button that calls `sendWhatsAppBillReady`
- `PaymentActivity` → after recording payment, call `sendWhatsAppPaymentReceived`
- `BillListActivity` → overflow menu item "Send reminders" → `sendBulkSms` to overdue customers

---

## FEATURE 5 — Google Maps Route Optimization
### Files: RouteOptimizationActivity.java + layouts

**Destinations:**
- `app/src/main/java/com/dailyserviceapp/maps/RouteOptimizationActivity.java`  ← NEW package
- `app/src/main/res/layout/activity_route_optimization.xml`  ← NEW file
- `app/src/main/res/layout/row_route_stop.xml`              ← NEW file
- `app/src/main/res/menu/menu_route.xml`                    ← NEW file

**AndroidManifest.xml — add the activity:**
```xml
<activity
    android:name=".maps.RouteOptimizationActivity"
    android:exported="false"
    android:theme="@style/Theme.DailyServiceApp.NoActionBar" />
```

**Launch from DashboardActivity drawer — add to `onNavigationItemSelected`:**
```java
} else if (id == R.id.nav_route) {
    startActivity(new Intent(this, RouteOptimizationActivity.class));
}
```

**Add a drawer menu item to drawer_menu.xml:**
```xml
<item
    android:id="@+id/nav_route"
    android:icon="@drawable/ic_calendar_24"
    android:title="Delivery Route" />
```

**What it does:**
- Loads all active, non-vacation customers ordered by area
- Drag-and-drop list so provider can reorder stops manually
- "Sort by area" menu option for a quick alphabetical reset
- "Open in Maps (N stops)" FAB that builds a Google Maps multi-stop
  directions URL and launches Google Maps with turn-by-turn routing
- Handles up to 25 stops (Google Maps free limit); shows a warning if more

---

## QUICK CHECKLIST

- [ ] Replace FCMService.java
- [ ] Replace NotificationListActivity.java
- [ ] Replace activity_notification_list.xml
- [ ] Add row_notification.xml
- [ ] Add menu_notifications.xml
- [ ] Apply 6-step patch to DashboardActivity.java
- [ ] Add FCM token refresh call to LoginActivity after sign-in
- [ ] Add MessagingUtils.java
- [ ] Add SEND_SMS permission to AndroidManifest.xml
- [ ] Add RouteOptimizationActivity.java (new `maps` package)
- [ ] Add activity_route_optimization.xml
- [ ] Add row_route_stop.xml
- [ ] Add menu_route.xml
- [ ] Add RouteOptimizationActivity to AndroidManifest.xml
- [ ] Add nav_route item to drawer_menu.xml
- [ ] Launch RouteOptimizationActivity from drawer in DashboardActivity

---

## NOTES

**No new Gradle dependencies needed.**
All features use libraries already in your build.gradle:
- Firebase Messaging (FCM) — already present
- Firebase Firestore — already present
- Material Components (BadgeDrawable) — already present
- Android SmsManager — part of Android SDK
- Google Maps: uses a URL intent, no Maps SDK needed

**Firestore index needed for NotificationListActivity:**
Add this composite index in Firebase Console (or firestore.indexes.json):
  Collection: notifications
  Fields: userId ASC, timestamp DESC
