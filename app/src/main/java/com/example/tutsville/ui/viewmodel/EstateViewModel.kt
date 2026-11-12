package com.example.tutsville.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tutsville.data.model.*
import com.example.tutsville.data.repository.EstateRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EstateViewModel(
  private val repository: EstateRepository = EstateRepository.getInstance()
) : ViewModel() {

  val isLoading: StateFlow<Boolean> = repository.isLoadingFlow

  // Automatic Role Detection State
  private var currentUserEmail: String? = null
  private val _currentRole = MutableStateFlow(UserRole.MANAGER)
  val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

  private val _currentTenant = MutableStateFlow<TenantProfile?>(null)
  val currentTenant: StateFlow<TenantProfile?> = _currentTenant.asStateFlow()

  init {
    viewModelScope.launch {
      repository.tenants.collect { tenantList ->
        recomputeUserRole(currentUserEmail, tenantList)
      }
    }
  }

  fun setUserEmail(email: String?) {
    currentUserEmail = email
    recomputeUserRole(email, repository.tenants.value)
    if (!email.isNullOrBlank()) {
      claimPropertyForEmail(email)
    }
  }

  private fun recomputeUserRole(email: String?, tenantList: List<TenantProfile>) {
    if (email.isNullOrBlank()) {
      _currentRole.value = UserRole.MANAGER
      _currentTenant.value = null
      return
    }
    val normalized = email.trim().lowercase()
    val matched = tenantList.find { it.email.equals(normalized, ignoreCase = true) }
    if (matched != null) {
      _currentRole.value = UserRole.TENANT
      _currentTenant.value = matched
    } else {
      _currentRole.value = UserRole.MANAGER
      _currentTenant.value = null
    }
  }

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
  val tenants: StateFlow<List<TenantProfile>> = repository.tenants

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
  var activeCreateTenantDialog = MutableStateFlow(false)
  var activeAssignPropertyToTenant = MutableStateFlow<TenantProfile?>(null)
  var activeTenantInviteSheet = MutableStateFlow<TenantProfile?>(null)
  var activeAddFacilityDialog = MutableStateFlow(false)
  var activeAddVendorDialog = MutableStateFlow(false)
  var activeAddContractDialog = MutableStateFlow(false)

  // Transient User Feedback Message
  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  fun clearSnackbar() {
    _snackbarMessage.value = null
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

  fun syncOrSeedFirestore(force: Boolean = true) {
    repository.seedDatabase(force = force)
    _snackbarMessage.value = if (force) "Seeding all 9 collections to Cloud Firestore..." else "Syncing with Cloud Firestore..."
  }

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
    repository.claimTenantInvite(email)
    if (prop != null) {
      _snackbarMessage.value = "Welcome home! Linked to ${prop.unitNumber} (${prop.pavilionWing})"
    }
  }

  // --- Tenant Manager Operations ---
  fun createTenant(
    fullName: String,
    email: String,
    phone: String,
    emergencyContactName: String,
    emergencyContactPhone: String
  ) {
    val tenant = repository.createTenant(
      fullName = fullName,
      email = email,
      phone = phone,
      emergencyContactName = emergencyContactName,
      emergencyContactPhone = emergencyContactPhone
    )
    activeCreateTenantDialog.value = false
    // Immediately open share invite sheet so manager can send download link + code
    activeTenantInviteSheet.value = tenant
    _snackbarMessage.value = "Tenant profile created for ${tenant.fullName}!"
  }

  fun assignTenantToPropertyById(
    tenantId: String,
    propertyId: String,
    leaseStartDate: String,
    leaseEndDate: String
  ) {
    val updated = repository.assignTenantToPropertyById(
      tenantId = tenantId,
      propertyId = propertyId,
      leaseStartDate = leaseStartDate,
      leaseEndDate = leaseEndDate
    )
    activeAssignPropertyToTenant.value = null
    if (updated != null) {
      activeTenantInviteSheet.value = updated
      _snackbarMessage.value = "Assigned ${updated.fullName} to ${updated.assignedPropertyUnit}!"
    }
  }

  fun unassignTenant(tenantId: String) {
    repository.unassignTenant(tenantId)
    _snackbarMessage.value = "Tenant unassigned from property"
  }

  fun claimTenantInvite(email: String, code: String? = null): Boolean {
    val tenant = repository.claimTenantInvite(email, code)
    return if (tenant != null) {
      _snackbarMessage.value = "Verified! Welcome ${tenant.fullName} to ${tenant.assignedPropertyUnit ?: "Tutsville Estate"}"
      true
    } else false
  }

  // --- Facility Management ---
  fun submitNewFacility(
    name: String,
    tag: String,
    capacity: Int,
    location: String,
    description: String,
    amenities: List<String>,
    slots: List<String>,
    rules: String
  ) {
    val created = repository.createFacility(name, tag, capacity, location, description, amenities, slots, rules)
    activeAddFacilityDialog.value = false
    _snackbarMessage.value = "Amenity '${created.name}' created and published!"
  }

  fun deleteFacility(id: String) {
    repository.deleteFacility(id)
    _snackbarMessage.value = "Facility removed"
  }

  // --- Vendor Management ---
  fun submitNewVendor(
    name: String,
    specialty: CaseCategory,
    phone: String,
    email: String,
    leadTech: String
  ) {
    val created = repository.createVendor(name, specialty, phone, email, leadTech)
    activeAddVendorDialog.value = false
    _snackbarMessage.value = "Vendor partner '${created.name}' onboarded!"
  }

  fun deleteVendor(id: String) {
    repository.deleteVendor(id)
    _snackbarMessage.value = "Vendor partner removed"
  }

  // --- Contract Management ---
  fun submitNewContract(
    title: String,
    counterparty: String,
    type: ContractType,
    annualValue: String,
    startDate: String,
    expiryDate: String,
    daysRemaining: Int,
    renewalTerms: String,
    contactPerson: String,
    contactEmail: String
  ) {
    val created = repository.createContract(title, counterparty, type, annualValue, startDate, expiryDate, daysRemaining, renewalTerms, contactPerson, contactEmail)
    activeAddContractDialog.value = false
    _snackbarMessage.value = "Contract agreement '${created.title}' registered!"
  }

  fun deleteContract(id: String) {
    repository.deleteContract(id)
    _snackbarMessage.value = "Contract agreement removed"
  }

  fun deleteTenant(id: String) {
    repository.deleteTenant(id)
    _snackbarMessage.value = "Resident profile deleted permanently"
  }

  fun deleteProperty(id: String) {
    repository.deleteProperty(id)
    _snackbarMessage.value = "Unit property removed permanently"
  }

  fun deleteCase(id: String) {
    repository.deleteCase(id)
    _snackbarMessage.value = "Work order / case removed permanently"
  }
}


