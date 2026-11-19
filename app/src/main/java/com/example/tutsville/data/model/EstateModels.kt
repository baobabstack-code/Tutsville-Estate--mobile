package com.example.tutsville.data.model

enum class UserRole(val label: String, val badge: String) {
  MANAGER("Property Manager", "Admin"),
  TENANT("Resident / Tenant", "Resident"),
  VENDOR("Certified Contractor", "Contractor"),
  UNREGISTERED("Pending Access", "Pending")
}

enum class CaseCategory(val label: String) {
  PLUMBING("Plumbing & Drains"),
  ELECTRICAL("Electrical & Power"),
  HVAC("Heating & Air Conditioning"),
  STRUCTURAL("Doors, Windows & Locks"),
  APPLIANCE("Kitchen & Laundry"),
  GROUNDS("Common Areas & Grounds"),
  SECURITY("Access & Surveillance");

  companion object {
    fun fromLabel(raw: String): CaseCategory {
      val clean = raw.trim()
      return entries.find {
        it.label.equals(clean, ignoreCase = true) ||
          it.name.equals(clean, ignoreCase = true) ||
          clean.contains(it.name, ignoreCase = true)
      } ?: STRUCTURAL
    }
  }
}

enum class CasePriority(val label: String, val slaHours: Int) {
  LOW("Low (72h SLA)", 72),
  MEDIUM("Standard (24h SLA)", 24),
  HIGH("Urgent (8h SLA)", 8),
  EMERGENCY("Critical (2h SLA)", 2)
}

enum class CaseStatus(val label: String) {
  OPEN("Reported"),
  ASSIGNED("Assigned"),
  IN_PROGRESS("In Progress"),
  RESOLVED("Resolved")
}

data class CaseNote(
  val id: String = "",
  val authorName: String = "",
  val authorEmail: String = "",
  val authorRole: String = "MANAGER",
  val text: String = "",
  val createdAt: String = "",
  val createdAtEpochMs: Long = 0L
)

data class CaseMovementEntry(
  val status: String = "",
  val note: String = "",
  val updatedBy: String = "",
  val timestamp: String = "",
  val epochMs: Long = 0L
)

data class CaseItem(
  val id: String,
  val number: String = id,
  val userId: String = "",
  val tenantEmail: String = "",
  val title: String,
  val description: String,
  val category: CaseCategory,
  val categoryLabel: String = category.label,
  val priority: CasePriority,
  val location: String,
  val unitNumber: String = location,
  val reportedBy: String,
  val tenantNumber: String = reportedBy,
  val reportedDate: String,
  val date: String = reportedDate,
  val preferredDate: String? = null,
  val assignedVendorId: String? = null,
  val assignedVendorName: String? = null,
  val vendor: String = assignedVendorName ?: "Unassigned",
  val status: CaseStatus = CaseStatus.OPEN,
  val notes: List<CaseNote> = emptyList(),
  val statusHistory: List<CaseMovementEntry> = emptyList(),
  val createdAtEpochMs: Long = 0L,
  val rating: Int? = null,
  val reviewComment: String? = null,
  val reviewTags: List<String> = emptyList(),
  val photoNote: String? = null
) {
  val effectiveCategoryLabel: String
    get() = categoryLabel.ifBlank { category.label }
}

enum class MaintenanceDiscipline(val label: String) {
  HVAC("HVAC & Ventilation"),
  ELEVATORS("Lifts & Vertical Transit"),
  FIRE_LIFE("Fire & Life Safety"),
  ELECTRICAL("Transformers & Generators"),
  PLUMBING("Boilers & Pressure Pumps"),
  GROUNDS("Landscaping & Water Features");

  companion object {
    fun fromLabel(raw: String): MaintenanceDiscipline {
      val clean = raw.trim()
      return entries.find {
        it.label.equals(clean, ignoreCase = true) ||
          it.name.equals(clean, ignoreCase = true) ||
          clean.contains(it.name.replace("_", " "), ignoreCase = true) ||
          clean.contains(it.name, ignoreCase = true)
      } ?: HVAC
    }
  }
}

