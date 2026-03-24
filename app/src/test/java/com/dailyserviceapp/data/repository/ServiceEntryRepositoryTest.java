package com.dailyserviceapp.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Boilerplate scaffold for ServiceEntryRepository tests.
 */
public class ServiceEntryRepositoryTest {

    private AutoCloseable closeable;

    @Mock
    private FirebaseFirestore firestore;
    @Mock
    private CollectionReference collectionReference;
    @Mock
    private DocumentReference documentReference;
    @Mock
    private Query query;
    @Mock
    private QuerySnapshot querySnapshot;
    @Mock
    private DocumentSnapshot documentSnapshot;
    @Mock
    private WriteBatch writeBatch;
    @Mock
    private Transaction transaction;
    @Mock
    private FirebaseAuth firebaseAuth;
    @Mock
    private FirebaseUser firebaseUser;

    private ServiceEntryRepository repository;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        repository = new ServiceEntryRepository(firestore);
    }

    @After
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    public void scaffold_serviceEntryRepository_pendingImplementation() {
        // TODO(codex): Add success, failure, and edge-case tests.
    }
}
