// ══════════════════════════════════════════════════════════════════════════════
// PATCH: DashboardActivity — notification bell fix + unread badge
//
// HOW TO APPLY:
//   1. Open DashboardActivity.java
//   2. Add the imports listed below (Step A)
//   3. Add the field declarations (Step B)
//   4. Call loadUnreadNotificationCount() from the end of onCreate() (Step C)
//   5. Replace the onOptionsItemSelected method with the one below (Step D)
//   6. Add the two helper methods (Step E)
//   7. In onDestroy(), remove the listener (Step F)
// ══════════════════════════════════════════════════════════════════════════════

// ── Step A: Add these imports ─────────────────────────────────────────────────
/*
import com.dailyserviceapp.notifications.NotificationListActivity;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
*/

// ── Step B: Add these fields inside the class ─────────────────────────────────
/*
    // Notification badge
    private ListenerRegistration notifListener;
    private int unreadNotifCount = 0;
*/

// ── Step C: At the END of onCreate(), add this line ───────────────────────────
/*
    loadUnreadNotificationCount();

    // Also: if we arrived here after tapping a push notification, open the list
    if (getIntent() != null && getIntent().getBooleanExtra("openNotifications", false)) {
        startActivity(new Intent(this, NotificationListActivity.class));
    }
*/

// ── Step D: Replace your existing onOptionsItemSelected ───────────────────────
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.sortButton) {
            showSortDialog();
            return true;
        } else if (id == R.id.action_calendar) {
            startActivity(new Intent(this, ServiceEntryActivity.class));
            return true;
        } else if (id == R.id.action_notifications) {
            // ✅ FIX: bell now opens NotificationListActivity
            startActivity(new Intent(this, NotificationListActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
*/

// ── Step E: Add these two helper methods inside DashboardActivity ──────────────
/*
    /**
     * Listens to Firestore in real-time and updates the notification bell badge.
     * /
    private void loadUnreadNotificationCount() {
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        notifListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection(com.dailyserviceapp.core.utils.Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("read", false)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    unreadNotifCount = snapshots.size();
                    updateNotificationBadge();
                });
    }

    /**
     * Draws a numeric badge (or a simple dot) on the notification bell menu icon.
     * Call invalidateOptionsMenu() to force a redraw if needed.
     * /
    private void updateNotificationBadge() {
        // Simple approach: update the toolbar icon tint or title
        // For a proper numeric badge, use a BadgeDrawable from Material Components.
        // This snippet uses BadgeDrawable if your toolbar is a MaterialToolbar.
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) return;

        // Find the notification menu item
        Menu menu = toolbar.getMenu();
        if (menu == null) return;

        MenuItem notifItem = menu.findItem(R.id.action_notifications);
        if (notifItem == null) return;

        // Use Material BadgeDrawable
        com.google.android.material.badge.BadgeDrawable badge =
                com.google.android.material.badge.BadgeUtils.attachBadgeDrawable(
                        com.google.android.material.badge.BadgeDrawable.create(this),
                        toolbar,
                        R.id.action_notifications);

        if (unreadNotifCount > 0) {
            badge.setNumber(unreadNotifCount);
            badge.setVisible(true);
        } else {
            badge.setVisible(false);
        }
    }
*/

// ── Step F: In onDestroy(), add ────────────────────────────────────────────────
/*
    if (notifListener != null) notifListener.remove();
*/
