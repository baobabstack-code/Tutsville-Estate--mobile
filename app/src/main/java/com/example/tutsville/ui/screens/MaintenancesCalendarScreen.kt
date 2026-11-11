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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.MaintenanceDiscipline
import com.example.tutsville.data.model.MaintenanceEvent
import com.example.tutsville.data.model.MaintenanceStatus
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.ui.components.MaintenanceStatusPill
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.TutsvilleFloatingActionButton
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun MaintenancesCalendarScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val maintenances by viewModel.maintenances.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val filter by viewModel.maintenanceFilter.collectAsState()

  var selectedDay by remember { mutableStateOf("12") }

  val days = listOf(
    "08" to "Sun",
    "09" to "Mon",
    "10" to "Tue",
    "11" to "Wed",
    "12" to "Thu",
    "13" to "Fri",
    "14" to "Sat",
    "15" to "Sun"
  )

  val filteredEvents = remember(maintenances, filter) {
    if (filter == null) maintenances else maintenances.filter { it.discipline == filter }
  }

  Box(modifier = modifier.fillMaxSize().background(TutsvilleBackground)) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 90.dp)
    ) {
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Text(
            text = "Scheduled Maintenances",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          Text(
            text = "Estate mechanical, life safety & structural servicing calendar",
            style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Calendar Day Ribbon
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            days.forEach { (day, dow) ->
              val isSelected = day == selectedDay
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) TutsvillePrimary else TutsvilleCardBg,
                border = BorderStroke(1.dp, if (isSelected) TutsvillePrimary else TutsvilleOutline),
                modifier = Modifier
                  .width(52.dp)
                  .clickable { selectedDay = day }
                  .testTag("calendar_day_$day")
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = dow.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 10.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = if (isSelected) GoldAccent else TutsvilleOnSurfaceVariant
                    )
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = day,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) Color.White else TutsvilleOnBackground
                    )
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Discipline Filter Tabs
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = filter == null,
              onClick = { viewModel.setMaintenanceFilter(null) },
              label = { Text("All Disciplines") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TutsvillePrimary,
                selectedLabelColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp)
            )
            MaintenanceDiscipline.values().forEach { disc ->
              FilterChip(
                selected = filter == disc,
                onClick = { viewModel.setMaintenanceFilter(if (filter == disc) null else disc) },
                label = { Text(disc.label.substringBefore(" &")) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = TutsvillePrimary,
                  selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
              )
            }
          }
        }
      }

      items(filteredEvents, key = { it.id }) { event ->
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          OperationalCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              MaintenanceStatusPill(status = event.status)
              Text(
                text = event.scheduledDate,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = TutsvilleSecondary
                )
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
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
              Icon(Icons.Default.Schedule, contentDescription = null, tint = TutsvilleOnSurfaceVariant, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = event.scheduledTime,
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Icon(Icons.Default.Place, contentDescription = null, tint = TutsvilleOnSurfaceVariant, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = event.targetArea,
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Contractor: ${event.contractorName} • Servicing Cycle: Every ${event.intervalMonths} mo",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
            )

            if (event.notes.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = event.notes,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 12.sp,
                  color = Color(0xFF4A504B),
                  fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              OutlinedButton(
                onClick = { viewModel.toggleMaintenanceCompletion(event) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, TutsvilleOutline),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                  .height(36.dp)
                  .testTag("toggle_maint_${event.id.lowercase()}")
              ) {
                Icon(
                  imageVector = if (event.status == MaintenanceStatus.COMPLETED) Icons.Default.Replay else Icons.Default.CheckCircle,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = if (event.status == MaintenanceStatus.COMPLETED) TutsvilleSecondary else TutsvillePrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (event.status == MaintenanceStatus.COMPLETED) "Reopen" else "Mark Complete",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TutsvillePrimary
                )
              }
            }
          }
        }
      }
    }

    // Schedule Maintenance FAB (Visible for Managers)
    if (currentRole == UserRole.MANAGER) {
      TutsvilleFloatingActionButton(
        text = "Schedule Task",
        icon = Icons.Default.Add,
        onClick = { viewModel.activeAddMaintenanceDialog.value = true },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 20.dp, bottom = 20.dp),
        testTag = "add_maintenance_fab"
      )
    }
  }
}
