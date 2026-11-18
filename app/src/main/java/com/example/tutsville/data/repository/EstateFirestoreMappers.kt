package com.example.tutsville.data.repository

import com.example.tutsville.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Locale

internal object EstateFirestoreMappers {
  private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

  fun buildSlotLockId(facilityId: String, dateKey: String, timeSlot: String): String {
    val sanitizedSlot = timeSlot.trim().replace(Regex("[^a-zA-Z0-9]+"), "_").trim('_')
    val sanitizedDate = dateKey.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_")
    val sanitizedFac = facilityId.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_")
    return "${sanitizedFac}__${sanitizedDate}__${sanitizedSlot}".take(120)
  }

  fun buildHourlySlotLockId(facilityId: String, dateKey: String, hour24: Int): String {
    val sanitizedDate = dateKey.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_")
    val sanitizedFac = facilityId.trim().replace(Regex("[^a-zA-Z0-9_-]+"), "_")
    val paddedHour = String.format(Locale.US, "H%02d", hour24.coerceIn(0, 23))
    return "${sanitizedFac}__${sanitizedDate}__${paddedHour}".take(120)
  }

  // --- Model -> Firestore Map ---

  fun mapCaseNote(n: CaseNote): Map<String, Any> = mapOf(
    "id" to n.id,
    "authorName" to n.authorName,
    "authorEmail" to n.authorEmail,
    "authorRole" to n.authorRole,
    "text" to n.text,
    "createdAt" to n.createdAt,
    "createdAtEpochMs" to n.createdAtEpochMs
  )

