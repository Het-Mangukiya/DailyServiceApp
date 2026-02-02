# DailyServiceApp Unit Tests

This directory contains comprehensive unit tests for the DailyServiceApp Android application.

## Test Structure

```
test/
└── java/
    └── com/
        └── dailyserviceapp/
            ├── SplashActivityTest.java              # Splash screen tests
            ├── billing/
            │   └── BillAdapterTest.java             # Bill adapter tests
            ├── core/
            │   ├── base/
            │   │   └── BaseActivityTest.java        # Base activity tests
            │   └── offline/
            │       └── OfflineCacheTest.java        # Offline caching tests
            ├── data/
            │   └── models/
            │       ├── BillTest.java                # Bill model tests
            │       └── CustomerTest.java            # Customer model tests
            └── debug/
                └── TestDataBroadcastReceiverTest.java # Debug receiver tests
```

## Test Coverage

### Data Models (50+ tests)
- **BillTest.java** - 30 tests
  - Bill creation and initialization
  - Nested classes (BillItem, ExtraCharge, Adjustment)
  - Getters/setters validation
  - Collection management
  - Edge cases (zero amounts, null values)

- **CustomerTest.java** - 25 tests
  - Customer creation and initialization
  - Service type handling
  - Rate and quantity validation
  - Lent amount tracking
  - Vacation mode
  - Status management

### Core Functionality (38+ tests)
- **BaseActivityTest.java** - 20 tests
  - Activity initialization
  - Toolbar setup
  - Network monitoring
  - Session management
  - Navigation
  - Logout functionality

- **OfflineCacheTest.java** - 18 tests
  - Customer caching
  - Pending entry queue
  - SharedPreferences persistence
  - Data serialization/deserialization
  - Cache clearing
  - Sync time tracking

### UI Components (31+ tests)
- **BillAdapterTest.java** - 23 tests
  - RecyclerView adapter
  - Data binding
  - ViewHolder creation
  - Payment status display
  - Customer name handling
  - Amount formatting

- **SplashActivityTest.java** - 8 tests
  - Activity lifecycle
  - Navigation logic
  - User session detection
  - Timing validation

### Debug Tools (6+ tests)
- **TestDataBroadcastReceiverTest.java** - 6 tests
  - Broadcast receiver
  - Intent handling
  - Error handling

## Running Tests

### All Tests
```bash
./gradlew test
```

### Specific Test Class
```bash
./gradlew test --tests "com.dailyserviceapp.data.models.BillTest"
```

### With Coverage
```bash
./gradlew testDebugUnitTest jacocoTestReport
```

### Using Script
```bash
./run_tests.sh
```

## Test Dependencies

The following dependencies are required (already in `app/build.gradle`):

```gradle
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:5.8.0'
testImplementation 'org.robolectric:robolectric:4.11.1'
testImplementation 'androidx.test:core:1.5.0'
```

## Test Framework

- **JUnit 4** - Testing framework
- **Mockito** - Mocking framework for dependencies
- **Robolectric** - Android framework simulation for unit tests
- **AndroidX Test** - Android testing utilities

## Best Practices Implemented

1. **Test Isolation** - Each test is independent
2. **AAA Pattern** - Arrange, Act, Assert structure
3. **Descriptive Names** - Test names clearly describe what is tested
4. **Edge Cases** - Null, zero, empty, and boundary values tested
5. **Setup/Teardown** - Proper initialization and cleanup
6. **Fast Execution** - Pure unit tests without emulator

## Test Naming Convention

Tests follow the pattern: `test<MethodName><Scenario>`

Examples:
- `testDefaultConstructor()` - Tests default constructor
- `testSettersAndGetters()` - Tests all setters and getters
- `testNullInputHandling()` - Tests behavior with null inputs
- `testEdgeCaseZeroAmount()` - Tests edge case with zero amount

## What is NOT Tested

The following require integration or instrumentation tests:
- Firebase Authentication flows
- Firestore database operations
- Network requests
- UI interactions (clicks, scrolls)
- Intent results
- Broadcast receivers with Firebase dependency

These should be tested with:
- Espresso (UI tests)
- Firebase Test Lab
- Integration tests with Firebase emulator

## Assertion Examples

```java
// Basic assertions
assertEquals("Message", expected, actual);
assertTrue("Message", condition);
assertFalse("Message", condition);
assertNull("Message", object);
assertNotNull("Message", object);

// Delta for floating point
assertEquals("Amount should match", 100.50, actual, 0.001);

// Collections
assertEquals("Size should match", 5, list.size());
assertTrue("Should contain item", list.contains(item));
```

## Mock Examples

```java
// Mock objects
@Mock
private FirebaseAuth mockAuth;

// Setup mock behavior
when(mockAuth.getCurrentUser()).thenReturn(mockUser);

// Verify interactions
verify(mockAuth, times(1)).signOut();
```

## Troubleshooting

### Tests Not Running
1. Sync Gradle: `./gradlew build`
2. Clean build: `./gradlew clean`
3. Invalidate caches in IDE

### Robolectric Issues
- Ensure SDK 28 is installed
- Check `@Config(sdk = 28)` annotation
- Update Robolectric version if needed

### Mockito Issues
- Use `MockitoAnnotations.openMocks(this)` in `@Before`
- Ensure mocks are initialized before use
- Check mock/spy usage is correct

## Contributing Tests

When adding new tests:
1. Follow existing naming conventions
2. Test both success and failure paths
3. Include edge cases
4. Document complex test logic
5. Keep tests isolated and fast
6. Aim for >80% code coverage

## Reports

After running tests, view reports at:
- HTML Report: `app/build/reports/tests/testDebugUnitTest/index.html`
- XML Results: `app/build/test-results/testDebugUnitTest/`
- Coverage Report: `app/build/reports/jacoco/jacocoTestReport/html/index.html`

## Total Test Count

**130+ comprehensive unit tests** covering:
- ✅ Activity lifecycle
- ✅ Data models
- ✅ Caching and persistence
- ✅ UI adapters
- ✅ Navigation
- ✅ Session management
- ✅ Error handling
- ✅ Edge cases