<p align="center">
  <h1 align="center">🥛 DailyDrop</h1>
  <p align="center">
    <strong>Smart daily service management for providers &amp; customers</strong>
  </p>
  <p align="center">
    <a href="#features">Features</a> •
    <a href="#tech-stack">Tech Stack</a> •
    <a href="#architecture">Architecture</a> •
    <a href="#getting-started">Getting Started</a> •
    <a href="#ci--cd">CI / CD</a> •
    <a href="#contributing">Contributing</a>
  </p>
  <p align="center">
    <img src="https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white" alt="Min SDK">
    <img src="https://img.shields.io/badge/Java%20%7C%20Kotlin-17-007396?logo=openjdk&logoColor=white" alt="Language">
    <img src="https://img.shields.io/badge/Firebase-Backend-FFCA28?logo=firebase&logoColor=black" alt="Firebase">
    <img src="https://github.com/Het-Mangukiya/DailyServiceApp/actions/workflows/android-ci.yml/badge.svg" alt="Android CI">
    <img src="https://github.com/Het-Mangukiya/DailyServiceApp/actions/workflows/firestore-rules-tests.yml/badge.svg" alt="Firestore Rules Tests">
  </p>
</p>

---

## 📖 Introduction

**DailyDrop** is an Android application designed to digitize and streamline daily service operations — such as milk, water, newspaper, or tiffin delivery — for both **service providers** and **customers**.

**Providers** get a powerful dashboard to manage customers, log daily deliveries, generate bills, record payments, and view business analytics. **Customers** get a self-service portal to track their deliveries, view bills, raise complaints, and request quantity changes — all in real time.

The app uses a serverless Firebase backend, ensuring zero infrastructure overhead, real-time data sync, and effortless scalability.

---

## ✨ Features

### 🔐 Authentication & Onboarding
| Function | Description |
|---|---|
| **Email / Password Sign-Up & Login** | Standard credential-based authentication via Firebase Auth |
| **Google Sign-In** | One-tap login using Google account (Play Services Auth) |
| **Forgot Password** | Email-based password reset flow |
| **Role-Based Routing** | Splash screen auto-routes users to Provider or Customer home based on role |
| **Deep Link / Invite Handling** | Supports `dailydrop://invite` and `https://dailydrop.app/invite` deep links for onboarding |

---

### 🏪 Provider Dashboard & Management
| Function | Description |
|---|---|
| **Provider Dashboard** | At-a-glance stats — today's deliveries, pending payments, active customers, revenue overview |
| **Real-Time Customer List** | Live Firestore snapshot listener for instant customer updates |
| **Add Customer** | Create new customer with name, phone, address, area, service type, rate, and default quantity |
| **Edit Customer** | Update customer details, rate per unit, default quantity, and vacation status |
| **Delete Customer** | Cascade-delete customer along with all associated service entries, bills, and payments |
| **Customer Detail View** | Detailed customer profile with delivery history, payment history, and ledger summary |
| **Customer Status Management** | Mark customers as Active / Inactive / On Vacation |
| **Avatar Generation** | Auto-generates colour-coded letter avatars for customer profiles |

---

### 📋 Daily Service Entry
| Function | Description |
|---|---|
| **Mark Delivery** | Log daily delivery per customer with quantity, rate, and delivered status |
| **Batch Delivery** | Mark deliveries for multiple customers in a single transactional operation (up to 200 at once) |
| **Duplicate Prevention** | Deterministic entry IDs (`providerId_customerId_yyyyMMdd`) prevent double-entry per day |
| **Atomic Transactions** | Firestore transactions atomically update service entry + customer's lent (outstanding) amount |
| **Date-Range Querying** | Fetch service entries filtered by provider, customer, and date range with index fallback |
| **Real-Time Sync** | Firestore snapshot listeners for live delivery status updates |

---

### 💰 Billing
| Function | Description |
|---|---|
| **Auto Bill Generation** | Generate monthly bills with detailed day-by-day delivery breakdown |
| **Bill List View** | Browse all bills with status indicators (Pending / Paid / Overdue) |
| **Bill Detail View** | Drill into individual bills showing line items, service total, payments received, and outstanding |
| **Customer Ledger Calculator** | Computes running ledger summary (total billed, total paid, net outstanding) per customer |
| **PDF Invoice Generation** | Generate professional A4 PDF invoices with provider/customer info, delivery table, and totals |
| **PDF Sharing** | Share generated PDF invoices via Android share sheet (FileProvider) |
| **Overdue Bill Detection** | Automatically flags bills past due date that remain unpaid |

---

