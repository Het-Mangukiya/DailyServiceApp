# Daily Service App - Requirements & Architecture Document

**Version:** 1.0  
**Date:** January 7, 2026  
**Language:** Java  
**Platform:** Android

---

## 1. FUNCTIONAL REQUIREMENTS

### 1.1 Authentication & User Management

- **FR-1.1**: Users can sign up with email/phone number and password
- **FR-1.2**: Users can log in using registered credentials
- **FR-1.3**: Users can log out securely from the application
- **FR-1.4**: System supports password reset via email/OTP
- **FR-1.5**: Users select role during registration (Service Provider or Customer)
- **FR-1.6**: Profile management with ability to update personal details
- **FR-1.7**: Email verification for new accounts
- **FR-1.8**: Session management with automatic timeout after inactivity

### 1.2 Customer Management (Provider Features)

- **FR-2.1**: Providers can add new customers with details (name, address, phone, service type, rate per unit)
- **FR-2.2**: Providers can edit existing customer information
- **FR-2.3**: Providers can view a searchable list of all customers
- **FR-2.4**: Providers can delete or archive inactive customers
- **FR-2.5**: Providers can categorize customers by service type (milk, maid, newspaper, laundry)
- **FR-2.6**: Providers can set custom billing rates per customer
- **FR-2.7**: Providers can add notes to customer profiles
- **FR-2.8**: System prevents duplicate customer entries

### 1.3 Daily Service Entry

- **FR-3.1**: Providers can mark daily service delivery (delivered/skipped) for each customer
- **FR-3.2**: Providers can specify quantity/units delivered (liters, hours, copies, kg)
- **FR-3.3**: Providers can add notes for specific entries (e.g., "holiday", "customer not home")
- **FR-3.4**: System displays calendar view showing monthly service history
- **FR-3.5**: Providers can edit entries for current and previous days
- **FR-3.6**: Support for bulk entry (mark multiple customers at once)
- **FR-3.7**: System auto-saves entries to prevent data loss
- **FR-3.8**: Offline mode for entry recording with sync when online

### 1.4 Billing & Invoicing

- **FR-4.1**: System automatically generates bills on the 1st of each month
- **FR-4.2**: Bill calculation: (total units delivered × rate) + extras - adjustments
- **FR-4.3**: Providers can add extra charges (e.g., additional services)
- **FR-4.4**: Providers can apply discounts or adjustments
- **FR-4.5**: Bills include itemized breakdown of services
- **FR-4.6**: System generates bills in PDF format
- **FR-4.7**: Providers can manually generate bills for any date range
- **FR-4.8**: Bills include provider and customer details, tax information

### 1.5 Payment Tracking

- **FR-5.1**: Providers can mark bills as paid with payment date and method
- **FR-5.2**: System tracks payment status (pending, partial, paid, overdue)
- **FR-5.3**: Providers can record partial payments
- **FR-5.4**: Customers can view their billing history
- **FR-5.5**: Customers can see outstanding amounts
- **FR-5.6**: Payment history displays all transactions chronologically
- **FR-5.7**: System calculates and displays total receivables for providers

### 1.6 Reports & Analytics

- **FR-6.1**: Monthly revenue report showing total income
- **FR-6.2**: Customer-wise payment summary report
- **FR-6.3**: Service delivery statistics (total deliveries, skipped days)
- **FR-6.4**: Outstanding payments report
- **FR-6.5**: Date range filtering for all reports
- **FR-6.6**: Export reports as PDF or CSV
- **FR-6.7**: Visual charts for revenue trends
- **FR-6.8**: Customer activity analysis

### 1.7 Notifications

- **FR-7.1**: Push notifications when monthly bill is generated
- **FR-7.2**: Payment reminder notifications to customers (configurable days before/after due date)
- **FR-7.3**: Payment confirmation notifications to providers
- **FR-7.4**: Service delivery confirmation to customers (optional)
- **FR-7.5**: System alerts for missed entries
- **FR-7.6**: In-app notification center showing all alerts
- **FR-7.7**: Notification preferences management

