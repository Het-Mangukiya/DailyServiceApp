package com.dailyserviceapp.data.models;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for Customer model class.
 * Tests customer creation, getters/setters, and business logic.
 */
public class CustomerTest {

    private Customer customer;

    @Before
    public void setUp() {
        customer = new Customer();
    }

    @Test
    public void testDefaultConstructor() {
        assertNotNull("Customer should be created", customer);
        assertEquals("Default quantity should be 1.0", 1.0, customer.getDefaultQuantity(), 0.001);
        assertEquals("Default lent amount should be 0.0", 0.0, customer.getLentAmount(), 0.001);
        assertFalse("Default vacation status should be false", customer.isOnVacation());
    }

    @Test
    public void testParameterizedConstructor() {
        Timestamp now = Timestamp.now();
        Customer paramCustomer = new Customer("John Doe", "1234567890",
            "123 Main St", "Milk", 50.0, now);

        assertEquals("Name should match", "John Doe", paramCustomer.getName());
        assertEquals("Phone should match", "1234567890", paramCustomer.getPhone());
        assertEquals("Address should match", "123 Main St", paramCustomer.getAddress());
        assertEquals("Service type should match", "Milk", paramCustomer.getServiceType());
        assertEquals("Rate should match", 50.0, paramCustomer.getRatePerUnit(), 0.001);
        assertEquals("Created at should match", now, paramCustomer.getCreatedAt());
        assertEquals("Status should be ACTIVE", "ACTIVE", paramCustomer.getStatus());
        assertEquals("Default quantity should be 1.0", 1.0, paramCustomer.getDefaultQuantity(), 0.001);
        assertEquals("Lent amount should be 0.0", 0.0, paramCustomer.getLentAmount(), 0.001);
    }

    @Test
    public void testSettersAndGetters() {
        customer.setId("customer123");
        customer.setName("Jane Smith");
        customer.setPhone("9876543210");
        customer.setAddress("456 Oak Ave");
        customer.setArea("Downtown");
        customer.setServiceType("Newspaper");
        customer.setRatePerUnit(15.5);
        customer.setDefaultQuantity(2.0);
        customer.setLentAmount(500.0);
        customer.setProviderId("provider789");
        customer.setStatus("INACTIVE");
        customer.setNotes("Special delivery instructions");
        customer.setOnVacation(true);

        assertEquals("ID should match", "customer123", customer.getId());
        assertEquals("Name should match", "Jane Smith", customer.getName());
        assertEquals("Phone should match", "9876543210", customer.getPhone());
        assertEquals("Address should match", "456 Oak Ave", customer.getAddress());
        assertEquals("Area should match", "Downtown", customer.getArea());
        assertEquals("Service type should match", "Newspaper", customer.getServiceType());
        assertEquals("Rate should match", 15.5, customer.getRatePerUnit(), 0.001);
        assertEquals("Default quantity should match", 2.0, customer.getDefaultQuantity(), 0.001);
        assertEquals("Lent amount should match", 500.0, customer.getLentAmount(), 0.001);
        assertEquals("Provider ID should match", "provider789", customer.getProviderId());
        assertEquals("Status should match", "INACTIVE", customer.getStatus());
        assertEquals("Notes should match", "Special delivery instructions", customer.getNotes());
        assertTrue("Vacation status should be true", customer.isOnVacation());
    }

    @Test
    public void testServiceTypes() {
        String[] serviceTypes = {"Milk", "Newspaper", "Maid", "Laundry", "Custom Service"};

        for (String serviceType : serviceTypes) {
            customer.setServiceType(serviceType);
            assertEquals("Service type should be set correctly", serviceType,
                customer.getServiceType());
        }
    }

    @Test
    public void testRatePerUnitRange() {
        double[] rates = {0.0, 0.5, 10.0, 50.0, 100.0, 999.99};

        for (double rate : rates) {
            customer.setRatePerUnit(rate);
            assertEquals("Rate should be set correctly", rate,
                customer.getRatePerUnit(), 0.001);
        }
    }