enum class CalendarEventCategory(val label: String) {
  MAINTENANCE("Maintenance / Repair"),
  GENERAL("General Event"),
  INSPECTION("Inspection / Audit"),
  COMMUNITY("Community Event")
}

enum class EventTargetScope(val label: String) {
  ALL_RESIDENTS("All Residents"),
  SPECIFIC_UNITS("Specific Units / Areas")
}

enum class MaintenanceStatus(val label: String) {
  SCHEDULED("Scheduled"),
  IN_PROGRESS("In Progress"),
  COMPLETED("Completed"),
  OVERDUE("Overdue")
}

data class MaintenanceEvent(
  val id: String,
  val title: String,
  val discipline: MaintenanceDiscipline,
  val disciplineLabel: String = discipline.label,
  val eventCategory: CalendarEventCategory = CalendarEventCategory.MAINTENANCE,
  val scheduledDate: String,
  val scheduledTime: String,
  val contractorName: String,
  val targetArea: String,
  val targetScope: EventTargetScope = EventTargetScope.ALL_RESIDENTS,
  val targetUnits: List<String> = emptyList(),
  val intervalMonths: Int,
  val status: MaintenanceStatus = MaintenanceStatus.SCHEDULED,
  val notes: String
) {
  val effectiveDisciplineLabel: String
    get() = disciplineLabel.ifBlank { discipline.label }
}

enum class ContractType(val label: String) {
  VENDOR_SLA("Contractor SLA"),
  TENANT_LEASE("Tenant Lease"),
  EQUIPMENT_WARRANTY("Equipment Warranty")
}

enum class ContractUrgency {
  HEALTHY,
  EXPIRING_60_DAYS,
  EXPIRING_30_DAYS,
  URGENT_CRITICAL
}

enum class LeaseContractStatus(val label: String) {
  PENDING_SIGNATURE("Active Lease"),
  ACTIVE("Active Lease"),
  EXPIRED("Expired"),
  TERMINATED("Terminated")
}

data class ContractDocumentMeta(
  val fileName: String = "",
  val contentUri: String = "",
  val mimeType: String = "",
  val attachedAt: String = ""
)

data class ContractItem(
  val id: String,
  val number: String = id,
  val title: String,
  val counterparty: String,
  val tenant: String = counterparty,
  val unit: String = "Estate Wide",
  val type: ContractType,
  val annualValue: String,
  val contractAmount: String = annualValue,
  val startDate: String,
  val expiryDate: String,
  val daysRemaining: Int,
  val renewalTerms: String,
  val contactPerson: String,
  val contactEmail: String,
  val isNotificationSent: Boolean = false,
  val tenantId: String? = null,
  val tenantEmail: String? = null,
  val propertyId: String? = null,
  val managerName: String = "",
  val managerEmail: String = "",
  val leaseStatus: LeaseContractStatus = LeaseContractStatus.ACTIVE,
  val isSignedByTenant: Boolean = false,
  val signedByName: String? = null,
  val signedByEmail: String? = null,
  val signedAt: String? = null,
  val isArchived: Boolean = false,
  val signedContractDoc: ContractDocumentMeta? = null,
  val codeOfConductDoc: ContractDocumentMeta? = null
) {
  val effectiveDaysRemaining: Int
    get() = EstateDateUtils.daysUntilDisplay(expiryDate) ?: daysRemaining

  val urgency: ContractUrgency
    get() = when {
      effectiveDaysRemaining <= 14 -> ContractUrgency.URGENT_CRITICAL
      effectiveDaysRemaining <= 30 -> ContractUrgency.EXPIRING_30_DAYS
      effectiveDaysRemaining <= 60 -> ContractUrgency.EXPIRING_60_DAYS
      else -> ContractUrgency.HEALTHY
    }
}

data class Facility(
  val id: String,
  val number: String = id,
  val name: String,
  val tag: String,
  val capacity: Int,
  val location: String,
  val description: String,
  val amenities: List<String>,
  val allowedSlots: List<String>,
  val rules: String
)

