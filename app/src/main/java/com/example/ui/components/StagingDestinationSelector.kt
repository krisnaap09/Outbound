package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.StagingDestinationConstants
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StagingDestinationSelector(
  selectedDestination: String,
  onDestinationSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var showSelectionDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Navy800, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "TUJUAN STAGING",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextMuted
      )

      Text(
        text = "34 Kode Default",
        fontSize = 10.sp,
        color = AmberAccent
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Prominent selection card
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(Navy900)
        .border(1.5.dp, BluePrimary, RoundedCornerShape(10.dp))
        .clickable { showSelectionDialog = true }
        .padding(horizontal = 14.dp, vertical = 12.dp)
        .testTag("destination_selector_card"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(BluePrimary),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = selectedDestination,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
          Text(
            text = "Ketuk untuk ganti tujuan staging",
            fontSize = 11.sp,
            color = TextMuted
          )
        }
      }

      Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = "Pilih Tujuan",
        tint = BluePrimary,
        modifier = Modifier.size(28.dp)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Quick chips for top 6 destinations
    Text(
      text = "Akses Cepat:",
      fontSize = 10.sp,
      color = TextMuted
    )
    Spacer(modifier = Modifier.height(4.dp))
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      val topCodes = listOf("TANB", "TBET", "KBYB", "JGKA", "CNER", "PSMG")
      for (code in topCodes) {
        val isChosen = code == selectedDestination
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isChosen) AmberAccent else Navy700)
            .clickable { onDestinationSelected(code) }
            .padding(horizontal = 10.dp, vertical = 5.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = code,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isChosen) Color.Black else TextLight
          )
        }
      }
    }
  }

  if (showSelectionDialog) {
    DestinationSearchDialog(
      currentCode = selectedDestination,
      onSelect = {
        onDestinationSelected(it)
        showSelectionDialog = false
      },
      onDismiss = { showSelectionDialog = false }
    )
  }
}

@Composable
private fun DestinationSearchDialog(
  currentCode: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val allCodes = StagingDestinationConstants.DEFAULT_CODES
  val filteredCodes = remember(searchQuery) {
    if (searchQuery.isBlank()) {
      allCodes
    } else {
      allCodes.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Navy800),
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.75f)
        .padding(4.dp)
        .testTag("destination_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Pilih Tujuan Staging",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextMuted)
          }
        }

        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Cari kode staging (cth: TANB, BOGU)...", color = TextMuted, fontSize = 13.sp) },
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
            .padding(vertical = 8.dp)
            .testTag("staging_search_input")
        )

        Text(
          text = "Daftar Kode Staging (${filteredCodes.size} dari ${allCodes.size}):",
          fontSize = 11.sp,
          color = TextMuted,
          modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          items(filteredCodes) { code ->
            val isCurrent = code == currentCode
            Surface(
              onClick = { onSelect(code) },
              shape = RoundedCornerShape(8.dp),
              color = if (isCurrent) BluePrimary else Navy900,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("staging_item_$code")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = code,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isCurrent) Color.White else TextLight
                )
                if (isCurrent) {
                  Text(
                    text = "TERPILIH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberAccent
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
