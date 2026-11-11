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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BagsealRecord
import com.example.data.model.BagsealTag
import com.example.ui.components.BarcodeEnlargeDialog
import com.example.ui.components.CameraScannerDialog
import com.example.ui.components.StagingDestinationSelector
import com.example.ui.components.TripSelectorBar
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.OutboundUiState
import com.example.ui.viewmodel.OutboundViewModel
import com.example.util.BarcodeView

@Composable
fun OutboundScanScreen(
  state: OutboundUiState,
  viewModel: OutboundViewModel,
  modifier: Modifier = Modifier
) {
  val batchCountPerTrip = remember(state.boundBatches) {
    state.boundBatches.groupBy { it.tripNumber }.mapValues { it.value.size }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Navy900)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // 1. Operational Trip Selector
      item {
        TripSelectorBar(
          currentTrip = state.selectedTrip,
          batchCountPerTrip = batchCountPerTrip,
          onTripSelected = { viewModel.selectTrip(it) },
          onCompleteTrip = { viewModel.advanceTrip() }
        )
      }

      // 2. Staging Destination Selector
      item {
        StagingDestinationSelector(
          selectedDestination = state.selectedDestination,
          onDestinationSelected = { viewModel.selectDestination(it) }
        )
      }

      // 3. TARGET QTY SECTION WITH DEDICATED EDIT FEATURE
      item {
        QuantityInputSection(
          targetQty = state.targetQty,
          isConfirmed = state.isQtyConfirmed,
          destination = state.selectedDestination,
          scannedCount = state.activeBagseals.size,
          onQtyChange = { viewModel.setTargetQty(it) },
          onOpenEditDialog = { viewModel.setEditQtyDialogVisible(true) }
        )
      }

      // 4. SCANNING CONTROLS (Unlocked when Target Qty > 0)
      item {
        if (!state.isQtyConfirmed || state.targetQty <= 0) {
          LockedScanningPlaceholder(destination = state.selectedDestination)
        } else {
          ActiveScanningControls(
            state = state,
            viewModel = viewModel
          )
        }
      }

      // 5. Progress summary header
      item {
        ScannedProgressHeader(
          scannedCount = state.activeBagseals.size,
          targetQty = state.targetQty,
          destination = state.selectedDestination,
          tripNumber = state.selectedTrip
        )
      }

      // 6. List of active scanned bagseals
      if (state.activeBagseals.isEmpty()) {
        item {
          EmptyBagsealsCard(isLocked = !state.isQtyConfirmed || state.targetQty <= 0)
        }
      } else {
        items(
          items = state.activeBagseals,
          key = { "${it.id}_${it.code}" }
        ) { item ->
          BagsealItemCard(
            item = item,
            onDelete = { viewModel.deleteBagseal(item) },
            onEnlargeBarcode = { viewModel.setEnlargedBarcode(item) }
          )
        }
      }

      // 7. Prominent IKAT (Bind/Lock) Action Button
      item {
        IkatActionButton(
          scannedCount = state.activeBagseals.size,
          targetQty = state.targetQty,
          onIkat = { viewModel.performIkat() },
          modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
      }
    }

    // Camera Scanner Dialog
    if (state.isCameraScannerOpen) {
      CameraScannerDialog(
        onBarcodeScanned = { code ->
          viewModel.addBagseal(code)
          viewModel.setCameraScannerOpen(false)
        },
        onDismiss = { viewModel.setCameraScannerOpen(false) }
      )
    }

    // Barcode Enlarge Dialog for external laser scanning
    state.enlargedBarcodeItem?.let { item ->
      BarcodeEnlargeDialog(
        barcodeCode = item.code,
        destination = item.stagingDestination,
        tripNumber = item.tripNumber,
        tag = item.tag,
        onDismiss = { viewModel.setEnlargedBarcode(null) }
      )
    }

    // Edit Target Qty Dialog
    if (state.isEditQtyDialogVisible) {
      EditTargetQtyDialog(
        currentQty = state.targetQty,
        destination = state.selectedDestination,
        scannedCount = state.activeBagseals.size,
        onConfirm = { newQty -> viewModel.editTargetQty(newQty) },
        onDismiss = { viewModel.setEditQtyDialogVisible(false) }
      )
    }

    // Operation notification toast / snackbar bar
    state.lastOperationMessage?.let { msg ->
      val isError = state.isLastOperationError
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .padding(16.dp)
      ) {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = if (isError) CrimsonAlert.copy(alpha = 0.95f) else Navy800
          ),
          shape = RoundedCornerShape(10.dp),
          border = borderModifier(if (isError) CrimsonAlert else EmeraldSuccess),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = if (isError) Icons.Default.Close else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isError) Color.White else EmeraldSuccess,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = msg,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
              )
            }
            IconButton(
              onClick = { viewModel.clearOperationMessage() },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White.copy(alpha = 0.8f))
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuantityInputSection(
  targetQty: Int,
  isConfirmed: Boolean,
  destination: String,
  scannedCount: Int,
  onQtyChange: (Int) -> Unit,
  onOpenEditDialog: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Navy800),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("qty_input_section")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (isConfirmed && targetQty > 0) Icons.Default.LockOpen else Icons.Default.Lock,
            contentDescription = null,
            tint = if (isConfirmed && targetQty > 0) EmeraldSuccess else AmberAccent,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "TARGET QTY [$destination]",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (isConfirmed && targetQty > 0) EmeraldSuccess else AmberAccent
          )
        }

        if (isConfirmed && targetQty > 0) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(EmeraldSuccess.copy(alpha = 0.2f))
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = "SCAN AKTIF",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = EmeraldSuccess
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (isConfirmed && targetQty > 0) {
        // Confirmed state: clear display with dedicated "Edit Qty" button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "$targetQty",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Bagseal Target",
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 4.dp)
              )
            }
            Text(
              text = "Terisi $scannedCount dari $targetQty bagseal",
              fontSize = 11.sp,
              color = if (scannedCount >= targetQty) EmeraldSuccess else AmberAccent
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Quick +1 button
            QuickQtyButton(label = "+1", onClick = { onQtyChange(targetQty + 1) })

            // Dedicated Edit Target Qty Button
            Button(
              onClick = onOpenEditDialog,
              colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .height(42.dp)
                .testTag("edit_target_qty_button")
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Edit Qty", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      } else {
        // Not configured yet: Manual input first
        var textInput by remember { mutableStateOf("") }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = textInput,
            onValueChange = { input ->
              if (input.all { it.isDigit() } && input.length <= 4) {
                textInput = input
                val parsed = input.toIntOrNull() ?: 0
                if (parsed > 0) {
                  onQtyChange(parsed)
                }
              }
            },
            placeholder = { Text("Input Qty...", color = TextMuted) },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Number,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
              val parsed = textInput.toIntOrNull() ?: 0
              if (parsed > 0) onQtyChange(parsed)
            }),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = AmberAccent,
              unfocusedBorderColor = Navy700,
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold
            ),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("target_qty_input")
          )

          Spacer(modifier = Modifier.width(8.dp))

          // Quick preset buttons
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            QuickQtyButton(label = "+5", onClick = { onQtyChange(5) })
            QuickQtyButton(label = "+10", onClick = { onQtyChange(10) })
            QuickQtyButton(label = "+20", onClick = { onQtyChange(20) })
          }
        }

        Text(
          text = "Wajib masukkan jumlah target bagseal untuk membuka fitur scan di staging ini.",
          fontSize = 11.sp,
          color = AmberAccent,
          modifier = Modifier.padding(top = 6.dp)
        )
      }
    }
  }
}

