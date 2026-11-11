package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted

@Composable
fun TripSelectorBar(
  currentTrip: Int,
  batchCountPerTrip: Map<Int, Int>,
  onTripSelected: (Int) -> Unit,
  onCompleteTrip: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Navy800, RoundedCornerShape(14.dp))
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "PILIHAN TRIP OPERASIONAL",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextMuted
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(BluePrimary.copy(alpha = 0.2f))
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text(
          text = "Aktif: Trip $currentTrip",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = BluePrimary
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      for (trip in 1..4) {
        val isSelected = trip == currentTrip
        val count = batchCountPerTrip[trip] ?: 0
        val isCompleted = count > 0 && !isSelected

        val bgColor by animateColorAsState(
          targetValue = when {
            isSelected -> BluePrimary
            isCompleted -> Navy700
            else -> Navy800
          },
          label = "tripBg"
        )

        Box(
          modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
              width = if (isSelected) 2.dp else 1.dp,
              color = if (isSelected) AmberAccent else Navy700,
              shape = RoundedCornerShape(10.dp)
            )
            .clickable { onTripSelected(trip) }
            .testTag("trip_tab_$trip"),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "TRIP $trip",
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextLight
              )
              if (isCompleted) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Selesai",
                  tint = EmeraldSuccess,
                  modifier = Modifier.size(12.dp)
                )
              }
            }
            Text(
              text = "$count Ikat",
              fontSize = 10.sp,
              color = if (isSelected) Color(0xFFE2E8F0) else TextMuted
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Advance trip button
    Button(
      onClick = onCompleteTrip,
      colors = ButtonDefaults.buttonColors(
        containerColor = if (currentTrip < 4) AmberAccent else EmeraldSuccess
      ),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(38.dp)
        .testTag("advance_trip_button")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Default.FastForward,
          contentDescription = null,
          tint = Color.Black,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (currentTrip < 4) "Selesaikan Trip $currentTrip & Lanjut ke Trip ${currentTrip + 1}" else "Selesaikan Trip 4 (Siklus Selesai)",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.Black
        )
      }
    }
  }
}
