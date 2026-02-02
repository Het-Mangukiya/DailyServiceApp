#!/bin/bash

# Run Tests Script for DailyServiceApp
# This script runs the comprehensive unit test suite

set -e

echo "=========================================="
echo "DailyServiceApp Test Suite"
echo "=========================================="
echo ""

# Check Java installation
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 17 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
echo "Java version: $JAVA_VERSION"
echo ""

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

echo ""
echo "=========================================="
echo "Running Unit Tests"
echo "=========================================="
echo ""

# Run all unit tests
./gradlew test --info

echo ""
echo "=========================================="
echo "Running Specific Test Classes"
echo "=========================================="
echo ""

# Run individual test classes for detailed feedback
TEST_CLASSES=(
    "com.dailyserviceapp.data.models.BillTest"
    "com.dailyserviceapp.data.models.CustomerTest"
    "com.dailyserviceapp.core.offline.OfflineCacheTest"
    "com.dailyserviceapp.billing.BillAdapterTest"
    "com.dailyserviceapp.core.base.BaseActivityTest"
    "com.dailyserviceapp.SplashActivityTest"
    "com.dailyserviceapp.debug.TestDataBroadcastReceiverTest"
)

for TEST_CLASS in "${TEST_CLASSES[@]}"; do
    echo ""
    echo "Running $TEST_CLASS..."
    ./gradlew test --tests "$TEST_CLASS"
done

echo ""
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo ""

# Display test results location
echo "Test reports generated at:"
echo "  HTML: app/build/reports/tests/testDebugUnitTest/index.html"
echo "  XML:  app/build/test-results/testDebugUnitTest/"
echo ""

# Count test results
if [ -d "app/build/test-results/testDebugUnitTest" ]; then
    TEST_COUNT=$(find app/build/test-results/testDebugUnitTest -name "*.xml" | wc -l)
    echo "Total test files: $TEST_COUNT"
fi

echo ""
echo "=========================================="
echo "All tests completed successfully!"
echo "=========================================="