@Composable
fun EditTargetQtyDialog(
  currentQty: Int,
  destination: String,
  scannedCount: Int,
  onConfirm: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  var qtyInput by remember { mutableStateOf(if (currentQty > 0) currentQty.toString() else "") }
  var qtyInt by remember { mutableIntStateOf(currentQty) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Navy800),
      border = borderModifier(BluePrimary),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("edit_target_qty_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Target Qty [$destination]", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (scannedCount > 0) {
          Text(
            text = "Sudah ada $scannedCount bagseal discan di staging ini.",
            fontSize = 12.sp,
            color = AmberAccent,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
          value = qtyInput,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 4) {
              qtyInput = input
              qtyInt = input.toIntOrNull() ?: 0
            }
          },
          label = { Text("Jumlah Target Qty Baru", color = TextMuted) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
          keyboardActions = KeyboardActions(onDone = {
            if (qtyInt > 0) onConfirm(qtyInt)
          }),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = Navy700,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
          ),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Quick add/subtract adjustments
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          QuickQtyButton(label = "-5", onClick = {
            val updated = (qtyInt - 5).coerceAtLeast(1)
            qtyInt = updated
            qtyInput = updated.toString()
          })
          QuickQtyButton(label = "-1", onClick = {
            val updated = (qtyInt - 1).coerceAtLeast(1)
            qtyInt = updated
            qtyInput = updated.toString()
          })
          QuickQtyButton(label = "+1", onClick = {
            val updated = qtyInt + 1
            qtyInt = updated
            qtyInput = updated.toString()
          })
          QuickQtyButton(label = "+5", onClick = {
            val updated = qtyInt + 5
            qtyInt = updated
            qtyInput = updated.toString()
          })
          QuickQtyButton(label = "+10", onClick = {
            val updated = qtyInt + 10
            qtyInt = updated
            qtyInput = updated.toString()
          })
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(46.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Batal", color = TextMuted)
          }

          Button(
            onClick = {
              if (qtyInt > 0) {
                onConfirm(qtyInt)
              }
            },
            enabled = qtyInt > 0,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
            modifier = Modifier
              .weight(1f)
              .height(46.dp),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Simpan Qty", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
private fun QuickQtyButton(label: String, isReset: Boolean = false, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (isReset) CrimsonAlert.copy(alpha = 0.2f) else Navy700)
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = if (isReset) CrimsonAlert else TextLight
    )
  }
}

