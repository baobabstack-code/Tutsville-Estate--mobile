package com.example.tutsville.data.model

enum class UserRole(val label: String, val badge: String) {
  MANAGER("Property Manager", "Admin"),
  TENANT("Resident / Tenant", "Resident"),
  VENDOR("Certified Vendor", "Vendor")
}

enum class CaseCategory(val label: String) {
  PLUMBING("Plumbing & Drains"),
  ELECTRICAL("Electrical & Power"),
  HVAC("Heating & Air Conditioning"),
  STRUCTURAL("Doors, Windows & Locks"),
  APPLIANCE("Kitchen & Laundry"),
  GROUNDS("Common Areas & Grounds"),
  SECURITY("Access & Surveillance")
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

data class CaseItem(
  val id: String,
  val number: String = id,
  val title: String,
  val description: String,
  val category: CaseCategory,
  val priority: CasePriority,
  val location: String,
  val unitNumber: String = location,
  val reportedBy: String,
  val tenantNumber: String = reportedBy,
  val reportedDate: String,
  val date: String = reportedDate,
  val assignedVendorId: String? = null,
  val assignedVendorName: String? = null,
  val vendor: String = assignedVendorName ?: "Unassigned",
  val status: CaseStatus = CaseStatus.OPEN,
  val rating: Int? = null,
  val reviewComment: String? = null,
  val reviewTags: List<String> = emptyList(),
  val photoNote: String? = null
)

enum class MaintenanceDiscipline(val label: String) {
  HVAC("HVAC & Ventilation"),
  ELEVATORS("Lifts & Vertical Transit"),
  FIRE_LIFE("Fire & Life Safety"),
  ELECTRICAL("Transformers & Generators"),
  PLUMBING("Boilers & Pressure Pumps"),
  GROUNDS("Landscaping & Water Features")
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
  val scheduledDate: String,
  val scheduledTime: String,
  val contractorName: String,
  val targetArea: String,
  val intervalMonths: Int,
  val status: MaintenanceStatus = MaintenanceStatus.SCHEDULED,
  val notes: String
)

enum class ContractType(val label: String) {
  VENDOR_SLA("Vendor SLA"),
  TENANT_LEASE("Tenant Lease"),
  EQUIPMENT_WARRANTY("Equipment Warranty")
}

enum class ContractUrgency {
  HEALTHY,
  EXPIRING_60_DAYS,
  EXPIRING_30_DAYS,
  URGENT_CRITICAL
}

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
  val isNotificationSent: Boolean = false
) {
  val urgency: ContractUrgency
    get() = when {
      daysRemaining <= 14 -> ContractUrgency.URGENT_CRITICAL
      daysRemaining <= 30 -> ContractUrgency.EXPIRING_30_DAYS
      daysRemaining <= 60 -> ContractUrgency.EXPIRING_60_DAYS
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

data class FacilityBooking(
  val id: String,
  val facilityId: String,
  val facilityName: String,
  val residentName: String,
  val unit: String,
  val date: String,
  val timeSlot: String,
  val guestCount: Int,
  val notes: String = "",
  val confirmedTimestamp: String
)

data class VendorItem(
  val id: String,
  val name: String,
  val specialty: CaseCategory,
  val rating: Float,
  val reviewCount: Int,
  val activeJobsCount: Int,
  val phone: String,
  val phoneNumber: String = phone,
  val email: String,
  val leadTech: String
)

data class ReminderItem(
  val id: String,
  val title: String,
  val message: String,
  val targetAudience: String,
  val urgency: String,
  val timestamp: String,
  val isBroadcast: Boolean = false
)

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
  val viewType: String
)

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
  val pavilionWing: String,
  val architecture: PropertyArchitecture,
  val parkingSpots: List<String>,
  val amenities: List<String>,
  val monthlyRental: String,
  val status: PropertyStatus = PropertyStatus.AVAILABLE,
  val assignedTenant: AssignedTenantInfo? = null,
  val listedDate: String,
  val customDescription: String? = null,
  val customSize: String? = null
) {
  val description: String
    get() = customDescription ?: "$pavilionWing • ${architecture.bedrooms} Bed, ${architecture.bathrooms} Bath"

  val size: String
    get() = customSize ?: "${architecture.sqFt} sq ft"
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

    // Handle "Today (Nov 09)" or "Tomorrow (Nov 10)" legacy/chip formats
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
}



