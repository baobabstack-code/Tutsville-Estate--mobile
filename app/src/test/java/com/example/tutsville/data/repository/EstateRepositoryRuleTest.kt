package com.example.tutsville.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.CasePriority
import com.example.tutsville.data.model.CaseStatus
import com.example.tutsville.data.model.ContractType
import com.example.tutsville.data.model.EstateDateUtils
import com.example.tutsville.data.model.LeaseContractStatus
import com.example.tutsville.data.model.NoticeRecordType
import com.example.tutsville.data.model.NoticeTriggerEvent
import com.example.tutsville.data.model.PropertyArchitecture
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.data.repository.EstateFirestoreMappers.buildSlotLockId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
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
        firestore.collection(collection).document(docId).get().await()
      } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
          delay(80)
          continue
        }
        throw e
      }
      if (snap.exists() && condition(snap)) return@withTimeout snap
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
  fun managerAndTenantWorkflow_unitAssignmentAutoLease_andTenantDigitalSignature(): Unit = runBlocking {
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
        pavilionWing = "East Pavilion",
        architecture = PropertyArchitecture(3, 2.5f, 1850, "Level 3", "Garden"),
        parkingSpots = listOf("Bay-$suffix"),
        amenities = listOf("Terrace"),
        monthlyRental = "$5,400 / mo"
      )
      propId = property.id
      awaitDocCondition("properties", propId) { it.getString("unitNumber") == "Villa $suffix" }

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
        it.getString("leaseStatus") == LeaseContractStatus.PENDING_SIGNATURE.name &&
          it.getString("tenantEmail") == tenantEmail
      }
      awaitDocCondition("properties", propId) {
        it.getString("status") == "OCCUPIED" && it.getString("tenantEmail") == tenantEmail
      }
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
      assertEquals(LeaseContractStatus.PENDING_SIGNATURE, syncedLease.leaseStatus)
      assertFalse(syncedLease.isSignedByTenant)

      // Tenant claims invite and digitally signs their lease contract
      tenantRepo.claimTenantInvite(tenantEmail)
      awaitDocCondition("tenants", tenantId) { it.getBoolean("isAppRegistered") == true }

      tenantRepo.signTenantContract(leaseId, "Alice $suffix", tenantEmail)
      awaitDocCondition("contracts", leaseId) {
        it.getString("leaseStatus") == LeaseContractStatus.ACTIVE.name &&
          it.getBoolean("isSignedByTenant") == true &&
          it.getString("signedByEmail") == tenantEmail
      }

      // Cross-tenant unfiltered queries must still fail
      assertPermissionDenied {
        firestore.collection("contracts").get().await()
      }
    } finally {
      tenantRepo.detachAllListeners()
    }
  }

  @Test
  fun caseLifecycle_tenantEditsOpenCase_managerResolves_tenantReviewsVendor(): Unit = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val tenantEmail = "case.tenant.$suffix@test.com"

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
        leadTech = "Chris"
      )
      vendorId = vendor.id
      awaitDocCondition("vendors", vendorId) { it.getString("name") == "Vendor $suffix" }
    } finally {
      managerRepo.detachAllListeners()
      auth.signOut()
    }

    // Tenant creates & edits OPEN case
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
        tenantEmail = tenantEmail
      )
      caseId = createdCase.id
      awaitDocCondition("cases", caseId) { it.getString("status") == CaseStatus.OPEN.name }

      tenantRepo.updateCase(
        caseId = caseId,
        title = "Sink Burst $suffix",
        description = "Water pooling under cabinet",
        location = "Unit $suffix",
        category = CaseCategory.PLUMBING,
        priority = CasePriority.EMERGENCY
      )
      awaitDocCondition("cases", caseId) {
        it.getString("title") == "Sink Burst $suffix" &&
          it.getString("priority") == CasePriority.EMERGENCY.name
      }
    } finally {
      tenantRepo.detachAllListeners()
      auth.signOut()
    }

    // Manager dispatches vendor & resolves case
    signInTestUser(EstateRepository.BOOTSTRAP_MANAGER_EMAIL, verifyEmail = true)
    val managerRepo2 = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { managerRepo2.isCurrentUserAdmin.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        managerRepo2.vendors.first { list -> list.any { it.id == vendorId } }
      }
      managerRepo2.assignVendorToCase(caseId, vendorId)
      awaitDocCondition("cases", caseId) { it.getString("status") == CaseStatus.ASSIGNED.name }

      managerRepo2.updateCaseStatus(caseId, CaseStatus.RESOLVED)
      awaitDocCondition("cases", caseId) { it.getString("status") == CaseStatus.RESOLVED.name }
    } finally {
      managerRepo2.detachAllListeners()
      auth.signOut()
    }

    // Tenant submits service review and vendor rating persists
    signInTestUser(tenantEmail, verifyEmail = true)
    val tenantRepo2 = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { tenantRepo2.isRoleResolved.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        tenantRepo2.cases.first { list -> list.any { it.id == caseId && it.status == CaseStatus.RESOLVED } }
      }
      withTimeout(FLOW_TIMEOUT_MS) {
        tenantRepo2.vendors.first { list -> list.any { it.id == vendorId } }
      }

      tenantRepo2.submitCaseReview(caseId, 5, listOf("Fast", "Clean"), "Great repair")
      awaitDocCondition("cases", caseId) { it.getLong("rating")?.toInt() == 5 }
      awaitDocCondition("vendors", vendorId) {
        (it.getDouble("rating") ?: 0.0) >= 4.9 && (it.getLong("reviewCount")?.toInt() ?: 0) >= 1
      }
    } finally {
      tenantRepo2.detachAllListeners()
    }
  }

  @Test
  fun bookingSlotLock_blocksDoubleBookingAcrossUsers(): Unit = runBlocking {
    val suffix = UUID.randomUUID().toString().take(8)
    val slot = "09:00 AM - 11:00 AM ($suffix)"
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
        allowedSlots = listOf(slot),
        rules = "Advance pass required"
      )
      facilityId = facility.id
      awaitDocCondition("facilities", facilityId) { it.getString("name") == "Spa $suffix" }
    } finally {
      managerRepo.detachAllListeners()
      auth.signOut()
    }

    // Tenant A books slot atomically
    val tenantAEmail = "tenanta.$suffix@test.com"
    signInTestUser(tenantAEmail, verifyEmail = true)
    val tenantARepo = EstateRepository(databaseId, firestore, auth)
    val lockId = buildSlotLockId(facilityId, dateKey, slot)
    val bookingAId: String
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { tenantARepo.isRoleResolved.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        tenantARepo.facilities.first { list -> list.any { it.id == facilityId } }
      }
      val bookingA = tenantARepo.bookFacility(
        facilityId = facilityId,
        date = dateDisplay,
        timeSlot = slot,
        residentName = "Tenant A",
        unit = "Unit 101",
        guestCount = 2,
        notes = "Morning spa"
      )
      bookingAId = bookingA.id
      awaitDocCondition("bookings", bookingAId) { it.getString("facilityId") == facilityId }
      awaitDocCondition("slot_locks", lockId) { it.getString("bookingId") == bookingAId }
    } finally {
      tenantARepo.detachAllListeners()
      auth.signOut()
    }

    // Tenant B sees slot lock and is blocked both locally and by Firestore rules
    val tenantBEmail = "tenantb.$suffix@test.com"
    val tenantBUid = signInTestUser(tenantBEmail, verifyEmail = true)
    val tenantBRepo = EstateRepository(databaseId, firestore, auth)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) { tenantBRepo.isRoleResolved.first { it } }
      withTimeout(FLOW_TIMEOUT_MS) {
        tenantBRepo.slotLocks.first { list -> list.any { it.id == lockId } }
      }
      assertTrue(tenantBRepo.isSlotBooked(facilityId, dateDisplay, slot))

      var threwLocal = false
      try {
        tenantBRepo.bookFacility(facilityId, dateDisplay, slot, "Tenant B", "Unit 102", 1, "")
      } catch (_: IllegalStateException) {
        threwLocal = true
      }
      assertTrue("Expected local double-booking guard to throw IllegalStateException", threwLocal)

      // Even direct Firestore batch bypassing UI guard must fail with PERMISSION_DENIED
      assertPermissionDenied {
        val batch = firestore.batch()
        val bkRef = firestore.collection("bookings").document("BK-ROGUE-$suffix")
        val lockRef = firestore.collection("slot_locks").document(lockId)
        batch.set(
          bkRef,
          mapOf(
            "userId" to tenantBUid,
            "facilityId" to facilityId,
            "facilityName" to "Spa $suffix",
            "date" to dateDisplay,
            "timeSlot" to slot,
            "guestCount" to 1,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
          )
        )
        batch.set(
          lockRef,
          mapOf(
            "facilityId" to facilityId,
            "dateKey" to dateKey,
            "timeSlot" to slot,
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
    val slot = "10:00 AM - 12:00 PM ($suffix)"
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

    // 2. Verified Tenant triggers Case + Amenity Booking lifecycle notifications and syncs read state
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
        notes = "Study"
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
        while (firestore.collection("reminders").document(advisoryId).get().await().exists()) {
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
