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
import com.example.tutsville.data.model.*
import com.example.tutsville.ui.components.DataColumn
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.InteractiveDataTable
import com.example.ui.theme.*
import com.example.tutsville.ui.viewmodel.EstateViewModel

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

  var editingCase by remember { mutableStateOf<CaseItem?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingCase by remember { mutableStateOf<CaseItem?>(null) }

  val filteredCases = remember(cases, currentRole, currentTenant, categoryFilter) {
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
    list
  }

  val columns = remember(viewModel) {
    listOf(
      DataColumn<CaseItem>(
        header = "Case #",
        widthDp = 100,
        getValue = { it.id },
        cellContent = { c ->
          Text(
            text = c.id,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvilleSecondary
          )
        }
      ),
      DataColumn<CaseItem>(
        header = "Location / Unit",
        widthDp = 140,
        getValue = { it.location },
        cellContent = { c ->
          Text(
            text = c.location,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        }
      ),
      DataColumn<CaseItem>(
        header = "Category",
        widthDp = 120,
        getValue = { it.category.label },
        cellContent = { c ->
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = TutsvilleSecondaryContainer
          ) {
            Text(
              text = c.category.label,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = TutsvilleOnSecondaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      ),
      DataColumn<CaseItem>(
        header = "Priority",
        widthDp = 100,
        getValue = { it.priority.label },
        cellContent = { c ->
          val isUrgent = c.priority == CasePriority.EMERGENCY || c.priority == CasePriority.HIGH
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isUrgent) StatusUrgentBg else StatusPendingBg
          ) {
            Text(
              text = c.priority.label.split(" ")[0],
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (isUrgent) StatusUrgentText else StatusPendingText,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      ),
      DataColumn<CaseItem>(
        header = "Reported By",
        widthDp = 130,
        getValue = { it.reportedBy },
        cellContent = { c ->
          Text(text = c.reportedBy, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<CaseItem>(
        header = "Reported Date",
        widthDp = 110,
        getValue = { it.reportedDate },
        cellContent = { c ->
          Text(text = c.reportedDate, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<CaseItem>(
        header = "Assigned Vendor",
        widthDp = 140,
        getValue = { it.assignedVendorName ?: "Unassigned" },
        cellContent = { c ->
          Text(
            text = c.assignedVendorName ?: "Unassigned",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (c.assignedVendorName != null) GoldAccent else StatusPendingText
          )
        }
      ),
      DataColumn<CaseItem>(
        header = "Status",
        widthDp = 100,
        getValue = { it.status.label },
        cellContent = { c ->
          val isDone = c.status == CaseStatus.RESOLVED
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isDone) StatusCompletedBg else StatusPendingBg
          ) {
            Text(
              text = c.status.label,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (isDone) StatusCompletedText else StatusPendingText,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      ),
      DataColumn<CaseItem>(
        header = "Actions",
        widthDp = 110,
        cellContent = { c ->
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (currentRole == UserRole.MANAGER) {
              IconButton(
                onClick = { viewModel.activeAssignVendorCase.value = c },
                modifier = Modifier.size(28.dp).testTag("btn_assign_case_${c.id.lowercase()}")
              ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Assign Vendor", tint = TutsvillePrimary, modifier = Modifier.size(16.dp))
              }
            }

            IconButton(
              onClick = { editingCase = c },
              modifier = Modifier.size(28.dp).testTag("btn_edit_case_${c.id.lowercase()}")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Case", tint = TutsvilleSecondary, modifier = Modifier.size(16.dp))
            }

            IconButton(
              onClick = { deletingCase = c },
              modifier = Modifier.size(28.dp).testTag("btn_delete_case_${c.id.lowercase()}")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Case", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
            }
          }
        }
      )
    )
  }

  Box(modifier = modifier.fillMaxSize()) {
    InteractiveDataTable(
      title = if (currentRole == UserRole.TENANT) "My Unit Maintenance Requests" else "Incident Work Orders Ledger",
      subtitle = "Full row-and-column ledger of work order tickets, priority SLAs, contractor assignments & resolution status",
      items = filteredCases,
      columns = columns,
      searchQuery = searchQuery,
      onSearchChange = { searchQuery = it },
      onAddNew = { viewModel.activeReportCaseDialog.value = true },
      addNewLabel = "+ Report Repair",
      getItemKey = { it.id }
    )

    // Add / Edit Case Modal Form
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
        onConfirm = { deletingCase = null },
        onDismiss = { deletingCase = null }
      )
    }
  }
}
