package com.example.tutsville.ui.components

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tutsville.data.model.CaseCategory
import com.example.tutsville.data.model.ContractDocumentMeta
import com.example.tutsville.data.model.ContractType
import com.example.tutsville.data.model.EstateConfigOptions
import com.example.tutsville.data.model.EstateDateUtils
import com.example.tutsville.data.model.ManagerInvite
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFacilityDialog(
  categoryTags: List<String> = EstateConfigOptions.DEFAULT_AMENITY_CATEGORY_TAGS,
  managedTimeSlots: List<String> = EstateConfigOptions.DEFAULT_AMENITY_TIME_SLOTS,
  onSaveCategoryTags: ((List<String>) -> Unit)? = null,
  onSaveManagedTimeSlots: ((List<String>) -> Unit)? = null,
  onDismiss: () -> Unit,
  onSubmit: (name: String, tag: String, capacity: Int, location: String, description: String, amenities: List<String>, slots: List<String>, rules: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var tag by remember { mutableStateOf("") }
  var tagDropdownExpanded by remember { mutableStateOf(false) }
  var showManageTags by remember { mutableStateOf(false) }
  var showManageTimes by remember { mutableStateOf(false) }
  var capacity by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var amenitiesInput by remember { mutableStateOf("") }
  var slotsInput by remember { mutableStateOf("") }
  var rules by remember { mutableStateOf("") }

  val capParsed = capacity.toIntOrNull()?.coerceAtLeast(0) ?: 0
  val parsedSlots = remember(slotsInput) {
    slotsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
  }
  val hasValidSlots = parsedSlots.isEmpty() || (parsedSlots.size <= 30 && parsedSlots.all { it.length in 3..80 })
  val isValid = name.trim().length >= 2 && hasValidSlots

  if (showManageTags && onSaveCategoryTags != null) {
    ManageOptionsSubDialog(
      title = "Manage Category Tags",
      options = categoryTags,
      placeholder = "e.g., Wellness, Lounge, Sports",
      minItems = 0,
      saveButtonLabel = "Save Tags",
      onDismiss = { showManageTags = false },
      onSave = { updated ->
        onSaveCategoryTags(updated)
        if (tag.isNotBlank() && updated.none { it.equals(tag, ignoreCase = true) }) {
          tag = ""
        }
        showManageTags = false
      }
    )
  }

  if (showManageTimes && onSaveManagedTimeSlots != null) {
    ManageOptionsSubDialog(
      title = "Manage Amenity Times",
      options = managedTimeSlots,
      placeholder = "e.g., 09:00 AM - 11:00 AM",
      minItems = 0,
      saveButtonLabel = "Save Times",
      onDismiss = { showManageTimes = false },
      onSave = { updated ->
        onSaveManagedTimeSlots(updated)
        showManageTimes = false
      }
    )
  }

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
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Add Amenity",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Only Amenity Name is required — all other details are optional",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
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

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Category Tag (Optional)", style = MaterialTheme.typography.labelMedium.copy(color = TutsvilleOnSurfaceVariant))
          if (onSaveCategoryTags != null) {
            TextButton(
              onClick = { showManageTags = true },
              modifier = Modifier.heightIn(min = 48.dp).testTag("btn_manage_amenity_tags")
            ) {
              Icon(Icons.Default.Tune, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Manage Options", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
            }
          }
        }

        ExposedDropdownMenuBox(
          expanded = tagDropdownExpanded,
          onExpandedChange = { tagDropdownExpanded = !tagDropdownExpanded }
        ) {
          OutlinedTextField(
            value = tag.ifBlank { "None (Optional)" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Category Tag") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagDropdownExpanded) },
            singleLine = true,
            modifier = Modifier
              .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
              .fillMaxWidth()
              .testTag("dropdown_amenity_tag"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          ExposedDropdownMenu(
            expanded = tagDropdownExpanded,
            onDismissRequest = { tagDropdownExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text("None (Optional)", color = TutsvilleOnSurfaceVariant) },
              onClick = {
                tag = ""
                tagDropdownExpanded = false
              }
            )
            categoryTags.forEach { option ->
              DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                  tag = option
                  tagDropdownExpanded = false
                }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = capacity,
            onValueChange = { capacity = it.filter { ch -> ch.isDigit() } },
            label = { Text("Capacity") },
            placeholder = { Text("Optional") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
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
          placeholder = { Text("Overview of this amenity (optional)") },
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

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Available Times (Optional)", style = MaterialTheme.typography.labelMedium.copy(color = TutsvilleOnSurfaceVariant))
          if (onSaveManagedTimeSlots != null) {
            TextButton(
              onClick = { showManageTimes = true },
              modifier = Modifier.heightIn(min = 48.dp).testTag("btn_manage_amenity_times")
            ) {
              Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Manage Times", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
            }
          }
        }

        OutlinedTextField(
          value = slotsInput,
          onValueChange = { slotsInput = it },
          label = { Text("Selected Time Slots (Optional)") },
          placeholder = { Text("Tap managed times below or type comma-separated") },
          isError = !hasValidSlots,
          modifier = Modifier.fillMaxWidth().testTag("input_facility_slots"),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        if (managedTimeSlots.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            managedTimeSlots.forEach { preset ->
              val isAdded = parsedSlots.any { it.equals(preset, ignoreCase = true) }
              FilterChip(
                selected = isAdded,
                onClick = {
                  val updated = if (isAdded) {
                    parsedSlots.filterNot { it.equals(preset, ignoreCase = true) }
                  } else {
                    parsedSlots + preset
                  }
                  slotsInput = updated.joinToString(", ")
                },
                label = { Text(preset, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = TutsvilleSecondaryContainer,
                  selectedLabelColor = TutsvilleOnSecondaryContainer
                )
              )
            }
          }
        }
        if (!hasValidSlots) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Each time slot must be between 3 and 80 characters (max 30).",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = StatusUrgentText)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = rules,
          onValueChange = { rules = it },
          label = { Text("Notes") },
          placeholder = { Text("Booking notes or resident reminders (optional)") },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Cancel", color = TutsvilleOnSurfaceVariant)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (isValid) {
                val amList = amenitiesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onSubmit(name.trim(), tag.trim(), capParsed, location.trim(), description.trim(), amList, parsedSlots, rules.trim())
              }
            },
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            modifier = Modifier.heightIn(min = 48.dp).testTag("button_submit_facility")
          ) {
            Text("Publish Amenity", color = Color.White)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVendorDialog(
  disciplines: List<String> = EstateConfigOptions.DEFAULT_CONTRACTOR_DISCIPLINES,
  onSaveDisciplines: ((List<String>) -> Unit)? = null,
  onDismiss: () -> Unit,
  onSubmit: (name: String, specialty: CaseCategory, phone: String, email: String, leadTech: String, discipline: String) -> Unit
) {
  val effectiveOptions = remember(disciplines) {
    disciplines.ifEmpty { EstateConfigOptions.DEFAULT_CONTRACTOR_DISCIPLINES }
  }
  var name by remember { mutableStateOf("") }
  var selectedDiscipline by remember(effectiveOptions) {
    mutableStateOf(effectiveOptions.firstOrNull() ?: CaseCategory.HVAC.label)
  }
  var disciplineExpanded by remember { mutableStateOf(false) }
  var showManageOptions by remember { mutableStateOf(false) }
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var leadTech by remember { mutableStateOf("") }

  val isValid = name.trim().length >= 2

  if (showManageOptions && onSaveDisciplines != null) {
    ManageOptionsSubDialog(
      title = "Manage Contractor Disciplines",
      options = effectiveOptions,
      placeholder = "e.g., Pool & Spa Maintenance",
      onDismiss = { showManageOptions = false },
      onSave = { updated ->
        onSaveDisciplines(updated)
        if (updated.isNotEmpty() && !updated.contains(selectedDiscipline)) {
          selectedDiscipline = updated.first()
        }
        showManageOptions = false
      }
    )
  }

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
            text = "Add Contractor",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Contractor Name *") },
          placeholder = { Text("e.g. Company or contractor name") },
          modifier = Modifier.fillMaxWidth().testTag("input_vendor_name"),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Discipline *", style = MaterialTheme.typography.labelMedium.copy(color = TutsvilleOnSurfaceVariant))
          if (onSaveDisciplines != null) {
            TextButton(
              onClick = { showManageOptions = true },
              modifier = Modifier.heightIn(min = 48.dp).testTag("btn_manage_contractor_disciplines")
            ) {
              Icon(Icons.Default.Tune, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Manage Options", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
            }
          }
        }

        ExposedDropdownMenuBox(
          expanded = disciplineExpanded,
          onExpandedChange = { disciplineExpanded = !disciplineExpanded }
        ) {
          OutlinedTextField(
            value = selectedDiscipline,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Discipline") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = disciplineExpanded) },
            modifier = Modifier
              .menuAnchor()
              .fillMaxWidth()
              .testTag("dropdown_contractor_discipline"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          ExposedDropdownMenu(
            expanded = disciplineExpanded,
            onDismissRequest = { disciplineExpanded = false }
          ) {
            effectiveOptions.forEach { option ->
              DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                  selectedDiscipline = option
                  disciplineExpanded = false
                }
              )
            }
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
          TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Cancel", color = TutsvilleOnSurfaceVariant)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (isValid) {
                val mappedCategory = CaseCategory.entries.find {
                  it.label.equals(selectedDiscipline, ignoreCase = true) ||
                    selectedDiscipline.contains(it.name, ignoreCase = true)
                } ?: CaseCategory.STRUCTURAL
                onSubmit(name.trim(), mappedCategory, phone.trim(), email.trim(), leadTech.trim(), selectedDiscipline)
              }
            },
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            modifier = Modifier.heightIn(min = 48.dp).testTag("button_submit_vendor")
          ) {
            Text("Add Contractor", color = Color.White)
          }
        }
      }
    }
  }
}

