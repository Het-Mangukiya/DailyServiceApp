package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Notification data model representing an in-app notification.
 * Stores notification details for various app events including
 * bill generation, payment reminders, payment receipts, and service updates.
 * 
 * <p>Notification types:</p>
 * <ul>
 *   <li>BILL_GENERATED - New monthly bill created</li>
 *   <li>PAYMENT_REMINDER - Upcoming or overdue payment</li>
 *   <li>PAYMENT_RECEIVED - Payment successfully recorded</li>
 *   <li>SERVICE_DELIVERY - Service delivery update</li>
 * </ul>
 * 
 * <p>Notifications support read/unread status and can link to related entities.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class Notification {
    /** Firestore document ID */
    @DocumentId
    private String id;
    
    /** User ID who receives this notification */
    private String userId;
    
    /** Notification title */
    private String title;
    
    /** Notification message content */
    private String message;
    
    /** Notification type: BILL_GENERATED, PAYMENT_REMINDER, PAYMENT_RECEIVED, SERVICE_DELIVERY */
    private String type;
    
    /** Read status flag */
    private boolean read;
    
    /** ID of related bill, payment, or service entry */
    private String relatedId;
    
    /** Notification creation timestamp */
    private Timestamp timestamp;
    
    public Notification() {
        // Empty constructor required for Firestore
    }
    
    public Notification(String userId, String title, String message, String type) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = false;
        this.timestamp = Timestamp.now();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    public String getRelatedId() {
        return relatedId;
    }
    
    public void setRelatedId(String relatedId) {
        this.relatedId = relatedId;
    }
    
    public Timestamp getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
