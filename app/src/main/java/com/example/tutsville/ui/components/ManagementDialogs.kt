package com.example.tutsville.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.ContractType
import com.example.tutsville.data.model.EstateDateUtils
import com.example.ui.theme.*

@Composable
fun AddFacilityDialog(
  onDismiss: () -> Unit,
  onSubmit: (name: String, tag: String, capacity: Int, location: String, description: String, amenities: List<String>, slots: List<String>, rules: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var tag by remember { mutableStateOf("") }
  var capacity by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var amenitiesInput by remember { mutableStateOf("") }
  var slotsInput by remember { mutableStateOf("") }
  var rules by remember { mutableStateOf("") }

  val capParsed = capacity.toIntOrNull() ?: 0
  val isValid = name.trim().length >= 2 && capParsed >= 1 && location.isNotBlank()

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
            text = "Add Amenity",
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
          label = { Text("Amenity Name *") },
          placeholder = { Text("e.g. Clubhouse Lounge") },
          modifier = Modifier.fillMaxWidth().testTag("input_facility_name"),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = tag,
          onValueChange = { tag = it },
          label = { Text("Category Tag") },
          placeholder = { Text("e.g. Wellness, Sports, Dining") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = capacity,
            onValueChange = { capacity = it },
            label = { Text("Capacity *") },
            placeholder = { Text("e.g. 20") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location *") },
            placeholder = { Text("e.g. Ground Floor") },
            modifier = Modifier.weight(1.5f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description") },
          placeholder = { Text("Overview of this amenity") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 2,
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = amenitiesInput,
          onValueChange = { amenitiesInput = it },
          label = { Text("Included Features (comma separated)") },
          placeholder = { Text("e.g. Sound System, Wi-Fi") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = slotsInput,
          onValueChange = { slotsInput = it },
          label = { Text("Available Time Slots (comma separated)") },
          placeholder = { Text("e.g. 09:00 AM - 12:00 PM, 02:00 PM - 05:00 PM") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = rules,
          onValueChange = { rules = it },
          label = { Text("Rules & Guidelines") },
          placeholder = { Text("Booking and resident guidelines") },
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
              if (isValid) {
                val amList = amenitiesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val slotList = slotsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onSubmit(name.trim(), tag.trim(), capParsed, location.trim(), description.trim(), amList, slotList, rules.trim())
              }
            },
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            modifier = Modifier.testTag("button_submit_facility")
          ) {
            Text("Publish Amenity", color = Color.White)
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
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var leadTech by remember { mutableStateOf("") }

  val isValid = name.trim().length >= 2

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
            text = "Add Vendor",
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
          placeholder = { Text("e.g. Company name") },
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
          label = { Text("Lead Technician / Contact") },
          placeholder = { Text("Technician or contact name") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          placeholder = { Text("e.g. +1 (555) 000-0000") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Dispatch Email") },
          placeholder = { Text("contact@company.com") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
              if (isValid) {
                onSubmit(name.trim(), specialty, phone.trim(), email.trim(), leadTech.trim())
              }
            },
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            modifier = Modifier.testTag("button_submit_vendor")
          ) {
            Text("Add Vendor", color = Color.White)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContractDialog(
  onDismiss: () -> Unit,
  onSubmit: (title: String, counterparty: String, type: ContractType, value: String, start: String, end: String, days: Int, terms: String, contact: String, contactEmail: String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var counterparty by remember { mutableStateOf("") }
  var type by remember { mutableStateOf(ContractType.VENDOR_SLA) }
  var annualValue by remember { mutableStateOf("") }
  var startDate by remember { mutableStateOf("") }
  var expiryDate by remember { mutableStateOf("") }
  var daysRemaining by remember { mutableStateOf("") }
  var renewalTerms by remember { mutableStateOf("") }
  var contactPerson by remember { mutableStateOf("") }
  var contactEmail by remember { mutableStateOf("") }
  var showStartPicker by remember { mutableStateOf(false) }
  var showExpiryPicker by remember { mutableStateOf(false) }

  if (showStartPicker) {
    val pickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showStartPicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { millis ->
            val fmt = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).apply {
              timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            startDate = fmt.format(java.util.Date(millis))
          }
          showStartPicker = false
        }) { Text("Select", color = TutsvillePrimary, fontWeight = FontWeight.Bold) }
      },
      dismissButton = {
        TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
      }
    ) { DatePicker(state = pickerState) }
  }

  if (showExpiryPicker) {
    val pickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showExpiryPicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { millis ->
            val fmt = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).apply {
              timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            expiryDate = fmt.format(java.util.Date(millis))
            EstateDateUtils.daysUntilDisplay(expiryDate)?.let { d ->
              daysRemaining = d.toString()
            }
          }
          showExpiryPicker = false
        }) { Text("Select", color = TutsvillePrimary, fontWeight = FontWeight.Bold) }
      },
      dismissButton = {
        TextButton(onClick = { showExpiryPicker = false }) { Text("Cancel") }
      }
    ) { DatePicker(state = pickerState) }
  }

  val isValid = title.trim().length >= 2 && counterparty.isNotBlank() && startDate.isNotBlank() && expiryDate.isNotBlank()

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
            text = "Register Contract",
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
          placeholder = { Text("e.g. Service agreement title") },
          modifier = Modifier.fillMaxWidth().testTag("input_contract_title"),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = counterparty,
          onValueChange = { counterparty = it },
          label = { Text("Counterparty / Tenant *") },
          placeholder = { Text("Vendor or tenant name") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = annualValue,
            onValueChange = { annualValue = it },
            label = { Text("Contract Amount") },
            placeholder = { Text("e.g. $45,000 / yr") },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          OutlinedTextField(
            value = daysRemaining,
            onValueChange = { daysRemaining = it },
            label = { Text("Days Left") },
            placeholder = { Text("Auto") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = startDate,
            onValueChange = { startDate = it },
            label = { Text("Start Date *") },
            placeholder = { Text("Pick date") },
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { showStartPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Start Date", tint = TutsvillePrimary)
              }
            },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          OutlinedTextField(
            value = expiryDate,
            onValueChange = {
              expiryDate = it
              EstateDateUtils.daysUntilDisplay(it)?.let { d -> daysRemaining = d.toString() }
            },
            label = { Text("Expiry Date *") },
            placeholder = { Text("Pick date") },
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { showExpiryPicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Expiry Date", tint = TutsvillePrimary)
              }
            },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = renewalTerms,
          onValueChange = { renewalTerms = it },
          label = { Text("Renewal Terms & SLAs") },
          placeholder = { Text("Notice period and renewal clauses") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = contactPerson,
          onValueChange = { contactPerson = it },
          label = { Text("Contact Person") },
          placeholder = { Text("Primary representative") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = contactEmail,
          onValueChange = { contactEmail = it },
          label = { Text("Contact Email") },
          placeholder = { Text("email@domain.com") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
              if (isValid) {
                val computedDays = daysRemaining.toIntOrNull()
                  ?: EstateDateUtils.daysUntilDisplay(expiryDate)
                  ?: 0
                onSubmit(
                  title.trim(),
                  counterparty.trim(),
                  type,
                  annualValue.trim(),
                  startDate.trim(),
                  expiryDate.trim(),
                  computedDays,
                  renewalTerms.trim(),
                  contactPerson.trim(),
                  contactEmail.trim()
                )
              }
            },
            enabled = isValid,
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
