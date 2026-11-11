package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
  val contracts by viewModel.contracts.collectAsState()
  val properties by viewModel.properties.collectAsState()

  // Computed Dashboard Metrics
  val upcomingMaintenances = remember(maintenances) {
    maintenances.filter { it.status != MaintenanceStatus.COMPLETED }
  }

  val activeCases = remember(cases) {
    cases.filter { it.status != CaseStatus.RESOLVED }
  }

  val emergencyCasesCount = remember(activeCases) {
    activeCases.count { it.priority == CasePriority.EMERGENCY || it.priority == CasePriority.HIGH }
  }

  val expiringContractsCount = remember(contracts) {
    contracts.count { it.daysRemaining <= 60 }
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
      // Top spacer for KPI stat cards
      item {
        Spacer(modifier = Modifier.height(6.dp))
      }

      // 2. High-Impact KPI Stat Cards Row
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Stat 1: Upcoming Maintenances (links to Calendar Tab 1)
            DashboardStatCard(
              title = "Maintenances",
              countText = "${upcomingMaintenances.size}",
              subtitle = "Scheduled servicing",
              badgeText = "${maintenances.count { it.status == MaintenanceStatus.COMPLETED }} completed",
              icon = Icons.Filled.Build,
              accentColor = TutsvillePrimary,
              modifier = Modifier
                .weight(1f)
                .testTag("kpi_card_maintenances"),
              onClick = { onNavigateToTab(1) }
            )

            // Stat 2: Active Case Reports (links to Cases Tab 2)
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
              onClick = { onNavigateToTab(2) }
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Stat 3: Facilities & Residences (links to Facilities Tab 3)
            val occupiedUnits = properties.count { it.status == PropertyStatus.OCCUPIED }
            DashboardStatCard(
              title = "Units & Venues",
              countText = "${properties.size} Units",
              subtitle = "$occupiedUnits leased • ${bookings.size} passes",
              badgeText = "Estate Properties",
              icon = Icons.Filled.Apartment,
              accentColor = TutsvilleSecondary,
              modifier = Modifier
                .weight(1f)
                .testTag("kpi_card_facilities"),
              onClick = { onNavigateToTab(3) }
            )

            // Stat 4: Contracts & SLAs (links to Contracts Tab 4)
            DashboardStatCard(
              title = "Vendor SLAs",
              countText = "${contracts.size}",
              subtitle = if (expiringContractsCount > 0) "$expiringContractsCount expiring soon" else "All healthy",
              badgeText = if (expiringContractsCount > 0) "Review SLA" else "Active",
              badgeUrgent = expiringContractsCount > 0,
              icon = Icons.Filled.Description,
              accentColor = TutsvilleTertiary,
              modifier = Modifier
                .weight(1f)
                .testTag("kpi_card_contracts"),
              onClick = { onNavigateToTab(4) }
            )
          }
        }
      }

      // 3. Quick Operations Action Ribbon
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
              label = "Reserve Amenity",
              icon = Icons.Filled.Apartment,
              tag = "quick_action_book_facility",
              onClick = {
                val firstFac = facilities.firstOrNull()
                viewModel.activeBookFacility.value = firstFac
              }
            )
            QuickActionButton(
              label = "Broadcast Advisory",
              icon = Icons.Filled.Campaign,
              tag = "quick_action_send_reminder",
              onClick = { viewModel.activeSendReminderDialog.value = true }
            )
          }
        }
      }

      // 4. SECTION: Upcoming Maintenance Tasks Summary
      item {
        DashboardSectionHeader(
          title = "Upcoming Maintenance Tasks",
          subtitle = "Mechanical, life safety & structural servicing",
          actionLabel = "View Calendar",
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
                  // Discipline and Area
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    DisciplinePill(event.discipline)
                    Text(
                      text = "•  ${event.targetArea}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleOnSurfaceVariant,
                        fontSize = 11.sp
                      ),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
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

                  Spacer(modifier = Modifier.height(4.dp))

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Schedule,
                      contentDescription = null,
                      tint = TutsvilleSecondary,
                      modifier = Modifier.size(14.dp)
                    )
                    Text(
                      text = "${event.scheduledDate}  |  ${event.scheduledTime}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleOnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                      )
                    )
                  }

                  Text(
                    text = "Contractor: ${event.contractorName}",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = TutsvilleOnSurfaceVariant.copy(alpha = 0.8f),
                      fontSize = 11.sp
                    )
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                  horizontalAlignment = Alignment.End,
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  MaintenanceStatusPill(event.status)

                  // Fast toggle button
                  FilledTonalButton(
                    onClick = { viewModel.toggleMaintenanceCompletion(event) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                      containerColor = if (event.status == MaintenanceStatus.COMPLETED) StatusCompletedBg else TutsvilleSecondaryContainer,
                      contentColor = if (event.status == MaintenanceStatus.COMPLETED) StatusCompletedText else TutsvilleOnSecondaryContainer
                    ),
                    modifier = Modifier.height(30.dp)
                  ) {
                    Text(
                      text = if (event.status == MaintenanceStatus.COMPLETED) "Completed ✓" else "Mark Done",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 5. SECTION: Active Case Reports Summary
      item {
        Spacer(modifier = Modifier.height(10.dp))
        DashboardSectionHeader(
          title = "Active Case Reports",
          subtitle = "Incident triage, vendor dispatch & SLA tracking",
          actionLabel = "View All Cases",
          tag = "dashboard_view_cases_link",
          onActionClick = { onNavigateToTab(2) }
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
              onClick = { onNavigateToTab(2) }
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
                    Text(
                      text = caseItem.id,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TutsvillePrimary
                      )
                    )
                    PriorityBadge(caseItem.priority)
                    Text(
                      text = "•  ${caseItem.category.label}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleOnSurfaceVariant,
                        fontSize = 11.sp
                      )
                    )
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  Text(
                    text = caseItem.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = TutsvillePrimary
                    )
                  )

                  Spacer(modifier = Modifier.height(4.dp))

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Place,
                      contentDescription = null,
                      tint = TutsvilleSecondary,
                      modifier = Modifier.size(14.dp)
                    )
                    Text(
                      text = caseItem.location,
                      style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
                    )
                  }

                  Spacer(modifier = Modifier.height(2.dp))

                  Text(
                    text = if (caseItem.assignedVendorName != null) {
                      "Assigned: ${caseItem.assignedVendorName}"
                    } else {
                      "⚠️ Vendor unassigned — tap to dispatch"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = if (caseItem.assignedVendorName != null) TutsvillePrimary else Color(0xFFC62828),
                      fontWeight = if (caseItem.assignedVendorName != null) FontWeight.Normal else FontWeight.Bold,
                      fontSize = 11.sp
                    )
                  )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                  horizontalAlignment = Alignment.End,
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  CaseStatusPill(caseItem.status)

                  // Fast status advance button
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
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                      colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TutsvilleSecondaryContainer,
                        contentColor = TutsvilleOnSecondaryContainer
                      ),
                      modifier = Modifier.height(30.dp)
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

      // 6. SECTION: Facility Booking Statuses Summary
      item {
        Spacer(modifier = Modifier.height(10.dp))
        DashboardSectionHeader(
          title = "Facility Booking Statuses",
          subtitle = "Active estate passes & amenity reservations",
          actionLabel = "All Facilities",
          tag = "dashboard_view_facilities_link",
          onActionClick = { onNavigateToTab(3) }
        )
      }

      // Active Passes Carousel / Cards
      if (bookings.isNotEmpty()) {
        item {
          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
              text = "Confirmed Upcoming Passes (${bookings.size})",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TutsvilleSecondary
              )
            )
            Spacer(modifier = Modifier.height(6.dp))
          }
        }

        items(bookings, key = { it.id }) { booking ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleSecondary.copy(alpha = 0.5f)),
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
                    text = "Reserved by ${booking.residentName} (${booking.unit}) • ${booking.guestCount} Guests",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = TutsvilleOnSurfaceVariant,
                      fontSize = 11.sp
                    )
                  )
                }

                IconButton(
                  onClick = { viewModel.cancelBooking(booking.id) },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel booking",
                    tint = TutsvilleOnSurfaceVariant.copy(alpha = 0.6f)
                  )
                }
              }
            }
          }
        }
      }

      // Amenity Availability Status Overview List
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          Text(
            text = "Estate Amenity Live Availability",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      items(facilities, key = { it.id }) { facility ->
        val facilityBookings = remember(bookings, facility.id) {
          bookings.filter { it.facilityId == facility.id }
        }
        val isBooked = facilityBookings.isNotEmpty()

        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
          OperationalCard(
            onClick = { viewModel.activeBookFacility.value = facility }
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = facility.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = TutsvillePrimary
                    )
                  )
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TutsvilleSecondaryContainer
                  ) {
                    Text(
                      text = "Cap: ${facility.capacity}",
                      style = MaterialTheme.typography.labelSmall.copy(
                        color = TutsvilleOnSecondaryContainer,
                        fontSize = 10.sp
                      ),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                Text(
                  text = facility.location,
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = TutsvilleOnSurfaceVariant,
                    fontSize = 11.sp
                  )
                )

                Text(
                  text = if (isBooked) {
                    "Reserved: ${facilityBookings.first().timeSlot} (${facilityBookings.first().residentName})"
                  } else {
                    "✓ Available today • ${facility.allowedSlots.size} slots open"
                  },
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isBooked) StatusPendingText else StatusCompletedText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                  )
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              Button(
                onClick = { viewModel.activeBookFacility.value = facility },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = TutsvillePrimary,
                  contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Text(
                  text = "Reserve",
                  fontSize = 12.sp,
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
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = Color.White,
    border = BorderStroke(1.dp, TutsvilleSecondary.copy(alpha = 0.5f)),
    shadowElevation = 1.dp,
    modifier = Modifier
      .clickable { onClick() }
      .testTag(tag)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
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
        )
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
