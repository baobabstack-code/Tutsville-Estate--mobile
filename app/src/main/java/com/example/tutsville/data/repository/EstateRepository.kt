package com.example.tutsville.data.repository

import com.example.tutsville.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EstateRepository {

  private val _cases = MutableStateFlow<List<CaseItem>>(seedCases())
  val cases: StateFlow<List<CaseItem>> = _cases.asStateFlow()

  private val _maintenances = MutableStateFlow<List<MaintenanceEvent>>(seedMaintenances())
  val maintenances: StateFlow<List<MaintenanceEvent>> = _maintenances.asStateFlow()

  private val _facilities = MutableStateFlow<List<Facility>>(seedFacilities())
  val facilities: StateFlow<List<Facility>> = _facilities.asStateFlow()

  private val _bookings = MutableStateFlow<List<FacilityBooking>>(seedBookings())
  val bookings: StateFlow<List<FacilityBooking>> = _bookings.asStateFlow()

  private val _contracts = MutableStateFlow<List<ContractItem>>(seedContracts())
  val contracts: StateFlow<List<ContractItem>> = _contracts.asStateFlow()

  private val _vendors = MutableStateFlow<List<VendorItem>>(seedVendors())
  val vendors: StateFlow<List<VendorItem>> = _vendors.asStateFlow()

  private val _reminders = MutableStateFlow<List<ReminderItem>>(seedReminders())
  val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

  private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
  private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

  // --- Work Order / Case Operations ---
  fun reportCase(
    title: String,
    description: String,
    category: CaseCategory,
    priority: CasePriority,
    location: String,
    reportedBy: String
  ): CaseItem {
    val newCase = CaseItem(
      id = "CASE-${System.currentTimeMillis() % 10000}",
      title = title,
      description = description,
      category = category,
      priority = priority,
      location = location,
      reportedBy = reportedBy,
      reportedDate = dateFormat.format(Date()),
      status = CaseStatus.OPEN
    )
    _cases.update { listOf(newCase) + it }
    return newCase
  }

  fun assignVendorToCase(caseId: String, vendorId: String) {
    val vendor = _vendors.value.find { it.id == vendorId } ?: return
    _cases.update { current ->
      current.map { c ->
        if (c.id == caseId) {
          c.copy(
            assignedVendorId = vendor.id,
            assignedVendorName = vendor.name,
            status = CaseStatus.ASSIGNED
          )
        } else c
      }
    }
    // Update active jobs count for vendor
    _vendors.update { current ->
      current.map { v ->
        if (v.id == vendorId) v.copy(activeJobsCount = v.activeJobsCount + 1) else v
      }
    }
  }

  fun updateCaseStatus(caseId: String, newStatus: CaseStatus) {
    _cases.update { current ->
      current.map { c ->
        if (c.id == caseId) c.copy(status = newStatus) else c
      }
    }
  }

  fun submitCaseReview(caseId: String, rating: Int, reviewTags: List<String>, comment: String) {
    _cases.update { current ->
      current.map { c ->
        if (c.id == caseId) {
          c.copy(
            rating = rating,
            reviewTags = reviewTags,
            reviewComment = comment,
            status = CaseStatus.RESOLVED
          )
        } else c
      }
    }

    // Recalculate vendor average rating if assigned
    val theCase = _cases.value.find { it.id == caseId }
    theCase?.assignedVendorId?.let { vendorId ->
      val vendorCasesWithRatings = _cases.value.filter { it.assignedVendorId == vendorId && it.rating != null }
      if (vendorCasesWithRatings.isNotEmpty()) {
        val avg = vendorCasesWithRatings.mapNotNull { it.rating }.average().toFloat()
        _vendors.update { list ->
          list.map { v ->
            if (v.id == vendorId) {
              v.copy(
                rating = String.format(Locale.US, "%.1f", avg).toFloat(),
                reviewCount = vendorCasesWithRatings.size
              )
            } else v
          }
        }
      }
    }
  }

  // --- Facility Booking Operations ---
  fun bookFacility(
    facilityId: String,
    date: String,
    timeSlot: String,
    residentName: String,
    unit: String,
    guestCount: Int,
    notes: String
  ): FacilityBooking {
    val facility = _facilities.value.find { it.id == facilityId }
    val newBooking = FacilityBooking(
      id = "BK-${System.currentTimeMillis() % 10000}",
      facilityId = facilityId,
      facilityName = facility?.name ?: "Estate Amenity",
      residentName = residentName,
      unit = unit,
      date = date,
      timeSlot = timeSlot,
      guestCount = guestCount,
      notes = notes,
      confirmedTimestamp = dateFormat.format(Date()) + " " + timeFormat.format(Date())
    )
    _bookings.update { listOf(newBooking) + it }
    return newBooking
  }

  fun cancelBooking(bookingId: String) {
    _bookings.update { it.filterNot { b -> b.id == bookingId } }
  }

  // --- Maintenance & Calendar Operations ---
  fun scheduleMaintenance(
    title: String,
    discipline: MaintenanceDiscipline,
    date: String,
    time: String,
    contractor: String,
    targetArea: String,
    intervalMonths: Int,
    notes: String
  ) {
    val event = MaintenanceEvent(
      id = "MAINT-${System.currentTimeMillis() % 10000}",
      title = title,
      discipline = discipline,
      scheduledDate = date,
      scheduledTime = time,
      contractorName = contractor,
      targetArea = targetArea,
      intervalMonths = intervalMonths,
      status = MaintenanceStatus.SCHEDULED,
      notes = notes
    )
    _maintenances.update { listOf(event) + it }
  }

  fun toggleMaintenanceStatus(eventId: String, newStatus: MaintenanceStatus) {
    _maintenances.update { current ->
      current.map { if (it.id == eventId) it.copy(status = newStatus) else it }
    }
  }

  // --- Reminders Operations ---
  fun sendReminder(
    title: String,
    message: String,
    targetAudience: String,
    urgency: String,
    isBroadcast: Boolean = false
  ): ReminderItem {
    val newReminder = ReminderItem(
      id = "REM-${System.currentTimeMillis() % 10000}",
      title = title,
      message = message,
      targetAudience = targetAudience,
      urgency = urgency,
      timestamp = dateFormat.format(Date()) + " • " + timeFormat.format(Date()),
      isBroadcast = isBroadcast
    )
    _reminders.update { listOf(newReminder) + it }
    return newReminder
  }

  // --- Contract Operations & Expiry Notification ---
  fun sendContractRundownNotification(contractId: String) {
    _contracts.update { list ->
      list.map { c ->
        if (c.id == contractId) c.copy(isNotificationSent = true) else c
      }
    }
    val contract = _contracts.value.find { it.id == contractId } ?: return
    sendReminder(
      title = "Contract Expiry Alert: ${contract.title}",
      message = "${contract.title} with ${contract.counterparty} has ${contract.daysRemaining} days remaining. Review terms or start renewal RFP.",
      targetAudience = "Property Managers & Executive Board",
      urgency = "High Priority"
    )
  }

  fun renewContract(contractId: String, additionalYears: Int = 1) {
    _contracts.update { list ->
      list.map { c ->
        if (c.id == contractId) {
          c.copy(
            daysRemaining = c.daysRemaining + (365 * additionalYears),
            expiryDate = "Nov 15, 2027",
            isNotificationSent = false
          )
        } else c
      }
    }
  }

  // --- Seed Data Generators ---
  private fun seedCases(): List<CaseItem> = listOf(
    CaseItem(
      id = "CASE-1082",
      title = "East Wing Master Chiller Pressure Drop",
      description = "Secondary condenser loop pressure dropped below 2.4 bar. Compressor vibrating excessively in plant room.",
      category = CaseCategory.HVAC,
      priority = CasePriority.EMERGENCY,
      location = "Central Utility Basement • Plant B",
      reportedBy = "Julian Vance (Chief Engineer)",
      reportedDate = "Nov 09, 2026",
      assignedVendorId = "VEND-01",
      assignedVendorName = "Apex HVAC Solutions",
      status = CaseStatus.IN_PROGRESS
    ),
    CaseItem(
      id = "CASE-1081",
      title = "Penthouse 602 Master Bath Valve Seepage",
      description = "Thermostatic mixer valve behind marble wall has intermittent seepage causing water pressure fluctuations.",
      category = CaseCategory.PLUMBING,
      priority = CasePriority.HIGH,
      location = "North Tower • Penthouse 602",
      reportedBy = "Eleanor Sterling (Resident)",
      reportedDate = "Nov 08, 2026",
      assignedVendorId = "VEND-03",
      assignedVendorName = "AquaPave Plumbing",
      status = CaseStatus.ASSIGNED
    ),
    CaseItem(
      id = "CASE-1079",
      title = "Clubhouse Veranda Column Sconce Flickering",
      description = "Architectural LED driver intermittent on west colonnade. Needs driver replacement and circuit load test.",
      category = CaseCategory.ELECTRICAL,
      priority = CasePriority.MEDIUM,
      location = "The Orangery Clubhouse • West Veranda",
      reportedBy = "Marcus Rowe (Estate Concierge)",
      reportedDate = "Nov 07, 2026",
      assignedVendorId = "VEND-02",
      assignedVendorName = "VoltCraft Engineering",
      status = CaseStatus.OPEN
    ),
    CaseItem(
      id = "CASE-1075",
      title = "South Pavilion Courtyard Fountain Pump Overhaul",
      description = "Variable frequency drive tuned and dual impeller replaced. Flow rate restored to optimal 450 GPM.",
      category = CaseCategory.GROUNDS,
      priority = CasePriority.MEDIUM,
      location = "South Courtyard • Tiered Fountain",
      reportedBy = "Diana Chen (Grounds Manager)",
      reportedDate = "Nov 04, 2026",
      assignedVendorId = "VEND-04",
      assignedVendorName = "Verdant Living Grounds",
      status = CaseStatus.RESOLVED,
      rating = 5,
      reviewTags = listOf("Punctual", "Clean Worksite", "High Craftsmanship"),
      reviewComment = "Impeccable execution. Water feature running silently with balanced pressure."
    ),
    CaseItem(
      id = "CASE-1071",
      title = "Tower B Lift 3 Door Sensor Realignment",
      description = "Light curtain photodiode array re-calibrated. Door closing torque within safety margins.",
      category = CaseCategory.STRUCTURAL,
      priority = CasePriority.HIGH,
      location = "Tower B • Lift Shaft 3",
      reportedBy = "Julian Vance",
      reportedDate = "Nov 01, 2026",
      assignedVendorId = "VEND-05",
      assignedVendorName = "Otis Vertical Transit",
      status = CaseStatus.RESOLVED,
      rating = 4,
      reviewTags = listOf("Resolved First Visit", "Detailed Report"),
      reviewComment = "Prompt response time and comprehensive diagnostic test report provided."
    )
  )

  private fun seedMaintenances(): List<MaintenanceEvent> = listOf(
    MaintenanceEvent(
      id = "MAINT-501",
      title = "Annual Fire Sprinkler & Hydrant Flow Test",
      discipline = MaintenanceDiscipline.FIRE_LIFE,
      scheduledDate = "Nov 12, 2026",
      scheduledTime = "09:00 AM - 01:00 PM",
      contractorName = "Sentinel Fire & Safety",
      targetArea = "Entire Estate • Wings A, B & C",
      intervalMonths = 12,
      status = MaintenanceStatus.SCHEDULED,
      notes = "Riser flow rates, diesel booster pump activation test, and alarm panel linkage audit."
    ),
    MaintenanceEvent(
      id = "MAINT-502",
      title = "Quarterly Elevator Brake & Cable Safety Audit",
      discipline = MaintenanceDiscipline.ELEVATORS,
      scheduledDate = "Nov 14, 2026",
      scheduledTime = "10:00 AM - 04:00 PM",
      contractorName = "Otis Vertical Transit",
      targetArea = "Towers A, B & Central Core",
      intervalMonths = 3,
      status = MaintenanceStatus.SCHEDULED,
      notes = "Governor rope tension, safety gear drop test, and car leveling sensor calibration."
    ),
    MaintenanceEvent(
      id = "MAINT-503",
      title = "Primary Substation Transformer Infrared Scan",
      discipline = MaintenanceDiscipline.ELECTRICAL,
      scheduledDate = "Nov 18, 2026",
      scheduledTime = "01:00 PM - 05:00 PM",
      contractorName = "VoltCraft Engineering",
      targetArea = "High Voltage Substation 1",
      intervalMonths = 6,
      status = MaintenanceStatus.SCHEDULED,
      notes = "Thermographic thermometry scan for hot spots across busbars and circuit breakers."
    ),
    MaintenanceEvent(
      id = "MAINT-504",
      title = "Central Chiller Water Treatment & Chemical Balancing",
      discipline = MaintenanceDiscipline.HVAC,
      scheduledDate = "Nov 06, 2026",
      scheduledTime = "08:00 AM - 12:00 PM",
      contractorName = "Apex HVAC Solutions",
      targetArea = "Cooling Towers & Closed Loops",
      intervalMonths = 1,
      status = MaintenanceStatus.COMPLETED,
      notes = "Biocide injection, conductivity check, and scale inhibitor levels verified."
    ),
    MaintenanceEvent(
      id = "MAINT-505",
      title = "Domestic Potable Water Tank Sterilization",
      discipline = MaintenanceDiscipline.PLUMBING,
      scheduledDate = "Nov 02, 2026",
      scheduledTime = "07:00 AM - 03:00 PM",
      contractorName = "AquaPave Plumbing",
      targetArea = "Rooftop Storage Cisterns",
      intervalMonths = 6,
      status = MaintenanceStatus.COMPLETED,
      notes = "Complete drain-down, ultraviolet scrub, and certified laboratory water quality test."
    )
  )

  private fun seedFacilities(): List<Facility> = listOf(
    Facility(
      id = "FAC-01",
      name = "The Orangery Clubhouse",
      tag = "Grand Reception & Dining",
      capacity = 45,
      location = "Central Gardens • Pavillion A",
      description = "Heritage glasshouse with vaulted timber ceilings, catering prep kitchen, fireside salon, and integrated Sonos sound system.",
      amenities = listOf("Catering Kitchen", "Fireplace Lounge", "Grand Piano", "Wine Chiller", "Audiovisual"),
      allowedSlots = listOf("10:00 AM - 02:00 PM", "03:00 PM - 07:00 PM", "07:30 PM - 11:30 PM"),
      rules = "Max 45 guests. Music cutoff at 11:00 PM. Catering setup allowed 1 hour prior."
    ),
    Facility(
      id = "FAC-02",
      name = "North Terrace Tennis Court",
      tag = "All-Weather Surface",
      capacity = 8,
      location = "North Grounds",
      description = "Championship regulation hard court with LED floodlighting, automated ball launcher, and courtside spectator pergola.",
      amenities = listOf("Night Floodlights", "Ball Machine", "Shaded Pergola", "Chilled Water Station"),
      allowedSlots = listOf("07:00 AM - 09:00 AM", "09:30 AM - 11:30 AM", "04:00 PM - 06:00 PM", "06:30 PM - 08:30 PM"),
      rules = "Non-marking tennis shoes required. 2-hour max session per resident per day."
    ),
    Facility(
      id = "FAC-03",
      name = "Skyline Rooftop Lounge",
      tag = "Panoramic Sunset Deck",
      capacity = 30,
      location = "Tower A • Level 28",
      description = "Open-air landscaped terrace with 360-degree estate views, brass fire troughs, cocktail bar, and heated lounge booths.",
      amenities = listOf("Gas Fire Pits", "Cocktail Bar", "Heated Seating", "Sunset Viewing Deck"),
      allowedSlots = listOf("04:00 PM - 07:00 PM", "07:30 PM - 11:00 PM"),
      rules = "Residents must be present with guests. No glassware near perimeter railings."
    ),
    Facility(
      id = "FAC-04",
      name = "Bespoke Cinema Salon",
      tag = "Private Screening Room",
      capacity = 16,
      location = "Clubhouse Lower Level",
      description = "Acoustically tuned screening theater with motorized Italian leather recliners, 4K laser projection, and Dolby Atmos 9.2.",
      amenities = listOf("Dolby Atmos", "4K Laser Projection", "Leather Recliners", "Popcorn Bar"),
      allowedSlots = listOf("01:00 PM - 04:00 PM", "05:00 PM - 08:00 PM", "08:30 PM - 11:30 PM"),
      rules = "Streaming services pre-logged. Please return room to clean state after screening."
    ),
    Facility(
      id = "FAC-05",
      name = "Garden BBQ Pergola",
      tag = "Alfresco Culinary Station",
      capacity = 20,
      location = "South Meadow Gardens",
      description = "Cedar pergola featuring dual Lynx infrared grills, wood-fired pizza oven, prep counters, and rustic teak dining table.",
      amenities = listOf("Dual Lynx Gas Grills", "Wood Pizza Oven", "Teak Dining Table", "Outdoor Sink"),
      allowedSlots = listOf("11:30 AM - 03:00 PM", "04:30 PM - 08:30 PM"),
      rules = "Clean grill grates after use with provided brass wire brushes. Extinguish oven embers."
    )
  )

  private fun seedBookings(): List<FacilityBooking> = listOf(
    FacilityBooking(
      id = "BK-8021",
      facilityId = "FAC-01",
      facilityName = "The Orangery Clubhouse",
      residentName = "Lord Arthur Tutsville",
      unit = "Penthouse A",
      date = "Nov 15, 2026",
      timeSlot = "07:30 PM - 11:30 PM",
      guestCount = 28,
      notes = "Annual Autumn Estate Stewardship Dinner",
      confirmedTimestamp = "Nov 05, 2026 • 10:14 AM"
    ),
    FacilityBooking(
      id = "BK-8019",
      facilityId = "FAC-02",
      facilityName = "North Terrace Tennis Court",
      residentName = "Camilla Davenport",
      unit = "Unit 304",
      date = "Nov 10, 2026",
      timeSlot = "09:30 AM - 11:30 AM",
      guestCount = 4,
      notes = "Morning doubles friendly",
      confirmedTimestamp = "Nov 08, 2026 • 04:22 PM"
    )
  )

  private fun seedContracts(): List<ContractItem> = listOf(
    ContractItem(
      id = "CTR-SLA-01",
      title = "Elevator & Vertical Transit Master SLA",
      counterparty = "Otis Vertical Transit Corp",
      type = ContractType.VENDOR_SLA,
      annualValue = "$84,000 / yr",
      startDate = "Dec 01, 2023",
      expiryDate = "Nov 22, 2026",
      daysRemaining = 13, // URGENT RUNDOWN ALERT!
      renewalTerms = "Requires 30-day written tender notice. 3% annual inflation cap index.",
      contactPerson = "Marcus Kane (Account Executive)",
      contactEmail = "marcus.kane@otis-partners.com"
    ),
    ContractItem(
      id = "CTR-SLA-02",
      title = "Estate Landscaping & Arboriculture SLA",
      counterparty = "Verdant Living Grounds Ltd",
      type = ContractType.VENDOR_SLA,
      annualValue = "$62,500 / yr",
      startDate = "Jan 01, 2024",
      expiryDate = "Dec 05, 2026",
      daysRemaining = 26, // EXPIRING 30 DAYS!
      renewalTerms = "Includes spring tree pruning, irrigation winterization, weekly turf manicure.",
      contactPerson = "Elena Gomez",
      contactEmail = "elena@verdantgrounds.com"
    ),
    ContractItem(
      id = "CTR-LEASE-01",
      title = "Penthouse 801 Residential Lease Agreement",
      counterparty = "Dr. Nathaniel & Claire Thorne",
      type = ContractType.TENANT_LEASE,
      annualValue = "$180,000 / yr",
      startDate = "Jan 01, 2025",
      expiryDate = "Dec 28, 2026",
      daysRemaining = 49, // EXPIRING 60 DAYS!
      renewalTerms = "Two-year option at prevailing market index. Requires tenant intent 45 days prior.",
      contactPerson = "Nathaniel Thorne",
      contactEmail = "nthorne@medicine.org"
    ),
    ContractItem(
      id = "CTR-WARR-01",
      title = "Commercial Centrifugal Chiller Factory Warranty",
      counterparty = "Daikin Applied Engineering",
      type = ContractType.EQUIPMENT_WARRANTY,
      annualValue = "$18,000 / yr",
      startDate = "Apr 15, 2024",
      expiryDate = "May 15, 2027",
      daysRemaining = 187,
      renewalTerms = "Covers magnetic bearing compressors, electronic expansion valves, and heat exchangers.",
      contactPerson = "Daikin Care Desk",
      contactEmail = "support@daikinapplied.com"
    ),
    ContractItem(
      id = "CTR-SLA-03",
      title = "Estate Security & Biometric Access Maintenance",
      counterparty = "SecureGate Biometrics Inc",
      type = ContractType.VENDOR_SLA,
      annualValue = "$45,000 / yr",
      startDate = "Mar 01, 2025",
      expiryDate = "Feb 28, 2027",
      daysRemaining = 111,
      renewalTerms = "24/7 emergency dispatch response within 45 minutes. Bi-annual firmware updates.",
      contactPerson = "David Cross",
      contactEmail = "dcross@securegate.io"
    )
  )

  private fun seedVendors(): List<VendorItem> = listOf(
    VendorItem(
      id = "VEND-01",
      name = "Apex HVAC Solutions",
      specialty = CaseCategory.HVAC,
      rating = 4.9f,
      reviewCount = 42,
      activeJobsCount = 2,
      phone = "+1 (555) 234-8901",
      email = "dispatch@apexhvac.com",
      leadTech = "Robert Miller (Master Tech #819)"
    ),
    VendorItem(
      id = "VEND-02",
      name = "VoltCraft Engineering",
      specialty = CaseCategory.ELECTRICAL,
      rating = 4.8f,
      reviewCount = 38,
      activeJobsCount = 1,
      phone = "+1 (555) 442-1920",
      email = "ops@voltcraft-eng.com",
      leadTech = "Liam Sterling (Industrial Electrician)"
    ),
    VendorItem(
      id = "VEND-03",
      name = "AquaPave Plumbing",
      specialty = CaseCategory.PLUMBING,
      rating = 4.7f,
      reviewCount = 51,
      activeJobsCount = 3,
      phone = "+1 (555) 887-3402",
      email = "service@aquapave.com",
      leadTech = "Patrick O'Connor"
    ),
    VendorItem(
      id = "VEND-04",
      name = "Verdant Living Grounds",
      specialty = CaseCategory.GROUNDS,
      rating = 4.9f,
      reviewCount = 29,
      activeJobsCount = 1,
      phone = "+1 (555) 901-2244",
      email = "care@verdantgrounds.com",
      leadTech = "Mateo Rossi"
    ),
    VendorItem(
      id = "VEND-05",
      name = "Otis Vertical Transit",
      specialty = CaseCategory.STRUCTURAL,
      rating = 4.6f,
      reviewCount = 64,
      activeJobsCount = 2,
      phone = "+1 (555) 112-9900",
      email = "service@otis-partners.com",
      leadTech = "Gregory Vance"
    )
  )

  private fun seedReminders(): List<ReminderItem> = listOf(
    ReminderItem(
      id = "REM-01",
      title = "Scheduled Lift Servicing • Tower B",
      message = "Tower B Lift 2 will be offline tomorrow from 10:00 AM to 02:00 PM for cable re-tensioning.",
      targetAudience = "All Tower B Residents",
      urgency = "Notice",
      timestamp = "Nov 08, 2026 • 03:30 PM",
      isBroadcast = true
    ),
    ReminderItem(
      id = "REM-02",
      title = "Fire Alarm Acoustic Siren Calibration",
      message = "Routine 10-second bell testing will occur on Thursday Nov 12 at 11:00 AM across all common corridors.",
      targetAudience = "Estate-Wide Announcement",
      urgency = "Advisory",
      timestamp = "Nov 07, 2026 • 11:00 AM",
      isBroadcast = true
    ),
    ReminderItem(
      id = "REM-03",
      title = "Contract Expiry Notice: Otis Vertical Transit",
      message = "Master elevator maintenance SLA reaches 14-day renewal threshold. Board decision needed on 3-year extension.",
      targetAudience = "Property Managers",
      urgency = "Action Required",
      timestamp = "Nov 06, 2026 • 09:15 AM",
      isBroadcast = false
    )
  )
}
