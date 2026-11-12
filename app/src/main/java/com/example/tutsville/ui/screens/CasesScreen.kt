package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.CasePriority
import com.example.tutsville.data.model.CaseStatus
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.ui.components.CaseStatusPill
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.PriorityBadge
import com.example.tutsville.ui.components.StarRatingBar
import com.example.tutsville.ui.components.TutsvilleFloatingActionButton
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun CasesScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val cases by viewModel.cases.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val currentTenant by viewModel.currentTenant.collectAsState()
  val filter by viewModel.caseFilter.collectAsState()

  val filteredCases = remember(cases, filter, currentRole, currentTenant) {
    var list = cases
    if (filter != null) {
      list = list.filter { it.status == filter }
    }
    when (currentRole) {
      UserRole.TENANT -> {
        val unit = currentTenant?.assignedPropertyUnit
        list.filter { c ->
          (unit != null && c.location.contains(unit, ignoreCase = true)) ||
          c.reportedBy.equals(currentTenant?.fullName, ignoreCase = true)
        }
      }
      UserRole.VENDOR -> list.filter { it.assignedVendorId != null }
      else -> list
    }
  }

  Box(modifier = modifier.fillMaxSize().background(TutsvilleBackground)) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 90.dp)
    ) {
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (currentRole == UserRole.TENANT) "My Maintenance Requests" else "Incident Work Orders",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = if (currentRole == UserRole.TENANT) {
                  "Active repairs and technician dispatch for ${currentTenant?.assignedPropertyUnit ?: "your residence"}"
                } else {
                  "Active maintenance cases, dispatch tracking & resolutions"
                },
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }
            if (currentRole == UserRole.MANAGER) {
              OutlinedButton(
                onClick = { viewModel.activeAddVendorDialog.value = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.2.dp, GoldAccent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TutsvillePrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                  .height(38.dp)
                  .testTag("cases_screen_add_vendor_button")
              ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Add Vendor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Status Filter Chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = filter == null,
              onClick = { viewModel.setCaseFilter(null) },
              label = { Text("All Cases (${cases.size})") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TutsvillePrimary,
                selectedLabelColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp)
            )
            CaseStatus.values().forEach { st ->
              val count = cases.count { it.status == st }
              FilterChip(
                selected = filter == st,
                onClick = { viewModel.setCaseFilter(if (filter == st) null else st) },
                label = { Text("${st.label} ($count)") },
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

      if (filteredCases.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(40.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (currentRole == UserRole.TENANT) {
                "No maintenance requests logged for your residence. Everything is in order."
              } else {
                "No cases matching selected filter"
              },
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      }

      items(filteredCases, key = { it.id }) { item ->
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          OperationalCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                CaseStatusPill(status = item.status)
                Spacer(modifier = Modifier.width(8.dp))
                PriorityBadge(priority = item.priority)
              }
              Text(
                text = "#${item.id}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TutsvilleOnSurfaceVariant
                )
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = item.title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = item.description,
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnBackground)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Place, contentDescription = null, tint = TutsvilleOnSurfaceVariant, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = item.location, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant))
              Spacer(modifier = Modifier.width(12.dp))
              Icon(Icons.Default.Person, contentDescription = null, tint = TutsvilleOnSurfaceVariant, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = item.reportedBy, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant))
            }

            // Vendor assignment status badge
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (item.assignedVendorName != null) Color(0xFFF1EEE6) else Color(0xFFFDF6EB),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (item.assignedVendorName != null) Icons.Default.Engineering else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (item.assignedVendorName != null) TutsvillePrimary else StatusPendingText,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = item.assignedVendorName?.let { "Assigned to $it" } ?: "Unassigned (Pending Vendor Dispatch)",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = if (item.assignedVendorName != null) TutsvillePrimary else StatusPendingText
                    )
                  )
                }
                Text(
                  text = item.reportedDate,
                  style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
                )
              }
            }

            // If already resolved with rating
            if (item.rating != null) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                StarRatingBar(rating = item.rating, starSize = 16.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = item.reviewComment ?: "Service Verified",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = TutsvilleSecondary
                  )
                )
              }
            }

            // Action row depending on role & status (Generous 40dp height, spacedBy 8dp, zero overlap)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Manager Assign Vendor action (Workflow 6)
              if (currentRole == UserRole.MANAGER && item.assignedVendorId == null) {
                Button(
                  onClick = { viewModel.activeAssignVendorCase.value = item },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = TutsvilleSecondary),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier
                    .height(40.dp)
                    .testTag("assign_vendor_case_${item.id.lowercase()}")
                ) {
                  Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Assign Vendor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }

              // Status Advance (Vendor or Manager)
              if (item.status != CaseStatus.RESOLVED && (currentRole == UserRole.VENDOR || currentRole == UserRole.MANAGER)) {
                OutlinedButton(
                  onClick = { viewModel.advanceCaseStatus(item.id, item.status) },
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, TutsvilleOutline),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier
                    .height(40.dp)
                    .testTag("advance_status_${item.id.lowercase()}")
                ) {
                  Text(
                    text = when (item.status) {
                      CaseStatus.OPEN -> "Mark Assigned"
                      CaseStatus.ASSIGNED -> "Start Work"
                      CaseStatus.IN_PROGRESS -> "Mark Resolved"
                      CaseStatus.RESOLVED -> "Resolved"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TutsvillePrimary
                  )
                }
              }

              // Resident Review Action (Workflow 5)
              if (item.status == CaseStatus.RESOLVED && item.rating == null) {
                Button(
                  onClick = { viewModel.activeReviewCase.value = item },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier
                    .height(40.dp)
                    .testTag("review_service_${item.id.lowercase()}")
                ) {
                  Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Review Service", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
              }
            }
          }
        }
      }
    }

    // Report Case FAB using standardized Tutsville style
    TutsvilleFloatingActionButton(
      text = "Report Issue",
      icon = Icons.Default.AddAlert,
      onClick = { viewModel.activeReportCaseDialog.value = true },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 20.dp),
      testTag = "report_case_fab"
    )
  }
}
