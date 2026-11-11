package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.BagsealRecord
import com.example.data.model.StagingBatch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OutboundRepositoryRuleTest {

  private lateinit var db: AppDatabase
  private lateinit var repository: OutboundRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repository = OutboundRepository(db.outboundDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun createBatch_succeedsAndRetrieves() = runBlocking {
    val batch = StagingBatch(
      id = "batch-1",
      tripNumber = 1,
      operationalDate = "2026-11-11",
      stagingDestination = "TANB",
      targetQty = 10,
      scannedCount = 0,
      status = "active"
    )

    val createResult = repository.createOrUpdateBatch(batch)
    assertTrue(createResult.isSuccess)

    val batches = repository.observeBatches().first()
    assertEquals(1, batches.size)
    assertEquals("TANB", batches[0].stagingDestination)
    assertEquals(1, batches[0].tripNumber)
  }

  @Test
  fun bindBatch_savesBoundState() = runBlocking {
    val batch = StagingBatch(
      id = "batch-2",
      tripNumber = 2,
      operationalDate = "2026-11-11",
      stagingDestination = "TBET",
      targetQty = 5,
      scannedCount = 2,
      bagseals = listOf("SEAL-001", "SEAL-002"),
      status = "bound",
      boundAtMillis = System.currentTimeMillis()
    )

    repository.createOrUpdateBatch(batch)
    val batches = repository.observeBatches().first()
    assertEquals(1, batches.size)
    assertEquals("bound", batches[0].status)
    assertEquals(2, batches[0].bagseals.size)
    assertTrue(batches[0].getFormattedBoundTime().isNotBlank())
  }

  @Test
  fun addBagseal_and_deleteBagseal_succeeds() = runBlocking {
    val bagseal = BagsealRecord(
      id = "seal-1",
      code = "SEAL-XYZ-123",
      stagingDestination = "KBYB",
      tripNumber = 2,
      operationalDate = "2026-11-11",
      tag = "HV",
      isBound = false
    )

    val result = repository.addBagseal(bagseal)
    assertTrue(result.isSuccess)

    val seals = repository.observeBagseals().first()
    assertEquals(1, seals.size)
    assertEquals("SEAL-XYZ-123", seals[0].code)
    assertEquals("HV", seals[0].tag)

    val deleteResult = repository.deleteBagseal("seal-1")
    assertTrue(deleteResult.isSuccess)

    val sealsAfter = repository.observeBagseals().first()
    assertTrue(sealsAfter.isEmpty())
  }

  @Test
  fun multiStaging_bagsealsIsolation() = runBlocking {
    repository.addBagseal(
      BagsealRecord(code = "SEAL-TANB-1", stagingDestination = "TANB", tripNumber = 1, operationalDate = "2026-11-11")
    )
    repository.addBagseal(
      BagsealRecord(code = "SEAL-TBET-1", stagingDestination = "TBET", tripNumber = 1, operationalDate = "2026-11-11")
    )

    val allSeals = repository.observeBagseals().first()
    assertEquals(2, allSeals.size)

    val tanbSeals = allSeals.filter { it.stagingDestination == "TANB" }
    assertEquals(1, tanbSeals.size)
    assertEquals("SEAL-TANB-1", tanbSeals[0].code)

    val tbetSeals = allSeals.filter { it.stagingDestination == "TBET" }
    assertEquals(1, tbetSeals.size)
    assertEquals("SEAL-TBET-1", tbetSeals[0].code)
  }
}
