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

The project is structured into several layers to ensure separation of concerns and maintainability:

### 1. UI Layer (`ui` package)
Built entirely with **Jetpack Compose**.
- **MainActivity:** Serves as the entry point and handles the complex permission flow required for location services.
- **LocationViewModel:** Manages the UI state, reacts to connectivity changes, and interacts with the repository. It uses `StateFlow` to provide a reactive stream of data to the UI.

### 2. Data Layer (`data` package)
- **Repository (`LocationRepository`):** The single source of truth for the app. It coordinates data flow between the local Room database and the remote API.
- **Local (`data.local`):** Contains the Room database definition, entities, and DAOs.
- **Remote (`data.remote`):** Contains Retrofit interfaces and data transfer objects (DTOs) for API communication.

### 3. Service Layer (`service` package)
- **LocationTrackingService:** A Foreground Service that handles the actual location collection using the `FusedLocationProviderClient`. It runs independently of the UI.

### 4. Worker Layer (`worker` package)
- **SyncWorker:** A Hilt-enabled CoroutineWorker that handles the batch uploading of unsynced location logs. It's designed to be efficient, using network constraints to save battery and data.

## API Contract

The app synchronizes data by sending a JSON array of location objects to the configured endpoint.

**Payload Example:**
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

## Setup & Implementation Details

### Permissions
Location tracking requires several permissions, especially on newer Android versions:
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION` (Requested separately to comply with Google Play policies)
- `POST_NOTIFICATIONS` (For the Foreground Service notification)
- `FOREGROUND_SERVICE_LOCATION` (Required for Android 14+)

### Battery Optimization
To prevent the system from killing the background service, the app requests the user to whitelist it from battery optimizations via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.