### 💳 Payment Recording
| Function | Description |
|---|---|
| **Record Payment** | Log full or partial payments against customer balances |
| **Payment History** | View complete payment history per customer with date and amount |
| **Outstanding Balance Tracking** | Real-time tracking of lent amounts and outstanding balances per customer |
| **Payment Status** | Track payment status (Paid / Partial / Pending) per billing period |
| **Date-Range Payment Queries** | Fetch payments filtered by provider and date range |

---

### 📊 Reports & Analytics
| Function | Description |
|---|---|
| **Revenue Dashboard** | Animated counters showing total revenue, deliveries, active customers, total payments, and overdue bills |
| **Date Range Filter** | Custom start/end date picker to scope reports to any period |
| **Bar Chart** | Top 5 customers by revenue visualized in an interactive bar chart (MPAndroidChart) |
| **Pie Chart** | Customer revenue share displayed as an interactive pie chart with percentage labels |
| **Chart Toggle** | Switch between bar and pie chart views with smooth animations |
| **Service Type Breakdown** | Revenue breakdown by service type (e.g., Milk, Water, Newspaper) with contribution percentages |
| **Top Customers Ranking** | Ranked list of highest-revenue customers for the selected period |
| **Animated Counters** | Smooth count-up animations on all KPI cards |

---

### 🤖 AI & Sales Prediction
| Function | Description |
|---|---|
| **30-Day Demand Forecast** | AI-powered sales prediction via Retrofit-backed REST API (`/predict` endpoint) |
| **Category-Level Predictions** | Per-service-type breakdown with active customers, daily revenue, and 30-day projected revenue |
| **Revenue Pie Chart** | Category share visualization with animated pie chart |
| **Top Category Highlight** | Identifies and highlights the highest-revenue service category |
| **AI Training Data Export** | Generates JSONL training datasets from support ticket data for ML model training |
| **Animated Stats** | Smooth animated quantity and revenue counters on the prediction dashboard |

---

### 🗺 Route & Delivery Planning
| Function | Description |
|---|---|
| **Delivery Route List** | Sorted list of active, non-vacation customers grouped by area → address → name |
| **Google Maps Integration** | Open any customer's address directly in Google Maps for navigation |
| **One-Tap Call** | Direct dial customer phone number from the route list |
| **Stop Counter** | Shows total active delivery stops with refresh capability |
| **Route Optimization** | Dedicated route optimization activity for efficient delivery planning |

---

### 📱 QR Code & Customer Linking
| Function | Description |
|---|---|
| **Provider QR Code Generation** | Generate QR codes containing provider ID for customer onboarding (ZXing) |
| **QR Code Scanning** | Customers scan provider QR to initiate linking (zxing-android-embedded) |
| **Manual Provider Code Entry** | Link via manually typed provider code or short code |
| **Deep Link URI Parsing** | Supports `dd://provider/<id>` URI scheme for linking |
| **Join Request Workflow** | Customer sends PENDING request → Provider approves/rejects → Status becomes ACTIVE |
| **Provider Replacement** | Customers can replace existing linked provider with confirmation dialog |
| **Unlink Provider** | Customers can remove linked provider at any time |

---

### 👤 Customer Self-Service
| Function | Description |
|---|---|
| **Customer Home** | Welcome screen showing linked provider info, status, and quick actions |
| **Service Dashboard** | View delivery history and monthly summaries from the customer perspective |
| **Complaint & Support** | Raise support tickets with category, subject, and message — tracked in real time |
| **Quantity Change Requests** | Submit requests to change daily delivery quantity — provider approves/rejects |
| **Real-Time Link Status** | Live Firestore listener shows PENDING → ACTIVE status transitions instantly |
| **Notifications** | Receive real-time push notifications for deliveries, bills, and support updates |

---

### 🏢 Provider Operations
| Function | Description |
|---|---|
| **Join Requests Management** | View, approve, or reject customer join requests |
| **Complaints Management** | View and resolve customer-raised support tickets |
| **Quantity Requests Management** | Review and act on customer quantity-change requests (approve/reject) |
| **Bulk Operations** | Bulk customer updates, bulk delivery logging, and bulk operation request handling |
| **Customer Invite System** | Create invite tokens → customers claim via hashed token after login |

---

### 🔔 Notifications
| Function | Description |
|---|---|
| **Push Notifications** | Firebase Cloud Messaging (FCM) for real-time push delivery |
| **Multi-Channel Support** | Separate notification channels — General, Billing & Payments, Service Deliveries |
| **In-App Notification List** | All received notifications stored in Firestore and displayed in NotificationListActivity |
| **FCM Token Management** | Automatic token refresh and Firestore persistence per device |
| **Notification Routing** | Tapping a notification opens the dashboard and routes to relevant screen |

