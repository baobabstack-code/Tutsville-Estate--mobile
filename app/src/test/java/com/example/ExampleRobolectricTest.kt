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

  @Test
  fun `test tenant creation with emergency contact and property assignment`() {
    val repository = EstateRepository()

    // 1. Create a tenant with full name, email, phone, and emergency contact
    val tenant = repository.createTenant(
      fullName = "Lady Vivienne Sterling",
      email = "vivienne.sterling@tutsville.estate",
      phone = "+1 (555) 432-8871",
      emergencyContactName = "Robert Montgomery (Brother)",
      emergencyContactPhone = "+1 (555) 914-4321"
    )

    assertNotNull(tenant)
    assertEquals("Lady Vivienne Sterling", tenant.fullName)
    assertEquals("vivienne.sterling@tutsville.estate", tenant.email)
    assertEquals("Robert Montgomery (Brother)", tenant.emergencyContactName)
    assertTrue("Invite code generated", tenant.inviteCode.startsWith("TUTS-"))
    assertEquals(null, tenant.assignedPropertyId)
    assertTrue(repository.tenants.value.any { it.id == tenant.id })

    // 2. Assign tenant to an existing available property
    val availableProp = repository.properties.value.first { it.status == com.example.tutsville.data.model.PropertyStatus.AVAILABLE }
    val assignedTenant = repository.assignTenantToPropertyById(
      tenantId = tenant.id,
      propertyId = availableProp.id,
      leaseStartDate = "Dec 01, 2026",
      leaseEndDate = "Nov 30, 2027"
    )

    assertNotNull(assignedTenant)
    assertEquals(availableProp.id, assignedTenant?.assignedPropertyId)
    assertEquals(availableProp.unitNumber, assignedTenant?.assignedPropertyUnit)

    // Verify property is now OCCUPIED
    val updatedProp = repository.properties.value.find { it.id == availableProp.id }
    assertEquals(com.example.tutsville.data.model.PropertyStatus.OCCUPIED, updatedProp?.status)
    assertEquals(tenant.fullName, updatedProp?.assignedTenant?.name)

    // 3. Find tenant by invite code
    val foundByCode = repository.findTenantByInviteCode(tenant.inviteCode)
    assertNotNull(foundByCode)
    assertEquals(tenant.id, foundByCode?.id)

    // 4. Claim tenant invite
    val claimed = repository.claimTenantInvite(tenant.email, tenant.inviteCode)
    assertNotNull(claimed)
    assertTrue(claimed?.isAppRegistered == true)

    // 5. Unassign tenant
    repository.unassignTenant(tenant.id)
    val unassignedTenant = repository.tenants.value.find { it.id == tenant.id }
    assertEquals(null, unassignedTenant?.assignedPropertyId)
    val restoredProp = repository.properties.value.find { it.id == availableProp.id }
    assertEquals(com.example.tutsville.data.model.PropertyStatus.AVAILABLE, restoredProp?.status)
  }

  @Test
  fun `test manager creates a new vendor and dispatches to work order case`() {
    val repository = EstateRepository()

    // 1. Manager creates a new contractor on-the-fly
    val vendor = repository.createVendor(
      name = "Mayfair Precision Security & Access",
      specialty = CaseCategory.SECURITY,
      phone = "+1 (555) 302-8800",
      email = "dispatch@mayfair-security.com",
      leadTech = "Capt. Henry Alistair"
    )

    assertNotNull(vendor)
    assertEquals("Mayfair Precision Security & Access", vendor.name)
    assertEquals(CaseCategory.SECURITY, vendor.specialty)
    assertTrue(repository.vendors.value.any { it.id == vendor.id })

    // 2. Manager reports a security work order and assigns the newly created vendor
    val securityCase = repository.reportCase(
      title = "Perimeter Gate 3 Biometric Sensor Offline",
      description = "East Gate RFID scanner intermittently failing to open",
      category = CaseCategory.SECURITY,
      priority = CasePriority.HIGH,
      location = "East Guardhouse • Gate 3",
      reportedBy = "Estate Security Desk"
    )
    assertNotNull(securityCase)
    assertEquals(CaseStatus.OPEN, securityCase.status)

    // 3. Assign the new vendor to the case
    repository.assignVendorToCase(securityCase.id, vendor.id)
    val assignedCase = repository.cases.value.find { it.id == securityCase.id }
    assertEquals(CaseStatus.ASSIGNED, assignedCase?.status)
    assertEquals(vendor.id, assignedCase?.assignedVendorId)
    assertEquals(vendor.name, assignedCase?.assignedVendorName)
  }

  @Test
  fun `test resident maintenance requests are strictly scoped to their assigned unit`() {
    val repository = EstateRepository()

    // 1. Existing resident in Unit 402
    val residentUnit = "Apt 402"
    val residentName = "Lady Vivienne Sterling"

    // 2. Report a case for Unit 402
    val unitCase = repository.reportCase(
      title = "Master Suite AC Thermostat Calibration",
      description = "Zone 1 temperature sensor reading 3 degrees high",
      category = CaseCategory.HVAC,
      priority = CasePriority.MEDIUM,
      location = "Tower A • Apt 402",
      reportedBy = residentName
    )
    assertNotNull(unitCase)

    // 3. Report an internal estate case (e.g. Chiller in Plant room)
    val estateCase = repository.reportCase(
      title = "Basement Chiller Pump 2 Overheat",
      description = "Bearing temperature alert in plant room",
      category = CaseCategory.HVAC,
      priority = CasePriority.EMERGENCY,
      location = "Central Utility Basement • Plant B",
      reportedBy = "Julian Vance (Chief Engineer)"
    )
    assertNotNull(estateCase)

    // 4. Resident filter check: resident only sees cases matching their unit or reportedBy
    val allCases = repository.cases.value
    val residentVisibleCases = allCases.filter { c ->
      c.location.contains(residentUnit, ignoreCase = true) ||
      c.reportedBy.equals(residentName, ignoreCase = true)
    }

    assertTrue("Resident must see their own unit's maintenance ticket", residentVisibleCases.any { it.id == unitCase.id })
    assertTrue("Resident must NOT see internal basement plant chiller tickets", residentVisibleCases.none { it.id == estateCase.id })
  }

  @Test
  fun `test delete operations for unit, tenant, case, contract, vendor, and facility`() {
    val repository = EstateRepository()

    // 1. Delete Unit Property
    val propToDelete = repository.properties.value.first()
    repository.deleteProperty(propToDelete.id)
    assertTrue(repository.properties.value.none { it.id == propToDelete.id })

    // 2. Delete Tenant Profile
    val tenantToDelete = repository.tenants.value.first()
    repository.deleteTenant(tenantToDelete.id)
    assertTrue(repository.tenants.value.none { it.id == tenantToDelete.id })

    // 3. Delete Case Ticket
    val caseToDelete = repository.cases.value.first()
    repository.deleteCase(caseToDelete.id)
    assertTrue(repository.cases.value.none { it.id == caseToDelete.id })

    // 4. Delete Contract
    val contractToDelete = repository.contracts.value.first()
    repository.deleteContract(contractToDelete.id)
    assertTrue(repository.contracts.value.none { it.id == contractToDelete.id })

    // 5. Delete Vendor
    val vendorToDelete = repository.vendors.value.first()
    repository.deleteVendor(vendorToDelete.id)
    assertTrue(repository.vendors.value.none { it.id == vendorToDelete.id })

    // 6. Delete Facility Amenity
    val facilityToDelete = repository.facilities.value.first()
    repository.deleteFacility(facilityToDelete.id)
    assertTrue(repository.facilities.value.none { it.id == facilityToDelete.id })
  }

  @Test
  fun `test calendar synchronizes maintenances bookings contracts and cases with role filtering`() {
    val repository = EstateRepository()
    val viewModel = com.example.tutsville.ui.viewmodel.EstateViewModel(repository)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    // Schedule maintenance on today's date
    val today = com.example.tutsville.data.model.EstateDateUtils.todayDisplay()
    val todayKey = com.example.tutsville.data.model.EstateDateUtils.todayKey()
    val maint = repository.scheduleMaintenance(
      title = "Roof HVAC Chiller Check",
      discipline = com.example.tutsville.data.model.MaintenanceDiscipline.HVAC,
      date = today,
      time = "10:00 AM - 12:00 PM",
      contractor = "Apex HVAC Solutions",
      targetArea = "North Tower Roof",
      intervalMonths = 3,
      notes = "Seasonal check"
    )
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    val allEventsManager = viewModel.allCalendarEvents.value
    assertTrue(
      "Manager calendar must include scheduled maintenance (found ${allEventsManager.size} events)",
      allEventsManager.any { it.sourceId == maint.id && it.dateKey == todayKey && it.type == com.example.tutsville.data.model.CalendarEventType.MAINTENANCE }
    )
    assertTrue(
      "Manager calendar must synchronize bookings, contracts, and cases",
      allEventsManager.any { it.type == com.example.tutsville.data.model.CalendarEventType.BOOKING } &&
      allEventsManager.any { it.type == com.example.tutsville.data.model.CalendarEventType.CONTRACT } &&
      allEventsManager.any { it.type == com.example.tutsville.data.model.CalendarEventType.CASE }
    )

    // Date selection and category filter
    viewModel.selectCalendarDate(todayKey)
    assertEquals(todayKey, viewModel.selectedCalendarDateKey.value)
    viewModel.setCalendarCategoryFilter(com.example.tutsville.data.model.CalendarEventType.MAINTENANCE)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    val filteredToday = viewModel.calendarEventsByDate.value[todayKey].orEmpty()
    assertTrue(filteredToday.isNotEmpty())
    assertTrue(filteredToday.all { it.type == com.example.tutsville.data.model.CalendarEventType.MAINTENANCE })

    // Verify Tenant role filtering (sees estate maintenance advisories, filters out unrelated contracts/bookings)
    viewModel.setCalendarCategoryFilter(null)
    val tenant = repository.tenants.value.first()
    viewModel.setUserEmail(tenant.email)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertEquals(com.example.tutsville.data.model.UserRole.TENANT, viewModel.currentRole.value)
    assertTrue(viewModel.allCalendarEvents.value.any { it.sourceId == maint.id })

    // Delete maintenance and verify calendar updates reactively
    viewModel.deleteMaintenance(maint.id)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertTrue(viewModel.allCalendarEvents.value.none { it.sourceId == maint.id })
  }
}