enum class BookingApprovalStatus(val label: String) {
  PENDING("Pending Approval"),
  APPROVED("Approved"),
  DECLINED("Declined")
}

data class FacilityBooking(
  val id: String,
  val userId: String = "",
  val facilityId: String,
  val facilityName: String,
  val residentName: String,
  val unit: String,
  val date: String,
  val timeSlot: String,
  val startTime: String = "",
  val endTime: String = "",
  val startHour: Int = 8,
  val durationHours: Int = 1,
  val guestCount: Int,
  val notes: String = "",
  val confirmedTimestamp: String,
  val approvalStatus: BookingApprovalStatus = BookingApprovalStatus.APPROVED
)

data class BookingSlotLock(
  val id: String,
  val facilityId: String,
  val dateKey: String,
  val timeSlot: String,
  val bookingId: String,
  val bookedByUid: String
)

data class VendorItem(
  val id: String,
  val name: String,
  val specialty: CaseCategory,
  val discipline: String = specialty.label,
  val rating: Float = 0f,
  val reviewCount: Int = 0,
  val activeJobsCount: Int = 0,
  val phone: String,
  val phoneNumber: String = phone,
  val email: String,
  val leadTech: String
) {
  val effectiveDiscipline: String
    get() = discipline.ifBlank { specialty.label }
}

enum class NoticeRecordType(val label: String) {
  ANNOUNCEMENT("Announcement"),
  ACTIVITY("Activity")
}

enum class NoticeTriggerEvent(val label: String) {
  MANUAL_ADVISORY("Announcement"),
  CASE_CREATED("Case Submitted"),
  CASE_UPDATED("Case Updated"),
  CASE_VENDOR_ASSIGNED("Contractor Dispatched"),
  CASE_STATUS_UPDATED("Case Status Updated"),
  CASE_NOTE_ADDED("Case Note Added"),
  CASE_REVIEWED("Service Reviewed"),
  AMENITY_BOOKED("Amenity Requested"),
  AMENITY_APPROVED("Amenity Approved"),
  AMENITY_DECLINED("Amenity Declined"),
  AMENITY_CANCELLED("Booking Cancelled"),
  EVENT_SCHEDULED("Event Scheduled")
}

enum class NoticeRelatedEntityType {
  ADVISORY,
  CASE,
  BOOKING,
  EVENT
}

data class ReminderItem(
  val id: String,
  val title: String,
  val message: String,
  val targetAudience: String,
  val urgency: String,
  val timestamp: String,
  val isBroadcast: Boolean = false,
  val recordType: NoticeRecordType = NoticeRecordType.ACTIVITY,
  val triggerEvent: NoticeTriggerEvent = NoticeTriggerEvent.MANUAL_ADVISORY,
  val createdByUid: String = "",
  val createdByName: String = "",
  val createdByEmail: String = "",
  val createdByRole: UserRole = UserRole.MANAGER,
  val targetEmail: String? = null,
  val targetEmails: List<String> = emptyList(),
  val targetUnit: String? = null,
  val targetUnits: List<String> = emptyList(),
  val relatedEntityType: NoticeRelatedEntityType = NoticeRelatedEntityType.ADVISORY,
  val relatedEntityId: String? = null,
  val createdAtEpochMs: Long = 0L
) {
  val displayUrgency: String
    get() = if (urgency.equals("Advisory", ignoreCase = true)) "Announcement" else urgency
}

enum class PropertyStatus(val label: String) {
  AVAILABLE("Available"),
  OCCUPIED("Occupied"),
  UNDER_MAINTENANCE("Under Maintenance")
}

data class PropertyArchitecture(
  val bedrooms: Int,
  val bathrooms: Float,
  val sqFt: Int,
  val floorLevel: String,
  val viewType: String,
  val sqMeters: Int = sqFt
) {
  val effectiveSqMeters: Int
    get() = if (sqMeters > 0) sqMeters else sqFt
}