    @Test
    public void testDefaultQuantityValues() {
        double[] quantities = {0.5, 1.0, 1.5, 2.0, 5.0};

        for (double quantity : quantities) {
            customer.setDefaultQuantity(quantity);
            assertEquals("Default quantity should be set correctly", quantity,
                customer.getDefaultQuantity(), 0.001);
        }
    }

    @Test
    public void testLentAmountTracking() {
        customer.setLentAmount(0.0);
        assertEquals("Initial lent amount", 0.0, customer.getLentAmount(), 0.001);

        customer.setLentAmount(100.0);
        assertEquals("After lending 100", 100.0, customer.getLentAmount(), 0.001);

        customer.setLentAmount(250.5);
        assertEquals("After lending more", 250.5, customer.getLentAmount(), 0.001);

        customer.setLentAmount(0.0);
        assertEquals("After payment", 0.0, customer.getLentAmount(), 0.001);
    }

    @Test
    public void testStatusValues() {
        String[] statuses = {"ACTIVE", "INACTIVE", "PAUSED"};

        for (String status : statuses) {
            customer.setStatus(status);
            assertEquals("Status should be set correctly", status, customer.getStatus());
        }
    }

    @Test
    public void testVacationMode() {
        assertFalse("Initially not on vacation", customer.isOnVacation());

        customer.setOnVacation(true);
        assertTrue("Should be on vacation", customer.isOnVacation());

        customer.setOnVacation(false);
        assertFalse("Should not be on vacation", customer.isOnVacation());
    }

    @Test
    public void testNotesField() {
        String longNotes = "Customer prefers morning delivery. " +
            "Leave at doorstep. Call if not home. " +
            "Special instructions for holidays.";

        customer.setNotes(longNotes);
        assertEquals("Notes should be preserved", longNotes, customer.getNotes());

        customer.setNotes("");
        assertEquals("Empty notes should be allowed", "", customer.getNotes());

        customer.setNotes(null);
        assertNull("Null notes should be allowed", customer.getNotes());
    }

    @Test
    public void testAreaField() {
        String[] areas = {"Sector 12", "Downtown", "North Zone", "Area-B", ""};

        for (String area : areas) {
            customer.setArea(area);
            assertEquals("Area should be set correctly", area, customer.getArea());
        }
    }

    @Test
    public void testStartDateTimestamp() {
        Timestamp startDate = Timestamp.now();
        customer.setStartDate(startDate);

        assertEquals("Start date should match", startDate, customer.getStartDate());
    }

    @Test
    public void testCreatedAtTimestamp() {
        Timestamp createdAt = Timestamp.now();
        customer.setCreatedAt(createdAt);

        assertEquals("Created at should match", createdAt, customer.getCreatedAt());
    }

    @Test
    public void testPhoneNumberFormats() {
        String[] phoneNumbers = {
            "1234567890",
            "+911234567890",
            "123-456-7890",
            "(123) 456-7890"
        };

        for (String phone : phoneNumbers) {
            customer.setPhone(phone);
            assertEquals("Phone number should be stored as-is", phone, customer.getPhone());
        }
    }

    @Test
    public void testMultilineAddress() {
        String address = "Apartment 123\nBuilding Name\nStreet Name\nCity - 123456";
        customer.setAddress(address);

        assertEquals("Multiline address should be preserved", address, customer.getAddress());
    }

    @Test
    public void testZeroRatePerUnit() {
        customer.setRatePerUnit(0.0);
        assertEquals("Zero rate should be allowed", 0.0, customer.getRatePerUnit(), 0.001);
    }

    @Test
    public void testZeroDefaultQuantity() {
        customer.setDefaultQuantity(0.0);
        assertEquals("Zero default quantity should be allowed", 0.0,
            customer.getDefaultQuantity(), 0.001);
    }

