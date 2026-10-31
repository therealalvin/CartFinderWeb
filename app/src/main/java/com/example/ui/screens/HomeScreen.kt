package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.components.ArrivalCard
import com.example.ui.components.CompassBearingHud
import com.example.ui.components.GolfCartMapView
import com.example.ui.components.ParkCartDialog
import com.example.ui.viewmodel.CartFinderViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: CartFinderViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showParkDialog by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    // Location Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(fineGranted || coarseGranted)
    }

    LaunchedEffect(Unit) {
        val fineCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineCheck || coarseCheck) {
            viewModel.onPermissionResult(true)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Show snackbar messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearMessage()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD8F3DC)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsGolf,
                                contentDescription = null,
                                tint = Color(0xFF1B4332),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CartFinder",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Demo Mode toggle button
                    FilterChip(
                        selected = uiState.isSimulatingDemo,
                        onClick = { viewModel.toggleDemoSimulation() },
                        label = {
                            Text(
                                text = if (uiState.isSimulatingDemo) "Demo On" else "Demo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD4A373),
                            selectedLabelColor = Color(0xFF332005)
                        ),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("demo_mode_toggle")
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Rationale Banner (if denied)
            if (!uiState.hasLocationPermission) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("permission_warning")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Precise Location Required",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "CartFinder needs GPS location permission to pinpoint where you parked your golf cart on the course and lead you back.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("grant_permission_button")
                            ) {
                                Text("Grant GPS Permission")
                            }
                        }
                    }
                }
            }

            // GPS Signal Quality Pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val hasLock = uiState.currentLocation != null
                        Icon(
                            imageVector = if (hasLock) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                            contentDescription = null,
                            tint = if (hasLock) Color(0xFF2D6A4F) else Color(0xFFE76F51),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val accText = uiState.gpsAccuracyMeters?.let { "±${it.toInt()}m" } ?: "Searching..."
                        Text(
                            text = if (uiState.isSimulatingDemo) "Demo Golf Course GPS ($accText)" else "GPS Status: $accText",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.currentLocation != null) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.4f, %.4f",
                                uiState.currentLocation!!.latitude,
                                uiState.currentLocation!!.longitude
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // STATE A: Golf Cart is NOT Parked
            if (uiState.parkedLocation == null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("park_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Big Cart Icon / Illustration
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Color(0xFFD8F3DC), Color(0xFFB7E4C7))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsGolf,
                                    contentDescription = null,
                                    tint = Color(0xFF1B4332),
                                    modifier = Modifier.size(54.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            Text(
                                text = "Ready to Park Your Cart?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "When you step out of your golf cart, tap the button below. We'll lock your high-precision coordinates so you can find your way back from anywhere on the course.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Giant "Park Golf Cart Here" Button
                            Button(
                                onClick = { showParkDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .shadow(6.dp, RoundedCornerShape(16.dp))
                                    .testTag("park_cart_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1B4332)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PinDrop,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Park Golf Cart Here",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

            } else {
                // STATE B: Cart IS Parked! (Locate / Return Mode)
                val parked = uiState.parkedLocation!!
                val timeParked = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(parked.timestamp))

                // Parked Status Banner Card
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1B4332),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parked_status_banner")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE63946)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PinDrop,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = parked.spotLabel.ifBlank { "Golf Cart Parked" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Marked at $timeParked • GPS ±${parked.accuracyMeters.toInt()}m",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFD8F3DC)
                                        )
                                    }
                                }
                            }

                            if (parked.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Note: ${parked.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE8F1EC)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Google Maps walking navigation external intent button
                            OutlinedButton(
                                onClick = {
                                    val gmmIntentUri = Uri.parse(
                                        "google.navigation:q=${parked.latitude},${parked.longitude}&mode=w"
                                    )
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        val browserIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${parked.latitude},${parked.longitude}&travelmode=walking")
                                        )
                                        context.startActivity(browserIntent)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = Brush.horizontalGradient(listOf(Color(0xFFD8F3DC), Color.White))
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_external_maps_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsWalk,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Open Turn-by-Turn in Google Maps",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Interactive Map View with clear golf cart pin & path
                item {
                    Text(
                        text = "MAP VIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    GolfCartMapView(
                        currentLocation = uiState.currentLocation,
                        parkedLocation = uiState.parkedLocation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    )
                }

                // Compass & Live Distance HUD
                item {
                    CompassBearingHud(
                        distanceMeters = uiState.distanceMeters,
                        bearingDegrees = uiState.bearingDegrees,
                        needleAngle = uiState.relativeNeedleAngle,
                        unit = uiState.preferredUnit,
                        onUnitClick = { viewModel.setPreferredUnit(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Demo "Walk Closer" Button (only visible when Demo simulation active)
                if (uiState.isSimulatingDemo) {
                    item {
                        Button(
                            onClick = { viewModel.simulateWalkCloser() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("demo_walk_closer_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD4A373)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = Color(0xFF332005)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Demo: Simulate Walking 35% Closer",
                                color = Color(0xFF332005),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Arrival & Forget Location Card
                item {
                    ArrivalCard(
                        isArrived = uiState.isArrived,
                        onClearLocation = { showConfirmClearDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bottom breathing room
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Park Cart Dialog
    if (showParkDialog) {
        ParkCartDialog(
            accuracyMeters = uiState.gpsAccuracyMeters,
            onConfirm = { label, note ->
                viewModel.markCurrentLocationAsParked(label, note)
                showParkDialog = false
            },
            onDismiss = { showParkDialog = false }
        )
    }

    // Confirm Clear Dialog
    if (showConfirmClearDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = {
                Text(
                    text = "Forget This Parking Spot?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will clear the current golf cart pin so you are ready to mark your next spot."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearParkedLocation()
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC1121F)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_clear_button")
                ) {
                    Text("Forget Spot")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showConfirmClearDialog = false },
                    modifier = Modifier.testTag("cancel_clear_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
