package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.tutsville.data.model.*
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun CasesTableScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val cases by viewModel.cases.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val currentTenant by viewModel.currentTenant.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var categoryFilter by remember { mutableStateOf<CaseCategory?>(null) }
  var statusFilter by remember { mutableStateOf<CaseStatus?>(null) }

  var editingCase by remember { mutableStateOf<CaseItem?>(null) }
  var deletingCase by remember { mutableStateOf<CaseItem?>(null) }

  val filteredCases = remember(cases, currentRole, currentTenant, searchQuery, categoryFilter, statusFilter) {
    var list = if (currentRole == UserRole.TENANT) {
      val resUnit = currentTenant?.assignedPropertyUnit
      val resName = currentTenant?.fullName
      cases.filter { c ->
        (resUnit != null && c.location.contains(resUnit, ignoreCase = true)) ||
        (resName != null && c.reportedBy.equals(resName, ignoreCase = true))
      }
    } else cases

    if (categoryFilter != null) {
      list = list.filter { it.category == categoryFilter }
    }
    if (statusFilter != null) {
      list = list.filter { it.status == statusFilter }
    }
    if (searchQuery.isNotBlank()) {
      list = list.filter { c ->
        c.title.contains(searchQuery, ignoreCase = true) ||
        c.location.contains(searchQuery, ignoreCase = true) ||
        c.description.contains(searchQuery, ignoreCase = true) ||
        c.reportedBy.contains(searchQuery, ignoreCase = true) ||
        (c.assignedVendorName ?: "").contains(searchQuery, ignoreCase = true)
      }
    }
    list
  }

  val openCasesCount = remember(cases) { cases.count { it.status == CaseStatus.OPEN } }
  val assignedCasesCount = remember(cases) { cases.count { it.status == CaseStatus.ASSIGNED || it.status == CaseStatus.IN_PROGRESS } }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("cases_card_list"),
      contentPadding = PaddingValues(bottom = 96.dp)
    ) {
      // 1. Header & Priority Summary
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (currentRole == UserRole.TENANT) "My Unit Maintenance Requests" else "Incident Work Orders Ledger",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = "Triage repair requests, priority SLAs, vendor dispatch & resolution timeline",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }

            Button(
              onClick = { viewModel.activeReportCaseDialog.value = true },
              colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.height(40.dp).testTag("btn_report_repair_card_view")
            ) {
              Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("+ Report Repair", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Telemetry Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("OPEN TICKETS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = StatusPendingText))
                Text("$openCasesCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = StatusPendingText))
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("DISPATCHED / IN PROGRESS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = GoldAccent))
                Text("$assignedCasesCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Search Bar
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("input_search_cases"),
            placeholder = { Text("Search repair tickets, unit, priority, contractor...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TutsvilleSecondary) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Default.Close, contentDescription = "Clear", tint = TutsvilleOnSurfaceVariant)
                }
              }
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TutsvillePrimary,
              unfocusedBorderColor = TutsvilleOutline,
              focusedContainerColor = Color.White,
              unfocusedContainerColor = Color.White
            ),
            singleLine = true
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Filter Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = statusFilter == null,
              onClick = { statusFilter = null },
              label = { Text("All Work Orders (${filteredCases.size})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = statusFilter == CaseStatus.OPEN,
              onClick = { statusFilter = if (statusFilter == CaseStatus.OPEN) null else CaseStatus.OPEN },
              label = { Text("Open ($openCasesCount)", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = statusFilter == CaseStatus.ASSIGNED,
              onClick = { statusFilter = if (statusFilter == CaseStatus.ASSIGNED) null else CaseStatus.ASSIGNED },
              label = { Text("Dispatched", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      // 2. Case Cards List
      if (filteredCases.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No work order tickets match the search & filter criteria.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      } else {
        items(filteredCases, key = { it.id }) { caseItem ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard {
              // Top Header Row: Category, Priority Badge & Case ID
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  StatusPill(
                    text = caseItem.category.label,
                    bgColor = TutsvilleSecondaryContainer,
                    textColor = TutsvilleOnSecondaryContainer,
                    dotColor = TutsvilleSecondary
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  val isUrgent = caseItem.priority == CasePriority.EMERGENCY || caseItem.priority == CasePriority.HIGH
                  StatusPill(
                    text = caseItem.priority.label,
                    bgColor = if (isUrgent) StatusUrgentBg else StatusPendingBg,
                    textColor = if (isUrgent) StatusUrgentText else StatusPendingText,
                    dotColor = if (isUrgent) Color(0xFFC62828) else StatusPendingText
                  )
                }

                Text(
                  text = caseItem.id,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = caseItem.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              )

              Text(
                text = "Location: ${caseItem.location} • ${caseItem.reportedDate}",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary, fontWeight = FontWeight.SemiBold)
              )

              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = caseItem.description,
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnBackground)
              )

              Spacer(modifier = Modifier.height(10.dp))

              // Vendor Assignment & Status Banner
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFAF7F2),
                border = BorderStroke(1.dp, TutsvilleOutline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Engineering, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Contractor: ${caseItem.assignedVendorName ?: "Unassigned (Awaiting Triage)"}",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvillePrimary, fontWeight = FontWeight.Bold)
                    )
                  }

                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (caseItem.status == CaseStatus.RESOLVED) StatusCompletedBg else StatusPendingBg
                  ) {
                    Text(
                      text = caseItem.status.label,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (caseItem.status == CaseStatus.RESOLVED) StatusCompletedText else StatusPendingText,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Card Action Buttons Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                if (currentRole == UserRole.MANAGER) {
                  Button(
                    onClick = { viewModel.activeAssignVendorCase.value = caseItem },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp).testTag("btn_dispatch_vendor_card_${caseItem.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dispatch Vendor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }

                IconButton(
                  onClick = { editingCase = caseItem },
                  modifier = Modifier.size(38.dp).testTag("btn_edit_case_card_${caseItem.id.lowercase()}")
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit Ticket", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                  onClick = { deletingCase = caseItem },
                  modifier = Modifier.size(38.dp).testTag("btn_delete_case_card_${caseItem.id.lowercase()}")
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Ticket", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }

    // Edit Case Modal
    if (editingCase != null) {
      val c = editingCase!!
      var title by remember { mutableStateOf(c.title) }
      var desc by remember { mutableStateOf(c.description) }
      var loc by remember { mutableStateOf(c.location) }

      AlertDialog(
        onDismissRequest = { editingCase = null },
        title = { Text("Edit Case Ticket #${c.id}", fontWeight = FontWeight.Bold, color = TutsvillePrimary) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = title,
              onValueChange = { title = it },
              label = { Text("Title") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = loc,
              onValueChange = { loc = it },
              label = { Text("Location") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = desc,
              onValueChange = { desc = it },
              label = { Text("Description") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = { editingCase = null },
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Save Ticket", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(onClick = { editingCase = null }, shape = RoundedCornerShape(8.dp)) {
            Text("Cancel", color = TutsvillePrimary)
          }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
      )
    }

    // Delete Confirmation Modal
    deletingCase?.let { c ->
      DeleteConfirmDialog(
        title = "Delete Case Ticket?",
        message = "Are you sure you want to delete Case #${c.id} (${c.title})?",
        onConfirm = {
          viewModel.deleteCase(c.id)
          deletingCase = null
        },
        onDismiss = { deletingCase = null }
      )
    }
  }
}
