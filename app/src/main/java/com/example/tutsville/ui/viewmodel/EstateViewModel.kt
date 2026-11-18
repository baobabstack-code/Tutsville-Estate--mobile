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
  val isRoleResolved: StateFlow<Boolean> = repository.isRoleResolved
  val isUsingFirestore: Boolean get() = repository.isUsingFirestore

  // Automatic Role Detection State
  private val _currentUserEmail = MutableStateFlow<String?>(null)
  val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

  private val _currentUserDisplayName = MutableStateFlow<String?>(null)
  val currentUserDisplayName: StateFlow<String?> = _currentUserDisplayName.asStateFlow()

  private val _currentRole = MutableStateFlow(
    if (repository.isUsingFirestore) UserRole.UNREGISTERED else UserRole.MANAGER
  )
  val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

  private val _currentTenant = MutableStateFlow<TenantProfile?>(null)
  val currentTenant: StateFlow<TenantProfile?> = _currentTenant.asStateFlow()

  // Repository Data Flows
  val cases: StateFlow<List<CaseItem>> = repository.cases
  val maintenances: StateFlow<List<MaintenanceEvent>> = repository.maintenances
  val facilities: StateFlow<List<Facility>> = repository.facilities
  val bookings: StateFlow<List<FacilityBooking>> = repository.bookings
  val slotLocks: StateFlow<List<BookingSlotLock>> = repository.slotLocks
  val contracts: StateFlow<List<ContractItem>> = repository.contracts
  val vendors: StateFlow<List<VendorItem>> = repository.vendors
  val reminders: StateFlow<List<ReminderItem>> = repository.reminders
  val properties: StateFlow<List<PropertyItem>> = repository.properties
  val tenants: StateFlow<List<TenantProfile>> = repository.tenants

  // Transient User Feedback Message
  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private val _isBookingSubmitting = MutableStateFlow(false)
  val isBookingSubmitting: StateFlow<Boolean> = _isBookingSubmitting.asStateFlow()

  init {
    viewModelScope.launch {
      combine(
        _currentUserEmail,
        repository.tenants,
        repository.isCurrentUserAdmin,
        repository.isRoleResolved
      ) { email, tenantList, isAdmin, resolved ->
        RoleInputs(email, tenantList, isAdmin, resolved)
      }.collect { (email, tenantList, isAdmin, resolved) ->
        recomputeUserRole(email, tenantList, isAdmin, resolved)
      }
    }

    viewModelScope.launch {
      repository.lastError.collect { err ->
        if (!err.isNullOrBlank()) {
          _snackbarMessage.value = err
          repository.clearError()
        }
      }
    }
  }

  fun setUserEmail(email: String?) {
    setUserIdentity(email, _currentUserDisplayName.value)
  }

  fun setUserIdentity(email: String?, displayName: String?) {
    _currentUserEmail.value = email
    _currentUserDisplayName.value = displayName
    repository.setActorContext(displayName, email)
    recomputeUserRole(
      email = email,
      tenantList = repository.tenants.value,
      isAdmin = repository.isCurrentUserAdmin.value,
      resolved = repository.isRoleResolved.value
    )
    if (!email.isNullOrBlank()) {
      claimPropertyForEmail(email)
    }
  }

  fun refreshAccess() {
    repository.refreshAuthAndListeners()
    val email = _currentUserEmail.value
    if (!email.isNullOrBlank()) {
      claimPropertyForEmail(email)
    }
  }

  private fun recomputeUserRole(
    email: String?,
    tenantList: List<TenantProfile>,
    isAdmin: Boolean,
    resolved: Boolean
  ) {
    if (!repository.isUsingFirestore) {
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
      return
    }

    if (!resolved) {
      _currentRole.value = UserRole.UNREGISTERED
      _currentTenant.value = null
      return
    }

    if (isAdmin) {
      _currentRole.value = UserRole.MANAGER
      _currentTenant.value = null
      return
    }

    val normalized = email?.trim()?.lowercase().orEmpty()
    val matched = if (normalized.isNotBlank()) {
      tenantList.find { it.email.equals(normalized, ignoreCase = true) }
    } else null

    if (matched != null) {
      _currentRole.value = UserRole.TENANT
      _currentTenant.value = matched
    } else {
      _currentRole.value = UserRole.UNREGISTERED
      _currentTenant.value = null
    }
  }

  // Selected Navigation Tab index
  private val _selectedTab = MutableStateFlow(0)
  val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

  // Filter States
  private val _caseFilter = MutableStateFlow<CaseStatus?>(null)
  val caseFilter: StateFlow<CaseStatus?> = _caseFilter.asStateFlow()

  private val _contractFilter = MutableStateFlow<ContractType?>(null)
  val contractFilter: StateFlow<ContractType?> = _contractFilter.asStateFlow()

  private val _maintenanceFilter = MutableStateFlow<MaintenanceDiscipline?>(null)
  val maintenanceFilter: StateFlow<MaintenanceDiscipline?> = _maintenanceFilter.asStateFlow()

  // Synchronized Calendar States
  private val _selectedCalendarDateKey = MutableStateFlow(EstateDateUtils.todayKey())
  val selectedCalendarDateKey: StateFlow<String> = _selectedCalendarDateKey.asStateFlow()

  private val initialYearMonth = EstateDateUtils.parseKeyToYearMonth(EstateDateUtils.todayKey())
  private val _selectedCalendarYear = MutableStateFlow(initialYearMonth.first)
  val selectedCalendarYear: StateFlow<Int> = _selectedCalendarYear.asStateFlow()

  private val _selectedCalendarMonth = MutableStateFlow(initialYearMonth.second)
  val selectedCalendarMonth: StateFlow<Int> = _selectedCalendarMonth.asStateFlow()

  private val _calendarCategoryFilter = MutableStateFlow<CalendarEventType?>(null)
  val calendarCategoryFilter: StateFlow<CalendarEventType?> = _calendarCategoryFilter.asStateFlow()

  val allCalendarEvents: StateFlow<List<CalendarEventItem>> = combine(
    combine(maintenances, bookings, contracts, cases) { m, b, c, cs ->
      Quadruple(m, b, c, cs)
    },
    currentRole,
    currentTenant,
    _currentUserEmail
  ) { (maintList, bookingList, contractList, caseList), role, tenant, email ->
    buildCalendarEvents(
      maintenances = maintList,
      bookings = bookingList,
      contracts = contractList,
      cases = caseList,
      role = role,
      tenant = tenant,
      userEmail = email
    )
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val calendarEventsByDate: StateFlow<Map<String, List<CalendarEventItem>>> = combine(
    allCalendarEvents,
    calendarCategoryFilter
  ) { events, categoryFilter ->
    val filtered = if (categoryFilter == null) events else events.filter { it.type == categoryFilter }
    filtered.groupBy { it.dateKey }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

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

  // Synced Notification State (100% Attributed DB Records)
  val readNoticeIds: StateFlow<Set<String>> = repository.readNoticeIds

  val visibleNotifications: StateFlow<List<ReminderItem>> = combine(
    reminders,
    currentRole,
    currentUserEmail,
    currentTenant
  ) { allReminders, role, userEmail, tenant ->
    val attributed = allReminders.filter {
      it.createdByName.isNotBlank() &&
        it.createdByUid.isNotBlank() &&
        (it.recordType == NoticeRecordType.ANNOUNCEMENT ||
          it.relatedEntityType == NoticeRelatedEntityType.CASE ||
          it.relatedEntityType == NoticeRelatedEntityType.BOOKING)
    }
    when (role) {
      UserRole.MANAGER -> attributed
      UserRole.TENANT -> {
        val cleanEmail = (tenant?.email ?: userEmail).orEmpty().trim().lowercase()
        attributed.filter { rem ->
          val isResidentBroadcast = rem.targetAudience.equals("All Residents & Tenants", ignoreCase = true) ||
            rem.targetAudience.equals("ALL_RESIDENTS", ignoreCase = true) ||
            rem.targetAudience.equals("ALL_USERS", ignoreCase = true) ||
            rem.targetAudience.equals("All Residents", ignoreCase = true)
          val isTargetedToMe = cleanEmail.isNotEmpty() && rem.targetEmail?.equals(cleanEmail, ignoreCase = true) == true
          isResidentBroadcast || isTargetedToMe
        }
      }
      UserRole.VENDOR, UserRole.UNREGISTERED -> emptyList()
    }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val dashboardAnnouncements: StateFlow<List<ReminderItem>> = visibleNotifications
    .map { list -> list.filter { it.recordType == NoticeRecordType.ANNOUNCEMENT } }
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val unreadNotificationCount: StateFlow<Int> = combine(
    visibleNotifications,
    readNoticeIds
  ) { visible, readIds ->
    visible.count { !readIds.contains(it.id) }
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

  fun markNoticeAsRead(noticeId: String) {
    repository.markNoticeAsRead(noticeId)
  }

  fun markAllNoticesAsRead() {
    val ids = visibleNotifications.value.map { it.id }
    repository.markAllNoticesAsRead(ids)
    _snackbarMessage.value = "All notifications marked as read"
  }

  fun deleteNotification(reminderId: String) {
    repository.deleteReminder(reminderId)
    _snackbarMessage.value = "Notification removed"
  }

  fun clearAllNotifications() {
    repository.clearAllReminders()
    _snackbarMessage.value = "All notifications and advisories cleared"
  }

  fun openNotificationTarget(
    reminder: ReminderItem,
    onNavigateToTab: (Int) -> Unit,
    onNavigateToModule: (Int) -> Unit
  ) {
    markNoticeAsRead(reminder.id)
    activeNotificationPanel.value = false
    when (reminder.relatedEntityType) {
      NoticeRelatedEntityType.CASE -> onNavigateToTab(1)
      NoticeRelatedEntityType.BOOKING -> {
        if (currentRole.value == UserRole.MANAGER) {
          onNavigateToModule(3)
        } else {
          onNavigateToTab(2)
        }
      }
      NoticeRelatedEntityType.ADVISORY -> Unit
    }
  }

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

  fun selectCalendarDate(dateKey: String) {
    _selectedCalendarDateKey.value = dateKey
    val (y, m) = EstateDateUtils.parseKeyToYearMonth(dateKey)
    _selectedCalendarYear.value = y
    _selectedCalendarMonth.value = m
  }

  fun navigateCalendarMonth(deltaMonths: Int) {
    val cal = java.util.Calendar.getInstance()
    cal.set(_selectedCalendarYear.value, _selectedCalendarMonth.value, 1)
    cal.add(java.util.Calendar.MONTH, deltaMonths)
    _selectedCalendarYear.value = cal.get(java.util.Calendar.YEAR)
    _selectedCalendarMonth.value = cal.get(java.util.Calendar.MONTH)
  }

  fun jumpCalendarToToday() {
    selectCalendarDate(EstateDateUtils.todayKey())
  }

  fun setCalendarCategoryFilter(type: CalendarEventType?) {
    _calendarCategoryFilter.value = type
  }

  fun selectedCalendarDisplayDate(): String {
    return EstateDateUtils.displayFromKey(_selectedCalendarDateKey.value)
  }

  // --- Actions ---

  fun syncFirestore() {
    repository.refreshAuthAndListeners()
    _snackbarMessage.value = "Syncing live data with Cloud Firestore..."
  }

  fun submitNewCase(
    title: String,
    description: String,
    category: CaseCategory,
    priority: CasePriority,
    location: String
  ) {
    val tenant = _currentTenant.value
    val authName = _currentUserDisplayName.value?.takeIf { it.isNotBlank() }
      ?: _currentUserEmail.value?.takeIf { it.isNotBlank() }
    val reporter = when (_currentRole.value) {
      UserRole.MANAGER -> authName ?: "Estate Manager"
      UserRole.TENANT -> tenant?.fullName?.takeIf { it.isNotBlank() } ?: authName ?: "Resident"
      UserRole.VENDOR -> authName ?: "Vendor"
      UserRole.UNREGISTERED -> authName ?: "User"
    }
    val tenantEmailForCase = if (_currentRole.value == UserRole.TENANT) {
      tenant?.email ?: _currentUserEmail.value
    } else null

    val created = repository.reportCase(
      title = title,
      description = description,
      category = category,
      priority = priority,
      location = location,
      reportedBy = reporter,
      tenantEmail = tenantEmailForCase
    )
    activeReportCaseDialog.value = false
    _snackbarMessage.value = "Case #${created.id} logged successfully"
  }

  fun updateCase(
    caseId: String,
    title: String,
    description: String,
    location: String,
    category: CaseCategory,
    priority: CasePriority
  ) {
    repository.updateCase(caseId, title, description, location, category, priority)
    _snackbarMessage.value = "Case ticket updated"
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

  fun isSlotBooked(facilityId: String, date: String, timeSlot: String): Boolean {
    return repository.isSlotBooked(facilityId, date, timeSlot)
  }

  fun bookFacilitySlot(
    facilityId: String,
    date: String,
    timeSlot: String,
    guestCount: Int,
    notes: String
  ) {
    if (_isBookingSubmitting.value) return

    val tenant = _currentTenant.value
    val authName = _currentUserDisplayName.value?.takeIf { it.isNotBlank() }
      ?: _currentUserEmail.value?.takeIf { it.isNotBlank() }
    val resident = when (_currentRole.value) {
      UserRole.TENANT -> tenant?.fullName?.takeIf { it.isNotBlank() } ?: authName ?: "Resident"
      UserRole.MANAGER -> authName ?: "Estate Manager"
      UserRole.VENDOR -> authName ?: "Vendor"
      UserRole.UNREGISTERED -> authName ?: "Guest"
    }
    val unit = if (_currentRole.value == UserRole.TENANT) {
      tenant?.assignedPropertyUnit.orEmpty()
    } else {
      "Management Office"
    }

    val normalizedKey = EstateDateUtils.toDateKey(date)
    val cleanDate = if (normalizedKey != null) EstateDateUtils.displayFromKey(normalizedKey) else date

    _isBookingSubmitting.value = true
    try {
      repository.bookFacility(
        facilityId = facilityId,
        date = cleanDate,
        timeSlot = timeSlot,
        residentName = resident,
        unit = unit,
        guestCount = guestCount,
        notes = notes,
        onResult = { result ->
          _isBookingSubmitting.value = false
          result.onSuccess { booking ->
            activeBookFacility.value = null
            _snackbarMessage.value = "Reservation confirmed for ${booking.facilityName} (${booking.timeSlot})"
          }.onFailure { err ->
            _snackbarMessage.value = err.message ?: "Selected time slot could not be reserved"
          }
        }
      )
    } catch (e: IllegalStateException) {
      _isBookingSubmitting.value = false
      _snackbarMessage.value = e.message ?: "Selected time slot is already booked"
    }
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

  fun deleteMaintenance(eventId: String) {
    repository.deleteMaintenance(eventId)
    _snackbarMessage.value = "Scheduled maintenance removed from calendar"
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

  fun signContract(contractId: String) {
    val signerName = _currentTenant.value?.fullName?.takeIf { it.isNotBlank() }
      ?: _currentUserDisplayName.value?.takeIf { it.isNotBlank() }
      ?: _currentUserEmail.value
      ?: "Resident"
    val signerEmail = _currentTenant.value?.email?.takeIf { it.isNotBlank() }
      ?: _currentUserEmail.value
      ?: ""
    val signed = repository.signTenantContract(contractId, signerName, signerEmail)
    activeContractDetail.value = null
    if (signed != null) {
      _snackbarMessage.value = "Lease contract signed & activated for ${signed.unit}"
    }
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
    if (currentRole.value != UserRole.MANAGER) {
      activeListPropertyDialog.value = false
      return
    }
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

  fun updateProperty(
    propertyId: String,
    unitNumber: String,
    pavilionWing: String,
    architecture: PropertyArchitecture,
    parkingSpots: List<String>,
    amenities: List<String>,
    monthlyRental: String
  ) {
    repository.updateProperty(
      propertyId = propertyId,
      unitNumber = unitNumber,
      pavilionWing = pavilionWing,
      architecture = architecture,
      parkingSpots = parkingSpots,
      amenities = amenities,
      monthlyRental = monthlyRental
    )
    _snackbarMessage.value = "${unitNumber.trim()} updated successfully"
  }

  fun assignPropertyToTenant(
    propertyId: String,
    tenantName: String,
    tenantEmail: String,
    leaseStartDate: String,
    leaseEndDate: String
  ) {
    val mgrName = _currentUserDisplayName.value?.takeIf { it.isNotBlank() } ?: "Estate Manager"
    val mgrEmail = _currentUserEmail.value?.trim().orEmpty()
    val tenantInfo = repository.assignPropertyToTenant(
      propertyId = propertyId,
      tenantName = tenantName,
      tenantEmail = tenantEmail,
      leaseStartDate = leaseStartDate,
      leaseEndDate = leaseEndDate,
      managerName = mgrName,
      managerEmail = mgrEmail
    )
    val updatedProp = properties.value.find { it.id == propertyId }
    activeAssignTenantProperty.value = null
    activeShareInviteDialog.value = updatedProp
    _snackbarMessage.value = "Assigned to ${tenantInfo.name} & lease contract drafted!"
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
    emergencyContactPhone: String,
    nationalId: String = "",
    address: String = ""
  ) {
    val tenant = repository.createTenant(
      fullName = fullName,
      email = email,
      phone = phone,
      emergencyContactName = emergencyContactName,
      emergencyContactPhone = emergencyContactPhone,
      nationalId = nationalId,
      address = address
    )
    activeCreateTenantDialog.value = false
    activeTenantInviteSheet.value = tenant
    _snackbarMessage.value = "Tenant profile created for ${tenant.fullName}!"
  }

  fun updateTenant(
    tenantId: String,
    firstName: String,
    lastName: String,
    nationalId: String,
    address: String,
    email: String,
    phone: String,
    emergencyContactName: String,
    emergencyContactPhone: String
  ) {
    repository.updateTenant(
      tenantId = tenantId,
      firstName = firstName,
      lastName = lastName,
      nationalId = nationalId,
      address = address,
      email = email,
      phone = phone,
      emergencyContactName = emergencyContactName,
      emergencyContactPhone = emergencyContactPhone
    )
    _snackbarMessage.value = "Tenant profile updated"
  }

  fun assignTenantToPropertyById(
    tenantId: String,
    propertyId: String,
    leaseStartDate: String,
    leaseEndDate: String
  ) {
    val mgrName = _currentUserDisplayName.value?.takeIf { it.isNotBlank() } ?: "Estate Manager"
    val mgrEmail = _currentUserEmail.value?.trim().orEmpty()
    val updated = repository.assignTenantToPropertyById(
      tenantId = tenantId,
      propertyId = propertyId,
      leaseStartDate = leaseStartDate,
      leaseEndDate = leaseEndDate,
      managerName = mgrName,
      managerEmail = mgrEmail
    )
    activeAssignPropertyToTenant.value = null
    if (updated != null) {
      activeTenantInviteSheet.value = updated
      _snackbarMessage.value = "Assigned ${updated.fullName} to ${updated.assignedPropertyUnit} & lease drafted!"
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
    val cleanSlots = slots.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleanSlots.isEmpty() || cleanSlots.any { it.length !in 3..80 }) {
      _snackbarMessage.value = "Please configure at least one valid bookable time slot (3-80 characters)."
      return
    }
    val created = repository.createFacility(name, tag, capacity, location, description, amenities, cleanSlots, rules)
    activeAddFacilityDialog.value = false
    _snackbarMessage.value = "Amenity '${created.name}' created and published!"
  }

  fun updateFacility(
    facilityId: String,
    name: String,
    tag: String,
    capacity: Int,
    location: String,
    description: String,
    amenities: List<String>,
    slots: List<String>,
    rules: String
  ) {
    val cleanSlots = slots.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (cleanSlots.isEmpty() || cleanSlots.any { it.length !in 3..80 }) {
      _snackbarMessage.value = "Please configure at least one valid bookable time slot (3-80 characters)."
      return
    }
    repository.updateFacility(facilityId, name, tag, capacity, location, description, amenities, cleanSlots, rules)
    _snackbarMessage.value = "Amenity '${name.trim()}' updated!"
  }

  fun deleteFacility(id: String) {
    repository.deleteFacility(id)
    _snackbarMessage.value = "Amenity removed"
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

  fun updateVendor(
    vendorId: String,
    name: String,
    specialty: CaseCategory,
    phone: String,
    email: String,
    leadTech: String
  ) {
    repository.updateVendor(vendorId, name, specialty, phone, email, leadTech)
    _snackbarMessage.value = "Vendor partner '${name.trim()}' updated!"
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
    contactEmail: String,
    unit: String = "Estate Wide",
    tenantId: String? = null,
    tenantEmail: String? = null,
    propertyId: String? = null
  ) {
    val mgrName = _currentUserDisplayName.value?.takeIf { it.isNotBlank() } ?: "Estate Manager"
    val mgrEmail = _currentUserEmail.value?.trim().orEmpty()
    val created = repository.createContract(
      title = title,
      counterparty = counterparty,
      type = type,
      annualValue = annualValue,
      startDate = startDate,
      expiryDate = expiryDate,
      daysRemaining = daysRemaining,
      renewalTerms = renewalTerms,
      contactPerson = contactPerson,
      contactEmail = contactEmail,
      unit = unit,
      tenantId = tenantId,
      tenantEmail = tenantEmail,
      propertyId = propertyId,
      managerName = mgrName,
      managerEmail = mgrEmail
    )
    activeAddContractDialog.value = false
    _snackbarMessage.value = "Contract agreement '${created.title}' registered!"
  }

  fun updateContract(
    contractId: String,
    title: String,
    counterparty: String,
    type: ContractType,
    annualValue: String,
    startDate: String,
    expiryDate: String,
    daysRemaining: Int,
    renewalTerms: String,
    contactPerson: String,
    contactEmail: String,
    unit: String? = null
  ) {
    repository.updateContract(
      contractId = contractId,
      title = title,
      counterparty = counterparty,
      type = type,
      annualValue = annualValue,
      startDate = startDate,
      expiryDate = expiryDate,
      daysRemaining = daysRemaining,
      renewalTerms = renewalTerms,
      contactPerson = contactPerson,
      contactEmail = contactEmail,
      unit = unit
    )
    _snackbarMessage.value = "Contract agreement '${title.trim()}' updated!"
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
    _snackbarMessage.value = "Case removed permanently"
  }

  private fun buildCalendarEvents(
    maintenances: List<MaintenanceEvent>,
    bookings: List<FacilityBooking>,
    contracts: List<ContractItem>,
    cases: List<CaseItem>,
    role: UserRole,
    tenant: TenantProfile?,
    userEmail: String?
  ): List<CalendarEventItem> {
    if (role == UserRole.UNREGISTERED) return emptyList()

    val items = mutableListOf<CalendarEventItem>()
    val tenantName = tenant?.fullName?.trim().orEmpty()
    val tenantUnit = tenant?.assignedPropertyUnit?.trim().orEmpty()
    val normalizedEmail = (tenant?.email ?: userEmail).orEmpty().trim().lowercase()

    // 1. Scheduled Maintenances (visible to both Manager and Tenant as estate advisories)
    maintenances.forEach { m ->
      val key = EstateDateUtils.toDateKey(m.scheduledDate) ?: return@forEach
      items.add(
        CalendarEventItem(
          id = "cal_maint_${m.id}",
          sourceId = m.id,
          type = CalendarEventType.MAINTENANCE,
          dateKey = key,
          displayTime = m.scheduledTime.ifBlank { "All Day" },
          title = m.title,
          subtitle = "${m.targetArea} • ${m.contractorName}",
          statusLabel = m.status.label,
          isCompleted = m.status == MaintenanceStatus.COMPLETED,
          isUrgent = m.status == MaintenanceStatus.OVERDUE
        )
      )
    }

    // 2. Amenity Bookings (Manager: all, Tenant: own bookings)
    val visibleBookings = if (role == UserRole.MANAGER) {
      bookings
    } else {
      bookings.filter { b ->
        (tenantName.isNotEmpty() && b.residentName.equals(tenantName, ignoreCase = true)) ||
          (tenantUnit.isNotEmpty() && b.unit.equals(tenantUnit, ignoreCase = true))
      }
    }
    visibleBookings.forEach { b ->
      val key = EstateDateUtils.toDateKey(b.date) ?: return@forEach
      items.add(
        CalendarEventItem(
          id = "cal_book_${b.id}",
          sourceId = b.id,
          type = CalendarEventType.BOOKING,
          dateKey = key,
          displayTime = b.timeSlot.ifBlank { "Reserved Slot" },
          title = b.facilityName,
          subtitle = "${b.residentName} • ${b.unit} (${b.guestCount} guests)",
          statusLabel = "Confirmed Pass"
        )
      )
    }

    // 3. Contracts (Start & Expiry dates — Manager: all, Tenant: own TENANT_LEASE contracts only)
    val visibleContracts = if (role == UserRole.MANAGER) {
      contracts
    } else {
      contracts.filter { c ->
        c.type == ContractType.TENANT_LEASE && (
          (normalizedEmail.isNotEmpty() && c.tenantEmail?.equals(normalizedEmail, ignoreCase = true) == true) ||
            (tenantName.isNotEmpty() && c.counterparty.equals(tenantName, ignoreCase = true)) ||
            (tenantUnit.isNotEmpty() && c.unit.equals(tenantUnit, ignoreCase = true))
          )
      }
    }
    visibleContracts.forEach { c ->
      val effectiveDays = c.effectiveDaysRemaining
      val contractStatus = if (c.type == ContractType.TENANT_LEASE) {
        c.leaseStatus.label
      } else if (effectiveDays <= 60) {
        "${effectiveDays}d Left"
      } else {
        "Active"
      }

      EstateDateUtils.toDateKey(c.expiryDate)?.let { expKey ->
        items.add(
          CalendarEventItem(
            id = "cal_ctr_exp_${c.id}",
            sourceId = c.id,
            type = CalendarEventType.CONTRACT,
            dateKey = expKey,
            displayTime = "Contract Expiry",
            title = "${c.title} — Expiry",
            subtitle = "${c.tenant} • ${c.unit} • ${c.contractAmount}",
            statusLabel = contractStatus,
            isUrgent = effectiveDays <= 30 || c.leaseStatus == LeaseContractStatus.PENDING_SIGNATURE
          )
        )
      }
      EstateDateUtils.toDateKey(c.startDate)?.let { startKey ->
        items.add(
          CalendarEventItem(
            id = "cal_ctr_start_${c.id}",
            sourceId = c.id,
            type = CalendarEventType.CONTRACT,
            dateKey = startKey,
            displayTime = "Contract Start",
            title = "${c.title} — Start",
            subtitle = "${c.tenant} • ${c.unit} • ${c.contractAmount}",
            statusLabel = if (c.type == ContractType.TENANT_LEASE && !c.isSignedByTenant) "Awaiting Signature" else "Commencement",
            isUrgent = c.type == ContractType.TENANT_LEASE && !c.isSignedByTenant
          )
        )
      }
    }

    // 4. Cases (Reported dates — Manager: all, Tenant: own unit / email / reported cases)
    val visibleCases = if (role == UserRole.MANAGER) {
      cases
    } else {
      cases.filter { c ->
        (normalizedEmail.isNotEmpty() && c.tenantEmail.equals(normalizedEmail, ignoreCase = true)) ||
          (tenantUnit.isNotEmpty() && c.location.contains(tenantUnit, ignoreCase = true)) ||
          (tenantName.isNotEmpty() && c.reportedBy.contains(tenantName, ignoreCase = true))
      }
    }
    visibleCases.forEach { c ->
      val key = EstateDateUtils.toDateKey(c.reportedDate) ?: return@forEach
      items.add(
        CalendarEventItem(
          id = "cal_case_${c.id}",
          sourceId = c.id,
          type = CalendarEventType.CASE,
          dateKey = key,
          displayTime = c.priority.label,
          title = c.title,
          subtitle = "${c.location} • ${c.assignedVendorName ?: "Unassigned"}",
          statusLabel = c.status.label,
          isCompleted = c.status == CaseStatus.RESOLVED,
          isUrgent = (c.priority == CasePriority.EMERGENCY || c.priority == CasePriority.HIGH) && c.status != CaseStatus.RESOLVED
        )
      )
    }

    return items
  }
}

private data class RoleInputs(
  val email: String?,
  val tenants: List<TenantProfile>,
  val isAdmin: Boolean,
  val resolved: Boolean
)

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
