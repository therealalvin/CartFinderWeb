package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ArrivalCard(
    isArrived: Boolean,
    onClearLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                2.dp,
                if (isArrived) Color(0xFF2D6A4F) else Color(0xFFD1D5DB),
                RoundedCornerShape(20.dp)
            )
            .testTag("arrival_card"),
        color = if (isArrived) Color(0xFFD8F3DC) else Color(0xFFF9FAFB),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isArrived) Color(0xFF1B4332) else Color(0xFFD8F3DC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isArrived) Icons.Default.Check else Icons.Default.SportsGolf,
                        contentDescription = null,
                        tint = if (isArrived) Color.White else Color(0xFF1B4332),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArrived) "You Have Arrived!" else "Back at your golf cart?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = if (isArrived)
                            "Clear the spot now so you can mark a new one next time."
                        else
                            "Forget saved coordinates when you're done driving.",
                        fontSize = 14.sp,
                        color = Color(0xFF374151)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClearLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("clear_parked_spot_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isArrived) Color(0xFF1B4332) else Color(0xFFB91C1C),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "I'm Here — Forget This Spot",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}
