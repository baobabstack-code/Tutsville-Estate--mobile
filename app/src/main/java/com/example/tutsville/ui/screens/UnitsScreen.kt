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
import com.example.tutsville.data.model.PropertyArchitecture
import com.example.tutsville.data.model.PropertyItem
import com.example.tutsville.data.model.PropertyStatus
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun UnitsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val properties by viewModel.properties.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
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
        prop.pavilionWing.contains(searchQuery, ignoreCase = true) ||
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
      contentPadding = PaddingValues(bottom = 96.dp)
    ) {
      // 1. Header & Occupancy Telemetry
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Properties & Units Inventory",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = "Manage residential listings, floor elevation specs, parking spots & lease rates",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }

            if (currentRole == com.example.tutsville.data.model.UserRole.MANAGER) {
              Button(
                onClick = { isAddingNew = true },
                colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp).testTag("btn_list_property_card_view")
              ) {
                Icon(Icons.Default.AddHome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ List Property", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
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
            placeholder = { Text("Search by unit #, wing, tenant, or rent...", fontSize = 13.sp) },
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
              text = "No property units match the search & filter criteria.",
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
              // Header Row: Unit Name, Wing & Status Pill
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
                  Text(
                    text = property.pavilionWing,
                    style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary, fontWeight = FontWeight.SemiBold)
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

              // Architectural Specs Grid (Beds, Baths, Sq Ft, Elevation)
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
                    Text("SQ FT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                    Text("${property.architecture.sqFt}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  }
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ELEVATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                    Text(property.architecture.floorLevel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
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

                if (currentRole == com.example.tutsville.data.model.UserRole.MANAGER) {
                  IconButton(
                    onClick = { editingProperty = property },
                    modifier = Modifier.size(36.dp).testTag("btn_edit_unit_card_${property.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Unit", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                  }

                  IconButton(
                    onClick = { deletingProperty = property },
                    modifier = Modifier.size(36.dp).testTag("btn_delete_unit_card_${property.id.lowercase()}")
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

    // Add / Edit Property Modal
    if (isAddingNew || editingProperty != null) {
      val isEdit = editingProperty != null
      val current = editingProperty

      var unitNumber by remember { mutableStateOf(current?.unitNumber ?: "") }
      var wing by remember { mutableStateOf(current?.pavilionWing ?: "North Pavilion") }
      var sqFt by remember { mutableStateOf(current?.architecture?.sqFt?.toString() ?: "1850") }
      var bedrooms by remember { mutableStateOf(current?.architecture?.bedrooms?.toString() ?: "2") }
      var bathrooms by remember { mutableStateOf(current?.architecture?.bathrooms?.toInt()?.toString() ?: "2") }
      var rent by remember { mutableStateOf(current?.monthlyRental ?: "$3,500/mo") }

      AlertDialog(
        onDismissRequest = {
          isAddingNew = false
          editingProperty = null
        },
        title = {
          Text(
            text = if (isEdit) "Edit Unit Specifications" else "List New Residential Property",
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
              label = { Text("Unit Number (e.g. Tower A • Apt 402)") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = wing,
              onValueChange = { wing = it },
              label = { Text("Wing / Pavilion") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = sqFt,
              onValueChange = { sqFt = it },
              label = { Text("Size in Sq Ft") },
              modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = bedrooms,
                onValueChange = { bedrooms = it },
                label = { Text("Bedrooms") },
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = bathrooms,
                onValueChange = { bathrooms = it },
                label = { Text("Bathrooms") },
                modifier = Modifier.weight(1f)
              )
            }
            OutlinedTextField(
              value = rent,
              onValueChange = { rent = it },
              label = { Text("Monthly Rent Valuation") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val sqFtInt = sqFt.toIntOrNull() ?: 1850
              val bedsInt = bedrooms.toIntOrNull() ?: 2
              val bathsFloat = bathrooms.toFloatOrNull() ?: 2f

              viewModel.listProperty(
                unitNumber = unitNumber.ifEmpty { "Apt ${properties.size + 101}" },
                pavilionWing = wing,
                architecture = PropertyArchitecture(
                  bedrooms = bedsInt,
                  bathrooms = bathsFloat,
                  sqFt = sqFtInt,
                  floorLevel = "Upper Elevation",
                  viewType = "Garden View"
                ),
                parkingSpots = listOf("Bay P-01"),
                amenities = listOf("Balcony"),
                monthlyRental = rent
              )
              isAddingNew = false
              editingProperty = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(if (isEdit) "Save Changes" else "List Property", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = {
              isAddingNew = false
              editingProperty = null
            },
            shape = RoundedCornerShape(8.dp)
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
        title = "Delete Property Listing?",
        message = "Are you sure you want to remove unit ${prop.unitNumber} from the estate inventory?",
        onConfirm = {
          viewModel.deleteProperty(prop.id)
          deletingProperty = null
        },
        onDismiss = { deletingProperty = null }
      )
    }
  }
}
