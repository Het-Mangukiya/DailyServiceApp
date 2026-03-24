package com.dailyserviceapp.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.dailyserviceapp.core.utils.CurrencyUtils;
import com.dailyserviceapp.data.repository.CustomerRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ProviderDashboardViewModel extends ViewModel {

    private final FirebaseFirestore firestore;
    private final CustomerRepository customerRepository;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return _isLoading; }

    private final MutableLiveData<String> _todayDelivered = new MutableLiveData<>("0 / 0");
    public LiveData<String> todayDelivered() { return _todayDelivered; }

    private final MutableLiveData<String> _todayEarnings = new MutableLiveData<>("₹0.00");
    public LiveData<String> todayEarnings() { return _todayEarnings; }

    private final MutableLiveData<String> _totalLent = new MutableLiveData<>("₹0.00");
    public LiveData<String> totalLent() { return _totalLent; }

    private final MutableLiveData<String> _pendingAmount = new MutableLiveData<>("₹0.00");
    public LiveData<String> pendingAmount() { return _pendingAmount; }

    private final MutableLiveData<String> _totalReceived = new MutableLiveData<>("₹0.00");
    public LiveData<String> totalReceived() { return _totalReceived; }
    
    private final MutableLiveData<String> _monthlyEarnings = new MutableLiveData<>("₹0.00");
    public LiveData<String> monthlyEarnings() { return _monthlyEarnings; }

    private final MutableLiveData<String> _monthlyDeliveries = new MutableLiveData<>("0");
    public LiveData<String> monthlyDeliveries() { return _monthlyDeliveries; }

    private int loadingTasks = 0;
    private static final int TOTAL_TASKS = 3;

    @Inject
    public ProviderDashboardViewModel(FirebaseFirestore firestore, CustomerRepository customerRepository) {
        this.firestore = firestore;
        this.customerRepository = customerRepository;
    }

    public void loadDashboardData(String providerId) {
        if (providerId == null || providerId.isEmpty()) return;
        
        _isLoading.setValue(true);
        loadingTasks = 0;
        
        loadTodaysSummary(providerId);
        loadPaymentOverview(providerId);
        loadMonthlyOverview(providerId);
    }

    private synchronized void checkLoadingComplete() {
        loadingTasks++;
        if (loadingTasks >= TOTAL_TASKS) {
            _isLoading.postValue(false);
        }
    }

    private void loadTodaysSummary(String providerId) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Calendar tomorrow = (Calendar) today.clone();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);
        
        Timestamp startOfDay = new Timestamp(today.getTime());
        Timestamp endExclusive = new Timestamp(tomorrow.getTime());

        Task<QuerySnapshot> customersTask = firestore.collection("customers")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("status", "ACTIVE")
            .get();

        Task<QuerySnapshot> entriesTask = firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("delivered", true)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThan("date", endExclusive)
            .get();

        Tasks.whenAllSuccess(customersTask, entriesTask)
            .addOnSuccessListener(results -> {
                QuerySnapshot customerSnapshot = (QuerySnapshot) results.get(0);
                QuerySnapshot entriesSnapshot = (QuerySnapshot) results.get(1);

                int totalCustomers = customerSnapshot.size();
                Map<String, Double> customerRates = new HashMap<>();
                for (QueryDocumentSnapshot doc : customerSnapshot) {
                    Double rate = doc.getDouble("ratePerUnit");
                    if (rate != null) customerRates.put(doc.getId(), rate);
                }

                int deliveredCount = 0;
                double todayEarningsVal = 0.0;
                
                for (QueryDocumentSnapshot doc : entriesSnapshot) {
                    deliveredCount++;
                    Double rate = doc.getDouble("rate");
                    Double quantity = doc.getDouble("quantity");
                    String customerId = doc.getString("customerId");

                    if ((rate == null || rate == 0.0) && customerId != null) {
                        rate = customerRates.get(customerId);
                    }
                    if (rate != null && quantity != null) {
                        todayEarningsVal += (rate * quantity);
                    }
                }

                _todayDelivered.postValue(deliveredCount + " / " + totalCustomers);
                _todayEarnings.postValue(CurrencyUtils.formatIndianCurrency(todayEarningsVal));
                checkLoadingComplete();
            })
            .addOnFailureListener(e -> checkLoadingComplete());
    }

    private void loadPaymentOverview(String providerId) {
        Task<QuerySnapshot> customersTask = firestore.collection("customers")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("status", "ACTIVE")
            .get();

        Task<QuerySnapshot> paymentsTask = firestore.collection("payments")
            .whereEqualTo("providerId", providerId)
            .get();

        Tasks.whenAllSuccess(customersTask, paymentsTask)
            .addOnSuccessListener(results -> {
                QuerySnapshot customerSnapshot = (QuerySnapshot) results.get(0);
                QuerySnapshot paymentSnapshot = (QuerySnapshot) results.get(1);

                double totalLentVal = 0.0;
                for (QueryDocumentSnapshot doc : customerSnapshot) {
                    Double lentAmount = doc.getDouble("lentAmount");
                    if (lentAmount != null) totalLentVal += lentAmount;
                }
                
                double totalReceivedVal = 0.0;
                for (QueryDocumentSnapshot doc : paymentSnapshot) {
                    Double amount = doc.getDouble("amount");
                    if (amount != null) totalReceivedVal += amount;
                }
                
                _totalLent.postValue(CurrencyUtils.formatIndianCurrency(totalLentVal));
                _totalReceived.postValue(CurrencyUtils.formatIndianCurrency(totalReceivedVal));
                _pendingAmount.postValue(CurrencyUtils.formatIndianCurrency(totalLentVal - totalReceivedVal));
                checkLoadingComplete();
            })
            .addOnFailureListener(e -> checkLoadingComplete());
    }

    private void loadMonthlyOverview(String providerId) {
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);
        
        Timestamp startOfMonth = new Timestamp(monthStart.getTime());
        Calendar nextMonth = (Calendar) monthStart.clone();
        nextMonth.add(Calendar.MONTH, 1);
        Timestamp endExclusive = new Timestamp(nextMonth.getTime());

        Task<QuerySnapshot> customersTask = firestore.collection("customers")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("status", "ACTIVE")
            .get();

        Task<QuerySnapshot> entriesTask = firestore.collection("serviceEntries")
            .whereEqualTo("providerId", providerId)
            .whereEqualTo("delivered", true)
            .whereGreaterThanOrEqualTo("date", startOfMonth)
            .whereLessThan("date", endExclusive)
            .get();

        Tasks.whenAllSuccess(customersTask, entriesTask)
            .addOnSuccessListener(results -> {
                QuerySnapshot customerSnapshot = (QuerySnapshot) results.get(0);
                QuerySnapshot entriesSnapshot = (QuerySnapshot) results.get(1);

                Map<String, Double> customerRates = new HashMap<>();
                for (QueryDocumentSnapshot doc : customerSnapshot) {
                    Double rate = doc.getDouble("ratePerUnit");
                    if (rate != null) customerRates.put(doc.getId(), rate);
                }

                int monthlyDeliveriesVal = 0;
                double monthlyEarningsVal = 0.0;

                for (QueryDocumentSnapshot doc : entriesSnapshot) {
                    monthlyDeliveriesVal++;
                    Double rate = doc.getDouble("rate");
                    Double quantity = doc.getDouble("quantity");
                    String customerId = doc.getString("customerId");

                    if ((rate == null || rate == 0.0) && customerId != null) {
                        rate = customerRates.get(customerId);
                    }
                    if (rate != null && quantity != null) {
                        monthlyEarningsVal += (rate * quantity);
                    }
                }

                _monthlyDeliveries.postValue(String.valueOf(monthlyDeliveriesVal));
                _monthlyEarnings.postValue(CurrencyUtils.formatIndianCurrency(monthlyEarningsVal));
                checkLoadingComplete();
            })
            .addOnFailureListener(e -> checkLoadingComplete());
    }
}
