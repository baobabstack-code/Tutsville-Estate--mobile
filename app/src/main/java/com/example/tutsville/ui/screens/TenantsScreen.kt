package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.SendToMobile
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
import com.example.tutsville.data.model.TenantProfile
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun TenantsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val tenants by viewModel.tenants.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var tenantFilter by remember { mutableStateOf("ALL") } // ALL, ASSIGNED, UNASSIGNED, VERIFIED

  var editingTenant by remember { mutableStateOf<TenantProfile?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingTenant by remember { mutableStateOf<TenantProfile?>(null) }

  val filteredTenants = remember(tenants, searchQuery, tenantFilter) {
    tenants.filter { t ->
      val matchesFilter = when (tenantFilter) {
        "ASSIGNED" -> t.assignedPropertyId != null
        "UNASSIGNED" -> t.assignedPropertyId == null
        "VERIFIED" -> t.isAppRegistered
        else -> true
      }
      val matchesSearch = if (searchQuery.isBlank()) true
      else {
        t.fullName.contains(searchQuery, ignoreCase = true) ||
        t.email.contains(searchQuery, ignoreCase = true) ||
        t.phone.contains(searchQuery, ignoreCase = true) ||
        (t.assignedPropertyUnit ?: "").contains(searchQuery, ignoreCase = true) ||
        t.nationalId.contains(searchQuery, ignoreCase = true)
      }
      matchesFilter && matchesSearch
    }
  }

  val assignedCount = remember(tenants) { tenants.count { it.assignedPropertyId != null } }
  val verifiedCount = remember(tenants) { tenants.count { it.isAppRegistered } }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("tenants_card_list"),
      contentPadding = PaddingValues(bottom = 96.dp)
    ) {
      // 1. Header & Telemetry Summary
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Tenants",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = "Manage tenant profiles, national IDs, unit allocations & app access links",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }

            if (currentRole == com.example.tutsville.data.model.UserRole.MANAGER) {
              Button(
                onClick = { isAddingNew = true },
                colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp).testTag("btn_add_tenant_card_view")
              ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Add Tenant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Text("TOTAL RESIDENTS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant))
                Text("${tenants.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
                Text("$assignedCount allocated", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = StatusCompletedText))
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("APP ONBOARDED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant))
                Text("$verifiedCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TutsvilleSecondary))
                Text("${tenants.size - verifiedCount} pending invite", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = StatusPendingText))
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
              .testTag("input_search_tenants"),
            placeholder = { Text("Search tenants...", fontSize = 13.sp) },
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
              selected = tenantFilter == "ALL",
              onClick = { tenantFilter = "ALL" },
              label = { Text("All Tenants (${tenants.size})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = tenantFilter == "ASSIGNED",
              onClick = { tenantFilter = "ASSIGNED" },
              label = { Text("Assigned ($assignedCount)", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = tenantFilter == "UNASSIGNED",
              onClick = { tenantFilter = "UNASSIGNED" },
              label = { Text("Unassigned (${tenants.size - assignedCount})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = tenantFilter == "VERIFIED",
              onClick = { tenantFilter = "VERIFIED" },
              label = { Text("App Verified ($verifiedCount)", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      // 2. Tenant Cards Grid List
      if (filteredTenants.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No tenants match the search & filter criteria.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      } else {
        items(filteredTenants, key = { it.id }) { tenant ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard {
              // Top Header Row: Avatar, Name, Email, App Verified Pill
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Surface(
                    shape = CircleShape,
                    color = TutsvillePrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, TutsvillePrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.size(44.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = tenant.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(10.dp))

                  Column {
                    Text(
                      text = tenant.fullName,
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                    )
                    Text(
                      text = "${tenant.email} • ${tenant.phone}",
                      style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 11.sp)
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (tenant.isAppRegistered) StatusCompletedBg else StatusPendingBg,
                  border = BorderStroke(0.8.dp, if (tenant.isAppRegistered) Color(0xFFC4ECD4) else Color(0xFFF2DCAE))
                ) {
                  Text(
                    text = if (tenant.isAppRegistered) "App Verified ✓" else "Invite Dispatched ⏳",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 9.sp,
                      color = if (tenant.isAppRegistered) StatusCompletedText else StatusPendingText
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // National ID & Contact Info Pills
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
                    Icon(Icons.Default.Badge, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "National ID: ${tenant.nationalId.ifEmpty { "ID-9482701" }}",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
                    )
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = tenant.phone,
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvillePrimary, fontWeight = FontWeight.Bold)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Unit Allocation Banner
              if (tenant.assignedPropertyId != null) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = TutsvilleSecondaryContainer,
                  border = BorderStroke(1.dp, TutsvilleSecondary.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Home, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = tenant.assignedPropertyUnit ?: "Assigned Unit",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TutsvilleOnSecondaryContainer, fontSize = 11.sp)
                      )
                    }

                    TextButton(
                      onClick = { viewModel.unassignTenant(tenant.id) },
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                      modifier = Modifier.height(28.dp)
                    ) {
                      Text("Unassign", fontSize = 10.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                  }
                }
              } else {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = StatusUrgentBg,
                  border = BorderStroke(1.dp, Color(0xFFF0B3B3)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Warning, contentDescription = null, tint = StatusUrgentText, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "No property allocated",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = StatusUrgentText, fontSize = 11.sp)
                      )
                    }

                    TextButton(
                      onClick = { viewModel.activeAssignPropertyToTenant.value = tenant },
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                      modifier = Modifier.height(28.dp).testTag("assign_property_btn_${tenant.id.lowercase()}")
                    ) {
                      Text("Assign Unit →", fontSize = 10.sp, color = TutsvillePrimary, fontWeight = FontWeight.Bold)
                    }
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
                  onClick = { viewModel.activeTenantInviteSheet.value = tenant },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.weight(1f).height(36.dp).testTag("share_tenant_link_${tenant.id.lowercase()}")
                ) {
                  Icon(Icons.AutoMirrored.Filled.SendToMobile, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Share Invite & App Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (currentRole == com.example.tutsville.data.model.UserRole.MANAGER) {
                  IconButton(
                    onClick = { editingTenant = tenant },
                    modifier = Modifier.size(36.dp).testTag("btn_edit_tenant_card_${tenant.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Tenant", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                  }

                  IconButton(
                    onClick = { deletingTenant = tenant },
                    modifier = Modifier.size(36.dp).testTag("btn_delete_tenant_card_${tenant.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Tenant", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }
    }

    // Add / Edit Tenant Modal Form
    if (isAddingNew || editingTenant != null) {
      val isEdit = editingTenant != null
      val current = editingTenant

      var firstName by remember { mutableStateOf(current?.firstName ?: "") }
      var lastName by remember { mutableStateOf(current?.lastName ?: "") }
      var nationalId by remember { mutableStateOf(current?.nationalId ?: "ID-9482701") }
      var address by remember { mutableStateOf(current?.address ?: "") }
      var email by remember { mutableStateOf(current?.email ?: "") }
      var phone by remember { mutableStateOf(current?.phone ?: "") }

      AlertDialog(
        onDismissRequest = {
          isAddingNew = false
          editingTenant = null
        },
        title = {
          Text(
            text = if (isEdit) "Edit Tenant Profile" else "Add New Resident Tenant",
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
              value = firstName,
              onValueChange = { firstName = it },
              label = { Text("First Name") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = lastName,
              onValueChange = { lastName = it },
              label = { Text("Last Name") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = nationalId,
              onValueChange = { nationalId = it },
              label = { Text("National ID / Passport #") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = address,
              onValueChange = { address = it },
              label = { Text("Address / Unit") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = email,
              onValueChange = { email = it },
              label = { Text("Email Address") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text("Phone Number") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val fullName = "$firstName $lastName".trim()
              if (isEdit && current != null) {
                viewModel.createTenant(
                  fullName = fullName.ifEmpty { current.fullName },
                  email = email.ifEmpty { current.email },
                  phone = phone.ifEmpty { current.phone },
                  emergencyContactName = current.emergencyContactName,
                  emergencyContactPhone = current.emergencyContactPhone
                )
              } else {
                viewModel.createTenant(
                  fullName = fullName.ifEmpty { "New Resident" },
                  email = email.ifEmpty { "resident@estate.com" },
                  phone = phone.ifEmpty { "+1 555-0199" },
                  emergencyContactName = "Emergency Desk",
                  emergencyContactPhone = "+1 555-9999"
                )
              }
              isAddingNew = false
              editingTenant = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(if (isEdit) "Save Changes" else "Create Tenant", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = {
              isAddingNew = false
              editingTenant = null
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
    deletingTenant?.let { tenant ->
      DeleteConfirmDialog(
        title = "Delete Tenant?",
        message = "Are you sure you want to delete ${tenant.fullName} from the estate database? This action cannot be undone.",
        onConfirm = {
          viewModel.deleteTenant(tenant.id)
          deletingTenant = null
        },
        onDismiss = { deletingTenant = null }
      )
    }
  }
}
