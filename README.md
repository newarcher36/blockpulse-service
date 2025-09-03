contents:

# ⚡️ Bitcoin Fee Comparator -- Real-Time Analytics

This project is more than a blockchain data streamer.\
It applies **real-time business logic** on top of raw Bitcoin mempool
and block data to reveal **market dynamics in transaction fees**.\
The goal is to showcase **event-driven architecture**, **real-time
analytics**, and **clean separation of concerns** between data
ingestion, business logic, and presentation.

------------------------------------------------------------------------

##  What This Application Does

Bitcoin block space is scarce. Every \~10 minutes miners choose which
transactions to confirm. Since space is limited, users bid with fees.\
This app **compares and classifies fees in real time** to make sense of
that competition.

Instead of showing raw transaction lists, it enriches them with **live
insights**:

------------------------------------------------------------------------

###  1. Outliers Detection

-   Detect transactions paying **far above** or **far below** the
    prevailing mempool fee rate.
-   **High outliers** may indicate urgent transfers (or poorly tuned
    wallets).\
-   **Low outliers** highlight transactions unlikely to confirm soon.

------------------------------------------------------------------------

### ️ 2. Transaction Labeling (Cheap / Normal / Expensive)

-   Each transaction is classified in real time based on fee per vByte
    relative to network conditions.
    -   **Cheap:** risk of long confirmation delay.\
    -   **Normal:** aligns with current mempool market rate.\
    -   **Expensive:** significantly above average, likely to confirm
        quickly.

------------------------------------------------------------------------

###  3. Surge & Fee War Patterns

-   **Fee Surge:** sudden, short-term spikes in average fee rate. Often
    caused by network congestion or market events.\
-   **Fee War:** extended periods of competitive bidding where fees
    escalate block by block.\
-   Pattern detection logic identifies these scenarios and pushes **live
    alerts**.
------------------------------------------------------------------------

###  4. Fee Trends

-   Continuous calculation of moving averages and rolling medians of
    transaction fees.\
-   Provides a **trend line** for recruiters or decision-makers to see
    whether the network is stabilizing or heating up.

------------------------------------------------------------------------

##  Why This Matters

This project turns **low-level blockchain data** into **actionable
insights**.\
It simulates the kind of analytics pipeline real companies build for: -
Fraud detection - Market monitoring - Payment optimization

No database is required --- all computations are **in-memory and real
time**.\
The value lies in **business logic**: transforming raw data into
something understandable, visual, and useful.

##  Technologies Used

- Language and Build
    - Java 21
    - Maven
- Frameworks and Libraries
    - Spring Boot (application bootstrap and configuration)
    - Spring Web/WebSocket: persistent WS connection for real-time mempool/block events with graceful lifecycle (connect/reconnect/shutdown).
    - Spring WebFlux with Reactor (Flux): non-blocking, backpressure-aware stream processing and fan-out to multiple consumers.
      Uses operators (map/filter/window) to express analytics stages declaratively.
    - Server-Sent Events (SSE): lightweight one-way push over HTTP for continuous telemetry to dashboards.
      Supports auto-reconnect with Last-Event-ID for resilient live updates.
- Runtime and Packaging
    - Docker (Dockerfile provided)
    - Docker Compose (service orchestration file present)
- Testing
    - JUnit (standard Spring/Java testing stack, inferred from Maven + src/test)
- Other
    - In-memory analytics (no external DB)

Notes:
- The presence of BlockPulseServiceApplication.java and config packages indicates Spring Boot.
- WebSocket and REST client packages suggest Spring’s Web/WebSocket stack for ingestion.

## Why Reactor for SSE

- Fanout efficiency: serves many concurrent clients with few threads.
- Backpressure: protects both server and clients from overload.
- Easy pacing: operators like `sample(2s)`/`bufferTimeout` send readable updates (e.g., latest every 2 seconds).
- Maintainable: concise, composable streaming code that’s easy to evolve.

## ️ Architecture

High level: event-driven, streaming analytics pipeline with clear separation of concerns.