### 1.8 Customer Features

- **FR-8.1**: Customers can view their linked service providers
- **FR-8.2**: Customers can see daily service history
- **FR-8.3**: Customers can view current and past bills
- **FR-8.4**: Customers can download bill PDFs
- **FR-8.5**: Customers receive notifications for new bills
- **FR-8.6**: Customers can view payment history

---

## 2. NON-FUNCTIONAL REQUIREMENTS

### 2.1 Performance

- **NFR-1.1**: App launch time ≤ 3 seconds on mid-range devices
- **NFR-1.2**: Daily entry submission response time ≤ 1 second
- **NFR-1.3**: Bill generation processing time ≤ 5 seconds per customer
- **NFR-1.4**: List scrolling at minimum 60 FPS
- **NFR-1.5**: Support for minimum 5,000 customers per provider account
- **NFR-1.6**: Database queries return results within 2 seconds
- **NFR-1.7**: Offline mode operations with instant feedback

### 2.2 Security

- **NFR-2.1**: All data transmission encrypted using TLS 1.3
- **NFR-2.2**: Password minimum requirements: 8 characters, alphanumeric with special characters
- **NFR-2.3**: Passwords hashed using industry-standard algorithms
- **NFR-2.4**: Session tokens expire after 30 minutes of inactivity
- **NFR-2.5**: Role-based access control enforced at API level
- **NFR-2.6**: Input validation and sanitization to prevent injection attacks
- **NFR-2.7**: Secure storage of sensitive data using Android Keystore
- **NFR-2.8**: Multi-factor authentication option (phone OTP)
- **NFR-2.9**: Audit logs for critical operations

### 2.3 Reliability & Availability

- **NFR-3.1**: System uptime of 99.5% or higher
- **NFR-3.2**: Automatic data synchronization when connectivity restored
- **NFR-3.3**: Local data backup every 24 hours
- **NFR-3.4**: Cloud backup with 30-day retention
- **NFR-3.5**: Graceful error handling with user-friendly messages
- **NFR-3.6**: Crash rate < 1% of sessions
- **NFR-3.7**: Automatic retry mechanism for failed operations

### 2.4 Usability

- **NFR-4.1**: Adherence to Material Design 3 guidelines
- **NFR-4.2**: Maximum 3 taps to access any core feature
- **NFR-4.3**: Consistent UI patterns across all screens
- **NFR-4.4**: Contextual help and tooltips for first-time users
- **NFR-4.5**: Support for multiple languages (English, Hindi minimum)
- **NFR-4.6**: Font size adjustability for accessibility
- **NFR-4.7**: Color contrast ratio meeting WCAG 2.1 AA standards
- **NFR-4.8**: Touch targets minimum 48dp for accessibility

### 2.5 Scalability

- **NFR-5.1**: Support for 100,000+ concurrent users
- **NFR-5.2**: Horizontal scaling capability for backend services
- **NFR-5.3**: Database partitioning for large datasets
- **NFR-5.4**: Efficient pagination (50 items per page)
- **NFR-5.5**: CDN integration for static content delivery
- **NFR-5.6**: Load balancing for distributed traffic

### 2.6 Compatibility

- **NFR-6.1**: Minimum Android version: 7.0 (API 24)
- **NFR-6.2**: Target Android version: 14 (API 34)
- **NFR-6.3**: Support for screen sizes from 4.0" to 10.1"
- **NFR-6.4**: Portrait and landscape orientations
- **NFR-6.5**: Optimization for devices with 2GB+ RAM
- **NFR-6.6**: Support for various screen densities (mdpi to xxxhdpi)

### 2.7 Maintainability

- **NFR-7.1**: Modular architecture for independent feature updates
- **NFR-7.2**: Code coverage minimum 70% for unit tests
- **NFR-7.3**: Comprehensive documentation for all APIs
- **NFR-7.4**: Logging framework for debugging
- **NFR-7.5**: Version control with semantic versioning

