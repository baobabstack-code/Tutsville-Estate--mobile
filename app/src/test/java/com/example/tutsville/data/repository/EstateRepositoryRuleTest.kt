package com.example.tutsville.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.tutsville.data.model.BookingApprovalStatus
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.CasePriority
import com.example.tutsville.data.model.CaseStatus
import com.example.tutsville.data.model.ContractDocumentMeta
import com.example.tutsville.data.model.ContractType
import com.example.tutsville.data.model.EstateDateUtils
import com.example.tutsville.data.model.LeaseContractStatus
import com.example.tutsville.data.model.NoticeRecordType
import com.example.tutsville.data.model.NoticeTriggerEvent
import com.example.tutsville.data.model.PropertyArchitecture
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.data.repository.EstateFirestoreMappers.buildHourlySlotLockId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class EstateRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private suspend fun awaitDocCondition(
    collection: String,
    docId: String,
    condition: (DocumentSnapshot) -> Boolean
  ): DocumentSnapshot = withTimeout(DEFAULT_TIMEOUT_MS) {
    while (true) {
      val snap = try {
        firestore.collection(collection).document(docId).get(Source.SERVER).await()
      } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
          e.code == FirebaseFirestoreException.Code.UNAVAILABLE
        ) {
          delay(80)
          continue
        }
        throw e
      }
      if (snap.exists() && !snap.metadata.hasPendingWrites() && condition(snap)) return@withTimeout snap
      delay(80)
    }
    @Suppress("UNREACHABLE_CODE")
    error("Unreachable")
  }

  private suspend fun assertPermissionDenied(block: suspend () -> Unit) {
    var thrown: Throwable? = null
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { block() }
    } catch (t: Throwable) {
      thrown = t
    }
    val firestoreException = thrown?.unwrapFirestoreException()
    assertNotNull("Expected FirebaseFirestoreException(PERMISSION_DENIED), got $thrown", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException!!.code)
  }

  @Test
  fun unauthenticatedUser_failsProtectedQueriesAndRepositoryStaysLocked(): Unit = runBlocking {
    auth.signOut()
    val repo = EstateRepository(databaseId, firestore, auth)
    try {
      assertFalse(repo.isCurrentUserAdmin.value)
      assertTrue(repo.tenants.value.isEmpty())

      assertPermissionDenied {
        firestore.collection("tenants").get().await()
      }
      assertPermissionDenied {
        firestore.collection("contracts").get().await()
      }
      assertPermissionDenied {
        firestore.collection("cases").get().await()
      }
    } finally {
      repo.detachAllListeners()
    }
  }

  @Test
  fun nonBootstrapVerifiedUser_cannotBootstrapAdminAndCannotListPii(): Unit = runBlocking {
    val strangerUid = signInTestUser("stranger.${UUID.randomUUID().toString().take(8)}@test.com", verifyEmail = true)
    val repo = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { repo.isRoleResolved.first { it } }
      assertFalse(repo.isCurrentUserAdmin.value)

      assertPermissionDenied {
        firestore.collection("admins").document(strangerUid).set(
          mapOf(
            "uid" to strangerUid,
            "email" to "stranger@test.com",
            "role" to "ADMIN"
          )
        ).await()
      }
      assertPermissionDenied {
        firestore.collection("tenants").get().await()
      }
      assertPermissionDenied {
        firestore.collection("properties").get().await()
      }
      assertPermissionDenied {
        firestore.collection("contracts").get().await()
      }
    } finally {
      repo.detachAllListeners()
    }
  }

  @Test
  fun coManagerInvite_allowsVerifiedInviteeClaimAndEstateConfigUpdates(): Unit = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val coMgrEmail = "comgr.$suffix@test.com"

    signInTestUser(EstateRepository.BOOTSTRAP_MANAGER_EMAIL, verifyEmail = true)
    val managerRepo = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { managerRepo.isCurrentUserAdmin.first { it } }

      managerRepo.inviteManager(coMgrEmail, "Co-Manager $suffix")
      awaitDocCondition("manager_invites", coMgrEmail) {
        it.getString("status") == "PENDING"
      }

      managerRepo.updateConfigOptions(
        floorElevations = listOf("1 Story", "2 Story", "Custom Loft"),
        contractorDisciplines = listOf("Plumbing & Drains", "Gate Systems")
      )
      awaitDocCondition("estate_config", "options") { snap ->
        @Suppress("UNCHECKED_CAST")
        val floors = snap.get("floorElevations") as? List<String> ?: emptyList()
        floors.contains("Custom Loft")
      }
    } finally {
      managerRepo.detachAllListeners()
      auth.signOut()
    }

    // Verified invited co-manager claims invite automatically via handleAuthState
    val coMgrUid = signInTestUser(coMgrEmail, verifyEmail = true)
    val coMgrRepo = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { coMgrRepo.isCurrentUserAdmin.first { it } }
      awaitDocCondition("admins", coMgrUid) { it.getString("email") == coMgrEmail }
      awaitDocCondition("manager_invites", coMgrEmail) { it.getString("status") == "CLAIMED" }
      assertTrue(coMgrRepo.estateConfigOptions.value.floorElevations.contains("Custom Loft"))
    } finally {
      coMgrRepo.detachAllListeners()
    }
  }

  @Test
  fun managerAndTenantWorkflow_unitAssignmentAutoActiveLease_archiveAndAttachments(): Unit = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val tenantEmail = "alice.$suffix@test.com"
    val today = EstateDateUtils.todayDisplay()
    val nextYear = EstateDateUtils.addMonthsDisplay(today, 12)

    signInTestUser(EstateRepository.BOOTSTRAP_MANAGER_EMAIL, verifyEmail = true)
    val managerRepo = EstateRepository(databaseId, firestore, auth)
    val propId: String
    val tenantId: String
    val leaseId: String
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { managerRepo.isCurrentUserAdmin.first { it } }

      val property = managerRepo.listProperty(
        unitNumber = "Villa $suffix",
        architecture = PropertyArchitecture(3, 2.5f, 180, "2 Story", "Garden", sqMeters = 180),
        amenities = listOf("Terrace"),
        monthlyRental = "$5,400 / mo"
      )
      propId = property.id
      awaitDocCondition("properties", propId) {
        it.getString("unitNumber") == "Villa $suffix" && it.getLong("sqMeters")?.toInt() == 180
      }

      val tenant = managerRepo.createTenant(
        fullName = "Alice $suffix",
        email = tenantEmail,
        phone = "+1 555 200 1000",
        emergencyContactName = "Bob $suffix",
        emergencyContactPhone = "+1 555 200 2000"
      )
      tenantId = tenant.id
      awaitDocCondition("tenants", tenantId) { it.getString("email") == tenantEmail }

      managerRepo.assignTenantToPropertyById(
        tenantId = tenantId,
        propertyId = propId,
        leaseStartDate = today,
        leaseEndDate = nextYear,
        managerName = "Estate Manager",
        managerEmail = EstateRepository.BOOTSTRAP_MANAGER_EMAIL
      )

      val autoLease = managerRepo.contracts.value.firstOrNull {
        it.type == ContractType.TENANT_LEASE && it.tenantEmail == tenantEmail
      }
      assertNotNull("Expected auto-created TENANT_LEASE contract", autoLease)
      leaseId = autoLease!!.id

      awaitDocCondition("contracts", leaseId) {
        it.getString("leaseStatus") == LeaseContractStatus.ACTIVE.name &&
          it.getString("tenantEmail") == tenantEmail &&
          it.getBoolean("isArchived") == false
      }
      awaitDocCondition("properties", propId) {
        it.getString("status") == "OCCUPIED" && it.getString("tenantEmail") == tenantEmail
      }

      // Attach document metadata and test Archive toggle
      managerRepo.attachContractDocument(
        contractId = leaseId,
        isCodeOfConduct = false,
        docMeta = ContractDocumentMeta("lease_$suffix.pdf", "content://docs/lease_$suffix.pdf", "application/pdf", today)
      )
      awaitDocCondition("contracts", leaseId) { snap ->
        @Suppress("UNCHECKED_CAST")
        val docMap = snap.get("signedContractDoc") as? Map<String, Any?>
        docMap?.get("fileName") == "lease_$suffix.pdf"
      }

      managerRepo.setContractArchived(leaseId, true)
      awaitDocCondition("contracts", leaseId) { it.getBoolean("isArchived") == true }
      managerRepo.setContractArchived(leaseId, false)
      awaitDocCondition("contracts", leaseId) { it.getBoolean("isArchived") == false }
    } finally {
      managerRepo.detachAllListeners()
      auth.signOut()
    }

    // Switch to verified Tenant account
    signInTestUser(tenantEmail, verifyEmail = true)
    val tenantRepo = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { tenantRepo.isRoleResolved.first { it } }
      assertFalse(tenantRepo.isCurrentUserAdmin.value)

      val syncedLease = withTimeout(FLOW_TIMEOUT_MS) {
        tenantRepo.contracts.first { list -> list.any { it.id == leaseId } }
      }.first { it.id == leaseId }
      assertEquals(LeaseContractStatus.ACTIVE, syncedLease.leaseStatus)
      assertEquals("lease_$suffix.pdf", syncedLease.signedContractDoc?.fileName)

      // Tenant claims invite
      tenantRepo.claimTenantInvite(tenantEmail)
      awaitDocCondition("tenants", tenantId) { it.getBoolean("isAppRegistered") == true }

      // Tenant cannot tamper with contract or archive it, nor list all contracts
      assertPermissionDenied {
        firestore.collection("contracts").document(leaseId).update("isArchived", true).await()
      }
      assertPermissionDenied {
        firestore.collection("contracts").get().await()
      }
    } finally {
      tenantRepo.detachAllListeners()
    }
  }

  @Test
  fun caseLifecycle_preferredDate_tenantEditsOpenCase_managerNotesAndResolves(): Unit = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val tenantEmail = "case.tenant.$suffix@test.com"
    val preferred = EstateDateUtils.offsetDisplay(3)

    signInTestUser(EstateRepository.BOOTSTRAP_MANAGER_EMAIL, verifyEmail = true)
    val managerRepo = EstateRepository(databaseId, firestore, auth)
    val vendorId: String
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { managerRepo.isCurrentUserAdmin.first { it } }
      val vendor = managerRepo.createVendor(
        name = "Vendor $suffix",
        specialty = CaseCategory.PLUMBING,
        phone = "+1 555 900 1000",
        email = "v.$suffix@test.com",
        leadTech = "Chris",
        discipline = "Plumbing & Drains"
      )
      vendorId = vendor.id
      awaitDocCondition("vendors", vendorId) { it.getString("name") == "Vendor $suffix" }
    } finally {
      managerRepo.detachAllListeners()
      auth.signOut()
    }

    // Tenant creates & edits OPEN case with preferredDate
    signInTestUser(tenantEmail, verifyEmail = true)
    val tenantRepo = EstateRepository(databaseId, firestore, auth)
    val caseId: String
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { tenantRepo.isRoleResolved.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        tenantRepo.vendors.first { list -> list.any { it.id == vendorId } }
      }

      val createdCase = tenantRepo.reportCase(
        title = "Sink Drip $suffix",
        description = "Initial leak",
        category = CaseCategory.PLUMBING,
        priority = CasePriority.LOW,
        location = "Unit $suffix",
        reportedBy = "Resident $suffix",
        tenantEmail = tenantEmail,
        preferredDate = preferred
      )
      caseId = createdCase.id
      awaitDocCondition("cases", caseId) {
        it.getString("status") == CaseStatus.OPEN.name && it.getString("preferredDate") == preferred
      }

      tenantRepo.updateCase(
        caseId = caseId,
        title = "Sink Burst $suffix",
        description = "Water pooling under cabinet",
        location = "Unit $suffix",
        category = CaseCategory.PLUMBING,
        priority = CasePriority.EMERGENCY,
        preferredDate = preferred
      )
      awaitDocCondition("cases", caseId) {
        it.getString("title") == "Sink Burst $suffix" &&
          it.getString("priority") == CasePriority.EMERGENCY.name
      }
    } finally {
      tenantRepo.detachAllListeners()
      auth.signOut()
    }

    // Manager adds note (preserving resident description), dispatches contractor & resolves case
    signInTestUser(EstateRepository.BOOTSTRAP_MANAGER_EMAIL, verifyEmail = true)
    val managerRepo2 = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { managerRepo2.isCurrentUserAdmin.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        managerRepo2.vendors.first { list -> list.any { it.id == vendorId } }
      }
      withTimeout(FLOW_TIMEOUT_MS) {
        managerRepo2.cases.first { list -> list.any { it.id == caseId } }
      }

      // Verify rules forbid Manager from rewriting resident's original description
      assertPermissionDenied {
        firestore.collection("cases").document(caseId).update("description", "Altered text").await()
      }

      managerRepo2.appendCaseNote(caseId, "Shutoff valve checked, contractor en route")
      awaitDocCondition("cases", caseId) { snap ->
        @Suppress("UNCHECKED_CAST")
        val notes = snap.get("notes") as? List<Map<String, Any?>> ?: emptyList()
        notes.any { it["text"] == "Shutoff valve checked, contractor en route" }
      }

      managerRepo2.assignVendorToCase(caseId, vendorId)
      awaitDocCondition("cases", caseId) { it.getString("status") == CaseStatus.ASSIGNED.name }
      awaitDocCondition("vendors", vendorId) { (it.getLong("activeJobsCount") ?: 0L) >= 1L }

      managerRepo2.updateCaseStatus(caseId, CaseStatus.RESOLVED)
      awaitDocCondition("cases", caseId) { it.getString("status") == CaseStatus.RESOLVED.name }
    } finally {
      managerRepo2.detachAllListeners()
    }
  }

  @Test
  fun bookingApprovalAndHourlySlotLock_preventsOverlappingBookingsAcrossUsers(): Unit = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val slotA = "09:00 AM - 11:00 AM"
    val slotOverlap = "10:00 AM - 12:00 PM"
    val dateKey = "2026-12-20"
    val dateDisplay = EstateDateUtils.displayFromKey(dateKey)

    signInTestUser(EstateRepository.BOOTSTRAP_MANAGER_EMAIL, verifyEmail = true)
    val managerRepo = EstateRepository(databaseId, firestore, auth)
    val facilityId: String
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { managerRepo.isCurrentUserAdmin.first { it } }
      val facility = managerRepo.createFacility(
        name = "Spa $suffix",
        tag = "Wellness",
        capacity = 8,
        location = "Sky Deck",
        description = "Private spa",
        amenities = listOf("Sauna"),
        allowedSlots = listOf(slotA, slotOverlap),
        rules = "Advance pass required"
      )
      facilityId = facility.id
      awaitDocCondition("facilities", facilityId) { it.getString("name") == "Spa $suffix" }
    } finally {
      managerRepo.detachAllListeners()
      auth.signOut()
    }

    // Tenant A submits PENDING booking request (does NOT lock hours until approved)
    val tenantAEmail = "tenanta.$suffix@test.com"
    signInTestUser(tenantAEmail, verifyEmail = true)
    val tenantARepo = EstateRepository(databaseId, firestore, auth)
    val bookingAId: String
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { tenantARepo.isRoleResolved.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        tenantARepo.facilities.first { list -> list.any { it.id == facilityId } }
      }
      val bookingA = tenantARepo.bookFacility(
        facilityId = facilityId,
        date = dateDisplay,
        timeSlot = slotA,
        residentName = "Tenant A",
        unit = "Unit 101",
        guestCount = 2,
        notes = "Morning spa",
        startHour = 9,
        durationHours = 2
      )
      bookingAId = bookingA.id
      awaitDocCondition("bookings", bookingAId) {
        it.getString("facilityId") == facilityId &&
          it.getString("approvalStatus") == BookingApprovalStatus.PENDING.name
      }
    } finally {
      tenantARepo.detachAllListeners()
      auth.signOut()
    }

    // Manager approves Booking A -> atomically locks H09 and H10
    val lockH09 = buildHourlySlotLockId(facilityId, dateKey, 9)
    val lockH10 = buildHourlySlotLockId(facilityId, dateKey, 10)
    signInTestUser(EstateRepository.BOOTSTRAP_MANAGER_EMAIL, verifyEmail = true)
    val managerRepo2 = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { managerRepo2.isCurrentUserAdmin.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        managerRepo2.bookings.first { list -> list.any { it.id == bookingAId } }
      }
      managerRepo2.approveBooking(bookingAId)
      awaitDocCondition("bookings", bookingAId) {
        it.getString("approvalStatus") == BookingApprovalStatus.APPROVED.name
      }
      awaitDocCondition("slot_locks", lockH09) { it.getString("bookingId") == bookingAId }
      awaitDocCondition("slot_locks", lockH10) { it.getString("bookingId") == bookingAId }
    } finally {
      managerRepo2.detachAllListeners()
      auth.signOut()
    }

    // Tenant B sees hourly lock and is blocked both locally and by Firestore rules on overlapping H10
    val tenantBEmail = "tenantb.$suffix@test.com"
    val tenantBUid = signInTestUser(tenantBEmail, verifyEmail = true)
    val tenantBRepo = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { tenantBRepo.isRoleResolved.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        tenantBRepo.slotLocks.first { list -> list.any { it.id == lockH10 } }
      }
      assertTrue(tenantBRepo.isHourlyWindowOccupied(facilityId, dateDisplay, 10, 2))

      var threwLocal = false
      try {
        tenantBRepo.bookFacility(
          facilityId = facilityId,
          date = dateDisplay,
          timeSlot = slotOverlap,
          residentName = "Tenant B",
          unit = "Unit 102",
          guestCount = 1,
          notes = "",
          startHour = 10,
          durationHours = 2
        )
      } catch (_: IllegalStateException) {
        threwLocal = true
      }
      assertTrue("Expected local overlap guard to throw IllegalStateException", threwLocal)

      // Direct Firestore batch trying to steal H10 lock must fail with PERMISSION_DENIED
      assertPermissionDenied {
        val batch = firestore.batch()
        val bkRef = firestore.collection("bookings").document("BK-ROGUE-$suffix")
        val lockRef = firestore.collection("slot_locks").document(lockH10)
        batch.set(
          bkRef,
          mapOf(
            "userId" to tenantBUid,
            "facilityId" to facilityId,
            "facilityName" to "Spa $suffix",
            "date" to dateDisplay,
            "timeSlot" to slotOverlap,
            "startHour" to 10,
            "durationHours" to 2,
            "guestCount" to 1,
            "approvalStatus" to "PENDING",
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
          )
        )
        batch.set(
          lockRef,
          mapOf(
            "facilityId" to facilityId,
            "dateKey" to dateKey,
            "timeSlot" to "10:00 AM - 11:00 AM",
            "bookingId" to "BK-ROGUE-$suffix",
            "bookedByUid" to tenantBUid,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
          )
        )
        batch.commit().await()
      }
    } finally {
      tenantBRepo.detachAllListeners()
    }
  }

  @Test
  fun attributedNotifications_caseAndBookingLifecycles_readStateAndDeleteIsolation(): Unit = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val aliceEmail = "notice.alice.$suffix@test.com"
    val bobEmail = "notice.bob.$suffix@test.com"
    val slot = "10:00 AM - 12:00 PM"
    val dateDisplay = EstateDateUtils.displayFromKey("2026-12-22")

    // 1. Manager creates a facility and publishes an attributed advisory announcement
    signInTestUser(EstateRepository.BOOTSTRAP_MANAGER_EMAIL, verifyEmail = true)
    val managerRepo = EstateRepository(databaseId, firestore, auth)
    val facilityId: String
    val advisoryId: String
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { managerRepo.isCurrentUserAdmin.first { it } }
      managerRepo.setActorContext("Estate Manager", EstateRepository.BOOTSTRAP_MANAGER_EMAIL)

      val facility = managerRepo.createFacility(
        name = "Lounge $suffix",
        tag = "Social",
        capacity = 12,
        location = "Penthouse",
        description = "Sky lounge",
        amenities = listOf("Espresso"),
        allowedSlots = listOf(slot),
        rules = "Quiet hours after 10 PM"
      )
      facilityId = facility.id
      awaitDocCondition("facilities", facilityId) { it.getString("name") == "Lounge $suffix" }

      val advisory = managerRepo.sendReminder(
        title = "Elevator Maintenance $suffix",
        message = "West elevator offline Tuesday 9 AM - 11 AM.",
        targetAudience = "All Residents & Tenants",
        urgency = "Advisory",
        isBroadcast = true
      )
      advisoryId = advisory.id
      awaitDocCondition("reminders", advisoryId) {
        it.getString("recordType") == NoticeRecordType.ANNOUNCEMENT.name &&
          it.getString("triggerEvent") == NoticeTriggerEvent.MANUAL_ADVISORY.name &&
          it.getString("createdByRole") == UserRole.MANAGER.name
      }
    } finally {
      managerRepo.detachAllListeners()
      auth.signOut()
    }

    // 2. Verified Tenant triggers Case + Amenity Request lifecycle notifications and syncs read state
    val aliceUid = signInTestUser(aliceEmail, verifyEmail = true)
    val aliceRepo = EstateRepository(databaseId, firestore, auth)
    val caseNoticeId: String
    val bookingNoticeId: String
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.isRoleResolved.first { it } }
      aliceRepo.setActorContext("Alice $suffix", aliceEmail)
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.facilities.first { list -> list.any { it.id == facilityId } }
      }
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.reminders.first { list -> list.any { it.id == advisoryId } }
      }

      val createdCase = aliceRepo.reportCase(
        title = "Window Seal $suffix",
        description = "Draft coming through frame",
        category = CaseCategory.PLUMBING,
        priority = CasePriority.MEDIUM,
        location = "Unit $suffix",
        reportedBy = "Alice $suffix",
        tenantEmail = aliceEmail
      )
      val caseNotice = withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.reminders.first { list ->
          list.any { it.triggerEvent == NoticeTriggerEvent.CASE_CREATED && it.relatedEntityId == createdCase.id }
        }.first { it.triggerEvent == NoticeTriggerEvent.CASE_CREATED && it.relatedEntityId == createdCase.id }
      }
      caseNoticeId = caseNotice.id
      awaitDocCondition("reminders", caseNoticeId) {
        it.getString("createdByUid") == aliceUid &&
          it.getString("createdByRole") == UserRole.TENANT.name &&
          it.getString("createdByEmail") == aliceEmail
      }

      val booking = aliceRepo.bookFacility(
        facilityId = facilityId,
        date = dateDisplay,
        timeSlot = slot,
        residentName = "Alice $suffix",
        unit = "Unit $suffix",
        guestCount = 2,
        notes = "Study",
        startHour = 10,
        durationHours = 2
      )
      val bookingNotice = withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.reminders.first { list ->
          list.any { it.triggerEvent == NoticeTriggerEvent.AMENITY_BOOKED && it.relatedEntityId == booking.id }
        }.first { it.triggerEvent == NoticeTriggerEvent.AMENITY_BOOKED && it.relatedEntityId == booking.id }
      }
      bookingNoticeId = bookingNotice.id
      awaitDocCondition("reminders", bookingNoticeId) {
        it.getString("recordType") == NoticeRecordType.ACTIVITY.name &&
          it.getString("triggerEvent") == NoticeTriggerEvent.AMENITY_BOOKED.name
      }

      // Sync read state into /user_notice_states/{aliceUid}
      aliceRepo.markNoticeAsRead(advisoryId)
      awaitDocCondition("user_notice_states", aliceUid) { snap ->
        @Suppress("UNCHECKED_CAST")
        val ids = snap.get("readNoticeIds") as? List<String> ?: emptyList()
        ids.contains(advisoryId)
      }

      // Tenant cannot delete notifications
      assertPermissionDenied {
        firestore.collection("reminders").document(advisoryId).delete().await()
      }
    } finally {
      aliceRepo.detachAllListeners()
      auth.signOut()
    }

    // 3. Another resident cannot list unfiltered reminders or read Alice's case notification or read state
    signInTestUser(bobEmail, verifyEmail = true)
    val bobRepo = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.isRoleResolved.first { it } }
      assertPermissionDenied {
        firestore.collection("reminders").get().await()
      }
      assertPermissionDenied {
        firestore.collection("reminders").document(caseNoticeId).get().await()
      }
      assertPermissionDenied {
        firestore.collection("user_notice_states").document(aliceUid).get().await()
      }
    } finally {
      bobRepo.detachAllListeners()
      auth.signOut()
    }

    // 4. Manager deletes notification via repository cleanly
    signInTestUser(EstateRepository.BOOTSTRAP_MANAGER_EMAIL, verifyEmail = true)
    val managerRepo2 = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { managerRepo2.isCurrentUserAdmin.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        managerRepo2.reminders.first { list -> list.any { it.id == advisoryId } }
      }
      managerRepo2.deleteReminder(advisoryId)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        while (firestore.collection("reminders").document(advisoryId).get(Source.SERVER).await().exists()) {
          delay(80)
        }
      }
    } finally {
      managerRepo2.detachAllListeners()
    }
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 6000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
