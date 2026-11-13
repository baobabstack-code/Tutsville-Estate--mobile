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
import com.example.tutsville.data.model.EstateDateUtils
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
  val currentRole by viewModel.currentRole.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var filterExpiringOnly by remember { mutableStateOf(false) }

  var editingContract by remember { mutableStateOf<ContractItem?>(null) }
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
                text = "Contracts",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = "Manage tenant lease agreements, vendor SLAs, expiry terms & contract amounts",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }

            if (currentRole == com.example.tutsville.data.model.UserRole.MANAGER) {
              Button(
                onClick = { viewModel.activeAddContractDialog.value = true },
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
            placeholder = { Text("Search contracts...", fontSize = 13.sp) },
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
              text = "No contracts match the search & filter criteria.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      } else {
        items(filteredContracts, key = { it.id }) { contract ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard {
              // Top Row: Contract Number & Urgency Pill
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                StatusPill(
                  text = "Contract #${contract.number}",
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

              Spacer(modifier = Modifier.height(8.dp))

              // 6 Core Contract Fields Spec Box
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFAF7F2),
                border = BorderStroke(1.dp, TutsvilleOutline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column {
                      Text("TENANT", fontSize = 9.sp, color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                      Text(contract.tenant, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                      Text("UNIT", fontSize = 9.sp, color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                      Text(contract.unit, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))
                  HorizontalDivider(color = TutsvilleOutline.copy(alpha = 0.4f))
                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column {
                      Text("START DATE", fontSize = 9.sp, color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                      Text(contract.startDate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TutsvilleOnBackground)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                      Text("EXPIRY DATE", fontSize = 9.sp, color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                      Text(contract.expiryDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (contract.daysRemaining <= 60) Color(0xFFC62828) else TutsvillePrimary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                      Text("CONTRACT AMOUNT", fontSize = 9.sp, color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                      Text(contract.contractAmount, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                    }
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

    // Edit Contract Modal
    editingContract?.let { current ->
      var title by remember(current.id) { mutableStateOf(current.title) }
      var party by remember(current.id) { mutableStateOf(current.counterparty) }
      var value by remember(current.id) { mutableStateOf(current.annualValue) }
      var startDate by remember(current.id) { mutableStateOf(current.startDate) }
      var expiry by remember(current.id) { mutableStateOf(current.expiryDate) }
      var showStartPicker by remember { mutableStateOf(false) }
      var showExpiryPicker by remember { mutableStateOf(false) }

      val isFormValid = title.trim().length >= 2 &&
        party.trim().isNotBlank() &&
        value.trim().isNotBlank() &&
        expiry.trim().isNotBlank()

      AlertDialog(
        onDismissRequest = { editingContract = null },
        title = { Text("Edit Contract Record", fontWeight = FontWeight.Bold, color = TutsvillePrimary) },
        text = {
          Column(
            modifier = Modifier
              .verticalScroll(rememberScrollState())
              .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = title,
              onValueChange = { title = it },
              label = { Text("Contract Title *") },
              placeholder = { Text("Enter contract title") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = party,
              onValueChange = { party = it },
              label = { Text("Counterparty / Tenant Name *") },
              placeholder = { Text("Enter counterparty or tenant") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = value,
              onValueChange = { value = it },
              label = { Text("Annual Value / Contract Amount *") },
              placeholder = { Text("e.g. $45,000 / yr") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = startDate,
              onValueChange = { startDate = it },
              label = { Text("Start Date") },
              placeholder = { Text("Select or enter start date") },
              trailingIcon = {
                IconButton(onClick = { showStartPicker = true }) {
                  Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Start Date", tint = TutsvillePrimary)
                }
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = expiry,
              onValueChange = { expiry = it },
              label = { Text("Expiry Date *") },
              placeholder = { Text("Select or enter expiry date") },
              trailingIcon = {
                IconButton(onClick = { showExpiryPicker = true }) {
                  Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Expiry Date", tint = TutsvillePrimary)
                }
              },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val daysLeft = EstateDateUtils.daysUntilDisplay(expiry) ?: current.daysRemaining
              viewModel.updateContract(
                contractId = current.id,
                title = title,
                counterparty = party,
                type = current.type,
                annualValue = value,
                startDate = startDate,
                expiryDate = expiry,
                daysRemaining = daysLeft,
                renewalTerms = current.renewalTerms,
                contactPerson = current.contactPerson,
                contactEmail = current.contactEmail
              )
              editingContract = null
            },
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Save Contract", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = { editingContract = null },
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Cancel", color = TutsvillePrimary)
          }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
      )

      if (showStartPicker) {
        ContractScreenDatePickerDialog(
          onDismiss = { showStartPicker = false },
          onDateSelected = { formatted ->
            startDate = formatted
            showStartPicker = false
          }
        )
      }
      if (showExpiryPicker) {
        ContractScreenDatePickerDialog(
          onDismiss = { showExpiryPicker = false },
          onDateSelected = { formatted ->
            expiry = formatted
            showExpiryPicker = false
          }
        )
      }
    }

    // Delete Confirmation Modal
    deletingContract?.let { c ->
      DeleteConfirmDialog(
        title = "Delete Contract?",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractScreenDatePickerDialog(
  onDismiss: () -> Unit,
  onDateSelected: (String) -> Unit
) {
  val datePickerState = rememberDatePickerState()
  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          val millis = datePickerState.selectedDateMillis
          if (millis != null) {
            val utcFmt = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).apply {
              timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            onDateSelected(utcFmt.format(java.util.Date(millis)))
          } else {
            onDismiss()
          }
        }
      ) {
        Text("Confirm", fontWeight = FontWeight.Bold, color = TutsvillePrimary)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TutsvilleOnSurfaceVariant)
      }
    }
  ) {
    DatePicker(state = datePickerState)
  }
}
