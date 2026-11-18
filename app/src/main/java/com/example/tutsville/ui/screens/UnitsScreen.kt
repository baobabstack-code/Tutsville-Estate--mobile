package com.example.tutsville.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.ContractDocumentMeta
import com.example.tutsville.data.model.EstateConfigOptions
import com.example.tutsville.data.model.EstateDateUtils
import com.example.tutsville.data.model.PropertyArchitecture
import com.example.tutsville.data.model.PropertyItem
import com.example.tutsville.data.model.PropertyStatus
import com.example.tutsville.data.model.TenantProfile
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.ui.components.AutofilledTenantSummaryCard
import com.example.tutsville.ui.components.ContractDocAttachmentRow
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.ManageOptionsSubDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.components.TutsvilleFloatingActionButton
import com.example.tutsville.ui.components.openAttachedDocument
import com.example.tutsville.ui.components.resolveDocumentMeta
import com.example.tutsville.ui.components.resolveTenantAssignmentPrefill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val properties by viewModel.properties.collectAsState()
  val tenants by viewModel.tenants.collectAsState()
  val contracts by viewModel.contracts.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val configOptions by viewModel.estateConfigOptions.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf<PropertyStatus?>(null) }

  var editingProperty by remember { mutableStateOf<PropertyItem?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingProperty by remember { mutableStateOf<PropertyItem?>(null) }

  val filteredProperties = remember(properties, searchQuery, statusFilter) {
    properties.filter { prop ->
      val matchesFilter = statusFilter == null || prop.status == statusFilter
      val matchesSearch = if (searchQuery.isBlank()) true
      else {
        prop.unitNumber.contains(searchQuery, ignoreCase = true) ||
        prop.architecture.floorLevel.contains(searchQuery, ignoreCase = true) ||
        prop.monthlyRental.contains(searchQuery, ignoreCase = true) ||
        (prop.assignedTenant?.name ?: "").contains(searchQuery, ignoreCase = true)
      }
      matchesFilter && matchesSearch
    }
  }

  val occupiedCount = remember(properties) { properties.count { it.status == PropertyStatus.OCCUPIED } }
  val availableCount = remember(properties) { properties.count { it.status == PropertyStatus.AVAILABLE } }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("units_card_list"),
      contentPadding = PaddingValues(bottom = 104.dp)
    ) {
      // 1. Header & Occupancy Telemetry
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Units",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Manage unit inventory, floor elevation specs, size (sq m) & lease rates",
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
                Text("TOTAL UNITS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant))
                Text("${properties.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("OCCUPIED / LEASED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = StatusCompletedText))
                Text("$occupiedCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = StatusCompletedText))
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("AVAILABLE TO LEASE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = GoldAccent))
                Text("$availableCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
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
              .testTag("input_search_units"),
            placeholder = { Text("Search units...", fontSize = 13.sp) },
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
              label = { Text("All Units (${properties.size})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = statusFilter == PropertyStatus.AVAILABLE,
              onClick = { statusFilter = PropertyStatus.AVAILABLE },
              label = { Text("Available ($availableCount)", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = statusFilter == PropertyStatus.OCCUPIED,
              onClick = { statusFilter = PropertyStatus.OCCUPIED },
              label = { Text("Occupied ($occupiedCount)", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      // 2. Unit Operational Cards Grid List
      if (filteredProperties.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No units match the search & filter criteria.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      } else {
        items(filteredProperties, key = { it.id }) { property ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard(
              border = BorderStroke(
                1.dp,
                if (property.status == PropertyStatus.AVAILABLE) GoldAccent.copy(alpha = 0.6f) else TutsvilleOutline
              )
            ) {
              // Header Row: Unit Name & Status Pill
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = property.unitNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontFamily = FontFamily.Serif,
                      fontWeight = FontWeight.Bold,
                      color = TutsvillePrimary
                    )
                  )
                }

                if (property.status == PropertyStatus.AVAILABLE) {
                  StatusPill(
                    text = "Available to Lease",
                    bgColor = StatusPendingBg,
                    textColor = StatusPendingText,
                    dotColor = GoldAccent
                  )
                } else {
                  StatusPill(
                    text = "Occupied / Leased",
                    bgColor = StatusCompletedBg,
                    textColor = StatusCompletedText,
                    dotColor = Color(0xFF2E7D32)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Architectural Specs Grid (Beds, Baths, Sq m, Elevation)
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF9F7F2),
                border = BorderStroke(1.dp, TutsvilleOutline),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BEDS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                    Text("${property.architecture.bedrooms}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  }
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("BATHS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                    Text("${property.architecture.bathrooms.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  }
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SQ M", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                    Text("${property.architecture.effectiveSqMeters}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  }
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ELEVATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                    Text(property.architecture.floorLevel.ifBlank { "—" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Monthly Rent & Tenant Info Box
              if (property.status == PropertyStatus.AVAILABLE) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text("MONTHLY RENT VALUATION", fontSize = 9.sp, color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(property.monthlyRental, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                  }

                  Button(
                    onClick = { viewModel.activeAssignTenantProperty.value = property },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp).testTag("btn_assign_tenant_unit_${property.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assign to Tenant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              } else {
                val tenant = property.assignedTenant
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = TutsvilleSecondaryContainer,
                  border = BorderStroke(1.dp, TutsvilleSecondary.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = tenant?.name ?: "Resident",
                          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvilleOnSecondaryContainer)
                        )
                      }

                      TextButton(
                        onClick = { viewModel.vacateProperty(property.id) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                      ) {
                        Text("Vacate Unit", fontSize = 10.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                      }
                    }
                    Text(
                      text = "Email: ${tenant?.email ?: ""} • Lease: ${tenant?.leaseStartDate ?: ""}",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSecondaryContainer.copy(alpha = 0.85f))
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Card Action Buttons Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { viewModel.activeShareInviteDialog.value = property },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1f).height(36.dp).testTag("share_unit_invite_${property.id.lowercase()}")
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Share Unit Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (currentRole == UserRole.MANAGER) {
                  IconButton(
                    onClick = { editingProperty = property },
                    modifier = Modifier.size(48.dp).testTag("btn_edit_unit_card_${property.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Unit", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                  }

                  IconButton(
                    onClick = { deletingProperty = property },
                    modifier = Modifier.size(48.dp).testTag("btn_delete_unit_card_${property.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Unit", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
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
        text = "Add Unit",
        icon = Icons.Default.AddHome,
        onClick = { isAddingNew = true },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 20.dp, bottom = 20.dp),
        testTag = "fab_add_unit"
      )
    }

    // Add / Edit Property Modal
    if (isAddingNew || editingProperty != null) {
      val isEdit = editingProperty != null
      val current = editingProperty
      val elevationOptions = remember(configOptions.floorElevations) {
        configOptions.floorElevations.ifEmpty { EstateConfigOptions.DEFAULT_FLOOR_ELEVATIONS }
      }
      val sortedTenants = remember(tenants) {
        tenants.sortedWith(compareBy<TenantProfile> { it.assignedPropertyId != null }.thenBy { it.fullName.lowercase() })
      }

      var unitNumber by remember(current?.id, isAddingNew) { mutableStateOf(current?.unitNumber ?: "") }
      var sqMeters by remember(current?.id, isAddingNew) {
        mutableStateOf(current?.architecture?.effectiveSqMeters?.takeIf { it > 0 }?.toString() ?: "")
      }
      var bedrooms by remember(current?.id, isAddingNew) {
        mutableStateOf(current?.architecture?.bedrooms?.takeIf { it > 0 }?.toString() ?: "")
      }
      var bathrooms by remember(current?.id, isAddingNew) {
        mutableStateOf(
          current?.architecture?.bathrooms?.takeIf { it > 0f }?.let {
            if (it % 1f == 0f) it.toInt().toString() else it.toString()
          } ?: ""
        )
      }
      var floorLevel by remember(current?.id, isAddingNew, elevationOptions) {
        mutableStateOf(
          current?.architecture?.floorLevel?.takeIf { it.isNotBlank() }
            ?: elevationOptions.firstOrNull().orEmpty()
        )
      }
      var elevationExpanded by remember { mutableStateOf(false) }
      var showManageElevations by remember { mutableStateOf(false) }
      var rent by remember(current?.id, isAddingNew) { mutableStateOf(current?.monthlyRental ?: "") }

      // Optional tenant assignment during new unit creation
      var assignMode by remember(current?.id, isAddingNew) { mutableStateOf("NONE") }
      var selectedTenant by remember(current?.id, isAddingNew) { mutableStateOf<TenantProfile?>(null) }
      var tenantPickerExpanded by remember { mutableStateOf(false) }
      var tenantName by remember(current?.id, isAddingNew) { mutableStateOf("") }
      var tenantEmail by remember(current?.id, isAddingNew) { mutableStateOf("") }
      var leaseStartDate by remember(current?.id, isAddingNew) { mutableStateOf("") }
      var showLeaseStartPicker by remember { mutableStateOf(false) }
      var leaseDurationMonths by remember(current?.id, isAddingNew) { mutableStateOf("12") }
      var signedContractDoc by remember(current?.id, isAddingNew) { mutableStateOf<ContractDocumentMeta?>(null) }
      var codeOfConductDoc by remember(current?.id, isAddingNew) { mutableStateOf<ContractDocumentMeta?>(null) }

      val signedDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
          try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
          } catch (_: Exception) {}
          signedContractDoc = resolveDocumentMeta(context, uri)
        }
      }
      val conductDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
          try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
          } catch (_: Exception) {}
          codeOfConductDoc = resolveDocumentMeta(context, uri)
        }
      }

      if (showLeaseStartPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
          onDismissRequest = { showLeaseStartPicker = false },
          confirmButton = {
            TextButton(onClick = {
              pickerState.selectedDateMillis?.let { millis ->
                val fmt = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).apply {
                  timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                leaseStartDate = fmt.format(java.util.Date(millis))
              }
              showLeaseStartPicker = false
            }) { Text("Select", color = TutsvillePrimary, fontWeight = FontWeight.Bold) }
          },
          dismissButton = {
            TextButton(onClick = { showLeaseStartPicker = false }) { Text("Cancel") }
          }
        ) { DatePicker(state = pickerState) }
      }

      val tenantAssignmentValid = isEdit || assignMode == "NONE" || (
        tenantName.isNotBlank() &&
          tenantEmail.contains("@") &&
          leaseStartDate.isNotBlank() &&
          signedContractDoc != null
        )
      val isFormValid = unitNumber.trim().isNotBlank() && rent.trim().isNotBlank() && tenantAssignmentValid

      if (showManageElevations) {
        ManageOptionsSubDialog(
          title = "Manage Floor Elevations",
          options = elevationOptions,
          placeholder = "e.g., 4 Story",
          onDismiss = { showManageElevations = false },
          onSave = { updated ->
            viewModel.updateFloorElevations(updated)
            if (updated.isNotEmpty() && !updated.contains(floorLevel)) {
              floorLevel = updated.first()
            }
            showManageElevations = false
          }
        )
      }

      AlertDialog(
        onDismissRequest = {
          isAddingNew = false
          editingProperty = null
        },
        title = {
          Text(
            text = if (isEdit) "Edit Unit Specifications" else "List New Unit",
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
              value = unitNumber,
              onValueChange = { unitNumber = it },
              label = { Text("Unit Number *") },
              placeholder = { Text("e.g. Unit 101") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = sqMeters,
              onValueChange = { sqMeters = it },
              label = { Text("Size (sq m)") },
              placeholder = { Text("e.g. 165") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = bedrooms,
                onValueChange = { bedrooms = it },
                label = { Text("Bedrooms") },
                placeholder = { Text("e.g. 2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = bathrooms,
                onValueChange = { bathrooms = it },
                label = { Text("Bathrooms") },
                placeholder = { Text("e.g. 2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Floor Elevation",
                style = MaterialTheme.typography.labelMedium.copy(color = TutsvilleOnSurfaceVariant)
              )
              TextButton(
                onClick = { showManageElevations = true },
                modifier = Modifier.heightIn(min = 48.dp).testTag("btn_units_manage_elevations")
              ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manage Options", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
              }
            }
            ExposedDropdownMenuBox(
              expanded = elevationExpanded,
              onExpandedChange = { elevationExpanded = !elevationExpanded }
            ) {
              OutlinedTextField(
                value = floorLevel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select Floor Elevation") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = elevationExpanded) },
                modifier = Modifier
                  .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                  .fillMaxWidth()
                  .testTag("dropdown_unit_screen_elevation"),
                singleLine = true
              )
              ExposedDropdownMenu(
                expanded = elevationExpanded,
                onDismissRequest = { elevationExpanded = false }
              ) {
                elevationOptions.forEach { option ->
                  DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                      floorLevel = option
                      elevationExpanded = false
                    }
                  )
                }
              }
            }
            OutlinedTextField(
              value = rent,
              onValueChange = { rent = it },
              label = { Text("Monthly Rent Valuation *") },
              placeholder = { Text("e.g. $3,500 / mo") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            if (!isEdit) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Assign Tenant (Optional)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              )
              ExposedDropdownMenuBox(
                expanded = tenantPickerExpanded,
                onExpandedChange = { tenantPickerExpanded = !tenantPickerExpanded }
              ) {
                val pickerText = when (assignMode) {
                  "EXISTING" -> selectedTenant?.let { "${it.fullName} (${it.email})" } ?: "Select Existing Tenant"
                  "MANUAL" -> "Enter New Tenant Manually"
                  else -> "Leave Vacant (Assign Later)"
                }
                OutlinedTextField(
                  value = pickerText,
                  onValueChange = {},
                  readOnly = true,
                  label = { Text("Select Existing Tenant or Leave Vacant") },
                  leadingIcon = { Icon(Icons.Default.PersonSearch, contentDescription = null, tint = TutsvillePrimary) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tenantPickerExpanded) },
                  singleLine = true,
                  modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth()
                    .testTag("unit_create_tenant_picker")
                )
                ExposedDropdownMenu(
                  expanded = tenantPickerExpanded,
                  onDismissRequest = { tenantPickerExpanded = false }
                ) {
                  DropdownMenuItem(
                    text = { Text("Leave Vacant (Assign Later)") },
                    onClick = {
                      assignMode = "NONE"
                      selectedTenant = null
                      tenantName = ""
                      tenantEmail = ""
                      leaseStartDate = ""
                      signedContractDoc = null
                      codeOfConductDoc = null
                      tenantPickerExpanded = false
                    }
                  )
                  sortedTenants.forEach { t ->
                    DropdownMenuItem(
                      text = {
                        Column {
                          Text(t.fullName, fontWeight = FontWeight.SemiBold, color = TutsvillePrimary)
                          Text(
                            text = "${t.email} • ${t.assignedPropertyUnit?.let { "Unit: $it" } ?: "Unassigned"}",
                            fontSize = 11.sp,
                            color = TutsvilleOnSurfaceVariant
                          )
                        }
                      },
                      onClick = {
                        val prefill = resolveTenantAssignmentPrefill(t, contracts)
                        assignMode = "EXISTING"
                        selectedTenant = t
                        tenantName = prefill.tenantName
                        tenantEmail = prefill.tenantEmail
                        leaseStartDate = prefill.leaseStartDate
                        leaseDurationMonths = prefill.durationMonths
                        signedContractDoc = prefill.signedContractDoc
                        codeOfConductDoc = prefill.codeOfConductDoc
                        tenantPickerExpanded = false
                      }
                    )
                  }
                  DropdownMenuItem(
                    text = { Text("+ Enter New Tenant Manually", color = GoldAccent, fontWeight = FontWeight.Bold) },
                    onClick = {
                      assignMode = "MANUAL"
                      selectedTenant = null
                      tenantName = ""
                      tenantEmail = ""
                      leaseStartDate = EstateDateUtils.todayDisplay()
                      leaseDurationMonths = "12"
                      signedContractDoc = null
                      codeOfConductDoc = null
                      tenantPickerExpanded = false
                    }
                  )
                }
              }

              if (assignMode != "NONE") {
                selectedTenant?.let { loaded ->
                  AutofilledTenantSummaryCard(
                    tenant = loaded,
                    onClear = {
                      assignMode = "NONE"
                      selectedTenant = null
                      tenantName = ""
                      tenantEmail = ""
                      leaseStartDate = ""
                      signedContractDoc = null
                      codeOfConductDoc = null
                    }
                  )
                }
                OutlinedTextField(
                  value = tenantName,
                  onValueChange = { tenantName = it },
                  label = { Text("Tenant Full Name *") },
                  placeholder = { Text("Enter tenant full name") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("unit_create_tenant_name")
                )
                OutlinedTextField(
                  value = tenantEmail,
                  onValueChange = { tenantEmail = it },
                  label = { Text("Tenant Email Address *") },
                  placeholder = { Text("name@example.com") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("unit_create_tenant_email")
                )
                OutlinedTextField(
                  value = leaseStartDate,
                  onValueChange = { leaseStartDate = it },
                  label = { Text("Lease Start Date *") },
                  placeholder = { Text("Select or enter start date") },
                  trailingIcon = {
                    IconButton(onClick = { showLeaseStartPicker = true }) {
                      Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Lease Start Date", tint = TutsvillePrimary)
                    }
                  },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("unit_create_lease_start")
                )
                Text(
                  text = "Lease Duration",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  listOf("6 Months", "12 Months", "24 Months").forEach { dur ->
                    val num = dur.split(" ")[0]
                    FilterChip(
                      selected = leaseDurationMonths == num,
                      onClick = { leaseDurationMonths = num },
                      label = { Text(dur, fontSize = 11.sp) },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TutsvillePrimary,
                        selectedLabelColor = Color.White
                      )
                    )
                  }
                }
                ContractDocAttachmentRow(
                  label = "Signed Contract",
                  doc = signedContractDoc,
                  canAttach = true,
                  isRequired = true,
                  onOpen = { doc -> openAttachedDocument(context, doc) },
                  onAttach = { signedDocLauncher.launch(arrayOf("application/pdf", "image/*", "*/*")) },
                  tagPrefix = "unit_create_signed_contract"
                )
                ContractDocAttachmentRow(
                  label = "Code of Conduct",
                  doc = codeOfConductDoc,
                  canAttach = true,
                  onOpen = { doc -> openAttachedDocument(context, doc) },
                  onAttach = { conductDocLauncher.launch(arrayOf("application/pdf", "image/*", "*/*")) },
                  tagPrefix = "unit_create_code_of_conduct"
                )
                if (signedContractDoc == null) {
                  Text(
                    text = "Signed contract document is required when assigning a tenant.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFFC62828))
                  )
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val sqmInt = sqMeters.trim().toIntOrNull() ?: 0
              val bedsInt = bedrooms.trim().toIntOrNull() ?: 0
              val bathsFloat = bathrooms.trim().toFloatOrNull() ?: 0f
              val arch = PropertyArchitecture(
                bedrooms = bedsInt,
                bathrooms = bathsFloat,
                sqFt = sqmInt,
                floorLevel = floorLevel.trim(),
                viewType = current?.architecture?.viewType ?: "",
                sqMeters = sqmInt
              )

              if (isEdit && current != null) {
                viewModel.updateProperty(
                  propertyId = current.id,
                  unitNumber = unitNumber,
                  pavilionWing = "",
                  architecture = arch,
                  parkingSpots = emptyList(),
                  amenities = current.amenities,
                  monthlyRental = rent
                )
              } else {
                val months = leaseDurationMonths.toIntOrNull() ?: 12
                val leaseEnd = if (assignMode != "NONE") EstateDateUtils.addMonthsDisplay(leaseStartDate, months) else null
                viewModel.listProperty(
                  unitNumber = unitNumber,
                  pavilionWing = "",
                  architecture = arch,
                  parkingSpots = emptyList(),
                  amenities = emptyList(),
                  monthlyRental = rent,
                  assignTenantName = if (assignMode != "NONE") tenantName.trim() else null,
                  assignTenantEmail = if (assignMode != "NONE") tenantEmail.trim() else null,
                  leaseStartDate = if (assignMode != "NONE") leaseStartDate.trim() else null,
                  leaseEndDate = leaseEnd,
                  signedContractDoc = if (assignMode != "NONE") signedContractDoc else null,
                  codeOfConductDoc = if (assignMode != "NONE") codeOfConductDoc else null
                )
              }
              isAddingNew = false
              editingProperty = null
            },
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.heightIn(min = 48.dp)
          ) {
            Text(
              if (isEdit) "Save Changes" else if (assignMode != "NONE") "Add & Assign Unit" else "Add Unit",
              fontWeight = FontWeight.Bold
            )
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = {
              isAddingNew = false
              editingProperty = null
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
    deletingProperty?.let { prop ->
      DeleteConfirmDialog(
        title = "Delete Unit?",
        message = "Are you sure you want to delete unit ${prop.unitNumber} from the estate inventory?",
        onConfirm = {
          viewModel.deleteProperty(prop.id)
          deletingProperty = null
        },
        onDismiss = { deletingProperty = null }
      )
    }
  }
}