### 2.8 Compliance

- **NFR-8.1**: GDPR compliance for data protection
- **NFR-8.2**: User data deletion capability
- **NFR-8.3**: Privacy policy and terms of service
- **NFR-8.4**: Data retention policies

---

## 3. APP ARCHITECTURE

### Architecture Pattern: **Clean Architecture + MVP/MVVM**

```
┌─────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                          │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐   │
│  │  Activities  │──▶│  Fragments   │──▶│   ViewModels     │   │
│  │   (Views)    │   │   (Views)    │   │  (State Mgmt)    │   │
│  └──────────────┘   └──────────────┘   └──────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  UI Components: Adapters, Custom Views, Navigation       │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                        DOMAIN LAYER                              │
│  ┌────────────────────┐          ┌──────────────────────────┐  │
│  │    Use Cases       │          │    Domain Models         │  │
│  │  - LoginUseCase    │          │  - User, Customer        │  │
│  │  - AddCustomer     │          │  - Service, Bill         │  │
│  │  - CreateBill      │          │  - Payment, Report       │  │
│  │  - RecordService   │          │                          │  │
│  └────────────────────┘          └──────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Repository Interfaces (Contracts)                 │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                         DATA LAYER                               │
│  ┌────────────────────────────────────────────────────────┐    │
│  │           Repository Implementations                    │    │
│  └───────────────────┬──────────────────┬─────────────────┘    │
│                      │                  │                       │
│  ┌──────────────────▼──────┐   ┌──────▼──────────────────┐    │
│  │   Local Data Source     │   │  Remote Data Source      │    │
│  │  ┌──────────────────┐  │   │  ┌──────────────────┐   │    │
│  │  │   Room Database  │  │   │  │   Firebase       │   │    │
│  │  │   - DAOs         │  │   │  │   - Firestore    │   │    │
│  │  │   - Entities     │  │   │  │   - Auth         │   │    │
│  │  │   - Migrations   │  │   │  │   - Storage      │   │    │
│  │  └──────────────────┘  │   │  └──────────────────┘   │    │
│  │  ┌──────────────────┐  │   │  ┌──────────────────┐   │    │
│  │  │ SharedPreferences│  │   │  │   API Service    │   │    │
│  │  └──────────────────┘  │   │  └──────────────────┘   │    │
│  └─────────────────────────┘   └─────────────────────────┘    │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                    CROSS-CUTTING CONCERNS                         │
│  - Dependency Injection (Dagger 2)                               │
│  - Network Monitor                                                │
│  - Error Handling                                                 │
│  - Logging & Analytics                                            │
│  - Background Jobs (WorkManager)                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities:

#### Presentation Layer (Java)
- **Activities**: Host fragments, handle navigation, lifecycle management
- **Fragments**: Display UI, collect user input, observe ViewModel state
- **ViewModels**: Business logic for UI, state management, trigger use cases
- **Adapters**: Bind data to RecyclerViews efficiently
- **Navigation**: Handle screen transitions

#### Domain Layer (Pure Java - No Android Dependencies)
- **Use Cases**: Single-responsibility business operations
- **Domain Models**: Pure business entities (POJOs)
- **Repository Interfaces**: Abstract data operations

#### Data Layer (Java + Android Framework)
- **Repositories**: Coordinate between local and remote data sources
- **Room Database**: Offline-first data persistence
- **Firebase**: Cloud storage, authentication, real-time sync
- **Data Mappers**: Convert between data models and domain models

### Key Architectural Principles:

1. **Dependency Rule**: Dependencies point inward (Presentation → Domain ← Data)
2. **Single Responsibility**: Each class has one clear purpose
3. **Dependency Inversion**: High-level modules don't depend on low-level modules
4. **Offline-First**: Local database as source of truth, sync to cloud
5. **Separation of Concerns**: Clear boundaries between layers

---

## 4. FIREBASE SERVICES REQUIRED

### 4.1 Firebase Authentication
**Purpose**: User authentication and session management

**Features Used**:
- Email/Password authentication
- Phone number authentication (OTP)
- Password reset
- Email verification
- Custom claims for role-based access (Provider/Customer)

**Java Implementation**:
```java
FirebaseAuth mAuth = FirebaseAuth.getInstance();
mAuth.signInWithEmailAndPassword(email, password);
mAuth.createUserWithEmailAndPassword(email, password);
mAuth.sendPasswordResetEmail(email);
mAuth.signOut();
```

---

### 4.2 Cloud Firestore (NoSQL Database)
**Purpose**: Primary data storage with real-time synchronization

**Database Structure**:
```
/users/{userId}
  - email, name, phone, role, createdAt
  
