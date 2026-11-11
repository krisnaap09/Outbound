package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staging_batches")
data class StagingBatchEntity(
  @PrimaryKey val id: String,
  val tripNumber: Int,
  val operationalDate: String,
  val stagingDestination: String,
  val targetQty: Int,
  val scannedCount: Int,
  val bagsealsCsv: String = "", // Comma-separated bagseal codes
  val status: String = "active", // "active" or "bound"
  val createdAt: Long = System.currentTimeMillis(),
  val boundAt: Long? = null
)

@Entity(tableName = "bagseals")
data class BagsealEntity(
  @PrimaryKey val id: String,
  val code: String,
  val stagingDestination: String,
  val tripNumber: Int,
  val operationalDate: String,
  val tag: String = "NONE", // "REG", "HV", "DOC", "BP", "NONE"
  val batchId: String? = null,
  val isBound: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)
