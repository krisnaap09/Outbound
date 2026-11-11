package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.model.BagsealRecord
import com.example.data.model.StagingBatch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SheetsExporter {

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

  /**
   * Generates a CSV string representing all bound batches and bagseals.
   */
  fun generateCsv(batches: List<StagingBatch>, bagseals: List<BagsealRecord>): String {
    val sb = StringBuilder()
    // CSV Header
    sb.append("Trip,Tanggal Operasional,Tujuan Staging,Target Qty,Scanned Qty,Status,Waktu Ikat,Kode Bagseal,Tag\n")

    val bagsealMap = bagseals.groupBy { it.batchId ?: "" }

    for (batch in batches) {
      val boundTime = batch.getFullFormattedBoundTime()
      val seals = bagsealMap[batch.id] ?: emptyList()

      if (seals.isEmpty()) {
        sb.append("Trip ${batch.tripNumber},")
        sb.append("\"${batch.operationalDate}\",")
        sb.append("\"${batch.stagingDestination}\",")
        sb.append("${batch.targetQty},")
        sb.append("${batch.scannedCount},")
        sb.append("\"${batch.status}\",")
        sb.append("\"$boundTime\",")
        sb.append("-\n")
      } else {
        for (seal in seals) {
          sb.append("Trip ${batch.tripNumber},")
          sb.append("\"${batch.operationalDate}\",")
          sb.append("\"${batch.stagingDestination}\",")
          sb.append("${batch.targetQty},")
          sb.append("${batch.scannedCount},")
          sb.append("\"${batch.status}\",")
          sb.append("\"$boundTime\",")
          sb.append("\"${seal.code}\",")
          sb.append("\"${seal.tag}\"\n")
        }
      }
    }
    return sb.toString()
  }

  /**
   * Generates a TSV (Tab Separated Values) string for direct 1-tap paste into Google Sheets.
   */
  fun generateTsv(batches: List<StagingBatch>, bagseals: List<BagsealRecord>): String {
    val sb = StringBuilder()
    sb.append("Trip\tTanggal Operasional\tTujuan Staging\tTarget Qty\tScanned Qty\tStatus\tWaktu Ikat\tKode Bagseal\tTag\n")

    val bagsealMap = bagseals.groupBy { it.batchId ?: "" }

    for (batch in batches) {
      val boundTime = batch.getFullFormattedBoundTime()
      val seals = bagsealMap[batch.id] ?: emptyList()

      if (seals.isEmpty()) {
        sb.append("Trip ${batch.tripNumber}\t${batch.operationalDate}\t${batch.stagingDestination}\t${batch.targetQty}\t${batch.scannedCount}\t${batch.status}\t$boundTime\t-\t-\n")
      } else {
        for (seal in seals) {
          sb.append("Trip ${batch.tripNumber}\t${batch.operationalDate}\t${batch.stagingDestination}\t${batch.targetQty}\t${batch.scannedCount}\t${batch.status}\t$boundTime\t${seal.code}\t${seal.tag}\n")
        }
      }
    }
    return sb.toString()
  }

  fun copyToClipboardForGoogleSheets(context: Context, batches: List<StagingBatch>, bagseals: List<BagsealRecord>) {
    val tsv = generateTsv(batches, bagseals)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Outbound Google Sheets Data", tsv)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Data disalin! Buka Google Sheets & Paste langsung.", Toast.LENGTH_LONG).show()
  }

  fun shareToGoogleSheetsOrExcel(context: Context, batches: List<StagingBatch>, bagseals: List<BagsealRecord>) {
    val csv = generateCsv(batches, bagseals)
    val sendIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, csv)
      putExtra(Intent.EXTRA_SUBJECT, "Outbound Manual Reguler - Export ${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}")
      type = "text/csv"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Ekspor Data ke Google Sheets / Excel")
    context.startActivity(shareIntent)
  }
}
