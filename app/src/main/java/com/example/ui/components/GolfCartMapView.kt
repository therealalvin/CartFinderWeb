package com.example.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.GolfCartLocation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun GolfCartMapView(
    currentLocation: Location?,
    parkedLocation: GolfCartLocation?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize osmdroid configuration
    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(18.5)
        }
    }

    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> {
                    mapView.onPause()
                    mapView.onDetach()
                }
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Update markers and polyline when positions change
    LaunchedEffect(currentLocation, parkedLocation) {
        mapView.overlays.clear()

        val points = mutableListOf<GeoPoint>()

        // 1. Add Parked Golf Cart Marker (Clear Pin)
        if (parkedLocation != null) {
            val cartPoint = GeoPoint(parkedLocation.latitude, parkedLocation.longitude)
            points.add(cartPoint)

            val cartMarker = Marker(mapView).apply {
                position = cartPoint
                title = parkedLocation.spotLabel.ifBlank { "Golf Cart" }
                snippet = "Parked location (±${parkedLocation.accuracyMeters.toInt()}m)"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_golf_cart_pin)
            }
            mapView.overlays.add(cartMarker)
            cartMarker.showInfoWindow()
        }

        // 2. Add Current Location Marker
        if (currentLocation != null) {
            val userPoint = GeoPoint(currentLocation.latitude, currentLocation.longitude)
            points.add(userPoint)

            val userMarker = Marker(mapView).apply {
                position = userPoint
                title = "You Are Here"
                snippet = "Accuracy ±${currentLocation.accuracy.toInt()}m"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_my_location_marker)
            }
            mapView.overlays.add(userMarker)
        }

        // 3. Add Walking Polyline between User and Cart
        if (currentLocation != null && parkedLocation != null) {
            val line = Polyline(mapView).apply {
                outlinePaint.color = AndroidColor.parseColor("#E63946")
                outlinePaint.strokeWidth = 10f
                outlinePaint.strokeCap = Paint.Cap.ROUND
                outlinePaint.style = Paint.Style.STROKE
                addPoint(GeoPoint(currentLocation.latitude, currentLocation.longitude))
                addPoint(GeoPoint(parkedLocation.latitude, parkedLocation.longitude))
            }
            mapView.overlays.add(line)
        }

        // Center on cart or both
        if (points.size >= 2) {
            try {
                val minLat = minOf(points[0].latitude, points[1].latitude)
                val maxLat = maxOf(points[0].latitude, points[1].latitude)
                val minLon = minOf(points[0].longitude, points[1].longitude)
                val maxLon = maxOf(points[0].longitude, points[1].longitude)

                val padding = 0.0003
                val boundingBox = BoundingBox(
                    maxLat + padding,
                    maxLon + padding,
                    minLat - padding,
                    minLon - padding
                )
                mapView.zoomToBoundingBox(boundingBox, true, 100)
            } catch (e: Exception) {
                mapView.controller.animateTo(points[0])
            }
        } else if (points.isNotEmpty()) {
            mapView.controller.animateTo(points[0])
        }

        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
            .testTag("map_container")
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Map Controls Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom In
            MapControlButton(
                icon = Icons.Default.Add,
                contentDescription = "Zoom In",
                testTag = "map_zoom_in",
                onClick = { mapView.controller.zoomIn() }
            )

            // Zoom Out
            MapControlButton(
                icon = Icons.Default.Remove,
                contentDescription = "Zoom Out",
                testTag = "map_zoom_out",
                onClick = { mapView.controller.zoomOut() }
            )

            // Recenter on Cart Pin
            if (parkedLocation != null) {
                MapControlButton(
                    icon = Icons.Default.Place,
                    contentDescription = "Center on Golf Cart",
                    tint = Color(0xFFE63946),
                    testTag = "map_center_cart",
                    onClick = {
                        mapView.controller.animateTo(
                            GeoPoint(parkedLocation.latitude, parkedLocation.longitude)
                        )
                        mapView.controller.setZoom(19.0)
                    }
                )
            }

            // Recenter on User Location
            if (currentLocation != null) {
                MapControlButton(
                    icon = Icons.Default.MyLocation,
                    contentDescription = "Center on My Location",
                    tint = Color(0xFF1D70B8),
                    testTag = "map_center_me",
                    onClick = {
                        mapView.controller.animateTo(
                            GeoPoint(currentLocation.latitude, currentLocation.longitude)
                        )
                        mapView.controller.setZoom(19.0)
                    }
                )
            }

            // Fit Both
            if (currentLocation != null && parkedLocation != null) {
                MapControlButton(
                    icon = Icons.Default.CropFree,
                    contentDescription = "Fit Both",
                    tint = MaterialTheme.colorScheme.primary,
                    testTag = "map_fit_both",
                    onClick = {
                        try {
                            val minLat = minOf(currentLocation.latitude, parkedLocation.latitude)
                            val maxLat = maxOf(currentLocation.latitude, parkedLocation.latitude)
                            val minLon = minOf(currentLocation.longitude, parkedLocation.longitude)
                            val maxLon = maxOf(currentLocation.longitude, parkedLocation.longitude)
                            val padding = 0.00035
                            mapView.zoomToBoundingBox(
                                BoundingBox(maxLat + padding, maxLon + padding, minLat - padding, minLon - padding),
                                true,
                                80
                            )
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier
            .size(44.dp)
            .testTag(testTag)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
