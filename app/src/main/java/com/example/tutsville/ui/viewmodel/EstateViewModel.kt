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
  val estateConfigOptions: StateFlow<EstateConfigOptions> = repository.estateConfigOptions
  val managerInvites: StateFlow<List<ManagerInvite>> = repository.managerInvites

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
  var activeAddNoteCase = MutableStateFlow<CaseItem?>(null)
  var activeBookFacility = MutableStateFlow<Facility?>(null)
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
  var activeManageManagersDialog = MutableStateFlow(false)

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
          it.relatedEntityType == NoticeRelatedEntityType.BOOKING ||
          it.relatedEntityType == NoticeRelatedEntityType.EVENT)
    }
    when (role) {
      UserRole.MANAGER -> attributed
      UserRole.TENANT -> {
        val cleanEmail = (tenant?.email ?: userEmail).orEmpty().trim().lowercase()
        val unitName = tenant?.assignedPropertyUnit?.trim().orEmpty()
        attributed.filter { rem ->
          val isResidentBroadcast = rem.targetAudience.equals("All Residents & Tenants", ignoreCase = true) ||
            rem.targetAudience.equals("ALL_RESIDENTS", ignoreCase = true) ||
            rem.targetAudience.equals("ALL_USERS", ignoreCase = true) ||
            rem.targetAudience.equals("All Residents", ignoreCase = true)
          val isTargetedToMe = cleanEmail.isNotEmpty() && (
            rem.targetEmail?.equals(cleanEmail, ignoreCase = true) == true ||
              rem.targetEmails.any { it.equals(cleanEmail, ignoreCase = true) }
            )
          val isUnitTargeted = unitName.isNotEmpty() && (
            rem.targetUnit?.equals(unitName, ignoreCase = true) == true ||
              rem.targetUnits.any { it.equals(unitName, ignoreCase = true) }
            )
          isResidentBroadcast || isTargetedToMe || isUnitTargeted
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
    _snackbarMessage.value = "All notifications and announcements cleared"
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
      NoticeRelatedEntityType.EVENT -> onNavigateToTab(3)
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
    location: String,
    preferredDate: String? = null,
    categoryLabel: String = category.label
  ) {
    val tenant = _currentTenant.value
    val authName = _currentUserDisplayName.value?.takeIf { it.isNotBlank() }
      ?: _currentUserEmail.value?.takeIf { it.isNotBlank() }
    val reporter = when (_currentRole.value) {
      UserRole.MANAGER -> authName ?: "Estate Manager"
      UserRole.TENANT -> tenant?.fullName?.takeIf { it.isNotBlank() } ?: authName ?: "Resident"
      UserRole.VENDOR -> authName ?: "Contractor"
      UserRole.UNREGISTERED -> authName ?: "User"
    }
    val tenantEmailForCase = if (_currentRole.value == UserRole.TENANT) {
      tenant?.email ?: _currentUserEmail.value
    } else null

    val effectiveLocation = if (_currentRole.value == UserRole.TENANT && !tenant?.assignedPropertyUnit.isNullOrBlank()) {
      tenant!!.assignedPropertyUnit!!
    } else {
      location
    }

    val created = repository.reportCase(
      title = title,
      description = description,
      category = category,
      priority = priority,
      location = effectiveLocation,
      reportedBy = reporter,
      tenantEmail = tenantEmailForCase,
      preferredDate = preferredDate,
      categoryLabel = categoryLabel
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
    priority: CasePriority,
    preferredDate: String? = null,
    categoryLabel: String = category.label
  ) {
    repository.updateCase(caseId, title, description, location, category, priority, preferredDate, categoryLabel)
    _snackbarMessage.value = "Case updated"
  }

  fun appendCaseNote(caseId: String, noteText: String) {
    repository.appendCaseNote(caseId, noteText)
    activeAddNoteCase.value = null
    _snackbarMessage.value = "Manager note added to case timeline"
  }

  fun assignVendor(caseId: String, vendorId: String) {
    repository.assignVendorToCase(caseId, vendorId)
    val vendor = vendors.value.find { it.id == vendorId }
    activeAssignVendorCase.value = null
    _snackbarMessage.value = "Dispatched to ${vendor?.name ?: "Contractor"}"
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

  fun isSlotBooked(facilityId: String, date: String, timeSlot: String): Boolean {
    return repository.isSlotBooked(facilityId, date, timeSlot)
  }

  fun isHourlyWindowAvailable(
    facilityId: String,
    date: String,
    startHour: Int,
    durationHours: Int
  ): Boolean {
    return !repository.isHourlyWindowOccupied(facilityId, date, startHour, durationHours)
  }

  fun bookFacilityWindow(
    facilityId: String,
    date: String,
    startHour: Int,
    durationHours: Int,
    guestCount: Int,
    notes: String
  ) {
    if (_isBookingSubmitting.value) return
    val windowLabel = EstateDateUtils.formatHourWindow(startHour, durationHours)
    val tenant = _currentTenant.value
    val authName = _currentUserDisplayName.value?.takeIf { it.isNotBlank() }
      ?: _currentUserEmail.value?.takeIf { it.isNotBlank() }
    val resident = when (_currentRole.value) {
      UserRole.TENANT -> tenant?.fullName?.takeIf { it.isNotBlank() } ?: authName ?: "Resident"
      UserRole.MANAGER -> authName ?: "Estate Manager"
      UserRole.VENDOR -> authName ?: "Contractor"
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
        timeSlot = windowLabel,
        residentName = resident,
        unit = unit,
        guestCount = guestCount,
        notes = notes,
        startHour = startHour,
        durationHours = durationHours,
        onResult = { result ->
          _isBookingSubmitting.value = false
          result.onSuccess { booking ->
            activeBookFacility.value = null
            _snackbarMessage.value = "Request sent for ${booking.facilityName} (${booking.timeSlot}) — Pending Approval"
          }.onFailure { err ->
            _snackbarMessage.value = err.message ?: "Selected time window could not be requested"
          }
        }
      )
    } catch (e: IllegalStateException) {
      _isBookingSubmitting.value = false
      _snackbarMessage.value = e.message ?: "Selected time window is already reserved"
    }
  }

  fun bookFacilitySlot(
    facilityId: String,
    date: String,
    timeSlot: String,
    guestCount: Int,
    notes: String
  ) {
    val parsed = EstateDateUtils.parseTimeRangeHours(timeSlot)
    if (parsed != null) {
      bookFacilityWindow(
        facilityId = facilityId,
        date = date,
        startHour = parsed.first,
        durationHours = (parsed.last - parsed.first + 1).coerceIn(1, 3),
        guestCount = guestCount,
        notes = notes
      )
    } else {
      bookFacilityWindow(
        facilityId = facilityId,
        date = date,
        startHour = 9,
        durationHours = 1,
        guestCount = guestCount,
        notes = notes
      )
    }
  }

  fun approveBooking(bookingId: String) {
    repository.approveBooking(bookingId) { result ->
      result.onSuccess { b ->
        _snackbarMessage.value = "Approved ${b.facilityName} (${b.timeSlot}) for ${b.residentName}"
      }.onFailure { err ->
        _snackbarMessage.value = err.message ?: "Could not approve booking"
      }
    }
  }

  fun declineBooking(bookingId: String, reason: String = "") {
    repository.declineBooking(bookingId, reason)
    _snackbarMessage.value = "Booking request declined"
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
    notes: String,
    eventCategory: CalendarEventCategory = CalendarEventCategory.MAINTENANCE,
    targetScope: EventTargetScope = EventTargetScope.ALL_RESIDENTS,
    targetUnits: List<String> = emptyList(),
    disciplineLabel: String = discipline.label
  ) {
    repository.scheduleMaintenance(
      title = title,
      discipline = discipline,
      date = date,
      time = time,
      contractor = contractor,
      targetArea = targetArea,
      intervalMonths = intervalMonths,
      notes = notes,
      eventCategory = eventCategory,
      targetScope = targetScope,
      targetUnits = targetUnits,
      disciplineLabel = disciplineLabel
    )
    activeAddMaintenanceDialog.value = false
    _snackbarMessage.value = "Event added to estate calendar"
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
    _snackbarMessage.value = "Scheduled event removed from calendar"
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

  fun setContractArchived(contractId: String, archived: Boolean) {
    repository.setContractArchived(contractId, archived)
    activeContractDetail.value = null
    _snackbarMessage.value = if (archived) "Contract moved to Archive" else "Contract restored to Active"
  }

  fun attachContractDocument(contractId: String, isCodeOfConduct: Boolean, docMeta: ContractDocumentMeta) {
    repository.attachContractDocument(contractId, isCodeOfConduct, docMeta)
    _snackbarMessage.value = if (isCodeOfConduct) "Code of Conduct attached" else "Signed Contract attached"
  }

  // --- Config & Manager Allowlist ---
  fun updateFloorElevations(elevations: List<String>) {
    repository.updateConfigOptions(floorElevations = elevations)
    _snackbarMessage.value = "Floor elevation options saved"
  }

  fun updateContractorDisciplines(disciplines: List<String>) {
    repository.updateConfigOptions(contractorDisciplines = disciplines)
    _snackbarMessage.value = "Contractor disciplines saved"
  }

  fun updateAmenityCategoryTags(tags: List<String>) {
    repository.updateConfigOptions(amenityCategoryTags = tags)
    _snackbarMessage.value = "Amenity category tags saved"
  }

  fun updateAmenityTimeSlots(slots: List<String>) {
    repository.updateConfigOptions(amenityTimeSlots = slots)
    _snackbarMessage.value = "Managed amenity times saved"
  }

  fun updateCaseDisciplines(disciplines: List<String>) {
    repository.updateConfigOptions(caseDisciplines = disciplines)
    _snackbarMessage.value = "Case disciplines saved"
  }

  fun updateEngineeringDisciplines(disciplines: List<String>) {
    repository.updateConfigOptions(engineeringDisciplines = disciplines)
    _snackbarMessage.value = "Engineering disciplines saved"
  }

  fun quickCreateUnit(
    unitNumber: String,
    floorLevel: String = "",
    monthlyRental: String = ""
  ): PropertyItem? {
    if (currentRole.value != UserRole.MANAGER) return null
    val cleanUnit = unitNumber.trim()
    if (cleanUnit.length < 2) {
      _snackbarMessage.value = "Please enter a unit number or title (at least 2 characters)."
      return null
    }
    val existing = properties.value.find { it.unitNumber.equals(cleanUnit, ignoreCase = true) }
    if (existing != null) {
      _snackbarMessage.value = "${existing.unitNumber} already exists and was selected."
      return existing
    }
    val created = repository.listProperty(
      unitNumber = cleanUnit,
      pavilionWing = "",
      architecture = PropertyArchitecture(
        bedrooms = 0,
        bathrooms = 0f,
        sqFt = 0,
        floorLevel = floorLevel.trim(),
        viewType = "",
        sqMeters = 0
      ),
      parkingSpots = emptyList(),
      amenities = emptyList(),
      monthlyRental = monthlyRental.trim()
    )
    _snackbarMessage.value = "${created.unitNumber} created and added to Units"
    return created
  }

  fun inviteManager(email: String, displayName: String) {
    try {
      repository.inviteManager(email, displayName) { result ->
        result.onSuccess { inv ->
          _snackbarMessage.value = "Co-manager invitation created for ${inv.email}"
        }.onFailure { err ->
          _snackbarMessage.value = err.message ?: "Failed to invite manager"
        }
      }
    } catch (e: IllegalArgumentException) {
      _snackbarMessage.value = e.message ?: "Invalid email address"
    }
  }

  fun revokeManagerInvite(email: String) {
    repository.revokeManagerInvite(email)
    _snackbarMessage.value = "Removed co-manager invite for $email"
  }

  // --- Property & Tenant Management ---
  fun listProperty(
    unitNumber: String,
    pavilionWing: String = "",
    architecture: PropertyArchitecture,
    parkingSpots: List<String> = emptyList(),
    amenities: List<String>,
    monthlyRental: String,
    assignTenantName: String? = null,
    assignTenantEmail: String? = null,
    leaseStartDate: String? = null,
    leaseEndDate: String? = null,
    signedContractDoc: ContractDocumentMeta? = null,
    codeOfConductDoc: ContractDocumentMeta? = null
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
    val cleanTenantName = assignTenantName?.trim().orEmpty()
    val cleanTenantEmail = assignTenantEmail?.trim().orEmpty()
    val cleanStartDate = leaseStartDate?.trim().orEmpty()
    val cleanEndDate = leaseEndDate?.trim().orEmpty()
    if (cleanTenantName.isNotBlank() && cleanTenantEmail.isNotBlank() && cleanStartDate.isNotBlank()) {
      val mgrName = _currentUserDisplayName.value?.takeIf { it.isNotBlank() } ?: "Estate Manager"
      val mgrEmail = _currentUserEmail.value?.trim().orEmpty()
      val tenantInfo = repository.assignPropertyToTenant(
        propertyId = created.id,
        tenantName = cleanTenantName,
        tenantEmail = cleanTenantEmail,
        leaseStartDate = cleanStartDate,
        leaseEndDate = cleanEndDate,
        managerName = mgrName,
        managerEmail = mgrEmail,
        signedContractDoc = signedContractDoc,
        codeOfConductDoc = codeOfConductDoc
      )
      val updatedProp = properties.value.find { it.id == created.id } ?: created
      activeShareInviteDialog.value = updatedProp
      _snackbarMessage.value = "${created.unitNumber} added & assigned to ${tenantInfo.name}"
    } else {
      _snackbarMessage.value = "${created.unitNumber} added successfully"
    }
  }

  fun updateProperty(
    propertyId: String,
    unitNumber: String,
    pavilionWing: String = "",
    architecture: PropertyArchitecture,
    parkingSpots: List<String> = emptyList(),
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
    leaseEndDate: String,
    signedContractDoc: ContractDocumentMeta? = null,
    codeOfConductDoc: ContractDocumentMeta? = null
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
      managerEmail = mgrEmail,
      signedContractDoc = signedContractDoc,
      codeOfConductDoc = codeOfConductDoc
    )
    val updatedProp = properties.value.find { it.id == propertyId }
    activeAssignTenantProperty.value = null
    activeShareInviteDialog.value = updatedProp
    _snackbarMessage.value = "Assigned to ${tenantInfo.name} & lease agreement created!"
  }

  fun vacateProperty(propertyId: String) {
    repository.vacateProperty(propertyId)
    _snackbarMessage.value = "Unit marked as vacant and available for lease"
  }

  fun claimPropertyForEmail(email: String) {
    val prop = repository.claimPropertyInvite(email)
    repository.claimTenantInvite(email)
    if (prop != null) {
      _snackbarMessage.value = "Welcome home! Linked to ${prop.unitNumber}"
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
    leaseEndDate: String,
    signedContractDoc: ContractDocumentMeta? = null,
    codeOfConductDoc: ContractDocumentMeta? = null
  ) {
    val mgrName = _currentUserDisplayName.value?.takeIf { it.isNotBlank() } ?: "Estate Manager"
    val mgrEmail = _currentUserEmail.value?.trim().orEmpty()
    val updated = repository.assignTenantToPropertyById(
      tenantId = tenantId,
      propertyId = propertyId,
      leaseStartDate = leaseStartDate,
      leaseEndDate = leaseEndDate,
      managerName = mgrName,
      managerEmail = mgrEmail,
      signedContractDoc = signedContractDoc,
      codeOfConductDoc = codeOfConductDoc
    )
    activeAssignPropertyToTenant.value = null
    if (updated != null) {
      activeTenantInviteSheet.value = updated
      _snackbarMessage.value = "Assigned ${updated.fullName} to ${updated.assignedPropertyUnit} & lease registered!"
    }
  }

  fun unassignTenant(tenantId: String) {
    repository.unassignTenant(tenantId)
    _snackbarMessage.value = "Tenant unassigned from unit"
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
    val cleanName = name.trim()
    if (cleanName.length < 2) {
      _snackbarMessage.value = "Please enter an amenity name (at least 2 characters)."
      return
    }
    val cleanSlots = slots.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)
    if (cleanSlots.any { it.length !in 3..80 }) {
      _snackbarMessage.value = "Each configured time slot must be between 3 and 80 characters."
      return
    }
    val created = repository.createFacility(cleanName, tag, capacity.coerceAtLeast(0), location, description, amenities, cleanSlots, rules)
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
    val cleanName = name.trim()
    if (cleanName.length < 2) {
      _snackbarMessage.value = "Please enter an amenity name (at least 2 characters)."
      return
    }
    val cleanSlots = slots.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)
    if (cleanSlots.any { it.length !in 3..80 }) {
      _snackbarMessage.value = "Each configured time slot must be between 3 and 80 characters."
      return
    }
    repository.updateFacility(facilityId, cleanName, tag, capacity.coerceAtLeast(0), location, description, amenities, cleanSlots, rules)
    _snackbarMessage.value = "Amenity '$cleanName' updated!"
  }

  fun deleteFacility(id: String) {
    repository.deleteFacility(id)
    _snackbarMessage.value = "Amenity removed"
  }

  // --- Contractor Management ---
  fun submitNewVendor(
    name: String,
    specialty: CaseCategory,
    phone: String,
    email: String,
    leadTech: String,
    discipline: String = specialty.label
  ) {
    val created = repository.createVendor(name, specialty, phone, email, leadTech, discipline)
    activeAddVendorDialog.value = false
    _snackbarMessage.value = "Contractor '${created.name}' onboarded!"
  }

  fun updateVendor(
    vendorId: String,
    name: String,
    specialty: CaseCategory,
    phone: String,
    email: String,
    leadTech: String,
    discipline: String = specialty.label
  ) {
    repository.updateVendor(vendorId, name, specialty, phone, email, leadTech, discipline)
    _snackbarMessage.value = "Contractor '${name.trim()}' updated!"
  }

  fun deleteVendor(id: String) {
    repository.deleteVendor(id)
    _snackbarMessage.value = "Contractor removed"
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
    propertyId: String? = null,
    signedContractDoc: ContractDocumentMeta? = null,
    codeOfConductDoc: ContractDocumentMeta? = null
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
      managerEmail = mgrEmail,
      signedContractDoc = signedContractDoc,
      codeOfConductDoc = codeOfConductDoc
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
    unit: String? = null,
    signedContractDoc: ContractDocumentMeta? = null,
    codeOfConductDoc: ContractDocumentMeta? = null
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
      unit = unit,
      signedContractDoc = signedContractDoc,
      codeOfConductDoc = codeOfConductDoc
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

    // 1. Scheduled Estate Events (filtered by targetScope/targetUnits for Tenants)
    val visibleMaintenances = if (role == UserRole.MANAGER) {
      maintenances
    } else {
      maintenances.filter { m ->
        m.targetScope == EventTargetScope.ALL_RESIDENTS ||
          (tenantUnit.isNotEmpty() && m.targetUnits.any { it.equals(tenantUnit, ignoreCase = true) }) ||
          m.targetUnits.any { it.equals("Clubhouse", ignoreCase = true) }
      }
    }
    visibleMaintenances.forEach { m ->
      val key = EstateDateUtils.toDateKey(m.scheduledDate) ?: return@forEach
      val subtitleParts = buildList {
        if (m.targetArea.isNotBlank()) add(m.targetArea)
        if (m.eventCategory == CalendarEventCategory.MAINTENANCE && m.effectiveDisciplineLabel.isNotBlank()) {
          add(m.effectiveDisciplineLabel)
        }
        if (m.contractorName.isNotBlank()) add(m.contractorName)
        add(m.eventCategory.label)
      }
      items.add(
        CalendarEventItem(
          id = "cal_maint_${m.id}",
          sourceId = m.id,
          type = CalendarEventType.MAINTENANCE,
          dateKey = key,
          displayTime = m.scheduledTime.ifBlank { "All Day" },
          title = m.title,
          subtitle = subtitleParts.joinToString(" • "),
          statusLabel = m.status.label,
          isCompleted = m.status == MaintenanceStatus.COMPLETED,
          isUrgent = m.status == MaintenanceStatus.OVERDUE
        )
      )
    }

    // 2. Amenity Bookings (exclude DECLINED from calendar)
    val visibleBookings = if (role == UserRole.MANAGER) {
      bookings.filter { it.approvalStatus != BookingApprovalStatus.DECLINED }
    } else {
      bookings.filter { b ->
        b.approvalStatus != BookingApprovalStatus.DECLINED && (
          (tenantName.isNotEmpty() && b.residentName.equals(tenantName, ignoreCase = true)) ||
            (tenantUnit.isNotEmpty() && b.unit.equals(tenantUnit, ignoreCase = true))
          )
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
          displayTime = b.timeSlot.ifBlank { "Reserved Window" },
          title = b.facilityName,
          subtitle = "${b.residentName} • ${b.unit} (${b.guestCount} guests)",
          statusLabel = if (b.approvalStatus == BookingApprovalStatus.APPROVED) "Approved Pass" else "Pending Approval",
          isUrgent = b.approvalStatus == BookingApprovalStatus.PENDING
        )
      )
    }

    // 3. Active Contracts (Start & Expiry dates — Manager: non-archived, Tenant: own active TENANT_LEASE contracts)
    val visibleContracts = if (role == UserRole.MANAGER) {
      contracts.filter { !it.isArchived }
    } else {
      contracts.filter { c ->
        !c.isArchived && c.type == ContractType.TENANT_LEASE && (
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
            isUrgent = effectiveDays <= 30
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
            statusLabel = "Commencement",
            isUrgent = false
          )
        )
      }
    }

    // 4. Cases (Use Preferred Completion Date when specified, otherwise Reported Date)
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
      val targetDate = c.preferredDate?.takeIf { it.isNotBlank() } ?: c.reportedDate
      val key = EstateDateUtils.toDateKey(targetDate) ?: return@forEach
      val dateNote = if (!c.preferredDate.isNullOrBlank()) " • Target Due ${c.preferredDate}" else ""
      items.add(
        CalendarEventItem(
          id = "cal_case_${c.id}",
          sourceId = c.id,
          type = CalendarEventType.CASE,
          dateKey = key,
          displayTime = c.priority.label,
          title = c.title,
          subtitle = "${c.location} • ${c.assignedVendorName ?: "Unassigned"}$dateNote",
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
