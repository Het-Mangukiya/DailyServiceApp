# Testing Guide for DailyServiceApp

## Overview
This guide explains how to run and maintain the comprehensive test suite for the DailyServiceApp Android application.

## Prerequisites

### Required Software:
- JDK 17 or higher
- Android SDK (API level 24-35)
- Gradle 7.0+ (included via wrapper)
- Android Studio (recommended) or command-line tools

### Verify Prerequisites:
```bash
# Check Java version
java -version  # Should show version 17+

# Check Gradle
./gradlew --version

# Check Android SDK
echo $ANDROID_HOME  # Should point to your SDK location
```

## Test Structure

```
app/src/test/java/com/dailyserviceapp/
├── SplashActivityTest.java
├── auth/
│   ├── LoginActivityTest.java
│   └── SignupActivityTest.java
├── billing/
│   └── BillAdapterTest.java
├── core/
│   ├── base/
│   │   └── BaseActivityTest.java
│   └── offline/
│       └── OfflineCacheTest.java
└── debug/
    └── TestDataBroadcastReceiverTest.java
```

## Running Tests

### 1. Run All Tests
```bash
./gradlew test
```

### 2. Run Tests for Specific Build Variant
```bash
# Debug tests
./gradlew testDebugUnitTest

# Release tests
./gradlew testReleaseUnitTest
```

### 3. Run Specific Test Class
```bash
./gradlew test --tests "com.dailyserviceapp.SplashActivityTest"
```

### 4. Run Specific Test Method
```bash
./gradlew test --tests "com.dailyserviceapp.auth.LoginActivityTest.testInvalidEmailValidation"
```

### 5. Run Tests with Pattern
```bash
# Run all authentication tests
./gradlew test --tests "com.dailyserviceapp.auth.*"

# Run all billing tests
./gradlew test --tests "com.dailyserviceapp.billing.*"
```

### 6. Run Tests with Stack Trace
```bash
./gradlew test --stacktrace
```

### 7. Run Tests with Info Logging
```bash
./gradlew test --info
```

## Viewing Test Results

### Command Line Output
Test results are displayed in the terminal after running tests.

### HTML Reports
After running tests, open the HTML report:
```bash
# Location of test reports
open app/build/reports/tests/testDebugUnitTest/index.html
```

### Test Report Contents:
- Total tests run
- Passed/Failed/Skipped counts
- Execution time
- Detailed failure messages
- Stack traces for failures

## Running Tests in Android Studio

### Run All Tests:
1. Open Android Studio
2. Right-click on `app/src/test` directory
3. Select "Run 'Tests in 'test''"

### Run Single Test Class:
1. Open the test file (e.g., `LoginActivityTest.java`)
2. Click the green play button next to the class name
3. Or right-click class name → "Run 'LoginActivityTest'"

### Run Single Test Method:
1. Open the test file
2. Click the green play button next to the test method
3. Or right-click method name → "Run 'testMethodName()'"

### View Results in Android Studio:
- Results appear in the "Run" panel at bottom
- Green checkmarks indicate passing tests
- Red X marks indicate failures
- Click on a test to see details

## Test Coverage

### Generate Coverage Report:
```bash
./gradlew testDebugUnitTest jacocoTestReport
```

### View Coverage Report:
```bash
open app/build/reports/jacoco/testDebugUnitTest/html/index.html
```

### Coverage Metrics:
- **Line Coverage:** Percentage of lines executed
- **Branch Coverage:** Percentage of conditional branches tested
- **Method Coverage:** Percentage of methods invoked
- **Class Coverage:** Percentage of classes loaded

## Debugging Failed Tests

### 1. Read the Failure Message
```
testInvalidEmailValidation FAILED
    java.lang.AssertionError: Email error should be set
        at LoginActivityTest.testInvalidEmailValidation(LoginActivityTest.java:XX)
```

### 2. Check Stack Trace
The stack trace shows:
- Where the test failed
- What assertion failed
- Expected vs actual values

### 3. Run Single Test with Logging
```bash
./gradlew test --tests "TestClass.testMethod" --info
```

### 4. Add Debug Output to Test
```java
@Test
public void testSomething() {
    System.out.println("Debug: value = " + value);
    assertEquals(expected, actual);
}
```

### 5. Use Debugger in Android Studio
1. Set breakpoint in test method
2. Right-click test → "Debug 'testMethod()'"
3. Step through code with debugger

## Common Issues and Solutions

### Issue 1: "JAVA_HOME not set"
**Solution:**
```bash
export JAVA_HOME=/path/to/jdk-17
```

### Issue 2: "Android SDK not found"
**Solution:**
```bash
export ANDROID_HOME=/path/to/android-sdk
```
Or create `local.properties`:
```properties
sdk.dir=/path/to/android-sdk
```

