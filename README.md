# ₿itcoin Fee Analyzer ⚡Real-Time⚡ Analytics

This project is more than a blockchain data streamer.\
It applies **real-time business logic** on top of raw Bitcoin mempool
and block data to reveal **market dynamics in transaction fees**.\
The goal is to showcase **event-driven architecture**, **real-time
analytics**, and **clean separation of concerns** between data
ingestion, business logic, and presentation.

------------------------------------------------------------------------


##  Live Demo on AWS 🌐

- URL: https://d3q8hndklwuv8q.cloudfront.net
- App Runner (`blockpulse-service`): runs the Docker container image for the backend service with managed deployment and auto‑scaling.
- Amazon S3 + CloudFront: delivers a global, low‑latency edge endpoint; S3 stores static assets while CloudFront fronts and routes requests to the backend service.

Open the link above to see the service in action.

##  What This Application Does 🎯

Bitcoin block space is scarce. Every \~10 minutes miners choose which
transactions to confirm. Since space is limited, users bid with fees.\
This app **compares and classifies fees in real time** to make sense of
that competition.

Instead of showing raw transaction lists, it enriches them with **live
insights**:

------------------------------------------------------------------------

###  1. Outliers Detection

-   Detect transactions paying **far above** or **far below** the
    prevailing mempool fee rate or statistical outlier thresholds.
-   **High outliers** may indicate urgent transfers (or poorly tuned
    wallets).
-   **Low outliers** highlight transactions unlikely to confirm soon.

------------------------------------------------------------------------

### ️ 2. Transaction Labeling (Cheap / Normal / Expensive)

-   Each transaction is classified in real time based on fee per vByte
    relative to network conditions.
    -   **Cheap:** risk of long confirmation delay.
    -   **Normal:** aligns with current mempool market rate.
    -   **Expensive:** significantly above average, likely to confirm
        quickly.

------------------------------------------------------------------------

###  3. Surge & Fee War Patterns

-   **Fee Surge:** sudden, short-term spikes in average fee rate. Often
    caused by network congestion or market events.
-   **Fee War(Future idea):** extended periods of competitive bidding where fees
    escalate block by block. 
-   Pattern detection logic identifies these scenarios and pushes **live
    alerts**.
------------------------------------------------------------------------

###  4. Fee Trends

-   Continuous calculation of moving averages and rolling medians of
    transaction fees.
-   Provides a **trend line** for decision-makers to see
    whether the network is stabilizing or heating up.

------------------------------------------------------------------------

##  Why This Matters 💡

This project turns **low-level blockchain data** into **actionable
insights**.\
It simulates the kind of analytics pipeline real companies build for: -
Fraud detection - Market monitoring - Payment optimization

No database is required --- all computations are **in-memory and real
time**.\
The value lies in **business logic**: transforming raw data into
something understandable, visual, and useful — for people who need to
act on fee dynamics:

- Traders and market makers: time transfers, avoid stuck funds, react to
  surges for arbitrage or settlement.
- Merchants and payment processors: pick fees for desired confirmation
  targets; detect congestion and switch policies (e.g., batching or
  Lightning fallback).
- Wallets and product teams: power smart fee suggestions, ETAs, and UX
  warnings about abnormal network conditions.


##  Technologies Used 🛠️

- Language and Build
    - Java 21
    - Maven
- Frameworks and Libraries
    - Spring Boot (application bootstrap and configuration)
    - Spring Web/WebSocket: persistent WS connection for real-time mempool/block events with graceful lifecycle (connect/reconnect/shutdown).
    - Spring WebFlux: reactor, non-blocking, backpressure-aware stream processing and fan-out to multiple consumers.
      Why Reactor for SSE:
        - Fanout efficiency: serves many concurrent clients with few threads.
        - Backpressure: protects both server and clients from overload.
        - Easy pacing: operators like `sample(2s)`/`bufferTimeout` send readable updates (e.g., latest every 2 seconds).
        - Replay capabilities: latest 1000 events are retained and new clients are served with this replay.
    - Server-Sent Events (SSE): lightweight one-way push over HTTP for continuous telemetry to dashboards.
- Runtime and Packaging
    - Docker (Dockerfile provided)
    - Docker Compose (service orchestration file present)
- Testing
    - JUnit (standard Spring/Java testing stack, inferred from Maven + src/test)
    - Integration Tests (IT) present for end-to-end validation of service components and application flow.


## ️ Architecture 🏗️

High level: event-driven, streaming analytics pipeline with clear separation of concerns.

- Ingestion Layer
    - WebSocket client: connects to mempool.space stream and handles subscription, message reception, and delegation for processing.
    - REST client: periodic mempool state updates that are fed into the analysis context influencing the fee analysis outcome.
- Processing and Analytics
    - Sliding window aggregation and snapshotting
    - Analyzers:
        - OutlierAnalyzer: detects low/high fee outliers
        - FeeClassificationAnalyzer: labels transactions Cheap/Normal/Expensive
        - SurgeAnalyzer: detects short-term fee spikes and patterns