data class AssignedTenantInfo(
  val name: String,
  val email: String,
  val leaseStartDate: String,
  val leaseEndDate: String,
  val inviteCode: String,
  val inviteLink: String,
  val isClaimed: Boolean = false,
  val assignedTimestamp: String
)

data class PropertyItem(
  val id: String,
  val unitNumber: String,
  val pavilionWing: String = "",
  val architecture: PropertyArchitecture,
  val parkingSpots: List<String> = emptyList(),
  val amenities: List<String> = emptyList(),
  val monthlyRental: String,
  val status: PropertyStatus = PropertyStatus.AVAILABLE,
  val assignedTenant: AssignedTenantInfo? = null,
  val listedDate: String,
  val customDescription: String? = null,
  val customSize: String? = null
) {
  val description: String
    get() = customDescription ?: buildString {
      append("${architecture.bedrooms} Bed, ${architecture.bathrooms} Bath")
      if (architecture.floorLevel.isNotBlank()) append(" • ${architecture.floorLevel}")
    }

  val size: String
    get() = customSize ?: "${architecture.effectiveSqMeters} sq m"
}

data class TenantProfile(
  val id: String,
  val firstName: String = "",
  val lastName: String = "",
  val fullName: String = if (firstName.isNotEmpty() || lastName.isNotEmpty()) "$firstName $lastName".trim() else "Resident",
  val nationalId: String = "",
  val address: String = "",
  val email: String,
  val phone: String,
  val profilePhoto: String = "",
  val emergencyContactName: String = "",
  val emergencyContactPhone: String = "",
  val assignedPropertyId: String? = null,
  val assignedPropertyUnit: String? = null,
  val leaseStartDate: String? = null,
  val leaseEndDate: String? = null,
  val inviteCode: String = "",
  val isAppRegistered: Boolean = false,
  val createdAt: String = ""
)

data class EstateConfigOptions(
  val id: String = "options",
  val floorElevations: List<String> = DEFAULT_FLOOR_ELEVATIONS,
  val contractorDisciplines: List<String> = DEFAULT_CONTRACTOR_DISCIPLINES,
  val amenityCategoryTags: List<String> = DEFAULT_AMENITY_CATEGORY_TAGS,
  val amenityTimeSlots: List<String> = DEFAULT_AMENITY_TIME_SLOTS,
  val caseDisciplines: List<String> = DEFAULT_CASE_DISCIPLINES,
  val engineeringDisciplines: List<String> = DEFAULT_ENGINEERING_DISCIPLINES
) {
  companion object {
    val DEFAULT_FLOOR_ELEVATIONS = listOf("1 Story", "2 Story", "3 Story")
    val DEFAULT_CONTRACTOR_DISCIPLINES = CaseCategory.entries.map { it.label }
    val DEFAULT_AMENITY_CATEGORY_TAGS = listOf("Wellness", "Recreation", "Dining & Lounge", "Sports", "Business", "Estate Amenity")
    val DEFAULT_AMENITY_TIME_SLOTS = listOf(
      "08:00 AM - 10:00 AM",
      "10:00 AM - 12:00 PM",
      "12:00 PM - 02:00 PM",
      "02:00 PM - 04:00 PM",
      "04:00 PM - 06:00 PM",
      "06:00 PM - 08:00 PM",
      "08:00 PM - 10:00 PM"
    )
    val DEFAULT_CASE_DISCIPLINES = CaseCategory.entries.map { it.label }
    val DEFAULT_ENGINEERING_DISCIPLINES = MaintenanceDiscipline.entries.map { it.label }
  }
}

data class ManagerInvite(
  val id: String = "",
  val email: String = "",
  val displayName: String = "",
  val invitedByEmail: String = "",
  val invitedDate: String = "",
  val status: String = "PENDING"
)

enum class CalendarEventType(val label: String) {
  MAINTENANCE("Maintenance"),
  BOOKING("Amenities"),
  CONTRACT("Contracts"),
  CASE("Cases")
}