@Composable
fun ManageOptionsSubDialog(
  title: String,
  options: List<String>,
  placeholder: String,
  minItems: Int = 1,
  saveButtonLabel: String = "Save Options",
  onDismiss: () -> Unit,
  onSave: (List<String>) -> Unit
) {
  val itemsList = remember(options) { mutableStateListOf<String>().apply { addAll(options) } }
  var newItemText by remember { mutableStateOf("") }
  var editingIndex by remember { mutableStateOf<Int?>(null) }
  var editingText by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }

  fun commitInlineEdit(): Boolean {
    val idx = editingIndex ?: return true
    if (idx !in itemsList.indices) {
      editingIndex = null
      return true
    }
    val clean = editingText.trim()
    if (clean.isEmpty()) {
      errorText = "Option name cannot be empty."
      return false
    }
    val isDuplicate = itemsList.withIndex().any { (i, existing) ->
      i != idx && existing.equals(clean, ignoreCase = true)
    }
    if (isDuplicate) {
      errorText = "\"$clean\" already exists."
      return false
    }
    itemsList[idx] = clean
    editingIndex = null
    editingText = ""
    errorText = null
    return true
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, GoldAccent),
      modifier = Modifier.fillMaxWidth().testTag("dialog_manage_options")
    ) {
      Column(
        modifier = Modifier
          .padding(18.dp)
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary),
            modifier = Modifier.weight(1f)
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = newItemText,
            onValueChange = {
              newItemText = it
              errorText = null
            },
            placeholder = { Text(placeholder, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("input_new_option")
          )
          Button(
            onClick = {
              val clean = newItemText.trim()
              when {
                clean.isEmpty() -> Unit
                itemsList.size >= 40 -> errorText = "Maximum of 40 options reached."
                itemsList.any { it.equals(clean, ignoreCase = true) } -> errorText = "\"$clean\" already exists."
                else -> {
                  itemsList.add(clean)
                  newItemText = ""
                  errorText = null
                }
              }
            },
            enabled = newItemText.trim().isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            modifier = Modifier.height(48.dp).testTag("btn_add_option")
          ) {
            Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
          }
        }

        if (errorText != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = errorText.orEmpty(),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = StatusUrgentText)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        if (itemsList.isEmpty()) {
          Text(
            text = "No options configured yet. Add one above or save empty.",
            style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsList.forEachIndexed { index, opt ->
              val isEditing = editingIndex == index
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = TutsvilleBackground,
                border = BorderStroke(1.dp, if (isEditing) GoldAccent else TutsvilleOutline),
                modifier = Modifier.fillMaxWidth()
              ) {
                if (isEditing) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    OutlinedTextField(
                      value = editingText,
                      onValueChange = {
                        editingText = it
                        errorText = null
                      },
                      singleLine = true,
                      modifier = Modifier.weight(1f).testTag("input_edit_option_$index")
                    )
                    IconButton(
                      onClick = { commitInlineEdit() },
                      modifier = Modifier.size(48.dp).testTag("btn_confirm_edit_option_$index")
                    ) {
                      Icon(Icons.Default.Check, contentDescription = "Save rename", tint = StatusCompletedText, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                      onClick = {
                        editingIndex = null
                        editingText = ""
                        errorText = null
                      },
                      modifier = Modifier.size(48.dp)
                    ) {
                      Icon(Icons.Default.Close, contentDescription = "Cancel rename", tint = TutsvilleOnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                  }
                } else {
                  Row(
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = opt, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      IconButton(
                        onClick = {
                          editingIndex = index
                          editingText = opt
                          errorText = null
                        },
                        modifier = Modifier.size(48.dp).testTag("btn_edit_option_$index")
                      ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit $opt", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                      }
                      if (itemsList.size > minItems) {
                        IconButton(
                          onClick = {
                            itemsList.removeAt(index)
                            if (editingIndex == index) {
                              editingIndex = null
                              editingText = ""
                            } else if (editingIndex != null && editingIndex!! > index) {
                              editingIndex = editingIndex!! - 1
                            }
                            errorText = null
                          },
                          modifier = Modifier.size(48.dp).testTag("btn_remove_option_$index")
                        ) {
                          Icon(Icons.Default.DeleteOutline, contentDescription = "Remove $opt", tint = StatusUrgentText, modifier = Modifier.size(18.dp))
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
          onClick = {
            if (commitInlineEdit()) {
              onSave(itemsList.toList())
            }
          },
          enabled = itemsList.size >= minItems,
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_save_managed_options")
        ) {
          Text(saveButtonLabel, color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun ManageManagersDialog(
  invites: List<ManagerInvite>,
  onDismiss: () -> Unit,
  onInvite: (email: String, displayName: String) -> Unit,
  onRevoke: (email: String) -> Unit
) {
  var email by remember { mutableStateOf("") }
  var displayName by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.2.dp, GoldAccent),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("dialog_manage_managers")
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
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Managers & Landlords",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Invite co-managers who can sign in with Google and manage Tutsville Estate",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = displayName,
          onValueChange = { displayName = it },
          label = { Text("Full Name") },
          placeholder = { Text("e.g., Jordan Vance") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_manager_name")
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Google Email Address *") },
          placeholder = { Text("landlord@example.com") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("input_manager_email")
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = {
            if (email.contains("@")) {
              onInvite(email.trim(), displayName.trim())
              email = ""
              displayName = ""
            }
          },
          enabled = email.contains("@"),
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_invite_manager")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Grant Manager / Landlord Access", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Authorized Co-Manager Invites (${invites.size})",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (invites.isEmpty()) {
          Text(
            text = "No additional co-managers invited yet.",
            style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            invites.forEach { inv ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = TutsvilleBackground,
                border = BorderStroke(1.dp, TutsvilleOutline),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = inv.displayName.ifBlank { inv.email },
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                    )
                    Text(
                      text = "${inv.email} • ${inv.status}",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
                    )
                  }
                  IconButton(
                    onClick = { onRevoke(inv.email) },
                    modifier = Modifier.size(48.dp).testTag("btn_revoke_manager_${inv.email}")
                  ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Revoke Access", tint = StatusUrgentText)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContractDialog(
  tenants: List<com.example.tutsville.data.model.TenantProfile> = emptyList(),
  vendors: List<com.example.tutsville.data.model.VendorItem> = emptyList(),
  properties: List<com.example.tutsville.data.model.PropertyItem> = emptyList(),
  onDismiss: () -> Unit,
  onSubmit: (
    title: String,
    counterparty: String,
    type: ContractType,
    value: String,
    start: String,
    end: String,
    days: Int,
    terms: String,
    contact: String,
    contactEmail: String,
    unit: String,
    tenantId: String?,
    tenantEmail: String?,
    propertyId: String?,
    signedContractDoc: ContractDocumentMeta?,
    codeOfConductDoc: ContractDocumentMeta?
  ) -> Unit
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var titleManuallyEdited by remember { mutableStateOf(false) }
  var counterparty by remember { mutableStateOf("") }
  var counterpartyManuallyEdited by remember { mutableStateOf(false) }
  var type by remember { mutableStateOf(ContractType.TENANT_LEASE) }
  var unit by remember { mutableStateOf("") }
  var selectedTenantId by remember { mutableStateOf<String?>(null) }
  var selectedTenantEmail by remember { mutableStateOf<String?>(null) }
  var selectedPropertyId by remember { mutableStateOf<String?>(null) }
  var annualValue by remember { mutableStateOf("") }
  var startDate by remember { mutableStateOf("") }
  var expiryDate by remember { mutableStateOf("") }
  var daysRemaining by remember { mutableStateOf("") }
  var renewalTerms by remember { mutableStateOf("") }
  var contactPerson by remember { mutableStateOf("") }
  var contactEmail by remember { mutableStateOf("") }
  var signedContractDoc by remember { mutableStateOf<ContractDocumentMeta?>(null) }
  var codeOfConductDoc by remember { mutableStateOf<ContractDocumentMeta?>(null) }
  var showStartPicker by remember { mutableStateOf(false) }
  var showExpiryPicker by remember { mutableStateOf(false) }

  val signedContractPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      } catch (_: Exception) {}
      signedContractDoc = resolveDocumentMeta(context, uri)
    }
  }
  val codeOfConductPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
      } catch (_: Exception) {}
      codeOfConductDoc = resolveDocumentMeta(context, uri)
    }
  }

  fun recomputeDays(start: String, expiry: String) {
    val computed = EstateDateUtils.daysUntilDisplay(expiry)
      ?: EstateDateUtils.daysBetweenDisplay(start, expiry)
    if (computed != null) {
      daysRemaining = computed.toString()
    }
  }

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
            if (expiryDate.isNotBlank()) recomputeDays(startDate, expiryDate)
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
            recomputeDays(startDate, expiryDate)
          }
          showExpiryPicker = false
        }) { Text("Select", color = TutsvillePrimary, fontWeight = FontWeight.Bold) }
      },
      dismissButton = {
        TextButton(onClick = { showExpiryPicker = false }) { Text("Cancel") }
      }
    ) { DatePicker(state = pickerState) }
  }

  val isValid = title.trim().length >= 2 &&
    counterparty.isNotBlank() &&
    startDate.isNotBlank() &&
    expiryDate.isNotBlank() &&
    signedContractDoc != null

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
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("Contract Type *", style = MaterialTheme.typography.labelMedium.copy(color = TutsvilleOnSurfaceVariant))
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ContractType.values().forEach { ct ->
            FilterChip(
              selected = type == ct,
              onClick = { type = ct },
              label = { Text(ct.label, fontSize = 11.sp) }
            )
          }
        }

        if (type == ContractType.TENANT_LEASE && tenants.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text("Quick Pick Registered Tenant", style = MaterialTheme.typography.labelSmall.copy(color = TutsvillePrimary, fontWeight = FontWeight.Bold))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            tenants.forEach { t ->
              FilterChip(
                selected = selectedTenantId == t.id,
                onClick = {
                  selectedTenantId = t.id
                  selectedTenantEmail = t.email
                  selectedPropertyId = t.assignedPropertyId
                  if (!counterpartyManuallyEdited || counterparty.isBlank()) {
                    counterparty = t.fullName
                  }
                  if (contactPerson.isBlank()) contactPerson = t.fullName
                  if (contactEmail.isBlank()) contactEmail = t.email
                  if (!t.assignedPropertyUnit.isNullOrBlank()) {
                    unit = t.assignedPropertyUnit
                    val prop = properties.find { p -> p.id == t.assignedPropertyId || p.unitNumber == t.assignedPropertyUnit }
                    if (prop != null && annualValue.isBlank()) {
                      annualValue = prop.monthlyRental
                    }
                    if (!titleManuallyEdited && title.isBlank()) {
                      title = "Residential Lease — ${t.assignedPropertyUnit}"
                    }
                  }
                },
                label = { Text(t.fullName, fontSize = 11.sp) }
              )
            }
          }
        } else if (type == ContractType.VENDOR_SLA && vendors.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text("Quick Pick Approved Contractor", style = MaterialTheme.typography.labelSmall.copy(color = TutsvillePrimary, fontWeight = FontWeight.Bold))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            vendors.forEach { v ->
              FilterChip(
                selected = counterparty.equals(v.name, ignoreCase = true),
                onClick = {
                  selectedTenantId = null
                  selectedTenantEmail = null
                  selectedPropertyId = null
                  if (!counterpartyManuallyEdited || counterparty.isBlank()) {
                    counterparty = v.name
                  }
                  if (contactPerson.isBlank()) contactPerson = v.leadTech
                  if (contactEmail.isBlank()) contactEmail = v.email
                },
                label = { Text(v.name, fontSize = 11.sp) }
              )
            }
          }
        }

        if (properties.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text("Quick Pick Unit", style = MaterialTheme.typography.labelSmall.copy(color = TutsvillePrimary, fontWeight = FontWeight.Bold))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            properties.forEach { p ->
              FilterChip(
                selected = selectedPropertyId == p.id || unit.equals(p.unitNumber, ignoreCase = true),
                onClick = {
                  selectedPropertyId = p.id
                  unit = p.unitNumber
                  if (type == ContractType.TENANT_LEASE && annualValue.isBlank()) {
                    annualValue = p.monthlyRental
                  }
                  if (type == ContractType.TENANT_LEASE && !titleManuallyEdited && title.isBlank()) {
                    title = "Residential Lease — ${p.unitNumber}"
                  }
                },
                label = { Text(p.unitNumber, fontSize = 11.sp) }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = title,
          onValueChange = {
            title = it
            titleManuallyEdited = it.isNotBlank()
          },
          label = { Text("Contract Title *") },
          placeholder = { Text(if (type == ContractType.TENANT_LEASE) "e.g. Residential Lease — Unit 701" else "e.g. Service agreement title") },
          modifier = Modifier.fillMaxWidth().testTag("input_contract_title"),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = counterparty,
            onValueChange = {
              counterparty = it
              counterpartyManuallyEdited = it.isNotBlank()
            },
            label = { Text(if (type == ContractType.TENANT_LEASE) "Tenant Name *" else "Contractor / Partner *") },
            placeholder = { Text("Contractor or tenant name") },
            modifier = Modifier.weight(1.3f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
          OutlinedTextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text("Unit / Scope") },
            placeholder = { Text("e.g. Unit 701") },
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
          )
        }

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
            onValueChange = {
              startDate = it
              if (expiryDate.isNotBlank()) recomputeDays(it, expiryDate)
            },
            label = { Text("Start Date *") },
            placeholder = { Text("Pick date") },
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { showStartPicker = true }, modifier = Modifier.size(48.dp)) {
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
              recomputeDays(startDate, it)
            },
            label = { Text("Expiry Date *") },
            placeholder = { Text("Pick date") },
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { showExpiryPicker = true }, modifier = Modifier.size(48.dp)) {
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
          onValueChange = {
            contactEmail = it
            if (type == ContractType.TENANT_LEASE) {
              selectedTenantEmail = it.trim().lowercase().takeIf { e -> e.isNotBlank() }
            }
          },
          label = { Text(if (type == ContractType.TENANT_LEASE) "Tenant Email" else "Contact Email") },
          placeholder = { Text("email@domain.com") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Contract Documents",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        ContractDocAttachmentRow(
          label = "Signed Contract",
          doc = signedContractDoc,
          canAttach = true,
          isRequired = true,
          onOpen = { doc -> openAttachedDocument(context, doc) },
          onAttach = { signedContractPicker.launch(arrayOf("application/pdf", "image/*", "*/*")) },
          tagPrefix = "add_signed_contract"
        )
        Spacer(modifier = Modifier.height(6.dp))
        ContractDocAttachmentRow(
          label = "Code of Conduct",
          doc = codeOfConductDoc,
          canAttach = true,
          onOpen = { doc -> openAttachedDocument(context, doc) },
          onAttach = { codeOfConductPicker.launch(arrayOf("application/pdf", "image/*", "*/*")) },
          tagPrefix = "add_code_of_conduct"
        )
        if (signedContractDoc == null) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Signed contract document is required to register a contract.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = Color(0xFFC62828))
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Cancel", color = TutsvilleOnSurfaceVariant)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (isValid) {
                val computedDays = daysRemaining.toIntOrNull()
                  ?: EstateDateUtils.daysUntilDisplay(expiryDate)
                  ?: EstateDateUtils.daysBetweenDisplay(startDate, expiryDate)
                  ?: 0
                val resolvedEmail = if (type == ContractType.TENANT_LEASE) {
                  (selectedTenantEmail ?: contactEmail.trim().lowercase()).takeIf { it.isNotBlank() }
                } else null
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
                  contactEmail.trim(),
                  unit.trim().ifBlank { "Estate Wide" },
                  selectedTenantId,
                  resolvedEmail,
                  selectedPropertyId,
                  signedContractDoc,
                  codeOfConductDoc
                )
              }
            },
            enabled = isValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            modifier = Modifier.heightIn(min = 48.dp).testTag("button_submit_contract")
          ) {
            Text("Register Contract", color = Color.White)
          }
        }
      }
    }
  }
}