---

### 📤 Data Export
| Function | Description |
|---|---|
| **PDF Bill Export** | Export monthly bills as shareable PDF documents |
| **AI Training Data Export** | Export support ticket data as JSONL for machine learning pipelines |
| **Data Export Utilities** | Core export module with generator and utility classes |

---

### 👤 Profile Management
| Function | Description |
|---|---|
| **View/Edit Profile** | Update user name, business name, phone, and profile details |
| **Provider Profile** | Business-specific profile (business name, service types, provider code) |
| **Logout** | Sign out with Google Sign-In revocation and local data cleanup |

---

### ⚡ Offline & Sync
| Function | Description |
|---|---|
| **Offline Cache (Room DB)** | Customers cached locally in Room database for instant offline access |
| **Pending Entry Queue** | Service entries queued locally (via Gson/SharedPreferences) when offline |
| **Background Sync Worker** | WorkManager-based `PendingEntriesSyncWorker` syncs queued entries to Firestore on reconnect |
| **Transactional Sync** | Each pending entry synced via Firestore transaction (dedup check + lent amount update) |
| **Smart Retry Logic** | Differentiates permanent failures (auth/invalid) from transient failures (network) — retries only transient |
| **Concurrent-Safe Reconciliation** | Thread-safe merge of processed vs. newly-queued entries post-sync |
| **Network Monitor** | Real-time network state observer for auto-triggering sync |
| **Firestore Offline Persistence** | Firebase SDK-level offline persistence for seamless reads |
| **Shimmer Loading States** | Facebook Shimmer effect for elegant loading placeholders |

---

### 🔒 Security
| Function | Description |
|---|---|
| **Firestore Security Rules** | Fine-grained per-collection rules enforcing ownership, role checks, and field validation |
| **Provider-Scoped Data** | All customer/entry/bill/payment data scoped to owning provider via `providerId` checks |
| **Customer Data Isolation** | Customers can only access their own linked data |
| **Transaction Integrity** | Deterministic document IDs + Firestore transactions prevent duplicate entries |
| **Network Security Config** | Cleartext traffic disabled; custom network security configuration |
| **ProGuard / R8** | Code minification and resource shrinking enabled for release builds |

---

### 🛠 Utility Functions
| Function | Description |
|---|---|
| **CurrencyUtils** | Indian currency formatting (`₹`), compact notation (`₹1.2K`), and currency parsing |
| **DateUtils** | Date formatting, month-year labels, and date range helpers |
| **ValidationUtils** | Input validation for phone numbers, emails, names, and form fields |
| **MessagingUtils** | SMS sending utilities for customer notifications |
| **PreferenceManager** | SharedPreferences wrapper for user session, role, and app settings |
| **NetworkMonitor** | Real-time connectivity state observer |
| **Constants** | Centralized collection names and notification type constants |

---

## 🛠 Tech Stack

### Core
| Layer | Technology |
|---|---|
| **Language** | Java 17 + Kotlin |
| **UI Toolkit** | Android View system · Material Design 3 · ViewBinding |
| **Min / Target SDK** | 24 / 35 |
| **Build System** | Gradle 8.13.2 · AGP |

### Backend — Firebase
| Service | Usage |
|---|---|
| **Authentication** | Email/password + Google Sign-In |
| **Cloud Firestore** | Primary NoSQL database with fine-grained security rules |
| **Cloud Storage** | Profile images and file uploads |
| **Cloud Messaging (FCM)** | Push notifications (multi-channel) |
| **Crashlytics** | Real-time crash reporting |
| **Analytics** | User behaviour tracking |

### Architecture & Libraries
| Category | Library |
|---|---|
| **Dependency Injection** | Hilt (Dagger) 2.51 |
| **Local Database** | Room (offline customer cache) |
| **Background Tasks** | WorkManager (offline sync) |
| **Networking** | Retrofit 2 + OkHttp + Gson |
| **Pagination** | Paging 3 |
| **Navigation** | AndroidX Navigation Component |
| **Lifecycle** | ViewModel · LiveData |
| **Image Loading** | Glide 4.16 |
| **Charts** | MPAndroidChart (bar + pie) |
| **PDF Generation** | Android PdfDocument API |
| **QR Code** | ZXing + zxing-android-embedded |
| **Shimmer** | Facebook Shimmer (loading placeholders) |
| **Animations** | AndroidX DynamicAnimation |
| **Date / Time** | ThreeTenABP + desugared `java.time` |
| **JSON** | Gson (offline cache serialization) |

