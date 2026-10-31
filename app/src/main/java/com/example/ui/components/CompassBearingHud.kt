package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DistanceUnit
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassBearingHud(
    distanceMeters: Float?,
    bearingDegrees: Float?,
    needleAngle: Float,
    unit: DistanceUnit,
    onUnitClick: (DistanceUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    // Formatted distance string
    val distanceDisplay = when (unit) {
        DistanceUnit.YARDS -> {
            val yards = (distanceMeters ?: 0f) * 1.09361f
            "${yards.toInt()} yd"
        }
        DistanceUnit.METERS -> {
            "${(distanceMeters ?: 0f).toInt()} m"
        }
        DistanceUnit.FEET -> {
            val feet = (distanceMeters ?: 0f) * 3.28084f
            "${feet.toInt()} ft"
        }
    }

    // Is user facing directly toward cart? (within +/- 15 degrees)
    val isFacingCart = needleAngle in 0f..15f || needleAngle in 345f..360f

    val dialRingColor by animateColorAsState(
        targetValue = if (isFacingCart) Color(0xFF1B4332) else Color(0xFF9CA3AF),
        animationSpec = tween(300),
        label = "dialColor"
    )

    val animatedRotation by animateFloatAsState(
        targetValue = needleAngle,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "needleRotation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, Color(0xFFD1D5DB), RoundedCornerShape(24.dp))
            .testTag("compass_bearing_hud"),
        tonalElevation = 2.dp,
        color = Color(0xFFF9FAFB)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Unit Switcher Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WALKING GUIDANCE",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF111827),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                // Large readable toggle chips
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE5E7EB))
                        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    DistanceUnit.values().forEach { u ->
                        val selected = u == unit
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color(0xFF1B4332) else Color.Transparent)
                                .clickable { onUnitClick(u) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("unit_toggle_${u.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = u.shortLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (selected) Color.White else Color(0xFF374151)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compass Dial & Needle
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White,
                                Color(0xFFEDF2F7)
                            )
                        )
                    )
                    .border(3.5.dp, dialRingColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Compass Cardinal Marks (N, E, S, W, ticks)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    for (degree in 0 until 360 step 30) {
                        val angleRad = Math.toRadians((degree - 90).toDouble())
                        val tickLength = if (degree % 90 == 0) 14.dp.toPx() else 7.dp.toPx()
                        val strokeWidth = if (degree % 90 == 0) 3f else 1.5f

                        val startX = center.x + (radius - 8.dp.toPx()) * cos(angleRad).toFloat()
                        val startY = center.y + (radius - 8.dp.toPx()) * sin(angleRad).toFloat()
                        val endX = center.x + (radius - 8.dp.toPx() - tickLength) * cos(angleRad).toFloat()
                        val endY = center.y + (radius - 8.dp.toPx() - tickLength) * sin(angleRad).toFloat()

                        drawLine(
                            color = if (degree == 0) Color(0xFFDC2626) else Color(0xFF6B7280),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeWidth
                        )
                    }
                }

                // Rotating Needle & Cart Pointer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedRotation),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(170.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val needleLength = size.minDimension / 2f - 20.dp.toPx()

                        // North / Target Arrow
                        val arrowColor = if (isFacingCart) Color(0xFF1B4332) else Color(0xFFE63946)
                        val arrowPath = Path().apply {
                            moveTo(center.x, center.y - needleLength)
                            lineTo(center.x - 15.dp.toPx(), center.y - 15.dp.toPx())
                            lineTo(center.x, center.y)
                            close()
                        }
                        drawPath(arrowPath, color = arrowColor)

                        val arrowBevel = Path().apply {
                            moveTo(center.x, center.y - needleLength)
                            lineTo(center.x + 15.dp.toPx(), center.y - 15.dp.toPx())
                            lineTo(center.x, center.y)
                            close()
                        }
                        drawPath(arrowBevel, color = arrowColor.copy(alpha = 0.8f))

                        // Tail of needle
                        val tailPath = Path().apply {
                            moveTo(center.x, center.y + needleLength * 0.5f)
                            lineTo(center.x - 10.dp.toPx(), center.y + 10.dp.toPx())
                            lineTo(center.x, center.y)
                            close()
                        }
                        drawPath(tailPath, color = Color(0xFF4B5563))
                    }

                    // Top golf pin icon atop needle tip
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 14.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE63946))
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Golf cart pointer",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Center Hub with Large, Clear Live Distance
                Box(
                    modifier = Modifier
                        .size(105.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.5.dp, Color(0xFFD1D5DB), CircleShape)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = distanceDisplay,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1B4332)
                        )
                        Text(
                            text = "TO CART",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = Color(0xFF374151)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guidance status badge with large, high-contrast text
            val statusText = when {
                (distanceMeters ?: 999f) <= 8f -> "You are at your golf cart!"
                isFacingCart -> "Head straight forward"
                needleAngle < 180f -> "Turn right ${needleAngle.toInt()}°"
                else -> "Turn left ${(360f - needleAngle).toInt()}°"
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isFacingCart) Color(0xFFD8F3DC) else Color(0xFFFFFFFF)
                    )
                    .border(
                        2.dp,
                        if (isFacingCart) Color(0xFF2D6A4F) else Color(0xFFD1D5DB),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isFacingCart) Icons.Default.CheckCircle else Icons.Default.Navigation,
                    contentDescription = null,
                    tint = if (isFacingCart) Color(0xFF1B4332) else Color(0xFF1B4332),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statusText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFacingCart) Color(0xFF081C15) else Color(0xFF111827)
                )
            }
        }
    }
}
