package com.dailyserviceapp.billing;

import android.util.Log;

import com.dailyserviceapp.data.models.Customer;
import com.dailyserviceapp.data.models.Payment;
import com.dailyserviceapp.data.models.ServiceEntry;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Utility to build customer-wise billing summaries directly from service entries and payments.
 */
public final class CustomerLedgerCalculator {

    private static final double EPSILON = 0.01;

    private CustomerLedgerCalculator() {
    }

    public static CustomerLedgerSummary calculate(Customer customer, List<ServiceEntry> serviceEntries,
                                                  List<Payment> payments) {
        CustomerLedgerSummary summary = new CustomerLedgerSummary();
        summary.setCustomerId(customer != null ? customer.getId() : null);
        summary.setCustomerName(customer != null ? customer.getName() : "Unknown");

        List<ServiceEntry> deliveredEntries = new ArrayList<>();
        if (serviceEntries != null) {
            for (ServiceEntry entry : serviceEntries) {
                if (entry != null && entry.isDelivered() && entry.getDate() != null) {
                    deliveredEntries.add(entry);
                }
            }
        }
        deliveredEntries.sort(Comparator.comparing(e -> e.getDate().toDate()));

        List<Payment> validPayments = new ArrayList<>();
        if (payments != null) {
            for (Payment payment : payments) {
                if (payment != null && payment.getPaymentDate() != null) {
                    validPayments.add(payment);
                }
            }
        }
        validPayments.sort(Comparator.comparing(p -> p.getPaymentDate().toDate()));

        double totalServiceAmount = 0.0;
        for (ServiceEntry entry : deliveredEntries) {
            totalServiceAmount += calculateEntryAmount(entry, customer);
        }

        double totalPaidAmount = 0.0;
        for (Payment payment : validPayments) {
            totalPaidAmount += Math.max(0.0, payment.getAmount());
        }

        double outstanding = totalServiceAmount - totalPaidAmount;
        if (Math.abs(outstanding) <= EPSILON) {
            outstanding = 0.0;
        } else if (outstanding < 0) {
            // Keep ledger stable even if customer has advance payment.
            outstanding = 0.0;
        }

        Timestamp firstServiceDate = deliveredEntries.isEmpty() ? null : deliveredEntries.get(0).getDate();
        Timestamp lastServiceDate = deliveredEntries.isEmpty()
            ? null : deliveredEntries.get(deliveredEntries.size() - 1).getDate();

        Timestamp paidTillDate = null;
        if (!validPayments.isEmpty()) {
            paidTillDate = validPayments.get(validPayments.size() - 1).getPaymentDate();
        }
        if (outstanding <= EPSILON && lastServiceDate != null && totalPaidAmount > 0) {
            paidTillDate = lastServiceDate;
        }

        Timestamp dueFromDate = null;
        if (outstanding > EPSILON) {
            if (paidTillDate == null) {
                dueFromDate = firstServiceDate;
            } else {
                for (ServiceEntry entry : deliveredEntries) {
                    if (entry.getDate().toDate().after(paidTillDate.toDate())) {
                        dueFromDate = entry.getDate();
                        break;
                    }
                }
                if (dueFromDate == null) {
                    String customerIdForLog = customer != null ? customer.getId() : "unknown";
                    safeLogWarn("CustomerLedgerCalculator",
                        "dueFromDate fallback to firstServiceDate for customer=" + customerIdForLog
                            + ", outstanding=" + outstanding
                            + ", paidTillDate=" + (paidTillDate != null ? paidTillDate.toDate() : null));
                    dueFromDate = firstServiceDate;
                }
            }
        }

        summary.setDeliveredEntries(deliveredEntries.size());
        summary.setTotalServiceAmount(totalServiceAmount);
        summary.setTotalPaidAmount(totalPaidAmount);
        summary.setOutstandingAmount(outstanding);
        summary.setFirstServiceDate(firstServiceDate);
        summary.setLastServiceDate(lastServiceDate);
        summary.setPaidTillDate(paidTillDate);
        summary.setDueFromDate(dueFromDate);
        return summary;
    }

    public static double calculateEntryAmount(ServiceEntry entry, Customer customer) {
        if (entry == null) return 0.0;

        double quantity = entry.getQuantity() > 0 ? entry.getQuantity() : 1.0;
        double rate = entry.getRate();
        if (rate <= 0 && customer != null) {
            rate = customer.getRatePerUnit();
        }
        return Math.max(0.0, quantity * Math.max(0.0, rate));
    }

    private static void safeLogWarn(String tag, String message) {
        try {
            Log.w(tag, message);
        } catch (RuntimeException ignored) {
            // Avoid crashing local unit tests where android.util.Log is not mocked.
        }
    }
}
