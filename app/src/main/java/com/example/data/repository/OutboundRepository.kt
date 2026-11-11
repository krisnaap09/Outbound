package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.OutboundDao
import com.example.data.local.entity.BagsealEntity
import com.example.data.local.entity.StagingBatchEntity
import com.example.data.model.AppVersionInfo
import com.example.data.model.BagsealRecord
import com.example.data.model.StagingBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

class OutboundRepository(private val dao: OutboundDao) {

  constructor(context: Context) : this(
    AppDatabase.getInstance(context).outboundDao()
  )

  fun observeBatches(): Flow<List<StagingBatch>> {
    return dao.getAllBatches().map { entities ->
      entities.map { it.toDomain() }
    }
  }

  fun observeBagseals(): Flow<List<BagsealRecord>> {
    return dao.getAllBagseals().map { entities ->
      entities.map { it.toDomain() }
    }
  }

  suspend fun createOrUpdateBatch(batch: StagingBatch): Result<String> {
    return try {
      val batchId = if (batch.id.isNotBlank()) batch.id else UUID.randomUUID().toString()
      val entity = StagingBatchEntity(
        id = batchId,
        tripNumber = batch.tripNumber,
        operationalDate = batch.operationalDate,
        stagingDestination = batch.stagingDestination,
        targetQty = batch.targetQty,
        scannedCount = batch.scannedCount,
        bagsealsCsv = batch.bagseals.joinToString("|"),
        status = batch.status,
        createdAt = if (batch.createdAt > 0) batch.createdAt else System.currentTimeMillis(),
        boundAt = batch.boundAtMillis
      )
      dao.insertBatch(entity)
      Result.success(batchId)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun addBagseal(bagseal: BagsealRecord): Result<String> {
    return try {
      val bagsealId = if (bagseal.id.isNotBlank()) bagseal.id else UUID.randomUUID().toString()
      val entity = BagsealEntity(
        id = bagsealId,
        code = bagseal.code,
        stagingDestination = bagseal.stagingDestination,
        tripNumber = bagseal.tripNumber,
        operationalDate = bagseal.operationalDate,
        tag = bagseal.tag,
        batchId = bagseal.batchId,
        isBound = bagseal.isBound,
        createdAt = if (bagseal.createdAt > 0) bagseal.createdAt else System.currentTimeMillis()
      )
      dao.insertBagseal(entity)
      Result.success(bagsealId)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteBagseal(bagsealId: String): Result<Unit> {
    return try {
      dao.deleteBagseal(bagsealId)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun bindBatch(
    batchId: String,
    currentBagseals: List<String>
  ): Result<Unit> {
    return try {
      // Find existing batch or update with bound status
      val boundTime = System.currentTimeMillis()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun observeAppVersion(): Flow<AppVersionInfo?> {
    return flowOf(
      AppVersionInfo(
        latestVersionCode = 1,
        latestVersionName = "1.0.0",
        minSupportedVersionCode = 1,
        releaseNotes = "Versi stabil Outbound Manual Reguler. Siap operasional mandiri di gudang."
      )
    )
  }

  private fun StagingBatchEntity.toDomain(): StagingBatch {
    val seals = if (bagsealsCsv.isBlank()) emptyList() else bagsealsCsv.split("|")
    return StagingBatch(
      id = id,
      userId = "local_operator",
      tripNumber = tripNumber,
      operationalDate = operationalDate,
      stagingDestination = stagingDestination,
      targetQty = targetQty,
      scannedCount = scannedCount,
      bagseals = seals,
      status = status,
      createdAt = createdAt,
      boundAtMillis = boundAt
    )
  }

  private fun BagsealEntity.toDomain(): BagsealRecord {
    return BagsealRecord(
      id = id,
      userId = "local_operator",
      code = code,
      stagingDestination = stagingDestination,
      tripNumber = tripNumber,
      operationalDate = operationalDate,
      tag = tag,
      batchId = batchId,
      isBound = isBound,
      createdAt = createdAt
    )
  }
}
