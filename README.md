# LocationTrackor

LocationTrackor is a reliable, offline-first Android application designed for continuous background location tracking. Built with modern Android development practices, it ensures that location data is captured accurately and synchronized to a server even under challenging network conditions or device reboots.

## Features

- **Robust Background Tracking:** Utilizes a Foreground Service with a persistent notification to ensure the system prioritizes the tracking process, even when the app is not in focus.
- **Offline-First Architecture:** All captured locations are immediately persisted in a local Room database. This guarantees no data loss during network outages or app crashes.
- **Smart Data Synchronization:** Leverages Android's WorkManager to handle data uploads. It automatically retries failed syncs with exponential backoff and only attempts uploads when a stable internet connection is detected.
- **Resilience to Reboots:** A dedicated BroadcastReceiver monitors device boots to automatically resume tracking if it was active before the shutdown.
- **Battery Optimization Handling:** Includes built-in logic to guide users through disabling system battery optimizations, which is critical for consistent background performance on modern Android versions.
- **Real-time Connectivity Monitoring:** Displays the current network status within the app, giving users transparency over their sync state.

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Declarative UI)
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
- **Dependency Injection:** Dagger Hilt
- **Local Database:** Room
- **Networking:** Retrofit & OkHttp
- **Background Tasks:** WorkManager & Foreground Services
- **Location Services:** Google Play Services (Fused Location Provider)

## Project Architecture

The project follows **Clean Architecture** and **MVVM** principles to ensure a clear separation of concerns, testability, and maintainability.

### Architecture Layers

1.  **UI Layer (`ui` package)**: 
    *   Built with **Jetpack Compose** for a modern, reactive UI.
    *   **MainActivity**: Manages the application lifecycle and the complex permission acquisition flow.
    *   **LocationViewModel**: Acts as a state holder, exposing `StateFlow` to the UI and reacting to repository updates.

2.  **Domain/Data Layer (`data` package)**:
    *   **LocationRepository**: The central hub that abstracts data sources. It manages the flow between the local database and the remote API.
    *   **Local Data (`data.local`)**: Uses Room persistence library to store location logs.
    *   **Remote Data (`data.remote`)**: Uses Retrofit for API communication.

3.  **Service Layer (`service` package)**:
    *   **LocationTrackingService**: A Foreground Service that runs independently of the UI. It uses the `FusedLocationProviderClient` for high-accuracy location tracking.

4.  **Worker Layer (`worker` package)**:
    *   **SyncWorker**: Handles background synchronization using WorkManager, ensuring data is uploaded even if the app is closed.

## Offline Storage

Offline-first capability is a core pillar of LocationTrackor.

-   **Database**: Uses **Room** with a table named `location_logs`.
-   **Persistence**: Every captured location is immediately saved as a `LocationEntity` with an `isSynced` flag set to `false`.
-   **Data Integrity**: By persisting data before attempting to sync, the app ensures that no location point is lost due to network failure, app crashes, or device reboots.

## Sync Mechanism

The synchronization process is designed to be robust and battery-efficient:

1.  **Trigger**: A sync is attempted every time a new location is saved or when the service starts.
2.  **WorkManager**: Leverages Android's `WorkManager` to handle `SyncWorker`.
3.  **Constraints**: Syncing only occurs when a valid **Internet Connection** is detected (`NetworkType.CONNECTED`).
4.  **Batching**: The worker retrieves all unsynced records from Room and sends them as a single batch to the server.
5.  **Reliability**: Uses **Exponential Backoff** policy for retries in case of API failures.
6.  **Atomic Updates**: Records are marked as `isSynced = true` only after a successful 2xx response from the server.

## API Contract

The app communicates with a backend via a POST request.

-   **Endpoint**: `/posts` (Configurable in `LocationApi.kt`)
-   **Method**: `POST`
-   **Content-Type**: `application/json`
-   **Payload**: A JSON array of location objects.

**Example Payload:**
```json
[
  {
    "employeeId": "EMP001",
    "latitude": 37.42199,
    "longitude": -122.08405,
    "accuracy": 15.0,
    "timestamp": 1672531200000,
    "speed": 0.5
  }
]
```

## Assumptions and Limitations

-   **Employee Identification**: Currently, `employeeId` is hardcoded as `"EMP001"`. In a production environment, this would be retrieved from a session or user profile.
-   **Location Provider**: Relies on **Google Play Services** (Fused Location Provider). Devices without Play Services are not supported.
-   **Permissions**: Continuous background tracking requires `ACCESS_BACKGROUND_LOCATION`, which must be manually granted by the user in system settings.
-   **Battery Optimization**: For reliable long-term tracking, users must manually whitelist the app from battery optimizations (a prompt is provided).
-   **Sync Frequency**: Syncing is triggered per-location capture but managed by WorkManager's scheduling logic, which might vary based on OS battery-saving states.

## Setup & Implementation Details

### Permissions
Location tracking requires several permissions, especially on newer Android versions:
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION` (Requested separately to comply with Google Play policies)
- `POST_NOTIFICATIONS` (For the Foreground Service notification)
- `FOREGROUND_SERVICE_LOCATION` (Required for Android 14+)

### Battery Optimization
To prevent the system from killing the background service, the app requests the user to whitelist it from battery optimizations via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.