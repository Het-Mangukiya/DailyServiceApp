package com.dailyserviceapp.customer;

import androidx.lifecycle.MutableLiveData;

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
 * Boilerplate scaffold for Customer ViewModel tests.
 */
public class CustomerViewModelTest {

    private AutoCloseable closeable;

    @Mock
    private FirebaseFirestore firestore;
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

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    public void scaffold_customerViewModel_pendingImplementation() {
        // TODO(codex): Add ViewModel test setup when Customer ViewModel test target is finalized.
    }
}
