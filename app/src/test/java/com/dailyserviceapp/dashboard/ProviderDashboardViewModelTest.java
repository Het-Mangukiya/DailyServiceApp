package com.dailyserviceapp.dashboard;

import com.dailyserviceapp.data.repository.CustomerRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Boilerplate scaffold for ProviderDashboardViewModel tests.
 */
public class ProviderDashboardViewModelTest {

    private AutoCloseable closeable;

    @Mock
    private FirebaseFirestore firestore;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CollectionReference collectionReference;
    @Mock
    private Query query;
    @Mock
    private QuerySnapshot querySnapshot;
    @Mock
    private DocumentSnapshot documentSnapshot;
    @Mock
    private FirebaseAuth firebaseAuth;
    @Mock
    private FirebaseUser firebaseUser;

    private ProviderDashboardViewModel viewModel;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        viewModel = new ProviderDashboardViewModel(firestore, customerRepository);
    }

    @After
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    public void scaffold_providerDashboardViewModel_pendingImplementation() {
        // TODO(codex): Add dashboard metric LiveData tests.
    }
}
