package com.example.tutsville.data.repository

import android.util.Log
import com.example.tutsville.data.model.*
import com.example.tutsville.data.repository.EstateFirestoreMappers.buildSlotLockId
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToBooking
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToCase
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToContract
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToFacility
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToMaintenance
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToProperty
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToReminder
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToSlotLock
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToTenant
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToVendor
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapBookingToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapCaseToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapContractToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapFacilityToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapMaintenanceToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapPropertyToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapReminderToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapSlotLockToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapTenantToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapVendorToFirestore
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EstateRepository(
  val databaseId: String? = null,
  private val injectedDb: FirebaseFirestore? = null,
  private val injectedAuth: FirebaseAuth? = null
) {

  companion object {
    const val BOOTSTRAP_MANAGER_EMAIL = "baobabstack@gmail.com"
    const val DEFAULT_DATABASE_ID = "ai-studio-android-tutsvill-d32054fc-09ff-4509-8bdd-53cb7f4790e9"

    @Volatile
    private var instance: EstateRepository? = null

    fun getInstance(databaseId: String? = null): EstateRepository {
      val existing = instance
      if (existing != null && (databaseId == null || existing.databaseId == databaseId)) {
        return existing
      }
      return synchronized(this) {
        val current = instance
        if (current != null && (databaseId == null || current.databaseId == databaseId)) {
          current
        } else {
          val effectiveId = databaseId ?: DEFAULT_DATABASE_ID
          EstateRepository(effectiveId).also { instance = it }
        }
      }
    }
  }

  private val db: FirebaseFirestore? = injectedDb ?: try {
    if (!databaseId.isNullOrBlank()) {
      FirebaseFirestore.getInstance(databaseId)
    } else {
      null
    }
  } catch (_: Exception) {
    null
  }

  private val firebaseAuth: FirebaseAuth? = injectedAuth ?: try {
    if (db != null) Firebase.auth else null
  } catch (_: Exception) {
    null
  }

  val isUsingFirestore: Boolean get() = db != null

  val isLoading = MutableStateFlow(false)
  val isLoadingFlow: StateFlow<Boolean> = isLoading.asStateFlow()

  private val _isCurrentUserAdmin = MutableStateFlow(db == null)
  val isCurrentUserAdmin: StateFlow<Boolean> = _isCurrentUserAdmin.asStateFlow()

  private val _isRoleResolved = MutableStateFlow(db == null)
  val isRoleResolved: StateFlow<Boolean> = _isRoleResolved.asStateFlow()

  private val _lastError = MutableStateFlow<String?>(null)
  val lastError: StateFlow<String?> = _lastError.asStateFlow()

  private val _cases = MutableStateFlow<List<CaseItem>>(emptyList())
  val cases: StateFlow<List<CaseItem>> = _cases.asStateFlow()

  private val _maintenances = MutableStateFlow<List<MaintenanceEvent>>(emptyList())
  val maintenances: StateFlow<List<MaintenanceEvent>> = _maintenances.asStateFlow()

  private val _facilities = MutableStateFlow<List<Facility>>(emptyList())
  val facilities: StateFlow<List<Facility>> = _facilities.asStateFlow()

  private val _bookings = MutableStateFlow<List<FacilityBooking>>(emptyList())
  val bookings: StateFlow<List<FacilityBooking>> = _bookings.asStateFlow()

  private val _slotLocks = MutableStateFlow<List<BookingSlotLock>>(emptyList())
  val slotLocks: StateFlow<List<BookingSlotLock>> = _slotLocks.asStateFlow()

  private val _contracts = MutableStateFlow<List<ContractItem>>(emptyList())
  val contracts: StateFlow<List<ContractItem>> = _contracts.asStateFlow()

  private val _vendors = MutableStateFlow<List<VendorItem>>(emptyList())
  val vendors: StateFlow<List<VendorItem>> = _vendors.asStateFlow()

  private val _reminders = MutableStateFlow<List<ReminderItem>>(emptyList())
  val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

  private val _properties = MutableStateFlow<List<PropertyItem>>(emptyList())
  val properties: StateFlow<List<PropertyItem>> = _properties.asStateFlow()

  private val _tenants = MutableStateFlow<List<TenantProfile>>(emptyList())
  val tenants: StateFlow<List<TenantProfile>> = _tenants.asStateFlow()

  private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
  private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

  private val collectionListeners = mutableListOf<ListenerRegistration>()
  private var adminDocListener: ListenerRegistration? = null
  private var currentListeningUid: String? = null
  private var currentListeningAdminMode: Boolean? = null

  private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
    handleAuthState(auth.currentUser)
  }

  private fun getCurrentUserId(): String {
    return try {
      firebaseAuth?.currentUser?.uid ?: "user_default"
    } catch (_: Exception) {
      "user_default"
    }
  }

  private fun getCurrentUserEmail(): String {
    return try {
      firebaseAuth?.currentUser?.email?.trim()?.lowercase().orEmpty()
    } catch (_: Exception) {
      ""
    }
  }

  init {
    if (db != null) {
      try {
        firebaseAuth?.addAuthStateListener(authStateListener)
        handleAuthState(firebaseAuth?.currentUser)
      } catch (e: Exception) {
        Log.e("EstateRepository", "Error registering AuthStateListener: ${e.message}")
      }
    }
  }

  fun refreshAuthAndListeners() {
    if (db == null) return
    currentListeningUid = null
    currentListeningAdminMode = null
    handleAuthState(firebaseAuth?.currentUser)
  }

  fun clearError() {
    _lastError.value = null
  }

  private fun handleAuthState(user: FirebaseUser?) {
    val firestore = db ?: return
    if (user == null) {
      detachAllListeners()
      clearStateFlows()
      _isCurrentUserAdmin.value = false
      _isRoleResolved.value = false
      isLoading.value = false
      return
    }

    if (currentListeningUid == user.uid && adminDocListener != null) {
      return
    }

    detachAllListeners()
    currentListeningUid = user.uid
    _isRoleResolved.value = false
    isLoading.value = true

    val email = user.email?.trim()?.lowercase().orEmpty()
    val isBootstrapEmail = email == BOOTSTRAP_MANAGER_EMAIL && user.isEmailVerified

    if (isBootstrapEmail) {
      val adminRef = firestore.collection("admins").document(user.uid)
      adminRef.get()
        .addOnSuccessListener { doc ->
          if (!doc.exists()) {
            val adminPayload = mapOf(
              "uid" to user.uid,
              "email" to email,
              "displayName" to (user.displayName ?: "Estate Manager"),
              "role" to "ADMIN",
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
            adminRef.set(adminPayload)
              .addOnSuccessListener {
                attachAdminDocListener(firestore, user)
              }
              .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.CREATE, "admins/${user.uid}", firebaseAuth)
                attachAdminDocListener(firestore, user)
              }
          } else {
            attachAdminDocListener(firestore, user)
          }
        }
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.GET, "admins/${user.uid}", firebaseAuth)
          attachAdminDocListener(firestore, user)
        }
    } else {
      attachAdminDocListener(firestore, user)
    }
  }

  private fun attachAdminDocListener(firestore: FirebaseFirestore, user: FirebaseUser) {
    adminDocListener?.remove()
    adminDocListener = firestore.collection("admins").document(user.uid)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.GET, "admins/${user.uid}", firebaseAuth)
          _isCurrentUserAdmin.value = false
          _isRoleResolved.value = true
          if (currentListeningAdminMode != false) {
            attachRoleScopedListeners(firestore, user, isAdmin = false)
          }
          return@addSnapshotListener
        }
        val isAdmin = snapshot?.exists() == true
        _isCurrentUserAdmin.value = isAdmin
        _isRoleResolved.value = true
        if (currentListeningAdminMode != isAdmin) {
          attachRoleScopedListeners(firestore, user, isAdmin = isAdmin)
        }
      }
  }

  private fun detachCollectionListeners() {
    collectionListeners.forEach { it.remove() }
    collectionListeners.clear()
    currentListeningAdminMode = null
  }

  fun detachAllListeners() {
    detachCollectionListeners()
    adminDocListener?.remove()
    adminDocListener = null
    currentListeningUid = null
  }

  private fun clearStateFlows() {
    _cases.value = emptyList()
    _properties.value = emptyList()
    _tenants.value = emptyList()
    _facilities.value = emptyList()
    _bookings.value = emptyList()
    _slotLocks.value = emptyList()
    _maintenances.value = emptyList()
    _contracts.value = emptyList()
    _reminders.value = emptyList()
    _vendors.value = emptyList()
  }

  private fun attachRoleScopedListeners(
    firestore: FirebaseFirestore,
    user: FirebaseUser,
    isAdmin: Boolean
  ) {
    detachCollectionListeners()
    currentListeningAdminMode = isAdmin

    val uid = user.uid
    val cleanEmail = user.email?.trim()?.lowercase().orEmpty()
    val hasVerifiedEmail = user.isEmailVerified && cleanEmail.isNotBlank()

    // Shared Estate Operational Data (non-PII)
    listenQuery(firestore.collection("facilities"), "facilities") { docs ->
      _facilities.value = docs.mapNotNull { docToFacility(it) }
    }
    listenQuery(firestore.collection("maintenances"), "maintenances") { docs ->
      _maintenances.value = docs.mapNotNull { docToMaintenance(it) }
    }
    listenQuery(firestore.collection("reminders"), "reminders") { docs ->
      _reminders.value = docs.mapNotNull { docToReminder(it) }
    }
    listenQuery(firestore.collection("vendors"), "vendors") { docs ->
      _vendors.value = docs.mapNotNull { docToVendor(it) }
    }
    listenQuery(firestore.collection("slot_locks"), "slot_locks") { docs ->
      _slotLocks.value = docs.mapNotNull { docToSlotLock(it) }
    }

    if (isAdmin) {
      listenQuery(firestore.collection("cases"), "cases") { docs ->
        _cases.value = docs.mapNotNull { docToCase(it) }
      }
      listenQuery(firestore.collection("properties"), "properties") { docs ->
        _properties.value = docs.mapNotNull { docToProperty(it) }
      }
      listenQuery(firestore.collection("tenants"), "tenants") { docs ->
        _tenants.value = docs.mapNotNull { docToTenant(it) }
      }
      listenQuery(firestore.collection("bookings"), "bookings") { docs ->
        _bookings.value = docs.mapNotNull { docToBooking(it) }
      }
      listenQuery(firestore.collection("contracts"), "contracts") { docs ->
        _contracts.value = docs.mapNotNull { docToContract(it) }
      }
    } else {
      // Resident / Non-admin strictly scoped queries matching firestore.rules
      listenQuery(firestore.collection("bookings").whereEqualTo("userId", uid), "bookings") { docs ->
        _bookings.value = docs.mapNotNull { docToBooking(it) }
      }

      if (hasVerifiedEmail) {
        listenQuery(firestore.collection("tenants").whereEqualTo("email", cleanEmail), "tenants") { docs ->
          _tenants.value = docs.mapNotNull { docToTenant(it) }
        }
        listenQuery(firestore.collection("properties").whereEqualTo("tenantEmail", cleanEmail), "properties") { docs ->
          _properties.value = docs.mapNotNull { docToProperty(it) }
        }
        listenQuery(firestore.collection("contracts").whereEqualTo("tenantEmail", cleanEmail), "contracts") { docs ->
          _contracts.value = docs.mapNotNull { docToContract(it) }
        }

        // Cases: merge owner cases (userId == uid) and assigned unit tenant cases (tenantEmail == cleanEmail)
        var ownCases = emptyList<CaseItem>()
        var emailCases = emptyList<CaseItem>()
        fun mergeCases() {
          _cases.value = (ownCases + emailCases).distinctBy { it.id }
        }
        listenQuery(firestore.collection("cases").whereEqualTo("userId", uid), "cases(userId)") { docs ->
          ownCases = docs.mapNotNull { docToCase(it) }
          mergeCases()
        }
        listenQuery(firestore.collection("cases").whereEqualTo("tenantEmail", cleanEmail), "cases(tenantEmail)") { docs ->
          emailCases = docs.mapNotNull { docToCase(it) }
          mergeCases()
        }
      } else {
        _tenants.value = emptyList()
        _properties.value = emptyList()
        _contracts.value = emptyList()
        listenQuery(firestore.collection("cases").whereEqualTo("userId", uid), "cases(userId)") { docs ->
          _cases.value = docs.mapNotNull { docToCase(it) }
        }
      }
    }
  }

  private fun listenQuery(
    query: Query,
    path: String,
    onDocs: (List<com.google.firebase.firestore.DocumentSnapshot>) -> Unit
  ) {
    val reg = query.addSnapshotListener { snapshot, error ->
      if (error != null) {
        handleFirestoreError(error, OperationType.LIST, path, firebaseAuth)
        isLoading.value = false
        return@addSnapshotListener
      }
      if (snapshot != null) {
        onDocs(snapshot.documents)
        isLoading.value = false
      }
    }
    collectionListeners.add(reg)
  }

  // --- Work Order / Case Operations ---
  fun reportCase(
    title: String,
    description: String,
    category: CaseCategory,
    priority: CasePriority,
    location: String,
    reportedBy: String,
    tenantEmail: String? = null
  ): CaseItem {
    val cleanLocation = location.trim()
    val resolvedTenantEmail = tenantEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
      ?: _properties.value.find { it.unitNumber.equals(cleanLocation, ignoreCase = true) }?.assignedTenant?.email?.lowercase()
      ?: _tenants.value.find {
        it.assignedPropertyUnit?.equals(cleanLocation, ignoreCase = true) == true ||
          it.fullName.equals(reportedBy.trim(), ignoreCase = true)
      }?.email?.lowercase()
      ?: getCurrentUserEmail()

    val uid = getCurrentUserId()
    val newCase = CaseItem(
      id = "CASE-${System.currentTimeMillis()}-${(100..999).random()}",
      userId = uid,
      tenantEmail = resolvedTenantEmail,
      title = title.trim(),
      description = description.trim(),
      category = category,
      priority = priority,
      location = cleanLocation,
      reportedBy = reportedBy.trim(),
      reportedDate = dateFormat.format(Date()),
      status = CaseStatus.OPEN
    )
    _cases.update { listOf(newCase) + it }

    db?.let { firestore ->
      val payload = mapCaseToFirestore(newCase, uid, isCreate = true)
      firestore.collection("cases").document(newCase.id).set(payload)
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "cases/${newCase.id}", firebaseAuth)
        }
    }

    return newCase
  }

  fun updateCase(
    caseId: String,
    title: String,
    description: String,
    location: String,
    category: CaseCategory,
    priority: CasePriority
  ) {
    val cleanTitle = title.trim()
    val cleanDesc = description.trim()
    val cleanLoc = location.trim()
    _cases.update { list ->
      list.map { c ->
        if (c.id == caseId) {
          c.copy(
            title = cleanTitle,
            description = cleanDesc,
            location = cleanLoc,
            unitNumber = cleanLoc,
            category = category,
            priority = priority
          )
        } else c
      }
    }

    db?.let { firestore ->
      firestore.collection("cases").document(caseId).update(
        mapOf(
          "title" to cleanTitle,
          "description" to cleanDesc,
          "location" to cleanLoc,
          "category" to category.name,
          "priority" to priority.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "cases/$caseId", firebaseAuth)
      }
    }
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
    _vendors.update { current ->
      current.map { v ->
        if (v.id == vendorId) v.copy(activeJobsCount = v.activeJobsCount + 1) else v
      }
    }

    db?.let { firestore ->
      firestore.collection("cases").document(caseId).update(
        mapOf(
          "assignedVendorId" to vendor.id,
          "assignedVendorName" to vendor.name,
          "status" to CaseStatus.ASSIGNED.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "cases/$caseId", firebaseAuth)
      }
      firestore.collection("vendors").document(vendorId).update(
        mapOf(
          "activeJobsCount" to FieldValue.increment(1),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "vendors/$vendorId", firebaseAuth)
      }
    }
  }

  fun updateCaseStatus(caseId: String, newStatus: CaseStatus) {
    _cases.update { current ->
      current.map { c ->
        if (c.id == caseId) c.copy(status = newStatus) else c
      }
    }

    db?.let { firestore ->
      firestore.collection("cases").document(caseId).update(
        mapOf(
          "status" to newStatus.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "cases/$caseId", firebaseAuth)
      }
    }
  }

  fun submitCaseReview(caseId: String, rating: Int, reviewTags: List<String>, comment: String) {
    val existingCase = _cases.value.find { it.id == caseId }
    val previousRating = existingCase?.rating

    _cases.update { current ->
      current.map { c ->
        if (c.id == caseId) {
          c.copy(
            rating = rating,
            reviewTags = reviewTags,
            reviewComment = comment.trim(),
            status = CaseStatus.RESOLVED
          )
        } else c
      }
    }

    val theCase = _cases.value.find { it.id == caseId }
    var updatedVendorRating: Float? = null
    var updatedVendorReviewCount: Int? = null
    val vendorId = theCase?.assignedVendorId

    if (vendorId != null) {
      val existingVendor = _vendors.value.find { it.id == vendorId }
      val vendorCasesWithRatings = _cases.value.filter { it.assignedVendorId == vendorId && it.rating != null }
      if (existingVendor != null && existingVendor.reviewCount > vendorCasesWithRatings.size) {
        val currentCount = existingVendor.reviewCount.coerceAtLeast(0)
        val totalScore = existingVendor.rating.toDouble() * currentCount
        val (newCount, newTotal) = if (previousRating != null && currentCount > 0) {
          currentCount to (totalScore - previousRating + rating)
        } else {
          (currentCount + 1) to (totalScore + rating)
        }
        val avg = (newTotal / newCount.coerceAtLeast(1)).coerceIn(0.0, 5.0)
        val rounded = String.format(Locale.US, "%.1f", avg).toFloat()
        updatedVendorRating = rounded
        updatedVendorReviewCount = newCount
      } else if (vendorCasesWithRatings.isNotEmpty()) {
        val avg = vendorCasesWithRatings.mapNotNull { it.rating }.average().toFloat()
        val rounded = String.format(Locale.US, "%.1f", avg).toFloat()
        val count = vendorCasesWithRatings.size
        updatedVendorRating = rounded
        updatedVendorReviewCount = count
      }
      if (updatedVendorRating != null && updatedVendorReviewCount != null) {
        _vendors.update { list ->
          list.map { v ->
            if (v.id == vendorId) v.copy(rating = updatedVendorRating, reviewCount = updatedVendorReviewCount) else v
          }
        }
      }
    }

    db?.let { firestore ->
      firestore.collection("cases").document(caseId).update(
        mapOf(
          "rating" to rating,
          "reviewTags" to reviewTags,
          "reviewComment" to comment.trim(),
          "status" to CaseStatus.RESOLVED.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "cases/$caseId", firebaseAuth)
      }

      if (vendorId != null && updatedVendorRating != null && updatedVendorReviewCount != null) {
        firestore.collection("vendors").document(vendorId).update(
          mapOf(
            "rating" to updatedVendorRating.toDouble(),
            "reviewCount" to updatedVendorReviewCount,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.UPDATE, "vendors/$vendorId", firebaseAuth)
        }
      }
    }
  }

  fun deleteCase(caseId: String) {
    _cases.update { it.filterNot { c -> c.id == caseId } }
    db?.let { firestore ->
      firestore.collection("cases").document(caseId).delete()
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.DELETE, "cases/$caseId", firebaseAuth)
        }
    }
  }

  // --- Facility & Slot-Locked Booking Operations ---
  fun isSlotBooked(facilityId: String, date: String, timeSlot: String): Boolean {
    val targetDateKey = EstateDateUtils.toDateKey(date) ?: date.trim()
    val cleanSlot = timeSlot.trim()
    val lockedInLocks = _slotLocks.value.any { lock ->
      lock.facilityId == facilityId &&
        lock.dateKey == targetDateKey &&
        lock.timeSlot.equals(cleanSlot, ignoreCase = true)
    }
    if (lockedInLocks) return true
    return _bookings.value.any { b ->
      val bookingKey = EstateDateUtils.toDateKey(b.date) ?: b.date.trim()
      b.facilityId == facilityId &&
        bookingKey == targetDateKey &&
        b.timeSlot.equals(cleanSlot, ignoreCase = true)
    }
  }

  fun bookFacility(
    facilityId: String,
    date: String,
    timeSlot: String,
    residentName: String,
    unit: String,
    guestCount: Int,
    notes: String
  ): FacilityBooking {
    val dateKey = EstateDateUtils.toDateKey(date) ?: date.trim()
    val cleanDate = EstateDateUtils.toDateKey(date)?.let { EstateDateUtils.displayFromKey(it) } ?: date.trim()
    val cleanSlot = timeSlot.trim()

    if (isSlotBooked(facilityId, cleanDate, cleanSlot)) {
      throw IllegalStateException("This slot ($cleanSlot on $cleanDate) is already reserved. Please select another open window.")
    }

    val facility = _facilities.value.find { it.id == facilityId }
    val uid = getCurrentUserId()
    val newBooking = FacilityBooking(
      id = "BK-${System.currentTimeMillis()}-${(100..999).random()}",
      userId = uid,
      facilityId = facilityId,
      facilityName = facility?.name ?: "Estate Amenity",
      residentName = residentName.trim(),
      unit = unit.trim(),
      date = cleanDate,
      timeSlot = cleanSlot,
      guestCount = guestCount,
      notes = notes.trim(),
      confirmedTimestamp = dateFormat.format(Date()) + " " + timeFormat.format(Date())
    )
    val lockId = buildSlotLockId(facilityId, dateKey, cleanSlot)
    val slotLock = BookingSlotLock(
      id = lockId,
      facilityId = facilityId,
      dateKey = dateKey,
      timeSlot = cleanSlot,
      bookingId = newBooking.id,
      bookedByUid = uid
    )

    _bookings.update { listOf(newBooking) + it }
    _slotLocks.update { listOf(slotLock) + it.filterNot { l -> l.id == lockId } }

    db?.let { firestore ->
      val batch = firestore.batch()
      val bookingRef = firestore.collection("bookings").document(newBooking.id)
      val lockRef = firestore.collection("slot_locks").document(lockId)
      batch.set(bookingRef, mapBookingToFirestore(newBooking, uid))
      batch.set(lockRef, mapSlotLockToFirestore(slotLock))
      batch.commit()
        .addOnFailureListener { e ->
          _bookings.update { list -> list.filterNot { it.id == newBooking.id } }
          _slotLocks.update { list -> list.filterNot { it.id == lockId } }
          val msg = "Unable to reserve slot — it may have just been booked by another resident."
          _lastError.value = msg
          handleFirestoreError(e, OperationType.WRITE, "bookings/${newBooking.id}", firebaseAuth)
        }
    }

    return newBooking
  }

  fun cancelBooking(bookingId: String) {
    val existing = _bookings.value.find { it.id == bookingId }
    val matchingLock = _slotLocks.value.find { it.bookingId == bookingId }
      ?: existing?.let { b ->
        val dateKey = EstateDateUtils.toDateKey(b.date) ?: b.date.trim()
        BookingSlotLock(
          id = buildSlotLockId(b.facilityId, dateKey, b.timeSlot),
          facilityId = b.facilityId,
          dateKey = dateKey,
          timeSlot = b.timeSlot,
          bookingId = b.id,
          bookedByUid = b.userId
        )
      }

    _bookings.update { it.filterNot { b -> b.id == bookingId } }
    if (matchingLock != null) {
      _slotLocks.update { it.filterNot { l -> l.id == matchingLock.id || l.bookingId == bookingId } }
    }

    db?.let { firestore ->
      val batch = firestore.batch()
      batch.delete(firestore.collection("bookings").document(bookingId))
      if (matchingLock != null) {
        batch.delete(firestore.collection("slot_locks").document(matchingLock.id))
      }
      batch.commit().addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.DELETE, "bookings/$bookingId", firebaseAuth)
      }
    }
  }

  fun createFacility(
    name: String,
    tag: String,
    capacity: Int,
    location: String,
    description: String,
    amenities: List<String>,
    allowedSlots: List<String>,
    rules: String
  ): Facility {
    val newFacility = Facility(
      id = "FAC-${System.currentTimeMillis()}-${(100..999).random()}",
      name = name.trim(),
      tag = tag.trim(),
      capacity = capacity,
      location = location.trim(),
      description = description.trim(),
      amenities = amenities,
      allowedSlots = allowedSlots,
      rules = rules.trim()
    )
    _facilities.update { listOf(newFacility) + it }

    db?.let { firestore ->
      firestore.collection("facilities").document(newFacility.id)
        .set(mapFacilityToFirestore(newFacility, isCreate = true))
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "facilities/${newFacility.id}", firebaseAuth)
        }
    }
    return newFacility
  }

  fun updateFacility(
    facilityId: String,
    name: String,
    tag: String,
    capacity: Int,
    location: String,
    description: String,
    amenities: List<String>,
    allowedSlots: List<String>,
    rules: String
  ) {
    var updated: Facility? = null
    _facilities.update { list ->
      list.map { f ->
        if (f.id == facilityId) {
          f.copy(
            name = name.trim(),
            tag = tag.trim(),
            capacity = capacity,
            location = location.trim(),
            description = description.trim(),
            amenities = amenities,
            allowedSlots = allowedSlots,
            rules = rules.trim()
          ).also { updated = it }
        } else f
      }
    }
    updated?.let { fac ->
      db?.let { firestore ->
        firestore.collection("facilities").document(facilityId)
          .update(
            mapOf(
              "name" to fac.name,
              "tag" to fac.tag,
              "capacity" to fac.capacity,
              "location" to fac.location,
              "description" to fac.description,
              "amenities" to fac.amenities,
              "allowedSlots" to fac.allowedSlots,
              "rules" to fac.rules,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
          .addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.UPDATE, "facilities/$facilityId", firebaseAuth)
          }
      }
    }
  }

  fun deleteFacility(facilityId: String) {
    _facilities.update { it.filterNot { f -> f.id == facilityId } }
    db?.let { firestore ->
      firestore.collection("facilities").document(facilityId).delete()
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.DELETE, "facilities/$facilityId", firebaseAuth)
        }
    }
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
  ): MaintenanceEvent {
    val normalizedKey = EstateDateUtils.toDateKey(date)
    val cleanDate = if (normalizedKey != null) EstateDateUtils.displayFromKey(normalizedKey) else date.trim()
    val uid = getCurrentUserId()
    val event = MaintenanceEvent(
      id = "MAINT-${System.currentTimeMillis()}-${(100..999).random()}",
      title = title.trim(),
      discipline = discipline,
      scheduledDate = cleanDate,
      scheduledTime = time.trim(),
      contractorName = contractor.trim(),
      targetArea = targetArea.trim(),
      intervalMonths = intervalMonths,
      status = MaintenanceStatus.SCHEDULED,
      notes = notes.trim()
    )
    _maintenances.update { listOf(event) + it }

    db?.let { firestore ->
      val payload = mapMaintenanceToFirestore(event, uid, isCreate = true)
      firestore.collection("maintenances").document(event.id).set(payload)
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "maintenances/${event.id}", firebaseAuth)
        }
    }
    return event
  }

  fun toggleMaintenanceStatus(eventId: String, newStatus: MaintenanceStatus) {
    _maintenances.update { current ->
      current.map { if (it.id == eventId) it.copy(status = newStatus) else it }
    }

    db?.let { firestore ->
      firestore.collection("maintenances").document(eventId).update(
        mapOf(
          "status" to newStatus.name,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "maintenances/$eventId", firebaseAuth)
      }
    }
  }

  fun deleteMaintenance(eventId: String) {
    _maintenances.update { it.filterNot { m -> m.id == eventId } }
    db?.let { firestore ->
      firestore.collection("maintenances").document(eventId).delete()
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.DELETE, "maintenances/$eventId", firebaseAuth)
        }
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
    val uid = getCurrentUserId()
    val newReminder = ReminderItem(
      id = "REM-${System.currentTimeMillis()}-${(100..999).random()}",
      title = title.trim(),
      message = message.trim(),
      targetAudience = targetAudience.trim(),
      urgency = urgency.trim(),
      timestamp = dateFormat.format(Date()) + " • " + timeFormat.format(Date()),
      isBroadcast = isBroadcast
    )
    _reminders.update { listOf(newReminder) + it }

    db?.let { firestore ->
      val payload = mapReminderToFirestore(newReminder, uid)
      firestore.collection("reminders").document(newReminder.id).set(payload)
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "reminders/${newReminder.id}", firebaseAuth)
        }
    }

    return newReminder
  }

  // --- Contract & Bilateral Lease Agreement Operations ---
  fun sendContractRundownNotification(contractId: String) {
    _contracts.update { list ->
      list.map { c -> if (c.id == contractId) c.copy(isNotificationSent = true) else c }
    }
    val contract = _contracts.value.find { it.id == contractId } ?: return

    db?.let { firestore ->
      firestore.collection("contracts").document(contractId).update(
        mapOf(
          "isNotificationSent" to true,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "contracts/$contractId", firebaseAuth)
      }
    }

    sendReminder(
      title = "Contract Expiry Alert: ${contract.title}",
      message = "${contract.title} with ${contract.counterparty} has ${contract.effectiveDaysRemaining} days remaining. Review terms or start renewal.",
      targetAudience = if (contract.type == ContractType.TENANT_LEASE) "Assigned Residents" else "Property Managers & Executive Board",
      urgency = "High Priority"
    )
  }

  fun renewContract(contractId: String, additionalYears: Int = 1) {
    val existing = _contracts.value.find { it.id == contractId }
    val cal = java.util.Calendar.getInstance()
    val parsedKey = existing?.expiryDate?.let { EstateDateUtils.toDateKey(it) }
    if (parsedKey != null) {
      val parts = parsedKey.split("-")
      if (parts.size == 3) {
        cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
      }
    }
    cal.add(java.util.Calendar.YEAR, additionalYears)
    val nextExpiry = dateFormat.format(cal.time)
    val nextDays = EstateDateUtils.daysUntilDisplay(nextExpiry) ?: ((existing?.effectiveDaysRemaining ?: 0) + 365 * additionalYears)

    val nextLeaseStatus = if (existing?.leaseStatus == LeaseContractStatus.EXPIRED) {
      LeaseContractStatus.ACTIVE
    } else {
      existing?.leaseStatus ?: LeaseContractStatus.ACTIVE
    }

    _contracts.update { list ->
      list.map { c ->
        if (c.id == contractId) {
          c.copy(
            daysRemaining = nextDays,
            expiryDate = nextExpiry,
            isNotificationSent = false,
            leaseStatus = nextLeaseStatus
          )
        } else c
      }
    }

    db?.let { firestore ->
      firestore.collection("contracts").document(contractId).update(
        mapOf(
          "daysRemaining" to nextDays,
          "expiryDate" to nextExpiry,
          "renewalDate" to nextExpiry,
          "leaseStatus" to nextLeaseStatus.name,
          "isNotificationSent" to false,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "contracts/$contractId", firebaseAuth)
      }
    }
  }

  fun createContract(
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
    managerName: String = "",
    managerEmail: String = "",
    leaseStatus: LeaseContractStatus? = null
  ): ContractItem {
    val computedDays = EstateDateUtils.daysUntilDisplay(expiryDate) ?: daysRemaining
    val resolvedStatus = leaseStatus ?: if (type == ContractType.TENANT_LEASE) LeaseContractStatus.PENDING_SIGNATURE else LeaseContractStatus.ACTIVE
    val resolvedTenantEmail = tenantEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
      ?: if (type == ContractType.TENANT_LEASE && contactEmail.isNotBlank()) contactEmail.trim().lowercase() else null

    val newContract = ContractItem(
      id = "CTR-${System.currentTimeMillis()}-${(100..999).random()}",
      title = title.trim(),
      counterparty = counterparty.trim(),
      unit = unit.trim().ifBlank { "Estate Wide" },
      type = type,
      annualValue = annualValue.trim(),
      startDate = startDate.trim(),
      expiryDate = expiryDate.trim(),
      daysRemaining = computedDays,
      renewalTerms = renewalTerms.trim(),
      contactPerson = contactPerson.trim(),
      contactEmail = contactEmail.trim(),
      isNotificationSent = false,
      tenantId = tenantId,
      tenantEmail = resolvedTenantEmail,
      propertyId = propertyId,
      managerName = managerName.ifBlank { "Estate Manager" },
      managerEmail = managerEmail.trim().lowercase().ifBlank { getCurrentUserEmail() },
      leaseStatus = resolvedStatus,
      isSignedByTenant = resolvedStatus == LeaseContractStatus.ACTIVE && type != ContractType.TENANT_LEASE
    )
    _contracts.update { listOf(newContract) + it }

    db?.let { firestore ->
      firestore.collection("contracts").document(newContract.id)
        .set(mapContractToFirestore(newContract, isCreate = true))
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "contracts/${newContract.id}", firebaseAuth)
        }
    }
    return newContract
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
    val computedDays = EstateDateUtils.daysUntilDisplay(expiryDate) ?: daysRemaining
    var updated: ContractItem? = null
    _contracts.update { list ->
      list.map { c ->
        if (c.id == contractId) {
          c.copy(
            title = title.trim(),
            counterparty = counterparty.trim(),
            tenant = counterparty.trim(),
            unit = unit?.trim()?.takeIf { it.isNotBlank() } ?: c.unit,
            type = type,
            annualValue = annualValue.trim(),
            contractAmount = annualValue.trim(),
            startDate = startDate.trim(),
            expiryDate = expiryDate.trim(),
            daysRemaining = computedDays,
            renewalTerms = renewalTerms.trim(),
            contactPerson = contactPerson.trim(),
            contactEmail = contactEmail.trim()
          ).also { updated = it }
        } else c
      }
    }
    updated?.let { ctr ->
      db?.let { firestore ->
        firestore.collection("contracts").document(contractId)
          .update(
            mapOf(
              "title" to ctr.title,
              "counterparty" to ctr.counterparty,
              "vendorName" to ctr.counterparty,
              "unit" to ctr.unit,
              "serviceCategory" to ctr.type.name,
              "type" to ctr.type.name,
              "annualValue" to ctr.annualValue,
              "startDate" to ctr.startDate,
              "expiryDate" to ctr.expiryDate,
              "renewalDate" to ctr.expiryDate,
              "daysRemaining" to computedDays,
              "renewalTerms" to ctr.renewalTerms,
              "contactPerson" to ctr.contactPerson,
              "contactEmail" to ctr.contactEmail,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
          .addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.UPDATE, "contracts/$contractId", firebaseAuth)
          }
      }
    }
  }

  fun signTenantContract(contractId: String, signerName: String, signerEmail: String): ContractItem? {
    val cleanName = signerName.trim()
    val cleanEmail = signerEmail.trim().lowercase()
    val signedAtStamp = dateFormat.format(Date()) + " • " + timeFormat.format(Date())
    var signedContract: ContractItem? = null

    _contracts.update { list ->
      list.map { c ->
        if (c.id == contractId) {
          c.copy(
            leaseStatus = LeaseContractStatus.ACTIVE,
            isSignedByTenant = true,
            signedByName = cleanName,
            signedByEmail = cleanEmail,
            signedAt = signedAtStamp
          ).also { signedContract = it }
        } else c
      }
    }

    signedContract?.let { ctr ->
      db?.let { firestore ->
        firestore.collection("contracts").document(contractId).update(
          mapOf(
            "leaseStatus" to LeaseContractStatus.ACTIVE.name,
            "isSignedByTenant" to true,
            "signedByName" to cleanName,
            "signedByEmail" to cleanEmail,
            "signedAt" to signedAtStamp,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.UPDATE, "contracts/$contractId", firebaseAuth)
        }
      }
    }
    return signedContract
  }

  fun deleteContract(contractId: String) {
    _contracts.update { it.filterNot { c -> c.id == contractId } }
    db?.let { firestore ->
      firestore.collection("contracts").document(contractId).delete()
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.DELETE, "contracts/$contractId", firebaseAuth)
        }
    }
  }

  // --- Vendor Management Operations ---
  fun createVendor(
    name: String,
    specialty: CaseCategory,
    phone: String,
    email: String,
    leadTech: String
  ): VendorItem {
    val newVendor = VendorItem(
      id = "VEND-${System.currentTimeMillis()}-${(100..999).random()}",
      name = name.trim(),
      specialty = specialty,
      rating = 0.0f,
      reviewCount = 0,
      activeJobsCount = 0,
      phone = phone.trim(),
      email = email.trim(),
      leadTech = leadTech.trim()
    )
    _vendors.update { listOf(newVendor) + it }

    db?.let { firestore ->
      firestore.collection("vendors").document(newVendor.id)
        .set(mapVendorToFirestore(newVendor, isCreate = true))
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "vendors/${newVendor.id}", firebaseAuth)
        }
    }
    return newVendor
  }

  fun updateVendor(
    vendorId: String,
    name: String,
    specialty: CaseCategory,
    phone: String,
    email: String,
    leadTech: String
  ) {
    var updated: VendorItem? = null
    _vendors.update { list ->
      list.map { v ->
        if (v.id == vendorId) {
          v.copy(
            name = name.trim(),
            specialty = specialty,
            phone = phone.trim(),
            phoneNumber = phone.trim(),
            email = email.trim(),
            leadTech = leadTech.trim()
          ).also { updated = it }
        } else v
      }
    }
    updated?.let { vend ->
      db?.let { firestore ->
        firestore.collection("vendors").document(vendorId)
          .update(
            mapOf(
              "name" to vend.name,
              "category" to vend.specialty.name,
              "specialty" to vend.specialty.name,
              "phone" to vend.phone,
              "email" to vend.email,
              "leadTech" to vend.leadTech,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
          .addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.UPDATE, "vendors/$vendorId", firebaseAuth)
          }
      }
    }
  }

  fun deleteVendor(vendorId: String) {
    _vendors.update { it.filterNot { v -> v.id == vendorId } }
    db?.let { firestore ->
      firestore.collection("vendors").document(vendorId).delete()
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.DELETE, "vendors/$vendorId", firebaseAuth)
        }
    }
  }

  // --- Property & Tenant Management Operations ---
  fun listProperty(
    unitNumber: String,
    pavilionWing: String,
    architecture: PropertyArchitecture,
    parkingSpots: List<String>,
    amenities: List<String>,
    monthlyRental: String
  ): PropertyItem {
    val newProp = PropertyItem(
      id = "PROP-${System.currentTimeMillis()}-${(100..999).random()}",
      unitNumber = unitNumber.trim(),
      pavilionWing = pavilionWing.trim(),
      architecture = architecture,
      parkingSpots = parkingSpots,
      amenities = amenities,
      monthlyRental = monthlyRental.trim(),
      status = PropertyStatus.AVAILABLE,
      assignedTenant = null,
      listedDate = dateFormat.format(Date())
    )
    _properties.update { listOf(newProp) + it }

    db?.let { firestore ->
      val payload = mapPropertyToFirestore(newProp, isCreate = true)
      firestore.collection("properties").document(newProp.id).set(payload)
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "properties/${newProp.id}", firebaseAuth)
        }
    }

    return newProp
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
    var updated: PropertyItem? = null
    _properties.update { list ->
      list.map { p ->
        if (p.id == propertyId) {
          p.copy(
            unitNumber = unitNumber.trim(),
            pavilionWing = pavilionWing.trim(),
            architecture = architecture,
            parkingSpots = parkingSpots,
            amenities = amenities,
            monthlyRental = monthlyRental.trim()
          ).also { updated = it }
        } else p
      }
    }
    updated?.let { prop ->
      db?.let { firestore ->
        firestore.collection("properties").document(propertyId)
          .update(
            mapOf(
              "unitNumber" to prop.unitNumber,
              "pavilionWing" to prop.pavilionWing,
              "bedrooms" to prop.architecture.bedrooms,
              "bathrooms" to prop.architecture.bathrooms.toDouble(),
              "sqFt" to prop.architecture.sqFt,
              "floorLevel" to prop.architecture.floorLevel,
              "viewType" to prop.architecture.viewType,
              "parkingSpots" to prop.parkingSpots,
              "amenities" to prop.amenities,
              "monthlyRental" to prop.monthlyRental,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
          .addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.UPDATE, "properties/$propertyId", firebaseAuth)
          }
      }
    }
  }

  fun deleteProperty(propertyId: String) {
    _properties.update { it.filterNot { p -> p.id == propertyId } }
    val affectedTenants = _tenants.value.filter { it.assignedPropertyId == propertyId }
    _tenants.update { list ->
      list.map { t ->
        if (t.assignedPropertyId == propertyId) {
          t.copy(assignedPropertyId = null, assignedPropertyUnit = null, leaseStartDate = null, leaseEndDate = null)
        } else t
      }
    }
    db?.let { firestore ->
      firestore.collection("properties").document(propertyId).delete()
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.DELETE, "properties/$propertyId", firebaseAuth)
        }
      affectedTenants.forEach { t ->
        firestore.collection("tenants").document(t.id).update(
          mapOf(
            "assignedPropertyId" to null,
            "assignedPropertyUnit" to null,
            "leaseStartDate" to null,
            "leaseEndDate" to null,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.UPDATE, "tenants/${t.id}", firebaseAuth)
        }
      }
    }
  }

  private fun createAutoLeaseContract(
    property: PropertyItem,
    tenantName: String,
    tenantEmail: String,
    tenantId: String?,
    leaseStartDate: String,
    leaseEndDate: String,
    managerName: String,
    managerEmail: String
  ): ContractItem {
    val cleanEmail = tenantEmail.trim().lowercase()
    val effectiveManagerName = managerName.ifBlank { "Estate Manager" }
    val effectiveManagerEmail = managerEmail.trim().lowercase().ifBlank { getCurrentUserEmail() }
    val rentalVal = property.monthlyRental.ifBlank { "Per Lease Agreement" }
    val endStr = leaseEndDate.trim().ifBlank { EstateDateUtils.addMonthsDisplay(leaseStartDate, 12) }
    val startStr = leaseStartDate.trim().ifBlank { EstateDateUtils.todayDisplay() }

    return createContract(
      title = "Residential Lease — ${property.unitNumber}",
      counterparty = tenantName.trim(),
      type = ContractType.TENANT_LEASE,
      annualValue = rentalVal,
      startDate = startStr,
      expiryDate = endStr,
      daysRemaining = EstateDateUtils.daysUntilDisplay(endStr) ?: 365,
      renewalTerms = "Bilateral lease agreement between $effectiveManagerName and ${tenantName.trim()} for ${property.unitNumber} (${property.pavilionWing}). Monthly valuation: $rentalVal. Notice required 60 days prior to expiry.",
      contactPerson = effectiveManagerName,
      contactEmail = effectiveManagerEmail,
      unit = property.unitNumber,
      tenantId = tenantId,
      tenantEmail = cleanEmail,
      propertyId = property.id,
      managerName = effectiveManagerName,
      managerEmail = effectiveManagerEmail,
      leaseStatus = LeaseContractStatus.PENDING_SIGNATURE
    )
  }

  fun assignPropertyToTenant(
    propertyId: String,
    tenantName: String,
    tenantEmail: String,
    leaseStartDate: String,
    leaseEndDate: String,
    managerName: String = "",
    managerEmail: String = ""
  ): AssignedTenantInfo {
    val cleanName = tenantName.trim()
    val cleanEmail = tenantEmail.trim().lowercase()
    val cleanStart = leaseStartDate.trim()
    val cleanEnd = leaseEndDate.trim()
    val property = _properties.value.find { it.id == propertyId }

    val existingTenant = _tenants.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
    val codeSuffix = (propertyId.replace("PROP-", "") + "-" + (System.currentTimeMillis() % 1000)).uppercase()
    val inviteCode = existingTenant?.inviteCode?.takeIf { it.isNotBlank() } ?: "TUTS-$codeSuffix"
    val inviteLink = "tutsville://onboard?propertyId=$propertyId&email=$cleanEmail&code=$inviteCode"
    val tenantInfo = AssignedTenantInfo(
      name = cleanName,
      email = cleanEmail,
      leaseStartDate = cleanStart,
      leaseEndDate = cleanEnd,
      inviteCode = inviteCode,
      inviteLink = inviteLink,
      isClaimed = existingTenant?.isAppRegistered ?: false,
      assignedTimestamp = dateFormat.format(Date()) + " • " + timeFormat.format(Date())
    )

    // 1. Update Property state
    _properties.update { list ->
      list.map { p ->
        if (p.id == propertyId) {
          p.copy(status = PropertyStatus.OCCUPIED, assignedTenant = tenantInfo)
        } else p
      }
    }

    // 2. Sync Tenant record (update existing OR create new TenantProfile so resident login recognizes them!)
    val syncedTenant: TenantProfile
    val isNewTenantProfile: Boolean
    if (existingTenant != null) {
      isNewTenantProfile = false
      syncedTenant = existingTenant.copy(
        fullName = cleanName.ifBlank { existingTenant.fullName },
        assignedPropertyId = propertyId,
        assignedPropertyUnit = property?.unitNumber ?: existingTenant.assignedPropertyUnit,
        leaseStartDate = cleanStart,
        leaseEndDate = cleanEnd,
        inviteCode = inviteCode
      )
      _tenants.update { list ->
        list.map { t -> if (t.id == existingTenant.id) syncedTenant else t }
      }
    } else {
      isNewTenantProfile = true
      val parts = cleanName.split(" ").filter { it.isNotBlank() }
      syncedTenant = TenantProfile(
        id = "TENANT-${System.currentTimeMillis()}-${(100..999).random()}",
        firstName = parts.firstOrNull().orEmpty(),
        lastName = parts.drop(1).joinToString(" "),
        fullName = cleanName,
        email = cleanEmail,
        phone = "",
        emergencyContactName = "",
        emergencyContactPhone = "",
        assignedPropertyId = propertyId,
        assignedPropertyUnit = property?.unitNumber ?: "",
        leaseStartDate = cleanStart,
        leaseEndDate = cleanEnd,
        inviteCode = inviteCode,
        isAppRegistered = false,
        createdAt = dateFormat.format(Date())
      )
      _tenants.update { listOf(syncedTenant) + it }
    }

    db?.let { firestore ->
      val tenantMap = mapOf(
        "name" to tenantInfo.name,
        "email" to tenantInfo.email,
        "leaseStartDate" to tenantInfo.leaseStartDate,
        "leaseEndDate" to tenantInfo.leaseEndDate,
        "inviteCode" to tenantInfo.inviteCode,
        "inviteLink" to tenantInfo.inviteLink,
        "isClaimed" to tenantInfo.isClaimed,
        "assignedTimestamp" to tenantInfo.assignedTimestamp
      )
      firestore.collection("properties").document(propertyId).update(
        mapOf(
          "status" to PropertyStatus.OCCUPIED.name,
          "tenantEmail" to cleanEmail,
          "assignedTenant" to tenantMap,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "properties/$propertyId", firebaseAuth)
      }

      if (isNewTenantProfile) {
        firestore.collection("tenants").document(syncedTenant.id)
          .set(mapTenantToFirestore(syncedTenant, isCreate = true))
          .addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.CREATE, "tenants/${syncedTenant.id}", firebaseAuth)
          }
      } else {
        firestore.collection("tenants").document(syncedTenant.id).update(
          mapOf(
            "assignedPropertyId" to propertyId,
            "assignedPropertyUnit" to (property?.unitNumber ?: ""),
            "leaseStartDate" to cleanStart,
            "leaseEndDate" to cleanEnd,
            "inviteCode" to inviteCode,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.UPDATE, "tenants/${syncedTenant.id}", firebaseAuth)
        }
      }
    }

    // 3. Auto-create bilateral Lease Contract awaiting tenant signature
    if (property != null) {
      createAutoLeaseContract(
        property = property,
        tenantName = cleanName,
        tenantEmail = cleanEmail,
        tenantId = syncedTenant.id,
        leaseStartDate = cleanStart,
        leaseEndDate = cleanEnd,
        managerName = managerName,
        managerEmail = managerEmail
      )
    }

    return tenantInfo
  }

  fun claimPropertyInvite(email: String): PropertyItem? {
    val normalized = email.trim().lowercase()
    var claimedProp: PropertyItem? = null
    _properties.update { list ->
      list.map { p ->
        if (p.assignedTenant?.email?.equals(normalized, ignoreCase = true) == true) {
          val updatedTenant = p.assignedTenant.copy(isClaimed = true)
          p.copy(assignedTenant = updatedTenant).also { claimedProp = it }
        } else p
      }
    }

    claimedProp?.let { prop ->
      db?.let { firestore ->
        firestore.collection("properties").document(prop.id).update(
          mapOf(
            "assignedTenant.isClaimed" to true,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.UPDATE, "properties/${prop.id}", firebaseAuth)
        }
      }
    }

    return claimedProp
  }

  fun vacateProperty(propertyId: String) {
    val affectedTenants = _tenants.value.filter { it.assignedPropertyId == propertyId }
    val affectedContracts = _contracts.value.filter {
      it.propertyId == propertyId &&
        it.type == ContractType.TENANT_LEASE &&
        (it.leaseStatus == LeaseContractStatus.PENDING_SIGNATURE || it.leaseStatus == LeaseContractStatus.ACTIVE)
    }

    _properties.update { list ->
      list.map { p ->
        if (p.id == propertyId) p.copy(status = PropertyStatus.AVAILABLE, assignedTenant = null) else p
      }
    }
    _tenants.update { list ->
      list.map { t ->
        if (t.assignedPropertyId == propertyId) {
          t.copy(assignedPropertyId = null, assignedPropertyUnit = null, leaseStartDate = null, leaseEndDate = null)
        } else t
      }
    }
    if (affectedContracts.isNotEmpty()) {
      _contracts.update { list ->
        list.map { c ->
          if (affectedContracts.any { it.id == c.id }) c.copy(leaseStatus = LeaseContractStatus.TERMINATED) else c
        }
      }
    }

    db?.let { firestore ->
      firestore.collection("properties").document(propertyId).update(
        mapOf(
          "status" to PropertyStatus.AVAILABLE.name,
          "tenantEmail" to "",
          "assignedTenant" to null,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "properties/$propertyId", firebaseAuth)
      }

      affectedTenants.forEach { t ->
        firestore.collection("tenants").document(t.id).update(
          mapOf(
            "assignedPropertyId" to null,
            "assignedPropertyUnit" to null,
            "leaseStartDate" to null,
            "leaseEndDate" to null,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.UPDATE, "tenants/${t.id}", firebaseAuth)
        }
      }

      affectedContracts.forEach { ctr ->
        firestore.collection("contracts").document(ctr.id).update(
          mapOf(
            "leaseStatus" to LeaseContractStatus.TERMINATED.name,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.UPDATE, "contracts/${ctr.id}", firebaseAuth)
        }
      }
    }
  }

  // --- Tenant Operations ---
  fun createTenant(
    fullName: String,
    email: String,
    phone: String,
    emergencyContactName: String,
    emergencyContactPhone: String,
    nationalId: String = "",
    address: String = ""
  ): TenantProfile {
    val cleanFull = fullName.trim()
    val nameParts = cleanFull.split(" ").filter { it.isNotBlank() }
    val first = nameParts.firstOrNull().orEmpty()
    val last = nameParts.drop(1).joinToString(" ")
    val cleanEmail = email.trim().lowercase()
    val codeSuffix = (System.currentTimeMillis() % 10000).toString().padStart(4, '0')
    val inviteCode = "TUTS-RES-$codeSuffix"
    val newTenant = TenantProfile(
      id = "TENANT-${System.currentTimeMillis()}-${(100..999).random()}",
      firstName = first,
      lastName = last,
      fullName = cleanFull,
      nationalId = nationalId.trim(),
      address = address.trim(),
      email = cleanEmail,
      phone = phone.trim(),
      emergencyContactName = emergencyContactName.trim(),
      emergencyContactPhone = emergencyContactPhone.trim(),
      assignedPropertyId = null,
      assignedPropertyUnit = null,
      leaseStartDate = null,
      leaseEndDate = null,
      inviteCode = inviteCode,
      isAppRegistered = false,
      createdAt = dateFormat.format(Date())
    )
    _tenants.update { listOf(newTenant) + it }

    db?.let { firestore ->
      val payload = mapTenantToFirestore(newTenant, isCreate = true)
      firestore.collection("tenants").document(newTenant.id).set(payload)
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "tenants/${newTenant.id}", firebaseAuth)
        }
    }

    return newTenant
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
    val combinedName = "$firstName $lastName".trim()
    var updated: TenantProfile? = null
    _tenants.update { list ->
      list.map { t ->
        if (t.id == tenantId) {
          t.copy(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            fullName = combinedName,
            nationalId = nationalId.trim(),
            address = address.trim(),
            email = email.trim().lowercase(),
            phone = phone.trim(),
            emergencyContactName = emergencyContactName.trim(),
            emergencyContactPhone = emergencyContactPhone.trim()
          ).also { updated = it }
        } else t
      }
    }
    updated?.let { tenant ->
      db?.let { firestore ->
        firestore.collection("tenants").document(tenantId)
          .update(
            mapOf(
              "firstName" to tenant.firstName,
              "lastName" to tenant.lastName,
              "fullName" to tenant.fullName,
              "nationalId" to tenant.nationalId,
              "address" to tenant.address,
              "email" to tenant.email,
              "phone" to tenant.phone,
              "emergencyContactName" to tenant.emergencyContactName,
              "emergencyContactPhone" to tenant.emergencyContactPhone,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
          .addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.UPDATE, "tenants/$tenantId", firebaseAuth)
          }
      }
    }
  }

  fun deleteTenant(tenantId: String) {
    unassignTenant(tenantId)
    _tenants.update { it.filterNot { t -> t.id == tenantId } }
    db?.let { firestore ->
      firestore.collection("tenants").document(tenantId).delete()
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.DELETE, "tenants/$tenantId", firebaseAuth)
        }
    }
  }

  fun assignTenantToPropertyById(
    tenantId: String,
    propertyId: String,
    leaseStartDate: String,
    leaseEndDate: String,
    managerName: String = "",
    managerEmail: String = ""
  ): TenantProfile? {
    val tenant = _tenants.value.find { it.id == tenantId } ?: return null
    val property = _properties.value.find { it.id == propertyId } ?: return null

    val inviteCode = tenant.inviteCode
    val cleanEmail = tenant.email.lowercase()
    val cleanStart = leaseStartDate.trim()
    val cleanEnd = leaseEndDate.trim()
    val inviteLink = "tutsville://onboard?propertyId=$propertyId&email=$cleanEmail&code=$inviteCode"
    val tenantInfo = AssignedTenantInfo(
      name = tenant.fullName,
      email = cleanEmail,
      leaseStartDate = cleanStart,
      leaseEndDate = cleanEnd,
      inviteCode = inviteCode,
      inviteLink = inviteLink,
      isClaimed = tenant.isAppRegistered,
      assignedTimestamp = dateFormat.format(Date()) + " • " + timeFormat.format(Date())
    )

    _properties.update { list ->
      list.map { p ->
        if (p.id == propertyId) p.copy(status = PropertyStatus.OCCUPIED, assignedTenant = tenantInfo) else p
      }
    }

    var updatedTenant: TenantProfile? = null
    _tenants.update { list ->
      list.map { t ->
        if (t.id == tenantId) {
          t.copy(
            assignedPropertyId = propertyId,
            assignedPropertyUnit = property.unitNumber,
            leaseStartDate = cleanStart,
            leaseEndDate = cleanEnd
          ).also { updatedTenant = it }
        } else t
      }
    }

    db?.let { firestore ->
      val tenantMap = mapOf(
        "name" to tenantInfo.name,
        "email" to tenantInfo.email,
        "leaseStartDate" to tenantInfo.leaseStartDate,
        "leaseEndDate" to tenantInfo.leaseEndDate,
        "inviteCode" to tenantInfo.inviteCode,
        "inviteLink" to tenantInfo.inviteLink,
        "isClaimed" to tenantInfo.isClaimed,
        "assignedTimestamp" to tenantInfo.assignedTimestamp
      )
      firestore.collection("properties").document(propertyId).update(
        mapOf(
          "status" to PropertyStatus.OCCUPIED.name,
          "tenantEmail" to cleanEmail,
          "assignedTenant" to tenantMap,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "properties/$propertyId", firebaseAuth)
      }

      firestore.collection("tenants").document(tenantId).update(
        mapOf(
          "assignedPropertyId" to propertyId,
          "assignedPropertyUnit" to property.unitNumber,
          "leaseStartDate" to cleanStart,
          "leaseEndDate" to cleanEnd,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "tenants/$tenantId", firebaseAuth)
      }
    }

    createAutoLeaseContract(
      property = property,
      tenantName = tenant.fullName,
      tenantEmail = cleanEmail,
      tenantId = tenant.id,
      leaseStartDate = cleanStart,
      leaseEndDate = cleanEnd,
      managerName = managerName,
      managerEmail = managerEmail
    )

    return updatedTenant
  }

  fun unassignTenant(tenantId: String) {
    val tenant = _tenants.value.find { it.id == tenantId }
    val propId = tenant?.assignedPropertyId

    if (propId != null) {
      vacateProperty(propId)
    }

    _tenants.update { list ->
      list.map { t ->
        if (t.id == tenantId) {
          t.copy(assignedPropertyId = null, assignedPropertyUnit = null, leaseStartDate = null, leaseEndDate = null)
        } else t
      }
    }

    db?.let { firestore ->
      firestore.collection("tenants").document(tenantId).update(
        mapOf(
          "assignedPropertyId" to null,
          "assignedPropertyUnit" to null,
          "leaseStartDate" to null,
          "leaseEndDate" to null,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "tenants/$tenantId", firebaseAuth)
      }
    }
  }

  fun claimTenantInvite(email: String, code: String? = null): TenantProfile? {
    val cleanEmail = email.trim().lowercase()
    var claimedTenant: TenantProfile? = null
    _tenants.update { list ->
      list.map { t ->
        val emailMatch = t.email.equals(cleanEmail, ignoreCase = true)
        val codeMatch = code != null && t.inviteCode.equals(code.trim(), ignoreCase = true)
        if (emailMatch || codeMatch) {
          t.copy(isAppRegistered = true).also { claimedTenant = it }
        } else t
      }
    }
    claimPropertyInvite(cleanEmail)

    claimedTenant?.let { t ->
      db?.let { firestore ->
        firestore.collection("tenants").document(t.id).update(
          mapOf(
            "isAppRegistered" to true,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.UPDATE, "tenants/${t.id}", firebaseAuth)
        }
      }
    }

    return claimedTenant
  }

  fun findTenantByInviteCode(code: String): TenantProfile? {
    val clean = code.trim()
    return _tenants.value.find { it.inviteCode.equals(clean, ignoreCase = true) }
  }
}
