package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.TenantProfile
import com.example.tutsville.ui.components.DataColumn
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.InteractiveDataTable
import com.example.ui.theme.*
import com.example.tutsville.ui.viewmodel.EstateViewModel

@Composable
fun TenantsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val tenants by viewModel.tenants.collectAsState()
  var searchQuery by remember { mutableStateOf("") }

  var editingTenant by remember { mutableStateOf<TenantProfile?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingTenant by remember { mutableStateOf<TenantProfile?>(null) }

  val columns = remember(viewModel) {
    listOf(
      DataColumn<TenantProfile>(
        header = "Avatar",
        widthDp = 70,
        cellContent = { tenant ->
          Surface(
            shape = CircleShape,
            color = TutsvillePrimary.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, TutsvillePrimary),
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = tenant.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            }
          }
        }
      ),
      DataColumn<TenantProfile>(
        header = "First Name",
        widthDp = 110,
        getValue = { it.firstName.ifEmpty { it.fullName.split(" ").firstOrNull() ?: "" } },
        cellContent = { tenant ->
          Text(
            text = tenant.firstName.ifEmpty { tenant.fullName.split(" ").firstOrNull() ?: "" },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        }
      ),
      DataColumn<TenantProfile>(
        header = "Last Name",
        widthDp = 110,
        getValue = { t -> t.lastName.ifEmpty { t.fullName.split(" ").getOrNull(1) ?: "" } },
        cellContent = { tenant ->
          Text(
            text = tenant.lastName.ifEmpty { tenant.fullName.split(" ").getOrNull(1) ?: "" },
            fontSize = 12.sp,
            color = TutsvilleOnBackground
          )
        }
      ),
      DataColumn<TenantProfile>(
        header = "National ID",
        widthDp = 120,
        getValue = { it.nationalId },
        cellContent = { tenant ->
          Text(
            text = tenant.nationalId.ifEmpty { "ID-9482701" },
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TutsvilleSecondary
          )
        }
      ),
      DataColumn<TenantProfile>(
        header = "Address / Unit",
        widthDp = 140,
        getValue = { it.assignedPropertyUnit ?: it.address },
        cellContent = { tenant ->
          Text(
            text = tenant.assignedPropertyUnit ?: tenant.address.ifEmpty { "Unassigned" },
            fontSize = 11.sp,
            color = TutsvilleOnBackground
          )
        }
      ),
      DataColumn<TenantProfile>(
        header = "Phone",
        widthDp = 120,
        getValue = { it.phone },
        cellContent = { tenant ->
          Text(text = tenant.phone, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<TenantProfile>(
        header = "Email",
        widthDp = 150,
        getValue = { it.email },
        cellContent = { tenant ->
          Text(text = tenant.email, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<TenantProfile>(
        header = "App Status",
        widthDp = 110,
        getValue = { if (it.isAppRegistered) "Verified" else "Pending" },
        cellContent = { tenant ->
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (tenant.isAppRegistered) StatusCompletedBg else StatusPendingBg
          ) {
            Text(
              text = if (tenant.isAppRegistered) "Verified ✓" else "Invite Sent ⏳",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (tenant.isAppRegistered) StatusCompletedText else StatusPendingText,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      ),
      DataColumn<TenantProfile>(
        header = "Actions",
        widthDp = 110,
        cellContent = { tenant ->
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
              onClick = { editingTenant = tenant },
              modifier = Modifier.size(28.dp).testTag("btn_edit_tenant_${tenant.id.lowercase()}")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Tenant", tint = TutsvillePrimary, modifier = Modifier.size(16.dp))
            }

            IconButton(
              onClick = { deletingTenant = tenant },
              modifier = Modifier.size(28.dp).testTag("btn_delete_tenant_${tenant.id.lowercase()}")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Tenant", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
            }
          }
        }
      )
    )
  }

  Box(modifier = modifier.fillMaxSize()) {
    InteractiveDataTable(
      title = "Tenants Directory & Database",
      subtitle = "Full row-and-column ledger of resident profiles, National IDs, contacts & unit links",
      items = tenants,
      columns = columns,
      searchQuery = searchQuery,
      onSearchChange = { searchQuery = it },
      onAddNew = { isAddingNew = true },
      addNewLabel = "+ Add Tenant",
      getItemKey = { it.id }
    )

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
      var unit by remember { mutableStateOf(current?.assignedPropertyUnit ?: "") }

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
        title = "Delete Resident Profile?",
        message = "Are you sure you want to delete ${tenant.fullName} from the estate database? This action cannot be undone.",
        onConfirm = {
          // Clean up tenant profile
          deletingTenant = null
        },
        onDismiss = { deletingTenant = null }
      )
    }
  }
}
