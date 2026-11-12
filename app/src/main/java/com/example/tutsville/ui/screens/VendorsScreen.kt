package com.example.tutsville.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.VendorItem
import com.example.tutsville.ui.components.DataColumn
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.InteractiveDataTable
import com.example.ui.theme.*
import com.example.tutsville.ui.viewmodel.EstateViewModel

@Composable
fun VendorsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val vendors by viewModel.vendors.collectAsState()
  var searchQuery by remember { mutableStateOf("") }

  var editingVendor by remember { mutableStateOf<VendorItem?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingVendor by remember { mutableStateOf<VendorItem?>(null) }

  val columns = remember(viewModel) {
    listOf(
      DataColumn<VendorItem>(
        header = "Vendor ID",
        widthDp = 100,
        getValue = { it.id },
        cellContent = { v ->
          Text(
            text = v.id,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvilleSecondary
          )
        }
      ),
      DataColumn<VendorItem>(
        header = "Company Name",
        widthDp = 160,
        getValue = { it.name },
        cellContent = { v ->
          Text(
            text = v.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        }
      ),
      DataColumn<VendorItem>(
        header = "Specialty / Discipline",
        widthDp = 140,
        getValue = { it.specialty.label },
        cellContent = { v ->
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = TutsvilleSecondaryContainer
          ) {
            Text(
              text = v.specialty.label,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = TutsvilleOnSecondaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      ),
      DataColumn<VendorItem>(
        header = "Phone Contact",
        widthDp = 120,
        getValue = { it.phone },
        cellContent = { v ->
          Text(text = v.phone, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<VendorItem>(
        header = "Email",
        widthDp = 150,
        getValue = { it.email },
        cellContent = { v ->
          Text(text = v.email, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<VendorItem>(
        header = "SLA Rating",
        widthDp = 110,
        getValue = { item -> "${item.rating} ★" },
        cellContent = { v ->
          Text(
            text = "${v.rating} ★ (${v.reviewCount})",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent
          )
        }
      ),
      DataColumn<VendorItem>(
        header = "Active Cases",
        widthDp = 100,
        getValue = { item -> "${item.activeJobsCount}" },
        cellContent = { v ->
          Text(
            text = "${v.activeJobsCount} active",
            fontSize = 11.sp,
            color = TutsvillePrimary,
            fontWeight = FontWeight.Bold
          )
        }
      ),
      DataColumn<VendorItem>(
        header = "Actions",
        widthDp = 110,
        cellContent = { v ->
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
              onClick = { editingVendor = v },
              modifier = Modifier.size(28.dp).testTag("btn_edit_vendor_${v.id.lowercase()}")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Vendor", tint = TutsvillePrimary, modifier = Modifier.size(16.dp))
            }

            IconButton(
              onClick = { deletingVendor = v },
              modifier = Modifier.size(28.dp).testTag("btn_delete_vendor_${v.id.lowercase()}")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Vendor", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
            }
          }
        }
      )
    )
  }

  Box(modifier = modifier.fillMaxSize()) {
    InteractiveDataTable(
      title = "Certified Vendors Directory",
      subtitle = "Full row-and-column ledger of approved contractors, phone lines, SLA ratings & active work orders",
      items = vendors,
      columns = columns,
      searchQuery = searchQuery,
      onSearchChange = { searchQuery = it },
      onAddNew = { isAddingNew = true },
      addNewLabel = "+ Add Vendor",
      getItemKey = { it.id }
    )

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
        onConfirm = { deletingVendor = null },
        onDismiss = { deletingVendor = null }
      )
    }
  }
}
