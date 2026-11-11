package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppVersionInfo
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.TextMuted

const val CURRENT_APP_VERSION_CODE = 1
const val CURRENT_APP_VERSION_NAME = "1.0.0"

@Composable
fun VersionUpdateDialog(
  versionInfo: AppVersionInfo?,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val hasUpdate = versionInfo != null && versionInfo.latestVersionCode > CURRENT_APP_VERSION_CODE

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Navy800),
      modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)
        .testTag("version_dialog")
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
          Text(
            text = "Informasi Versi Aplikasi",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(if (hasUpdate) AmberAccent.copy(alpha = 0.2f) else EmeraldSuccess.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (hasUpdate) Icons.Default.SystemUpdate else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (hasUpdate) AmberAccent else EmeraldSuccess,
            modifier = Modifier.size(32.dp)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = if (hasUpdate) "Pembaruan Tersedia!" else "Aplikasi Versi Terbaru",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Versi Saat Ini: v$CURRENT_APP_VERSION_NAME ($CURRENT_APP_VERSION_CODE)",
          fontSize = 12.sp,
          color = TextMuted
        )

        if (hasUpdate && versionInfo != null) {
          Text(
            text = "Versi Rilis Terbaru: v${versionInfo.latestVersionName} (${versionInfo.latestVersionCode})",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AmberAccent,
            modifier = Modifier.padding(top = 2.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(Navy900, RoundedCornerShape(8.dp))
              .padding(12.dp)
          ) {
            Text(
              text = "Catatan Rilis (Changelog):",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = versionInfo.releaseNotes.ifBlank { "Peningkatan performa pemindai barcode dan sinkronisasi cloud." },
              fontSize = 11.sp,
              color = TextMuted
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = {
              if (versionInfo.downloadUrl.isNotBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(versionInfo.downloadUrl))
                context.startActivity(intent)
              }
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Perbarui Aplikasi Sekarang", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        } else {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Anda menggunakan versi terbaru dari Outbound Manual Reguler. Semua fungsi cloud database dan barcode scanner berjalan optimal.",
            fontSize = 12.sp,
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
            Text("Tutup", color = Color.White)
          }
        }
      }
    }
  }
}