/providers/{providerId}
  - businessName, serviceType, address, gstNumber
  
/customers/{customerId}
  - name, address, phone, providerId, serviceType
  - rate, startDate, status, notes
  
/serviceEntries/{entryId}
  - providerId, customerId, date, quantity
  - delivered, notes, createdAt
  
/bills/{billId}
  - providerId, customerId, month, year
  - totalAmount, daysServed, paidStatus, pdfUrl
  - items[], extras[], adjustments[]
  
/payments/{paymentId}
  - billId, amount, paymentDate, method
  - providerId, customerId
  
/notifications/{notificationId}
  - userId, title, message, type, read, timestamp
```

**Features Used**:
- Real-time listeners for live updates
- Offline persistence
- Compound queries with indexes
- Batch writes for transactions
- Security rules for access control

---

### 4.3 Firebase Cloud Functions
**Purpose**: Serverless backend logic

**Functions to Implement**:

1. **generateMonthlyBills** (Scheduled - 1st of each month)
   - Iterate all active customers
   - Calculate bills from service entries
   - Create bill documents
   - Trigger notifications

2. **onBillCreate** (Firestore Trigger)
   - Generate PDF invoice
   - Upload to Cloud Storage
   - Send notification to customer

3. **sendPaymentReminders** (Scheduled - Daily)
   - Check overdue bills
   - Send reminder notifications

4. **onUserCreate** (Auth Trigger)
   - Create user profile in Firestore
   - Send welcome notification

5. **calculateBillAmount** (Callable Function)
   - Server-side bill calculation for accuracy
   - Apply business rules

---

### 4.4 Firebase Cloud Messaging (FCM)
**Purpose**: Push notifications

**Notification Types**:
- Bill generated notification
- Payment reminder
- Payment received confirmation
- Service delivery updates
- System announcements

**Topics**:
- `bill_notifications`
- `payment_reminders`
- `service_updates`

---

### 4.5 Firebase Cloud Storage
**Purpose**: Store generated files and images

**Storage Structure**:
```
/bills/{providerId}/{billId}.pdf
/profile_images/{userId}/profile.jpg
/reports/{providerId}/{reportId}.pdf
```

---

### 4.6 Firebase Analytics
**Purpose**: User behavior tracking and app insights

**Events to Track**:
- `user_login`, `user_signup`
- `customer_added`, `customer_edited`
- `service_entry_added`
- `bill_generated`, `bill_viewed`
- `payment_recorded`
- `report_generated`

---

### 4.7 Firebase Crashlytics
**Purpose**: Crash reporting and stability monitoring

---

### 4.8 Firebase Performance Monitoring
**Purpose**: App performance tracking

---

### 4.9 Firebase Remote Config (Optional)
**Purpose**: Dynamic configuration without app updates

---

### 4.10 Firebase Cloud Scheduler
**Purpose**: Scheduled tasks via Cloud Functions

**Schedules**:
- Monthly bill generation: `0 0 1 * *` (1st of every month at midnight)
- Daily payment reminders: `0 9 * * *` (9 AM daily)

---

## 5. HIGH-LEVEL MODULE BREAKDOWN

### Module 1: **Authentication Module** 🔐
**Package**: `com.dailyservice.auth`

**Java Classes**:
- `LoginActivity.java`
- `SignupActivity.java`
- `ForgotPasswordActivity.java`
- `AuthViewModel.java`
- `LoginUseCase.java`, `SignupUseCase.java`, `LogoutUseCase.java`
- `AuthRepository.java`, `AuthRepositoryImpl.java`
- `FirebaseAuthService.java`

**Responsibilities**:
- User registration with role selection
- Login with email/phone and password
- Password reset functionality
- Session management
- Role-based routing after login

---

### Module 2: **Dashboard Module** 📊
**Package**: `com.dailyservice.dashboard`

**Java Classes**:
- `DashboardActivity.java`
- `ProviderDashboardFragment.java`
- `CustomerDashboardFragment.java`
- `DashboardViewModel.java`
- `GetDashboardStatsUseCase.java`

**Responsibilities**:
- Display role-specific dashboard
- Show key metrics and statistics
- Quick action buttons
- Navigation hub

---

### Module 3: **Customer Management Module** 👥
**Package**: `com.dailyservice.customer`

**Java Classes**:
- `CustomerListActivity.java`
- `CustomerListFragment.java`
- `CustomerDetailFragment.java`
- `AddEditCustomerActivity.java`
- `CustomerViewModel.java`
- `AddCustomerUseCase.java`, `UpdateCustomerUseCase.java`, `DeleteCustomerUseCase.java`
- `CustomerRepository.java`, `CustomerRepositoryImpl.java`
- `CustomerAdapter.java` (RecyclerView)

**Responsibilities**:
- CRUD operations for customers
- Customer search and filtering
- View customer service history

---

### Module 4: **Service Entry Module** 📝
**Package**: `com.dailyservice.service`

**Java Classes**:
- `ServiceEntryActivity.java`
- `CalendarViewFragment.java`
- `QuickEntryFragment.java`
- `ServiceEntryViewModel.java`
- `RecordServiceUseCase.java`, `UpdateServiceUseCase.java`
- `ServiceRepository.java`, `ServiceRepositoryImpl.java`
- `CalendarAdapter.java`, `ServiceEntryAdapter.java`

**Responsibilities**:
- Record daily service deliveries
- Calendar view for monthly overview
- Bulk entry for multiple customers
- Offline support with sync

---

### Module 5: **Billing Module** 💰
**Package**: `com.dailyservice.billing`

**Java Classes**:
- `BillListActivity.java`
- `BillListFragment.java`
- `BillDetailActivity.java`
- `CreateBillActivity.java`
- `BillViewModel.java`
- `GenerateBillUseCase.java`, `GetBillsUseCase.java`
- `BillingRepository.java`, `BillingRepositoryImpl.java`
- `BillPdfGenerator.java`
- `BillAdapter.java`

**Responsibilities**:
- Auto-generate monthly bills
- Calculate bill amounts
- Generate PDF invoices
- View bill history

---

### Module 6: **Payment Module** 💳
**Package**: `com.dailyservice.payment`

**Java Classes**:
- `PaymentActivity.java`
- `PaymentHistoryFragment.java`
- `RecordPaymentDialog.java`
- `PaymentViewModel.java`
- `RecordPaymentUseCase.java`, `GetPaymentsUseCase.java`
- `PaymentRepository.java`, `PaymentRepositoryImpl.java`
- `PaymentAdapter.java`

**Responsibilities**:
- Record payment receipt
- Track payment status
- Payment history
- Outstanding amount calculation

---

### Module 7: **Reports Module** 📈
**Package**: `com.dailyservice.reports`

**Java Classes**:
- `ReportsActivity.java`
- `RevenueReportFragment.java`
- `CustomerReportFragment.java`
- `ServiceReportFragment.java`
- `ReportViewModel.java`
- `GenerateReportUseCase.java`
- `ReportExporter.java`
- `ChartRenderer.java`

**Responsibilities**:
- Generate various reports
- Data visualization
- Export to PDF/CSV

---

### Module 8: **Notifications Module** 🔔
**Package**: `com.dailyservice.notifications`

**Java Classes**:
- `FCMService.java` (extends FirebaseMessagingService)
- `NotificationHelper.java`
- `NotificationListActivity.java`
- `NotificationViewModel.java`
- `NotificationRepository.java`, `NotificationRepositoryImpl.java`

**Responsibilities**:
- Handle FCM push notifications
- Display local notifications
- Notification center

---

### Module 9: **Profile & Settings Module** ⚙️
**Package**: `com.dailyservice.profile`

**Java Classes**:
- `ProfileActivity.java`
- `SettingsActivity.java`
- `ProfileViewModel.java`
- `UpdateProfileUseCase.java`
- `ProfileRepository.java`, `ProfileRepositoryImpl.java`

**Responsibilities**:
- View and edit profile
- Change password
- App settings
- Logout

---

### Module 10: **Common/Core Module** 🛠️
**Package**: `com.dailyservice.core`

**Java Classes**:
- `BaseActivity.java`, `BaseFragment.java`
- `BaseViewModel.java`
- `NetworkMonitor.java`
- `DateUtils.java`, `CurrencyUtils.java`, `ValidationUtils.java`
- `Constants.java`
- `Resource.java` (wrapper for API responses)
- `UiState.java`

**Responsibilities**:
- Shared utilities and helpers
- Base classes for consistency
- Common UI components
- Network connectivity monitoring
- Error handling

---

## TECHNOLOGY STACK (JAVA)

### Core Technologies:
- **Language**: Java 11+
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle

### Android Jetpack Components:
- **ViewModel & LiveData**: State management
- **Room**: Local database
- **WorkManager**: Background tasks
- **Navigation Component**: Navigation graph
- **ViewBinding**: UI binding
- **Paging 3**: Efficient list loading
- **Lifecycle**: Lifecycle-aware components

### Dependency Injection:
- **Dagger 2**: Dependency injection

### Asynchronous:
- **AsyncTask** / **ExecutorService**: Background operations
- **LiveData**: Observable data holder

### Networking:
- **Retrofit**: REST API (if needed)
- **Firebase SDK**: Firebase services
- **OkHttp**: HTTP client

### UI:
- **Material Design Components**: UI components
- **RecyclerView**: Efficient lists
- **MPAndroidChart**: Charts and graphs
- **Glide**: Image loading

### PDF Generation:
- **iTextPDF**: PDF creation

### Testing:
- **JUnit 4**: Unit testing
- **Mockito**: Mocking
- **Espresso**: UI testing

---

## IMPLEMENTATION PHASES

### Phase 1: Foundation (Weeks 1-2)
- Project setup and dependencies
- Authentication module
- Firebase integration
- Basic navigation structure

### Phase 2: Core Features (Weeks 3-5)
- Customer management
- Service entry module
- Dashboard implementation

### Phase 3: Billing & Payments (Weeks 6-7)
- Billing module
- Payment tracking
- PDF generation

### Phase 4: Advanced Features (Weeks 8-9)
- Reports module
- Notifications
- Offline support

### Phase 5: Polish & Testing (Weeks 10-12)
- UI/UX refinement
- Comprehensive testing
- Performance optimization
- Bug fixes

---

## DATA FLOW EXAMPLE: Recording Daily Service Entry

```
User marks service delivered
         ↓
ServiceEntryActivity (UI)
         ↓
ServiceEntryViewModel
         ↓
RecordServiceUseCase (Domain)
         ↓
ServiceRepository (Data)
         ↓
    ┌────┴────┐
    ↓         ↓
Room DB   Firestore
(Local)   (Remote)
    ↓         ↓
LiveData ← Repository
    ↓
ViewModel observes
    ↓
UI updates with success
```

---

**End of Requirements & Architecture Document**