@Composable
private fun LockedScanningPlaceholder(destination: String) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Navy800.copy(alpha = 0.6f)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("locked_scanning_notice")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = "Terkunci",
        tint = AmberAccent,
        modifier = Modifier.size(36.dp)
      )
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = "Scanning [$destination] Terkunci",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Masukkan Target Qty di atas (> 0) untuk mengaktifkan pemindai kamera dan input barcode manual.",
        fontSize = 12.sp,
        color = TextMuted,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun ActiveScanningControls(
  state: OutboundUiState,
  viewModel: OutboundViewModel
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Navy800),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("active_scanning_controls")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "PEMINDAI BAGSEAL (DUAL MODE)",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = TextMuted
        )

        Text(
          text = "Tag: ${state.selectedTag.code}",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = AmberAccent
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Tag selection chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val tags = listOf(BagsealTag.NONE, BagsealTag.REG, BagsealTag.HV, BagsealTag.DOC, BagsealTag.BP)
        for (tag in tags) {
          val isSelected = tag == state.selectedTag
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) BluePrimary else Navy700)
              .clickable { viewModel.setTag(tag) }
              .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (tag == BagsealTag.NONE) "NO TAG" else tag.code,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (isSelected) Color.White else TextLight
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Button: Scan Camera (ML Kit)
      Button(
        onClick = { viewModel.setCameraScannerOpen(true) },
        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("open_camera_scan_button")
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Scan Kamera (ML Kit Barcode)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Manual / Laser Gun Barcode Input with instant Enter handling
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = state.manualInputCode,
          onValueChange = { viewModel.updateManualInputCode(it) },
          placeholder = { Text("Ketik kode / Tembak Gun scanner...", color = TextMuted, fontSize = 13.sp) },
          leadingIcon = {
            Icon(Icons.Default.QrCode, contentDescription = null, tint = AmberAccent)
          },
          keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Send
          ),
          keyboardActions = KeyboardActions(
            onSend = { viewModel.submitManualBagseal() },
            onDone = { viewModel.submitManualBagseal() },
            onGo = { viewModel.submitManualBagseal() }
          ),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BluePrimary,
            unfocusedBorderColor = Navy700,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("manual_barcode_input")
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = { viewModel.submitManualBagseal() },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .height(52.dp)
            .testTag("submit_manual_barcode_button")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Tambah Bagseal",
            tint = Color.White
          )
        }
      }
    }
  }
}

@Composable
private fun ScannedProgressHeader(
  scannedCount: Int,
  targetQty: Int,
  destination: String,
  tripNumber: Int
) {
  val progress = if (targetQty > 0) (scannedCount.toFloat() / targetQty).coerceIn(0f, 1f) else 0f
  val isComplete = targetQty > 0 && scannedCount >= targetQty

  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Navy800),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "PROGRESS SCAN [$destination]",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted
          )
          Text(
            text = "Trip $tripNumber",
            fontSize = 12.sp,
            color = AmberAccent
          )
        }

        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = "$scannedCount",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isComplete) EmeraldSuccess else Color.White
          )
          Text(
            text = " / $targetQty Bagseal",
            fontSize = 14.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp)
          .clip(RoundedCornerShape(4.dp)),
        color = if (isComplete) EmeraldSuccess else BluePrimary,
        trackColor = Navy700
      )
    }
  }
}

@Composable
private fun EmptyBagsealsCard(isLocked: Boolean) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Navy800.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = if (isLocked) "Belum ada bagseal. Masukkan Target Qty untuk mulai scan." else "Belum ada bagseal discan. Gunakan kamera atau tembak scanner barcode.",
        fontSize = 12.sp,
        color = TextMuted,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun BagsealItemCard(
  item: BagsealRecord,
  onDelete: () -> Unit,
  onEnlargeBarcode: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Navy800),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("bagseal_item_${item.code}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = item.code,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            color = Color.White
          )
          if (item.tag != "NONE") {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(AmberAccent)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = item.tag,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
              )
            }
          }
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Hapus",
            tint = CrimsonAlert
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Scannable 1D Barcode Preview (tap to enlarge)
      BarcodeView(
        barcodeText = item.code,
        showLabel = false,
        onClick = onEnlargeBarcode,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Ketuk barcode di atas untuk perbesar & tembak scanner eksternal",
        fontSize = 10.sp,
        color = TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
private fun IkatActionButton(
  scannedCount: Int,
  targetQty: Int,
  onIkat: () -> Unit,
  modifier: Modifier = Modifier
) {
  val canIkat = scannedCount > 0
  Button(
    onClick = onIkat,
    enabled = canIkat,
    colors = ButtonDefaults.buttonColors(
      containerColor = EmeraldSuccess,
      disabledContainerColor = Navy700
    ),
    shape = RoundedCornerShape(12.dp),
    modifier = modifier
      .fillMaxWidth()
      .height(54.dp)
      .testTag("ikat_bind_button")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = if (canIkat) Color.White else TextMuted,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = if (canIkat) "IKAT SEKARANG ($scannedCount Bagseal)" else "IKAT (Belum ada Bagseal)",
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (canIkat) Color.White else TextMuted
      )
    }
  }
}

private fun borderModifier(color: Color): androidx.compose.foundation.BorderStroke {
  return androidx.compose.foundation.BorderStroke(1.dp, color)
}
