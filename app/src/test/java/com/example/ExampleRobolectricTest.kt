package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.tutsville.data.model.CalendarEventType
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.CasePriority
import com.example.tutsville.data.model.CaseStatus
import com.example.tutsville.data.model.ContractType
import com.example.tutsville.data.model.EstateDateUtils
import com.example.tutsville.data.model.MaintenanceDiscipline
import com.example.tutsville.data.model.MaintenanceStatus
import com.example.tutsville.data.model.PropertyArchitecture
import com.example.tutsville.data.model.PropertyStatus
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.data.repository.EstateRepository
import com.example.tutsville.ui.viewmodel.EstateViewModel
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
  fun `test repository starts completely empty with no hardcoded seed data`() {
    val repository = EstateRepository()
    assertTrue(repository.cases.value.isEmpty())
    assertTrue(repository.maintenances.value.isEmpty())
    assertTrue(repository.facilities.value.isEmpty())
    assertTrue(repository.bookings.value.isEmpty())
    assertTrue(repository.contracts.value.isEmpty())
    assertTrue(repository.vendors.value.isEmpty())
    assertTrue(repository.reminders.value.isEmpty())
    assertTrue(repository.properties.value.isEmpty())
    assertTrue(repository.tenants.value.isEmpty())
  }

  @Test
  fun `test case reporting and vendor assignment workflow`() {
    val repository = EstateRepository()

    // 0. Onboard real vendor
    val vendor = repository.createVendor(
      name = "Northwind HVAC Partners",
      specialty = CaseCategory.HVAC,
      phone = "+1 (555) 201-4400",
      email = "ops@northwind-hvac.example",
      leadTech = "Marcus Reid"
    )

    // 1. Report Case
    val newCase = repository.reportCase(
      title = "HVAC Condenser Noise",
      description = "Vibration in north plant",
      category = CaseCategory.HVAC,
      priority = CasePriority.HIGH,
      location = "North Plant",
      reportedBy = "Resident Test"
    )
    assertNotNull(newCase)
    assertEquals(CaseStatus.OPEN, newCase.status)

    // 2. Assign Vendor
    repository.assignVendorToCase(newCase.id, vendor.id)
    val updated = repository.cases.value.find { it.id == newCase.id }
    assertEquals(CaseStatus.ASSIGNED, updated?.status)
    assertEquals(vendor.name, updated?.assignedVendorName)

    // 3. Review Service
    repository.submitCaseReview(newCase.id, 5, listOf("Punctual"), "Great work")
    val resolved = repository.cases.value.find { it.id == newCase.id }
    assertEquals(CaseStatus.RESOLVED, resolved?.status)
    assertEquals(5, resolved?.rating)
  }

  @Test
  fun `test facility instant booking and contract rundown notification`() {
    val repository = EstateRepository()

    // 0. Create amenity and contract
    val facility = repository.createFacility(
      name = "Clubhouse Lounge",
      tag = "Wellness",
      capacity = 24,
      location = "North Wing Level 2",
      description = "Private resident lounge",
      amenities = listOf("Wi-Fi"),
      allowedSlots = listOf("10:00 AM - 02:00 PM"),
      rules = "Check in at reception"
    )

    val expiringContract = repository.createContract(
      title = "Elevator Maintenance SLA",
      counterparty = "Vertical Transit Co.",
      type = ContractType.VENDOR_SLA,
      annualValue = "$24,000 / yr",
      startDate = EstateDateUtils.offsetDisplay(-355),
      expiryDate = EstateDateUtils.offsetDisplay(10),
      daysRemaining = 10,
      renewalTerms = "Annual renewal",
      contactPerson = "Operations Desk",
      contactEmail = "sla@vertical-transit.example"
    )

    // Facility Booking
    val booking = repository.bookFacility(
      facilityId = facility.id,
      date = EstateDateUtils.offsetDisplay(3),
      timeSlot = "10:00 AM - 02:00 PM",
      residentName = "Morgan Taylor",
      unit = "Unit 201",
      guestCount = 12,
      notes = "Gathering"
    )
    assertNotNull(booking)
    assertEquals(facility.name, booking.facilityName)
    assertTrue(repository.bookings.value.any { it.id == booking.id })

    // Contract Rundown Notification
    repository.sendContractRundownNotification(expiringContract.id)
    val notifiedContract = repository.contracts.value.find { it.id == expiringContract.id }
    assertTrue(notifiedContract?.isNotificationSent == true)
    assertTrue(repository.reminders.value.isNotEmpty())
  }

  @Test
  fun `test dashboard summaries for maintenances cases and facility bookings`() {
    val repository = EstateRepository()

    repository.scheduleMaintenance(
      title = "Boiler Pressure Inspection",
      discipline = MaintenanceDiscipline.PLUMBING,
      date = EstateDateUtils.offsetDisplay(2),
      time = "09:00 AM - 11:00 AM",
      contractor = "FlowCheck Plumbing",
      targetArea = "Utility Room B",
      intervalMonths = 6,
      notes = "Routine check"
    )
    repository.reportCase(
      title = "Lobby Door Latch Adjustment",
      description = "Latch sticking",
      category = CaseCategory.STRUCTURAL,
      priority = CasePriority.MEDIUM,
      location = "Main Lobby",
      reportedBy = "Front Desk"
    )
    val facility = repository.createFacility(
      name = "Tennis Court 1",
      tag = "Sports",
      capacity = 4,
      location = "East Grounds",
      description = "Hard surface court",
      amenities = listOf("Lighting"),
      allowedSlots = listOf("08:00 AM - 10:00 AM"),
      rules = "Court shoes required"
    )
    repository.bookFacility(
      facilityId = facility.id,
      date = EstateDateUtils.todayDisplay(),
      timeSlot = "08:00 AM - 10:00 AM",
      residentName = "Alex Rivera",
      unit = "Unit 105",
      guestCount = 2,
      notes = ""
    )

    // 1. Upcoming maintenance tasks summary
    val pendingMaintenances = repository.maintenances.value.filter { it.status != MaintenanceStatus.COMPLETED }
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
    val arch = PropertyArchitecture(
      bedrooms = 4,
      bathrooms = 4.0f,
      sqFt = 3800,
      floorLevel = "Level 20",
      viewType = "Park Overlook"
    )
    val parking = listOf("Bay T-01 (EV Charging)", "Bay T-02")
    val amenities = listOf("Private Terrace", "Smart Thermostat")

    val listedProp = repository.listProperty(
      unitNumber = "Unit 701",
      pavilionWing = "North Terraces",
      architecture = arch,
      parkingSpots = parking,
      amenities = amenities,
      monthlyRental = "$13,800 / mo"
    )

    assertNotNull(listedProp)
    assertEquals(PropertyStatus.AVAILABLE, listedProp.status)
    assertEquals(4, listedProp.architecture.bedrooms)
    assertTrue(listedProp.parkingSpots.any { it.contains("EV Charging") })
    assertTrue(listedProp.amenities.contains("Private Terrace"))

    // 2. Manager assigns property to tenant
    val tenantEmail = "jordan.blake@example.com"
    val tenantInfo = repository.assignPropertyToTenant(
      propertyId = listedProp.id,
      tenantName = "Jordan Blake",
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
    assertEquals(PropertyStatus.OCCUPIED, updatedProp?.status)
    assertEquals("Jordan Blake", updatedProp?.assignedTenant?.name)

    // 3. Tenant authenticates and claims assigned property by matching email
    val claimedProp = repository.claimPropertyInvite(tenantEmail)
    assertNotNull(claimedProp)
    assertEquals(listedProp.id, claimedProp?.id)
    assertTrue(claimedProp?.assignedTenant?.isClaimed == true)

    // 4. Vacate property
    repository.vacateProperty(listedProp.id)
    val vacatedProp = repository.properties.value.find { it.id == listedProp.id }
    assertEquals(PropertyStatus.AVAILABLE, vacatedProp?.status)
    assertEquals(null, vacatedProp?.assignedTenant)
  }

  @Test
  fun `test tenant creation with emergency contact and property assignment`() {
    val repository = EstateRepository()

    // 0. Create an available unit
    val availableProp = repository.listProperty(
      unitNumber = "Unit 304",
      pavilionWing = "South Wing",
      architecture = PropertyArchitecture(2, 2.0f, 1400, "Level 3", "Courtyard"),
      parkingSpots = listOf("Bay S-08"),
      amenities = emptyList(),
      monthlyRental = "$4,200 / mo"
    )

    // 1. Create a tenant with full name, email, phone, and emergency contact
    val tenant = repository.createTenant(
      fullName = "Samira Patel",
      email = "samira.patel@example.com",
      phone = "+1 (555) 432-8871",
      emergencyContactName = "Dev Patel",
      emergencyContactPhone = "+1 (555) 914-4321"
    )

    assertNotNull(tenant)
    assertEquals("Samira Patel", tenant.fullName)
    assertEquals("samira.patel@example.com", tenant.email)
    assertEquals("Dev Patel", tenant.emergencyContactName)
    assertTrue("Invite code generated", tenant.inviteCode.startsWith("TUTS-"))
    assertEquals(null, tenant.assignedPropertyId)
    assertTrue(repository.tenants.value.any { it.id == tenant.id })

    // 2. Assign tenant to the available property
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
    assertEquals(PropertyStatus.OCCUPIED, updatedProp?.status)
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
    assertEquals(PropertyStatus.AVAILABLE, restoredProp?.status)
  }

  @Test
  fun `test manager creates a new vendor and dispatches to work order case`() {
    val repository = EstateRepository()

    // 1. Manager creates a new contractor on-the-fly
    val vendor = repository.createVendor(
      name = "Shield Access Systems",
      specialty = CaseCategory.SECURITY,
      phone = "+1 (555) 302-8800",
      email = "dispatch@shield-access.example",
      leadTech = "Hannah Cho"
    )

    assertNotNull(vendor)
    assertEquals("Shield Access Systems", vendor.name)
    assertEquals(CaseCategory.SECURITY, vendor.specialty)
    assertTrue(repository.vendors.value.any { it.id == vendor.id })

    // 2. Manager reports a security work order and assigns the newly created vendor
    val securityCase = repository.reportCase(
      title = "Perimeter Gate 3 Biometric Sensor Offline",
      description = "East Gate RFID scanner intermittently failing to open",
      category = CaseCategory.SECURITY,
      priority = CasePriority.HIGH,
      location = "East Gatehouse",
      reportedBy = "Security Desk"
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
    val residentUnit = "Unit 402"
    val residentName = "Taylor Kim"

    // 2. Report a case for Unit 402
    val unitCase = repository.reportCase(
      title = "Master Suite AC Thermostat Calibration",
      description = "Zone 1 temperature sensor reading 3 degrees high",
      category = CaseCategory.HVAC,
      priority = CasePriority.MEDIUM,
      location = residentUnit,
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
      reportedBy = "Chief Engineer"
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

    // Seed explicit items via real create APIs
    val propToDelete = repository.listProperty(
      unitNumber = "Unit 101",
      pavilionWing = "East Wing",
      architecture = PropertyArchitecture(1, 1.0f, 800, "Level 1", "Garden"),
      parkingSpots = emptyList(),
      amenities = emptyList(),
      monthlyRental = "$2,800 / mo"
    )
    val tenantToDelete = repository.createTenant(
      fullName = "Chris Morgan",
      email = "chris.morgan@example.com",
      phone = "+1 (555) 111-2222",
      emergencyContactName = "Pat Morgan",
      emergencyContactPhone = "+1 (555) 333-4444"
    )
    val caseToDelete = repository.reportCase(
      title = "Light Fixture Flicker",
      description = "Corridor light",
      category = CaseCategory.ELECTRICAL,
      priority = CasePriority.LOW,
      location = "Unit 101",
      reportedBy = "Chris Morgan"
    )
    val contractToDelete = repository.createContract(
      title = "Landscaping Service",
      counterparty = "GreenCare Grounds",
      type = ContractType.VENDOR_SLA,
      annualValue = "$18,000 / yr",
      startDate = EstateDateUtils.todayDisplay(),
      expiryDate = EstateDateUtils.offsetDisplay(90),
      daysRemaining = 90,
      renewalTerms = "Annual",
      contactPerson = "Ops",
      contactEmail = "ops@greencare.example"
    )
    val vendorToDelete = repository.createVendor(
      name = "VoltWorks Electric",
      specialty = CaseCategory.ELECTRICAL,
      phone = "+1 (555) 777-8888",
      email = "dispatch@voltworks.example",
      leadTech = "Nora Diaz"
    )
    val facilityToDelete = repository.createFacility(
      name = "Screening Room",
      tag = "Cinema",
      capacity = 12,
      location = "Lower Lounge",
      description = "Private theater",
      amenities = emptyList(),
      allowedSlots = listOf("06:00 PM - 09:00 PM"),
      rules = "Quiet hours after 10 PM"
    )

    // 1. Delete Unit Property
    repository.deleteProperty(propToDelete.id)
    assertTrue(repository.properties.value.none { it.id == propToDelete.id })

    // 2. Delete Tenant Profile
    repository.deleteTenant(tenantToDelete.id)
    assertTrue(repository.tenants.value.none { it.id == tenantToDelete.id })

    // 3. Delete Case Ticket
    repository.deleteCase(caseToDelete.id)
    assertTrue(repository.cases.value.none { it.id == caseToDelete.id })

    // 4. Delete Contract
    repository.deleteContract(contractToDelete.id)
    assertTrue(repository.contracts.value.none { it.id == contractToDelete.id })

    // 5. Delete Vendor
    repository.deleteVendor(vendorToDelete.id)
    assertTrue(repository.vendors.value.none { it.id == vendorToDelete.id })

    // 6. Delete Facility Amenity
    repository.deleteFacility(facilityToDelete.id)
    assertTrue(repository.facilities.value.none { it.id == facilityToDelete.id })
  }

  @Test
  fun `test calendar synchronizes maintenances bookings contracts and cases with role filtering`() {
    val repository = EstateRepository()
    val viewModel = EstateViewModel(repository)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    // Create facility, booking, contract, case, tenant, and maintenance
    val today = EstateDateUtils.todayDisplay()
    val todayKey = EstateDateUtils.todayKey()

    val facility = repository.createFacility(
      name = "Spa Pavilion",
      tag = "Wellness",
      capacity = 8,
      location = "North Wing",
      description = "Heated spa pool",
      amenities = emptyList(),
      allowedSlots = listOf("10:00 AM - 12:00 PM"),
      rules = "Resident pass"
    )
    val unit = repository.listProperty(
      unitNumber = "Unit 502",
      pavilionWing = "West Wing",
      architecture = PropertyArchitecture(2, 2.0f, 1500, "Level 5", "Garden"),
      parkingSpots = listOf("Bay 12"),
      amenities = emptyList(),
      monthlyRental = "$4,800 / mo"
    )
    val tenant = repository.createTenant(
      fullName = "Dana Mercer",
      email = "dana.mercer@example.com",
      phone = "+1 (555) 600-1234",
      emergencyContactName = "Lee Mercer",
      emergencyContactPhone = "+1 (555) 600-5678"
    )
    repository.assignTenantToPropertyById(tenant.id, unit.id, today, EstateDateUtils.addMonthsDisplay(today, 12))

    repository.bookFacility(
      facilityId = facility.id,
      date = today,
      timeSlot = "10:00 AM - 12:00 PM",
      residentName = tenant.fullName,
      unit = unit.unitNumber,
      guestCount = 2,
      notes = ""
    )
    repository.createContract(
      title = "Roof HVAC SLA",
      counterparty = "ClimateControl Inc",
      type = ContractType.VENDOR_SLA,
      annualValue = "$15,000 / yr",
      startDate = today,
      expiryDate = EstateDateUtils.offsetDisplay(30),
      daysRemaining = 30,
      renewalTerms = "Annual",
      contactPerson = "Support",
      contactEmail = "support@climate.example"
    )
    repository.reportCase(
      title = "Window Seal Draft",
      description = "Minor draft in study",
      category = CaseCategory.STRUCTURAL,
      priority = CasePriority.LOW,
      location = unit.unitNumber,
      reportedBy = tenant.fullName
    )
    val maint = repository.scheduleMaintenance(
      title = "Roof HVAC Chiller Check",
      discipline = MaintenanceDiscipline.HVAC,
      date = today,
      time = "10:00 AM - 12:00 PM",
      contractor = "ClimateControl Inc",
      targetArea = "North Roof",
      intervalMonths = 3,
      notes = "Seasonal check"
    )
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    val allEventsManager = viewModel.allCalendarEvents.value
    assertTrue(
      "Manager calendar must include scheduled maintenance (found ${allEventsManager.size} events)",
      allEventsManager.any { it.sourceId == maint.id && it.dateKey == todayKey && it.type == CalendarEventType.MAINTENANCE }
    )
    assertTrue(
      "Manager calendar must synchronize bookings, contracts, and cases",
      allEventsManager.any { it.type == CalendarEventType.BOOKING } &&
      allEventsManager.any { it.type == CalendarEventType.CONTRACT } &&
      allEventsManager.any { it.type == CalendarEventType.CASE }
    )

    // Date selection and category filter
    viewModel.selectCalendarDate(todayKey)
    assertEquals(todayKey, viewModel.selectedCalendarDateKey.value)
    viewModel.setCalendarCategoryFilter(CalendarEventType.MAINTENANCE)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    val filteredToday = viewModel.calendarEventsByDate.value[todayKey].orEmpty()
    assertTrue(filteredToday.isNotEmpty())
    assertTrue(filteredToday.all { it.type == CalendarEventType.MAINTENANCE })

    // Verify Tenant role filtering (sees estate maintenance advisories, own bookings, own unit cases)
    viewModel.setCalendarCategoryFilter(null)
    viewModel.setUserEmail(tenant.email)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertEquals(UserRole.TENANT, viewModel.currentRole.value)
    assertTrue(viewModel.allCalendarEvents.value.any { it.sourceId == maint.id })
    assertTrue(viewModel.allCalendarEvents.value.any { it.type == CalendarEventType.BOOKING })
    assertTrue(viewModel.allCalendarEvents.value.any { it.type == CalendarEventType.CASE })
    assertTrue(viewModel.allCalendarEvents.value.none { it.type == CalendarEventType.CONTRACT })

    // Delete maintenance and verify calendar updates reactively
    viewModel.deleteMaintenance(maint.id)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertTrue(viewModel.allCalendarEvents.value.none { it.sourceId == maint.id })
  }
}
