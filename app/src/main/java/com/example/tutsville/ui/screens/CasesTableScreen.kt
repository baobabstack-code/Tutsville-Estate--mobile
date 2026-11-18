package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.example.tutsville.ui.components.TutsvilleFloatingActionButton
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasesTableScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val cases by viewModel.cases.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val currentTenant by viewModel.currentTenant.collectAsState()
  val currentUserEmail by viewModel.currentUserEmail.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var categoryFilter by remember { mutableStateOf<CaseCategory?>(null) }
  var statusFilter by remember { mutableStateOf<CaseStatus?>(null) }

  var editingCase by remember { mutableStateOf<CaseItem?>(null) }
  var deletingCase by remember { mutableStateOf<CaseItem?>(null) }

  val roleScopedCases = remember(cases, currentRole, currentTenant, currentUserEmail) {
    if (currentRole == UserRole.TENANT) {
      val resUnit = currentTenant?.assignedPropertyUnit
      val resName = currentTenant?.fullName
      val resEmail = currentTenant?.email ?: currentUserEmail
      cases.filter { c ->
        (!resUnit.isNullOrBlank() && c.location.contains(resUnit, ignoreCase = true)) ||
          (!resName.isNullOrBlank() && c.reportedBy.equals(resName, ignoreCase = true)) ||
          (!resEmail.isNullOrBlank() && c.tenantEmail.equals(resEmail, ignoreCase = true))
      }
    } else cases
  }

  val filteredCases = remember(roleScopedCases, searchQuery, categoryFilter, statusFilter) {
    var list = roleScopedCases
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

  val openCasesCount = remember(roleScopedCases) { roleScopedCases.count { it.status == CaseStatus.OPEN } }
  val assignedCasesCount = remember(roleScopedCases) {
    roleScopedCases.count { it.status == CaseStatus.ASSIGNED || it.status == CaseStatus.IN_PROGRESS }
  }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("cases_card_list"),
      contentPadding = PaddingValues(bottom = 104.dp)
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
                text = "Cases",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = "Triage cases, priority SLAs, contractor dispatch & resolution timeline",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }

            Button(
              onClick = { viewModel.activeReportCaseDialog.value = true },
              colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("btn_report_repair_card_view")
            ) {
              Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("+ Report Case", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            placeholder = { Text("Search cases...", fontSize = 13.sp) },
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
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = statusFilter == null,
              onClick = { statusFilter = null },
              label = { Text("All Cases (${roleScopedCases.size})", fontSize = 11.sp) },
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
            FilterChip(
              selected = statusFilter == CaseStatus.IN_PROGRESS,
              onClick = { statusFilter = if (statusFilter == CaseStatus.IN_PROGRESS) null else CaseStatus.IN_PROGRESS },
              label = { Text("In Progress", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = statusFilter == CaseStatus.RESOLVED,
              onClick = { statusFilter = if (statusFilter == CaseStatus.RESOLVED) null else CaseStatus.RESOLVED },
              label = { Text("Resolved", fontSize = 11.sp) },
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
              text = "No cases match the search & filter criteria.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      } else {
        items(filteredCases, key = { it.id }) { caseItem ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard {
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

              if (!caseItem.preferredDate.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Preferred Completion: ${caseItem.preferredDate}",
                  style = MaterialTheme.typography.labelSmall.copy(color = TutsvillePrimary, fontWeight = FontWeight.SemiBold)
                )
              }

              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = caseItem.description,
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnBackground)
              )

              Spacer(modifier = Modifier.height(10.dp))

              // Contractor Assignment & Status Banner
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
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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

              if (caseItem.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = TutsvilleBackground,
                  border = BorderStroke(1.dp, TutsvilleOutline),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                      text = "Notes Timeline (${caseItem.notes.size})",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                    )
                    caseItem.notes.forEach { n ->
                      Text(
                        text = "• ${n.text} (${n.authorName.ifBlank { "Manager" }}, ${n.createdAt})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Role-aware Lifecycle & Action Buttons Row
              val isTenantOwner = currentRole == UserRole.TENANT && (
                (!currentUserEmail.isNullOrBlank() && caseItem.tenantEmail.equals(currentUserEmail, ignoreCase = true)) ||
                  (!currentTenant?.fullName.isNullOrBlank() && caseItem.reportedBy.equals(currentTenant?.fullName, ignoreCase = true))
                )
              val canEditDelete = currentRole == UserRole.MANAGER || (isTenantOwner && caseItem.status == CaseStatus.OPEN)

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (currentRole == UserRole.MANAGER) {
                  when (caseItem.status) {
                    CaseStatus.OPEN -> {
                      Button(
                        onClick = { viewModel.activeAssignVendorCase.value = caseItem },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                          .weight(1f)
                          .heightIn(min = 48.dp)
                          .testTag("btn_dispatch_vendor_card_${caseItem.id.lowercase()}")
                      ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dispatch Contractor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                    CaseStatus.ASSIGNED -> {
                      Button(
                        onClick = { viewModel.advanceCaseStatus(caseItem.id, caseItem.status) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                          .weight(1f)
                          .heightIn(min = 48.dp)
                          .testTag("btn_advance_case_card_${caseItem.id.lowercase()}")
                      ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Work", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                      OutlinedButton(
                        onClick = { viewModel.activeAssignVendorCase.value = caseItem },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                      ) {
                        Text("Reassign", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TutsvillePrimary)
                      }
                    }
                    CaseStatus.IN_PROGRESS -> {
                      Button(
                        onClick = { viewModel.advanceCaseStatus(caseItem.id, caseItem.status) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                          .weight(1f)
                          .heightIn(min = 48.dp)
                          .testTag("btn_resolve_case_card_${caseItem.id.lowercase()}")
                      ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Resolved", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                    CaseStatus.RESOLVED -> {
                      StatusPill(
                        text = "Resolved",
                        bgColor = StatusCompletedBg,
                        textColor = StatusCompletedText,
                        dotColor = StatusCompletedText
                      )
                      Spacer(modifier = Modifier.weight(1f))
                    }
                  }

                  OutlinedButton(
                    onClick = { viewModel.activeAddNoteCase.value = caseItem },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                      .heightIn(min = 48.dp)
                      .testTag("btn_add_note_card_${caseItem.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.NoteAdd, contentDescription = null, tint = TutsvillePrimary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Note", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  }
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }

                if (canEditDelete) {
                  IconButton(
                    onClick = { editingCase = caseItem },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("btn_edit_case_card_${caseItem.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Ticket", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                  }

                  IconButton(
                    onClick = { deletingCase = caseItem },
                    modifier = Modifier
                      .size(48.dp)
                      .testTag("btn_delete_case_card_${caseItem.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Ticket", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }
    }

    TutsvilleFloatingActionButton(
      text = "Report Case",
      icon = Icons.Default.AddAlert,
      onClick = { viewModel.activeReportCaseDialog.value = true },
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 20.dp),
      testTag = "fab_report_case_cases"
    )

    // Edit Case Modal
    editingCase?.let { c ->
      var title by remember(c.id) { mutableStateOf(c.title) }
      var desc by remember(c.id) { mutableStateOf(c.description) }
      var loc by remember(c.id) { mutableStateOf(c.location) }
      var category by remember(c.id) { mutableStateOf(c.category) }
      var priority by remember(c.id) { mutableStateOf(c.priority) }
      var preferredDate by remember(c.id) { mutableStateOf(c.preferredDate.orEmpty()) }
      var showPreferredPicker by remember { mutableStateOf(false) }
      val isFormValid = title.trim().length >= 3 && loc.trim().isNotBlank()

      AlertDialog(
        onDismissRequest = { editingCase = null },
        title = { Text("Edit Case #${c.id}", fontWeight = FontWeight.Bold, color = TutsvillePrimary) },
        text = {
          Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = title,
              onValueChange = { title = it },
              label = { Text("Summary *") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_case_title_input")
            )
            OutlinedTextField(
              value = loc,
              onValueChange = { if (currentRole != UserRole.TENANT) loc = it },
              readOnly = currentRole == UserRole.TENANT,
              label = { Text("Unit / Location *") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_case_location_input")
            )
            OutlinedTextField(
              value = desc,
              onValueChange = { if (currentRole != UserRole.MANAGER) desc = it },
              readOnly = currentRole == UserRole.MANAGER,
              label = { Text(if (currentRole == UserRole.MANAGER) "Description (Resident Original)" else "Description") },
              minLines = 2,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_case_description_input")
            )
            OutlinedTextField(
              value = preferredDate,
              onValueChange = {},
              readOnly = true,
              label = { Text("Preferred Completion Date (Optional)") },
              placeholder = { Text("Select date") },
              trailingIcon = {
                Row {
                  if (preferredDate.isNotEmpty()) {
                    IconButton(onClick = { preferredDate = "" }) {
                      Icon(Icons.Default.Close, contentDescription = "Clear Date", tint = TutsvilleOnSurfaceVariant)
                    }
                  }
                  IconButton(onClick = { showPreferredPicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = TutsvillePrimary)
                  }
                }
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_case_preferred_date_input")
            )

            Text(
              text = "Category Discipline",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
            )
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              CaseCategory.values().forEach { cat ->
                FilterChip(
                  selected = cat == category,
                  onClick = { category = cat },
                  label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                )
              }
            }

            Text(
              text = "Priority Level",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              CasePriority.values().forEach { prio ->
                val isSel = prio == priority
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (isSel) TutsvilleSecondary else TutsvilleBackground,
                  border = BorderStroke(1.dp, if (isSel) TutsvilleSecondary else TutsvilleOutline),
                  modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { priority = prio }
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = prio.name.take(3),
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) Color.White else TutsvilleOnSurfaceVariant
                      ),
                      modifier = Modifier.padding(vertical = 8.dp),
                      textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (isFormValid) {
                viewModel.updateCase(
                  caseId = c.id,
                  title = title.trim(),
                  description = desc.trim(),
                  location = loc.trim(),
                  category = category,
                  priority = priority,
                  preferredDate = preferredDate.trim().ifBlank { null }
                )
                editingCase = null
              }
            },
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.heightIn(min = 48.dp).testTag("btn_save_edited_case")
          ) {
            Text("Save Case", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = { editingCase = null },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.heightIn(min = 48.dp)
          ) {
            Text("Cancel", color = TutsvillePrimary)
          }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
      )

      if (showPreferredPicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
          onDismissRequest = { showPreferredPicker = false },
          confirmButton = {
            TextButton(
              onClick = {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                  val utcFmt = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                  }
                  preferredDate = utcFmt.format(java.util.Date(millis))
                }
                showPreferredPicker = false
              }
            ) {
              Text("Confirm", fontWeight = FontWeight.Bold, color = TutsvillePrimary)
            }
          },
          dismissButton = {
            TextButton(onClick = { showPreferredPicker = false }) {
              Text("Cancel", color = TutsvilleOnSurfaceVariant)
            }
          }
        ) {
          DatePicker(state = datePickerState)
        }
      }
    }

    // Delete Confirmation Modal
    deletingCase?.let { c ->
      DeleteConfirmDialog(
        title = "Delete Case?",
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
