package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.Navy800
import com.example.ui.theme.TextMuted
import com.example.util.BarcodeView

@Composable
fun BarcodeEnlargeDialog(
  barcodeCode: String,
  destination: String,
  tripNumber: Int,
  tag: String,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Navy800),
      modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)
        .testTag("barcode_enlarge_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.QrCode2,
              contentDescription = null,
              tint = AmberAccent,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = " Barcode Bagseal (Tembak Manual)",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High contrast barcode container for laser scanner guns
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(vertical = 18.dp, horizontal = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BarcodeView(
              barcodeText = barcodeCode,
              showLabel = false,
              modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = barcodeCode,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.ExtraBold,
              fontSize = 18.sp,
              color = Color.Black,
              letterSpacing = 2.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Metadata chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          MetadataBadge(label = "Tujuan", value = destination)
          MetadataBadge(label = "Trip", value = "Trip $tripNumber")
          if (tag != "NONE") {
            MetadataBadge(label = "Tag", value = tag)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Arahkan barcode scanner / PDA gun ke layar untuk menembak barcode bagseal ini.",
          fontSize = 11.sp,
          color = TextMuted,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Selesai & Tutup", color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun MetadataBadge(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, fontSize = 10.sp, color = TextMuted)
    Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
  }
}
