package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppVersionInfo
import com.example.data.model.BagsealRecord
import com.example.data.model.BagsealTag
import com.example.data.model.StagingBatch
import com.example.data.repository.OutboundRepository
import com.example.util.SoundVibrationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OutboundUiState(
  val selectedTrip: Int = 1,
  val operationalDate: String = "",
  val selectedDestination: String = "TANB",
  val targetQty: Int = 0,
  val isQtyConfirmed: Boolean = false,
  val isEditQtyDialogVisible: Boolean = false,
  val selectedTag: BagsealTag = BagsealTag.NONE,
  val manualInputCode: String = "",
  val activeBagseals: List<BagsealRecord> = emptyList(),
  val boundBatches: List<StagingBatch> = emptyList(),
  val allBagseals: List<BagsealRecord> = emptyList(),
  val isCameraScannerOpen: Boolean = false,
  val enlargedBarcodeItem: BagsealRecord? = null,
  val appVersionInfo: AppVersionInfo? = null,
  val isVersionDialogVisible: Boolean = false,
  val lastOperationMessage: String? = null,
  val isLastOperationError: Boolean = false,
  val isLoading: Boolean = false
)

class OutboundViewModel(
  application: Application,
  private val repository: OutboundRepository
) : AndroidViewModel(application) {

  private val prefs = application.getSharedPreferences("outbound_prefs", Context.MODE_PRIVATE)

  private val _uiState = MutableStateFlow(
    OutboundUiState(
      operationalDate = getCurrentDateString()
    )
  )
  val uiState: StateFlow<OutboundUiState> = _uiState.asStateFlow()

  init {
    checkCalendarDayReset()
    observeData()
    restoreTargetQtyForCurrentDestination()
  }

  private fun getCurrentDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
  }

  private fun checkCalendarDayReset() {
    val today = getCurrentDateString()
    val lastRecordedDate = prefs.getString("last_operational_date", null)
    val savedTrip = prefs.getInt("current_trip", 1)

    if (lastRecordedDate != null && lastRecordedDate != today) {
      // New calendar day! Reset active trips and operational data to Trip 1
      prefs.edit()
        .putString("last_operational_date", today)
        .putInt("current_trip", 1)
        .apply()

      // Clear saved target qty keys for new day
      clearSavedTargetQuantities()

      _uiState.update {
        it.copy(
          operationalDate = today,
          selectedTrip = 1,
          targetQty = 0,
          isQtyConfirmed = false,
          activeBagseals = emptyList(),
          lastOperationMessage = "Hari berganti ($today). Trip direset otomatis ke Trip 1.",
          isLastOperationError = false
        )
      }
    } else {
      if (lastRecordedDate == null) {
        prefs.edit().putString("last_operational_date", today).apply()
      }
      _uiState.update {
        it.copy(
          operationalDate = today,
          selectedTrip = savedTrip.coerceIn(1, 4)
        )
      }
    }
  }

  private fun clearSavedTargetQuantities() {
    val editor = prefs.edit()
    val allKeys = prefs.all.keys
    for (key in allKeys) {
      if (key.startsWith("qty_")) {
        editor.remove(key)
      }
    }
    editor.apply()
  }

  private fun restoreTargetQtyForCurrentDestination() {
    val state = _uiState.value
    val key = "qty_${state.selectedTrip}_${state.selectedDestination}"
    val savedQty = prefs.getInt(key, 0)
    if (savedQty > 0) {
      _uiState.update {
        it.copy(
          targetQty = savedQty,
          isQtyConfirmed = true
        )
      }
    }
  }

  private fun observeData() {
    // 1. Observe all batches for history and counts
    viewModelScope.launch {
      repository.observeBatches().catch { /* handled */ }.collectLatest { batches ->
        val today = _uiState.value.operationalDate
        val todayBatches = batches.filter { it.operationalDate == today }
        val bound = todayBatches.filter { it.status == "bound" }
        _uiState.update { it.copy(boundBatches = bound) }
      }
    }

    // 2. Observe bagseals
    viewModelScope.launch {
      repository.observeBagseals().catch { /* handled */ }.collectLatest { bagseals ->
        val today = _uiState.value.operationalDate
        val todayBagseals = bagseals.filter { it.operationalDate == today }
        val active = todayBagseals.filter {
          !it.isBound &&
            it.tripNumber == _uiState.value.selectedTrip &&
            it.stagingDestination == _uiState.value.selectedDestination
        }

        _uiState.update { current ->
          // If destination already has active bagseals but targetQty was 0, auto-restore
          val currentTarget = current.targetQty
          val updatedTarget = if (currentTarget <= 0 && active.isNotEmpty()) {
            val saved = prefs.getInt("qty_${current.selectedTrip}_${current.selectedDestination}", 0)
            if (saved > 0) saved else active.size
          } else {
            currentTarget
          }

          current.copy(
            allBagseals = todayBagseals,
            activeBagseals = active,
            targetQty = updatedTarget,
            isQtyConfirmed = updatedTarget > 0
          )
        }
      }
    }

    // 3. Observe App Version for update check
    viewModelScope.launch {
      repository.observeAppVersion().collectLatest { versionInfo ->
        _uiState.update { it.copy(appVersionInfo = versionInfo) }
      }
    }
  }

  fun selectTrip(trip: Int) {
    if (trip in 1..4) {
      prefs.edit().putInt("current_trip", trip).apply()
      val dest = _uiState.value.selectedDestination
      val savedQty = prefs.getInt("qty_${trip}_${dest}", 0)

      _uiState.update {
        it.copy(
          selectedTrip = trip,
          targetQty = savedQty,
          isQtyConfirmed = savedQty > 0,
          manualInputCode = "",
          lastOperationMessage = null
        )
      }
    }
  }

  fun advanceTrip() {
    val current = _uiState.value.selectedTrip
    val nextTrip = if (current < 4) current + 1 else 1
    selectTrip(nextTrip)
    SoundVibrationHelper.playBindSuccess(getApplication())
    _uiState.update {
      it.copy(
        lastOperationMessage = "Trip berganti ke Trip $nextTrip",
        isLastOperationError = false
      )
    }
  }

  fun selectDestination(destination: String) {
    val trip = _uiState.value.selectedTrip
    val savedQty = prefs.getInt("qty_${trip}_${destination}", 0)

    // Check if there are active bagseals for destination in state
    val activeForDest = _uiState.value.allBagseals.filter {
      !it.isBound && it.tripNumber == trip && it.stagingDestination == destination
    }

    val target = if (savedQty > 0) {
      savedQty
    } else if (activeForDest.isNotEmpty()) {
      activeForDest.size
    } else {
      0
    }

    _uiState.update {
      it.copy(
        selectedDestination = destination,
        targetQty = target,
        isQtyConfirmed = target > 0,
        manualInputCode = "",
        lastOperationMessage = null
      )
    }
  }

  fun setTargetQty(qty: Int) {
    val cleanQty = qty.coerceAtLeast(0)
    val state = _uiState.value
    val key = "qty_${state.selectedTrip}_${state.selectedDestination}"

    if (cleanQty > 0) {
      prefs.edit().putInt(key, cleanQty).apply()
    } else {
      prefs.edit().remove(key).apply()
    }

    _uiState.update {
      it.copy(
        targetQty = cleanQty,
        isQtyConfirmed = cleanQty > 0,
        isEditQtyDialogVisible = false,
        lastOperationMessage = if (cleanQty > 0) "Target Qty staging ${state.selectedDestination} diatur: $cleanQty" else null,
        isLastOperationError = false
      )
    }
  }

  fun editTargetQty(newQty: Int) {
    val cleanQty = newQty.coerceAtLeast(1)
    val state = _uiState.value
    val key = "qty_${state.selectedTrip}_${state.selectedDestination}"
    prefs.edit().putInt(key, cleanQty).apply()

    _uiState.update {
      it.copy(
        targetQty = cleanQty,
        isQtyConfirmed = true,
        isEditQtyDialogVisible = false,
        lastOperationMessage = "Target Qty berhasil diubah menjadi $cleanQty",
        isLastOperationError = false
      )
    }
    SoundVibrationHelper.playScanSuccess(getApplication())
  }

  fun setEditQtyDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(isEditQtyDialogVisible = visible) }
  }

  fun setTag(tag: BagsealTag) {
    _uiState.update { it.copy(selectedTag = tag) }
  }

  fun updateManualInputCode(text: String) {
    // Hardware laser scanner gun auto-enter detection
    if (text.contains("\n") || text.contains("\r")) {
      val clean = text.replace("\n", "").replace("\r", "").trim()
      if (clean.isNotBlank()) {
        addBagseal(clean)
      }
      _uiState.update { it.copy(manualInputCode = "") }
    } else {
      _uiState.update { it.copy(manualInputCode = text) }
    }
  }

  fun submitManualBagseal() {
    val code = _uiState.value.manualInputCode
      .replace("\n", "")
      .replace("\r", "")
      .trim()
    if (code.isNotBlank()) {
      addBagseal(code)
      _uiState.update { it.copy(manualInputCode = "") }
    }
  }

  fun addBagseal(code: String) {
    val cleanCode = code
      .replace("\n", "")
      .replace("\r", "")
      .trim()
      .uppercase()

    if (cleanCode.isBlank()) return

    val state = _uiState.value

    // 1. Require Target Qty First check
    var effectiveTargetQty = state.targetQty
    if (!state.isQtyConfirmed || effectiveTargetQty <= 0) {
      // Auto-heal if active bagseals exist, otherwise prompt user
      if (state.activeBagseals.isNotEmpty()) {
        effectiveTargetQty = state.activeBagseals.size + 1
        setTargetQty(effectiveTargetQty)
      } else {
        SoundVibrationHelper.playScanError(getApplication())
        _uiState.update {
          it.copy(
            lastOperationMessage = "Target Qty untuk staging ${state.selectedDestination} belum diatur. Masukkan Target Qty terlebih dahulu.",
            isLastOperationError = true
          )
        }
        return
      }
    }

    // 2. Check duplicate in current active staging
    val exists = state.activeBagseals.any { it.code.equals(cleanCode, ignoreCase = true) }
    if (exists) {
      SoundVibrationHelper.playScanError(getApplication())
      _uiState.update {
        it.copy(
          lastOperationMessage = "Bagseal $cleanCode sudah ada di staging ${state.selectedDestination}!",
          isLastOperationError = true
        )
      }
      return
    }

    // 3. Inform if duplicate with previously bound batches today
    val boundBatch = state.boundBatches.firstOrNull { it.bagseals.contains(cleanCode) }
    if (boundBatch != null) {
      SoundVibrationHelper.playScanError(getApplication())
      _uiState.update {
        it.copy(
          lastOperationMessage = "Perhatian: Bagseal $cleanCode sudah pernah diikat pada Trip ${boundBatch.tripNumber} (${boundBatch.stagingDestination})!",
          isLastOperationError = true
        )
      }
      return
    }

    val newRecord = BagsealRecord(
      code = cleanCode,
      stagingDestination = state.selectedDestination,
      tripNumber = state.selectedTrip,
      operationalDate = state.operationalDate,
      tag = state.selectedTag.code,
      isBound = false
    )

    viewModelScope.launch {
      val result = repository.addBagseal(newRecord)
      if (result.isSuccess) {
        SoundVibrationHelper.playScanSuccess(getApplication())
        val newCount = _uiState.value.activeBagseals.size + 1
        _uiState.update {
          it.copy(
            lastOperationMessage = "✓ Bagseal $cleanCode berhasil ditambahkan (${newCount}/$effectiveTargetQty)",
            isLastOperationError = false
          )
        }
      } else {
        SoundVibrationHelper.playScanError(getApplication())
        val err = result.exceptionOrNull()?.localizedMessage ?: "Terjadi kesalahan database"
        _uiState.update {
          it.copy(
            lastOperationMessage = "Gagal menyimpan bagseal: $err",
            isLastOperationError = true
          )
        }
      }
    }
  }

  fun deleteBagseal(bagseal: BagsealRecord) {
    viewModelScope.launch {
      repository.deleteBagseal(bagseal.id)
      SoundVibrationHelper.vibrate(getApplication(), 50)
      _uiState.update {
        it.copy(
          lastOperationMessage = "Bagseal ${bagseal.code} dihapus dari staging ${bagseal.stagingDestination}",
          isLastOperationError = false
        )
      }
    }
  }

  /**
   * The "Ikat" (Bind/Lock) Action:
   * Bound data moves smoothly to History with NO automatic popups!
   */
  fun performIkat() {
    val state = _uiState.value
    val seals = state.activeBagseals
    if (seals.isEmpty()) {
      SoundVibrationHelper.playScanError(getApplication())
      _uiState.update {
        it.copy(
          lastOperationMessage = "Belum ada bagseal yang di-scan untuk diikat!",
          isLastOperationError = true
        )
      }
      return
    }

    val sealCodes = seals.map { it.code }
    val newBatch = StagingBatch(
      tripNumber = state.selectedTrip,
      operationalDate = state.operationalDate,
      stagingDestination = state.selectedDestination,
      targetQty = state.targetQty,
      scannedCount = sealCodes.size,
      bagseals = sealCodes,
      status = "bound",
      boundAtMillis = System.currentTimeMillis()
    )

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      val createBatchResult = repository.createOrUpdateBatch(newBatch)
      if (createBatchResult.isSuccess) {
        val batchId = createBatchResult.getOrThrow()
        // Update all individual bagseals as bound to this batch
        for (seal in seals) {
          repository.deleteBagseal(seal.id)
          val boundSeal = seal.copy(id = "", batchId = batchId, isBound = true)
          repository.addBagseal(boundSeal)
        }

        // Clean up staging target qty preference for completed batch
        val key = "qty_${state.selectedTrip}_${state.selectedDestination}"
        prefs.edit().remove(key).apply()

        SoundVibrationHelper.playBindSuccess(getApplication())

        // Reset scanning panel for next destination without any popups
        _uiState.update {
          it.copy(
            isLoading = false,
            targetQty = 0,
            isQtyConfirmed = false,
            activeBagseals = emptyList(),
            lastOperationMessage = "✓ Berhasil Ikat ${sealCodes.size} Bagseal ke ${state.selectedDestination}!",
            isLastOperationError = false
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isLoading = false,
            lastOperationMessage = "Gagal mengikat batch: ${createBatchResult.exceptionOrNull()?.localizedMessage}",
            isLastOperationError = true
          )
        }
        SoundVibrationHelper.playScanError(getApplication())
      }
    }
  }

  fun setCameraScannerOpen(isOpen: Boolean) {
    _uiState.update { it.copy(isCameraScannerOpen = isOpen) }
  }

  fun setEnlargedBarcode(item: BagsealRecord?) {
    _uiState.update { it.copy(enlargedBarcodeItem = item) }
  }

  fun setVersionDialogVisible(isVisible: Boolean) {
    _uiState.update { it.copy(isVersionDialogVisible = isVisible) }
  }

  fun clearOperationMessage() {
    _uiState.update { it.copy(lastOperationMessage = null) }
  }
}