  fun mapCaseMovement(m: CaseMovementEntry): Map<String, Any> = mapOf(
    "status" to m.status,
    "note" to m.note,
    "updatedBy" to m.updatedBy,
    "timestamp" to m.timestamp,
    "epochMs" to m.epochMs
  )

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
      "preferredDate" to c.preferredDate?.takeIf { it.isNotBlank() },
      "assignedVendorId" to c.assignedVendorId,
      "assignedVendorName" to c.assignedVendorName,
      "status" to c.status.name,
      "notes" to c.notes.map { mapCaseNote(it) },
      "statusHistory" to c.statusHistory.map { mapCaseMovement(it) },
      "createdAtEpochMs" to c.createdAtEpochMs,
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
    val sqm = p.architecture.effectiveSqMeters
    val base = mutableMapOf<String, Any?>(
      "unitNumber" to p.unitNumber,
      "pavilionWing" to p.pavilionWing,
      "bedrooms" to p.architecture.bedrooms,
      "bathrooms" to p.architecture.bathrooms.toDouble(),
      "sqFt" to sqm,
      "sqMeters" to sqm,
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
      "amenities" to f.amenities.map { it.trim() }.filter { it.isNotEmpty() },
      "allowedSlots" to f.allowedSlots.map { it.trim() }.filter { it.isNotEmpty() },
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
      "startTime" to b.startTime,
      "endTime" to b.endTime,
      "startHour" to b.startHour,
      "durationHours" to b.durationHours,
      "guestCount" to b.guestCount,
      "notes" to b.notes,
      "confirmedTimestamp" to b.confirmedTimestamp,
      "approvalStatus" to b.approvalStatus.name,
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
      "eventCategory" to m.eventCategory.name,
      "scheduledDate" to m.scheduledDate,
      "scheduledTime" to m.scheduledTime,
      "contractorName" to m.contractorName,
      "targetArea" to m.targetArea,
      "targetScope" to m.targetScope.name,
      "targetUnits" to m.targetUnits,
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

  fun mapDocumentMeta(doc: ContractDocumentMeta?): Map<String, Any>? {
    if (doc == null || doc.contentUri.isBlank()) return null
    return mapOf(
      "fileName" to doc.fileName,
      "contentUri" to doc.contentUri,
      "mimeType" to doc.mimeType,
      "attachedAt" to doc.attachedAt
    )
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
      "isArchived" to c.isArchived,
      "signedContractDoc" to mapDocumentMeta(c.signedContractDoc),
      "codeOfConductDoc" to mapDocumentMeta(c.codeOfConductDoc),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  fun mapReminderToFirestore(r: ReminderItem, uid: String): Map<String, Any> {
    val payload = mutableMapOf<String, Any?>(
      "userId" to uid,
      "title" to r.title,
      "message" to r.message,
      "targetAudience" to r.targetAudience,
      "urgency" to r.urgency,
      "timestamp" to r.timestamp,
      "isBroadcast" to r.isBroadcast,
      "recordType" to r.recordType.name,
      "triggerEvent" to r.triggerEvent.name,
      "createdByUid" to r.createdByUid.ifBlank { uid },
      "createdByName" to r.createdByName,
      "createdByEmail" to r.createdByEmail,
      "createdByRole" to (if (r.createdByRole == UserRole.MANAGER) "MANAGER" else "TENANT"),
      "targetEmail" to r.targetEmail?.takeIf { it.isNotBlank() },
      "targetEmails" to r.targetEmails.takeIf { it.isNotEmpty() },
      "targetUnit" to r.targetUnit?.takeIf { it.isNotBlank() },
      "targetUnits" to r.targetUnits.takeIf { it.isNotEmpty() },
      "relatedEntityType" to r.relatedEntityType.name,
      "relatedEntityId" to r.relatedEntityId?.takeIf { it.isNotBlank() },
      "createdAtEpochMs" to r.createdAtEpochMs,
      "createdAt" to FieldValue.serverTimestamp()
    )
    @Suppress("UNCHECKED_CAST")
    return payload.filterValues { it != null } as Map<String, Any>
  }

  fun mapVendorToFirestore(v: VendorItem, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
      "name" to v.name,
      "category" to v.specialty.name,
      "specialty" to v.specialty.name,
      "discipline" to v.effectiveDiscipline,
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

  fun mapConfigToFirestore(cfg: EstateConfigOptions, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
      "floorElevations" to cfg.floorElevations.map { it.trim() }.filter { it.isNotEmpty() },
      "contractorDisciplines" to cfg.contractorDisciplines.map { it.trim() }.filter { it.isNotEmpty() },
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  fun mapManagerInviteToFirestore(invite: ManagerInvite, isCreate: Boolean = true): Map<String, Any> {
    val base = mutableMapOf<String, Any?>(
      "email" to invite.email.trim().lowercase(),
      "displayName" to invite.displayName.trim(),
      "invitedByEmail" to invite.invitedByEmail.trim().lowercase(),
      "invitedDate" to invite.invitedDate,
      "status" to invite.status,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isCreate) {
      base["createdAt"] = FieldValue.serverTimestamp()
    }
    @Suppress("UNCHECKED_CAST")
    return base.filterValues { it != null } as Map<String, Any>
  }

  // --- Firestore Document -> Model ---

  @Suppress("UNCHECKED_CAST")
  fun docToCase(doc: DocumentSnapshot): CaseItem? {
    val id = doc.id
    val title = doc.getString("title") ?: return null
    val description = doc.getString("description") ?: ""
    val catStr = doc.getString("category") ?: CaseCategory.HVAC.name
    val priorityStr = doc.getString("priority") ?: CasePriority.MEDIUM.name
    val location = doc.getString("location") ?: ""
    val reportedBy = doc.getString("reportedBy") ?: "Resident"
    val reportedDate = doc.getString("reportedDate") ?: ""
    val preferredDate = doc.getString("preferredDate")?.takeIf { it.isNotBlank() }
    val statusStr = doc.getString("status") ?: CaseStatus.OPEN.name
    val rating = doc.getLong("rating")?.toInt()

    val category = try { CaseCategory.valueOf(catStr) } catch (_: Exception) { CaseCategory.HVAC }
    val priority = try { CasePriority.valueOf(priorityStr) } catch (_: Exception) { CasePriority.MEDIUM }
    val status = try { CaseStatus.valueOf(statusStr) } catch (_: Exception) { CaseStatus.OPEN }

    val notes = (doc.get("notes") as? List<*>)?.mapNotNull { raw ->
      val m = raw as? Map<String, Any?> ?: return@mapNotNull null
      val text = m["text"] as? String ?: return@mapNotNull null
      CaseNote(
        id = m["id"] as? String ?: "",
        authorName = m["authorName"] as? String ?: "Manager",
        authorEmail = m["authorEmail"] as? String ?: "",
        authorRole = m["authorRole"] as? String ?: "MANAGER",
        text = text,
        createdAt = m["createdAt"] as? String ?: "",
        createdAtEpochMs = (m["createdAtEpochMs"] as? Number)?.toLong() ?: 0L
      )
    } ?: emptyList()

    val statusHistory = (doc.get("statusHistory") as? List<*>)?.mapNotNull { raw ->
      val m = raw as? Map<String, Any?> ?: return@mapNotNull null
      CaseMovementEntry(
        status = m["status"] as? String ?: "",
        note = m["note"] as? String ?: "",
        updatedBy = m["updatedBy"] as? String ?: "",
        timestamp = m["timestamp"] as? String ?: "",
        epochMs = (m["epochMs"] as? Number)?.toLong() ?: 0L
      )
    } ?: emptyList()

    val createdAtEpochMs = doc.getLong("createdAtEpochMs")
      ?: doc.getTimestamp("createdAt")?.toDate()?.time
      ?: 0L

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
      preferredDate = preferredDate,
      assignedVendorId = doc.getString("assignedVendorId"),
      assignedVendorName = doc.getString("assignedVendorName"),
      status = status,
      notes = notes,
      statusHistory = statusHistory,
      createdAtEpochMs = createdAtEpochMs,
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
    val sqMeters = doc.getLong("sqMeters")?.toInt() ?: sqFt
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
      architecture = PropertyArchitecture(bedrooms, bathrooms, sqFt, floorLevel, viewType, sqMeters),
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
    val amenities = (doc.get("amenities") as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } } ?: emptyList()
    val allowedSlots = (doc.get("allowedSlots") as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } } ?: emptyList()
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
    val parsedRange = EstateDateUtils.parseTimeRangeHours(timeSlot)
    val startHour = doc.getLong("startHour")?.toInt() ?: parsedRange?.first ?: 8
    val durationHours = doc.getLong("durationHours")?.toInt()
      ?: parsedRange?.let { (it.last - it.first + 1).coerceAtLeast(1) }
      ?: 1
    val startTime = doc.getString("startTime")?.takeIf { it.isNotBlank() }
      ?: EstateDateUtils.formatHourLabel(startHour)
    val endTime = doc.getString("endTime")?.takeIf { it.isNotBlank() }
      ?: EstateDateUtils.formatHourLabel(startHour + durationHours)
    val guestCount = doc.getLong("guestCount")?.toInt() ?: 1
    val notes = doc.getString("notes") ?: ""
    val confirmedTimestamp = doc.getString("confirmedTimestamp") ?: ""
    val approvalStr = doc.getString("approvalStatus")
    val approvalStatus = try {
      if (approvalStr != null) BookingApprovalStatus.valueOf(approvalStr)
      else BookingApprovalStatus.APPROVED
    } catch (_: Exception) {
      BookingApprovalStatus.APPROVED
    }

    return FacilityBooking(
      id = id,
      userId = doc.getString("userId") ?: "",
      facilityId = facilityId,
      facilityName = facilityName,
      residentName = residentName,
      unit = unit,
      date = date,
      timeSlot = timeSlot,
      startTime = startTime,
      endTime = endTime,
      startHour = startHour,
      durationHours = durationHours,
      guestCount = guestCount,
      notes = notes,
      confirmedTimestamp = confirmedTimestamp,
      approvalStatus = approvalStatus
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
    val catStr = doc.getString("eventCategory") ?: CalendarEventCategory.MAINTENANCE.name
    val scheduledDate = doc.getString("scheduledDate") ?: ""
    val scheduledTime = doc.getString("scheduledTime") ?: ""
    val contractorName = doc.getString("contractorName") ?: ""
    val targetArea = doc.getString("targetArea") ?: ""
    val scopeStr = doc.getString("targetScope") ?: EventTargetScope.ALL_RESIDENTS.name
    val targetUnits = (doc.get("targetUnits") as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } } ?: emptyList()
    val intervalMonths = doc.getLong("intervalMonths")?.toInt() ?: 6
    val statusStr = doc.getString("status") ?: MaintenanceStatus.SCHEDULED.name
    val notes = doc.getString("notes") ?: ""

    val discipline = try { MaintenanceDiscipline.valueOf(discStr) } catch (_: Exception) { MaintenanceDiscipline.HVAC }
    val eventCategory = try { CalendarEventCategory.valueOf(catStr) } catch (_: Exception) { CalendarEventCategory.MAINTENANCE }
    val targetScope = try { EventTargetScope.valueOf(scopeStr) } catch (_: Exception) { EventTargetScope.ALL_RESIDENTS }
    val status = try { MaintenanceStatus.valueOf(statusStr) } catch (_: Exception) { MaintenanceStatus.SCHEDULED }

    return MaintenanceEvent(
      id = id,
      title = title,
      discipline = discipline,
      eventCategory = eventCategory,
      scheduledDate = scheduledDate,
      scheduledTime = scheduledTime,
      contractorName = contractorName,
      targetArea = targetArea,
      targetScope = targetScope,
      targetUnits = targetUnits,
      intervalMonths = intervalMonths,
      status = status,
      notes = notes
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun mapToDocumentMeta(raw: Any?): ContractDocumentMeta? {
    val m = raw as? Map<String, Any?> ?: return null
    val uri = m["contentUri"] as? String ?: return null
    if (uri.isBlank()) return null
    return ContractDocumentMeta(
      fileName = m["fileName"] as? String ?: "Document",
      contentUri = uri,
      mimeType = m["mimeType"] as? String ?: "application/pdf",
      attachedAt = m["attachedAt"] as? String ?: ""
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
      when {
        leaseStatusStr == null -> LeaseContractStatus.ACTIVE
        leaseStatusStr == LeaseContractStatus.PENDING_SIGNATURE.name -> LeaseContractStatus.ACTIVE
        else -> LeaseContractStatus.valueOf(leaseStatusStr)
      }
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
      signedAt = doc.getString("signedAt"),
      isArchived = doc.getBoolean("isArchived") ?: false,
      signedContractDoc = mapToDocumentMeta(doc.get("signedContractDoc")),
      codeOfConductDoc = mapToDocumentMeta(doc.get("codeOfConductDoc"))
    )
  }

  fun docToReminder(doc: DocumentSnapshot): ReminderItem? {
    val id = doc.id
    val title = doc.getString("title")?.takeIf { it.isNotBlank() } ?: return null
    val triggerStr = doc.getString("triggerEvent")?.takeIf { it.isNotBlank() } ?: return null
    val createdByUid = doc.getString("createdByUid")?.takeIf { it.isNotBlank() } ?: return null
    val createdByName = doc.getString("createdByName")?.takeIf { it.isNotBlank() } ?: return null
    val createdByRoleStr = doc.getString("createdByRole")?.takeIf { it.isNotBlank() } ?: return null

    val triggerEvent = try {
      NoticeTriggerEvent.valueOf(triggerStr)
    } catch (_: Exception) {
      return null
    }
    val createdByRole = when (createdByRoleStr.uppercase()) {
      "MANAGER", "ADMIN" -> UserRole.MANAGER
      "TENANT", "RESIDENT" -> UserRole.TENANT
      else -> return null
    }
    val recordTypeStr = doc.getString("recordType") ?: NoticeRecordType.ACTIVITY.name
    val recordType = try {
      NoticeRecordType.valueOf(recordTypeStr)
    } catch (_: Exception) {
      NoticeRecordType.ACTIVITY
    }
    val relatedTypeStr = doc.getString("relatedEntityType") ?: NoticeRelatedEntityType.ADVISORY.name
    val relatedEntityType = try {
      NoticeRelatedEntityType.valueOf(relatedTypeStr)
    } catch (_: Exception) {
      NoticeRelatedEntityType.ADVISORY
    }

    val message = doc.getString("message") ?: ""
    val targetAudience = doc.getString("targetAudience") ?: "Estate Management"
    val urgency = doc.getString("urgency") ?: "Notice"
    val timestamp = doc.getString("timestamp") ?: ""
    val isBroadcast = doc.getBoolean("isBroadcast") ?: false
    val createdByEmail = doc.getString("createdByEmail") ?: ""
    val targetEmail = doc.getString("targetEmail")?.takeIf { it.isNotBlank() }
    val targetEmails = (doc.get("targetEmails") as? List<*>)?.mapNotNull { it?.toString()?.trim()?.lowercase()?.takeIf { s -> s.isNotEmpty() } } ?: emptyList()
    val targetUnit = doc.getString("targetUnit")?.takeIf { it.isNotBlank() }
    val targetUnits = (doc.get("targetUnits") as? List<*>)?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } } ?: emptyList()
    val relatedEntityId = doc.getString("relatedEntityId")?.takeIf { it.isNotBlank() }
    val createdAtEpochMs = doc.getLong("createdAtEpochMs")
      ?: doc.getTimestamp("createdAt")?.toDate()?.time
      ?: 0L

    return ReminderItem(
      id = id,
      title = title,
      message = message,
      targetAudience = targetAudience,
      urgency = urgency,
      timestamp = timestamp,
      isBroadcast = isBroadcast,
      recordType = recordType,
      triggerEvent = triggerEvent,
      createdByUid = createdByUid,
      createdByName = createdByName,
      createdByEmail = createdByEmail,
      createdByRole = createdByRole,
      targetEmail = targetEmail,
      targetEmails = targetEmails,
      targetUnit = targetUnit,
      targetUnits = targetUnits,
      relatedEntityType = relatedEntityType,
      relatedEntityId = relatedEntityId,
      createdAtEpochMs = createdAtEpochMs
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
    val discipline = doc.getString("discipline")?.takeIf { it.isNotBlank() } ?: specialty.label

    return VendorItem(
      id = id,
      name = name,
      specialty = specialty,
      discipline = discipline,
      rating = rating,
      reviewCount = reviewCount,
      activeJobsCount = activeJobsCount,
      phone = phone,
      email = email,
      leadTech = leadTech
    )
  }

  fun docToEstateConfig(doc: DocumentSnapshot): EstateConfigOptions {
    val elevations = (doc.get("floorElevations") as? List<*>)
      ?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
      ?.takeIf { it.isNotEmpty() }
      ?: EstateConfigOptions.DEFAULT_FLOOR_ELEVATIONS
    val disciplines = (doc.get("contractorDisciplines") as? List<*>)
      ?.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
      ?.takeIf { it.isNotEmpty() }
      ?: EstateConfigOptions.DEFAULT_CONTRACTOR_DISCIPLINES
    return EstateConfigOptions(
      id = doc.id,
      floorElevations = elevations,
      contractorDisciplines = disciplines
    )
  }

  fun docToManagerInvite(doc: DocumentSnapshot): ManagerInvite? {
    val email = doc.getString("email") ?: return null
    return ManagerInvite(
      id = doc.id,
      email = email,
      displayName = doc.getString("displayName") ?: "",
      invitedByEmail = doc.getString("invitedByEmail") ?: "",
      invitedDate = doc.getString("invitedDate") ?: "",
      status = doc.getString("status") ?: "PENDING"
    )
  }
}
