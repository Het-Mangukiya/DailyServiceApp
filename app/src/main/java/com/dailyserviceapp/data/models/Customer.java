package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

/**
 * Customer data model representing a service customer in DailyDrop.
 * Stores customer information, service details, and pricing for a provider's customer.
 * 
 * <p>Each customer is associated with a specific provider and service type.
 * Tracks customer status (ACTIVE/INACTIVE), service rate, and subscription details.</p>
 * 
 * <p>Uses Firebase Firestore annotations for document ID mapping.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public class Customer {
    /** Firestore document ID */
    @DocumentId
    private String id;

    /** Customer full name */
    private String name;
    
    /** Customer phone number */
    private String phone;
    
    /** Customer address for service delivery */
    private String address;
    
    /** Area/Locality for route planning (e.g., Sector 12, Downtown, etc.) */
    private String area;
    
    /** Type of service subscribed (Milk, Newspaper, Maid, Laundry) */
    private String serviceType;
    
    /** Rate per unit of service */
    private double ratePerUnit;
    
    /** Default quantity for daily delivery (everyday quantity) */
    private double defaultQuantity;
    
    /** Total amount lent to customer (pending payments) */
    private double lentAmount;
    
    /** Provider ID who manages this customer */
    private String providerId;
    
    /** Customer status: ACTIVE or INACTIVE */
    private String status;
    
    /** Additional notes about the customer */
    private String notes;
    
    /** Vacation mode - if true, customer is on vacation and service deliveries are paused */
    private boolean onVacation;
    
    /** Service subscription start date */
    private Timestamp startDate;
    
    /** Customer record creation timestamp */
    private Timestamp createdAt;

    /**
     * Creates a Customer instance with default values required by Firestore.
     *
     * Initializes observable defaults: defaultQuantity = 1.0, lentAmount = 0.0, and onVacation = false.
     */
    public Customer() {
        // Firestore requires a public no-arg constructor
        this.defaultQuantity = 1.0; // Default to 1 unit per day
        this.lentAmount = 0.0; // Default to no pending amount
        this.onVacation = false; // Default to not on vacation
    }

    /**
     * Constructs a Customer with the given personal, service, and pricing details and marks the customer active.
     *
     * Default values applied: status set to "ACTIVE", defaultQuantity set to 1.0, and lentAmount set to 0.0.
     *
     * @param name       customer's full name
     * @param phone      customer's phone number
     * @param address    service delivery address
     * @param serviceType type of subscribed service (e.g., "Milk", "Newspaper", "Maid", "Laundry")
     * @param ratePerUnit price per unit of service
     * @param createdAt  record creation timestamp
     */
    public Customer(String name, String phone, String address, String serviceType, double ratePerUnit, Timestamp createdAt) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.serviceType = serviceType;
        this.ratePerUnit = ratePerUnit;
        this.createdAt = createdAt;
        this.status = "ACTIVE";
        this.defaultQuantity = 1.0; // Default to 1 unit per day
        this.lentAmount = 0.0; // Default to no pending amount
    }

    /**
     * Gets the Firestore document identifier for this customer.
     *
     * @return the Firestore document ID for the customer, or {@code null} if not set
     */
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    /**
     * Retrieves the customer's service delivery address.
     *
     * @return the customer's delivery address, or null if not set
     */
    public String getAddress() {
        return address;
    }
    
    /**
     * Retrieves the customer's service area or locality.
     *
     * @return the customer's service area (locality), or null if not set
     */
    public String getArea() {
        return area;
    }

    /**
     * Gets the customer's subscribed service type.
     *
     * @return the service type for the customer (e.g., "Milk", "Newspaper", "Maid", "Laundry")
     */
    public String getServiceType() {
        return serviceType;
    }

    /**
     * The price charged per unit of the customer's subscribed service.
     *
     * @return the rate charged per unit of service
     */
    public double getRatePerUnit() {
        return ratePerUnit;
    }
    
    /**
     * Retrieves the customer's default daily service quantity.
     *
     * @return the default quantity per day in units (for example, 1.0)
     */
    public double getDefaultQuantity() {
        return defaultQuantity;
    }
    
    /**
     * Gets the total amount currently lent to the customer.
     *
     * @return the total amount lent to the customer
     */
    public double getLentAmount() {
        return lentAmount;
    }
    
    /**
     * Retrieves the provider identifier for this customer.
     *
     * @return the provider identifier, or {@code null} if not set
     */
    public String getProviderId() {
        return providerId;
    }
    
    public String getStatus() {
        return status;
    }
    
    /**
     * Retrieves additional notes associated with the customer.
     *
     * @return the customer's notes, or {@code null} if none are set
     */
    public String getNotes() {
        return notes;
    }
    
    /**
     * Indicates whether the customer's service deliveries are paused due to vacation.
     *
     * @return true if the customer is on vacation and deliveries are paused, false otherwise.
     */
    public boolean isOnVacation() {
        return onVacation;
    }
    
    /**
     * Get the customer's service subscription start date.
     *
     * @return the subscription start date as a Firestore {@code Timestamp}, or {@code null} if not set
     */
    public Timestamp getStartDate() {
        return startDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Sets the customer's service delivery address.
     *
     * @param address the service delivery address
     */
    public void setAddress(String address) {
        this.address = address;
    }
    
    /**
     * Sets the customer's service locality or area used for routing and delivery.
     *
     * @param area the locality or area name for the customer's address
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * Set the customer's subscribed service type.
     *
     * @param serviceType the service type identifier (for example "Milk", "Newspaper", "Maid", or "Laundry")
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * Set the customer's price per unit of service.
     *
     * @param ratePerUnit the price charged for one unit of the service (in the app's default currency)
     */
    public void setRatePerUnit(double ratePerUnit) {
        this.ratePerUnit = ratePerUnit;
    }
    
    /**
     * Sets the customer's default daily quantity of the subscribed service.
     *
     * @param defaultQuantity the quantity (units per day) to use by default for this customer
     */
    public void setDefaultQuantity(double defaultQuantity) {
        this.defaultQuantity = defaultQuantity;
    }
    
    /**
     * Sets the customer's total amount lent (outstanding credit).
     *
     * @param lentAmount the total amount lent to the customer
     */
    public void setLentAmount(double lentAmount) {
        this.lentAmount = lentAmount;
    }
    
    /**
     * Set the identifier of the provider responsible for this customer.
     *
     * @param providerId the provider's unique identifier or null to clear the association
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Sets free-form notes associated with the customer record.
     *
     * @param notes additional information or remarks about the customer; may be null or empty to clear existing notes
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    /**
     * Set the customer's vacation mode.
     *
     * When enabled, service deliveries for the customer are paused.
     *
     * @param onVacation true to pause deliveries for the customer, false to resume deliveries
     */
    public void setOnVacation(boolean onVacation) {
        this.onVacation = onVacation;
    }
    
    /**
     * Set the customer's service subscription start date.
     *
     * @param startDate the subscription start date as a Firestore `Timestamp`
     */
    public void setStartDate(Timestamp startDate) {
        this.startDate = startDate;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}