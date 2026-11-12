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
import com.example.tutsville.data.model.ContractItem
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun ContractsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val contracts by viewModel.contracts.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var filterExpiringOnly by remember { mutableStateOf(false) }

  var editingContract by remember { mutableStateOf<ContractItem?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingContract by remember { mutableStateOf<ContractItem?>(null) }

  val filteredContracts = remember(contracts, searchQuery, filterExpiringOnly) {
    contracts.filter { c ->
      val matchesFilter = !filterExpiringOnly || c.daysRemaining <= 60
      val matchesSearch = if (searchQuery.isBlank()) true
      else {
        c.title.contains(searchQuery, ignoreCase = true) ||
        c.counterparty.contains(searchQuery, ignoreCase = true) ||
        c.annualValue.contains(searchQuery, ignoreCase = true)
      }
      matchesFilter && matchesSearch
    }
  }

  val expiringCount = remember(contracts) { contracts.count { it.daysRemaining <= 60 } }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("contracts_card_list"),
      contentPadding = PaddingValues(bottom = 96.dp)
    ) {
      // 1. Header & Financial SLA Telemetry
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Lease & Vendor Contracts Ledger",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = "Manage tenant lease agreements, vendor SLAs, expiry terms & annual financial valuations",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }

            Button(
              onClick = { isAddingNew = true },
              colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
              modifier = Modifier.height(40.dp).testTag("btn_add_contract_card_view")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("+ Add Contract", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Text("REGISTERED CONTRACTS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant))
                Text("${contracts.size}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text("EXPIRING SOON (<60d)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = StatusUrgentText))
                Text("$expiringCount", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = StatusUrgentText))
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
              .testTag("input_search_contracts"),
            placeholder = { Text("Search contract title, tenant, SLA terms...", fontSize = 13.sp) },
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
              selected = !filterExpiringOnly,
              onClick = { filterExpiringOnly = false },
              label = { Text("All Contracts (${contracts.size})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            FilterChip(
              selected = filterExpiringOnly,
              onClick = { filterExpiringOnly = true },
              label = { Text("Expiring Soon ($expiringCount)", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      // 2. Contract Cards Grid List
      if (filteredContracts.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No contract agreements match the search & filter criteria.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      } else {
        items(filteredContracts, key = { it.id }) { contract ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard {
              // Top Row: Contract #, Title, and Urgency Pill
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                StatusPill(
                  text = "CNT-${contract.id.takeLast(4)}",
                  bgColor = TutsvilleSecondaryContainer,
                  textColor = TutsvilleOnSecondaryContainer,
                  dotColor = TutsvilleSecondary
                )

                val isUrgent = contract.daysRemaining <= 60
                StatusPill(
                  text = if (isUrgent) "${contract.daysRemaining}d Left (Expiring)" else "${contract.daysRemaining}d Left (Active)",
                  bgColor = if (isUrgent) StatusUrgentBg else StatusCompletedBg,
                  textColor = if (isUrgent) StatusUrgentText else StatusCompletedText,
                  dotColor = if (isUrgent) Color(0xFFC62828) else StatusCompletedText
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = contract.title,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )

              Text(
                text = "Counterparty: ${contract.counterparty}",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary, fontWeight = FontWeight.Bold)
              )

              Spacer(modifier = Modifier.height(8.dp))

              // Financial Value & Expiry Term Info Box
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
                  Column {
                    Text("ANNUAL VALUE", fontSize = 9.sp, color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(contract.annualValue, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text("TERM EXPIRY", fontSize = 9.sp, color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(contract.expiryDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Card Action Buttons Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { viewModel.renewContract(contract.id) },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.weight(1f).height(38.dp).testTag("btn_renew_contract_${contract.id.lowercase()}")
                ) {
                  Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Renew Contract", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                  onClick = { editingContract = contract },
                  modifier = Modifier.size(38.dp).testTag("btn_edit_contract_card_${contract.id.lowercase()}")
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit Contract", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                  onClick = { deletingContract = contract },
                  modifier = Modifier.size(38.dp).testTag("btn_delete_contract_card_${contract.id.lowercase()}")
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Contract", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }
    }

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
        onConfirm = {
          viewModel.deleteContract(c.id)
          deletingContract = null
        },
        onDismiss = { deletingContract = null }
      )
    }
  }
}