    @Test
    public void testNegativeLentAmount() {
        // In case of overpayment or credit
        customer.setLentAmount(-50.0);
        assertEquals("Negative lent amount should be allowed", -50.0,
            customer.getLentAmount(), 0.001);
    }

    @Test
    public void testLargeLentAmount() {
        customer.setLentAmount(99999.99);
        assertEquals("Large lent amount should be handled", 99999.99,
            customer.getLentAmount(), 0.001);
    }

    @Test
    public void testCustomerWithAllFieldsSet() {
        Timestamp now = Timestamp.now();

        customer.setId("cust001");
        customer.setName("Complete Customer");
        customer.setPhone("9999999999");
        customer.setAddress("Full Address Line");
        customer.setArea("Test Area");
        customer.setServiceType("Complete Service");
        customer.setRatePerUnit(75.5);
        customer.setDefaultQuantity(3.0);
        customer.setLentAmount(150.0);
        customer.setProviderId("provider001");
        customer.setStatus("ACTIVE");
        customer.setNotes("Complete notes");
        customer.setOnVacation(false);
        customer.setStartDate(now);
        customer.setCreatedAt(now);

        assertEquals("ID", "cust001", customer.getId());
        assertEquals("Name", "Complete Customer", customer.getName());
        assertEquals("Phone", "9999999999", customer.getPhone());
        assertEquals("Address", "Full Address Line", customer.getAddress());
        assertEquals("Area", "Test Area", customer.getArea());
        assertEquals("Service type", "Complete Service", customer.getServiceType());
        assertEquals("Rate", 75.5, customer.getRatePerUnit(), 0.001);
        assertEquals("Default quantity", 3.0, customer.getDefaultQuantity(), 0.001);
        assertEquals("Lent amount", 150.0, customer.getLentAmount(), 0.001);
        assertEquals("Provider ID", "provider001", customer.getProviderId());
        assertEquals("Status", "ACTIVE", customer.getStatus());
        assertEquals("Notes", "Complete notes", customer.getNotes());
        assertFalse("Vacation", customer.isOnVacation());
        assertEquals("Start date", now, customer.getStartDate());
        assertEquals("Created at", now, customer.getCreatedAt());
    }

    @Test
    public void testNullableFields() {
        customer.setId(null);
        customer.setArea(null);
        customer.setNotes(null);
        customer.setStartDate(null);

        assertNull("ID can be null", customer.getId());
        assertNull("Area can be null", customer.getArea());
        assertNull("Notes can be null", customer.getNotes());
        assertNull("Start date can be null", customer.getStartDate());
    }

    @Test
    public void testEmptyStringFields() {
        customer.setName("");
        customer.setPhone("");
        customer.setAddress("");
        customer.setArea("");
        customer.setServiceType("");
        customer.setProviderId("");
        customer.setStatus("");
        customer.setNotes("");

        assertEquals("Empty name", "", customer.getName());
        assertEquals("Empty phone", "", customer.getPhone());
        assertEquals("Empty address", "", customer.getAddress());
        assertEquals("Empty area", "", customer.getArea());
        assertEquals("Empty service type", "", customer.getServiceType());
        assertEquals("Empty provider ID", "", customer.getProviderId());
        assertEquals("Empty status", "", customer.getStatus());
        assertEquals("Empty notes", "", customer.getNotes());
    }

    @Test
    public void testFractionalDefaultQuantity() {
        double[] fractionalQuantities = {0.5, 1.5, 2.25, 3.75};

        for (double quantity : fractionalQuantities) {
            customer.setDefaultQuantity(quantity);
            assertEquals("Fractional quantity should be preserved", quantity,
                customer.getDefaultQuantity(), 0.001);
        }
    }

    @Test
    public void testDecimalRatePerUnit() {
        double[] decimalRates = {5.25, 10.50, 15.75, 99.99};

        for (double rate : decimalRates) {
            customer.setRatePerUnit(rate);
            assertEquals("Decimal rate should be preserved", rate,
                customer.getRatePerUnit(), 0.001);
        }
    }
}