---

## 🏗 Architecture

The project follows a **modular, package-by-feature** layout with **MVVM** architecture:

```
app/src/main/java/com/dailyserviceapp/
├── ai/                # AI training data generation (JSONL export)
├── auth/              # Login, Signup, Forgot Password
├── billing/           # Bill generation, detail views, PDF generation, ledger calculator
├── bulk/              # Bulk delivery & customer update operations
├── core/
│   ├── base/          # BaseActivity, BaseFragment, BaseViewModel
│   ├── offline/       # OfflineCache (Room + SharedPreferences queue)
│   ├── sync/          # PendingEntriesSyncWorker, SyncWorkScheduler
│   └── utils/         # Constants, CurrencyUtils, DateUtils, ValidationUtils, etc.
├── customer/          # Customer-facing screens (Home, Service Dashboard, Complaints)
├── dashboard/         # Provider & customer dashboards with ViewModel
├── data/
│   ├── local/         # Room database, DAOs, entities
│   ├── models/        # Bill, Customer, Payment, ServiceEntry, Notification, etc.
│   └── repository/    # Domain-specific repos (Customer, ServiceEntry, Bill, Payment)
├── di/                # Hilt modules & dependency injection
├── export/            # Data export (core, generators, utilities)
├── maps/              # Route optimization
├── notifications/     # FCM service, notification channels, in-app notification list
├── payment/           # Payment recording & history
├── profile/           # User profile management
├── provider/          # Provider-specific screens (join requests, complaints, quantity requests)
├── qr/                # QR code generation & scanning
├── reports/           # Charts & analytics (bar, pie, animated counters)
├── route/             # Delivery route planning with Maps integration
├── sales/             # Sales prediction (Retrofit API, repository, UI)
├── service/           # Daily service entry logging
├── ui/                # Shared UI components (customer edit/detail)
└── utils/             # Additional utility classes
```

### Key Patterns
- **MVVM** — ViewModels expose `LiveData` streams consumed by Activities
- **Repository Pattern** — Domain-specific repositories (Customer, ServiceEntry, Bill, Payment) abstract Firestore / Room
- **Dependency Injection** — Hilt provides scoped, testable dependencies
- **Offline-First** — Room cache + pending entry queue + WorkManager sync for seamless offline UX
- **Transactional Writes** — Firestore transactions ensure data consistency (dedup + atomic balance updates)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Ladybug (2024.2+) or later
- **JDK 17**
- A Firebase project with `google-services.json` placed in `app/`

### Setup

```bash
# 1. Clone the repository
git clone https://github.com/Het-Mangukiya/DailyServiceApp.git
cd DailyServiceApp

# 2. Add your Firebase config
cp /path/to/google-services.json app/

# 3. Build the debug APK
./gradlew assembleDebug

# 4. Run unit tests
./gradlew test

# 5. Run lint checks
./gradlew lint
```

> **Note:** Firestore security rules are defined in [`firestore.rules`](firestore.rules) and indexes in [`firestore.indexes.json`](firestore.indexes.json). Deploy them with the Firebase CLI:
> ```bash
> firebase deploy --only firestore:rules,firestore:indexes
> ```

---

## ⚙️ CI / CD

The project uses **GitHub Actions** with two automated workflows:

### 1. Android CI — `android-ci.yml`
Runs on every **push** and **pull request** to `main`.

| Step | Command |
|---|---|
| Checkout | `actions/checkout@v4` |
| Setup JDK 17 | `actions/setup-java@v4` |
| Build Debug | `./gradlew assembleDebug` |
| Lint | `./gradlew lint` |
| Unit Tests | `./gradlew test` |

### 2. Firestore Rules Tests — `firestore-rules-tests.yml`
Runs on **pull requests** that modify `firestore.rules`, `firestore.indexes.json`, or security tests.

| Step | Command |
|---|---|
| Checkout | `actions/checkout@v4` |
| Setup Node 20 | `actions/setup-node@v4` |
| Install deps | `npm --prefix tests/security install` |
| Run rules tests | Fires up Firebase Emulator and executes Firestore security rule tests |

---

## 🤝 Contributing

1. **Fork** the repository
2. **Create** a feature branch — `git checkout -b feature/amazing-feature`
3. **Commit** your changes — `git commit -m "feat: add amazing feature"`
4. **Push** to your branch — `git push origin feature/amazing-feature`
5. **Open** a Pull Request

Please follow [Conventional Commits](https://www.conventionalcommits.org/) for commit messages.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/Het-Mangukiya">Het Mangukiya</a>
</p>