### Issue 3: "Robolectric ClassNotFoundException"
**Solution:**
Ensure build.gradle has:
```gradle
testImplementation 'org.robolectric:robolectric:4.11.1'
```

### Issue 4: "Test execution failed"
**Solution:**
```bash
# Clean and rebuild
./gradlew clean test
```

### Issue 5: "Out of memory"
**Solution:**
Increase heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m
```

## Best Practices

### Writing New Tests:

1. **Follow naming convention:**
   ```java
   testMethodName_StateUnderTest_ExpectedBehavior()
   // Example:
   testLogin_WithInvalidEmail_ShowsErrorMessage()
   ```

2. **Use Arrange-Act-Assert pattern:**
   ```java
   @Test
   public void testSomething() {
       // Arrange: Set up test data
       String email = "test@example.com";

       // Act: Execute the method under test
       boolean result = validator.isValidEmail(email);

       // Assert: Verify the result
       assertTrue("Email should be valid", result);
   }
   ```

3. **Keep tests independent:**
   - Each test should run independently
   - Don't rely on test execution order
   - Clean up in @After method if needed

4. **Test one thing per test:**
   ```java
   // Good
   @Test
   public void testEmailValidation() {
       assertTrue(isValidEmail("test@example.com"));
   }

   @Test
   public void testPasswordValidation() {
       assertTrue(isValidPassword("Password123"));
   }

   // Bad - tests multiple things
   @Test
   public void testValidation() {
       assertTrue(isValidEmail("test@example.com"));
       assertTrue(isValidPassword("Password123"));
   }
   ```

5. **Use descriptive assertion messages:**
   ```java
   assertEquals("User ID should match", expectedId, actualId);
   ```

### Maintaining Tests:

1. **Run tests before committing:**
   ```bash
   ./gradlew test
   ```

2. **Update tests when code changes:**
   - If you modify a class, update its tests
   - Add tests for new methods
   - Remove tests for deleted methods

3. **Keep test code clean:**
   - Extract common setup to @Before
   - Use helper methods for repeated logic
   - Remove commented-out code

4. **Review test coverage:**
   ```bash
   ./gradlew jacocoTestReport
   ```

## Continuous Integration

### GitHub Actions Example:
Create `.github/workflows/tests.yml`:
```yaml
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
    - uses: actions/checkout@v2

    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Run tests
      run: ./gradlew test

    - name: Upload test reports
      if: always()
      uses: actions/upload-artifact@v2
      with:
        name: test-reports
        path: app/build/reports/tests/
```

### Pre-commit Hook:
Create `.git/hooks/pre-commit`:
```bash
#!/bin/sh
echo "Running tests..."
./gradlew test

if [ $? -ne 0 ]; then
    echo "Tests failed! Commit aborted."
    exit 1
fi
```

Make it executable:
```bash
chmod +x .git/hooks/pre-commit
```

## Performance Tips

### 1. Run Tests in Parallel
Add to `gradle.properties`:
```properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

### 2. Skip Tests When Not Needed
```bash
./gradlew build -x test
```

### 3. Run Only Changed Tests
```bash
./gradlew test --rerun-tasks
```

### 4. Use Test Filters
```bash
# Run only fast tests (if you tag them)
./gradlew test --tests "*Fast*"
```

## Test Metrics

### Current Test Suite:
- **Total Test Files:** 7
- **Total Test Cases:** 210+
- **Average Execution Time:** ~30 seconds
- **Coverage:** ~75% (estimated)

### Goals:
- ✅ All critical paths covered
- ✅ Edge cases tested
- ✅ Error conditions handled
- ✅ UI validation tested
- ✅ Data layer tested
- 🎯 Target: 85%+ coverage

## Getting Help

### Resources:
- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Robolectric Documentation](http://robolectric.org/)
- [Android Testing Guide](https://developer.android.com/training/testing)

### Common Commands Reference:
```bash
# Clean build
./gradlew clean

# Run all tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport

# Run specific test
./gradlew test --tests "ClassName.methodName"

# Run with detailed output
./gradlew test --info --stacktrace

# List all test tasks
./gradlew tasks --group verification
```

## Troubleshooting Checklist

- [ ] Is JAVA_HOME set correctly?
- [ ] Is ANDROID_HOME set correctly?
- [ ] Did you run `./gradlew clean`?
- [ ] Are all dependencies in build.gradle?
- [ ] Is internet connection working (for first-time dependency download)?
- [ ] Is there enough disk space?
- [ ] Is there enough memory (check gradle.properties)?
- [ ] Are file permissions correct (`chmod +x gradlew`)?

---

**Last Updated:** 2026-02-02
**Test Framework Version:** JUnit 4.13.2, Mockito 5.8.0, Robolectric 4.11.1
**Maintained By:** DailyDrop Development Team