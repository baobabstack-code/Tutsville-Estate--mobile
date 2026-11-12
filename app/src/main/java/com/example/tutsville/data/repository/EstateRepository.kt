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

  private val _cases = MutableStateFlow<List<CaseItem>>(if (db == null) seedCases() else emptyList())
  val cases: StateFlow<List<CaseItem>> = _cases.asStateFlow()

  private val _maintenances = MutableStateFlow<List<MaintenanceEvent>>(if (db == null) seedMaintenances() else emptyList())
  val maintenances: StateFlow<List<MaintenanceEvent>> = _maintenances.asStateFlow()

  private val _facilities = MutableStateFlow<List<Facility>>(if (db == null) seedFacilities() else emptyList())
  val facilities: StateFlow<List<Facility>> = _facilities.asStateFlow()

  private val _bookings = MutableStateFlow<List<FacilityBooking>>(if (db == null) seedBookings() else emptyList())
  val bookings: StateFlow<List<FacilityBooking>> = _bookings.asStateFlow()

  private val _contracts = MutableStateFlow<List<ContractItem>>(if (db == null) seedContracts() else emptyList())
  val contracts: StateFlow<List<ContractItem>> = _contracts.asStateFlow()

  private val _vendors = MutableStateFlow<List<VendorItem>>(if (db == null) seedVendors() else emptyList())
  val vendors: StateFlow<List<VendorItem>> = _vendors.asStateFlow()

  private val _reminders = MutableStateFlow<List<ReminderItem>>(if (db == null) seedReminders() else emptyList())
  val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

  private val _properties = MutableStateFlow<List<PropertyItem>>(if (db == null) seedProperties() else emptyList())
  val properties: StateFlow<List<PropertyItem>> = _properties.asStateFlow()

  private val _tenants = MutableStateFlow<List<TenantProfile>>(if (db == null) seedTenants() else emptyList())
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
          android.util.Log.d("EstateRepository", "Firebase Auth user active (${user.uid}). Triggering Firestore seed & sync.")
          db?.let { firestore ->
            checkAndSeedAllCollections(firestore, force = false)
            attachSnapshotListeners(firestore)
          }
        }
      }
    } catch (e: Exception) {
      android.util.Log.e("EstateRepository", "Error registering AuthStateListener: ${e.message}")
    }
    setupFirestoreSyncAndSeed()
  }

  fun seedDatabase(force: Boolean = false) {
    val firestore = db ?: return
    checkAndSeedAllCollections(firestore, force = force)
  }

  // --- Firestore Auto-Seed & Live Snapshot Sync ---
  private fun setupFirestoreSyncAndSeed() {
    val firestore = db ?: return
    checkAndSeedAllCollections(firestore, force = false)
    attachSnapshotListeners(firestore)
  }

  private fun checkAndSeedAllCollections(firestore: FirebaseFirestore, force: Boolean = false) {
    android.util.Log.d("EstateRepository", "Checking and seeding Firestore collections (force=$force)")

    // 1. Properties
    if (force) {
      seedProperties().forEach { p ->
        firestore.collection("properties").document(p.id).set(mapPropertyToFirestore(p))
      }
    } else {
      firestore.collection("properties").limit(1).get()
        .addOnSuccessListener { snap ->
          if (snap.isEmpty) {
            seedProperties().forEach { p ->
              firestore.collection("properties").document(p.id).set(mapPropertyToFirestore(p))
                .addOnSuccessListener { android.util.Log.d("EstateRepository", "Seeded property ${p.id}") }
                .addOnFailureListener { e -> android.util.Log.e("EstateRepository", "Failed property ${p.id}: ${e.message}") }
            }
          }
        }
        .addOnFailureListener { e ->
          android.util.Log.e("EstateRepository", "Properties query failed: ${e.message}")
        }
    }

    // 2. Tenants
    if (force) {
      seedTenants().forEach { t ->
        firestore.collection("tenants").document(t.id).set(mapTenantToFirestore(t))
      }
    } else {
      firestore.collection("tenants").limit(1).get()
        .addOnSuccessListener { snap ->
          if (snap.isEmpty) {
            seedTenants().forEach { t ->
              firestore.collection("tenants").document(t.id).set(mapTenantToFirestore(t))
                .addOnSuccessListener { android.util.Log.d("EstateRepository", "Seeded tenant ${t.id}") }
                .addOnFailureListener { e -> android.util.Log.e("EstateRepository", "Failed tenant ${t.id}: ${e.message}") }
            }
          }
        }
        .addOnFailureListener { e ->
          android.util.Log.e("EstateRepository", "Tenants query failed: ${e.message}")
        }
    }

    // 3. Facilities
    if (force) {
      seedFacilities().forEach { f ->
        firestore.collection("facilities").document(f.id).set(mapFacilityToFirestore(f))
      }
    } else {
      firestore.collection("facilities").limit(1).get()
        .addOnSuccessListener { snap ->
          if (snap.isEmpty) {
            seedFacilities().forEach { f ->
              firestore.collection("facilities").document(f.id).set(mapFacilityToFirestore(f))
                .addOnSuccessListener { android.util.Log.d("EstateRepository", "Seeded facility ${f.id}") }
                .addOnFailureListener { e -> android.util.Log.e("EstateRepository", "Failed facility ${f.id}: ${e.message}") }
            }
          }
        }
        .addOnFailureListener { e ->
          android.util.Log.e("EstateRepository", "Facilities query failed: ${e.message}")
        }
    }

    // 4. Vendors
    if (force) {
      seedVendors().forEach { v ->
        firestore.collection("vendors").document(v.id).set(mapVendorToFirestore(v))
      }
    } else {
      firestore.collection("vendors").limit(1).get()
        .addOnSuccessListener { snap ->
          if (snap.isEmpty) {
            seedVendors().forEach { v ->
              firestore.collection("vendors").document(v.id).set(mapVendorToFirestore(v))
                .addOnSuccessListener { android.util.Log.d("EstateRepository", "Seeded vendor ${v.id}") }
                .addOnFailureListener { e -> android.util.Log.e("EstateRepository", "Failed vendor ${v.id}: ${e.message}") }
            }
          }
        }
        .addOnFailureListener { e ->
          android.util.Log.e("EstateRepository", "Vendors query failed: ${e.message}")
        }
    }

    // 5. Contracts
    if (force) {
      seedContracts().forEach { c ->
        firestore.collection("contracts").document(c.id).set(mapContractToFirestore(c))
      }
    } else {
      firestore.collection("contracts").limit(1).get()
        .addOnSuccessListener { snap ->
          if (snap.isEmpty) {
            seedContracts().forEach { c ->
              firestore.collection("contracts").document(c.id).set(mapContractToFirestore(c))
                .addOnSuccessListener { android.util.Log.d("EstateRepository", "Seeded contract ${c.id}") }
                .addOnFailureListener { e -> android.util.Log.e("EstateRepository", "Failed contract ${c.id}: ${e.message}") }
            }
          }
        }
        .addOnFailureListener { e ->
          android.util.Log.e("EstateRepository", "Contracts query failed: ${e.message}")
        }
    }

    // 6. Maintenances
    if (force) {
      seedMaintenances().forEach { m ->
        firestore.collection("maintenances").document(m.id).set(mapMaintenanceToFirestore(m))
      }
    } else {
      firestore.collection("maintenances").limit(1).get()
        .addOnSuccessListener { snap ->
          if (snap.isEmpty) {
            seedMaintenances().forEach { m ->
              firestore.collection("maintenances").document(m.id).set(mapMaintenanceToFirestore(m))
                .addOnSuccessListener { android.util.Log.d("EstateRepository", "Seeded maintenance ${m.id}") }
                .addOnFailureListener { e -> android.util.Log.e("EstateRepository", "Failed maintenance ${m.id}: ${e.message}") }
            }
          }
        }
        .addOnFailureListener { e ->
          android.util.Log.e("EstateRepository", "Maintenances query failed: ${e.message}")
        }
    }

    // 7. Reminders
    if (force) {
      seedReminders().forEach { r ->
        firestore.collection("reminders").document(r.id).set(mapReminderToFirestore(r))
      }
    } else {
      firestore.collection("reminders").limit(1).get()
        .addOnSuccessListener { snap ->
          if (snap.isEmpty) {
            seedReminders().forEach { r ->
              firestore.collection("reminders").document(r.id).set(mapReminderToFirestore(r))
                .addOnSuccessListener { android.util.Log.d("EstateRepository", "Seeded reminder ${r.id}") }
                .addOnFailureListener { e -> android.util.Log.e("EstateRepository", "Failed reminder ${r.id}: ${e.message}") }
            }
          }
        }
        .addOnFailureListener { e ->
          android.util.Log.e("EstateRepository", "Reminders query failed: ${e.message}")
        }
    }

    // 8. Cases
    if (force) {
      seedCases().forEach { cs ->
        firestore.collection("cases").document(cs.id).set(mapCaseToFirestore(cs))
      }
    } else {
      firestore.collection("cases").limit(1).get()
        .addOnSuccessListener { snap ->
          if (snap.isEmpty) {
            seedCases().forEach { cs ->
              firestore.collection("cases").document(cs.id).set(mapCaseToFirestore(cs))
                .addOnSuccessListener { android.util.Log.d("EstateRepository", "Seeded case ${cs.id}") }
                .addOnFailureListener { e -> android.util.Log.e("EstateRepository", "Failed case ${cs.id}: ${e.message}") }
            }
          }
        }
        .addOnFailureListener { e ->
          android.util.Log.e("EstateRepository", "Cases query failed: ${e.message}")
        }
    }

    // 9. Bookings
    if (force) {
      seedBookings().forEach { b ->
        firestore.collection("bookings").document(b.id).set(mapBookingToFirestore(b))
      }
    } else {
      firestore.collection("bookings").limit(1).get()
        .addOnSuccessListener { snap ->
          if (snap.isEmpty) {
            seedBookings().forEach { b ->
              firestore.collection("bookings").document(b.id).set(mapBookingToFirestore(b))
                .addOnSuccessListener { android.util.Log.d("EstateRepository", "Seeded booking ${b.id}") }
                .addOnFailureListener { e -> android.util.Log.e("EstateRepository", "Failed booking ${b.id}: ${e.message}") }
            }
          }
        }
        .addOnFailureListener { e ->
          android.util.Log.e("EstateRepository", "Bookings query failed: ${e.message}")
        }
    }
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
    "fullName" to t.fullName,
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
    val bedrooms = doc.getLong("bedrooms")?.toInt() ?: 2
    val bathrooms = (doc.getDouble("bathrooms") ?: 2.0).toFloat()
    val sqFt = doc.getLong("sqFt")?.toInt() ?: 2000
    val floorLevel = doc.getString("floorLevel") ?: "Level 1"
    val viewType = doc.getString("viewType") ?: "Estate Garden View"
    val parkingSpots = (doc.get("parkingSpots") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val amenities = (doc.get("amenities") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
    val monthlyRental = doc.getString("monthlyRental") ?: "$8,000 / mo"
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
    val email = doc.getString("email") ?: ""
    val phone = doc.getString("phone") ?: ""
    val emergencyContactName = doc.getString("emergencyContactName") ?: ""
    val emergencyContactPhone = doc.getString("emergencyContactPhone") ?: ""
    val inviteCode = doc.getString("inviteCode") ?: ""

    return TenantProfile(
      id = id,
      fullName = fullName,
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
    val residentName = doc.getString("residentName") ?: "Resident"
    val unit = doc.getString("unit") ?: "Unit 101"
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
    val annualValue = doc.getString("annualValue") ?: "$50,000 / yr"
    val startDate = doc.getString("startDate") ?: ""
    val expiryDate = doc.getString("expiryDate") ?: doc.getString("renewalDate") ?: ""
    val daysRemaining = doc.getLong("daysRemaining")?.toInt() ?: 30
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
    val rating = (doc.getDouble("rating") ?: 4.5).toFloat()
    val reviewCount = doc.getLong("reviewCount")?.toInt() ?: 10
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
  ) {
    val event = MaintenanceEvent(
      id = "MAINT-${System.currentTimeMillis() % 10000}",
      title = title,
      discipline = discipline,
      scheduledDate = date,
      scheduledTime = time,
      contractorName = contractor,
      targetArea = targetArea,
      intervalMonths = intervalMonths,
      status = MaintenanceStatus.SCHEDULED,
      notes = notes
    )
    _maintenances.update { listOf(event) + it }

    db?.let { firestore ->
      try {
        val payload = mapMaintenanceToFirestore(event)
        firestore.collection("maintenances").document(event.id).set(payload)
      } catch (e: Exception) {
        // Fallback
      }
    }
  }

  fun toggleMaintenanceStatus(eventId: String, newStatus: MaintenanceStatus) {
    _maintenances.update { current ->
      current.map { if (it.id == eventId) it.copy(status = newStatus) else it }
    }

    db?.let { firestore ->
      try {
        firestore.collection("maintenances").document(eventId).update("status", newStatus.name)
      } catch (e: Exception) {
        // Fallback
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
    _contracts.update { list ->
      list.map { c ->
        if (c.id == contractId) {
          c.copy(
            daysRemaining = c.daysRemaining + newDays,
            expiryDate = "Nov 15, 2027",
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
            "expiryDate" to "Nov 15, 2027",
            "renewalDate" to "Nov 15, 2027",
            "isNotificationSent" to false
          )
        )
      } catch (e: Exception) {
        // Fallback
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
      rating = 5.0f,
      reviewCount = 1,
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
    emergencyContactPhone: String
  ): TenantProfile {
    val cleanEmail = email.trim().lowercase()
    val codeSuffix = (System.currentTimeMillis() % 10000).toString().padStart(4, '0')
    val inviteCode = "TUTS-RES-$codeSuffix"
    val newTenant = TenantProfile(
      id = "TENANT-${System.currentTimeMillis() % 10000}",
      fullName = fullName.trim(),
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

  // --- Default Seed Data ---
  private fun seedCases(): List<CaseItem> = listOf(
    CaseItem(
      id = "CASE-1082",
      title = "East Wing Master Chiller Pressure Drop",
      description = "Secondary condenser loop pressure dropped below 2.4 bar. Compressor vibrating excessively in plant room.",
      category = CaseCategory.HVAC,
      priority = CasePriority.EMERGENCY,
      location = "Central Utility Basement • Plant B",
      reportedBy = "Julian Vance (Chief Engineer)",
      reportedDate = "Nov 09, 2026",
      assignedVendorId = "VEND-01",
      assignedVendorName = "Apex HVAC Solutions",
      status = CaseStatus.IN_PROGRESS
    ),
    CaseItem(
      id = "CASE-1081",
      title = "Penthouse 602 Master Bath Valve Seepage",
      description = "Thermostatic mixer valve behind marble wall has intermittent seepage causing water pressure fluctuations.",
      category = CaseCategory.PLUMBING,
      priority = CasePriority.HIGH,
      location = "North Tower • Penthouse 602",
      reportedBy = "Eleanor Sterling (Resident)",
      reportedDate = "Nov 08, 2026",
      assignedVendorId = "VEND-03",
      assignedVendorName = "AquaPave Plumbing",
      status = CaseStatus.ASSIGNED
    ),
    CaseItem(
      id = "CASE-1079",
      title = "Clubhouse Veranda Column Sconce Flickering",
      description = "Architectural LED driver intermittent on west colonnade. Needs driver replacement and circuit load test.",
      category = CaseCategory.ELECTRICAL,
      priority = CasePriority.MEDIUM,
      location = "The Orangery Clubhouse • West Veranda",
      reportedBy = "Marcus Rowe (Estate Concierge)",
      reportedDate = "Nov 07, 2026",
      assignedVendorId = "VEND-02",
      assignedVendorName = "VoltCraft Engineering",
      status = CaseStatus.OPEN
    ),
    CaseItem(
      id = "CASE-1075",
      title = "South Pavilion Courtyard Fountain Pump Overhaul",
      description = "Variable frequency drive tuned and dual impeller replaced. Flow rate restored to optimal 450 GPM.",
      category = CaseCategory.GROUNDS,
      priority = CasePriority.MEDIUM,
      location = "South Courtyard • Tiered Fountain",
      reportedBy = "Diana Chen (Grounds Manager)",
      reportedDate = "Nov 04, 2026",
      assignedVendorId = "VEND-04",
      assignedVendorName = "Verdant Living Grounds",
      status = CaseStatus.RESOLVED,
      rating = 5,
      reviewTags = listOf("Punctual", "Clean Worksite", "High Craftsmanship"),
      reviewComment = "Impeccable execution. Water feature running silently with balanced pressure."
    ),
    CaseItem(
      id = "CASE-1071",
      title = "Tower B Lift 3 Door Sensor Realignment",
      description = "Light curtain photodiode array re-calibrated. Door closing torque within safety margins.",
      category = CaseCategory.STRUCTURAL,
      priority = CasePriority.HIGH,
      location = "Tower B • Lift Shaft 3",
      reportedBy = "Julian Vance",
      reportedDate = "Nov 01, 2026",
      assignedVendorId = "VEND-05",
      assignedVendorName = "Otis Vertical Transit",
      status = CaseStatus.RESOLVED,
      rating = 4,
      reviewTags = listOf("Resolved First Visit", "Detailed Report"),
      reviewComment = "Prompt response time and comprehensive diagnostic test report provided."
    )
  )

  private fun seedMaintenances(): List<MaintenanceEvent> = listOf(
    MaintenanceEvent(
      id = "MAINT-501",
      title = "Annual Fire Sprinkler & Hydrant Flow Test",
      discipline = MaintenanceDiscipline.FIRE_LIFE,
      scheduledDate = "Nov 12, 2026",
      scheduledTime = "09:00 AM - 01:00 PM",
      contractorName = "Sentinel Fire & Safety",
      targetArea = "Entire Estate • Wings A, B & C",
      intervalMonths = 12,
      status = MaintenanceStatus.SCHEDULED,
      notes = "Riser flow rates, diesel booster pump activation test, and alarm panel linkage audit."
    ),
    MaintenanceEvent(
      id = "MAINT-502",
      title = "Quarterly Elevator Brake & Cable Safety Audit",
      discipline = MaintenanceDiscipline.ELEVATORS,
      scheduledDate = "Nov 14, 2026",
      scheduledTime = "10:00 AM - 04:00 PM",
      contractorName = "Otis Vertical Transit",
      targetArea = "Towers A, B & Central Core",
      intervalMonths = 3,
      status = MaintenanceStatus.SCHEDULED,
      notes = "Governor rope tension, safety gear drop test, and car leveling sensor calibration."
    ),
    MaintenanceEvent(
      id = "MAINT-503",
      title = "Primary Substation Transformer Infrared Scan",
      discipline = MaintenanceDiscipline.ELECTRICAL,
      scheduledDate = "Nov 18, 2026",
      scheduledTime = "01:00 PM - 05:00 PM",
      contractorName = "VoltCraft Engineering",
      targetArea = "High Voltage Substation 1",
      intervalMonths = 6,
      status = MaintenanceStatus.SCHEDULED,
      notes = "Thermographic thermometry scan for hot spots across busbars and circuit breakers."
    ),
    MaintenanceEvent(
      id = "MAINT-504",
      title = "Central Chiller Water Treatment & Chemical Balancing",
      discipline = MaintenanceDiscipline.HVAC,
      scheduledDate = "Nov 06, 2026",
      scheduledTime = "08:00 AM - 12:00 PM",
      contractorName = "Apex HVAC Solutions",
      targetArea = "Cooling Towers & Closed Loops",
      intervalMonths = 1,
      status = MaintenanceStatus.COMPLETED,
      notes = "Biocide injection, conductivity check, and scale inhibitor levels verified."
    ),
    MaintenanceEvent(
      id = "MAINT-505",
      title = "Domestic Potable Water Tank Sterilization",
      discipline = MaintenanceDiscipline.PLUMBING,
      scheduledDate = "Nov 02, 2026",
      scheduledTime = "07:00 AM - 03:00 PM",
      contractorName = "AquaPave Plumbing",
      targetArea = "Rooftop Storage Cisterns",
      intervalMonths = 6,
      status = MaintenanceStatus.COMPLETED,
      notes = "Complete drain-down, ultraviolet scrub, and certified laboratory water quality test."
    )
  )

  private fun seedFacilities(): List<Facility> = listOf(
    Facility(
      id = "FAC-01",
      name = "The Orangery Clubhouse",
      tag = "Grand Reception & Dining",
      capacity = 45,
      location = "Central Gardens • Pavillion A",
      description = "Heritage glasshouse with vaulted timber ceilings, catering prep kitchen, fireside salon, and integrated Sonos sound system.",
      amenities = listOf("Catering Kitchen", "Fireplace Lounge", "Grand Piano", "Wine Chiller", "Audiovisual"),
      allowedSlots = listOf("10:00 AM - 02:00 PM", "03:00 PM - 07:00 PM", "07:30 PM - 11:30 PM"),
      rules = "Max 45 guests. Music cutoff at 11:00 PM. Catering setup allowed 1 hour prior."
    ),
    Facility(
      id = "FAC-02",
      name = "North Terrace Tennis Court",
      tag = "All-Weather Surface",
      capacity = 8,
      location = "North Grounds",
      description = "Championship regulation hard court with LED floodlighting, automated ball launcher, and courtside spectator pergola.",
      amenities = listOf("Night Floodlights", "Ball Machine", "Shaded Pergola", "Chilled Water Station"),
      allowedSlots = listOf("07:00 AM - 09:00 AM", "09:30 AM - 11:30 AM", "04:00 PM - 06:00 PM", "06:30 PM - 08:30 PM"),
      rules = "Non-marking tennis shoes required. 2-hour max session per resident per day."
    ),
    Facility(
      id = "FAC-03",
      name = "Skyline Rooftop Lounge",
      tag = "Panoramic Sunset Deck",
      capacity = 30,
      location = "Tower A • Level 28",
      description = "Open-air landscaped terrace with 360-degree estate views, brass fire troughs, cocktail bar, and heated lounge booths.",
      amenities = listOf("Gas Fire Pits", "Cocktail Bar", "Heated Seating", "Sunset Viewing Deck"),
      allowedSlots = listOf("04:00 PM - 07:00 PM", "07:30 PM - 11:00 PM"),
      rules = "Residents must be present with guests. No glassware near perimeter railings."
    ),
    Facility(
      id = "FAC-04",
      name = "Bespoke Cinema Salon",
      tag = "Private Screening Room",
      capacity = 16,
      location = "Clubhouse Lower Level",
      description = "Acoustically tuned screening theater with motorized Italian leather recliners, 4K laser projection, and Dolby Atmos 9.2.",
      amenities = listOf("Dolby Atmos", "4K Laser Projection", "Leather Recliners", "Popcorn Bar"),
      allowedSlots = listOf("01:00 PM - 04:00 PM", "05:00 PM - 08:00 PM", "08:30 PM - 11:30 PM"),
      rules = "Streaming services pre-logged. Please return room to clean state after screening."
    ),
    Facility(
      id = "FAC-05",
      name = "Garden BBQ Pergola",
      tag = "Alfresco Culinary Station",
      capacity = 20,
      location = "South Meadow Gardens",
      description = "Cedar pergola featuring dual Lynx infrared grills, wood-fired pizza oven, prep counters, and rustic teak dining table.",
      amenities = listOf("Dual Lynx Gas Grills", "Wood Pizza Oven", "Teak Dining Table", "Outdoor Sink"),
      allowedSlots = listOf("11:30 AM - 03:00 PM", "04:30 PM - 08:30 PM"),
      rules = "Clean grill grates after use with provided brass wire brushes. Extinguish oven embers."
    )
  )

  private fun seedBookings(): List<FacilityBooking> = listOf(
    FacilityBooking(
      id = "BK-8021",
      facilityId = "FAC-01",
      facilityName = "The Orangery Clubhouse",
      residentName = "Lord Arthur Tutsville",
      unit = "Penthouse A",
      date = "Nov 15, 2026",
      timeSlot = "07:30 PM - 11:30 PM",
      guestCount = 28,
      notes = "Annual Autumn Estate Stewardship Dinner",
      confirmedTimestamp = "Nov 05, 2026 • 10:14 AM"
    ),
    FacilityBooking(
      id = "BK-8019",
      facilityId = "FAC-02",
      facilityName = "North Terrace Tennis Court",
      residentName = "Camilla Davenport",
      unit = "Unit 304",
      date = "Nov 10, 2026",
      timeSlot = "09:30 AM - 11:30 AM",
      guestCount = 4,
      notes = "Morning doubles friendly",
      confirmedTimestamp = "Nov 08, 2026 • 04:22 PM"
    )
  )

  private fun seedContracts(): List<ContractItem> = listOf(
    ContractItem(
      id = "CTR-SLA-01",
      title = "Elevator & Vertical Transit Master SLA",
      counterparty = "Otis Vertical Transit Corp",
      type = ContractType.VENDOR_SLA,
      annualValue = "$84,000 / yr",
      startDate = "Dec 01, 2023",
      expiryDate = "Nov 22, 2026",
      daysRemaining = 13,
      renewalTerms = "Requires 30-day written tender notice. 3% annual inflation cap index.",
      contactPerson = "Marcus Kane (Account Executive)",
      contactEmail = "marcus.kane@otis-partners.com"
    ),
    ContractItem(
      id = "CTR-SLA-02",
      title = "Estate Landscaping & Arboriculture SLA",
      counterparty = "Verdant Living Grounds Ltd",
      type = ContractType.VENDOR_SLA,
      annualValue = "$62,500 / yr",
      startDate = "Jan 01, 2024",
      expiryDate = "Dec 05, 2026",
      daysRemaining = 26,
      renewalTerms = "Includes spring tree pruning, irrigation winterization, weekly turf manicure.",
      contactPerson = "Elena Gomez",
      contactEmail = "elena@verdantgrounds.com"
    ),
    ContractItem(
      id = "CTR-LEASE-01",
      title = "Penthouse 801 Residential Lease Agreement",
      counterparty = "Dr. Nathaniel & Claire Thorne",
      type = ContractType.TENANT_LEASE,
      annualValue = "$180,000 / yr",
      startDate = "Jan 01, 2025",
      expiryDate = "Dec 28, 2026",
      daysRemaining = 49,
      renewalTerms = "Two-year option at prevailing market index. Requires tenant intent 45 days prior.",
      contactPerson = "Nathaniel Thorne",
      contactEmail = "nthorne@medicine.org"
    ),
    ContractItem(
      id = "CTR-WARR-01",
      title = "Commercial Centrifugal Chiller Factory Warranty",
      counterparty = "Daikin Applied Engineering",
      type = ContractType.EQUIPMENT_WARRANTY,
      annualValue = "$18,000 / yr",
      startDate = "Apr 15, 2024",
      expiryDate = "May 15, 2027",
      daysRemaining = 187,
      renewalTerms = "Covers magnetic bearing compressors, electronic expansion valves, and heat exchangers.",
      contactPerson = "Daikin Care Desk",
      contactEmail = "support@daikinapplied.com"
    ),
    ContractItem(
      id = "CTR-SLA-03",
      title = "Estate Security & Biometric Access Maintenance",
      counterparty = "SecureGate Biometrics Inc",
      type = ContractType.VENDOR_SLA,
      annualValue = "$45,000 / yr",
      startDate = "Mar 01, 2025",
      expiryDate = "Feb 28, 2027",
      daysRemaining = 111,
      renewalTerms = "24/7 emergency dispatch response within 45 minutes. Bi-annual firmware updates.",
      contactPerson = "David Cross",
      contactEmail = "dcross@securegate.io"
    )
  )

  private fun seedVendors(): List<VendorItem> = listOf(
    VendorItem(
      id = "VEND-01",
      name = "Apex HVAC Solutions",
      specialty = CaseCategory.HVAC,
      rating = 4.9f,
      reviewCount = 42,
      activeJobsCount = 2,
      phone = "+1 (555) 234-8901",
      email = "dispatch@apexhvac.com",
      leadTech = "Robert Miller (Master Tech #819)"
    ),
    VendorItem(
      id = "VEND-02",
      name = "VoltCraft Engineering",
      specialty = CaseCategory.ELECTRICAL,
      rating = 4.8f,
      reviewCount = 38,
      activeJobsCount = 1,
      phone = "+1 (555) 442-1920",
      email = "ops@voltcraft-eng.com",
      leadTech = "Liam Sterling (Industrial Electrician)"
    ),
    VendorItem(
      id = "VEND-03",
      name = "AquaPave Plumbing",
      specialty = CaseCategory.PLUMBING,
      rating = 4.7f,
      reviewCount = 51,
      activeJobsCount = 3,
      phone = "+1 (555) 887-3402",
      email = "service@aquapave.com",
      leadTech = "Patrick O'Connor"
    ),
    VendorItem(
      id = "VEND-04",
      name = "Verdant Living Grounds",
      specialty = CaseCategory.GROUNDS,
      rating = 4.9f,
      reviewCount = 29,
      activeJobsCount = 1,
      phone = "+1 (555) 901-2244",
      email = "care@verdantgrounds.com",
      leadTech = "Mateo Rossi"
    ),
    VendorItem(
      id = "VEND-05",
      name = "Otis Vertical Transit",
      specialty = CaseCategory.STRUCTURAL,
      rating = 4.6f,
      reviewCount = 64,
      activeJobsCount = 2,
      phone = "+1 (555) 112-9900",
      email = "service@otis-partners.com",
      leadTech = "Gregory Vance"
    )
  )

  private fun seedReminders(): List<ReminderItem> = listOf(
    ReminderItem(
      id = "REM-01",
      title = "Scheduled Lift Servicing • Tower B",
      message = "Tower B Lift 2 will be offline tomorrow from 10:00 AM to 02:00 PM for cable re-tensioning.",
      targetAudience = "All Tower B Residents",
      urgency = "Notice",
      timestamp = "Nov 08, 2026 • 03:30 PM",
      isBroadcast = true
    ),
    ReminderItem(
      id = "REM-02",
      title = "Fire Alarm Acoustic Siren Calibration",
      message = "Routine 10-second bell testing will occur on Thursday Nov 12 at 11:00 AM across all common corridors.",
      targetAudience = "Estate-Wide Announcement",
      urgency = "Advisory",
      timestamp = "Nov 07, 2026 • 11:00 AM",
      isBroadcast = true
    ),
    ReminderItem(
      id = "REM-03",
      title = "Contract Expiry Notice: Otis Vertical Transit",
      message = "Master elevator maintenance SLA reaches 14-day renewal threshold. Board decision needed on 3-year extension.",
      targetAudience = "Property Managers",
      urgency = "Action Required",
      timestamp = "Nov 06, 2026 • 09:15 AM",
      isBroadcast = false
    )
  )

  private fun seedProperties(): List<PropertyItem> = listOf(
    PropertyItem(
      id = "PROP-801",
      unitNumber = "Penthouse 801 • Da Vinci Suite",
      pavilionWing = "North Tower • Summit",
      architecture = PropertyArchitecture(
        bedrooms = 4,
        bathrooms = 4.5f,
        sqFt = 4500,
        floorLevel = "Level 28 (Penthouse)",
        viewType = "360° Mountain & Estate Panoramic"
      ),
      parkingSpots = listOf("Bay P1-A (Tesla EV Ultra)", "Bay P1-B (Tesla EV Ultra)"),
      amenities = listOf(
        "Private Rooftop Plunge Pool",
        "Sommelier Wine Cellar",
        "Bang & Olufsen Smart Automation",
        "Private Keyed Elevator Foyer",
        "Wraparound Cedar Terrace"
      ),
      monthlyRental = "$15,000 / mo",
      status = PropertyStatus.OCCUPIED,
      assignedTenant = AssignedTenantInfo(
        name = "Dr. Nathaniel & Claire Thorne",
        email = "nthorne@medicine.org",
        leaseStartDate = "Jan 01, 2025",
        leaseEndDate = "Dec 28, 2026",
        inviteCode = "TUTS-801-921",
        inviteLink = "tutsville://onboard?propertyId=PROP-801&email=nthorne@medicine.org&code=TUTS-801-921",
        isClaimed = true,
        assignedTimestamp = "Dec 15, 2024 • 11:30 AM"
      ),
      listedDate = "Nov 01, 2024"
    ),
    PropertyItem(
      id = "PROP-402",
      unitNumber = "Villa 402 • Cedar Glade",
      pavilionWing = "West Pavilion Grounds",
      architecture = PropertyArchitecture(
        bedrooms = 3,
        bathrooms = 3.5f,
        sqFt = 3200,
        floorLevel = "Ground & Level 1 Dual-Level",
        viewType = "Private Forest Meadow & Fountains"
      ),
      parkingSpots = listOf("Bay W-12 (EV Level 2)", "Bay W-13"),
      amenities = listOf(
        "Private Heated Dip Pool",
        "Teak Outdoor Sun Deck",
        "Wolf & Sub-Zero Gourmet Kitchen",
        "Calacatta Marble Spa Bath",
        "Integrated Security System"
      ),
      monthlyRental = "$11,200 / mo",
      status = PropertyStatus.OCCUPIED,
      assignedTenant = AssignedTenantInfo(
        name = "Eleanor Sterling",
        email = "elementary221b@gmail.com",
        leaseStartDate = "Feb 01, 2025",
        leaseEndDate = "Jan 31, 2027",
        inviteCode = "TUTS-402-404",
        inviteLink = "tutsville://onboard?propertyId=PROP-402&email=elementary221b@gmail.com&code=TUTS-402-404",
        isClaimed = true,
        assignedTimestamp = "Jan 10, 2025 • 03:15 PM"
      ),
      listedDate = "Dec 05, 2024"
    ),
    PropertyItem(
      id = "PROP-104",
      unitNumber = "Garden Villa 104 • The Orangery Veranda",
      pavilionWing = "South Courtyard",
      architecture = PropertyArchitecture(
        bedrooms = 2,
        bathrooms = 2.0f,
        sqFt = 2100,
        floorLevel = "Ground Level with Private Garden",
        viewType = "Orangery Courtyard Fountains"
      ),
      parkingSpots = listOf("Bay S-04 (EV Charging Bay)"),
      amenities = listOf(
        "Enclosed Glass Herb Conservatory",
        "Carved Limestone Fireplace",
        "Private Lawned Courtyard",
        "Dual Vanity Dressing Suite"
      ),
      monthlyRental = "$8,500 / mo",
      status = PropertyStatus.AVAILABLE,
      assignedTenant = null,
      listedDate = "Nov 02, 2026"
    ),
    PropertyItem(
      id = "PROP-602",
      unitNumber = "Residence 602 • Sky View",
      pavilionWing = "Tower B • Upper Terraces",
      architecture = PropertyArchitecture(
        bedrooms = 3,
        bathrooms = 3.0f,
        sqFt = 2800,
        floorLevel = "Level 14",
        viewType = "Estate Skyline & Tennis Meadows"
      ),
      parkingSpots = listOf("Bay B-08 (EV Ready)"),
      amenities = listOf(
        "Custom Italian Poliform Kitchen",
        "Dual Sunset View Balconies",
        "Acoustic Glass Glazing",
        "Smart Climate Zoning"
      ),
      monthlyRental = "$9,800 / mo",
      status = PropertyStatus.AVAILABLE,
      assignedTenant = null,
      listedDate = "Nov 06, 2026"
    )
  )

  private fun seedTenants(): List<TenantProfile> = listOf(
    TenantProfile(
      id = "TENANT-101",
      fullName = "Dr. Nathaniel Thorne",
      email = "nthorne@medicine.org",
      phone = "+1 (555) 349-2104",
      emergencyContactName = "Claire Thorne (Spouse)",
      emergencyContactPhone = "+1 (555) 349-2105",
      assignedPropertyId = "PROP-801",
      assignedPropertyUnit = "Penthouse 801 • Da Vinci Suite",
      leaseStartDate = "Jan 01, 2025",
      leaseEndDate = "Dec 28, 2026",
      inviteCode = "TUTS-801-921",
      isAppRegistered = true,
      createdAt = "Dec 15, 2024"
    ),
    TenantProfile(
      id = "TENANT-102",
      fullName = "Eleanor Sterling",
      email = "elementary221b@gmail.com",
      phone = "+1 (555) 872-9901",
      emergencyContactName = "Marcus Sterling (Brother)",
      emergencyContactPhone = "+1 (555) 872-9908",
      assignedPropertyId = "PROP-402",
      assignedPropertyUnit = "Villa 402 • Cedar Glade",
      leaseStartDate = "Feb 01, 2025",
      leaseEndDate = "Jan 31, 2027",
      inviteCode = "TUTS-402-404",
      isAppRegistered = true,
      createdAt = "Jan 10, 2025"
    ),
    TenantProfile(
      id = "TENANT-103",
      fullName = "Camilla Davenport",
      email = "camilla.d@davenport.co",
      phone = "+1 (555) 604-1188",
      emergencyContactName = "Arthur Davenport (Father)",
      emergencyContactPhone = "+1 (555) 604-1190",
      assignedPropertyId = null,
      assignedPropertyUnit = null,
      leaseStartDate = null,
      leaseEndDate = null,
      inviteCode = "TUTS-RES-6041",
      isAppRegistered = false,
      createdAt = "Nov 05, 2026"
    ),
    TenantProfile(
      id = "TENANT-104",
      fullName = "Julian Vance",
      email = "julian.vance@tutsville.estate",
      phone = "+1 (555) 412-8820",
      emergencyContactName = "Victoria Vance",
      emergencyContactPhone = "+1 (555) 412-8821",
      assignedPropertyId = null,
      assignedPropertyUnit = null,
      leaseStartDate = null,
      leaseEndDate = null,
      inviteCode = "TUTS-RES-4128",
      isAppRegistered = false,
      createdAt = "Nov 08, 2026"
    )
  )
}
