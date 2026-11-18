package com.example.tutsville.data.repository

import android.util.Log
import com.example.tutsville.data.model.*
import com.example.tutsville.data.repository.EstateFirestoreMappers.buildHourlySlotLockId
import com.example.tutsville.data.repository.EstateFirestoreMappers.buildSlotLockId
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToBooking
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToCase
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToContract
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToEstateConfig
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToFacility
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToMaintenance
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToManagerInvite
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToProperty
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToReminder
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToSlotLock
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToTenant
import com.example.tutsville.data.repository.EstateFirestoreMappers.docToVendor
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapBookingToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapCaseMovement
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapCaseNote
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapCaseToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapConfigToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapContractToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapDocumentMeta
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapFacilityToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapMaintenanceToFirestore
import com.example.tutsville.data.repository.EstateFirestoreMappers.mapManagerInviteToFirestore
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

  private val _readNoticeIds = MutableStateFlow<Set<String>>(emptySet())
  val readNoticeIds: StateFlow<Set<String>> = _readNoticeIds.asStateFlow()

  private val _properties = MutableStateFlow<List<PropertyItem>>(emptyList())
  val properties: StateFlow<List<PropertyItem>> = _properties.asStateFlow()

  private val _tenants = MutableStateFlow<List<TenantProfile>>(emptyList())
  val tenants: StateFlow<List<TenantProfile>> = _tenants.asStateFlow()

  private val _estateConfigOptions = MutableStateFlow(EstateConfigOptions())
  val estateConfigOptions: StateFlow<EstateConfigOptions> = _estateConfigOptions.asStateFlow()

  private val _managerInvites = MutableStateFlow<List<ManagerInvite>>(emptyList())
  val managerInvites: StateFlow<List<ManagerInvite>> = _managerInvites.asStateFlow()

  private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
  private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

  private val collectionListeners = mutableListOf<ListenerRegistration>()
  private var adminDocListener: ListenerRegistration? = null
  private var currentListeningUid: String? = null
  private var currentListeningEmailVerified: Boolean? = null
  private var currentListeningAdminMode: Boolean? = null
  private val cleanedLegacyReminderIds = mutableSetOf<String>()

  private var actorDisplayName: String? = null
  private var actorEmail: String? = null

  fun setActorContext(displayName: String?, email: String?) {
    actorDisplayName = displayName?.trim()?.takeIf { it.isNotBlank() }
    actorEmail = email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
  }

  private data class ActorIdentity(
    val uid: String,
    val name: String,
    val email: String,
    val role: UserRole
  )

  private fun resolveActorIdentity(): ActorIdentity {
    val uid = getCurrentUserId()
    val authEmail = getCurrentUserEmail()
    val role = if (db == null) {
      val cleanActorEmail = actorEmail?.trim()?.lowercase().orEmpty()
      val matchedTenant = if (cleanActorEmail.isNotBlank()) {
        _tenants.value.find { it.email.equals(cleanActorEmail, ignoreCase = true) }
      } else null
      if (matchedTenant != null) UserRole.TENANT else UserRole.MANAGER
    } else {
      if (_isCurrentUserAdmin.value) UserRole.MANAGER else UserRole.TENANT
    }
    val effectiveEmail = authEmail.ifBlank { actorEmail?.trim()?.lowercase().orEmpty() }
    val authName = try {
      firebaseAuth?.currentUser?.displayName
    } catch (_: Exception) {
      null
    }
    val tenantName = if (effectiveEmail.isNotBlank()) {
      _tenants.value.find { it.email.equals(effectiveEmail, ignoreCase = true) }?.fullName
    } else null
    val resolvedName = when {
      !authName.isNullOrBlank() -> authName.trim()
      !actorDisplayName.isNullOrBlank() -> actorDisplayName!!.trim()
      !tenantName.isNullOrBlank() -> tenantName.trim()
      effectiveEmail.isNotBlank() -> effectiveEmail.substringBefore("@")
      role == UserRole.MANAGER -> "Estate Manager"
      else -> "Estate Resident"
    }
    return ActorIdentity(
      uid = uid,
      name = resolvedName,
      email = effectiveEmail,
      role = role
    )
  }

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
    currentListeningEmailVerified = null
    currentListeningAdminMode = null
    handleAuthState(firebaseAuth?.currentUser)
  }

  fun clearError() {
    _lastError.value = null
  }

  private fun handleAuthState(user: FirebaseUser?) {
    val firestore = db ?: return
    if (user == null) {
      detachFirestoreListeners()
      clearStateFlows()
      _isCurrentUserAdmin.value = false
      _isRoleResolved.value = false
      isLoading.value = false
      return
    }

    if (currentListeningUid == user.uid && currentListeningEmailVerified == user.isEmailVerified) {
      return
    }

    detachFirestoreListeners()
    currentListeningUid = user.uid
    currentListeningEmailVerified = user.isEmailVerified
    _isRoleResolved.value = false
    isLoading.value = true

    val email = user.email?.trim()?.lowercase().orEmpty()
    val isBootstrapEmail = email == BOOTSTRAP_MANAGER_EMAIL && user.isEmailVerified

    if (isBootstrapEmail) {
      ensureAdminDocAndListen(firestore, user, email)
    } else if (user.isEmailVerified && email.isNotBlank()) {
      firestore.collection("admins").document(user.uid).get()
        .addOnSuccessListener { adminSnap ->
          if (currentListeningUid != user.uid) return@addOnSuccessListener
          if (adminSnap.exists()) {
            attachAdminDocListener(firestore, user)
          } else {
            firestore.collection("manager_invites").document(email).get()
              .addOnSuccessListener { inviteSnap ->
                if (currentListeningUid != user.uid) return@addOnSuccessListener
                if (inviteSnap.exists()) {
                  val adminPayload = mapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "displayName" to (user.displayName ?: inviteSnap.getString("displayName") ?: "Estate Manager"),
                    "role" to "ADMIN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                  )
                  firestore.collection("admins").document(user.uid).set(adminPayload)
                    .addOnSuccessListener {
                      if (currentListeningUid != user.uid) return@addOnSuccessListener
                      firestore.collection("manager_invites").document(email).update(
                        mapOf(
                          "status" to "CLAIMED",
                          "updatedAt" to FieldValue.serverTimestamp()
                        )
                      )
                      attachAdminDocListener(firestore, user)
                    }
                    .addOnFailureListener { e ->
                      if (currentListeningUid != user.uid) return@addOnFailureListener
                      handleFirestoreError(e, OperationType.CREATE, "admins/${user.uid}", firebaseAuth)
                      attachAdminDocListener(firestore, user)
                    }
                } else {
                  attachAdminDocListener(firestore, user)
                }
              }
              .addOnFailureListener {
                if (currentListeningUid != user.uid) return@addOnFailureListener
                attachAdminDocListener(firestore, user)
              }
          }
        }
        .addOnFailureListener {
          if (currentListeningUid != user.uid) return@addOnFailureListener
          attachAdminDocListener(firestore, user)
        }
    } else {
      attachAdminDocListener(firestore, user)
    }
  }

  private fun ensureAdminDocAndListen(
    firestore: FirebaseFirestore,
    user: FirebaseUser,
    email: String
  ) {
    val adminRef = firestore.collection("admins").document(user.uid)
    adminRef.get()
      .addOnSuccessListener { doc ->
        if (currentListeningUid != user.uid) return@addOnSuccessListener
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
              if (currentListeningUid != user.uid) return@addOnSuccessListener
              attachAdminDocListener(firestore, user)
            }
            .addOnFailureListener { e ->
              if (currentListeningUid != user.uid) return@addOnFailureListener
              handleFirestoreError(e, OperationType.CREATE, "admins/${user.uid}", firebaseAuth)
              attachAdminDocListener(firestore, user)
            }
        } else {
          attachAdminDocListener(firestore, user)
        }
      }
      .addOnFailureListener { e ->
        if (currentListeningUid != user.uid) return@addOnFailureListener
        handleFirestoreError(e, OperationType.GET, "admins/${user.uid}", firebaseAuth)
        attachAdminDocListener(firestore, user)
      }
  }

  private fun attachAdminDocListener(firestore: FirebaseFirestore, user: FirebaseUser) {
    if (currentListeningUid != user.uid) return
    adminDocListener?.remove()
    adminDocListener = firestore.collection("admins").document(user.uid)
      .addSnapshotListener { snapshot, error ->
        if (currentListeningUid != user.uid) return@addSnapshotListener
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

  private fun detachFirestoreListeners() {
    detachCollectionListeners()
    adminDocListener?.remove()
    adminDocListener = null
    currentListeningUid = null
    currentListeningEmailVerified = null
  }

  fun detachAllListeners() {
    firebaseAuth?.removeAuthStateListener(authStateListener)
    detachFirestoreListeners()
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
    _readNoticeIds.value = emptySet()
    _vendors.value = emptyList()
    _managerInvites.value = emptyList()
    _estateConfigOptions.value = EstateConfigOptions()
    cleanedLegacyReminderIds.clear()
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
    listenQuery(firestore.collection("vendors"), "vendors") { docs ->
      _vendors.value = docs.mapNotNull { docToVendor(it) }
    }
    listenQuery(firestore.collection("slot_locks"), "slot_locks") { docs ->
      _slotLocks.value = docs.mapNotNull { docToSlotLock(it) }
    }

    val configReg = firestore.collection("estate_config").document("options")
      .addSnapshotListener { snap, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.GET, "estate_config/options", firebaseAuth)
          return@addSnapshotListener
        }
        if (snap != null && snap.exists()) {
          _estateConfigOptions.value = docToEstateConfig(snap)
        }
      }
    collectionListeners.add(configReg)

    // Per-user synced notification read state
    val readStateReg = firestore.collection("user_notice_states").document(uid)
      .addSnapshotListener { snap, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.GET, "user_notice_states/$uid", firebaseAuth)
          return@addSnapshotListener
        }
        val ids = (snap?.get("readNoticeIds") as? List<*>)
          ?.mapNotNull { it as? String }
          ?.toSet()
          ?: emptySet()
        _readNoticeIds.value = ids
      }
    collectionListeners.add(readStateReg)

    if (isAdmin) {
      listenQuery(firestore.collection("reminders"), "reminders") { docs ->
        val attributed = mutableListOf<ReminderItem>()
        val legacyDocIds = mutableListOf<String>()
        docs.forEach { doc ->
          val parsed = docToReminder(doc)
          if (parsed != null) {
            attributed.add(parsed)
          } else if (cleanedLegacyReminderIds.add(doc.id)) {
            legacyDocIds.add(doc.id)
          }
        }
        _reminders.value = attributed.sortedByDescending { it.createdAtEpochMs }
        if (legacyDocIds.isNotEmpty()) {
          purgeLegacyReminderDocs(firestore, legacyDocIds)
        }
      }
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
      listenQuery(firestore.collection("manager_invites"), "manager_invites") { docs ->
        _managerInvites.value = docs.mapNotNull { docToManagerInvite(it) }
      }
    } else {
      // Resident / Non-admin strictly scoped queries matching firestore.rules
      listenQuery(firestore.collection("bookings").whereEqualTo("userId", uid), "bookings") { docs ->
        _bookings.value = docs.mapNotNull { docToBooking(it) }
      }

      if (hasVerifiedEmail) {
        var broadcastReminders = emptyList<ReminderItem>()
        var personalReminders = emptyList<ReminderItem>()
        var multiRecipientReminders = emptyList<ReminderItem>()
        fun mergeReminders() {
          _reminders.value = (broadcastReminders + personalReminders + multiRecipientReminders)
            .distinctBy { it.id }
            .sortedByDescending { it.createdAtEpochMs }
        }
        listenQuery(
          firestore.collection("reminders").whereIn(
            "targetAudience",
            listOf("All Residents & Tenants", "ALL_RESIDENTS", "ALL_USERS", "All Residents")
          ),
          "reminders(broadcast)"
        ) { docs ->
          broadcastReminders = docs.mapNotNull { docToReminder(it) }
          mergeReminders()
        }
        listenQuery(
          firestore.collection("reminders").whereEqualTo("targetEmail", cleanEmail),
          "reminders(personal)"
        ) { docs ->
          personalReminders = docs.mapNotNull { docToReminder(it) }
          mergeReminders()
        }
        listenQuery(
          firestore.collection("reminders").whereArrayContains("targetEmails", cleanEmail),
          "reminders(targeted)"
        ) { docs ->
          multiRecipientReminders = docs.mapNotNull { docToReminder(it) }
          mergeReminders()
        }

        listenQuery(firestore.collection("tenants").whereEqualTo("email", cleanEmail), "tenants") { docs ->
          _tenants.value = docs.mapNotNull { docToTenant(it) }
        }
        listenQuery(firestore.collection("properties").whereEqualTo("tenantEmail", cleanEmail), "properties") { docs ->
          _properties.value = docs.mapNotNull { docToProperty(it) }
        }
        listenQuery(firestore.collection("contracts").whereEqualTo("tenantEmail", cleanEmail), "contracts") { docs ->
          _contracts.value = docs.mapNotNull { docToContract(it) }
        }

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
        _reminders.value = emptyList()
        _tenants.value = emptyList()
        _properties.value = emptyList()
        _contracts.value = emptyList()
        listenQuery(firestore.collection("cases").whereEqualTo("userId", uid), "cases(userId)") { docs ->
          _cases.value = docs.mapNotNull { docToCase(it) }
        }
      }
    }
  }

  private fun purgeLegacyReminderDocs(firestore: FirebaseFirestore, legacyIds: List<String>) {
    legacyIds.chunked(400).forEach { chunk ->
      val batch = firestore.batch()
      chunk.forEach { docId ->
        batch.delete(firestore.collection("reminders").document(docId))
      }
      batch.commit().addOnFailureListener { e ->
        Log.w("EstateRepository", "Legacy reminder cleanup skipped: ${e.message}")
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

  // --- Estate Config Options & Manager Invites ---
  fun updateConfigOptions(
    floorElevations: List<String>,
    contractorDisciplines: List<String>
  ) {
    val cleanElevations = floorElevations.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
      .ifEmpty { EstateConfigOptions.DEFAULT_FLOOR_ELEVATIONS }
    val cleanDisciplines = contractorDisciplines.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
      .ifEmpty { EstateConfigOptions.DEFAULT_CONTRACTOR_DISCIPLINES }

    val updated = EstateConfigOptions(
      id = "options",
      floorElevations = cleanElevations,
      contractorDisciplines = cleanDisciplines
    )
    _estateConfigOptions.value = updated

    db?.let { firestore ->
      firestore.collection("estate_config").document("options")
        .set(
          mapConfigToFirestore(updated, isCreate = false),
          com.google.firebase.firestore.SetOptions.merge()
        )
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.WRITE, "estate_config/options", firebaseAuth)
        }
    }
  }

  fun inviteManager(
    email: String,
    displayName: String,
    onResult: ((Result<ManagerInvite>) -> Unit)? = null
  ): ManagerInvite {
    val cleanEmail = email.trim().lowercase()
    require(cleanEmail.contains("@") && cleanEmail.length in 3..200) {
      "Please enter a valid Google account email address."
    }
    val invite = ManagerInvite(
      id = cleanEmail,
      email = cleanEmail,
      displayName = displayName.trim().ifBlank { cleanEmail.substringBefore("@") },
      invitedByEmail = getCurrentUserEmail().ifBlank { BOOTSTRAP_MANAGER_EMAIL },
      invitedDate = dateFormat.format(Date()),
      status = "PENDING"
    )
    _managerInvites.update { list ->
      listOf(invite) + list.filterNot { it.email.equals(cleanEmail, ignoreCase = true) }
    }

    if (db != null) {
      db.collection("manager_invites").document(cleanEmail)
        .set(mapManagerInviteToFirestore(invite, isCreate = true))
        .addOnSuccessListener {
          onResult?.invoke(Result.success(invite))
        }
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "manager_invites/$cleanEmail", firebaseAuth)
          onResult?.invoke(Result.failure(e))
        }
    } else {
      onResult?.invoke(Result.success(invite))
    }
    return invite
  }

  fun revokeManagerInvite(email: String) {
    val cleanEmail = email.trim().lowercase()
    _managerInvites.update { list -> list.filterNot { it.email.equals(cleanEmail, ignoreCase = true) } }
    db?.let { firestore ->
      firestore.collection("manager_invites").document(cleanEmail).delete()
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.DELETE, "manager_invites/$cleanEmail", firebaseAuth)
        }
    }
  }

  // --- Work Order / Case Operations ---
  fun reportCase(
    title: String,
    description: String,
    category: CaseCategory,
    priority: CasePriority,
    location: String,
    reportedBy: String,
    tenantEmail: String? = null,
    preferredDate: String? = null
  ): CaseItem {
    val cleanLocation = location.trim()
    val cleanPreferred = preferredDate?.trim()?.takeIf { it.isNotBlank() }
    val resolvedTenantEmail = tenantEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
      ?: _properties.value.find { it.unitNumber.equals(cleanLocation, ignoreCase = true) }?.assignedTenant?.email?.lowercase()
      ?: _tenants.value.find {
        it.assignedPropertyUnit?.equals(cleanLocation, ignoreCase = true) == true ||
          it.fullName.equals(reportedBy.trim(), ignoreCase = true)
      }?.email?.lowercase()
      ?: getCurrentUserEmail()

    val uid = getCurrentUserId()
    val nowMs = System.currentTimeMillis()
    val stamp = dateFormat.format(Date(nowMs)) + " • " + timeFormat.format(Date(nowMs))
    val initialMovement = CaseMovementEntry(
      status = CaseStatus.OPEN.name,
      note = "Case reported",
      updatedBy = reportedBy.trim().ifBlank { "Resident" },
      timestamp = stamp,
      epochMs = nowMs
    )

    val newCase = CaseItem(
      id = "CASE-${nowMs}-${(100..999).random()}",
      userId = uid,
      tenantEmail = resolvedTenantEmail,
      title = title.trim(),
      description = description.trim(),
      category = category,
      priority = priority,
      location = cleanLocation,
      reportedBy = reportedBy.trim(),
      reportedDate = dateFormat.format(Date(nowMs)),
      preferredDate = cleanPreferred,
      status = CaseStatus.OPEN,
      notes = emptyList(),
      statusHistory = listOf(initialMovement),
      createdAtEpochMs = nowMs
    )
    _cases.update { listOf(newCase) + it }

    db?.let { firestore ->
      val payload = mapCaseToFirestore(newCase, uid, isCreate = true)
      firestore.collection("cases").document(newCase.id).set(payload)
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "cases/${newCase.id}", firebaseAuth)
        }
    }

    emitActivityNotification(
      title = "New Case: ${newCase.title}",
      message = "${newCase.priority.label} ${newCase.category.label} case reported for ${newCase.location}.",
      targetAudience = "Estate Management",
      urgency = if (newCase.priority == CasePriority.EMERGENCY || newCase.priority == CasePriority.HIGH) "High Priority" else "Notice",
      triggerEvent = NoticeTriggerEvent.CASE_CREATED,
      targetEmail = resolvedTenantEmail.takeIf { it.isNotBlank() },
      targetUnit = cleanLocation,
      relatedEntityType = NoticeRelatedEntityType.CASE,
      relatedEntityId = newCase.id
    )

    return newCase
  }

  fun updateCase(
    caseId: String,
    title: String,
    description: String,
    location: String,
    category: CaseCategory,
    priority: CasePriority,
    preferredDate: String? = null
  ) {
    val existingCase = _cases.value.find { it.id == caseId }
    val cleanTitle = title.trim()
    val cleanDesc = description.trim()
    val cleanLoc = location.trim()
    val cleanPref = preferredDate?.trim()?.takeIf { it.isNotBlank() }

    _cases.update { list ->
      list.map { c ->
        if (c.id == caseId) {
          c.copy(
            title = cleanTitle,
            description = cleanDesc,
            location = cleanLoc,
            unitNumber = cleanLoc,
            category = category,
            priority = priority,
            preferredDate = cleanPref
          )
        } else c
      }
    }

    db?.let { firestore ->
      val updates = mutableMapOf<String, Any?>(
        "title" to cleanTitle,
        "description" to cleanDesc,
        "location" to cleanLoc,
        "category" to category.name,
        "priority" to priority.name,
        "preferredDate" to cleanPref,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      firestore.collection("cases").document(caseId).update(updates)
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.UPDATE, "cases/$caseId", firebaseAuth)
        }
    }

    if (existingCase != null) {
      val cleanTenantEmail = existingCase.tenantEmail.trim().lowercase().takeIf { it.isNotBlank() }
        ?: getCurrentUserEmail().takeIf { it.isNotBlank() }
      emitActivityNotification(
        title = "Case Updated: $cleanTitle",
        message = "Case for $cleanLoc updated (${priority.label} • ${category.label}).",
        targetAudience = "Estate Management",
        urgency = if (priority == CasePriority.EMERGENCY || priority == CasePriority.HIGH) "High Priority" else "Notice",
        triggerEvent = NoticeTriggerEvent.CASE_UPDATED,
        targetEmail = cleanTenantEmail,
        targetUnit = cleanLoc,
        relatedEntityType = NoticeRelatedEntityType.CASE,
        relatedEntityId = caseId
      )
    }
  }

  fun appendCaseNote(caseId: String, noteText: String) {
    val cleanText = noteText.trim()
    if (cleanText.isBlank()) return
    val existingCase = _cases.value.find { it.id == caseId } ?: return
    val actor = resolveActorIdentity()
    val nowMs = System.currentTimeMillis()
    val stamp = dateFormat.format(Date(nowMs)) + " • " + timeFormat.format(Date(nowMs))

    val noteEntry = CaseNote(
      id = "NOTE-${nowMs}-${(100..999).random()}",
      authorName = actor.name,
      authorEmail = actor.email,
      authorRole = if (actor.role == UserRole.MANAGER) "MANAGER" else "TENANT",
      text = cleanText,
      createdAt = stamp,
      createdAtEpochMs = nowMs
    )
    val movementEntry = CaseMovementEntry(
      status = existingCase.status.name,
      note = "Note added: $cleanText",
      updatedBy = actor.name,
      timestamp = stamp,
      epochMs = nowMs
    )

    val updatedNotes = existingCase.notes + noteEntry
    val updatedHistory = existingCase.statusHistory + movementEntry

    _cases.update { list ->
      list.map { c ->
        if (c.id == caseId) c.copy(notes = updatedNotes, statusHistory = updatedHistory) else c
      }
    }

    db?.let { firestore ->
      firestore.collection("cases").document(caseId).update(
        mapOf(
          "notes" to updatedNotes.map { mapCaseNote(it) },
          "statusHistory" to updatedHistory.map { mapCaseMovement(it) },
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "cases/$caseId", firebaseAuth)
      }
    }

    val cleanTenantEmail = existingCase.tenantEmail.trim().lowercase().takeIf { it.isNotBlank() }
    emitActivityNotification(
      title = "Case Note Added: ${existingCase.title}",
      message = "${actor.name}: $cleanText",
      targetAudience = if (cleanTenantEmail != null) "Targeted Resident" else "Estate Management",
      urgency = "Notice",
      triggerEvent = NoticeTriggerEvent.CASE_NOTE_ADDED,
      targetEmail = cleanTenantEmail,
      targetUnit = existingCase.location,
      relatedEntityType = NoticeRelatedEntityType.CASE,
      relatedEntityId = caseId
    )
  }

  fun assignVendorToCase(caseId: String, vendorId: String) {
    val vendor = _vendors.value.find { it.id == vendorId } ?: return
    val existingCase = _cases.value.find { it.id == caseId }
    val actor = resolveActorIdentity()
    val nowMs = System.currentTimeMillis()
    val stamp = dateFormat.format(Date(nowMs)) + " • " + timeFormat.format(Date(nowMs))
    val movementEntry = CaseMovementEntry(
      status = CaseStatus.ASSIGNED.name,
      note = "Contractor assigned: ${vendor.name}",
      updatedBy = actor.name,
      timestamp = stamp,
      epochMs = nowMs
    )
    val updatedHistory = (existingCase?.statusHistory ?: emptyList()) + movementEntry

    _cases.update { current ->
      current.map { c ->
        if (c.id == caseId) {
          c.copy(
            assignedVendorId = vendor.id,
            assignedVendorName = vendor.name,
            status = CaseStatus.ASSIGNED,
            statusHistory = updatedHistory
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
          "statusHistory" to updatedHistory.map { mapCaseMovement(it) },
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

    if (existingCase != null) {
      val cleanTenantEmail = existingCase.tenantEmail.trim().lowercase().takeIf { it.isNotBlank() }
      emitActivityNotification(
        title = "Contractor Dispatched: ${existingCase.title}",
        message = "${vendor.name} has been assigned to ${existingCase.location}.",
        targetAudience = if (cleanTenantEmail != null) "Targeted Resident" else "Estate Management",
        urgency = "Advisory",
        triggerEvent = NoticeTriggerEvent.CASE_VENDOR_ASSIGNED,
        targetEmail = cleanTenantEmail,
        targetUnit = existingCase.location,
        relatedEntityType = NoticeRelatedEntityType.CASE,
        relatedEntityId = caseId
      )
    }
  }

  fun updateCaseStatus(caseId: String, newStatus: CaseStatus) {
    val existingCase = _cases.value.find { it.id == caseId }
    val actor = resolveActorIdentity()
    val nowMs = System.currentTimeMillis()
    val stamp = dateFormat.format(Date(nowMs)) + " • " + timeFormat.format(Date(nowMs))
    val movementEntry = CaseMovementEntry(
      status = newStatus.name,
      note = "Status changed to ${newStatus.label}",
      updatedBy = actor.name,
      timestamp = stamp,
      epochMs = nowMs
    )
    val updatedHistory = (existingCase?.statusHistory ?: emptyList()) + movementEntry

    _cases.update { current ->
      current.map { c ->
        if (c.id == caseId) c.copy(status = newStatus, statusHistory = updatedHistory) else c
      }
    }

    db?.let { firestore ->
      firestore.collection("cases").document(caseId).update(
        mapOf(
          "status" to newStatus.name,
          "statusHistory" to updatedHistory.map { mapCaseMovement(it) },
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "cases/$caseId", firebaseAuth)
      }
    }

    if (existingCase != null) {
      val cleanTenantEmail = existingCase.tenantEmail.trim().lowercase().takeIf { it.isNotBlank() }
      emitActivityNotification(
        title = "Case ${newStatus.label}: ${existingCase.title}",
        message = "Case at ${existingCase.location} moved to ${newStatus.label}.",
        targetAudience = if (cleanTenantEmail != null) "Targeted Resident" else "Estate Management",
        urgency = if (newStatus == CaseStatus.RESOLVED) "Notice" else "Advisory",
        triggerEvent = NoticeTriggerEvent.CASE_STATUS_UPDATED,
        targetEmail = cleanTenantEmail,
        targetUnit = existingCase.location,
        relatedEntityType = NoticeRelatedEntityType.CASE,
        relatedEntityId = caseId
      )
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

  // --- Facility & Approval-Gated Hourly Booking Operations ---
  fun isHourlyWindowOccupied(
    facilityId: String,
    date: String,
    startHour: Int,
    durationHours: Int,
    excludeBookingId: String? = null
  ): Boolean {
    val targetDateKey = EstateDateUtils.toDateKey(date) ?: date.trim()
    val requestedHours = (startHour until (startHour + durationHours.coerceAtLeast(1))).toSet()

    val lockedInLocks = _slotLocks.value.any { lock ->
      if (lock.facilityId != facilityId || lock.dateKey != targetDateKey) return@any false
      if (excludeBookingId != null && lock.bookingId == excludeBookingId) return@any false
      val lockHourlyMatch = requestedHours.any { h ->
        lock.id == buildHourlySlotLockId(facilityId, targetDateKey, h)
      }
      if (lockHourlyMatch) return@any true
      val range = EstateDateUtils.parseTimeRangeHours(lock.timeSlot)
      if (range != null) {
        requestedHours.any { h -> h in range }
      } else false
    }
    if (lockedInLocks) return true

    return _bookings.value.any { b ->
      if (excludeBookingId != null && b.id == excludeBookingId) return@any false
      if (b.facilityId != facilityId) return@any false
      if (b.approvalStatus != BookingApprovalStatus.APPROVED) return@any false
      val bKey = EstateDateUtils.toDateKey(b.date) ?: b.date.trim()
      if (bKey != targetDateKey) return@any false
      val bRange = EstateDateUtils.parseTimeRangeHours(b.timeSlot)
        ?: (b.startHour until (b.startHour + b.durationHours.coerceAtLeast(1)))
      requestedHours.any { h -> h in bRange }
    }
  }

  fun isSlotBooked(facilityId: String, date: String, timeSlot: String): Boolean {
    val targetDateKey = EstateDateUtils.toDateKey(date) ?: date.trim()
    val cleanSlot = timeSlot.trim()
    if (cleanSlot.isEmpty()) return false
    val parsed = EstateDateUtils.parseTimeRangeHours(cleanSlot)
    if (parsed != null && isHourlyWindowOccupied(facilityId, date, parsed.first, (parsed.last - parsed.first + 1).coerceAtLeast(1))) {
      return true
    }
    val expectedLockId = buildSlotLockId(facilityId, targetDateKey, cleanSlot)
    val lockedInLocks = _slotLocks.value.any { lock ->
      lock.id == expectedLockId ||
        (lock.facilityId == facilityId &&
          lock.dateKey == targetDateKey &&
          lock.timeSlot.equals(cleanSlot, ignoreCase = true))
    }
    if (lockedInLocks) return true
    return _bookings.value.any { b ->
      val bookingKey = EstateDateUtils.toDateKey(b.date) ?: b.date.trim()
      b.approvalStatus == BookingApprovalStatus.APPROVED &&
        b.facilityId == facilityId &&
        bookingKey == targetDateKey &&
        b.timeSlot.equals(cleanSlot, ignoreCase = true)
    }
  }

  fun bookFacility(
    facilityId: String,
    date: String,
    timeSlot: String = "",
    residentName: String,
    unit: String,
    guestCount: Int,
    notes: String,
    startHour: Int? = null,
    durationHours: Int? = null,
    onResult: ((Result<FacilityBooking>) -> Unit)? = null
  ): FacilityBooking {
    val facility = _facilities.value.find { it.id == facilityId }
      ?: throw IllegalStateException("This amenity is no longer available.")
    val configuredSlots = facility.allowedSlots.map { it.trim() }.filter { it.isNotEmpty() }
    if (configuredSlots.isEmpty()) {
      throw IllegalStateException("Bookable hours have not been configured for ${facility.name} yet.")
    }

    val parsedFromSlot = if (timeSlot.isNotBlank()) EstateDateUtils.parseTimeRangeHours(timeSlot) else null
    val resolvedStartHour = (startHour ?: parsedFromSlot?.first ?: 8).coerceIn(6, 22)
    val resolvedDuration = (durationHours ?: parsedFromSlot?.let { (it.last - it.first + 1).coerceIn(1, 3) } ?: 1).coerceIn(1, 3)
    val formattedSlot = timeSlot.trim().takeIf { it.isNotBlank() }
      ?: EstateDateUtils.formatHourWindow(resolvedStartHour, resolvedDuration)
    val startLabel = EstateDateUtils.formatHourLabel(resolvedStartHour)
    val endLabel = EstateDateUtils.formatHourLabel(resolvedStartHour + resolvedDuration)

    val cleanDate = EstateDateUtils.toDateKey(date)?.let { EstateDateUtils.displayFromKey(it) } ?: date.trim()

    if (isHourlyWindowOccupied(facilityId, cleanDate, resolvedStartHour, resolvedDuration)) {
      throw IllegalStateException("Those hours ($formattedSlot on $cleanDate) are already reserved by an approved booking.")
    }

    val actor = resolveActorIdentity()
    val uid = getCurrentUserId()
    val safeGuestCount = guestCount.coerceIn(1, facility.capacity.coerceAtLeast(1))
    val newBooking = FacilityBooking(
      id = "BK-${System.currentTimeMillis()}-${(100..999).random()}",
      userId = uid,
      facilityId = facilityId,
      facilityName = facility.name,
      residentName = residentName.trim().ifBlank { actor.name },
      unit = unit.trim(),
      date = cleanDate,
      timeSlot = formattedSlot,
      startTime = startLabel,
      endTime = endLabel,
      startHour = resolvedStartHour,
      durationHours = resolvedDuration,
      guestCount = safeGuestCount,
      notes = notes.trim(),
      confirmedTimestamp = dateFormat.format(Date()) + " " + timeFormat.format(Date()),
      approvalStatus = BookingApprovalStatus.PENDING
    )

    _bookings.update { listOf(newBooking) + it }

    if (db != null) {
      db.collection("bookings").document(newBooking.id)
        .set(mapBookingToFirestore(newBooking, uid))
        .addOnSuccessListener {
          emitActivityNotification(
            title = "Amenity Request: ${newBooking.facilityName}",
            message = "${newBooking.residentName} requested ${newBooking.facilityName} on ${newBooking.date} (${newBooking.timeSlot}) for ${newBooking.guestCount} guests.",
            targetAudience = "Estate Management",
            urgency = "Notice",
            triggerEvent = NoticeTriggerEvent.AMENITY_BOOKED,
            targetEmail = getCurrentUserEmail().takeIf { it.isNotBlank() },
            targetUnit = newBooking.unit.takeIf { it.isNotBlank() },
            relatedEntityType = NoticeRelatedEntityType.BOOKING,
            relatedEntityId = newBooking.id
          )
          onResult?.invoke(Result.success(newBooking))
        }
        .addOnFailureListener { e ->
          _bookings.update { list -> list.filterNot { it.id == newBooking.id } }
          val msg = "Unable to submit booking request right now."
          if (onResult == null) _lastError.value = msg
          handleFirestoreError(e, OperationType.CREATE, "bookings/${newBooking.id}", firebaseAuth)
          onResult?.invoke(Result.failure(IllegalStateException(msg, e)))
        }
    } else {
      emitActivityNotification(
        title = "Amenity Request: ${newBooking.facilityName}",
        message = "${newBooking.residentName} requested ${newBooking.facilityName} on ${newBooking.date} (${newBooking.timeSlot}).",
        targetAudience = "Estate Management",
        urgency = "Notice",
        triggerEvent = NoticeTriggerEvent.AMENITY_BOOKED,
        targetEmail = actorEmail?.takeIf { it.isNotBlank() },
        targetUnit = newBooking.unit.takeIf { it.isNotBlank() },
        relatedEntityType = NoticeRelatedEntityType.BOOKING,
        relatedEntityId = newBooking.id
      )
      onResult?.invoke(Result.success(newBooking))
    }

    return newBooking
  }

  fun approveBooking(
    bookingId: String,
    onResult: ((Result<FacilityBooking>) -> Unit)? = null
  ) {
    val existing = _bookings.value.find { it.id == bookingId } ?: return
    val dateKey = EstateDateUtils.toDateKey(existing.date) ?: existing.date.trim()
    val startH = existing.startHour
    val duration = existing.durationHours.coerceAtLeast(1)

    if (isHourlyWindowOccupied(existing.facilityId, existing.date, startH, duration, excludeBookingId = bookingId)) {
      val msg = "Cannot approve: one or more requested hours on ${existing.date} are already locked."
      _lastError.value = msg
      onResult?.invoke(Result.failure(IllegalStateException(msg)))
      return
    }

    val locks = (startH until (startH + duration)).map { h ->
      BookingSlotLock(
        id = buildHourlySlotLockId(existing.facilityId, dateKey, h),
        facilityId = existing.facilityId,
        dateKey = dateKey,
        timeSlot = EstateDateUtils.formatHourWindow(h, 1),
        bookingId = existing.id,
        bookedByUid = existing.userId.ifBlank { getCurrentUserId() }
      )
    }
    val approved = existing.copy(approvalStatus = BookingApprovalStatus.APPROVED)

    _bookings.update { list -> list.map { if (it.id == bookingId) approved else it } }
    _slotLocks.update { current ->
      val lockIds = locks.map { it.id }.toSet()
      locks + current.filterNot { it.id in lockIds }
    }

    val residentEmail = resolveBookingResidentEmail(existing)

    if (db != null) {
      val batch = db.batch()
      batch.update(
        db.collection("bookings").document(bookingId),
        mapOf("approvalStatus" to BookingApprovalStatus.APPROVED.name)
      )
      locks.forEach { lock ->
        batch.set(db.collection("slot_locks").document(lock.id), mapSlotLockToFirestore(lock))
      }
      batch.commit()
        .addOnSuccessListener {
          emitActivityNotification(
            title = "Booking Approved: ${existing.facilityName}",
            message = "Your ${existing.facilityName} reservation for ${existing.date} (${existing.timeSlot}) is approved.",
            targetAudience = if (residentEmail != null) "Targeted Resident" else "All Residents",
            urgency = "Notice",
            triggerEvent = NoticeTriggerEvent.AMENITY_APPROVED,
            targetEmail = residentEmail,
            targetUnit = existing.unit.takeIf { it.isNotBlank() },
            relatedEntityType = NoticeRelatedEntityType.BOOKING,
            relatedEntityId = bookingId
          )
          onResult?.invoke(Result.success(approved))
        }
        .addOnFailureListener { e ->
          _bookings.update { list -> list.map { if (it.id == bookingId) existing else it } }
          val lockIds = locks.map { it.id }.toSet()
          _slotLocks.update { current -> current.filterNot { it.id in lockIds } }
          handleFirestoreError(e, OperationType.UPDATE, "bookings/$bookingId", firebaseAuth)
          onResult?.invoke(Result.failure(e))
        }
    } else {
      emitActivityNotification(
        title = "Booking Approved: ${existing.facilityName}",
        message = "Your ${existing.facilityName} reservation for ${existing.date} (${existing.timeSlot}) is approved.",
        targetAudience = if (residentEmail != null) "Targeted Resident" else "All Residents",
        urgency = "Notice",
        triggerEvent = NoticeTriggerEvent.AMENITY_APPROVED,
        targetEmail = residentEmail,
        targetUnit = existing.unit.takeIf { it.isNotBlank() },
        relatedEntityType = NoticeRelatedEntityType.BOOKING,
        relatedEntityId = bookingId
      )
      onResult?.invoke(Result.success(approved))
    }
  }

  fun declineBooking(bookingId: String, reason: String = "") {
    val existing = _bookings.value.find { it.id == bookingId } ?: return
    val declinedNote = reason.trim().takeIf { it.isNotBlank() }?.let {
      if (existing.notes.isBlank()) "Manager Note: $it" else "${existing.notes} • Manager Note: $it"
    } ?: existing.notes

    _bookings.update { list ->
      list.map {
        if (it.id == bookingId) it.copy(approvalStatus = BookingApprovalStatus.DECLINED, notes = declinedNote) else it
      }
    }
    val locksToRemove = _slotLocks.value.filter { it.bookingId == bookingId }
    if (locksToRemove.isNotEmpty()) {
      _slotLocks.update { current -> current.filterNot { it.bookingId == bookingId } }
    }

    db?.let { firestore ->
      val batch = firestore.batch()
      batch.update(
        firestore.collection("bookings").document(bookingId),
        mapOf(
          "approvalStatus" to BookingApprovalStatus.DECLINED.name,
          "notes" to declinedNote
        )
      )
      locksToRemove.forEach { l ->
        batch.delete(firestore.collection("slot_locks").document(l.id))
      }
      batch.commit().addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "bookings/$bookingId", firebaseAuth)
      }
    }

    val residentEmail = resolveBookingResidentEmail(existing)
    emitActivityNotification(
      title = "Booking Declined: ${existing.facilityName}",
      message = reason.trim().ifBlank { "Your ${existing.facilityName} request for ${existing.date} (${existing.timeSlot}) was declined." },
      targetAudience = if (residentEmail != null) "Targeted Resident" else "All Residents",
      urgency = "High Priority",
      triggerEvent = NoticeTriggerEvent.AMENITY_DECLINED,
      targetEmail = residentEmail,
      targetUnit = existing.unit.takeIf { it.isNotBlank() },
      relatedEntityType = NoticeRelatedEntityType.BOOKING,
      relatedEntityId = bookingId
    )
  }

  private fun resolveBookingResidentEmail(booking: FacilityBooking): String? {
    return _tenants.value.find { t ->
      (booking.residentName.isNotBlank() && t.fullName.equals(booking.residentName, ignoreCase = true)) ||
        (booking.unit.isNotBlank() && t.assignedPropertyUnit.equals(booking.unit, ignoreCase = true))
    }?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
      ?: _properties.value.find { p ->
        booking.unit.isNotBlank() && p.unitNumber.equals(booking.unit, ignoreCase = true)
      }?.assignedTenant?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
  }

  fun cancelBooking(bookingId: String) {
    val existing = _bookings.value.find { it.id == bookingId }
    val matchingLockIds = mutableSetOf<String>()
    _slotLocks.value.filter { it.bookingId == bookingId }.forEach { matchingLockIds.add(it.id) }
    existing?.let { b ->
      val dateKey = EstateDateUtils.toDateKey(b.date) ?: b.date.trim()
      matchingLockIds.add(buildSlotLockId(b.facilityId, dateKey, b.timeSlot))
      for (h in b.startHour until (b.startHour + b.durationHours.coerceAtLeast(1))) {
        matchingLockIds.add(buildHourlySlotLockId(b.facilityId, dateKey, h))
      }
    }

    _bookings.update { it.filterNot { b -> b.id == bookingId } }
    _slotLocks.update { it.filterNot { l -> l.id in matchingLockIds || l.bookingId == bookingId } }

    db?.let { firestore ->
      val batch = firestore.batch()
      batch.delete(firestore.collection("bookings").document(bookingId))
      matchingLockIds.forEach { lockId ->
        batch.delete(firestore.collection("slot_locks").document(lockId))
      }
      batch.commit().addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.DELETE, "bookings/$bookingId", firebaseAuth)
      }
    }

    if (existing != null) {
      val actor = resolveActorIdentity()
      val residentEmail = resolveBookingResidentEmail(existing)
        ?: getCurrentUserEmail().takeIf { it.isNotBlank() }
        ?: actorEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
      val cancelledByManager = actor.role == UserRole.MANAGER && residentEmail != null
      emitActivityNotification(
        title = "Booking Cancelled: ${existing.facilityName}",
        message = "${existing.facilityName} reservation on ${existing.date} (${existing.timeSlot}) was cancelled.",
        targetAudience = if (cancelledByManager) "Targeted Resident" else "Estate Management",
        urgency = "Notice",
        triggerEvent = NoticeTriggerEvent.AMENITY_CANCELLED,
        targetEmail = residentEmail,
        targetUnit = existing.unit.takeIf { it.isNotBlank() },
        relatedEntityType = NoticeRelatedEntityType.BOOKING,
        relatedEntityId = bookingId
      )
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
    val cleanSlots = allowedSlots.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)
    val cleanAmenities = amenities.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)
    val newFacility = Facility(
      id = "FAC-${System.currentTimeMillis()}-${(100..999).random()}",
      name = name.trim(),
      tag = tag.trim(),
      capacity = capacity.coerceIn(1, 5000),
      location = location.trim(),
      description = description.trim(),
      amenities = cleanAmenities,
      allowedSlots = cleanSlots,
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
    val cleanSlots = allowedSlots.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)
    val cleanAmenities = amenities.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)
    var updated: Facility? = null
    _facilities.update { list ->
      list.map { f ->
        if (f.id == facilityId) {
          f.copy(
            name = name.trim(),
            tag = tag.trim(),
            capacity = capacity.coerceIn(1, 5000),
            location = location.trim(),
            description = description.trim(),
            amenities = cleanAmenities,
            allowedSlots = cleanSlots,
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

  // --- Maintenance & Estate Calendar Event Operations ---
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
    targetUnits: List<String> = emptyList()
  ): MaintenanceEvent {
    val normalizedKey = EstateDateUtils.toDateKey(date)
    val cleanDate = if (normalizedKey != null) EstateDateUtils.displayFromKey(normalizedKey) else date.trim()
    val uid = getCurrentUserId()
    val cleanUnits = targetUnits.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val resolvedArea = if (targetScope == EventTargetScope.ALL_RESIDENTS) {
      "All Residents"
    } else {
      cleanUnits.joinToString(", ").ifBlank { targetArea.trim() }
    }
    val resolvedContractor = if (eventCategory == CalendarEventCategory.MAINTENANCE) contractor.trim() else ""

    val event = MaintenanceEvent(
      id = "MAINT-${System.currentTimeMillis()}-${(100..999).random()}",
      title = title.trim(),
      discipline = discipline,
      eventCategory = eventCategory,
      scheduledDate = cleanDate,
      scheduledTime = time.trim(),
      contractorName = resolvedContractor,
      targetArea = resolvedArea,
      targetScope = targetScope,
      targetUnits = cleanUnits,
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

    // Resolve targeted resident emails when scoped to specific units
    val targetedEmails = if (targetScope == EventTargetScope.SPECIFIC_UNITS && cleanUnits.isNotEmpty()) {
      val unitSet = cleanUnits.map { it.lowercase() }.toSet()
      val propEmails = _properties.value
        .filter { it.unitNumber.lowercase() in unitSet }
        .mapNotNull { it.assignedTenant?.email?.trim()?.lowercase()?.takeIf { e -> e.isNotBlank() } }
      val tenantEmails = _tenants.value
        .filter { (it.assignedPropertyUnit ?: "").lowercase() in unitSet }
        .mapNotNull { it.email.trim().lowercase().takeIf { e -> e.isNotBlank() } }
      (propEmails + tenantEmails).distinct()
    } else emptyList()

    emitActivityNotification(
      title = "${eventCategory.label}: ${event.title}",
      message = "Scheduled for ${event.scheduledDate} (${event.scheduledTime.ifBlank { "All Day" }}) • Scope: ${resolvedArea}",
      targetAudience = if (targetScope == EventTargetScope.ALL_RESIDENTS) "All Residents & Tenants" else resolvedArea,
      urgency = "Advisory",
      triggerEvent = NoticeTriggerEvent.EVENT_SCHEDULED,
      isBroadcast = targetScope == EventTargetScope.ALL_RESIDENTS,
      targetEmail = if (targetedEmails.size == 1) targetedEmails.first() else null,
      targetEmails = targetedEmails,
      targetUnit = cleanUnits.firstOrNull(),
      targetUnits = cleanUnits,
      relatedEntityType = NoticeRelatedEntityType.EVENT,
      relatedEntityId = event.id
    )

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

  // --- Attributed Reminders, Advisories & Activity Notifications ---
  private fun emitActivityNotification(
    title: String,
    message: String,
    targetAudience: String,
    urgency: String,
    triggerEvent: NoticeTriggerEvent,
    isBroadcast: Boolean = false,
    targetEmail: String? = null,
    targetEmails: List<String> = emptyList(),
    targetUnit: String? = null,
    targetUnits: List<String> = emptyList(),
    relatedEntityType: NoticeRelatedEntityType = NoticeRelatedEntityType.ADVISORY,
    relatedEntityId: String? = null
  ): ReminderItem {
    return sendReminder(
      title = title,
      message = message,
      targetAudience = targetAudience,
      urgency = urgency,
      isBroadcast = isBroadcast,
      recordType = NoticeRecordType.ACTIVITY,
      triggerEvent = triggerEvent,
      targetEmail = targetEmail,
      targetEmails = targetEmails,
      targetUnit = targetUnit,
      targetUnits = targetUnits,
      relatedEntityType = relatedEntityType,
      relatedEntityId = relatedEntityId
    )
  }

  fun sendReminder(
    title: String,
    message: String,
    targetAudience: String,
    urgency: String,
    isBroadcast: Boolean = false,
    recordType: NoticeRecordType = NoticeRecordType.ANNOUNCEMENT,
    triggerEvent: NoticeTriggerEvent = NoticeTriggerEvent.MANUAL_ADVISORY,
    targetEmail: String? = null,
    targetEmails: List<String> = emptyList(),
    targetUnit: String? = null,
    targetUnits: List<String> = emptyList(),
    relatedEntityType: NoticeRelatedEntityType = NoticeRelatedEntityType.ADVISORY,
    relatedEntityId: String? = null
  ): ReminderItem {
    val actor = resolveActorIdentity()
    val nowMs = System.currentTimeMillis()
    val cleanTargetEmail = targetEmail?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val cleanTargetEmails = targetEmails.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
    val cleanTargetUnit = targetUnit?.trim()?.takeIf { it.isNotBlank() }
    val cleanTargetUnits = targetUnits.map { it.trim() }.filter { it.isNotBlank() }.distinct()

    val newReminder = ReminderItem(
      id = "REM-${nowMs}-${(100..999).random()}",
      title = title.trim(),
      message = message.trim(),
      targetAudience = targetAudience.trim(),
      urgency = urgency.trim(),
      timestamp = dateFormat.format(Date(nowMs)) + " • " + timeFormat.format(Date(nowMs)),
      isBroadcast = isBroadcast,
      recordType = recordType,
      triggerEvent = triggerEvent,
      createdByUid = actor.uid,
      createdByName = actor.name,
      createdByEmail = actor.email,
      createdByRole = actor.role,
      targetEmail = cleanTargetEmail,
      targetEmails = cleanTargetEmails,
      targetUnit = cleanTargetUnit,
      targetUnits = cleanTargetUnits,
      relatedEntityType = relatedEntityType,
      relatedEntityId = relatedEntityId?.takeIf { it.isNotBlank() },
      createdAtEpochMs = nowMs
    )
    _reminders.update { list -> (listOf(newReminder) + list).sortedByDescending { it.createdAtEpochMs } }

    db?.let { firestore ->
      val payload = mapReminderToFirestore(newReminder, actor.uid)
      firestore.collection("reminders").document(newReminder.id).set(payload)
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.CREATE, "reminders/${newReminder.id}", firebaseAuth)
        }
    }

    return newReminder
  }

  fun deleteReminder(reminderId: String) {
    _reminders.update { it.filterNot { r -> r.id == reminderId } }
    db?.let { firestore ->
      firestore.collection("reminders").document(reminderId).delete()
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.DELETE, "reminders/$reminderId", firebaseAuth)
        }
    }
  }

  fun clearAllReminders() {
    val cachedIds = _reminders.value.map { it.id }
    _reminders.value = emptyList()
    db?.let { firestore ->
      firestore.collection("reminders").get()
        .addOnSuccessListener { snapshot ->
          val ids = (snapshot.documents.map { it.id } + cachedIds).distinct()
          if (ids.isNotEmpty()) {
            ids.chunked(400).forEach { chunk ->
              val batch = firestore.batch()
              chunk.forEach { id -> batch.delete(firestore.collection("reminders").document(id)) }
              batch.commit().addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.DELETE, "reminders", firebaseAuth)
              }
            }
          }
        }
        .addOnFailureListener { e ->
          handleFirestoreError(e, OperationType.LIST, "reminders", firebaseAuth)
        }
    }
  }

  fun markNoticeAsRead(noticeId: String) {
    val updated = (_readNoticeIds.value + noticeId).toList().takeLast(500).toSet()
    _readNoticeIds.value = updated
    persistReadNoticeIds(updated)
  }

  fun markAllNoticesAsRead(noticeIds: Collection<String>) {
    if (noticeIds.isEmpty()) return
    val updated = (_readNoticeIds.value + noticeIds).toList().takeLast(500).toSet()
    _readNoticeIds.value = updated
    persistReadNoticeIds(updated)
  }

  private fun persistReadNoticeIds(ids: Set<String>) {
    val firestore = db ?: return
    val uid = firebaseAuth?.currentUser?.uid ?: return
    val payload = mapOf(
      "uid" to uid,
      "readNoticeIds" to ids.toList().takeLast(500),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    firestore.collection("user_notice_states").document(uid).set(payload)
      .addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.WRITE, "user_notice_states/$uid", firebaseAuth)
      }
  }

  // --- Contract & Lease Operations ---
  fun sendContractRundownNotification(contractId: String) {
    _contracts.update { list ->
      list.map { c -> if (c.id == contractId) c.copy(isNotificationSent = true) else c }
    }

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
    leaseStatus: LeaseContractStatus? = null,
    emitNotification: Boolean = true,
    signedContractDoc: ContractDocumentMeta? = null,
    codeOfConductDoc: ContractDocumentMeta? = null
  ): ContractItem {
    val computedDays = EstateDateUtils.daysUntilDisplay(expiryDate) ?: daysRemaining
    val resolvedStatus = leaseStatus ?: LeaseContractStatus.ACTIVE
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
      isSignedByTenant = resolvedStatus == LeaseContractStatus.ACTIVE,
      isArchived = false,
      signedContractDoc = signedContractDoc,
      codeOfConductDoc = codeOfConductDoc
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
    unit: String? = null,
    signedContractDoc: ContractDocumentMeta? = null,
    codeOfConductDoc: ContractDocumentMeta? = null
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
            contactEmail = contactEmail.trim(),
            signedContractDoc = signedContractDoc ?: c.signedContractDoc,
            codeOfConductDoc = codeOfConductDoc ?: c.codeOfConductDoc
          ).also { updated = it }
        } else c
      }
    }
    updated?.let { ctr ->
      db?.let { firestore ->
        val payload = mutableMapOf<String, Any?>(
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
          "signedContractDoc" to mapDocumentMeta(ctr.signedContractDoc),
          "codeOfConductDoc" to mapDocumentMeta(ctr.codeOfConductDoc),
          "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        firestore.collection("contracts").document(contractId)
          .update(payload)
          .addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.UPDATE, "contracts/$contractId", firebaseAuth)
          }
      }
    }
  }

  fun setContractArchived(contractId: String, archived: Boolean) {
    _contracts.update { list ->
      list.map { c -> if (c.id == contractId) c.copy(isArchived = archived) else c }
    }
    db?.let { firestore ->
      firestore.collection("contracts").document(contractId).update(
        mapOf(
          "isArchived" to archived,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "contracts/$contractId", firebaseAuth)
      }
    }
  }

  fun attachContractDocument(
    contractId: String,
    isCodeOfConduct: Boolean,
    docMeta: ContractDocumentMeta
  ) {
    _contracts.update { list ->
      list.map { c ->
        if (c.id == contractId) {
          if (isCodeOfConduct) c.copy(codeOfConductDoc = docMeta) else c.copy(signedContractDoc = docMeta)
        } else c
      }
    }
    val fieldName = if (isCodeOfConduct) "codeOfConductDoc" else "signedContractDoc"
    val mapped = mapDocumentMeta(docMeta) ?: return
    db?.let { firestore ->
      firestore.collection("contracts").document(contractId).update(
        mapOf(
          fieldName to mapped,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).addOnFailureListener { e ->
        handleFirestoreError(e, OperationType.UPDATE, "contracts/$contractId", firebaseAuth)
      }
    }
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

  // --- Contractor Management Operations ---
  fun createVendor(
    name: String,
    specialty: CaseCategory,
    phone: String,
    email: String,
    leadTech: String,
    discipline: String = specialty.label
  ): VendorItem {
    val cleanDisc = discipline.trim().ifBlank { specialty.label }
    val newVendor = VendorItem(
      id = "VEND-${System.currentTimeMillis()}-${(100..999).random()}",
      name = name.trim(),
      specialty = specialty,
      discipline = cleanDisc,
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
    leadTech: String,
    discipline: String = specialty.label
  ) {
    val cleanDisc = discipline.trim().ifBlank { specialty.label }
    var updated: VendorItem? = null
    _vendors.update { list ->
      list.map { v ->
        if (v.id == vendorId) {
          v.copy(
            name = name.trim(),
            specialty = specialty,
            discipline = cleanDisc,
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
              "discipline" to vend.effectiveDiscipline,
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
    pavilionWing: String = "",
    architecture: PropertyArchitecture,
    parkingSpots: List<String> = emptyList(),
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
    pavilionWing: String = "",
    architecture: PropertyArchitecture,
    parkingSpots: List<String> = emptyList(),
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
      val sqm = prop.architecture.effectiveSqMeters
      db?.let { firestore ->
        firestore.collection("properties").document(propertyId)
          .update(
            mapOf(
              "unitNumber" to prop.unitNumber,
              "pavilionWing" to prop.pavilionWing,
              "bedrooms" to prop.architecture.bedrooms,
              "bathrooms" to prop.architecture.bathrooms.toDouble(),
              "sqFt" to sqm,
              "sqMeters" to sqm,
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
      renewalTerms = "Lease agreement between $effectiveManagerName and ${tenantName.trim()} for ${property.unitNumber}. Monthly valuation: $rentalVal. Notice required 60 days prior to expiry.",
      contactPerson = effectiveManagerName,
      contactEmail = effectiveManagerEmail,
      unit = property.unitNumber,
      tenantId = tenantId,
      tenantEmail = cleanEmail,
      propertyId = property.id,
      managerName = effectiveManagerName,
      managerEmail = effectiveManagerEmail,
      leaseStatus = LeaseContractStatus.ACTIVE,
      emitNotification = false
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

    _properties.update { list ->
      list.map { p ->
        if (p.id == propertyId) {
          p.copy(status = PropertyStatus.OCCUPIED, assignedTenant = tenantInfo)
        } else p
      }
    }

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
