package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.*
import com.example.tutsville.ui.components.*
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
  viewModel: EstateViewModel,
  onNavigateToTab: (Int) -> Unit = { viewModel.setSelectedTab(it) },
  modifier: Modifier = Modifier
) {
  val maintenances by viewModel.maintenances.collectAsState()
  val cases by viewModel.cases.collectAsState()
  val facilities by viewModel.facilities.collectAsState()
  val bookings by viewModel.bookings.collectAsState()
  val properties by viewModel.properties.collectAsState()
  val vendors by viewModel.vendors.collectAsState()
  val reminders by viewModel.reminders.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val currentTenant by viewModel.currentTenant.collectAsState()

  // Computed Metrics
  val upcomingMaintenances = remember(maintenances) {
    maintenances.filter { it.status != MaintenanceStatus.COMPLETED }
  }

  val activeCases = remember(cases) {
    cases.filter { it.status != CaseStatus.RESOLVED }
  }

  val emergencyCasesCount = remember(activeCases) {
    activeCases.count { it.priority == CasePriority.EMERGENCY || it.priority == CasePriority.HIGH }
  }

  // Resident Specific Filtered Data
  val residentUnit = currentTenant?.assignedPropertyUnit
  val myBookings = remember(bookings, currentTenant) {
    bookings.filter { b ->
      val tenantName = currentTenant?.fullName ?: ""
      val unit = residentUnit ?: ""
      (tenantName.isNotBlank() && b.residentName.equals(tenantName, ignoreCase = true)) ||
      (unit.isNotBlank() && b.unit.equals(unit, ignoreCase = true))
    }
  }

  val myCases = remember(cases, currentTenant) {
    cases.filter { c ->
      (residentUnit != null && c.location.contains(residentUnit, ignoreCase = true)) ||
      c.reportedBy.equals(currentTenant?.fullName, ignoreCase = true)
    }
  }

  if (isLoading && properties.isEmpty() && cases.isEmpty()) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(TutsvilleBackground),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Connecting to Tutsville Cloud Database...",
          style = MaterialTheme.typography.bodyMedium.copy(color = TutsvillePrimary, fontWeight = FontWeight.SemiBold)
        )
      }
    }
    return
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("dashboard_lazy_column"),
      contentPadding = PaddingValues(bottom = 96.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(6.dp))
      }

      // ==========================================
      // RESIDENT PORTAL VIEW (TENANT ROLE)
      // ==========================================
      if (currentRole == UserRole.TENANT) {
        // 1. Personalized Resident Welcome & Residence Card
        item {
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = Color(0xFF101B14),
              border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.7f)),
              shadowElevation = 4.dp,
              modifier = Modifier.fillMaxWidth().testTag("resident_hero_card")
            ) {
              Column(modifier = Modifier.padding(18.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                      shape = CircleShape,
                      color = GoldAccent.copy(alpha = 0.2f),
                      modifier = Modifier.size(42.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.HomeWork, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
                      }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(
                        text = "Welcome Home",
                        style = MaterialTheme.typography.labelSmall.copy(color = GoldAccent, letterSpacing = 1.sp)
                      )
                      Text(
                        text = currentTenant?.fullName ?: "Resident",
                        style = MaterialTheme.typography.titleLarge.copy(
                          fontWeight = FontWeight.Bold,
                          color = Color.White,
                          fontFamily = FontFamily.Serif
                        )
                      )
                    }
                  }

                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GoldAccent.copy(alpha = 0.2f),
                    border = BorderStroke(0.8.dp, GoldAccent)
                  ) {
                    Text(
                      text = "RESIDENT PASS",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                      ),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = GoldAccent.copy(alpha = 0.25f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(
                      text = "ASSIGNED RESIDENCE",
                      style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB0B9B2), fontSize = 10.sp)
                    )
                    Text(
                      text = residentUnit ?: "Tutsville Private Residence",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "PARKING BAY",
                      style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB0B9B2), fontSize = 10.sp)
                    )
                    Text(
                      text = "Bay P-04 (EV Ready)",
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = GoldAccent)
                    )
                  }
                }
              }
            }
          }
        }

        // 2. Resident Quick Action Buttons (Generous 48dp Touch Targets, Zero Overlap)
        item {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
              text = "Resident Concierge & Services",
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary,
                letterSpacing = 0.04.sp
              )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              QuickActionButton(
                label = "Book Amenity",
                icon = Icons.Filled.Apartment,
                tag = "resident_quick_book_amenity",
                modifier = Modifier.weight(1f),
                onClick = {
                  val firstFac = facilities.firstOrNull()
                  viewModel.activeBookFacility.value = firstFac
                }
              )
              QuickActionButton(
                label = "Report Repair",
                icon = Icons.Filled.Build,
                tag = "resident_quick_report_issue",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.activeReportCaseDialog.value = true }
              )
              QuickActionButton(
                label = "All Facilities",
                icon = Icons.Filled.Pool,
                tag = "resident_quick_view_passes",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(2) }
              )
            }
          }
        }

        // 3. SECTION: My Confirmed Amenity Passes
        item {
          DashboardSectionHeader(
            title = "My Amenity Passes",
            subtitle = "Active pool, gym & clubhouse reservations",
            actionLabel = "Reserve More",
            tag = "resident_view_amenities_link",
            onActionClick = { onNavigateToTab(2) }
          )
        }

        if (myBookings.isEmpty()) {
          item {
            EmptySectionCard(
              text = "No upcoming reservations. Reserve the Heated Infinity Pool, Gym, or Tennis Court above.",
              icon = Icons.Filled.EventAvailable
            )
          }
        } else {
          items(myBookings, key = { it.id }) { booking ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = TutsvilleCardBg,
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      StatusPill(
                        text = "CONFIRMED PASS",
                        bgColor = StatusCompletedBg,
                        textColor = StatusCompletedText,
                        dotColor = Color(0xFF2E7D32)
                      )
                      Text(
                        text = booking.id,
                        style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant)
                      )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                      text = booking.facilityName,
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TutsvillePrimary
                      )
                    )

                    Text(
                      text = "${booking.date}  •  ${booking.timeSlot}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleSecondary,
                        fontWeight = FontWeight.SemiBold
                      )
                    )

                    Text(
                      text = "${booking.guestCount} Guests Registered",
                      style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 11.sp)
                    )
                  }

                  OutlinedButton(
                    onClick = { viewModel.cancelBooking(booking.id) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, TutsvilleOutline),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                  ) {
                    Text("Cancel", fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
                  }
                }
              }
            }
          }
        }

        // 4. SECTION: My Unit Maintenance Work Orders
        item {
          Spacer(modifier = Modifier.height(8.dp))
          DashboardSectionHeader(
            title = "My Residence Work Orders",
            subtitle = "Active repairs & technician servicing for your unit",
            actionLabel = "View Tickets",
            tag = "resident_view_work_orders_link",
            onActionClick = { onNavigateToTab(1) }
          )
        }

        if (myCases.isEmpty()) {
          item {
            EmptySectionCard(
              text = "No open repairs for your unit. Everything is in pristine order.",
              icon = Icons.Filled.CheckCircle
            )
          }
        } else {
          items(myCases.take(3), key = { it.id }) { caseItem ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
              OperationalCard(
                onClick = { onNavigateToTab(1) }
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      CaseStatusPill(caseItem.status)
                      PriorityBadge(caseItem.priority)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                      text = caseItem.title,
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TutsvillePrimary
                      )
                    )

                    Text(
                      text = "${caseItem.category.label}  •  ${caseItem.location}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleSecondary,
                        fontWeight = FontWeight.SemiBold
                      )
                    )

                    if (caseItem.assignedVendorName != null) {
                      Text(
                        text = "Assigned Contractor: ${caseItem.assignedVendorName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                          color = GoldAccent,
                          fontWeight = FontWeight.Bold,
                          fontSize = 11.sp
                        )
                      )
                    }
                  }

                  if (caseItem.status == CaseStatus.RESOLVED && caseItem.rating == null) {
                    Button(
                      onClick = { viewModel.activeReviewCase.value = caseItem },
                      shape = RoundedCornerShape(8.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.height(34.dp)
                    ) {
                      Text("Review", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                  }
                }
              }
            }
          }
        }

      } else {
        // ==========================================
        // ESTATE MANAGER COMMAND VIEW
        // ==========================================

        // 1. High-Impact Operational KPI Stat Cards
        item {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Stat 1: Scheduled Servicing
              DashboardStatCard(
                title = "Scheduled Servicing",
                countText = "${upcomingMaintenances.size}",
                subtitle = "${maintenances.count { it.status == MaintenanceStatus.COMPLETED }} completed",
                badgeText = if (upcomingMaintenances.any { it.status == MaintenanceStatus.OVERDUE }) "Overdue Alerts" else "Healthy",
                badgeUrgent = upcomingMaintenances.any { it.status == MaintenanceStatus.OVERDUE },
                icon = Icons.Filled.Build,
                accentColor = TutsvillePrimary,
                modifier = Modifier
                  .weight(1f)
                  .testTag("kpi_card_maintenances"),
                onClick = { onNavigateToTab(1) }
              )

              // Stat 2: Active Cases
              DashboardStatCard(
                title = "Active Cases",
                countText = "${activeCases.size}",
                subtitle = if (emergencyCasesCount > 0) "$emergencyCasesCount critical/urgent" else "Standard SLA",
                badgeText = if (emergencyCasesCount > 0) "Action Required" else "Normal",
                badgeUrgent = emergencyCasesCount > 0,
                icon = Icons.AutoMirrored.Filled.Assignment,
                accentColor = if (emergencyCasesCount > 0) Color(0xFFC62828) else GoldAccent,
                modifier = Modifier
                  .weight(1f)
                  .testTag("kpi_card_cases"),
                onClick = { onNavigateToTab(1) }
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Stat 3: Units & Residences
              val occupiedUnits = properties.count { it.status == PropertyStatus.OCCUPIED }
              DashboardStatCard(
                title = "Units & Venues",
                countText = "${properties.size} Units",
                subtitle = "$occupiedUnits leased • ${facilities.size} amenities",
                badgeText = "Estate Properties",
                icon = Icons.Filled.Apartment,
                accentColor = TutsvilleSecondary,
                modifier = Modifier
                  .weight(1f)
                  .testTag("kpi_card_facilities"),
                onClick = { onNavigateToTab(2) }
              )

              // Stat 4: Approved Contractors
              DashboardStatCard(
                title = "Contractors",
                countText = "${vendors.size}",
                subtitle = "${vendors.count { it.activeJobsCount > 0 }} on active dispatch",
                badgeText = "Vetted Network",
                icon = Icons.Filled.Engineering,
                accentColor = TutsvilleTertiary,
                modifier = Modifier
                  .weight(1f)
                  .testTag("kpi_card_contracts"),
                onClick = { onNavigateToTab(1) }
              )
            }
          }
        }

        // 2. Quick Operations Action Ribbon
        item {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
              text = "Quick Dispatch Actions",
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary,
                letterSpacing = 0.04.sp
              )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              QuickActionButton(
                label = "List Residence",
                icon = Icons.Filled.AddHome,
                tag = "quick_action_list_property",
                onClick = { viewModel.activeListPropertyDialog.value = true }
              )
              QuickActionButton(
                label = "Log Work Order",
                icon = Icons.Filled.Add,
                tag = "quick_action_log_case",
                onClick = { viewModel.activeReportCaseDialog.value = true }
              )
              QuickActionButton(
                label = "Schedule Servicing",
                icon = Icons.Filled.CalendarMonth,
                tag = "quick_action_schedule_maintenance",
                onClick = { viewModel.activeAddMaintenanceDialog.value = true }
              )
              QuickActionButton(
                label = "Onboard Tenant",
                icon = Icons.Filled.PersonAdd,
                tag = "quick_action_create_tenant",
                onClick = { viewModel.activeCreateTenantDialog.value = true }
              )
              QuickActionButton(
                label = "Onboard Vendor",
                icon = Icons.Filled.Engineering,
                tag = "quick_action_add_vendor",
                onClick = { viewModel.activeAddVendorDialog.value = true }
              )
              QuickActionButton(
                label = "Broadcast Notice",
                icon = Icons.Filled.Campaign,
                tag = "quick_action_send_reminder",
                onClick = { viewModel.activeSendReminderDialog.value = true }
              )
            }
          }
        }

        // 3. SECTION: Upcoming Maintenance Tasks Summary
        item {
          DashboardSectionHeader(
            title = "Upcoming Maintenance Tasks",
            subtitle = "Mechanical, life safety & structural servicing",
            actionLabel = "View Pipeline",
            tag = "dashboard_view_calendar_link",
            onActionClick = { onNavigateToTab(1) }
          )
        }

        if (upcomingMaintenances.isEmpty()) {
          item {
            EmptySectionCard(
              text = "All scheduled servicing tasks completed.",
              icon = Icons.Filled.CheckCircle
            )
          }
        } else {
          items(upcomingMaintenances.take(3), key = { it.id }) { event ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
              OperationalCard(
                onClick = { onNavigateToTab(1) }
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      DisciplinePill(event.discipline)
                      StatusPill(
                        text = event.status.label.uppercase(),
                        bgColor = if (event.status == MaintenanceStatus.OVERDUE) StatusUrgentBg else StatusPendingBg,
                        textColor = if (event.status == MaintenanceStatus.OVERDUE) StatusUrgentText else StatusPendingText,
                        dotColor = if (event.status == MaintenanceStatus.OVERDUE) Color(0xFFC62828) else StatusPendingText
                      )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                      text = event.title,
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TutsvillePrimary
                      )
                    )

                    Text(
                      text = "${event.scheduledDate} at ${event.scheduledTime}  •  ${event.targetArea}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleSecondary,
                        fontWeight = FontWeight.SemiBold
                      )
                    )

                    Text(
                      text = "Contractor: ${event.contractorName} (${event.intervalMonths} mo interval)",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleOnSurfaceVariant,
                        fontSize = 11.sp
                      )
                    )
                  }

                  if (event.status != MaintenanceStatus.COMPLETED) {
                    Button(
                      onClick = { viewModel.toggleMaintenanceCompletion(event) },
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                      modifier = Modifier.height(34.dp)
                    ) {
                      Text("Complete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }

        // 4. SECTION: Active Work Orders (Incident Triage & Dispatch)
        item {
          Spacer(modifier = Modifier.height(8.dp))
          DashboardSectionHeader(
            title = "Active Incident Work Orders",
            subtitle = "Incident triage, vendor dispatch & SLA tracking",
            actionLabel = "All Work Orders",
            tag = "dashboard_view_cases_link",
            onActionClick = { onNavigateToTab(1) }
          )
        }

        if (activeCases.isEmpty()) {
          item {
            EmptySectionCard(
              text = "No open incident reports. Estate operations running smoothly.",
              icon = Icons.Filled.CheckCircle
            )
          }
        } else {
          items(activeCases.take(3), key = { it.id }) { caseItem ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
              OperationalCard(
                onClick = { onNavigateToTab(1) }
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      CaseStatusPill(caseItem.status)
                      PriorityBadge(caseItem.priority)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                      text = caseItem.title,
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TutsvillePrimary
                      )
                    )

                    Text(
                      text = "${caseItem.category.label}  •  ${caseItem.location}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleSecondary,
                        fontWeight = FontWeight.SemiBold
                      )
                    )

                    Text(
                      text = "Reported by ${caseItem.reportedBy}  •  ${caseItem.reportedDate}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleOnSurfaceVariant,
                        fontSize = 11.sp
                      )
                    )
                  }

                  if (caseItem.status != CaseStatus.RESOLVED) {
                    FilledTonalButton(
                      onClick = {
                        if (caseItem.assignedVendorId == null) {
                          viewModel.activeAssignVendorCase.value = caseItem
                        } else {
                          viewModel.advanceCaseStatus(caseItem.id, caseItem.status)
                        }
                      },
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TutsvilleSecondaryContainer,
                        contentColor = TutsvilleOnSecondaryContainer
                      ),
                      modifier = Modifier.height(34.dp)
                    ) {
                      Text(
                        text = if (caseItem.assignedVendorId == null) "Dispatch" else "Advance →",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // ==========================================
      // SHARED SECTION: ESTATE ADVISORIES & BULLETINS
      // ==========================================
      if (reminders.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Estate Announcements & Advisories",
            style = MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
          )
        }

        items(reminders.take(2), key = { it.id }) { reminder ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.White,
              border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Surface(
                  shape = CircleShape,
                  color = GoldAccent.copy(alpha = 0.15f),
                  modifier = Modifier.size(36.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                  }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  )
                  Text(
                    text = reminder.message,
                    style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 11.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DashboardStatCard(
  title: String,
  countText: String,
  subtitle: String,
  badgeText: String,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier,
  badgeUrgent: Boolean = false,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = TutsvilleCardBg,
    border = BorderStroke(1.dp, TutsvilleOutline),
    shadowElevation = 1.dp,
    modifier = modifier.clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = accentColor.copy(alpha = 0.12f),
          modifier = Modifier.size(34.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (badgeUrgent) StatusUrgentBg else StatusPendingBg,
          border = BorderStroke(0.8.dp, if (badgeUrgent) Color(0xFFF0B3B3) else Color(0xFFF2DCAE))
        ) {
          Text(
            text = badgeText,
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (badgeUrgent) StatusUrgentText else StatusPendingText,
              fontWeight = FontWeight.Bold,
              fontSize = 9.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = countText,
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TutsvillePrimary,
          fontFamily = FontFamily.Serif
        )
      )

      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          color = TutsvilleOnSurface
        )
      )

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          color = TutsvilleOnSurfaceVariant,
          fontSize = 11.sp
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun DashboardSectionHeader(
  title: String,
  subtitle: String,
  actionLabel: String,
  tag: String,
  onActionClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          color = TutsvillePrimary
        )
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall.copy(
          color = TutsvilleOnSurfaceVariant,
          fontSize = 11.sp
        )
      )
    }

    TextButton(
      onClick = onActionClick,
      modifier = Modifier.testTag(tag)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelMedium.copy(
            color = TutsvilleSecondary,
            fontWeight = FontWeight.Bold
          )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = null,
          tint = TutsvilleSecondary,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

@Composable
fun QuickActionButton(
  label: String,
  icon: ImageVector,
  tag: String,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = Color.White,
    border = BorderStroke(1.dp, TutsvilleSecondary.copy(alpha = 0.5f)),
    shadowElevation = 1.dp,
    modifier = modifier
      .clickable { onClick() }
      .testTag(tag)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = TutsvilleSecondary,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
          color = TutsvillePrimary
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun EmptySectionCard(
  text: String,
  icon: ImageVector
) {
  Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color.White,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = StatusCompletedText,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = text,
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )
      }
    }
  }
}

@Composable
fun DisciplinePill(discipline: MaintenanceDiscipline) {
  val (bgColor, textColor) = when (discipline) {
    MaintenanceDiscipline.FIRE_LIFE -> StatusUrgentBg to StatusUrgentText
    MaintenanceDiscipline.ELEVATORS -> Color(0xFFECEFF1) to Color(0xFF37474F)
    MaintenanceDiscipline.HVAC -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
    MaintenanceDiscipline.ELECTRICAL -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
    MaintenanceDiscipline.PLUMBING -> Color(0xFFE0F7FA) to Color(0xFF00838F)
    MaintenanceDiscipline.GROUNDS -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
  }

  Surface(
    shape = RoundedCornerShape(4.dp),
    color = bgColor
  ) {
    Text(
      text = discipline.label.split("&")[0].trim().uppercase(),
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        color = textColor
      ),
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}