- Configuration
    - Separate subpackages for ws, rest, analysis, service (for wiring, thresholds, scheduling).
- Exposure
    - controller package (HTTP endpoints for snapshots/insights)
    - ws sender/notification components to publish live insights
- Complexity & Performance
    - Sliding window updates are designed to be amortized **O(log N)** for ordered-structure maintenance, 
      with **O(N)** queries for common statistics (iqr, percentiles,...), enabling high-throughput real-time streams.

Data Flow:
1) WebSocket: mempool transaction events -> message handler -> analyzers -> classification/alerts.
2) REST: scheduled fetch of mempool stats -> update analysis context -> influences thresholds and labeling.
3) Sliding window: maintains recent transactions in-memory for rolling metrics and patterns.
4) Output: controllers and/or SSE provide live snapshots and insights to clients.

Key Design Choices:
- In-memory windows for real-time responsiveness and simplicity.
- Modular analyzers implementing specific concerns (SRP).
- Chain of Responsibility for analyzers: composable, orderable processing steps that keep components loosely coupled;
  enables easy insertion/removal/reordering, supports open/closed principle, and allows dynamic reconfiguration without touching existing logic.
- Context object (AnalysisContext) to share live network conditions across analyzers.
- Event-driven flow decoupling ingestion from analytics and presentation.

##  REST API Endpoints 🌐

The application exposes a REST API endpoint to provide live streaming of analyzed transaction events.

- **Base Path:** `/api/v1/transactions`
- **Endpoint:** `GET /stream`
- **Produces:** `text/event-stream`
- **Description:** Streams analyzed transaction events in real time as **Server-Sent Events (SSE)**.
- This endpoint supports live consumption by dashboards or clients, allowing them to receive continuous updates of transaction analysis.
- The stream supports configurable sampling intervals, with a default sampling period of 2000 milliseconds (2 seconds), to control the frequency of updates and optimize performance.


##  Analytics Logic 📊

- Normalization: all comparisons use fee rate per vByte to ensure fair and consistent comparison across transactions.
- Descriptive Statistics:
    1.	Computes summary metrics from the current sliding window of transactions.
    2.	It calculates the minimum, maximum, mean, median, Q1, Q3, and IQR of observed fee rates.
    3.	It also determines the Tukey fences (low and high thresholds) used for outlier detection.
    4.	These values are bundled into a stats summary object for downstream analyzers (outlier, spam, surge, classification).
    5.	This keeps all detection logic grounded in the same statistical snapshot of the mempool window.
- Transaction Fee Price Classification: the system first checks if a transaction is marked as an outlier; if so, it is labeled ABNORMAL_PRICE.
  If not, the fee rate is compared using one of two strategies:
    1.	Congested mempool: classification uses live mempool stats (fast vs medium fee levels).
          •	Above fast → EXPENSIVE
          •	Between medium and fast → NORMAL
          •	Below medium → CHEAP
    2.	Normal mempool: classification uses the IQR range from sliding window statistics.
          •	Below lower bound → CHEAP, within range → NORMAL, above upper bound → EXPENSIVE.
- Spam Detection:
    1. In Spam Detection, each new transaction’s fee rate is compared against the lower Tukey fence derived from the sliding window statistics.
    2. If the fee is below this threshold, the system flags the transaction as potential SCAM/SPAM, marking it with a PatternType.SCAM.
    3. This highlights transactions bidding unrealistically low fees, which are unlikely to confirm and may clutter the mempool.
       If the fee is above the lower fence, no spam pattern is assigned and the transaction flows through unaltered.
- Surge Detection: 
    In Surge Detection, a transaction is flagged when three conditions align:
    1.	Its fee per vByte is above the upper Tukey fence (statistical outlier threshold).
    2.	It is also well above the recommended “fast fee” level reported by mempool stats.
    3.	The mempool itself is in a congested state, exceeding a configured vByte size threshold.
        Only when all three hold, the system classifies the transaction pattern as a potential SURGE.
- Outlier Detection:
    1.	Each new transaction’s fee per vByte is compared against the Tukey fences derived from the sliding window statistics (Q1, Q3, IQR).
    2.	If the fee falls outside these bounds, the transaction is flagged as an outlier.
    3.	Outliers can be either unusually high or unusually low relative to recent mempool activity.
  

##  Build and Run 🚀

- Local
```bash
   mvn clean install
   java -jar target/blockpulse-service.jar
```
- Docker
```bash
   docker build -t blockpulse-service .
   docker compose up
```

##  Quick Start ⚡

- Run with Docker (Compose):
  ```bash
  docker compose up --build
  ```
- Run with Docker (direct):
  ```bash
  docker build -t blockpulse-service:latest .
  docker run -p 8080:8080 blockpulse-service:latest
  ```
- Stream API (SSE):
  ```bash
  curl -N -i -H "Accept: text/event-stream" http://localhost:8080/api/v1/transactions/stream
  ```
  - `-N` disables buffering so events stream continuously.
  - Response uses `text/event-stream`.