data class CalendarEventItem(
  val id: String,
  val sourceId: String,
  val type: CalendarEventType,
  val dateKey: String,
  val displayTime: String,
  val title: String,
  val subtitle: String,
  val statusLabel: String,
  val isCompleted: Boolean = false,
  val isUrgent: Boolean = false
)

data class CalendarDayCell(
  val dayOfMonth: Int,
  val dateKey: String,
  val isCurrentMonth: Boolean,
  val isToday: Boolean
)

object EstateDateUtils {
  private val keyFormat get() = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
  private val displayFormat get() = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
  private val fullHeaderFormat get() = java.text.SimpleDateFormat("EEEE, MMM dd, yyyy", java.util.Locale.US)
  private val monthYearFormat get() = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US)
  private val shortDayFormat get() = java.text.SimpleDateFormat("EEE (MMM dd)", java.util.Locale.US)

  fun todayKey(): String = keyFormat.format(java.util.Date())

  fun todayDisplay(): String = displayFormat.format(java.util.Date())

  fun offsetDisplay(daysOffset: Int): String {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, daysOffset)
    return displayFormat.format(cal.time)
  }

  fun offsetKey(daysOffset: Int): String {
    val cal = java.util.Calendar.getInstance()
    cal.add(java.util.Calendar.DAY_OF_YEAR, daysOffset)
    return keyFormat.format(cal.time)
  }

  fun parseKeyToYearMonth(dateKey: String): Pair<Int, Int> {
    return try {
      val date = keyFormat.parse(dateKey) ?: java.util.Date()
      val cal = java.util.Calendar.getInstance().apply { time = date }
      cal.get(java.util.Calendar.YEAR) to cal.get(java.util.Calendar.MONTH)
    } catch (e: Exception) {
      val cal = java.util.Calendar.getInstance()
      cal.get(java.util.Calendar.YEAR) to cal.get(java.util.Calendar.MONTH)
    }
  }

  fun displayFromKey(dateKey: String): String {
    return try {
      val date = keyFormat.parse(dateKey) ?: return dateKey
      displayFormat.format(date)
    } catch (e: Exception) {
      dateKey
    }
  }

  fun fullHeaderFromKey(dateKey: String): String {
    return try {
      val date = keyFormat.parse(dateKey) ?: return dateKey
      fullHeaderFormat.format(date)
    } catch (e: Exception) {
      dateKey
    }
  }

  fun shortChipLabelFromDisplay(displayDate: String): String {
    val key = toDateKey(displayDate) ?: return displayDate
    val today = todayKey()
    val tomorrow = offsetKey(1)
    return when (key) {
      today -> "Today (${displayDate.substringBefore(",")})"
      tomorrow -> "Tomorrow (${displayDate.substringBefore(",")})"
      else -> try {
        val d = keyFormat.parse(key)
        if (d != null) shortDayFormat.format(d) else displayDate
      } catch (e: Exception) {
        displayDate
      }
    }
  }

  fun monthTitle(year: Int, monthZeroBased: Int): String {
    val cal = java.util.Calendar.getInstance()
    cal.set(year, monthZeroBased, 1)
    return monthYearFormat.format(cal.time)
  }

  fun buildMonthCells(year: Int, monthZeroBased: Int): List<CalendarDayCell> {
    val today = todayKey()
    val cal = java.util.Calendar.getInstance()
    cal.set(year, monthZeroBased, 1, 0, 0, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)

    val firstDayOfWeekOffset = cal.get(java.util.Calendar.DAY_OF_WEEK) - java.util.Calendar.SUNDAY
    val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    val totalCells = if (firstDayOfWeekOffset + daysInMonth > 35) 42 else 35

    cal.add(java.util.Calendar.DAY_OF_MONTH, -firstDayOfWeekOffset)
    val cells = mutableListOf<CalendarDayCell>()
    for (i in 0 until totalCells) {
      val cellYear = cal.get(java.util.Calendar.YEAR)
      val cellMonth = cal.get(java.util.Calendar.MONTH)
      val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
      val key = keyFormat.format(cal.time)
      cells.add(
        CalendarDayCell(
          dayOfMonth = day,
          dateKey = key,
          isCurrentMonth = (cellYear == year && cellMonth == monthZeroBased),
          isToday = (key == today)
        )
      )
      cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
    }
    return cells
  }

  fun toDateKey(rawDate: String): String? {
    val trimmed = rawDate.trim()
    if (trimmed.isEmpty()) return null

    val parenRegex = Regex("\\(([A-Za-z]+\\s+\\d{1,2})\\)")
    val parenMatch = parenRegex.find(trimmed)
    if (parenMatch != null) {
      val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
      val candidate = "${parenMatch.groupValues[1]}, $currentYear"
      toDateKey(candidate)?.let { return it }
    }
    if (trimmed.startsWith("Today", ignoreCase = true)) return todayKey()
    if (trimmed.startsWith("Tomorrow", ignoreCase = true)) return offsetKey(1)

    val patterns = listOf(
      "yyyy-MM-dd",
      "MMM dd, yyyy",
      "MMM d, yyyy",
      "MMMM dd, yyyy",
      "MMMM d, yyyy",
      "MM/dd/yyyy",
      "dd/MM/yyyy"
    )
    for (pattern in patterns) {
      try {
        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
          isLenient = false
        }
        val parsed = sdf.parse(trimmed)
        if (parsed != null) {
          return keyFormat.format(parsed)
        }
      } catch (_: Exception) {
      }
    }
    return null
  }

  fun addMonthsDisplay(rawDate: String, months: Int): String {
    val key = toDateKey(rawDate)
    val cal = java.util.Calendar.getInstance()
    if (key != null) {
      val parsed = keyFormat.parse(key)
      if (parsed != null) cal.time = parsed
    }
    cal.add(java.util.Calendar.MONTH, months)
    return displayFormat.format(cal.time)
  }

  fun daysUntilDisplay(rawDate: String): Int? {
    val key = toDateKey(rawDate) ?: return null
    return try {
      val target = keyFormat.parse(key) ?: return null
      val today = keyFormat.parse(todayKey()) ?: return null
      val diffMs = target.time - today.time
      java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs).toInt().coerceAtLeast(0)
    } catch (_: Exception) {
      null
    }
  }

  fun daysBetweenDisplay(startRaw: String, endRaw: String): Int? {
    val startKey = toDateKey(startRaw) ?: return null
    val endKey = toDateKey(endRaw) ?: return null
    return try {
      val start = keyFormat.parse(startKey) ?: return null
      val end = keyFormat.parse(endKey) ?: return null
      val diffMs = end.time - start.time
      java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs).toInt().coerceAtLeast(0)
    } catch (_: Exception) {
      null
    }
  }

  fun formatHourLabel(hour24: Int): String {
    val normalized = ((hour24 % 24) + 24) % 24
    val amPm = if (normalized < 12) "AM" else "PM"
    val h12 = when {
      normalized == 0 -> 12
      normalized > 12 -> normalized - 12
      else -> normalized
    }
    return String.format(java.util.Locale.US, "%02d:00 %s", h12, amPm)
  }

  fun formatHourWindow(startHour: Int, durationHours: Int): String {
    return "${formatHourLabel(startHour)} - ${formatHourLabel(startHour + durationHours)}"
  }

  fun parseTimeRangeHours(timeSlot: String): IntRange? {
    val regex = Regex("(\\d{1,2}):(\\d{2})\\s*(AM|PM)", RegexOption.IGNORE_CASE)
    val matches = regex.findAll(timeSlot).toList()
    if (matches.size < 2) return null
    fun toHour24(m: MatchResult): Int {
      val rawH = m.groupValues[1].toIntOrNull() ?: return 8
      val ap = m.groupValues[3].uppercase()
      return when {
        ap == "AM" && rawH == 12 -> 0
        ap == "PM" && rawH < 12 -> rawH + 12
        else -> rawH
      }
    }
    val startH = toHour24(matches[0])
    val endH = toHour24(matches[1])
    return if (endH > startH) startH until endH else startH..startH
  }
}
