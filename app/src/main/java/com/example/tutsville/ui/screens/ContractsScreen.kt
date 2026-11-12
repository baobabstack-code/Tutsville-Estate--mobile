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
import com.example.tutsville.data.model.ContractItem
import com.example.tutsville.ui.components.DataColumn
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.InteractiveDataTable
import com.example.ui.theme.*
import com.example.tutsville.ui.viewmodel.EstateViewModel

@Composable
fun ContractsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val contracts by viewModel.contracts.collectAsState()
  var searchQuery by remember { mutableStateOf("") }

  var editingContract by remember { mutableStateOf<ContractItem?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingContract by remember { mutableStateOf<ContractItem?>(null) }

  val columns = remember(viewModel) {
    listOf(
      DataColumn<ContractItem>(
        header = "Contract #",
        widthDp = 120,
        getValue = { "CNT-${it.id.takeLast(4)}" },
        cellContent = { c ->
          Text(
            text = "CNT-${c.id.takeLast(4)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvilleSecondary
          )
        }
      ),
      DataColumn<ContractItem>(
        header = "Title / Subject",
        widthDp = 160,
        getValue = { it.title },
        cellContent = { c ->
          Text(
            text = c.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        }
      ),
      DataColumn<ContractItem>(
        header = "Counterparty / Tenant",
        widthDp = 150,
        getValue = { it.counterparty },
        cellContent = { c ->
          Text(text = c.counterparty, fontSize = 11.sp, color = TutsvilleOnBackground)
        }
      ),
      DataColumn<ContractItem>(
        header = "Contract Value",
        widthDp = 120,
        getValue = { it.annualValue },
        cellContent = { c ->
          Text(
            text = c.annualValue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent
          )
        }
      ),
      DataColumn<ContractItem>(
        header = "Start Date",
        widthDp = 110,
        getValue = { it.startDate },
        cellContent = { c ->
          Text(text = c.startDate, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<ContractItem>(
        header = "Expiry Date",
        widthDp = 110,
        getValue = { it.expiryDate },
        cellContent = { c ->
          Text(text = c.expiryDate, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<ContractItem>(
        header = "Term Status",
        widthDp = 120,
        getValue = { item -> "${item.daysRemaining} days" },
        cellContent = { c ->
          val isUrgent = c.daysRemaining <= 30
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isUrgent) StatusUrgentBg else StatusCompletedBg
          ) {
            Text(
              text = if (isUrgent) "${c.daysRemaining}d Left (Expiring)" else "${c.daysRemaining}d (Active)",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (isUrgent) StatusUrgentText else StatusCompletedText,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      ),
      DataColumn<ContractItem>(
        header = "Actions",
        widthDp = 110,
        cellContent = { c ->
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
              onClick = { editingContract = c },
              modifier = Modifier.size(28.dp).testTag("btn_edit_contract_${c.id.lowercase()}")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Contract", tint = TutsvillePrimary, modifier = Modifier.size(16.dp))
            }

            IconButton(
              onClick = { deletingContract = c },
              modifier = Modifier.size(28.dp).testTag("btn_delete_contract_${c.id.lowercase()}")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Contract", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
            }
          }
        }
      )
    )
  }

  Box(modifier = modifier.fillMaxSize()) {
    InteractiveDataTable(
      title = "Lease & Vendor Contracts Ledger",
      subtitle = "Full row-and-column ledger of tenant lease agreements, vendor SLAs, expiry dates & annual financial values",
      items = contracts,
      columns = columns,
      searchQuery = searchQuery,
      onSearchChange = { searchQuery = it },
      onAddNew = { isAddingNew = true },
      addNewLabel = "+ Add Contract",
      getItemKey = { it.id }
    )

    // Add / Edit Contract Modal
    if (isAddingNew || editingContract != null) {
      val isEdit = editingContract != null
      val current = editingContract

      var title by remember { mutableStateOf(current?.title ?: "") }
      var party by remember { mutableStateOf(current?.counterparty ?: "") }
      var value by remember { mutableStateOf(current?.annualValue ?: "$57,600/yr") }
      var expiry by remember { mutableStateOf(current?.expiryDate ?: "Dec 31, 2026") }

      AlertDialog(
        onDismissRequest = {
          isAddingNew = false
          editingContract = null
        },
        title = { Text(if (isEdit) "Edit Contract Record" else "Register New SLA / Lease Contract", fontWeight = FontWeight.Bold, color = TutsvillePrimary) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = title,
              onValueChange = { title = it },
              label = { Text("Contract Title") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = party,
              onValueChange = { party = it },
              label = { Text("Counterparty / Tenant Name") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = value,
              onValueChange = { value = it },
              label = { Text("Annual Value / Financial Amount") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = expiry,
              onValueChange = { expiry = it },
              label = { Text("Expiry Date") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              isAddingNew = false
              editingContract = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(if (isEdit) "Save Contract" else "Create Contract", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = {
              isAddingNew = false
              editingContract = null
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
    deletingContract?.let { c ->
      DeleteConfirmDialog(
        title = "Delete Contract Record?",
        message = "Are you sure you want to delete ${c.title} from the contract ledger?",
        onConfirm = { deletingContract = null },
        onDismiss = { deletingContract = null }
      )
    }
  }
}
