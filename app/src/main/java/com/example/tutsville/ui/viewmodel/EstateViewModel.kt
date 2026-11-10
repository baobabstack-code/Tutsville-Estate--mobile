package com.example.tutsville.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutsville.data.model.*
import com.example.tutsville.data.repository.EstateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EstateViewModel(
  private val repository: EstateRepository = EstateRepository()
) : ViewModel() {

  // Current Role Switcher State
  private val _currentRole = MutableStateFlow(UserRole.MANAGER)
  val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

  // Selected Navigation Tab index
  private val _selectedTab = MutableStateFlow(0)
  val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

  // Repository Data Flows
  val cases: StateFlow<List<CaseItem>> = repository.cases
  val maintenances: StateFlow<List<MaintenanceEvent>> = repository.maintenances
  val facilities: StateFlow<List<Facility>> = repository.facilities
  val bookings: StateFlow<List<FacilityBooking>> = repository.bookings
  val contracts: StateFlow<List<ContractItem>> = repository.contracts
  val vendors: StateFlow<List<VendorItem>> = repository.vendors
  val reminders: StateFlow<List<ReminderItem>> = repository.reminders
  val properties: StateFlow<List<PropertyItem>> = repository.properties

  // Filter States
  private val _caseFilter = MutableStateFlow<CaseStatus?>(null)
  val caseFilter: StateFlow<CaseStatus?> = _caseFilter.asStateFlow()

  private val _contractFilter = MutableStateFlow<ContractType?>(null)
  val contractFilter: StateFlow<ContractType?> = _contractFilter.asStateFlow()

  private val _maintenanceFilter = MutableStateFlow<MaintenanceDiscipline?>(null)
  val maintenanceFilter: StateFlow<MaintenanceDiscipline?> = _maintenanceFilter.asStateFlow()

  // Dialog & Modal Control States
  var activeReportCaseDialog = MutableStateFlow(false)
  var activeAssignVendorCase = MutableStateFlow<CaseItem?>(null)
  var activeBookFacility = MutableStateFlow<Facility?>(null)
  var activeReviewCase = MutableStateFlow<CaseItem?>(null)
  var activeSendReminderDialog = MutableStateFlow(false)
  var activeAddMaintenanceDialog = MutableStateFlow(false)
  var activeContractDetail = MutableStateFlow<ContractItem?>(null)
  var activeNotificationPanel = MutableStateFlow(false)
  var activeListPropertyDialog = MutableStateFlow(false)
  var activeAssignTenantProperty = MutableStateFlow<PropertyItem?>(null)
  var activeShareInviteDialog = MutableStateFlow<PropertyItem?>(null)

  // Transient User Feedback Message
  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  fun clearSnackbar() {
    _snackbarMessage.value = null
  }

  fun setRole(role: UserRole) {
    _currentRole.value = role
    _selectedTab.value = 0
    _snackbarMessage.value = "Switched to ${role.label} (${role.badge})"
  }

  fun setSelectedTab(index: Int) {
    _selectedTab.value = index
  }

  fun setCaseFilter(status: CaseStatus?) {
    _caseFilter.value = status
  }

  fun setContractFilter(type: ContractType?) {
    _contractFilter.value = type
  }

  fun setMaintenanceFilter(discipline: MaintenanceDiscipline?) {
    _maintenanceFilter.value = discipline
  }

  // --- Actions ---

  fun submitNewCase(
    title: String,
    description: String,
    category: CaseCategory,
    priority: CasePriority,
    location: String
  ) {
    val reporter = when (_currentRole.value) {
      UserRole.MANAGER -> "Estate Management Staff"
      UserRole.TENANT -> "Eleanor Sterling (Unit 402)"
      UserRole.VENDOR -> "Apex Lead Tech"
    }
    val created = repository.reportCase(
      title = title,
      description = description,
      category = category,
      priority = priority,
      location = location,
      reportedBy = reporter
    )
    activeReportCaseDialog.value = false
    _snackbarMessage.value = "Work order #${created.id} logged successfully"
  }

  fun assignVendor(caseId: String, vendorId: String) {
    repository.assignVendorToCase(caseId, vendorId)
    val vendor = vendors.value.find { it.id == vendorId }
    activeAssignVendorCase.value = null
    _snackbarMessage.value = "Dispatched to ${vendor?.name ?: "Vendor"} with SLA tracking"
  }

  fun advanceCaseStatus(caseId: String, currentStatus: CaseStatus) {
    val nextStatus = when (currentStatus) {
      CaseStatus.OPEN -> CaseStatus.ASSIGNED
      CaseStatus.ASSIGNED -> CaseStatus.IN_PROGRESS
      CaseStatus.IN_PROGRESS -> CaseStatus.RESOLVED
      CaseStatus.RESOLVED -> CaseStatus.RESOLVED
    }
    repository.updateCaseStatus(caseId, nextStatus)
    _snackbarMessage.value = "Case status updated to ${nextStatus.label}"
  }

  fun submitReview(caseId: String, rating: Int, tags: List<String>, comment: String) {
    repository.submitCaseReview(caseId, rating, tags, comment)
    activeReviewCase.value = null
    _snackbarMessage.value = "Thank you! Service review ($rating ★) recorded"
  }

  fun bookFacilitySlot(
    facilityId: String,
    date: String,
    timeSlot: String,
    guestCount: Int,
    notes: String
  ) {
    val resident = when (_currentRole.value) {
      UserRole.TENANT -> "Eleanor Sterling"
      UserRole.MANAGER -> "Estate Manager (Reserved for VIP)"
      UserRole.VENDOR -> "Vendor Maintenance Team"
    }
    val unit = if (_currentRole.value == UserRole.TENANT) "Unit 402" else "Admin Office"

    val booking = repository.bookFacility(
      facilityId = facilityId,
      date = date,
      timeSlot = timeSlot,
      residentName = resident,
      unit = unit,
      guestCount = guestCount,
      notes = notes
    )
    activeBookFacility.value = null
    _snackbarMessage.value = "Reservation confirmed for ${booking.facilityName} (${booking.timeSlot})"
  }

  fun cancelBooking(bookingId: String) {
    repository.cancelBooking(bookingId)
    _snackbarMessage.value = "Booking reservation cancelled"
  }

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
    repository.scheduleMaintenance(
      title = title,
      discipline = discipline,
      date = date,
      time = time,
      contractor = contractor,
      targetArea = targetArea,
      intervalMonths = intervalMonths,
      notes = notes
    )
    activeAddMaintenanceDialog.value = false
    _snackbarMessage.value = "Maintenance added to estate calendar"
  }

  fun toggleMaintenanceCompletion(event: MaintenanceEvent) {
    val newStatus = if (event.status == MaintenanceStatus.COMPLETED) {
      MaintenanceStatus.SCHEDULED
    } else {
      MaintenanceStatus.COMPLETED
    }
    repository.toggleMaintenanceStatus(event.id, newStatus)
    _snackbarMessage.value = "${event.title} marked as ${newStatus.label}"
  }

  fun sendReminder(
    title: String,
    message: String,
    targetAudience: String,
    urgency: String,
    isBroadcast: Boolean
  ) {
    repository.sendReminder(
      title = title,
      message = message,
      targetAudience = targetAudience,
      urgency = urgency,
      isBroadcast = isBroadcast
    )
    activeSendReminderDialog.value = false
    _snackbarMessage.value = "Notice broadcast dispatched to $targetAudience"
  }

  fun triggerContractRundownNotice(contractId: String) {
    repository.sendContractRundownNotification(contractId)
    _snackbarMessage.value = "Renewal reminder & rundown notice dispatched!"
  }

  fun renewContract(contractId: String) {
    repository.renewContract(contractId, 1)
    activeContractDetail.value = null
    _snackbarMessage.value = "Contract renewed for 1 additional year!"
  }

  // --- Property & Tenant Management ---
  fun listProperty(
    unitNumber: String,
    pavilionWing: String,
    architecture: PropertyArchitecture,
    parkingSpots: List<String>,
    amenities: List<String>,
    monthlyRental: String
  ) {
    val created = repository.listProperty(
      unitNumber = unitNumber,
      pavilionWing = pavilionWing,
      architecture = architecture,
      parkingSpots = parkingSpots,
      amenities = amenities,
      monthlyRental = monthlyRental
    )
    activeListPropertyDialog.value = false
    _snackbarMessage.value = "${created.unitNumber} listed successfully under $pavilionWing"
  }

  fun assignPropertyToTenant(
    propertyId: String,
    tenantName: String,
    tenantEmail: String,
    leaseStartDate: String,
    leaseEndDate: String
  ) {
    val tenantInfo = repository.assignPropertyToTenant(
      propertyId = propertyId,
      tenantName = tenantName,
      tenantEmail = tenantEmail,
      leaseStartDate = leaseStartDate,
      leaseEndDate = leaseEndDate
    )
    val updatedProp = properties.value.find { it.id == propertyId }
    activeAssignTenantProperty.value = null
    // Automatically trigger share invite dialog for manager to copy/share the link
    activeShareInviteDialog.value = updatedProp
    _snackbarMessage.value = "Assigned to ${tenantInfo.name}! Invitation link generated."
  }

  fun vacateProperty(propertyId: String) {
    repository.vacateProperty(propertyId)
    _snackbarMessage.value = "Property marked as vacant and available for lease"
  }

  fun claimPropertyForEmail(email: String) {
    val prop = repository.claimPropertyInvite(email)
    if (prop != null) {
      _snackbarMessage.value = "Welcome home! Linked to ${prop.unitNumber} (${prop.pavilionWing})"
    }
  }
}

