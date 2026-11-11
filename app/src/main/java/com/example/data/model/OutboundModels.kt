package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserProfile(
  val id: String = "",
  val userId: String = "",
  val email: String = "",
  val displayName: String = "",
  val currentTrip: Int = 1
)

data class StagingBatch(
  val id: String = "",
  val userId: String = "local_operator",
  val tripNumber: Int = 1,
  val operationalDate: String = "",
  val stagingDestination: String = "",
  val targetQty: Int = 0,
  val scannedCount: Int = 0,
  val bagseals: List<String> = emptyList(),
  val status: String = "active", // "active" or "bound"
  val createdAt: Long = System.currentTimeMillis(),
  val boundAtMillis: Long? = null
) {
  fun getFormattedBoundTime(): String {
    val millis = boundAtMillis ?: return "-"
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
  }

  fun getFullFormattedBoundTime(): String {
    val millis = boundAtMillis ?: return "-"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(millis))
  }
}

data class BagsealRecord(
  val id: String = "",
  val userId: String = "local_operator",
  val code: String = "",
  val stagingDestination: String = "",
  val tripNumber: Int = 1,
  val operationalDate: String = "",
  val tag: String = "NONE", // "REG", "HV", "DOC", "BP", "NONE"
  val batchId: String? = null,
  val isBound: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

data class TripSummary(
  val id: String = "",
  val tripNumber: Int = 1,
  val operationalDate: String = "",
  val status: String = "active", // "active", "completed", "pending"
  val totalBatches: Int = 0,
  val totalBagseals: Int = 0
)

data class AppVersionInfo(
  val latestVersionCode: Int = 1,
  val latestVersionName: String = "1.0.0",
  val minSupportedVersionCode: Int = 1,
  val releaseNotes: String = "",
  val downloadUrl: String = ""
)

enum class BagsealTag(val code: String, val label: String) {
  NONE("NONE", "No Tag"),
  REG("REG", "Regular (REG)"),
  HV("HV", "High Value (HV)"),
  DOC("DOC", "Document (DOC)"),
  BP("BP", "Big Parcel (BP)")
}

object StagingDestinationConstants {
  val DEFAULT_CODES = listOf(
    "TANB", "TBET", "KBYB", "JGKA", "CNER", "PSMG", "PREB", "CPYU",
    "KRAJ", "BJIN", "TJHL", "BGOT", "KASB", "BOGU", "CLKU", "SKMJ",
    "CBTO", "SDBR", "PDGD", "TLJU", "STIB", "TNGH", "BGSR", "JTSA",
    "TPOS", "KDUM", "SMJA", "PMAS", "PCRM", "CGIN", "CLBR", "MLYH",
    "SKMN", "SKB"
  )
}
