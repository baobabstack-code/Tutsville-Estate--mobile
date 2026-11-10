package com.example.tutsville.data.model

enum class UserRole(val label: String, val badge: String) {
  MANAGER("Property Manager", "Admin"),
  TENANT("Resident / Tenant", "Unit 402"),
  VENDOR("Certified Vendor", "Apex HVAC")
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
  val title: String,
  val description: String,
  val category: CaseCategory,
  val priority: CasePriority,
  val location: String,
  val reportedBy: String,
  val reportedDate: String,
  val assignedVendorId: String? = null,
  val assignedVendorName: String? = null,
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
  val title: String,
  val counterparty: String,
  val type: ContractType,
  val annualValue: String,
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
  val listedDate: String
)

