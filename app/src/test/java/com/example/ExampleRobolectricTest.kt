package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.tutsville.data.model.BookingApprovalStatus
import com.example.tutsville.data.model.CalendarEventCategory
import com.example.tutsville.data.model.CalendarEventType
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.CasePriority
import com.example.tutsville.data.model.CaseStatus
import com.example.tutsville.data.model.ContractDocumentMeta
import com.example.tutsville.data.model.ContractType
import com.example.tutsville.data.model.EstateConfigOptions
import com.example.tutsville.data.model.EstateDateUtils
import com.example.tutsville.data.model.EventTargetScope
import com.example.tutsville.data.model.LeaseContractStatus
import com.example.tutsville.data.model.MaintenanceDiscipline
import com.example.tutsville.data.model.MaintenanceStatus
import com.example.tutsville.data.model.NoticeTriggerEvent
import com.example.tutsville.data.model.PropertyArchitecture
import com.example.tutsville.data.model.PropertyStatus
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.data.repository.EstateRepository
import com.example.tutsville.ui.components.managerDrawerItems
import com.example.tutsville.ui.components.tenantDrawerItems
import com.example.tutsville.ui.viewmodel.EstateViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    assertTrue(repository.managerInvites.value.isEmpty())
    assertEquals(EstateConfigOptions.DEFAULT_FLOOR_ELEVATIONS, repository.estateConfigOptions.value.floorElevations)
    assertEquals(EstateConfigOptions.DEFAULT_CONTRACTOR_DISCIPLINES, repository.estateConfigOptions.value.contractorDisciplines)
  }

  @Test
  fun `test case reporting with preferredDate, manager notes, and contractor dispatch workflow`() {
    val repository = EstateRepository()
    val preferred = EstateDateUtils.offsetDisplay(5)

    // 0. Onboard contractor with explicit discipline
    val vendor = repository.createVendor(
      name = "Northwind HVAC Partners",
      specialty = CaseCategory.HVAC,
      phone = "+1 (555) 201-4400",
      email = "ops@northwind-hvac.example",
      leadTech = "Marcus Reid",
      discipline = "HVAC & Chiller Systems"
    )
    assertEquals("HVAC & Chiller Systems", vendor.effectiveDiscipline)

    // 1. Report Case with preferred completion date
    val newCase = repository.reportCase(
      title = "HVAC Condenser Noise",
      description = "Vibration in north plant",
      category = CaseCategory.HVAC,
      priority = CasePriority.HIGH,
      location = "Unit 201",
      reportedBy = "Resident Test",
      preferredDate = preferred
    )
    assertNotNull(newCase)
    assertEquals(CaseStatus.OPEN, newCase.status)
    assertEquals(preferred, newCase.preferredDate)
    assertEquals(1, newCase.statusHistory.size)

    // 2. Append Manager Note
    repository.appendCaseNote(newCase.id, "Technician confirmed arrival window")
    val noted = repository.cases.value.find { it.id == newCase.id }
    assertEquals(1, noted?.notes?.size)
    assertEquals("Technician confirmed arrival window", noted?.notes?.first()?.text)

    // 3. Assign Contractor and Resolve
    repository.assignVendorToCase(newCase.id, vendor.id)
    val assigned = repository.cases.value.find { it.id == newCase.id }
    assertEquals(CaseStatus.ASSIGNED, assigned?.status)
    assertEquals(vendor.name, assigned?.assignedVendorName)

    repository.updateCaseStatus(newCase.id, CaseStatus.RESOLVED)
    val resolved = repository.cases.value.find { it.id == newCase.id }
    assertEquals(CaseStatus.RESOLVED, resolved?.status)
    assertTrue((resolved?.statusHistory?.size ?: 0) >= 4)
  }

  @Test
  fun `test facility approval flow and hourly overlap prevention`() {
    val repository = EstateRepository()
    val date = EstateDateUtils.offsetDisplay(3)

    val facility = repository.createFacility(
      name = "Clubhouse Lounge",
      tag = "Wellness",
      capacity = 24,
      location = "North Wing Level 2",
      description = "Private resident lounge",
      amenities = listOf("Wi-Fi"),
      allowedSlots = listOf("09:00 AM - 11:00 AM", "10:00 AM - 12:00 PM"),
      rules = "Check in at reception"
    )

    // Booking starts PENDING without locking slots yet
    val aliceBooking = repository.bookFacility(
      facilityId = facility.id,
      date = date,
      timeSlot = "09:00 AM - 11:00 AM",
      residentName = "Morgan Taylor",
      unit = "Unit 201",
      guestCount = 12,
      notes = "Gathering"
    )
    assertEquals(BookingApprovalStatus.PENDING, aliceBooking.approvalStatus)
    assertFalse("Pending request must not lock hours until Manager approves", repository.isHourlyWindowOccupied(facility.id, date, 9, 2))

    // Manager approves Alice's 9-11 AM request -> locks hours 9 and 10
    var approvedResult: Result<com.example.tutsville.data.model.FacilityBooking>? = null
    repository.approveBooking(aliceBooking.id) { approvedResult = it }
    assertTrue(approvedResult?.isSuccess == true)
    assertEquals(BookingApprovalStatus.APPROVED, repository.bookings.value.first { it.id == aliceBooking.id }.approvalStatus)
    assertTrue(repository.isHourlyWindowOccupied(facility.id, date, 9, 2))
    assertTrue("Overlapping 10 AM - 12 PM must be blocked", repository.isHourlyWindowOccupied(facility.id, date, 10, 2))
    assertFalse("Non-overlapping 11 AM - 1 PM remains available", repository.isHourlyWindowOccupied(facility.id, date, 11, 2))

    // Duplicate/overlapping booking attempt throws IllegalStateException
    var overlapThrown = false
    try {
      repository.bookFacility(
        facilityId = facility.id,
        date = date,
        timeSlot = "10:00 AM - 12:00 PM",
        residentName = "Bob Smith",
        unit = "Unit 202",
        guestCount = 4,
        notes = ""
      )
    } catch (_: IllegalStateException) {
      overlapThrown = true
    }
    assertTrue(overlapThrown)

    // Cancel releases hourly locks
    repository.cancelBooking(aliceBooking.id)
    assertFalse(repository.isHourlyWindowOccupied(facility.id, date, 9, 2))
  }

  @Test
  fun `test notifications emit strictly for Case, Booking, and Scheduled Events and Dashboard announcements require Manager post`() {
    val repository = EstateRepository()
    val viewModel = EstateViewModel(repository)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    // Case lifecycle emits attributed CASE_* notifications (including CASE_NOTE_ADDED)
    val vendor = repository.createVendor(
      name = "Rapid Plumb",
      specialty = CaseCategory.PLUMBING,
      phone = "+1 555 222 3333",
      email = "ops@rapid.example",
      leadTech = "Sam"
    )
    val reported = repository.reportCase(
      title = "Kitchen Valve Drip",
      description = "Under sink leak",
      category = CaseCategory.PLUMBING,
      priority = CasePriority.HIGH,
      location = "Unit 401",
      reportedBy = "Alice",
      tenantEmail = "alice@example.com"
    )
    assertEquals(1, repository.reminders.value.size)
    assertEquals(NoticeTriggerEvent.CASE_CREATED, repository.reminders.value.first().triggerEvent)

    repository.updateCase(reported.id, "Kitchen Pipe Leak", "Steady drip", "Unit 401", CaseCategory.PLUMBING, CasePriority.EMERGENCY)
    repository.appendCaseNote(reported.id, "Parts ordered")
    repository.assignVendorToCase(reported.id, vendor.id)
    repository.updateCaseStatus(reported.id, CaseStatus.RESOLVED)

    // Calendar event emits EVENT_SCHEDULED activity notification (not an Announcement)
    repository.scheduleMaintenance(
      title = "Chiller Filter Swap",
      discipline = MaintenanceDiscipline.HVAC,
      date = EstateDateUtils.todayDisplay(),
      time = "09:00 AM",
      contractor = "AirTech",
      targetArea = "Plant B",
      intervalMonths = 3,
      notes = "Routine",
      eventCategory = CalendarEventCategory.MAINTENANCE,
      targetScope = EventTargetScope.ALL_RESIDENTS
    )

    val triggers = repository.reminders.value.map { it.triggerEvent }.toSet()
    assertTrue(triggers.contains(NoticeTriggerEvent.CASE_CREATED))
    assertTrue(triggers.contains(NoticeTriggerEvent.CASE_UPDATED))
    assertTrue(triggers.contains(NoticeTriggerEvent.CASE_NOTE_ADDED))
    assertTrue(triggers.contains(NoticeTriggerEvent.CASE_VENDOR_ASSIGNED))
    assertTrue(triggers.contains(NoticeTriggerEvent.CASE_STATUS_UPDATED))
    assertTrue(triggers.contains(NoticeTriggerEvent.EVENT_SCHEDULED))

    // Activity notifications do NOT appear in Dashboard Announcements
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertTrue(viewModel.dashboardAnnouncements.value.isEmpty())

    // Manager publishes an explicit announcement -> appears in Dashboard Announcements & Bell
    val advisory = repository.sendReminder(
      title = "Lobby Flooring Polish",
      message = "Main lobby marble polishing tonight 10 PM - 2 AM.",
      targetAudience = "All Residents & Tenants",
      urgency = "Advisory",
      isBroadcast = true
    )
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertEquals(1, viewModel.dashboardAnnouncements.value.size)
    assertEquals(advisory.id, viewModel.dashboardAnnouncements.value.first().id)

    // Delete advisory removes it cleanly
    viewModel.deleteNotification(advisory.id)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertTrue(viewModel.dashboardAnnouncements.value.isEmpty())
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

    val pendingMaintenances = repository.maintenances.value.filter { it.status != MaintenanceStatus.COMPLETED }
    assertTrue("Dashboard should have upcoming scheduled maintenance tasks", pendingMaintenances.isNotEmpty())

    val activeCases = repository.cases.value.filter { it.status != CaseStatus.RESOLVED }
    assertTrue("Dashboard should have active reported or in-progress cases", activeCases.isNotEmpty())

    assertTrue("Dashboard should display bookings", repository.bookings.value.isNotEmpty())
    assertTrue("Dashboard should display estate amenities", repository.facilities.value.isNotEmpty())
  }

  @Test
  fun `test unit listing with sqMeters, configurable floor elevation, and tenant assignment`() {
    val repository = EstateRepository()
    val viewModel = EstateViewModel(repository)

    // Customize configurable options
    viewModel.updateFloorElevations(listOf("1 Story", "2 Story", "3 Story", "Penthouse Level"))
    viewModel.updateContractorDisciplines(listOf("Plumbing & Drains", "Electrical & Power", "Smart Access"))
    assertTrue(repository.estateConfigOptions.value.floorElevations.contains("Penthouse Level"))
    assertTrue(repository.estateConfigOptions.value.contractorDisciplines.contains("Smart Access"))

    // Manager lists a unit with metric sqMeters and configured floor elevation
    val arch = PropertyArchitecture(
      bedrooms = 4,
      bathrooms = 4.0f,
      sqFt = 350,
      floorLevel = "Penthouse Level",
      viewType = "Park Overlook",
      sqMeters = 350
    )
    val listedProp = repository.listProperty(
      unitNumber = "Unit 701",
      architecture = arch,
      amenities = listOf("Private Terrace", "Smart Thermostat"),
      monthlyRental = "$13,800 / mo"
    )

    assertNotNull(listedProp)
    assertEquals(PropertyStatus.AVAILABLE, listedProp.status)
    assertEquals(350, listedProp.architecture.effectiveSqMeters)
    assertTrue(listedProp.size.contains("sq m"))
    assertEquals("Penthouse Level", listedProp.architecture.floorLevel)

    // Assign property to tenant
    val tenantEmail = "jordan.blake@example.com"
    val tenantInfo = repository.assignPropertyToTenant(
      propertyId = listedProp.id,
      tenantName = "Jordan Blake",
      tenantEmail = tenantEmail,
      leaseStartDate = "Dec 01, 2026",
      leaseEndDate = "Nov 30, 2027"
    )
    assertTrue(tenantInfo.inviteLink.startsWith("tutsville://onboard"))
    assertEquals(PropertyStatus.OCCUPIED, repository.properties.value.first { it.id == listedProp.id }.status)

    // Claim & Vacate
    val claimedProp = repository.claimPropertyInvite(tenantEmail)
    assertTrue(claimedProp?.assignedTenant?.isClaimed == true)
    repository.vacateProperty(listedProp.id)
    assertEquals(PropertyStatus.AVAILABLE, repository.properties.value.first { it.id == listedProp.id }.status)
  }

  @Test
  fun `test tenant creation with emergency contact and property assignment`() {
    val repository = EstateRepository()

    val availableProp = repository.listProperty(
      unitNumber = "Unit 304",
      architecture = PropertyArchitecture(2, 2.0f, 130, "2 Story", "Courtyard", sqMeters = 130),
      amenities = emptyList(),
      monthlyRental = "$4,200 / mo"
    )

    val tenant = repository.createTenant(
      fullName = "Samira Patel",
      email = "samira.patel@example.com",
      phone = "+1 (555) 432-8871",
      emergencyContactName = "Dev Patel",
      emergencyContactPhone = "+1 (555) 914-4321"
    )
    assertNotNull(tenant)
    assertTrue(tenant.inviteCode.startsWith("TUTS-"))

    val assignedTenant = repository.assignTenantToPropertyById(
      tenantId = tenant.id,
      propertyId = availableProp.id,
      leaseStartDate = "Dec 01, 2026",
      leaseEndDate = "Nov 30, 2027"
    )
    assertEquals(availableProp.id, assignedTenant?.assignedPropertyId)
    assertEquals(PropertyStatus.OCCUPIED, repository.properties.value.first { it.id == availableProp.id }.status)

    val claimed = repository.claimTenantInvite(tenant.email, tenant.inviteCode)
    assertTrue(claimed?.isAppRegistered == true)

    repository.unassignTenant(tenant.id)
    assertEquals(null, repository.tenants.value.first { it.id == tenant.id }.assignedPropertyId)
    assertEquals(PropertyStatus.AVAILABLE, repository.properties.value.first { it.id == availableProp.id }.status)
  }

  @Test
  fun `test lease auto-creation is active, supports archive restore, attachment metadata, and vacate termination`() {
    val repository = EstateRepository()
    val viewModel = EstateViewModel(repository)

    val unit = repository.listProperty(
      unitNumber = "Penthouse 901",
      architecture = PropertyArchitecture(4, 3.5f, 270, "3 Story", "Skyline", sqMeters = 270),
      amenities = listOf("Private Lift"),
      monthlyRental = "$9,500 / mo"
    )
    val today = EstateDateUtils.todayDisplay()
    val expiry = EstateDateUtils.addMonthsDisplay(today, 12)

    viewModel.assignPropertyToTenant(
      propertyId = unit.id,
      tenantName = "Morgan Reed",
      tenantEmail = "morgan.reed@example.com",
      leaseStartDate = today,
      leaseEndDate = expiry
    )

    val lease = repository.contracts.value.firstOrNull {
      it.type == ContractType.TENANT_LEASE && it.propertyId == unit.id
    }
    assertNotNull(lease)
    assertEquals(LeaseContractStatus.ACTIVE, lease!!.leaseStatus)
    assertFalse(lease.isArchived)

    // Attach signed agreement & code of conduct metadata
    viewModel.attachContractDocument(
      contractId = lease.id,
      isCodeOfConduct = false,
      docMeta = ContractDocumentMeta("lease_signed.pdf", "content://docs/lease_signed.pdf", "application/pdf", today)
    )
    viewModel.attachContractDocument(
      contractId = lease.id,
      isCodeOfConduct = true,
      docMeta = ContractDocumentMeta("conduct.pdf", "content://docs/conduct.pdf", "application/pdf", today)
    )
    val withDocs = repository.contracts.value.first { it.id == lease.id }
    assertEquals("lease_signed.pdf", withDocs.signedContractDoc?.fileName)
    assertEquals("conduct.pdf", withDocs.codeOfConductDoc?.fileName)

    // Archive and restore
    viewModel.setContractArchived(lease.id, true)
    assertTrue(repository.contracts.value.first { it.id == lease.id }.isArchived)
    viewModel.setContractArchived(lease.id, false)
    assertFalse(repository.contracts.value.first { it.id == lease.id }.isArchived)

    // Vacate terminates lease
    viewModel.vacateProperty(unit.id)
    val terminatedLease = repository.contracts.value.first { it.id == lease.id }
    assertEquals(LeaseContractStatus.TERMINATED, terminatedLease.leaseStatus)
    assertEquals(PropertyStatus.AVAILABLE, repository.properties.value.first { it.id == unit.id }.status)
  }

  @Test
  fun `test calendar synchronizes targeted events, bookings, contracts, and preferredDate cases with role filtering`() {
    val repository = EstateRepository()
    val viewModel = EstateViewModel(repository)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    val today = EstateDateUtils.todayDisplay()
    val todayKey = EstateDateUtils.todayKey()
    val targetCaseDate = EstateDateUtils.offsetDisplay(4)
    val targetCaseKey = EstateDateUtils.offsetKey(4)

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
    val unit502 = repository.listProperty(
      unitNumber = "Unit 502",
      architecture = PropertyArchitecture(2, 2.0f, 140, "2 Story", "Garden", sqMeters = 140),
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
    repository.assignTenantToPropertyById(tenant.id, unit502.id, today, EstateDateUtils.addMonthsDisplay(today, 12))

    repository.bookFacility(
      facilityId = facility.id,
      date = today,
      timeSlot = "10:00 AM - 12:00 PM",
      residentName = tenant.fullName,
      unit = unit502.unitNumber,
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
    val windowCase = repository.reportCase(
      title = "Window Seal Draft",
      description = "Minor draft in study",
      category = CaseCategory.STRUCTURAL,
      priority = CasePriority.LOW,
      location = unit502.unitNumber,
      reportedBy = tenant.fullName,
      preferredDate = targetCaseDate
    )
    val allResEvent = repository.scheduleMaintenance(
      title = "Estate Town Hall",
      discipline = MaintenanceDiscipline.GROUNDS,
      date = today,
      time = "06:00 PM",
      contractor = "",
      targetArea = "All Residents",
      intervalMonths = 0,
      notes = "Quarterly meeting",
      eventCategory = CalendarEventCategory.COMMUNITY,
      targetScope = EventTargetScope.ALL_RESIDENTS
    )
    val otherUnitEvent = repository.scheduleMaintenance(
      title = "Unit 999 Private Duct Check",
      discipline = MaintenanceDiscipline.HVAC,
      date = today,
      time = "11:00 AM",
      contractor = "AirTech",
      targetArea = "Unit 999",
      intervalMonths = 0,
      notes = "",
      eventCategory = CalendarEventCategory.MAINTENANCE,
      targetScope = EventTargetScope.SPECIFIC_UNITS,
      targetUnits = listOf("Unit 999")
    )
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

    // Case with preferredDate anchors to preferredDate
    val caseCalEvent = viewModel.allCalendarEvents.value.firstOrNull { it.sourceId == windowCase.id }
    assertEquals(targetCaseKey, caseCalEvent?.dateKey)

    // Tenant sees ALL_RESIDENTS event, hides Unit 999-only event, hides Vendor SLA
    viewModel.setUserEmail(tenant.email)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertEquals(UserRole.TENANT, viewModel.currentRole.value)
    assertTrue(viewModel.allCalendarEvents.value.any { it.sourceId == allResEvent.id })
    assertFalse(viewModel.allCalendarEvents.value.any { it.sourceId == otherUnitEvent.id })
    assertTrue(viewModel.allCalendarEvents.value.any { it.type == CalendarEventType.BOOKING })
    assertTrue(viewModel.allCalendarEvents.value.none { it.title.contains("Roof HVAC SLA") })
  }

  @Test
  fun `test navigation drawer removes Reviews from manager and Units from tenant`() {
    assertFalse(
      "Manager drawer must not include Reviews",
      managerDrawerItems.any { it.title.equals("Reviews", ignoreCase = true) }
    )
    assertTrue(
      "Manager drawer must include Contractors",
      managerDrawerItems.any { it.title.equals("Contractors", ignoreCase = true) }
    )
    assertTrue(
      "Tenant drawer must not contain Units",
      tenantDrawerItems.none { it.title.equals("Units", ignoreCase = true) }
    )
    assertEquals(listOf("Amenities", "Cases", "Advisories", "Calendar", "Contracts"), tenantDrawerItems.map { it.title })

    val repository = EstateRepository()
    val tenant = repository.createTenant(
      fullName = "Jordan Lee",
      email = "jordan.lee@example.com",
      phone = "+1 (555) 100-2000",
      emergencyContactName = "Taylor Lee",
      emergencyContactPhone = "+1 (555) 100-3000"
    )
    val viewModel = EstateViewModel(repository)
    viewModel.setUserEmail(tenant.email)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertEquals(UserRole.TENANT, viewModel.currentRole.value)

    viewModel.activeListPropertyDialog.value = true
    viewModel.listProperty(
      unitNumber = "Unauthorized Unit",
      architecture = PropertyArchitecture(1, 1.0f, 80, "1 Story", "Garden", sqMeters = 80),
      amenities = emptyList(),
      monthlyRental = "$1,000 / mo"
    )
    assertTrue("Tenants must not be allowed to create properties", repository.properties.value.isEmpty())
    assertFalse(viewModel.activeListPropertyDialog.value)
  }

  @Test
  fun `test co-manager invite and revoke workflow plus facility slot validation`() {
    val repository = EstateRepository()
    val viewModel = EstateViewModel(repository)

    // Co-manager invite & revoke
    viewModel.inviteManager("Partner.Manager@Example.com", "Alex Partner")
    assertEquals(1, repository.managerInvites.value.size)
    assertEquals("partner.manager@example.com", repository.managerInvites.value.first().email)
    assertEquals("PENDING", repository.managerInvites.value.first().status)

    viewModel.revokeManagerInvite("partner.manager@example.com")
    assertTrue(repository.managerInvites.value.isEmpty())

    // Facility without slots is rejected
    viewModel.submitNewFacility(
      name = "Rooftop Tennis Court",
      tag = "Sports",
      capacity = 4,
      location = "Roof Deck",
      description = "Floodlit court",
      amenities = listOf("Ball Machine"),
      slots = emptyList(),
      rules = "Non-marking shoes only"
    )
    assertTrue("Amenity without slots must not be created", repository.facilities.value.isEmpty())

    // Legacy amenity with empty allowedSlots reports unconfigured instead of booked
    val legacyFac = repository.createFacility(
      name = "Legacy Spa",
      tag = "Wellness",
      capacity = 6,
      location = "Level 2",
      description = "Thermal spa",
      amenities = emptyList(),
      allowedSlots = emptyList(),
      rules = ""
    )
    val today = EstateDateUtils.todayDisplay()
    assertFalse(repository.isSlotBooked(legacyFac.id, today, ""))

    var unconfiguredError: String? = null
    try {
      repository.bookFacility(legacyFac.id, today, "08:00 AM - 10:00 AM", "Alice", "Unit 1", 2, "")
    } catch (e: IllegalStateException) {
      unconfiguredError = e.message
    }
    assertTrue(unconfiguredError?.contains("not been configured", ignoreCase = true) == true)

    // Add slots, submit booking request, approve, and verify slot status
    viewModel.updateFacility(
      facilityId = legacyFac.id,
      name = legacyFac.name,
      tag = legacyFac.tag,
      capacity = legacyFac.capacity,
      location = legacyFac.location,
      description = legacyFac.description,
      amenities = legacyFac.amenities,
      slots = listOf("08:00 AM - 10:00 AM", "06:00 PM - 08:00 PM"),
      rules = legacyFac.rules
    )

    viewModel.activeBookFacility.value = repository.facilities.value.first { it.id == legacyFac.id }
    viewModel.bookFacilitySlot(
      facilityId = legacyFac.id,
      date = today,
      timeSlot = "08:00 AM - 10:00 AM",
      guestCount = 99,
      notes = "Evening soak"
    )
    val booking = repository.bookings.value.first()
    assertEquals(6, booking.guestCount)
    assertEquals(BookingApprovalStatus.PENDING, booking.approvalStatus)

    viewModel.approveBooking(booking.id)
    assertTrue(viewModel.isSlotBooked(legacyFac.id, today, "08:00 AM - 10:00 AM"))
    assertFalse(viewModel.isSlotBooked(legacyFac.id, today, "06:00 PM - 08:00 PM"))
  }
}
