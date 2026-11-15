package com.example.tutsville.data.repository

import com.example.tutsville.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object EstateFirestoreMappers {
  private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

  fun buildSlotLockId(facilityId: String, dateKey: String, timeSlot: String): String {
    val sanitizedSlot = timeSlot.trim().replace(Regex("[^a-zA-Z0-9]+"), "_").trim('_')
    val sanitizedDate = dateKey.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_")
    val sanitizedFac = facilityId.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_")
    return "${sanitizedFac}__${sanitizedDate}__${sanitizedSlot}".take(120)
  }

  // --- Model -> Firestore Map ---

  fun mapCaseToFirestore(c: CaseItem, uid: String, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
      "userId" to (c.userId.ifBlank { uid }),
      "tenantEmail" to c.tenantEmail.lowercase(),
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
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  fun mapPropertyToFirestore(p: PropertyItem, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
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
      "tenantEmail" to (p.assignedTenant?.email?.lowercase() ?: ""),
      "assignedTenant" to p.assignedTenant?.let { t ->
        mapOf(
          "name" to t.name,
          "email" to t.email.lowercase(),
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
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  fun mapTenantToFirestore(t: TenantProfile, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
      "firstName" to t.firstName,
      "lastName" to t.lastName,
      "fullName" to t.fullName,
      "nationalId" to t.nationalId,
      "address" to t.address,
      "email" to t.email.lowercase(),
      "phone" to t.phone,
      "emergencyContactName" to t.emergencyContactName,
      "emergencyContactPhone" to t.emergencyContactPhone,
      "assignedPropertyId" to t.assignedPropertyId,
      "assignedPropertyUnit" to t.assignedPropertyUnit,
      "leaseStartDate" to t.leaseStartDate,
      "leaseEndDate" to t.leaseEndDate,
      "inviteCode" to t.inviteCode,
      "isAppRegistered" to t.isAppRegistered,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  fun mapFacilityToFirestore(f: Facility, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
      "name" to f.name,
      "tag" to f.tag,
      "capacity" to f.capacity,
      "location" to f.location,
      "description" to f.description,
      "amenities" to f.amenities,
      "allowedSlots" to f.allowedSlots,
      "rules" to f.rules,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  fun mapBookingToFirestore(b: FacilityBooking, uid: String): Map<String, Any> {
    val base = mapOf<String, Any?>(
      "userId" to (b.userId.ifBlank { uid }),
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
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  fun mapSlotLockToFirestore(lock: BookingSlotLock): Map<String, Any> = mapOf(
    "facilityId" to lock.facilityId,
    "dateKey" to lock.dateKey,
    "timeSlot" to lock.timeSlot,
    "bookingId" to lock.bookingId,
    "bookedByUid" to lock.bookedByUid,
    "createdAt" to FieldValue.serverTimestamp()
  )

  fun mapMaintenanceToFirestore(m: MaintenanceEvent, uid: String, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
      "userId" to uid,
      "title" to m.title,
      "discipline" to m.discipline.name,
      "scheduledDate" to m.scheduledDate,
      "scheduledTime" to m.scheduledTime,
      "contractorName" to m.contractorName,
      "targetArea" to m.targetArea,
      "intervalMonths" to m.intervalMonths,
      "status" to m.status.name,
      "notes" to m.notes,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  fun mapContractToFirestore(c: ContractItem, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
      "title" to c.title,
      "counterparty" to c.counterparty,
      "vendorName" to c.counterparty,
      "unit" to c.unit,
      "serviceCategory" to c.type.name,
      "type" to c.type.name,
      "annualValue" to c.annualValue,
      "startDate" to c.startDate,
      "expiryDate" to c.expiryDate,
      "renewalDate" to c.expiryDate,
      "daysRemaining" to c.effectiveDaysRemaining,
      "renewalTerms" to c.renewalTerms,
      "contactPerson" to c.contactPerson,
      "contactEmail" to c.contactEmail,
      "isNotificationSent" to c.isNotificationSent,
      "tenantId" to c.tenantId,
      "tenantEmail" to c.tenantEmail?.lowercase(),
      "propertyId" to c.propertyId,
      "managerName" to c.managerName,
      "managerEmail" to c.managerEmail.lowercase(),
      "leaseStatus" to c.leaseStatus.name,
      "isSignedByTenant" to c.isSignedByTenant,
      "signedByName" to c.signedByName,
      "signedByEmail" to c.signedByEmail?.lowercase(),
      "signedAt" to c.signedAt,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  fun mapReminderToFirestore(r: ReminderItem, uid: String): Map<String, Any> = mapOf(
    "userId" to uid,
    "title" to r.title,
    "message" to r.message,
    "targetAudience" to r.targetAudience,
    "urgency" to r.urgency,
    "timestamp" to r.timestamp,
    "isBroadcast" to r.isBroadcast,
    "createdAt" to FieldValue.serverTimestamp()
  )

  fun mapVendorToFirestore(v: VendorItem, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
      "name" to v.name,
      "category" to v.specialty.name,
      "specialty" to v.specialty.name,
      "phone" to v.phone,
      "rating" to v.rating.toDouble(),
      "activeJobsCount" to v.activeJobsCount,
      "reviewCount" to v.reviewCount,
      "email" to v.email,
      "leadTech" to v.leadTech,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  // --- Firestore Document -> Model ---

  fun docToCase(doc: DocumentSnapshot): CaseItem? {
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

    val category = try { CaseCategory.valueOf(catStr) } catch (_: Exception) { CaseCategory.HVAC }
    val priority = try { CasePriority.valueOf(priorityStr) } catch (_: Exception) { CasePriority.MEDIUM }
    val status = try { CaseStatus.valueOf(statusStr) } catch (_: Exception) { CaseStatus.OPEN }

    return CaseItem(
      id = id,
      userId = doc.getString("userId") ?: "",
      tenantEmail = doc.getString("tenantEmail") ?: "",
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
  fun docToProperty(doc: DocumentSnapshot): PropertyItem? {
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

  fun docToTenant(doc: DocumentSnapshot): TenantProfile? {
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
    val createdAtDisplay = try {
      doc.getTimestamp("createdAt")?.toDate()?.let { dateFormat.format(it) }
    } catch (_: Exception) {
      null
    } ?: doc.getString("createdAt") ?: ""

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
      createdAt = createdAtDisplay
    )
  }

  fun docToFacility(doc: DocumentSnapshot): Facility? {
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

  fun docToBooking(doc: DocumentSnapshot): FacilityBooking? {
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
      userId = doc.getString("userId") ?: "",
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

  fun docToSlotLock(doc: DocumentSnapshot): BookingSlotLock? {
    val id = doc.id
    val facilityId = doc.getString("facilityId") ?: return null
    val dateKey = doc.getString("dateKey") ?: return null
    val timeSlot = doc.getString("timeSlot") ?: return null
    val bookingId = doc.getString("bookingId") ?: ""
    val bookedByUid = doc.getString("bookedByUid") ?: ""
    return BookingSlotLock(
      id = id,
      facilityId = facilityId,
      dateKey = dateKey,
      timeSlot = timeSlot,
      bookingId = bookingId,
      bookedByUid = bookedByUid
    )
  }

  fun docToMaintenance(doc: DocumentSnapshot): MaintenanceEvent? {
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

    val discipline = try { MaintenanceDiscipline.valueOf(discStr) } catch (_: Exception) { MaintenanceDiscipline.HVAC }
    val status = try { MaintenanceStatus.valueOf(statusStr) } catch (_: Exception) { MaintenanceStatus.SCHEDULED }

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

  fun docToContract(doc: DocumentSnapshot): ContractItem? {
    val id = doc.id
    val title = doc.getString("title") ?: return null
    val counterparty = doc.getString("counterparty") ?: doc.getString("vendorName") ?: ""
    val unit = doc.getString("unit") ?: "Estate Wide"
    val typeStr = doc.getString("type") ?: doc.getString("serviceCategory") ?: ContractType.VENDOR_SLA.name
    val annualValue = doc.getString("annualValue") ?: ""
    val startDate = doc.getString("startDate") ?: ""
    val expiryDate = doc.getString("expiryDate") ?: doc.getString("renewalDate") ?: ""
    val storedDays = doc.getLong("daysRemaining")?.toInt() ?: 0
    val daysRemaining = EstateDateUtils.daysUntilDisplay(expiryDate) ?: storedDays
    val renewalTerms = doc.getString("renewalTerms") ?: ""
    val contactPerson = doc.getString("contactPerson") ?: ""
    val contactEmail = doc.getString("contactEmail") ?: ""
    val isNotificationSent = doc.getBoolean("isNotificationSent") ?: false

    val type = try { ContractType.valueOf(typeStr) } catch (_: Exception) { ContractType.VENDOR_SLA }
    val leaseStatusStr = doc.getString("leaseStatus")
    val isSigned = doc.getBoolean("isSignedByTenant") ?: false
    val leaseStatus = try {
      if (leaseStatusStr != null) LeaseContractStatus.valueOf(leaseStatusStr)
      else if (type == ContractType.TENANT_LEASE && !isSigned) LeaseContractStatus.PENDING_SIGNATURE
      else LeaseContractStatus.ACTIVE
    } catch (_: Exception) {
      LeaseContractStatus.ACTIVE
    }

    return ContractItem(
      id = id,
      title = title,
      counterparty = counterparty,
      unit = unit,
      type = type,
      annualValue = annualValue,
      startDate = startDate,
      expiryDate = expiryDate,
      daysRemaining = daysRemaining,
      renewalTerms = renewalTerms,
      contactPerson = contactPerson,
      contactEmail = contactEmail,
      isNotificationSent = isNotificationSent,
      tenantId = doc.getString("tenantId"),
      tenantEmail = doc.getString("tenantEmail"),
      propertyId = doc.getString("propertyId"),
      managerName = doc.getString("managerName") ?: "",
      managerEmail = doc.getString("managerEmail") ?: "",
      leaseStatus = leaseStatus,
      isSignedByTenant = isSigned,
      signedByName = doc.getString("signedByName"),
      signedByEmail = doc.getString("signedByEmail"),
      signedAt = doc.getString("signedAt")
    )
  }

  fun docToReminder(doc: DocumentSnapshot): ReminderItem? {
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

  fun docToVendor(doc: DocumentSnapshot): VendorItem? {
    val id = doc.id
    val name = doc.getString("name") ?: return null
    val specStr = doc.getString("specialty") ?: doc.getString("category") ?: CaseCategory.HVAC.name
    val rating = (doc.getDouble("rating") ?: 0.0).toFloat()
    val reviewCount = doc.getLong("reviewCount")?.toInt() ?: 0
    val activeJobsCount = doc.getLong("activeJobsCount")?.toInt() ?: 0
    val phone = doc.getString("phone") ?: ""
    val email = doc.getString("email") ?: ""
    val leadTech = doc.getString("leadTech") ?: ""

    val specialty = try { CaseCategory.valueOf(specStr) } catch (_: Exception) { CaseCategory.HVAC }

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
}
