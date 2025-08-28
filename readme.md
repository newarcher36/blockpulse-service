# ⚡️ Bitcoin Fee Comparator -- Real-Time Analytics

This project is more than a blockchain data streamer.\
It applies **real-time business logic** on top of raw Bitcoin mempool
and block data to reveal **market dynamics in transaction fees**.\
The goal is to showcase **event-driven architecture**, **real-time
analytics**, and **clean separation of concerns** between data
ingestion, business logic, and presentation.

------------------------------------------------------------------------

## 🎯 What This Application Does

Bitcoin block space is scarce. Every \~10 minutes miners choose which
transactions to confirm. Since space is limited, users bid with fees.\
This app **compares and classifies fees in real time** to make sense of
that competition.

Instead of showing raw transaction lists, it enriches them with **live
insights**:

------------------------------------------------------------------------

### 🔍 1. Outliers Detection

-   Detect transactions paying **far above** or **far below** the
    prevailing mempool fee rate.
-   **High outliers** may indicate urgent transfers (or poorly tuned
    wallets).\
-   **Low outliers** highlight transactions unlikely to confirm soon.

------------------------------------------------------------------------

### 🏷️ 2. Transaction Labeling (Cheap / Normal / Expensive)

-   Each transaction is classified in real time based on fee per vByte
    relative to network conditions.
    -   **Cheap:** risk of long confirmation delay.\
    -   **Normal:** aligns with current mempool market rate.\
    -   **Expensive:** significantly above average, likely to confirm
        quickly.

------------------------------------------------------------------------

### 📈 3. Surge & Fee War Patterns

-   **Fee Surge:** sudden, short-term spikes in average fee rate. Often
    caused by network congestion or market events.\
-   **Fee War:** extended periods of competitive bidding where fees
    escalate block by block.\
-   Pattern detection logic identifies these scenarios and pushes **live
    alerts**.
------------------------------------------------------------------------

### 📊 4. Fee Trends

-   Continuous calculation of moving averages and rolling medians of
    transaction fees.\
-   Provides a **trend line** for recruiters or decision-makers to see
    whether the network is stabilizing or heating up.

------------------------------------------------------------------------

## 🚀 Why This Matters

This project turns **low-level blockchain data** into **actionable
insights**.\
It simulates the kind of analytics pipeline real companies build for: -
Fraud detection - Market monitoring - Payment optimization

No database is required --- all computations are **in-memory and real
time**.\
The value lies in **business logic**: transforming raw data into
something understandable, visual, and useful.
