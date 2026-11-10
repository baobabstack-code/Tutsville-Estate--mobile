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
@Config(sdk = [34])
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

  @Test
  fun `test dashboard summaries for maintenances cases and facility bookings`() {
    val repository = EstateRepository()

    // 1. Upcoming maintenance tasks summary
    val pendingMaintenances = repository.maintenances.value.filter { it.status != com.example.tutsville.data.model.MaintenanceStatus.COMPLETED }
    assertTrue("Dashboard should have upcoming scheduled maintenance tasks", pendingMaintenances.isNotEmpty())

    // 2. Active cases summary
    val activeCases = repository.cases.value.filter { it.status != CaseStatus.RESOLVED }
    assertTrue("Dashboard should have active reported or in-progress incident cases", activeCases.isNotEmpty())

    // 3. Facility booking statuses summary
    val confirmedBookings = repository.bookings.value
    val estateFacilities = repository.facilities.value
    assertTrue("Dashboard should display active facility bookings", confirmedBookings.isNotEmpty())
    assertTrue("Dashboard should display estate amenities", estateFacilities.isNotEmpty())
  }

  @Test
  fun `test property listing with architecture parking and amenities and tenant assignment with invite link`() {
    val repository = EstateRepository()

    // 1. Manager lists a new property
    val arch = com.example.tutsville.data.model.PropertyArchitecture(
      bedrooms = 4,
      bathrooms = 4.0f,
      sqFt = 3800,
      floorLevel = "Upper Terraces • Level 20",
      viewType = "Sunset Overlook & Forest Pavilion"
    )
    val parking = listOf("Bay T-01 (EV Charging)", "Bay T-02")
    val amenities = listOf("Private Heated Plunge Pool", "Sommelier Cellar", "Bang & Olufsen Smart Automation")

    val listedProp = repository.listProperty(
      unitNumber = "Villa 701 • Rosewood Pavilion",
      pavilionWing = "North Tower Terraces",
      architecture = arch,
      parkingSpots = parking,
      amenities = amenities,
      monthlyRental = "$13,800 / mo"
    )

    assertNotNull(listedProp)
    assertEquals(com.example.tutsville.data.model.PropertyStatus.AVAILABLE, listedProp.status)
    assertEquals(4, listedProp.architecture.bedrooms)
    assertTrue(listedProp.parkingSpots.any { it.contains("EV Charging") })
    assertTrue(listedProp.amenities.contains("Private Heated Plunge Pool"))

    // 2. Manager assigns property to tenant
    val tenantEmail = "arthur.pendelton@tutsville.com"
    val tenantInfo = repository.assignPropertyToTenant(
      propertyId = listedProp.id,
      tenantName = "Sir Arthur Pendelton",
      tenantEmail = tenantEmail,
      leaseStartDate = "Dec 01, 2026",
      leaseEndDate = "Nov 30, 2027"
    )

    assertNotNull(tenantInfo)
    assertTrue("Invite link must contain custom scheme and property ID", tenantInfo.inviteLink.startsWith("tutsville://onboard"))
    assertTrue("Invite link must contain tenant email", tenantInfo.inviteLink.contains(tenantEmail))
    assertTrue("Invite code should be generated", tenantInfo.inviteCode.startsWith("TUTS-"))

    // Verify property status in repository is now OCCUPIED
    val updatedProp = repository.properties.value.find { it.id == listedProp.id }
    assertEquals(com.example.tutsville.data.model.PropertyStatus.OCCUPIED, updatedProp?.status)
    assertEquals("Sir Arthur Pendelton", updatedProp?.assignedTenant?.name)

    // 3. Tenant authenticates and claims assigned property by matching email
    val claimedProp = repository.claimPropertyInvite(tenantEmail)
    assertNotNull(claimedProp)
    assertEquals(listedProp.id, claimedProp?.id)
    assertTrue(claimedProp?.assignedTenant?.isClaimed == true)

    // 4. Vacate property
    repository.vacateProperty(listedProp.id)
    val vacatedProp = repository.properties.value.find { it.id == listedProp.id }
    assertEquals(com.example.tutsville.data.model.PropertyStatus.AVAILABLE, vacatedProp?.status)
    assertEquals(null, vacatedProp?.assignedTenant)
  }
}


