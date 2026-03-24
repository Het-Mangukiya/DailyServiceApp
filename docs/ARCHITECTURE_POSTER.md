# 🧩 DailyServiceApp — Architecture & Flow Poster

This artifact provides a high-level visual representation of the project's architecture, user flows, and data relationships. 

---

## 🏛️ 1. High-Level Architecture
The app currently follows a monolithic, direct-access architecture where the UI layer talks directly to the data layer.

```mermaid
flowchart TD
    classDef ui fill:#4CAF50,stroke:#fff,stroke-width:2px,color:#fff
    classDef data fill:#2196F3,stroke:#fff,stroke-width:2px,color:#fff
    classDef offline fill:#FF9800,stroke:#fff,stroke-width:2px,color:#fff
    classDef cloud fill:#9C27B0,stroke:#fff,stroke-width:2px,color:#fff

    subgraph UILayer [UI Layer]
        Login[Auth Flow]:::ui
        Dashboard[Dashboards - Provider / Customer]:::ui
        ServiceEntry[Service Entries - Bills & PDF]:::ui
    end

    subgraph DataAccess [Data Layer]
        Repo(FirestoreRepository - 1300+ LoC God Class):::data
    end

    subgraph OfflineSync [Offline Handling]
        Cache[(OfflineCache - SharedPreferences)]:::offline
        Worker[[PendingEntriesSyncWorker]]:::offline
    end

    subgraph CloudBackend [Firebase Backend]
        Auth(Firebase Auth):::cloud
        DB[(Cloud Firestore)]:::cloud
        Storage[(Cloud Storage)]:::cloud
    end

    Login --> Auth
    Dashboard --> Repo
    ServiceEntry --> Repo

    Repo <--> DB
    
    ServiceEntry -.-> |If Offline| Cache
    Cache -.-> |Queued| Worker
    Worker -.-> |Syncs when Online| DB
```

---

## 🔄 2. Core User Journey / Flow

The app serves two primary personas: **Providers** (business owners) and **Customers** (subscribers). Here's how they interact.

```mermaid
sequenceDiagram
    autonumber
    actor Provider
    participant App
    participant Offline as Offline Cache
    participant DB as Firestore
    actor Customer

    Provider->>+App: Open App & Login
    App->>DB: Fetch Dashboard Data
    DB-->>App: Today's Deliveries & Earnings
    App-->>Provider: Show Dashboard
    
    Provider->>App: Submits Service Entry (e.g., Milk Delivery)
    
    alt Internet Available
        App->>DB: Write Service Entry & Update Lent Amount
        DB-->>App: Success
    else No Internet
        App->>Offline: Cache Entry locally
        Offline-->>App: Queued
        App->>Offline: Background Worker syncs when online
    end
    
    Provider->>App: Generate Monthly Bill
    App->>DB: Query monthly entries
    DB-->>App: Returns aggregated data
    App->>App: Generate PDF
    Provider->>Customer: Shares PDF (WhatsApp/SMS)
    
    Customer->>App: Scans Provider QR Code
    App->>DB: Send Join Request
    DB-->>Provider: Notify of New Request
    Provider->>App: Approve Request
```

---

## 🗄️ 3. Entity-Relationship (ER) Schema

The NoSQL database structure, mapped to a relational visualization to understand the connections between collections.

```mermaid
erDiagram
    PROVIDER ||--o{ CUSTOMER : manages
    PROVIDER ||--o{ SERVICE_ENTRY : logs
    PROVIDER ||--o{ BILL : generates
    PROVIDER ||--o{ PAYMENT : receives

    CUSTOMER ||--o{ SERVICE_ENTRY : receives
    CUSTOMER ||--o{ BILL : charged
    CUSTOMER ||--o{ PAYMENT : makes
    CUSTOMER ||--o{ SUPPORT_TICKET : creates

    PROVIDER {
        string id PK
        string businessName
        string phone
        string email
    }

    CUSTOMER {
        string id PK
        string providerId FK
        string name
        string serviceType
        float ratePerUnit
        float lentAmount "Total pending balance"
        boolean onVacation
    }

    SERVICE_ENTRY {
        string id PK
        string providerId FK
        string customerId FK
        date date
        float quantity
        float rate
        boolean delivered
    }

    BILL {
        string id PK
        string providerId FK
        string customerId FK
        string monthKey
        float totalAmount
        boolean paid
    }
```

---

## 🚦 4. App Flowchart — Navigation Tree

```mermaid
graph LR
    A[Splash Screen] --> B{Is Logged In?}
    B -- No --> C[Login / Signup]
    B -- Yes --> D{User Role}
    
    D -- Provider --> E[Provider Dashboard]
    D -- Customer --> F[Customer Dashboard]

    E --> G(Add Service Entry)
    E --> H(Customer Mgmt)
    E --> I(Generate Bills & PDFs)
    E --> J(Reports & Analytics)
    E --> K(View Join Requests)

    F --> L(View Daily Deliveries)
    F --> M(View Monthly Bill)
    F --> N(Raise Complaint/Ticket)
```
