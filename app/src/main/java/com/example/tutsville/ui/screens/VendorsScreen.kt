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
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.VendorItem
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun VendorsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val vendors by viewModel.vendors.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var specialtyFilter by remember { mutableStateOf<CaseCategory?>(null) }

  var editingVendor by remember { mutableStateOf<VendorItem?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingVendor by remember { mutableStateOf<VendorItem?>(null) }

  val filteredVendors = remember(vendors, searchQuery, specialtyFilter) {
    vendors.filter { v ->
      val matchesFilter = specialtyFilter == null || v.specialty == specialtyFilter
      val matchesSearch = if (searchQuery.isBlank()) true
      else {
        v.name.contains(searchQuery, ignoreCase = true) ||
        v.specialty.label.contains(searchQuery, ignoreCase = true) ||
        v.phone.contains(searchQuery, ignoreCase = true) ||
        v.email.contains(searchQuery, ignoreCase = true)
      }
      matchesFilter && matchesSearch
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("vendors_card_list"),
      contentPadding = PaddingValues(bottom = 96.dp)
    ) {
      // 1. Header & Telemetry
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Certified Vendors Directory",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = "Approved contractor partners, emergency phone lines, SLA ratings & active work orders",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }

            if (currentRole == com.example.tutsville.data.model.UserRole.MANAGER) {
              Button(
                onClick = { isAddingNew = true },
                colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp).testTag("btn_onboard_vendor_card_view")
              ) {
                Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Add Vendor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Text("APPROVED PARTNERS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant))
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
                Text("AVG SLA RATING", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = GoldAccent))
                Text("4.9 ★", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
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
            placeholder = { Text("Search contractor name, specialty, phone...", fontSize = 13.sp) },
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
              selected = specialtyFilter == null,
              onClick = { specialtyFilter = null },
              label = { Text("All Vendors (${vendors.size})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = specialtyFilter == CaseCategory.HVAC,
              onClick = { specialtyFilter = if (specialtyFilter == CaseCategory.HVAC) null else CaseCategory.HVAC },
              label = { Text("HVAC", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = specialtyFilter == CaseCategory.PLUMBING,
              onClick = { specialtyFilter = if (specialtyFilter == CaseCategory.PLUMBING) null else CaseCategory.PLUMBING },
              label = { Text("Plumbing", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      // 2. Vendor Cards List
      if (filteredVendors.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No certified vendors match the search criteria.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      } else {
        items(filteredVendors, key = { it.id }) { vendor ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard {
              // Top Header Row: Company Title, Specialty Pill & SLA Score
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                StatusPill(
                  text = vendor.specialty.label,
                  bgColor = TutsvilleSecondaryContainer,
                  textColor = TutsvilleOnSecondaryContainer,
                  dotColor = TutsvilleSecondary
                )

                Text(
                  text = "${vendor.rating} ★ (${vendor.reviewCount} reviews)",
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
                text = "Lead Tech: ${vendor.leadTech} • ${vendor.activeJobsCount} Active Jobs",
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
                if (currentRole == com.example.tutsville.data.model.UserRole.MANAGER) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Button(
                      onClick = { /* Dispatch */ },
                      shape = RoundedCornerShape(8.dp),
                      colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                      modifier = Modifier.weight(1f).height(38.dp).testTag("btn_dispatch_vendor_${vendor.id.lowercase()}")
                    ) {
                      Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Dispatch Ticket", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                      onClick = { editingVendor = vendor },
                      modifier = Modifier.size(38.dp).testTag("btn_edit_vendor_card_${vendor.id.lowercase()}")
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit Vendor", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                      onClick = { deletingVendor = vendor },
                      modifier = Modifier.size(38.dp).testTag("btn_delete_vendor_card_${vendor.id.lowercase()}")
                    ) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete Vendor", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                    }
                  }
                }
            }
          }
        }
      }
    }

    // Add / Edit Vendor Modal
    if (isAddingNew || editingVendor != null) {
      val isEdit = editingVendor != null
      val current = editingVendor

      var name by remember { mutableStateOf(current?.name ?: "") }
      var phone by remember { mutableStateOf(current?.phone ?: "") }
      var email by remember { mutableStateOf(current?.email ?: "") }

      AlertDialog(
        onDismissRequest = {
          isAddingNew = false
          editingVendor = null
        },
        title = { Text(if (isEdit) "Edit Vendor Details" else "Onboard Certified Contractor", fontWeight = FontWeight.Bold, color = TutsvillePrimary) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("Company / Contractor Name") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text("Emergency Dispatch Phone") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text("Business Email") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.submitNewVendor(
                name = name.ifEmpty { "Apex HVAC Solutions" },
                specialty = CaseCategory.HVAC,
                phone = phone.ifEmpty { "+1 (555) 019-9944" },
                email = email.ifEmpty { "dispatch@apexhvac.com" },
                leadTech = "Julian Vance"
              )
              isAddingNew = false
              editingVendor = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(if (isEdit) "Save Vendor" else "Onboard Vendor", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = {
              isAddingNew = false
              editingVendor = null
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
    deletingVendor?.let { v ->
      DeleteConfirmDialog(
        title = "Delete Contractor Record?",
        message = "Are you sure you want to remove ${v.name} from the approved vendor list?",
        onConfirm = {
          viewModel.deleteVendor(v.id)
          deletingVendor = null
        },
        onDismiss = { deletingVendor = null }
      )
    }
  }
}
