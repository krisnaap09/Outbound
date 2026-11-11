package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CURRENT_APP_VERSION_CODE
import com.example.ui.components.VersionUpdateDialog
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.OutboundViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
  viewModel: OutboundViewModel
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var currentTabIndex by remember { mutableIntStateOf(0) }

  val hasUpdate = state.appVersionInfo != null &&
    state.appVersionInfo!!.latestVersionCode > CURRENT_APP_VERSION_CODE

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Outbound Manual Reguler",
              fontSize = 16.sp,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AmberAccent)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "T${state.selectedTrip}",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
              )
            }
          }
        },
        actions = {
          // Version / Update Checker icon
          IconButton(
            onClick = { viewModel.setVersionDialogVisible(true) },
            modifier = Modifier.testTag("version_check_button")
          ) {
            BadgedBox(badge = {
              if (hasUpdate) {
                Badge(containerColor = AmberAccent) {
                  Text("!", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
              }
            }) {
              Icon(
                imageVector = if (hasUpdate) Icons.Default.SystemUpdate else Icons.Default.Info,
                contentDescription = "Versi Aplikasi",
                tint = if (hasUpdate) AmberAccent else TextMuted
              )
            }
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = Navy800
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = Navy800,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          selected = currentTabIndex == 0,
          onClick = { currentTabIndex = 0 },
          icon = {
            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Operasional")
          },
          label = {
            Text("Operasional Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = BluePrimary,
            indicatorColor = BluePrimary,
            unselectedIconColor = TextMuted,
            unselectedTextColor = TextMuted
          ),
          modifier = Modifier.testTag("nav_item_scan")
        )

        NavigationBarItem(
          selected = currentTabIndex == 1,
          onClick = { currentTabIndex = 1 },
          icon = {
            BadgedBox(badge = {
              if (state.boundBatches.isNotEmpty()) {
                Badge(containerColor = AmberAccent) {
                  Text("${state.boundBatches.size}", color = Color.Black, fontSize = 9.sp)
                }
              }
            }) {
              Icon(Icons.Default.History, contentDescription = "History & Sync")
            }
          },
          label = {
            Text("History & Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = BluePrimary,
            indicatorColor = BluePrimary,
            unselectedIconColor = TextMuted,
            unselectedTextColor = TextMuted
          ),
          modifier = Modifier.testTag("nav_item_history")
        )
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .background(Navy900)
    ) {
      when (currentTabIndex) {
        0 -> OutboundScanScreen(state = state, viewModel = viewModel)
        1 -> HistoryScreen(state = state)
      }
    }

    if (state.isVersionDialogVisible) {
      VersionUpdateDialog(
        versionInfo = state.appVersionInfo,
        onDismiss = { viewModel.setVersionDialogVisible(false) }
      )
    }
  }
}
