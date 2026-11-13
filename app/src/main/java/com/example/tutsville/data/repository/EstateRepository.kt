package com.example.tutsville.data.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.tutsville.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EstateRepository(
  val databaseId: String? = null
) {

  companion object {
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
          val effectiveId = databaseId ?: "ai-studio-android-tutsvill-d32054fc-09ff-4509-8bdd-53cb7f4790e9"
          EstateRepository(effectiveId).also { instance = it }
        }
      }
    }
  }

  private val db: FirebaseFirestore? = try {
    if (!databaseId.isNullOrBlank()) {
      FirebaseFirestore.getInstance(databaseId)
    } else {
      null
    }
  } catch (e: Exception) {
    null
  }

  val isLoading = MutableStateFlow(db != null)
  val isLoadingFlow: StateFlow<Boolean> = isLoading.asStateFlow()

  private val _cases = MutableStateFlow<List<CaseItem>>(emptyList())
  val cases: StateFlow<List<CaseItem>> = _cases.asStateFlow()

  private val _maintenances = MutableStateFlow<List<MaintenanceEvent>>(emptyList())
  val maintenances: StateFlow<List<MaintenanceEvent>> = _maintenances.asStateFlow()

  private val _facilities = MutableStateFlow<List<Facility>>(emptyList())
  val facilities: StateFlow<List<Facility>> = _facilities.asStateFlow()

  private val _bookings = MutableStateFlow<List<FacilityBooking>>(emptyList())
  val bookings: StateFlow<List<FacilityBooking>> = _bookings.asStateFlow()

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

  private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
  private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

  private fun getCurrentUserId(): String {
    return try {
      Firebase.auth.currentUser?.uid ?: "user_default"
    } catch (e: Exception) {
      "user_default"
    }
  }

  init {
    try {
      Firebase.auth.addAuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
          android.util.Log.d("EstateRepository", "Firebase Auth user active (${user.uid}). Attaching live Firestore listeners.")
          db?.let { firestore ->
            attachSnapshotListeners(firestore)
          }
        }
      }
    } catch (e: Exception) {
      android.util.Log.e("EstateRepository", "Error registering AuthStateListener: ${e.message}")
    }
    setupFirestoreSync()
  }

  // --- Live Cloud Firestore Snapshot Sync ---
  private fun setupFirestoreSync() {
    val firestore = db ?: return
    attachSnapshotListeners(firestore)
  }

  private fun attachSnapshotListeners(firestore: FirebaseFirestore) {
    // 1. Cases
    firestore.collection("cases").addSnapshotListener { snapshot, error ->
      if (error != null || snapshot == null) return@addSnapshotListener
      _cases.value = snapshot.documents.mapNotNull { docToCase(it) }
      isLoading.value = false
    }

    // 2. Properties
    firestore.collection("properties").addSnapshotListener { snapshot, error ->
      if (error != null || snapshot == null) return@addSnapshotListener
      _properties.value = snapshot.documents.mapNotNull { docToProperty(it) }
      isLoading.value = false
    }

    // 3. Tenants
    firestore.collection("tenants").addSnapshotListener { snapshot, error ->
      if (error != null || snapshot == null) return@addSnapshotListener
      _tenants.value = snapshot.documents.mapNotNull { docToTenant(it) }
      isLoading.value = false
    }

    // 4. Facilities
    firestore.collection("facilities").addSnapshotListener { snapshot, error ->
      if (error != null || snapshot == null) return@addSnapshotListener
      _facilities.value = snapshot.documents.mapNotNull { docToFacility(it) }
      isLoading.value = false
    }

    // 5. Bookings
    firestore.collection("bookings").addSnapshotListener { snapshot, error ->
      if (error != null || snapshot == null) return@addSnapshotListener
      _bookings.value = snapshot.documents.mapNotNull { docToBooking(it) }
      isLoading.value = false
    }

    // 6. Maintenances
    firestore.collection("maintenances").addSnapshotListener { snapshot, error ->
      if (error != null || snapshot == null) return@addSnapshotListener
      _maintenances.value = snapshot.documents.mapNotNull { docToMaintenance(it) }
      isLoading.value = false
    }

    // 7. Contracts
    firestore.collection("contracts").addSnapshotListener { snapshot, error ->
      if (error != null || snapshot == null) return@addSnapshotListener
      _contracts.value = snapshot.documents.mapNotNull { docToContract(it) }
      isLoading.value = false
    }

    // 8. Reminders
    firestore.collection("reminders").addSnapshotListener { snapshot, error ->
      if (error != null || snapshot == null) return@addSnapshotListener
      _reminders.value = snapshot.documents.mapNotNull { docToReminder(it) }
      isLoading.value = false
    }

    // 9. Vendors
    firestore.collection("vendors").addSnapshotListener { snapshot, error ->
      if (error != null || snapshot == null) return@addSnapshotListener
      _vendors.value = snapshot.documents.mapNotNull { docToVendor(it) }
      isLoading.value = false
    }
  }

  // --- Document Mappers: Model -> Firestore Map ---
  private fun mapCaseToFirestore(c: CaseItem): Map<String, Any?> = mapOf(
    "userId" to getCurrentUserId(),
    "title" to c.title,
    "description" to c.description,
    "category" to c.category.name,
    "priority" to c.priority.name,
    "location" to c.location,
    "reportedBy" to c.reportedBy,
    "reportedDate" to c.reportedDate,
    "assignedVendorId" to c.assignedVendorId,
    "assignedVendorName" to c.assignedVendorName,
    "status" to c.status.name,
    "rating" to c.rating,
    "reviewComment" to c.reviewComment,
    "reviewTags" to c.reviewTags,
    "photoNote" to c.photoNote,
    "createdAt" to FieldValue.serverTimestamp()
  )

  private fun mapPropertyToFirestore(p: PropertyItem): Map<String, Any?> = mapOf(
    "unitNumber" to p.unitNumber,
    "pavilionWing" to p.pavilionWing,
    "bedrooms" to p.architecture.bedrooms,
    "bathrooms" to p.architecture.bathrooms.toDouble(),
    "sqFt" to p.architecture.sqFt,
    "floorLevel" to p.architecture.floorLevel,
    "viewType" to p.architecture.viewType,
    "parkingSpots" to p.parkingSpots,
    "amenities" to p.amenities,
    "monthlyRental" to p.monthlyRental,
    "status" to if (p.status == PropertyStatus.UNDER_MAINTENANCE) "MAINTENANCE" else p.status.name,
    "assignedTenant" to p.assignedTenant?.let { t ->
      mapOf(
        "name" to t.name,
        "email" to t.email,
        "leaseStartDate" to t.leaseStartDate,
        "leaseEndDate" to t.leaseEndDate,
        "inviteCode" to t.inviteCode,
        "inviteLink" to t.inviteLink,
        "isClaimed" to t.isClaimed,
        "assignedTimestamp" to t.assignedTimestamp
      )
    },
    "listedDate" to p.listedDate,
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun mapTenantToFirestore(t: TenantProfile): Map<String, Any?> = mapOf(
    "firstName" to t.firstName,
    "lastName" to t.lastName,
    "fullName" to t.fullName,
    "nationalId" to t.nationalId,
    "address" to t.address,
    "email" to t.email,
    "phone" to t.phone,
    "emergencyContactName" to t.emergencyContactName,
    "emergencyContactPhone" to t.emergencyContactPhone,
    "assignedPropertyId" to t.assignedPropertyId,
    "assignedPropertyUnit" to t.assignedPropertyUnit,
    "leaseStartDate" to t.leaseStartDate,
    "leaseEndDate" to t.leaseEndDate,
    "inviteCode" to t.inviteCode,
    "isAppRegistered" to t.isAppRegistered,
    "createdAt" to t.createdAt,
    "updatedAt" to FieldValue.serverTimestamp()
  )

  private fun mapFacilityToFirestore(f: Facility): Map<String, Any?> = mapOf(
    "name" to f.name,
    "tag" to f.tag,
    "capacity" to f.capacity,
    "location" to f.location,
    "description" to f.description,
    "amenities" to f.amenities,
    "allowedSlots" to f.allowedSlots,
    "rules" to f.rules
  )

  private fun mapBookingToFirestore(b: FacilityBooking): Map<String, Any?> = mapOf(
    "userId" to getCurrentUserId(),
    "facilityId" to b.facilityId,
    "facilityName" to b.facilityName,
    "residentName" to b.residentName,
    "unit" to b.unit,
    "date" to b.date,
    "timeSlot" to b.timeSlot,
    "guestCount" to b.guestCount,
    "notes" to b.notes,
    "confirmedTimestamp" to b.confirmedTimestamp,
    "createdAt" to FieldValue.serverTimestamp()
  )

  private fun mapMaintenanceToFirestore(m: MaintenanceEvent): Map<String, Any?> = mapOf(
    "userId" to getCurrentUserId(),
    "title" to m.title,
    "discipline" to m.discipline.name,
    "scheduledDate" to m.scheduledDate,
    "scheduledTime" to m.scheduledTime,
    "contractorName" to m.contractorName,
    "targetArea" to m.targetArea,
    "intervalMonths" to m.intervalMonths,
    "status" to m.status.name,
    "notes" to m.notes,
    "createdAt" to FieldValue.serverTimestamp()
  )

  private fun mapContractToFirestore(c: ContractItem): Map<String, Any?> = mapOf(
    "title" to c.title,
    "counterparty" to c.counterparty,
    "vendorName" to c.counterparty,
    "serviceCategory" to c.type.name,
    "type" to c.type.name,
    "annualValue" to c.annualValue,
    "startDate" to c.startDate,
    "expiryDate" to c.expiryDate,
    "renewalDate" to c.expiryDate,
    "daysRemaining" to c.daysRemaining,
    "renewalTerms" to c.renewalTerms,
    "contactPerson" to c.contactPerson,
    "contactEmail" to c.contactEmail,
    "isNotificationSent" to c.isNotificationSent
  )

  private fun mapReminderToFirestore(r: ReminderItem): Map<String, Any?> = mapOf(
    "userId" to getCurrentUserId(),
    "title" to r.title,
    "message" to r.message,
    "targetAudience" to r.targetAudience,
    "urgency" to r.urgency,
    "timestamp" to r.timestamp,
    "isBroadcast" to r.isBroadcast,
    "createdAt" to FieldValue.serverTimestamp()
  )

  private fun mapVendorToFirestore(v: VendorItem): Map<String, Any?> = mapOf(
    "name" to v.name,
    "category" to v.specialty.name,
    "specialty" to v.specialty.name,
    "phone" to v.phone,
    "rating" to v.rating.toDouble(),
    "activeJobsCount" to v.activeJobsCount,
    "reviewCount" to v.reviewCount,
    "email" to v.email,
    "leadTech" to v.leadTech
  )

  // --- Document Mappers: Firestore Doc -> Model ---
  private fun docToCase(doc: DocumentSnapshot): CaseItem? {
    val id = doc.id
    val title = doc.getString("title") ?: return null
    val description = doc.getString("description") ?: ""
    val catStr = doc.getString("category") ?: CaseCategory.HVAC.name
    val priorityStr = doc.getString("priority") ?: CasePriority.MEDIUM.name
    val location = doc.getString("location") ?: ""
    val reportedBy = doc.getString("reportedBy") ?: "Resident"
    val reportedDate = doc.getString("reportedDate") ?: ""
    val statusStr = doc.getString("status") ?: CaseStatus.OPEN.name
    val rating = doc.getLong("rating")?.toInt()

    val category = try { CaseCategory.valueOf(catStr) } catch (e: Exception) { CaseCategory.HVAC }
    val priority = try { CasePriority.valueOf(priorityStr) } catch (e: Exception) { CasePriority.MEDIUM }
    val status = try { CaseStatus.valueOf(statusStr) } catch (e: Exception) { CaseStatus.OPEN }

    return CaseItem(
      id = id,
      title = title,
      description = description,
      category = category,
      priority = priority,
      location = location,
      reportedBy = reportedBy,
      reportedDate = reportedDate,
      assignedVendorId = doc.getString("assignedVendorId"),
      assignedVendorName = doc.getString("assignedVendorName"),
      status = status,
      rating = rating,
      reviewComment = doc.getString("reviewComment"),
      reviewTags = (doc.get("reviewTags") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
      photoNote = doc.getString("photoNote")
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun docToProperty(doc: DocumentSnapshot): PropertyItem? {
    val id = doc.id
    val unitNumber = doc.getString("unitNumber") ?: return null
    val pavilionWing = doc.getString("pavilionWing") ?: ""
    val bedrooms = doc.getLong("bedrooms")?.toInt() ?: 0
    val bathrooms = (doc.getDouble("bathrooms") ?: 0.0).toFloat()
    val sqFt = doc.getLong("sqFt")?.toInt() ?: 0
    val floorLevel = doc.getString("floorLevel") ?: ""
    val viewType = doc.getString("viewType") ?: ""
    val parkingSpots = (doc.get("parkingSpots") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val amenities = (doc.get("amenities") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val monthlyRental = doc.getString("monthlyRental") ?: ""
    val statusStr = doc.getString("status") ?: "AVAILABLE"
    val status = when (statusStr) {
      "OCCUPIED" -> PropertyStatus.OCCUPIED
      "MAINTENANCE", "UNDER_MAINTENANCE" -> PropertyStatus.UNDER_MAINTENANCE
      else -> PropertyStatus.AVAILABLE
    }

    val tenantMap = doc.get("assignedTenant") as? Map<String, Any?>
    val assignedTenant = tenantMap?.let {
      val name = it["name"] as? String ?: ""
      val email = it["email"] as? String ?: ""
      if (name.isNotBlank() && email.isNotBlank()) {
        AssignedTenantInfo(
          name = name,
          email = email,
          leaseStartDate = it["leaseStartDate"] as? String ?: "",
          leaseEndDate = it["leaseEndDate"] as? String ?: "",
          inviteCode = it["inviteCode"] as? String ?: "",
          inviteLink = it["inviteLink"] as? String ?: "",
          isClaimed = it["isClaimed"] as? Boolean ?: false,
          assignedTimestamp = it["assignedTimestamp"] as? String ?: ""
        )
      } else null
    }

    return PropertyItem(
      id = id,
      unitNumber = unitNumber,
      pavilionWing = pavilionWing,
      architecture = PropertyArchitecture(bedrooms, bathrooms, sqFt, floorLevel, viewType),
      parkingSpots = parkingSpots,
      amenities = amenities,
      monthlyRental = monthlyRental,
      status = status,
      assignedTenant = assignedTenant,
      listedDate = doc.getString("listedDate") ?: ""
    )
  }

  private fun docToTenant(doc: DocumentSnapshot): TenantProfile? {
    val id = doc.id
    val fullName = doc.getString("fullName") ?: return null
    val firstName = doc.getString("firstName") ?: ""
    val lastName = doc.getString("lastName") ?: ""
    val nationalId = doc.getString("nationalId") ?: ""
    val address = doc.getString("address") ?: ""
    val email = doc.getString("email") ?: ""
    val phone = doc.getString("phone") ?: ""
    val emergencyContactName = doc.getString("emergencyContactName") ?: ""
    val emergencyContactPhone = doc.getString("emergencyContactPhone") ?: ""
    val inviteCode = doc.getString("inviteCode") ?: ""

    return TenantProfile(
      id = id,
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      nationalId = nationalId,
      address = address,
      email = email,
      phone = phone,
      emergencyContactName = emergencyContactName,
      emergencyContactPhone = emergencyContactPhone,
      assignedPropertyId = doc.getString("assignedPropertyId"),
      assignedPropertyUnit = doc.getString("assignedPropertyUnit"),
      leaseStartDate = doc.getString("leaseStartDate"),
      leaseEndDate = doc.getString("leaseEndDate"),
      inviteCode = inviteCode,
      isAppRegistered = doc.getBoolean("isAppRegistered") ?: false,
      createdAt = doc.getString("createdAt") ?: ""
    )
  }

  private fun docToFacility(doc: DocumentSnapshot): Facility? {
    val id = doc.id
    val name = doc.getString("name") ?: return null
    val tag = doc.getString("tag") ?: "Estate Amenity"
    val capacity = doc.getLong("capacity")?.toInt() ?: 10
    val location = doc.getString("location") ?: ""
    val description = doc.getString("description") ?: ""
    val amenities = (doc.get("amenities") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val allowedSlots = (doc.get("allowedSlots") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val rules = doc.getString("rules") ?: ""

    return Facility(
      id = id,
      name = name,
      tag = tag,
      capacity = capacity,
      location = location,
      description = description,
      amenities = amenities,
      allowedSlots = allowedSlots,
      rules = rules
    )
  }

  private fun docToBooking(doc: DocumentSnapshot): FacilityBooking? {
    val id = doc.id
    val facilityId = doc.getString("facilityId") ?: return null
    val facilityName = doc.getString("facilityName") ?: "Amenity"
    val residentName = doc.getString("residentName") ?: ""
    val unit = doc.getString("unit") ?: ""
    val date = doc.getString("date") ?: ""
    val timeSlot = doc.getString("timeSlot") ?: ""
    val guestCount = doc.getLong("guestCount")?.toInt() ?: 1
    val notes = doc.getString("notes") ?: ""
    val confirmedTimestamp = doc.getString("confirmedTimestamp") ?: ""

    return FacilityBooking(
      id = id,
      facilityId = facilityId,
      facilityName = facilityName,
      residentName = residentName,
      unit = unit,
      date = date,
      timeSlot = timeSlot,
      guestCount = guestCount,
      notes = notes,
      confirmedTimestamp = confirmedTimestamp
    )
  }

  private fun docToMaintenance(doc: DocumentSnapshot): MaintenanceEvent? {
    val id = doc.id
    val title = doc.getString("title") ?: return null
    val discStr = doc.getString("discipline") ?: MaintenanceDiscipline.HVAC.name
    val scheduledDate = doc.getString("scheduledDate") ?: ""
    val scheduledTime = doc.getString("scheduledTime") ?: ""
    val contractorName = doc.getString("contractorName") ?: ""
    val targetArea = doc.getString("targetArea") ?: ""
    val intervalMonths = doc.getLong("intervalMonths")?.toInt() ?: 6
    val statusStr = doc.getString("status") ?: MaintenanceStatus.SCHEDULED.name
    val notes = doc.getString("notes") ?: ""

    val discipline = try { MaintenanceDiscipline.valueOf(discStr) } catch (e: Exception) { MaintenanceDiscipline.HVAC }
    val status = try { MaintenanceStatus.valueOf(statusStr) } catch (e: Exception) { MaintenanceStatus.SCHEDULED }

    return MaintenanceEvent(
      id = id,
      title = title,
      discipline = discipline,
      scheduledDate = scheduledDate,
      scheduledTime = scheduledTime,
      contractorName = contractorName,
      targetArea = targetArea,
      intervalMonths = intervalMonths,
      status = status,
      notes = notes
    )
  }

  private fun docToContract(doc: DocumentSnapshot): ContractItem? {
    val id = doc.id
    val title = doc.getString("title") ?: return null
    val counterparty = doc.getString("counterparty") ?: doc.getString("vendorName") ?: ""
    val typeStr = doc.getString("type") ?: doc.getString("serviceCategory") ?: ContractType.VENDOR_SLA.name
    val annualValue = doc.getString("annualValue") ?: ""
    val startDate = doc.getString("startDate") ?: ""
    val expiryDate = doc.getString("expiryDate") ?: doc.getString("renewalDate") ?: ""
    val daysRemaining = doc.getLong("daysRemaining")?.toInt() ?: 0
    val renewalTerms = doc.getString("renewalTerms") ?: ""
    val contactPerson = doc.getString("contactPerson") ?: ""
    val contactEmail = doc.getString("contactEmail") ?: ""
    val isNotificationSent = doc.getBoolean("isNotificationSent") ?: false

    val type = try { ContractType.valueOf(typeStr) } catch (e: Exception) { ContractType.VENDOR_SLA }

    return ContractItem(
      id = id,
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
      isNotificationSent = isNotificationSent
    )
  }

  private fun docToReminder(doc: DocumentSnapshot): ReminderItem? {
    val id = doc.id
    val title = doc.getString("title") ?: return null
    val message = doc.getString("message") ?: ""
    val targetAudience = doc.getString("targetAudience") ?: "All Residents"
    val urgency = doc.getString("urgency") ?: "Notice"
    val timestamp = doc.getString("timestamp") ?: ""
    val isBroadcast = doc.getBoolean("isBroadcast") ?: false

    return ReminderItem(
      id = id,
      title = title,
      message = message,
      targetAudience = targetAudience,
      urgency = urgency,
      timestamp = timestamp,
      isBroadcast = isBroadcast
    )
  }

  private fun docToVendor(doc: DocumentSnapshot): VendorItem? {
    val id = doc.id
    val name = doc.getString("name") ?: return null
    val specStr = doc.getString("specialty") ?: doc.getString("category") ?: CaseCategory.HVAC.name
    val rating = (doc.getDouble("rating") ?: 0.0).toFloat()
    val reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0
    val activeJobsCount = doc.getLong("activeJobsCount")?.toInt() ?: 0
    val phone = doc.getString("phone") ?: ""
    val email = doc.getString("email") ?: ""
    val leadTech = doc.getString("leadTech") ?: ""

    val specialty = try { CaseCategory.valueOf(specStr) } catch (e: Exception) { CaseCategory.HVAC }

    return VendorItem(
      id = id,
      name = name,
      specialty = specialty,
      rating = rating,
      reviewCount = reviewCount,
      activeJobsCount = activeJobsCount,
      phone = phone,
      email = email,
      leadTech = leadTech
    )
  }

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
      id = "CASE-${System.currentTimeMillis()}-${(100..999).random()}",
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

    db?.let { firestore ->
      try {
        val payload = mapCaseToFirestore(newCase)
        firestore.collection("cases").document(newCase.id).set(payload)
      } catch (e: Exception) {
        // Fallback to local state
      }
    }

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
    _vendors.update { current ->
      current.map { v ->
        if (v.id == vendorId) v.copy(activeJobsCount = v.activeJobsCount + 1) else v
      }
    }

    db?.let { firestore ->
      try {
        firestore.collection("cases").document(caseId).update(
          mapOf(
            "assignedVendorId" to vendor.id,
            "assignedVendorName" to vendor.name,
            "status" to CaseStatus.ASSIGNED.name
          )
        )
        firestore.collection("vendors").document(vendorId).update(
          "activeJobsCount", FieldValue.increment(1)
        )
      } catch (e: Exception) {
        // In-memory fallback
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
      try {
        firestore.collection("cases").document(caseId).update("status", newStatus.name)
      } catch (e: Exception) {
        // In-memory fallback
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

    db?.let { firestore ->
      try {
        firestore.collection("cases").document(caseId).update(
          mapOf(
            "rating" to rating,
            "reviewTags" to reviewTags,
            "reviewComment" to comment,
            "status" to CaseStatus.RESOLVED.name
          )
        )
      } catch (e: Exception) {
        // Fallback
      }
    }
  }

  fun deleteCase(caseId: String) {
    _cases.update { it.filterNot { c -> c.id == caseId } }
    db?.let { firestore ->
      try {
        firestore.collection("cases").document(caseId).delete()
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error deleting case: ${e.message}")
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

    db?.let { firestore ->
      try {
        val payload = mapBookingToFirestore(newBooking)
        firestore.collection("bookings").document(newBooking.id).set(payload)
      } catch (e: Exception) {
        // Fallback
      }
    }

    return newBooking
  }

  fun cancelBooking(bookingId: String) {
    _bookings.update { it.filterNot { b -> b.id == bookingId } }

    db?.let { firestore ->
      try {
        firestore.collection("bookings").document(bookingId).delete()
      } catch (e: Exception) {
        // Fallback
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
      id = "FAC-${System.currentTimeMillis() % 10000}",
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
      try {
        firestore.collection("facilities").document(newFacility.id).set(mapFacilityToFirestore(newFacility))
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error creating facility: ${e.message}")
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
        try {
          firestore.collection("facilities").document(facilityId).set(mapFacilityToFirestore(fac))
        } catch (e: Exception) {
          android.util.Log.e("EstateRepository", "Error updating facility: ${e.message}")
        }
      }
    }
  }

  fun deleteFacility(facilityId: String) {
    _facilities.update { it.filterNot { f -> f.id == facilityId } }
    db?.let { firestore ->
      try {
        firestore.collection("facilities").document(facilityId).delete()
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error deleting facility: ${e.message}")
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
    val event = MaintenanceEvent(
      id = "MAINT-${System.currentTimeMillis() % 10000}",
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
      try {
        val payload = mapMaintenanceToFirestore(event)
        firestore.collection("maintenances").document(event.id).set(payload)
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error scheduling maintenance: ${e.message}")
      }
    }
    return event
  }

  fun toggleMaintenanceStatus(eventId: String, newStatus: MaintenanceStatus) {
    _maintenances.update { current ->
      current.map { if (it.id == eventId) it.copy(status = newStatus) else it }
    }

    db?.let { firestore ->
      try {
        firestore.collection("maintenances").document(eventId).update("status", newStatus.name)
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error updating maintenance status: ${e.message}")
      }
    }
  }

  fun deleteMaintenance(eventId: String) {
    _maintenances.update { it.filterNot { m -> m.id == eventId } }
    db?.let { firestore ->
      try {
        firestore.collection("maintenances").document(eventId).delete()
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error deleting maintenance: ${e.message}")
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

    db?.let { firestore ->
      try {
        val payload = mapReminderToFirestore(newReminder)
        firestore.collection("reminders").document(newReminder.id).set(payload)
      } catch (e: Exception) {
        // Fallback
      }
    }

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

    db?.let { firestore ->
      try {
        firestore.collection("contracts").document(contractId).update("isNotificationSent", true)
      } catch (e: Exception) {
        // Fallback
      }
    }

    sendReminder(
      title = "Contract Expiry Alert: ${contract.title}",
      message = "${contract.title} with ${contract.counterparty} has ${contract.daysRemaining} days remaining. Review terms or start renewal RFP.",
      targetAudience = "Property Managers & Executive Board",
      urgency = "High Priority"
    )
  }

  fun renewContract(contractId: String, additionalYears: Int = 1) {
    val newDays = 365 * additionalYears
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

    _contracts.update { list ->
      list.map { c ->
        if (c.id == contractId) {
          c.copy(
            daysRemaining = c.daysRemaining + newDays,
            expiryDate = nextExpiry,
            isNotificationSent = false
          )
        } else c
      }
    }

    db?.let { firestore ->
      try {
        firestore.collection("contracts").document(contractId).update(
          mapOf(
            "daysRemaining" to FieldValue.increment(newDays.toLong()),
            "expiryDate" to nextExpiry,
            "renewalDate" to nextExpiry,
            "isNotificationSent" to false
          )
        )
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error renewing contract: ${e.message}")
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
    contactEmail: String
  ): ContractItem {
    val newContract = ContractItem(
      id = "CTR-${System.currentTimeMillis() % 10000}",
      title = title.trim(),
      counterparty = counterparty.trim(),
      type = type,
      annualValue = annualValue.trim(),
      startDate = startDate.trim(),
      expiryDate = expiryDate.trim(),
      daysRemaining = daysRemaining,
      renewalTerms = renewalTerms.trim(),
      contactPerson = contactPerson.trim(),
      contactEmail = contactEmail.trim(),
      isNotificationSent = false
    )
    _contracts.update { listOf(newContract) + it }

    db?.let { firestore ->
      try {
        firestore.collection("contracts").document(newContract.id).set(mapContractToFirestore(newContract))
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error creating contract: ${e.message}")
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
    contactEmail: String
  ) {
    var updated: ContractItem? = null
    _contracts.update { list ->
      list.map { c ->
        if (c.id == contractId) {
          c.copy(
            title = title.trim(),
            counterparty = counterparty.trim(),
            tenant = counterparty.trim(),
            type = type,
            annualValue = annualValue.trim(),
            contractAmount = annualValue.trim(),
            startDate = startDate.trim(),
            expiryDate = expiryDate.trim(),
            daysRemaining = daysRemaining,
            renewalTerms = renewalTerms.trim(),
            contactPerson = contactPerson.trim(),
            contactEmail = contactEmail.trim()
          ).also { updated = it }
        } else c
      }
    }
    updated?.let { ctr ->
      db?.let { firestore ->
        try {
          firestore.collection("contracts").document(contractId).set(mapContractToFirestore(ctr))
        } catch (e: Exception) {
          android.util.Log.e("EstateRepository", "Error updating contract: ${e.message}")
        }
      }
    }
  }

  fun deleteContract(contractId: String) {
    _contracts.update { it.filterNot { c -> c.id == contractId } }
    db?.let { firestore ->
      try {
        firestore.collection("contracts").document(contractId).delete()
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error deleting contract: ${e.message}")
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
      id = "VEND-${System.currentTimeMillis() % 10000}",
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
      try {
        firestore.collection("vendors").document(newVendor.id).set(mapVendorToFirestore(newVendor))
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error creating vendor: ${e.message}")
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
        try {
          firestore.collection("vendors").document(vendorId).set(mapVendorToFirestore(vend))
        } catch (e: Exception) {
          android.util.Log.e("EstateRepository", "Error updating vendor: ${e.message}")
        }
      }
    }
  }

  fun deleteVendor(vendorId: String) {
    _vendors.update { it.filterNot { v -> v.id == vendorId } }
    db?.let { firestore ->
      try {
        firestore.collection("vendors").document(vendorId).delete()
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error deleting vendor: ${e.message}")
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
      id = "PROP-${System.currentTimeMillis() % 10000}",
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
      try {
        val payload = mapPropertyToFirestore(newProp)
        firestore.collection("properties").document(newProp.id).set(payload)
      } catch (e: Exception) {
        // Fallback
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
        try {
          firestore.collection("properties").document(propertyId).set(mapPropertyToFirestore(prop))
        } catch (e: Exception) {
          android.util.Log.e("EstateRepository", "Error updating property: ${e.message}")
        }
      }
    }
  }

  fun deleteProperty(propertyId: String) {
    _properties.update { it.filterNot { p -> p.id == propertyId } }
    _tenants.update { list ->
      list.map { t ->
        if (t.assignedPropertyId == propertyId) {
          t.copy(assignedPropertyId = null, assignedPropertyUnit = null, leaseStartDate = null, leaseEndDate = null)
        } else t
      }
    }
    db?.let { firestore ->
      try {
        firestore.collection("properties").document(propertyId).delete()
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error deleting property: ${e.message}")
      }
    }
  }

  fun assignPropertyToTenant(
    propertyId: String,
    tenantName: String,
    tenantEmail: String,
    leaseStartDate: String,
    leaseEndDate: String
  ): AssignedTenantInfo {
    val codeSuffix = (propertyId.replace("PROP-", "") + "-" + (System.currentTimeMillis() % 1000)).uppercase()
    val inviteCode = "TUTS-$codeSuffix"
    val cleanEmail = tenantEmail.trim().lowercase()
    val inviteLink = "tutsville://onboard?propertyId=$propertyId&email=$cleanEmail&code=$inviteCode"
    val tenantInfo = AssignedTenantInfo(
      name = tenantName.trim(),
      email = cleanEmail,
      leaseStartDate = leaseStartDate.trim(),
      leaseEndDate = leaseEndDate.trim(),
      inviteCode = inviteCode,
      inviteLink = inviteLink,
      isClaimed = false,
      assignedTimestamp = dateFormat.format(Date()) + " • " + timeFormat.format(Date())
    )

    _properties.update { list ->
      list.map { p ->
        if (p.id == propertyId) {
          p.copy(
            status = PropertyStatus.OCCUPIED,
            assignedTenant = tenantInfo
          )
        } else p
      }
    }

    db?.let { firestore ->
      try {
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
            "assignedTenant" to tenantMap,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      } catch (e: Exception) {
        // Fallback
      }
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
          val updated = p.copy(assignedTenant = updatedTenant)
          claimedProp = updated
          updated
        } else p
      }
    }

    claimedProp?.let { prop ->
      db?.let { firestore ->
        try {
          firestore.collection("properties").document(prop.id).update(
            "assignedTenant.isClaimed", true
          )
        } catch (e: Exception) {
          // Fallback
        }
      }
    }

    return claimedProp
  }

  fun vacateProperty(propertyId: String) {
    _properties.update { list ->
      list.map { p ->
        if (p.id == propertyId) {
          p.copy(
            status = PropertyStatus.AVAILABLE,
            assignedTenant = null
          )
        } else p
      }
    }
    _tenants.update { list ->
      list.map { t ->
        if (t.assignedPropertyId == propertyId) {
          t.copy(
            assignedPropertyId = null,
            assignedPropertyUnit = null,
            leaseStartDate = null,
            leaseEndDate = null
          )
        } else t
      }
    }

    db?.let { firestore ->
      try {
        firestore.collection("properties").document(propertyId).update(
          mapOf(
            "status" to PropertyStatus.AVAILABLE.name,
            "assignedTenant" to null,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      } catch (e: Exception) {
        // Fallback
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
      id = "TENANT-${System.currentTimeMillis() % 10000}",
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
      try {
        val payload = mapTenantToFirestore(newTenant)
        firestore.collection("tenants").document(newTenant.id).set(payload)
      } catch (e: Exception) {
        // Fallback
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
        try {
          firestore.collection("tenants").document(tenantId).set(mapTenantToFirestore(tenant))
        } catch (e: Exception) {
          android.util.Log.e("EstateRepository", "Error updating tenant: ${e.message}")
        }
      }
    }
  }

  fun deleteTenant(tenantId: String) {
    unassignTenant(tenantId)
    _tenants.update { it.filterNot { t -> t.id == tenantId } }
    db?.let { firestore ->
      try {
        firestore.collection("tenants").document(tenantId).delete()
      } catch (e: Exception) {
        android.util.Log.e("EstateRepository", "Error deleting tenant: ${e.message}")
      }
    }
  }

  fun assignTenantToPropertyById(
    tenantId: String,
    propertyId: String,
    leaseStartDate: String,
    leaseEndDate: String
  ): TenantProfile? {
    val tenant = _tenants.value.find { it.id == tenantId } ?: return null
    val property = _properties.value.find { it.id == propertyId } ?: return null

    val inviteCode = tenant.inviteCode
    val cleanEmail = tenant.email.lowercase()
    val inviteLink = "tutsville://onboard?propertyId=$propertyId&email=$cleanEmail&code=$inviteCode"
    val tenantInfo = AssignedTenantInfo(
      name = tenant.fullName,
      email = cleanEmail,
      leaseStartDate = leaseStartDate.trim(),
      leaseEndDate = leaseEndDate.trim(),
      inviteCode = inviteCode,
      inviteLink = inviteLink,
      isClaimed = tenant.isAppRegistered,
      assignedTimestamp = dateFormat.format(Date()) + " • " + timeFormat.format(Date())
    )

    // Update Property
    _properties.update { list ->
      list.map { p ->
        if (p.id == propertyId) {
          p.copy(
            status = PropertyStatus.OCCUPIED,
            assignedTenant = tenantInfo
          )
        } else p
      }
    }

    // Update Tenant
    var updatedTenant: TenantProfile? = null
    _tenants.update { list ->
      list.map { t ->
        if (t.id == tenantId) {
          val upd = t.copy(
            assignedPropertyId = propertyId,
            assignedPropertyUnit = property.unitNumber,
            leaseStartDate = leaseStartDate.trim(),
            leaseEndDate = leaseEndDate.trim()
          )
          updatedTenant = upd
          upd
        } else t
      }
    }

    db?.let { firestore ->
      try {
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
            "assignedTenant" to tenantMap,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        firestore.collection("tenants").document(tenantId).update(
          mapOf(
            "assignedPropertyId" to propertyId,
            "assignedPropertyUnit" to property.unitNumber,
            "leaseStartDate" to leaseStartDate.trim(),
            "leaseEndDate" to leaseEndDate.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      } catch (e: Exception) {
        // Fallback
      }
    }

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
          t.copy(
            assignedPropertyId = null,
            assignedPropertyUnit = null,
            leaseStartDate = null,
            leaseEndDate = null
          )
        } else t
      }
    }

    db?.let { firestore ->
      try {
        firestore.collection("tenants").document(tenantId).update(
          mapOf(
            "assignedPropertyId" to null,
            "assignedPropertyUnit" to null,
            "leaseStartDate" to null,
            "leaseEndDate" to null,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      } catch (e: Exception) {
        // Fallback
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
          val upd = t.copy(isAppRegistered = true)
          claimedTenant = upd
          upd
        } else t
      }
    }
    claimPropertyInvite(cleanEmail)

    claimedTenant?.let { t ->
      db?.let { firestore ->
        try {
          firestore.collection("tenants").document(t.id).update(
            "isAppRegistered", true
          )
        } catch (e: Exception) {
          // Fallback
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
