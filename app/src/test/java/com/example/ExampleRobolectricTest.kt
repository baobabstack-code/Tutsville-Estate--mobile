package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.CasePriority
import com.example.tutsville.data.model.CaseStatus
import com.example.tutsville.data.repository.EstateRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Tutsville Estate", appName)
  }

  @Test
  fun `test case reporting and vendor assignment workflow`() {
    val repository = EstateRepository()
    
    // 1. Report Case
    val newCase = repository.reportCase(
      title = "HVAC Condenser Noise",
      description = "Vibration in north plant",
      category = CaseCategory.HVAC,
      priority = CasePriority.HIGH,
      location = "North Tower Plant",
      reportedBy = "Resident Test"
    )
    assertNotNull(newCase)
    assertEquals(CaseStatus.OPEN, newCase.status)

    // 2. Assign Vendor
    repository.assignVendorToCase(newCase.id, "VEND-01")
    val updated = repository.cases.value.find { it.id == newCase.id }
    assertEquals(CaseStatus.ASSIGNED, updated?.status)
    assertEquals("Apex HVAC Solutions", updated?.assignedVendorName)

    // 3. Review Service
    repository.submitCaseReview(newCase.id, 5, listOf("Punctual"), "Great work")
    val resolved = repository.cases.value.find { it.id == newCase.id }
    assertEquals(CaseStatus.RESOLVED, resolved?.status)
    assertEquals(5, resolved?.rating)
  }

  @Test
  fun `test facility instant booking and contract rundown notification`() {
    val repository = EstateRepository()

    // Facility Booking
    val booking = repository.bookFacility(
      facilityId = "FAC-01",
      date = "Nov 15, 2026",
      timeSlot = "10:00 AM - 02:00 PM",
      residentName = "Lord Tutsville",
      unit = "Penthouse A",
      guestCount = 12,
      notes = "VIP Gathering"
    )
    assertNotNull(booking)
    assertTrue(repository.bookings.value.any { it.id == booking.id })

    // Contract Rundown Notification
    val expiringContract = repository.contracts.value.first { it.daysRemaining <= 14 }
    repository.sendContractRundownNotification(expiringContract.id)
    val notifiedContract = repository.contracts.value.find { it.id == expiringContract.id }
    assertTrue(notifiedContract?.isNotificationSent == true)
  }
}
