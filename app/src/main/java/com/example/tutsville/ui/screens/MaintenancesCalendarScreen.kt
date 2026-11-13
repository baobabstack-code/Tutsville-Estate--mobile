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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.*
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenancesCalendarScreen(
  viewModel: EstateViewModel,
  onNavigateToTab: (Int) -> Unit = { viewModel.setSelectedTab(it) },
  modifier: Modifier = Modifier
) {
  val selectedDateKey by viewModel.selectedCalendarDateKey.collectAsState()
  val selectedYear by viewModel.selectedCalendarYear.collectAsState()
  val selectedMonth by viewModel.selectedCalendarMonth.collectAsState()
  val categoryFilter by viewModel.calendarCategoryFilter.collectAsState()
  val eventsByDate by viewModel.calendarEventsByDate.collectAsState()
  val allEvents by viewModel.allCalendarEvents.collectAsState()

  val maintenances by viewModel.maintenances.collectAsState()
  val contracts by viewModel.contracts.collectAsState()
  val cases by viewModel.cases.collectAsState()
  val facilities by viewModel.facilities.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()

  var deletingMaintenanceId by remember { mutableStateOf<String?>(null) }
  var cancellingBookingId by remember { mutableStateOf<String?>(null) }

  val monthCells = remember(selectedYear, selectedMonth) {
    EstateDateUtils.buildMonthCells(selectedYear, selectedMonth)
  }
  val monthTitle = remember(selectedYear, selectedMonth) {
    EstateDateUtils.monthTitle(selectedYear, selectedMonth)
  }
  val dayEvents = remember(eventsByDate, selectedDateKey) {
    eventsByDate[selectedDateKey] ?: emptyList()
  }

  val dayOfWeekHeaders = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("calendar_screen_list"),
      contentPadding = PaddingValues(bottom = 104.dp)
    ) {
      // 1. Header & Quick Create Button
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              Text(
                text = "Calendar",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Maintenances, amenities, contracts & cases",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            if (currentRole == UserRole.MANAGER) {
              Button(
                onClick = { viewModel.activeAddMaintenanceDialog.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                  .height(40.dp)
                  .testTag("btn_calendar_add_maintenance")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Schedule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            } else {
              Button(
                onClick = {
                  val firstFac = facilities.firstOrNull()
                  if (firstFac != null) {
                    viewModel.activeBookFacility.value = firstFac
                  } else {
                    viewModel.activeReportCaseDialog.value = true
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                  .height(40.dp)
                  .testTag("btn_calendar_tenant_action")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (facilities.isNotEmpty()) "Book" else "Add Case", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 2. Interactive Month Calendar Card
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = TutsvilleCardBg,
          border = BorderStroke(1.dp, TutsvilleOutline),
          shadowElevation = 2.dp,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("calendar_month_grid_card")
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            // Month Header Controls
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = { viewModel.navigateCalendarMonth(-1) },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("btn_calendar_prev_month")
              ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = TutsvillePrimary)
              }

              Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 4.dp)
              )

              TextButton(
                onClick = { viewModel.jumpCalendarToToday() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("btn_calendar_today")
              ) {
                Text(
                  text = "Today",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = GoldAccent,
                  maxLines = 1,
                  softWrap = false
                )
              }

              IconButton(
                onClick = { viewModel.navigateCalendarMonth(1) },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("btn_calendar_next_month")
              ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TutsvillePrimary)
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Weekday Headers
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              dayOfWeekHeaders.forEach { dow ->
                Box(
                  modifier = Modifier.weight(1f),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = dow,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = TutsvilleOnSurfaceVariant
                    ),
                    maxLines = 1
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 7-Column Calendar Rows
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              monthCells.chunked(7).forEach { week ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  week.forEach { cell ->
                    val isSelected = cell.dateKey == selectedDateKey
                    val cellEvents = eventsByDate[cell.dateKey].orEmpty()
                    val dotColors = cellEvents
                      .map { eventDotColor(it.type) }
                      .distinct()
                      .take(4)

                    val containerBg = when {
                      isSelected -> TutsvillePrimary
                      cell.isToday -> GoldLight
                      else -> Color.Transparent
                    }
                    val border = when {
                      isSelected -> BorderStroke(1.dp, TutsvillePrimary)
                      cell.isToday -> BorderStroke(1.2.dp, GoldAccent)
                      else -> null
                    }
                    val numberColor = when {
                      isSelected -> Color.White
                      cell.isToday -> TutsvillePrimary
                      cell.isCurrentMonth -> TutsvilleOnBackground
                      else -> TutsvilleOnSurfaceVariant.copy(alpha = 0.4f)
                    }

                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = containerBg,
                      border = border,
                      modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.selectCalendarDate(cell.dateKey) }
                        .testTag("calendar_cell_${cell.dateKey}")
                    ) {
                      Column(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                      ) {
                        Text(
                          text = "${cell.dayOfMonth}",
                          style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = if (isSelected || cell.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = numberColor
                          ),
                          maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                          modifier = Modifier.height(6.dp),
                          contentAlignment = Alignment.Center
                        ) {
                          if (dotColors.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                              dotColors.forEach { dotColor ->
                                Box(
                                  modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else dotColor)
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
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = TutsvilleOutline.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(8.dp))

            // Color Legend Row (Wraps cleanly with zero overlap on narrow screens or large fonts)
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              LegendDotLabel("Maint.", eventDotColor(CalendarEventType.MAINTENANCE))
              LegendDotLabel("Amenities", eventDotColor(CalendarEventType.BOOKING))
              LegendDotLabel("Contracts", eventDotColor(CalendarEventType.CONTRACT))
              LegendDotLabel("Cases", eventDotColor(CalendarEventType.CASE))
            }
          }
        }
      }

      // 3. Category Filter Strip
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = categoryFilter == null,
            onClick = { viewModel.setCalendarCategoryFilter(null) },
            label = { Text("All (${allEvents.size})", fontSize = 11.sp, maxLines = 1) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = TutsvillePrimary,
              selectedLabelColor = Color.White
            )
          )
          CalendarEventType.values().forEach { type ->
            val typeCount = allEvents.count { it.type == type }
            FilterChip(
              selected = categoryFilter == type,
              onClick = {
                viewModel.setCalendarCategoryFilter(if (categoryFilter == type) null else type)
              },
              label = { Text("${type.label} ($typeCount)", fontSize = 11.sp, maxLines = 1) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TutsvillePrimary,
                selectedLabelColor = Color.White
              )
            )
          }
        }
      }

      // 4. Selected Day Agenda Header
      item {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = EstateDateUtils.fullHeaderFromKey(selectedDateKey),
            style = MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            ),
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          StatusPill(
            text = if (dayEvents.size == 1) "1 Event" else "${dayEvents.size} Events",
            bgColor = if (dayEvents.isEmpty()) StatusNeutralBg else TutsvilleSecondaryContainer,
            textColor = if (dayEvents.isEmpty()) StatusNeutralText else TutsvilleOnSecondaryContainer,
            dotColor = if (dayEvents.isEmpty()) StatusNeutralText else GoldAccent
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // 5. Agenda List or Empty State
      if (dayEvents.isEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = TutsvilleCardBg,
            border = BorderStroke(1.dp, TutsvilleOutline),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .testTag("calendar_empty_day_card")
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null,
                tint = TutsvilleSecondary,
                modifier = Modifier.size(28.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No events scheduled for ${EstateDateUtils.displayFromKey(selectedDateKey)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = TutsvillePrimary
                )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Synchronized across Maintenances, Amenities, Contracts, and Cases.",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (currentRole == UserRole.MANAGER) {
                  Button(
                    onClick = { viewModel.activeAddMaintenanceDialog.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                  ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Schedule Maintenance", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                } else {
                  if (facilities.isNotEmpty()) {
                    Button(
                      onClick = { viewModel.activeBookFacility.value = facilities.first() },
                      colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                      shape = RoundedCornerShape(10.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                      modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                      Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("Book Amenity", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                  }
                  OutlinedButton(
                    onClick = { viewModel.activeReportCaseDialog.value = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TutsvillePrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                  ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = TutsvillePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Case", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                }
              }
            }
          }
        }
      } else {
        items(dayEvents, key = { it.id }) { event ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
            CalendarAgendaCard(
              event = event,
              currentRole = currentRole,
              onToggleMaintenance = {
                maintenances.find { it.id == event.sourceId }?.let { m ->
                  viewModel.toggleMaintenanceCompletion(m)
                }
              },
              onDeleteMaintenance = { deletingMaintenanceId = event.sourceId },
              onCancelBooking = { cancellingBookingId = event.sourceId },
              onOpenContract = {
                contracts.find { it.id == event.sourceId }?.let { ctr ->
                  viewModel.activeContractDetail.value = ctr
                }
              },
              onOpenCasesTab = { onNavigateToTab(1) },
              onDispatchCaseVendor = {
                cases.find { it.id == event.sourceId }?.let { c ->
                  viewModel.activeAssignVendorCase.value = c
                }
              }
            )
          }
        }
      }
    }

    // Centered Delete Confirmation for Maintenance
    deletingMaintenanceId?.let { maintId ->
      val itemTitle = maintenances.find { it.id == maintId }?.title ?: "this scheduled task"
      DeleteConfirmDialog(
        title = "Delete Maintenance?",
        message = "Are you sure you want to delete \"$itemTitle\" from the estate calendar?",
        onConfirm = {
          viewModel.deleteMaintenance(maintId)
          deletingMaintenanceId = null
        },
        onDismiss = { deletingMaintenanceId = null }
      )
    }

    // Centered Cancel Confirmation for Amenity Booking
    cancellingBookingId?.let { bookingId ->
      DeleteConfirmDialog(
        title = "Cancel Amenity Pass?",
        message = "Are you sure you want to cancel this amenity reservation?",
        onConfirm = {
          viewModel.cancelBooking(bookingId)
          cancellingBookingId = null
        },
        onDismiss = { cancellingBookingId = null }
      )
    }
  }
}

@Composable
private fun CalendarAgendaCard(
  event: CalendarEventItem,
  currentRole: UserRole,
  onToggleMaintenance: () -> Unit,
  onDeleteMaintenance: () -> Unit,
  onCancelBooking: () -> Unit,
  onOpenContract: () -> Unit,
  onOpenCasesTab: () -> Unit,
  onDispatchCaseVendor: () -> Unit
) {
  val dotColor = eventDotColor(event.type)
  val (pillBg, pillText) = when {
    event.isUrgent -> StatusUrgentBg to StatusUrgentText
    event.isCompleted -> StatusCompletedBg to StatusCompletedText
    else -> StatusPendingBg to StatusPendingText
  }

  OperationalCard {
    // Row 1: Category Pill & Status Pill (Never overlaps)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      StatusPill(
        text = event.type.label,
        bgColor = TutsvilleSecondaryContainer,
        textColor = TutsvillePrimary,
        dotColor = dotColor
      )

      StatusPill(
        text = event.statusLabel,
        bgColor = pillBg,
        textColor = pillText,
        dotColor = pillText
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Row 2: Title & Details
    Text(
      text = event.title,
      style = MaterialTheme.typography.titleMedium.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = TutsvillePrimary
      )
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Schedule, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = event.displayTime,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TutsvilleSecondary),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = event.subtitle,
      style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
    )

    // Row 3: Dedicated Action Bar (Prevents overlap with text)
    Spacer(modifier = Modifier.height(12.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      when (event.type) {
        CalendarEventType.MAINTENANCE -> {
          if (currentRole == UserRole.MANAGER) {
            OutlinedButton(
              onClick = onToggleMaintenance,
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, TutsvilleOutline),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
              Icon(
                imageVector = if (event.isCompleted) Icons.Default.Replay else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = TutsvillePrimary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (event.isCompleted) "Reopen" else "Mark Complete",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            IconButton(
              onClick = onDeleteMaintenance,
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Maintenance",
                tint = Color(0xFFC62828),
                modifier = Modifier.size(20.dp)
              )
            }
          } else {
            Text(
              text = "Estate Maintenance Advisory",
              style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }

        CalendarEventType.BOOKING -> {
          OutlinedButton(
            onClick = onCancelBooking,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFF0B3B3)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
          ) {
            Icon(Icons.Default.EventBusy, contentDescription = null, tint = StatusUrgentText, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancel Pass", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusUrgentText, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }

        CalendarEventType.CONTRACT -> {
          OutlinedButton(
            onClick = onOpenContract,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, TutsvillePrimary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
          ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = TutsvillePrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("View Contract", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }

        CalendarEventType.CASE -> {
          OutlinedButton(
            onClick = onOpenCasesTab,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, TutsvilleOutline),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
          ) {
            Text("View in Cases", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TutsvillePrimary, modifier = Modifier.size(14.dp))
          }

          if (currentRole == UserRole.MANAGER && !event.isCompleted) {
            Button(
              onClick = onDispatchCaseVendor,
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
              modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
              Text("Dispatch Vendor", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LegendDotLabel(label: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(7.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = TutsvilleOnSurfaceVariant,
        fontWeight = FontWeight.SemiBold
      ),
      maxLines = 1
    )
  }
}

private fun eventDotColor(type: CalendarEventType): Color = when (type) {
  CalendarEventType.MAINTENANCE -> TutsvillePrimary
  CalendarEventType.BOOKING -> GoldAccent
  CalendarEventType.CONTRACT -> StatusUrgentText
  CalendarEventType.CASE -> TutsvilleSecondary
}
