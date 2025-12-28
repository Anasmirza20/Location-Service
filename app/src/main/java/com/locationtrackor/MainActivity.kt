package com.locationtrackor

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.locationtrackor.service.LocationTrackingService
import com.locationtrackor.ui.theme.LocationTrackorTheme
import com.locationtrackor.ui.viewmodel.LocationViewModel
import com.locationtrackor.util.ConnectivityObserver
import com.locationtrackor.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocationTrackorTheme {
                val viewModel: LocationViewModel = viewModel()

                LocationScreen(viewModel)
            }
        }
    }
}

@Composable
fun LocationScreen(viewModel: LocationViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val showPermissionRequest by viewModel.showPermissionRequest.collectAsState()
    val showRationale by viewModel.showRationale.collectAsState()
    val goToSettings by viewModel.goToSettings.collectAsState()
    val startServiceTrigger by viewModel.startServiceTrigger.collectAsState()
    val stopServiceTrigger by viewModel.stopServiceTrigger.collectAsState()
    val context = LocalContext.current
    val activity = context as ComponentActivity

    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            showBatteryOptimizationDialog = true
        }
    }

    if (showBatteryOptimizationDialog) {
        BatteryOptimizationDialog(
            onDismiss = { showBatteryOptimizationDialog = false },
            onConfirm = {
                showBatteryOptimizationDialog = false
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = "package:${context.packageName}".toUri()
                }
                context.startActivity(intent)
            }
        )
    }

    val foregroundPermissions = remember {
        mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
        }.toTypedArray()
    }

    val backgroundPermission = remember {
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                if (!activity.shouldShowRequestPermissionRationale(backgroundPermission!!)) {
                    viewModel.onPermanentDenial()
                }
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val allForegroundGranted = permissions.entries.all { it.value }
            if (allForegroundGranted) {
                backgroundPermissionLauncher.launch(backgroundPermission)
            } else {
                val shouldShowRationale = foregroundPermissions.any {
                    activity.shouldShowRequestPermissionRationale(it)
                }
                if (shouldShowRationale) {
                    viewModel.onRationaleShown(LocationViewModel.PermissionType.FOREGROUND)
                } else {
                    viewModel.onPermanentDenial()
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(foregroundPermissions)
    }

    LaunchedEffect(showPermissionRequest) {
        if (showPermissionRequest) {
            permissionLauncher.launch(foregroundPermissions)
            viewModel.onPermissionRequestHandled()
        }
    }

    if (goToSettings) {
        AlertDialog(
            onDismissRequest = { viewModel.onSettingsOpened() },
            title = { Text("Permissions Required") },
            text = { Text("Some permissions are permanently denied. Please enable them in app settings to use tracking.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.onSettingsOpened()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onSettingsOpened() }) {
                    Text("Cancel")
                }
            }
        )
    }

    showRationale?.let { type ->
        AlertDialog(
            onDismissRequest = { viewModel.onRationaleDismissed() },
            title = { Text("Permission Rationale") },
            text = {
                Text(
                    if (type == LocationViewModel.PermissionType.FOREGROUND)
                        "Foreground location and notification permissions are needed to track your location reliably."
                    else
                        "Background location permission is required to keep tracking even when the app is closed."
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onRationaleDismissed()
                    if (type == LocationViewModel.PermissionType.FOREGROUND) {
                        permissionLauncher.launch(foregroundPermissions)
                    } else {
                        backgroundPermissionLauncher.launch(backgroundPermission)
                    }
                }) {
                    Text("Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onRationaleDismissed() }) {
                    Text("Dismiss")
                }
            }
        )
    }

    LaunchedEffect(startServiceTrigger) {
        if (startServiceTrigger) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.startForegroundService(intent)
            viewModel.onServiceStarted()
        }
    }

    LaunchedEffect(stopServiceTrigger) {
        if (stopServiceTrigger) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
            viewModel.onServiceStopped()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ConnectivityIndicator(uiState.connectivityStatus)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pending Offline Logs: ${uiState.pendingLogsCount}",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!uiState.isTracking) {
                Button(onClick = { 
                    viewModel.onStartTrackingClicked(PermissionUtils.hasRequiredPermissions(context)) 
                }) {
                    Text("Start Tracking")
                }
            } else {
                Button(onClick = { viewModel.onStopTrackingClicked() }) {
                    Text("Stop Tracking")
                }
            }
        }
    }
}

@Composable
fun ConnectivityIndicator(status: ConnectivityObserver.Status) {
    val color = when (status) {
        ConnectivityObserver.Status.Available -> Color.Green
        else -> Color.Red
    }

    val text = when (status) {
        ConnectivityObserver.Status.Available -> "Online"
        else -> "Offline"
    }

    Text(
        text = "Status: $text",
        color = color,
        style = MaterialTheme.typography.titleLarge
    )
}

@Composable
private fun BatteryOptimizationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Battery Optimization") },
        text = { Text("To ensure reliable background tracking, please disable battery optimization for this app.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Disable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}