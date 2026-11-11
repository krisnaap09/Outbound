package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BagsealRecord
import com.example.data.model.StagingBatch
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.OutboundUiState
import com.example.util.BarcodeView
import com.example.util.SheetsExporter
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
  state: OutboundUiState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedTripFilter by remember { mutableStateOf<Int?>(null) } // null = All Trips

  val filteredBatches = remember(state.boundBatches, searchQuery, selectedTripFilter) {
    state.boundBatches.filter { batch ->
      val matchesTrip = selectedTripFilter == null || batch.tripNumber == selectedTripFilter
      val matchesQuery = searchQuery.isBlank() ||
        batch.stagingDestination.contains(searchQuery.trim(), ignoreCase = true) ||
        batch.bagseals.any { it.contains(searchQuery.trim(), ignoreCase = true) }
      matchesTrip && matchesQuery
    }
  }

  val totalBoundBagseals = remember(state.boundBatches) {
    state.boundBatches.sumOf { it.scannedCount }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(Navy900)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Google Sheets Sync Card (Featured requirement)
    item {
      GoogleSheetsSyncCard(
        batches = state.boundBatches,
        bagseals = state.allBagseals.filter { it.isBound },
        onCopyToClipboard = {
          SheetsExporter.copyToClipboardForGoogleSheets(
            context,
            state.boundBatches,
            state.allBagseals.filter { it.isBound }
          )
        },
        onShareCsv = {
          SheetsExporter.shareToGoogleSheetsOrExcel(
            context,
            state.boundBatches,
            state.allBagseals.filter { it.isBound }
          )
        }
      )
    }

    // 2. Operational Statistics Summary
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Navy800),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          StatColumn(label = "Tanggal", value = state.operationalDate)
          StatColumn(label = "Total Ikat", value = "${state.boundBatches.size} Batch")
          StatColumn(label = "Total Bagseal", value = "$totalBoundBagseals Pcs", isHighlight = true)
        }
      }
    }

    // 3. Search and Trip Filter chips
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Navy800, RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Cari staging / nomor bagseal...", color = TextMuted, fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = Navy700,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("history_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          TripFilterChip(
            label = "Semua",
            isSelected = selectedTripFilter == null,
            onClick = { selectedTripFilter = null }
          )
          for (t in 1..4) {
            TripFilterChip(
              label = "Trip $t",
              isSelected = selectedTripFilter == t,
              onClick = { selectedTripFilter = t }
            )
          }
        }
      }
    }

    // 4. Bound Batches List
    if (filteredBatches.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = Navy800.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Description,
              contentDescription = null,
              tint = TextMuted,
              modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = if (searchQuery.isNotBlank() || selectedTripFilter != null) "Tidak ada riwayat ikat yang sesuai filter." else "Belum ada batch yang diikat hari ini.",
              fontSize = 13.sp,
              color = TextMuted,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    } else {
      items(filteredBatches, key = { it.id }) { batch ->
        BoundBatchCard(batch = batch)
      }
    }

    item {
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun GoogleSheetsSyncCard(
  batches: List<StagingBatch>,
  bagseals: List<BagsealRecord>,
  onCopyToClipboard: () -> Unit,
  onShareCsv: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Navy800),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldSuccess.copy(alpha = 0.6f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("google_sheets_sync_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(EmeraldSuccess.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.TableChart,
              contentDescription = null,
              tint = EmeraldSuccess,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Ekspor Data (Excel / CSV / Sheets)",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Unduh atau salin data outbound ke spreadsheet",
              fontSize = 11.sp,
              color = TextMuted
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Tersedia ${batches.size} batch ikat (${bagseals.size} bagseal) siap diekspor ke Google Sheets / Excel.",
        fontSize = 12.sp,
        color = Color(0xFFCBD5E1)
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Copy TSV
        Button(
          onClick = onCopyToClipboard,
          enabled = batches.isNotEmpty(),
          colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("copy_tsv_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Salin TSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Export CSV / Excel
        Button(
          onClick = onShareCsv,
          enabled = batches.isNotEmpty(),
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("export_csv_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Buka di Sheets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun BoundBatchCard(batch: StagingBatch) {
  var isExpanded by remember { mutableStateOf(false) }
  val boundTime = batch.getFormattedBoundTime()

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Navy800),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("bound_batch_${batch.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isExpanded = !isExpanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(BluePrimary)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = batch.stagingDestination,
              fontSize = 14.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Trip ${batch.tripNumber}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = AmberAccent
            )
            Text(
              text = "Diikat: $boundTime",
              fontSize = 10.sp,
              color = TextMuted
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(EmeraldSuccess.copy(alpha = 0.2f))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "${batch.scannedCount} / ${batch.targetQty} Pcs",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = EmeraldSuccess
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = TextMuted
          )
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
        ) {
          Text(
            text = "Daftar Bagseal di Ikat Ini (${batch.bagseals.size}):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted
          )
          Spacer(modifier = Modifier.height(6.dp))

          for (code in batch.bagseals) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(Navy900, RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(6.dp))
              BarcodeView(
                barcodeText = code,
                showLabel = false,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StatColumn(label: String, value: String, isHighlight: Boolean = false) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, fontSize = 10.sp, color = TextMuted)
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = value,
      fontSize = 14.sp,
      fontWeight = FontWeight.ExtraBold,
      color = if (isHighlight) EmeraldSuccess else Color.White
    )
  }
}

@Composable
private fun TripFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (isSelected) BluePrimary else Navy700)
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = if (isSelected) Color.White else TextLight
    )
  }
}
