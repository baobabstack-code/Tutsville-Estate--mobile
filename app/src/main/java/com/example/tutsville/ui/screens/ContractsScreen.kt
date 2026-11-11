package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.tutsville.data.model.ContractType
import com.example.tutsville.data.model.ContractUrgency
import com.example.tutsville.ui.components.ContractRundownBanner
import com.example.tutsville.ui.components.ContractUrgencyBadge
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun ContractsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val contracts by viewModel.contracts.collectAsState()
  val filter by viewModel.contractFilter.collectAsState()

  val filteredContracts = remember(contracts, filter) {
    if (filter == null) contracts else contracts.filter { it.type == filter }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground),
    contentPadding = PaddingValues(bottom = 90.dp)
  ) {
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
          text = "Contracts & Master SLAs",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        )
        Text(
          text = "Stewardship ledger for vendor agreements, residential leases & equipment warranties",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = { viewModel.activeAddContractDialog.value = true },
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("button_open_add_contract")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Register Contract", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = { viewModel.activeAddVendorDialog.value = true },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, TutsvillePrimary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("button_open_add_vendor")
          ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = TutsvillePrimary)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Onboard Vendor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
          }
        }
      }
    }

    // Workflow 8: Prominent Expiration Warning / Rundown Countdown Banner
    item {
      ContractRundownBanner(
        contracts = contracts,
        onTriggerAlert = { viewModel.triggerContractRundownNotice(it) },
        onViewContract = { viewModel.activeContractDetail.value = it }
      )
    }

    // Filter Chips
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilterChip(
          selected = filter == null,
          onClick = { viewModel.setContractFilter(null) },
          label = { Text("All Contracts (${contracts.size})") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TutsvillePrimary,
            selectedLabelColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp)
        )
        ContractType.values().forEach { type ->
          val count = contracts.count { it.type == type }
          FilterChip(
            selected = filter == type,
            onClick = { viewModel.setContractFilter(if (filter == type) null else type) },
            label = { Text("${type.label} ($count)") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = TutsvillePrimary,
              selectedLabelColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
          )
        }
      }
    }

    items(filteredContracts, key = { it.id }) { contract ->
      Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        OperationalCard(
          onClick = { viewModel.activeContractDetail.value = contract }
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = TutsvilleBackground,
              border = BorderStroke(1.dp, TutsvilleOutline)
            ) {
              Text(
                text = contract.type.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  color = TutsvillePrimary
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
            ContractUrgencyBadge(urgency = contract.urgency, days = contract.daysRemaining)
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

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Counterparty: ${contract.counterparty}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
          )

          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Value: ${contract.annualValue}",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TutsvilleSecondary
              )
            )
            Text(
              text = "Expires: ${contract.expiryDate}",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = if (contract.daysRemaining <= 30) StatusUrgentText else TutsvilleOnSurfaceVariant
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = contract.renewalTerms,
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 12.sp,
              color = TutsvilleOnSurfaceVariant
            )
          )

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (contract.urgency != ContractUrgency.HEALTHY) {
              OutlinedButton(
                onClick = { viewModel.triggerContractRundownNotice(contract.id) },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (contract.daysRemaining <= 14) Color(0xFFF0B3B3) else TutsvilleOutline),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp).testTag("trigger_alert_${contract.id.lowercase()}")
              ) {
                Icon(
                  Icons.Default.NotificationsActive,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = if (contract.daysRemaining <= 14) StatusUrgentText else TutsvillePrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (contract.isNotificationSent) "Alert Sent" else "Send Alert",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (contract.daysRemaining <= 14) StatusUrgentText else TutsvillePrimary
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
            }

            Button(
              onClick = { viewModel.activeContractDetail.value = contract },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
              modifier = Modifier.height(34.dp).testTag("view_contract_${contract.id.lowercase()}")
            ) {
              Text("Review & Renew", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
