const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/**
 * Triggered when a new notification document is created in Firestore.
 * Looks up the recipient's FCM token and sends a push notification.
 */
exports.sendPushNotification = onDocumentCreated(
  "notifications/{notifId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const data = snapshot.data();
    const userId = data.userId;
    const title = data.title || "DailyDrop";
    const message = data.message || "";
    const type = data.type || "SERVICE_DELIVERY";
    const relatedId = data.relatedId || "";

    if (!userId) {
      console.warn("Notification missing userId, skipping push.");
      return;
    }

    // Look up the user's FCM token
    const db = getFirestore();
    const userDoc = await db.collection("users").doc(userId).get();

    if (!userDoc.exists) {
      console.warn(`User ${userId} not found, skipping push.`);
      return;
    }

    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) {
      console.warn(`User ${userId} has no FCM token, skipping push.`);
      return;
    }

    // Build and send the FCM message
    const fcmMessage = {
      token: fcmToken,
      data: {
        title: title,
        body: message,
        type: type,
        relatedId: relatedId,
      },
      android: {
        priority: "high",
      },
    };

    try {
      await getMessaging().send(fcmMessage);
      console.log(`Push sent to user ${userId} for notification ${event.params.notifId}`);
    } catch (error) {
      console.error(`Failed to send push to user ${userId}:`, error.message);

      // If token is invalid, clean it up
      if (
        error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered"
      ) {
        console.log(`Removing stale FCM token for user ${userId}`);
        await db.collection("users").doc(userId).update({
          fcmToken: FieldValue.delete(),
        });
      }
    }
  }
);
