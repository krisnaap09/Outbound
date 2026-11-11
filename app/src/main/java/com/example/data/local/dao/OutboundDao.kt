package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BagsealEntity
import com.example.data.local.entity.StagingBatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboundDao {

  @Query("SELECT * FROM staging_batches ORDER BY createdAt DESC")
  fun getAllBatches(): Flow<List<StagingBatchEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBatch(batch: StagingBatchEntity)

  @Update
  suspend fun updateBatch(batch: StagingBatchEntity)

  @Query("DELETE FROM staging_batches WHERE id = :batchId")
  suspend fun deleteBatch(batchId: String)

  @Query("SELECT * FROM bagseals ORDER BY createdAt ASC")
  fun getAllBagseals(): Flow<List<BagsealEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBagseal(bagseal: BagsealEntity)

  @Query("DELETE FROM bagseals WHERE id = :bagsealId")
  suspend fun deleteBagseal(bagsealId: String)

  @Query("UPDATE bagseals SET isBound = 1, batchId = :batchId WHERE id IN (:bagsealIds)")
  suspend fun markBagsealsBound(bagsealIds: List<String>, batchId: String)
}
