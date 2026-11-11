package com.example.tutsville.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.ContractType
import com.example.ui.theme.*

@Composable
fun AddFacilityDialog(
  onDismiss: () -> Unit,
  onSubmit: (name: String, tag: String, capacity: Int, location: String, description: String, amenities: List<String>, slots: List<String>, rules: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var tag by remember { mutableStateOf("Grand Reception") }
  var capacity by remember { mutableStateOf("25") }
  var location by remember { mutableStateOf("Central Pavilion") }
  var description by remember { mutableStateOf("") }
  var amenitiesInput by remember { mutableStateOf("Catering Kitchen, Sound System, Wi-Fi") }
  var slotsInput by remember { mutableStateOf("10:00 AM - 02:00 PM, 03:00 PM - 07:00 PM") }
  var rules by remember { mutableStateOf("Residents must accompany guests. Music off at 11:00 PM.") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("dialog_add_facility")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Add Amenity / Facility",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Facility Name *") },
          placeholder = { Text("e.g. The Glasshouse Pavilion") },
          modifier = Modifier.fillMaxWidth().testTag("input_facility_name"),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = tag,
          onValueChange = { tag = it },
          label = { Text("Category Tag") },
          placeholder = { Text("e.g. Grand Reception & Dining") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = capacity,
            onValueChange = { capacity = it },
            label = { Text("Capacity") },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            modifier = Modifier.weight(1.5f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description") },
          placeholder = { Text("Architectural highlights and overview") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = amenitiesInput,
          onValueChange = { amenitiesInput = it },
          label = { Text("Included Amenities (comma separated)") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = slotsInput,
          onValueChange = { slotsInput = it },
          label = { Text("Available Booking Slots (comma separated)") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = rules,
          onValueChange = { rules = it },
          label = { Text("Rules & Guidelines") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = TutsvilleOnSurfaceVariant)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (name.isNotBlank()) {
                val amList = amenitiesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val slotList = slotsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val capInt = capacity.toIntOrNull() ?: 20
                onSubmit(name, tag, capInt, location, description, amList, slotList, rules)
              }
            },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            modifier = Modifier.testTag("button_submit_facility")
          ) {
            Text("Publish Facility", color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
fun AddVendorDialog(
  onDismiss: () -> Unit,
  onSubmit: (name: String, specialty: CaseCategory, phone: String, email: String, leadTech: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var specialty by remember { mutableStateOf(CaseCategory.HVAC) }
  var phone by remember { mutableStateOf("+1 (555) ") }
  var email by remember { mutableStateOf("") }
  var leadTech by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("dialog_add_vendor")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Onboard Approved Vendor",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Vendor / Contractor Name *") },
          placeholder = { Text("e.g. Apex Electrical Solutions") },
          modifier = Modifier.fillMaxWidth().testTag("input_vendor_name"),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text("Trade Discipline *", style = MaterialTheme.typography.labelMedium.copy(color = TutsvilleOnSurfaceVariant))
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(CaseCategory.HVAC, CaseCategory.ELECTRICAL, CaseCategory.PLUMBING).forEach { cat ->
            val sel = specialty == cat
            FilterChip(
              selected = sel,
              onClick = { specialty = cat },
              label = { Text(cat.label, fontSize = 11.sp) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = leadTech,
          onValueChange = { leadTech = it },
          label = { Text("Lead Technician / Contact Person") },
          placeholder = { Text("e.g. David Vance (Chief Master Tech)") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Dispatch Email") },
          placeholder = { Text("dispatch@company.com") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = TutsvilleOnSurfaceVariant)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (name.isNotBlank()) {
                onSubmit(name, specialty, phone, email, leadTech)
              }
            },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            modifier = Modifier.testTag("button_submit_vendor")
          ) {
            Text("Onboard Partner", color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
fun AddContractDialog(
  onDismiss: () -> Unit,
  onSubmit: (title: String, counterparty: String, type: ContractType, value: String, start: String, end: String, days: Int, terms: String, contact: String, contactEmail: String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var counterparty by remember { mutableStateOf("") }
  var type by remember { mutableStateOf(ContractType.VENDOR_SLA) }
  var annualValue by remember { mutableStateOf("$45,000 / yr") }
  var startDate by remember { mutableStateOf("Jan 01, 2026") }
  var expiryDate by remember { mutableStateOf("Dec 31, 2026") }
  var daysRemaining by remember { mutableStateOf("365") }
  var renewalTerms by remember { mutableStateOf("Requires 30 days written notice before automatic renewal.") }
  var contactPerson by remember { mutableStateOf("") }
  var contactEmail by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("dialog_add_contract")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Register SLA / Contract",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Contract Title *") },
          placeholder = { Text("e.g. Master Elevator Maintenance SLA") },
          modifier = Modifier.fillMaxWidth().testTag("input_contract_title"),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = counterparty,
          onValueChange = { counterparty = it },
          label = { Text("Counterparty / Provider *") },
          placeholder = { Text("e.g. Otis Vertical Transit") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = annualValue,
            onValueChange = { annualValue = it },
            label = { Text("Annual Value") },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          OutlinedTextField(
            value = daysRemaining,
            onValueChange = { daysRemaining = it },
            label = { Text("Days Left") },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            label = { Text("Start Date") },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          OutlinedTextField(
            value = expiryDate,
            onValueChange = { expiryDate = it },
            label = { Text("Expiry Date") },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = renewalTerms,
          onValueChange = { renewalTerms = it },
          label = { Text("Renewal Terms & SLAs") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = contactPerson,
          onValueChange = { contactPerson = it },
          label = { Text("Contact Person") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = contactEmail,
          onValueChange = { contactEmail = it },
          label = { Text("Contact Email") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel", color = TutsvilleOnSurfaceVariant)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (title.isNotBlank() && counterparty.isNotBlank()) {
                val daysInt = daysRemaining.toIntOrNull() ?: 365
                onSubmit(title, counterparty, type, annualValue, startDate, expiryDate, daysInt, renewalTerms, contactPerson, contactEmail)
              }
            },
            enabled = title.isNotBlank() && counterparty.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            modifier = Modifier.testTag("button_submit_contract")
          ) {
            Text("Register Contract", color = Color.White)
          }
        }
      }
    }
  }
}
