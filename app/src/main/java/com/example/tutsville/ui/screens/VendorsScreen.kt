package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.EstateConfigOptions
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.data.model.VendorItem
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.ManageOptionsSubDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.components.TutsvilleFloatingActionButton
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val vendors by viewModel.vendors.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val configOptions by viewModel.estateConfigOptions.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var disciplineFilter by remember { mutableStateOf<String?>(null) }

  var editingVendor by remember { mutableStateOf<VendorItem?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingVendor by remember { mutableStateOf<VendorItem?>(null) }

  val filteredVendors = remember(vendors, searchQuery, disciplineFilter) {
    vendors.filter { v ->
      val matchesFilter = disciplineFilter == null || v.effectiveDiscipline.equals(disciplineFilter, ignoreCase = true)
      val matchesSearch = if (searchQuery.isBlank()) true
      else {
        v.name.contains(searchQuery, ignoreCase = true) ||
        v.effectiveDiscipline.contains(searchQuery, ignoreCase = true) ||
        v.phone.contains(searchQuery, ignoreCase = true) ||
        v.email.contains(searchQuery, ignoreCase = true)
      }
      matchesFilter && matchesSearch
    }
  }

  val totalActiveJobs = remember(vendors) { vendors.sumOf { it.activeJobsCount } }
  val disciplineChips = remember(configOptions.contractorDisciplines, vendors) {
    (configOptions.contractorDisciplines + vendors.map { it.effectiveDiscipline }).distinct()
  }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("vendors_card_list"),
      contentPadding = PaddingValues(bottom = 104.dp)
    ) {
      // 1. Header & Telemetry
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Contractors",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Approved contractors, emergency phone lines, disciplines & active jobs",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
            )
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
                Text("APPROVED CONTRACTORS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant))
                Text("${vendors.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("ACTIVE ASSIGNMENTS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = GoldAccent))
                Text("$totalActiveJobs", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
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
              .testTag("input_search_vendors"),
            placeholder = { Text("Search contractors...", fontSize = 13.sp) },
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

          // Discipline Filter Chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = disciplineFilter == null,
              onClick = { disciplineFilter = null },
              label = { Text("All Contractors (${vendors.size})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            disciplineChips.forEach { disc ->
              FilterChip(
                selected = disciplineFilter.equals(disc, ignoreCase = true),
                onClick = { disciplineFilter = if (disciplineFilter.equals(disc, ignoreCase = true)) null else disc },
                label = { Text(disc, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      // 2. Contractor Cards List
      if (filteredVendors.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No contractors match the search criteria.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      } else {
        items(filteredVendors, key = { it.id }) { vendor ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard {
              // Top Header Row: Discipline Pill & Active Jobs
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                StatusPill(
                  text = vendor.effectiveDiscipline,
                  bgColor = TutsvilleSecondaryContainer,
                  textColor = TutsvilleOnSecondaryContainer,
                  dotColor = TutsvilleSecondary
                )

                Text(
                  text = "${vendor.activeJobsCount} Active Jobs",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GoldAccent)
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = vendor.name,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )

              Text(
                text = "Lead Tech: ${vendor.leadTech.ifBlank { "Not specified" }} • ${vendor.activeJobsCount} Active Jobs",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary, fontWeight = FontWeight.SemiBold)
              )

              Spacer(modifier = Modifier.height(8.dp))

              // Phone & Email Contact Info Box
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFAF7F2),
                border = BorderStroke(1.dp, TutsvilleOutline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = TutsvillePrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = vendor.phone, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvillePrimary, fontWeight = FontWeight.Bold))
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = vendor.email, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant))
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Card Action Buttons Row
                if (currentRole == UserRole.MANAGER) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Button(
                      onClick = { viewModel.setSelectedTab(1) },
                      shape = RoundedCornerShape(8.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                      modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("btn_dispatch_vendor_${vendor.id.lowercase()}")
                    ) {
                      Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Dispatch via Cases", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                      onClick = { editingVendor = vendor },
                      modifier = Modifier.size(48.dp).testTag("btn_edit_vendor_card_${vendor.id.lowercase()}")
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit Contractor", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                      onClick = { deletingVendor = vendor },
                      modifier = Modifier.size(48.dp).testTag("btn_delete_vendor_card_${vendor.id.lowercase()}")
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Contractor", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                    }
                  }
                }
            }
          }
        }
      }
    }

    if (currentRole == UserRole.MANAGER) {
      TutsvilleFloatingActionButton(
        text = "Add Contractor",
        icon = Icons.Default.Engineering,
        onClick = { isAddingNew = true },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 20.dp, bottom = 20.dp),
        testTag = "fab_add_contractor"
      )
    }

    // Add / Edit Contractor Modal
    if (isAddingNew || editingVendor != null) {
      val isEdit = editingVendor != null
      val current = editingVendor
      val disciplineOptions = remember(configOptions.contractorDisciplines) {
        configOptions.contractorDisciplines.ifEmpty { EstateConfigOptions.DEFAULT_CONTRACTOR_DISCIPLINES }
      }

      var name by remember(current?.id, isAddingNew) { mutableStateOf(current?.name ?: "") }
      var selectedDiscipline by remember(current?.id, isAddingNew, disciplineOptions) {
        mutableStateOf(current?.effectiveDiscipline ?: disciplineOptions.firstOrNull().orEmpty())
      }
      var disciplineExpanded by remember { mutableStateOf(false) }
      var showManageDisciplines by remember { mutableStateOf(false) }
      var phone by remember(current?.id, isAddingNew) { mutableStateOf(current?.phone ?: "") }
      var email by remember(current?.id, isAddingNew) { mutableStateOf(current?.email ?: "") }
      var leadTech by remember(current?.id, isAddingNew) { mutableStateOf(current?.leadTech ?: "") }

      val isFormValid = name.trim().length >= 2 && phone.trim().isNotBlank()

      if (showManageDisciplines) {
        ManageOptionsSubDialog(
          title = "Manage Contractor Disciplines",
          options = disciplineOptions,
          placeholder = "e.g., Pool & Spa Maintenance",
          onDismiss = { showManageDisciplines = false },
          onSave = { updated ->
            viewModel.updateContractorDisciplines(updated)
            if (updated.isNotEmpty() && !updated.contains(selectedDiscipline)) {
              selectedDiscipline = updated.first()
            }
            showManageDisciplines = false
          }
        )
      }

      AlertDialog(
        onDismissRequest = {
          isAddingNew = false
          editingVendor = null
        },
        title = {
          Text(
            text = if (isEdit) "Edit Contractor Details" else "Add Contractor",
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        },
        text = {
          Column(
            modifier = Modifier
              .verticalScroll(rememberScrollState())
              .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("Company / Contractor Name *") },
              placeholder = { Text("Enter company or contractor name") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Discipline *",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              )
              TextButton(
                onClick = { showManageDisciplines = true },
                modifier = Modifier.heightIn(min = 48.dp).testTag("btn_contractors_manage_disciplines")
              ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manage Options", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
              }
            }
            ExposedDropdownMenuBox(
              expanded = disciplineExpanded,
              onExpandedChange = { disciplineExpanded = !disciplineExpanded }
            ) {
              OutlinedTextField(
                value = selectedDiscipline,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Discipline") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = disciplineExpanded) },
                modifier = Modifier
                  .menuAnchor()
                  .fillMaxWidth()
                  .testTag("dropdown_contractors_screen_discipline"),
                singleLine = true
              )
              ExposedDropdownMenu(
                expanded = disciplineExpanded,
                onDismissRequest = { disciplineExpanded = false }
              ) {
                disciplineOptions.forEach { option ->
                  DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                      selectedDiscipline = option
                      disciplineExpanded = false
                    }
                  )
                }
              }
            }
            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text("Emergency Dispatch Phone *") },
              placeholder = { Text("Enter phone number") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text("Business Email") },
              placeholder = { Text("name@example.com") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = leadTech,
              onValueChange = { leadTech = it },
              label = { Text("Lead Technician") },
              placeholder = { Text("Enter technician name") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val mappedCategory = CaseCategory.entries.find {
                it.label.equals(selectedDiscipline, ignoreCase = true) ||
                  selectedDiscipline.contains(it.name, ignoreCase = true)
              } ?: CaseCategory.STRUCTURAL
              if (isEdit && current != null) {
                viewModel.updateVendor(
                  vendorId = current.id,
                  name = name,
                  specialty = mappedCategory,
                  phone = phone,
                  email = email,
                  leadTech = leadTech,
                  discipline = selectedDiscipline
                )
              } else {
                viewModel.submitNewVendor(
                  name = name,
                  specialty = mappedCategory,
                  phone = phone,
                  email = email,
                  leadTech = leadTech,
                  discipline = selectedDiscipline
                )
              }
              isAddingNew = false
              editingVendor = null
            },
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.heightIn(min = 48.dp)
          ) {
            Text(if (isEdit) "Save Contractor" else "Add Contractor", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = {
              isAddingNew = false
              editingVendor = null
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.heightIn(min = 48.dp)
          ) {
            Text("Cancel", color = TutsvillePrimary)
          }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
      )
    }

    // Delete Confirmation Modal
    deletingVendor?.let { v ->
      DeleteConfirmDialog(
        title = "Delete Contractor?",
        message = "Are you sure you want to delete ${v.name} from the approved contractor list?",
        onConfirm = {
          viewModel.deleteVendor(v.id)
          deletingVendor = null
        },
        onDismiss = { deletingVendor = null }
      )
    }
  }
}