- Ingestion Layer
    - WebSocket client: connects to mempool.space stream
        - com.blockchain.blockpulseservice.client.ws.*
        - Handles subscription, message reception, and delegation to processing.
    - REST client: periodic snapshots/statistics
        - com.blockchain.blockpulseservice.client.rest.MempoolStatsUpdater
- Domain and DTOs
    - com.blockchain.blockpulseservice.model.*
    - Transaction, MempoolStats, FeeClassification, PatternType, window snapshots, DTOs.
- Processing and Analytics
    - com.blockchain.blockpulseservice.service.*
    - Sliding window aggregation and snapshotting
    - Analyzers:
        - OutlierAnalyzer: detects low/high fee outliers
        - FeeClassificationAnalyzer: labels transactions Cheap/Normal/Expensive
        - SurgeAnalyzer: detects short-term fee spikes and patterns
    - AnalysisStream and TransactionAnalyzerService coordinate the flow.
- Configuration
    - com.blockchain.blockpulseservice.config.*
    - Separate subpackages for ws, rest, analysis, notification (for wiring, thresholds, scheduling).
- Exposure and Notification
    - controller package (HTTP endpoints for snapshots/insights)
    - ws sender/notification components to publish live insights

Data Flow:
1) WebSocket: mempool transaction events -> message handler -> analyzers -> classification/alerts.
2) REST: scheduled fetch of mempool stats -> update AnalysisContext -> influences thresholds and labeling.
3) Sliding window: maintains recent transactions in-memory for rolling metrics and patterns.
4) Output: controllers and/or websocket sender provide live snapshots and alerts to clients.

Key Design Choices:
- In-memory windows for real-time responsiveness and simplicity.
- Modular analyzers implementing specific concerns (SRP).
- Context object (AnalysisContext) to share live network conditions across analyzers.
- Event-driven flow decoupling ingestion from analytics and presentation.
- Chain of Responsibility for analyzers: composable, orderable processing steps that keep components loosely coupled; 
  enables easy insertion/removal/reordering, supports open/closed principle, and allows dynamic reconfiguration without touching existing logic.


##  Notable Packages

- com.blockchain.blockpulseservice.client.ws: WebSocket base client, session management, sender/handler
- com.blockchain.blockpulseservice.client.rest: Scheduled REST updater for mempool statistics
- com.blockchain.blockpulseservice.service.analysis: Outlier, Fee Classification, Surge analyzers
- com.blockchain.blockpulseservice.service.sliding_window: Window maintenance and snapshots
- com.blockchain.blockpulseservice.model: Core domain objects and DTOs
- com.blockchain.blockpulseservice.controller: HTTP endpoints (read models to external clients)
- com.blockchain.blockpulseservice.config: Configuration per concern (ws, rest, analysis, notification)

##  Analytics Logic

- Normalization
    - All comparisons use fee rate per vByte to ensure apples-to-apples across transactions.
- Order Statistics
    - Median (50th percentile): the middle value of the sorted sliding window. For even counts, the median is the average of the two middle values.
    - Percentiles (e.g., P99, P95, P90, P10, P5): determined by selecting the element at index ceil(p · N) − 1 in the sorted window, where p ∈ (0,1] and N is the number of items in the window.
        - High outliers: flagged using upper percentiles (e.g., P95/P99).
        - Low outliers: flagged using lower percentiles (e.g., P5/P1).
- Averages and Trend Measures
    - Mean: arithmetic average of fee rates for sensitivity to broad shifts.
    - Robust central tendency: median is used to dampen the effect of spikes.
    - Rolling aggregates: maintained over a time- or count-bounded sliding window for near-real-time updates.
- Classification
    - Transactions are labeled (e.g., Cheap/Normal/Expensive) based on how their fee rate compares to dynamic thresholds derived from current percentiles and recent window statistics.
- Complexity
    - The sliding window maintains an ordered view of fee rates, enabling updates in logarithmic time per event (O(log N)) and constant or logarithmic-time queries for median/percentiles/means, supporting real-time throughput.


##  Build and Run

- Local
    - mvn clean package
    - java -jar target/<artifact>.jar
- Docker
    - docker build -t blockpulse-service .
    - docker compose up
      // ... existing code ...
