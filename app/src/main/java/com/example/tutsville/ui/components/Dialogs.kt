package com.example.tutsville.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tutsville.data.model.*
import com.example.ui.theme.*

@Composable
fun ReportCaseDialog(
  initialLocation: String = "",
  onDismiss: () -> Unit,
  onSubmit: (title: String, desc: String, category: CaseCategory, priority: CasePriority, location: String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var location by remember { mutableStateOf(initialLocation) }
  var category by remember { mutableStateOf(CaseCategory.PLUMBING) }
  var priority by remember { mutableStateOf(CasePriority.MEDIUM) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
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
            text = "Report Incident",
            style = MaterialTheme.typography.titleLarge.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }
        Text(
          text = "Log an operational issue or maintenance work order",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Incident Title") },
          placeholder = { Text("e.g., Water pressure drop in kitchen") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("case_title_input"),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = location,
          onValueChange = { location = it },
          label = { Text("Location / Unit *") },
          placeholder = { Text("e.g., Unit number or common area") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("case_location_input"),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Category Discipline",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(CaseCategory.PLUMBING, CaseCategory.ELECTRICAL, CaseCategory.HVAC).forEach { cat ->
            val sel = cat == category
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (sel) TutsvillePrimary else TutsvilleBackground,
              border = BorderStroke(1.dp, if (sel) TutsvillePrimary else TutsvilleOutline),
              modifier = Modifier
                .weight(1f)
                .clickable { category = cat }
            ) {
              Text(
                text = cat.name.lowercase().capitalize(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                  color = if (sel) Color.White else TutsvilleOnSurfaceVariant
                ),
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Priority Level (SLA Window)",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          CasePriority.values().forEach { prio ->
            val isSel = prio == priority
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSel) TutsvilleSecondary else TutsvilleBackground,
              border = BorderStroke(1.dp, if (isSel) TutsvilleSecondary else TutsvilleOutline),
              modifier = Modifier
                .weight(1f)
                .clickable { priority = prio }
            ) {
              Text(
                text = prio.name.take(3),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (isSel) Color.White else TutsvilleOnSurfaceVariant
                ),
                modifier = Modifier.padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Detailed Description") },
          placeholder = { Text("Describe symptoms, affected equipment, or urgency...") },
          modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .testTag("case_description_input"),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = {
            if (title.trim().length >= 3 && location.isNotBlank()) {
              onSubmit(title.trim(), description.trim(), category, priority, location.trim())
            }
          },
          enabled = title.trim().length >= 3 && location.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_case_button")
        ) {
          Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Submit Work Order", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}

@Composable
fun AssignVendorDialog(
  caseItem: CaseItem,
  vendors: List<VendorItem>,
  onDismiss: () -> Unit,
  onAssign: (vendorId: String) -> Unit,
  onAddNewVendor: (() -> Unit)? = null
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 20.dp)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Assign Contractor",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Case #${caseItem.id} • ${caseItem.category.label}",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary)
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = caseItem.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
          text = "Location: ${caseItem.location} • SLA: ${caseItem.priority.label}",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Approved Vetted Contractors",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
          )
          if (onAddNewVendor != null) {
            TextButton(
              onClick = onAddNewVendor,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
              modifier = Modifier.testTag("assign_dialog_add_vendor_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(3.dp))
              Text("+ Register New", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
            }
          }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (onAddNewVendor != null) {
            item {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoldAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onAddNewVendor() }
                  .testTag("assign_dialog_register_new_contractor_card")
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Surface(
                    shape = CircleShape,
                    color = GoldAccent.copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.PersonAdd, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "+ Onboard New Contractor",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary, fontSize = 13.sp)
                    )
                    Text(
                      text = "Add specialty, phone, and SLA response time",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant)
                    )
                  }
                  Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                }
              }
            }
          }

          items(vendors) { vendor ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleBackground,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onAssign(vendor.id) }
                .testTag("vendor_item_${vendor.id.lowercase()}")
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = vendor.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  )
                  Text(
                    text = "Specialty: ${vendor.specialty.label} • Lead: ${vendor.leadTech}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
                  )
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "${vendor.rating} (${vendor.reviewCount} reviews) • ${vendor.activeJobsCount} active jobs",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleSecondary)
                    )
                  }
                }
                Button(
                  onClick = { onAssign(vendor.id) },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                  modifier = Modifier.height(36.dp)
                ) {
                  Text("Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
fun BookFacilitySheet(
  facility: Facility,
  initialDate: String = EstateDateUtils.todayDisplay(),
  isSlotBooked: (date: String, timeSlot: String) -> Boolean = { _, _ -> false },
  onDismiss: () -> Unit,
  onConfirm: (date: String, slot: String, guests: Int, notes: String) -> Unit
) {
  val todayDisp = remember { EstateDateUtils.todayDisplay() }
  val tomorrowDisp = remember { EstateDateUtils.offsetDisplay(1) }
  val dayAfterDisp = remember { EstateDateUtils.offsetDisplay(2) }
  val dateChoices = remember(initialDate, todayDisp, tomorrowDisp, dayAfterDisp) {
    listOf(initialDate, todayDisp, tomorrowDisp, dayAfterDisp).distinct().take(3)
  }

  var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
  var showDatePicker by remember { mutableStateOf(false) }
  var selectedSlot by remember(facility.id, initialDate) {
    val firstOpen = facility.allowedSlots.firstOrNull { !isSlotBooked(initialDate, it) }
    mutableStateOf(firstOpen ?: "")
  }
  var guestCount by remember { mutableStateOf(4) }
  var specialNotes by remember { mutableStateOf("") }

  LaunchedEffect(selectedDate, facility.allowedSlots) {
    if (selectedSlot.isBlank() || isSlotBooked(selectedDate, selectedSlot)) {
      selectedSlot = facility.allowedSlots.firstOrNull { !isSlotBooked(selectedDate, it) } ?: ""
    }
  }

  val anySlotAvailable = facility.allowedSlots.any { !isSlotBooked(selectedDate, it) }
  val canConfirm = selectedSlot.isNotBlank() && !isSlotBooked(selectedDate, selectedSlot)

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          datePickerState.selectedDateMillis?.let { millis ->
            val fmt = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).apply {
              timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            selectedDate = fmt.format(java.util.Date(millis))
          }
          showDatePicker = false
        }) {
          Text("Select", color = TutsvillePrimary, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Cancel", color = TutsvilleOnSurfaceVariant)
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
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
              text = "Reserve Amenity",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = facility.name,
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleSecondary, fontWeight = FontWeight.SemiBold)
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Max Capacity: ${facility.capacity} guests • Location: ${facility.location}",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Select Date ($selectedDate)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
          )
          TextButton(
            onClick = { showDatePicker = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Calendar Date", tint = GoldAccent, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Pick Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldAccent)
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          dateChoices.forEach { d ->
            val sel = d == selectedDate
            val chipLabel = EstateDateUtils.shortChipLabelFromDisplay(d).substringBefore(" (")
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (sel) TutsvillePrimary else TutsvilleBackground,
              border = BorderStroke(1.dp, if (sel) TutsvillePrimary else TutsvilleOutline),
              modifier = Modifier
                .weight(1f)
                .clickable { selectedDate = d }
            ) {
              Text(
                text = chipLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                  color = if (sel) Color.White else TutsvilleOnSurfaceVariant
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Available Time Slots",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          facility.allowedSlots.forEach { slot ->
            val isBooked = isSlotBooked(selectedDate, slot)
            val isSlotSel = !isBooked && slot == selectedSlot
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = when {
                isBooked -> Color(0xFFF1EFEA)
                isSlotSel -> TutsvilleSecondaryContainer
                else -> TutsvilleBackground
              },
              border = BorderStroke(
                1.dp,
                when {
                  isBooked -> TutsvilleOutline.copy(alpha = 0.5f)
                  isSlotSel -> TutsvilleSecondary
                  else -> TutsvilleOutline
                }
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isBooked) { selectedSlot = slot }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = slot,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSlotSel) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                      isBooked -> TutsvilleOnSurfaceVariant.copy(alpha = 0.6f)
                      isSlotSel -> TutsvilleOnSecondaryContainer
                      else -> TutsvilleOnBackground
                    }
                  )
                )
                if (isBooked) {
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = StatusUrgentBg
                  ) {
                    Text(
                      text = "Reserved",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = StatusUrgentText,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                } else if (isSlotSel) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }

        if (!anySlotAvailable) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "All slots on $selectedDate are already booked. Please choose another date.",
            style = MaterialTheme.typography.bodySmall.copy(color = StatusUrgentText, fontSize = 11.sp)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Guest Count",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { if (guestCount > 1) guestCount-- },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }
            Text(
              text = "$guestCount guests",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
              onClick = { if (guestCount < facility.capacity) guestCount++ },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Increase")
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = specialNotes,
          onValueChange = { specialNotes = it },
          label = { Text("Special Requests / Catering Notes") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = { if (canConfirm) onConfirm(selectedDate, selectedSlot, guestCount, specialNotes) },
          enabled = canConfirm,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("confirm_booking_button")
        ) {
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Confirm Instant Reservation", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun ServiceReviewDialog(
  caseItem: CaseItem,
  onDismiss: () -> Unit,
  onSubmit: (rating: Int, tags: List<String>, comment: String) -> Unit
) {
  var rating by remember { mutableStateOf(5) }
  var comment by remember { mutableStateOf("") }
  val availableTags = listOf("Punctual", "Clean Worksite", "High Craftsmanship", "Resolved First Visit", "Clear Communication")
  val selectedTags = remember { mutableStateListOf("Punctual", "Resolved First Visit") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
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
          Column {
            Text(
              text = "Service Review",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Work Order #${caseItem.id}",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary)
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = caseItem.title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
          text = "Vendor: ${caseItem.assignedVendorName ?: "Certified Contractor"}",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "How satisfied are you with the resolution?",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        StarRatingBar(
          rating = rating,
          onRatingChanged = { rating = it },
          starSize = 32.dp,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Service Highlights",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          availableTags.chunked(2).forEach { rowTags ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              rowTags.forEach { tag ->
                val isSel = selectedTags.contains(tag)
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (isSel) TutsvilleSecondaryContainer else TutsvilleBackground,
                  border = BorderStroke(1.dp, if (isSel) TutsvilleSecondary else TutsvilleOutline),
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      if (isSel) selectedTags.remove(tag) else selectedTags.add(tag)
                    }
                ) {
                  Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSel) TutsvilleOnSecondaryContainer else TutsvilleOnSurfaceVariant
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = comment,
          onValueChange = { comment = it },
          label = { Text("Detailed Feedback / Notes") },
          placeholder = { Text("Describe technician craftsmanship, resolution speed...") },
          modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .testTag("review_comment_input"),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = { onSubmit(rating, selectedTags.toList(), comment) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_service_review_button")
        ) {
          Text("Submit Certified Review", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun SendReminderDialog(
  onDismiss: () -> Unit,
  onSend: (title: String, message: String, audience: String, urgency: String, isBroadcast: Boolean) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }
  var audience by remember { mutableStateOf("All Residents & Tenants") }
  var urgency by remember { mutableStateOf("Advisory") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
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
            text = "Publish Estate Advisory",
            style = MaterialTheme.typography.titleLarge.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }
        Text(
          text = "Publish an official estate announcement or management bulletin",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Advisory Title *") },
          placeholder = { Text("e.g., Scheduled Water Shutdown • Wing B") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("reminder_title_input"),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Target Audience",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(4.dp))
        listOf("All Residents & Tenants", "Estate Management").forEach { aud ->
          val isSel = aud == audience
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { audience = aud }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(selected = isSel, onClick = { audience = aud })
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = aud, style = MaterialTheme.typography.bodyMedium)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Urgency Level",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("Advisory", "Notice", "High Priority").forEach { level ->
            val selected = urgency == level
            FilterChip(
              selected = selected,
              onClick = { urgency = level },
              label = { Text(level, fontSize = 11.sp, maxLines = 1) }
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text("Advisory Details *") },
          placeholder = { Text("Provide details, hours, contact info or instructions...") },
          modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .testTag("reminder_message_input"),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = {
            if (title.isNotBlank()) {
              val isBroadcast = audience.equals("All Residents & Tenants", ignoreCase = true)
              onSend(title.trim(), message.trim(), audience, urgency, isBroadcast)
            }
          },
          enabled = title.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("send_reminder_submit_button")
        ) {
          Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Publish Advisory", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun ContractDetailDialog(
  contract: ContractItem,
  currentRole: UserRole = UserRole.MANAGER,
  onDismiss: () -> Unit,
  onTriggerAlert: () -> Unit,
  onRenew: () -> Unit,
  onSignContract: (() -> Unit)? = null
) {
  val isPendingTenantSign = contract.type == ContractType.TENANT_LEASE &&
    contract.leaseStatus == LeaseContractStatus.PENDING_SIGNATURE
  val daysRemaining = contract.effectiveDaysRemaining

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
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
              text = if (contract.type == ContractType.TENANT_LEASE) "Lease Agreement" else "Contract Details",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "${contract.type.label} • ${contract.unit}",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary)
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = contract.title,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          StatusPill(
            text = contract.leaseStatus.label,
            bgColor = if (isPendingTenantSign) StatusPendingBg else StatusCompletedBg,
            textColor = if (isPendingTenantSign) StatusPendingText else StatusCompletedText,
            dotColor = if (isPendingTenantSign) GoldAccent else StatusCompletedText
          )
          ContractUrgencyBadge(urgency = contract.urgency, days = daysRemaining)
        }

        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = TutsvilleBackground,
          border = BorderStroke(1.dp, TutsvilleOutline),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (contract.type == ContractType.TENANT_LEASE) {
              Text(
                text = "Estate Manager: ${contract.managerName.ifBlank { "Tutsville Estate Management" }}" +
                  (contract.managerEmail.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""),
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }
            Text(
              text = "${if (contract.type == ContractType.TENANT_LEASE) "Resident / Tenant" else "Counterparty"}: ${contract.counterparty}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
              text = "Unit / Scope: ${contract.unit}",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvillePrimary, fontWeight = FontWeight.SemiBold)
            )
            Text(
              text = "Contract Value: ${contract.annualValue.ifBlank { "Standard Terms" }}",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleSecondary, fontWeight = FontWeight.Bold)
            )
            Text(
              text = "Term: ${contract.startDate} to ${contract.expiryDate}",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
            )
            if (contract.contactPerson.isNotBlank() || contract.contactEmail.isNotBlank()) {
              Text(
                text = "Point of Contact: ${contract.contactPerson} (${contract.contactEmail})",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }
          }
        }

        if (contract.isSignedByTenant) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = StatusCompletedBg,
            border = BorderStroke(1.dp, StatusCompletedText.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Verified, contentDescription = null, tint = StatusCompletedText, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Digitally signed by ${contract.signedByName ?: contract.counterparty}" +
                  (contract.signedAt?.let { " on $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall.copy(color = StatusCompletedText, fontWeight = FontWeight.SemiBold)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Lease & Renewal Terms",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Text(
          text = contract.renewalTerms.ifBlank { "Standard Tutsville Estate residential agreement terms apply." },
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(18.dp))
        if (currentRole == UserRole.TENANT) {
          if (isPendingTenantSign && onSignContract != null) {
            Button(
              onClick = onSignContract,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("dialog_sign_lease_button")
            ) {
              Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sign & Accept Lease Contract", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          } else {
            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
              Text("Close", fontWeight = FontWeight.Bold, color = TutsvillePrimary)
            }
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onTriggerAlert,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(44.dp).testTag("dialog_send_alert_button")
            ) {
              Text("Send Expiry Alert", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
              onClick = onRenew,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
              modifier = Modifier.weight(1f).height(44.dp).testTag("dialog_renew_contract_button")
            ) {
              Text("Renew (1 Year)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceDialog(
  initialDate: String = EstateDateUtils.todayDisplay(),
  vendors: List<VendorItem> = emptyList(),
  properties: List<PropertyItem> = emptyList(),
  onDismiss: () -> Unit,
  onSchedule: (title: String, discipline: MaintenanceDiscipline, date: String, time: String, contractor: String, targetArea: String, intervalMonths: Int, notes: String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var discipline by remember { mutableStateOf(MaintenanceDiscipline.HVAC) }
  var date by remember(initialDate) { mutableStateOf(initialDate) }
  var showDatePicker by remember { mutableStateOf(false) }
  var time by remember { mutableStateOf("") }
  var contractor by remember { mutableStateOf("") }
  var targetArea by remember { mutableStateOf("") }
  var intervalMonths by remember { mutableStateOf(3) }
  var notes by remember { mutableStateOf("") }

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          datePickerState.selectedDateMillis?.let { millis ->
            val fmt = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).apply {
              timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            date = fmt.format(java.util.Date(millis))
          }
          showDatePicker = false
        }) {
          Text("Select", color = TutsvillePrimary, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) {
          Text("Cancel", color = TutsvilleOnSurfaceVariant)
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
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
            text = "Schedule Maintenance",
            style = MaterialTheme.typography.titleLarge.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }
        Text(
          text = "Plan recurring or one-off estate equipment servicing",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Maintenance Title *") },
          placeholder = { Text("e.g., Seasonal HVAC servicing") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("maint_title_input"),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Scheduled Date *") },
            placeholder = { Text("Select date") },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("maint_date_input"),
            shape = RoundedCornerShape(10.dp),
            trailingIcon = {
              IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Pick Date", tint = TutsvillePrimary)
              }
            }
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = time,
          onValueChange = { time = it },
          label = { Text("Time Window") },
          placeholder = { Text("e.g., 09:00 AM - 01:00 PM") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("maint_time_input"),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Engineering Discipline", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(MaintenanceDiscipline.HVAC, MaintenanceDiscipline.ELEVATORS, MaintenanceDiscipline.FIRE_LIFE).forEach { d ->
            val sel = d == discipline
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (sel) TutsvillePrimary else TutsvilleBackground,
              border = BorderStroke(1.dp, if (sel) TutsvillePrimary else TutsvilleOutline),
              modifier = Modifier.weight(1f).clickable { discipline = d }
            ) {
              Text(
                text = d.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                  color = if (sel) Color.White else TutsvilleOnSurfaceVariant
                ),
                modifier = Modifier.padding(vertical = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = contractor,
          onValueChange = { contractor = it },
          label = { Text("Assigned Vendor") },
          placeholder = { Text("Enter or pick an onboarded vendor") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )
        if (vendors.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {},
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            vendors.take(3).forEach { v ->
              FilterChip(
                selected = contractor == v.name,
                onClick = { contractor = v.name },
                label = { Text(v.name, fontSize = 10.sp, maxLines = 1) }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = targetArea,
          onValueChange = { targetArea = it },
          label = { Text("Target Area / Unit") },
          placeholder = { Text("Enter or select unit / area") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )
        if (properties.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            properties.take(3).forEach { prop ->
              FilterChip(
                selected = targetArea == prop.unitNumber,
                onClick = { targetArea = prop.unitNumber },
                label = { Text(prop.unitNumber, fontSize = 10.sp, maxLines = 1) }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Servicing Scope / Checklist") },
          placeholder = { Text("Enter checklist or technician notes") },
          modifier = Modifier.fillMaxWidth().height(80.dp),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = {
            if (title.trim().length >= 3 && date.isNotBlank()) {
              onSchedule(
                title.trim(),
                discipline,
                date.trim(),
                time.trim(),
                contractor.trim(),
                targetArea.trim(),
                intervalMonths,
                notes.trim()
              )
            }
          },
          enabled = title.trim().length >= 3 && date.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_maintenance_button")
        ) {
          Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Add to Calendar", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun NotificationPanelSheet(
  notifications: List<ReminderItem>,
  readNoticeIds: Set<String>,
  currentRole: UserRole = UserRole.MANAGER,
  onDismiss: () -> Unit,
  onNoticeClick: (ReminderItem) -> Unit = {},
  onMarkAllRead: () -> Unit = {},
  onMarkRead: (String) -> Unit = {},
  onDeleteNotice: (String) -> Unit = {},
  onClearAllNotices: () -> Unit = {}
) {
  var selectedFilter by remember { mutableStateOf<NoticeRelatedEntityType?>(null) }
  var showClearConfirm by remember { mutableStateOf(false) }

  val filteredItems = remember(notifications, selectedFilter) {
    when (selectedFilter) {
      null -> notifications
      NoticeRelatedEntityType.CASE -> notifications.filter { it.relatedEntityType == NoticeRelatedEntityType.CASE }
      NoticeRelatedEntityType.BOOKING -> notifications.filter { it.relatedEntityType == NoticeRelatedEntityType.BOOKING }
      NoticeRelatedEntityType.ADVISORY -> notifications.filter { it.recordType == NoticeRecordType.ANNOUNCEMENT }
    }
  }
  val unreadCount = remember(notifications, readNoticeIds) {
    notifications.count { !readNoticeIds.contains(it.id) }
  }
  val caseCount = remember(notifications) {
    notifications.count { it.relatedEntityType == NoticeRelatedEntityType.CASE }
  }
  val bookingCount = remember(notifications) {
    notifications.count { it.relatedEntityType == NoticeRelatedEntityType.BOOKING }
  }
  val announcementCount = remember(notifications) {
    notifications.count { it.recordType == NoticeRecordType.ANNOUNCEMENT }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 20.dp)
        .testTag("notification_panel_sheet")
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
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = if (unreadCount > 0) "$unreadCount unread from live database" else "All caught up",
                style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 11.sp)
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close Notifications")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FilterChip(
            selected = selectedFilter == null,
            onClick = { selectedFilter = null },
            label = { Text("All (${notifications.size})", fontSize = 11.sp, maxLines = 1) }
          )
          FilterChip(
            selected = selectedFilter == NoticeRelatedEntityType.CASE,
            onClick = { selectedFilter = NoticeRelatedEntityType.CASE },
            label = { Text("Cases ($caseCount)", fontSize = 11.sp, maxLines = 1) }
          )
          FilterChip(
            selected = selectedFilter == NoticeRelatedEntityType.BOOKING,
            onClick = { selectedFilter = NoticeRelatedEntityType.BOOKING },
            label = { Text("Bookings ($bookingCount)", fontSize = 11.sp, maxLines = 1) }
          )
          FilterChip(
            selected = selectedFilter == NoticeRelatedEntityType.ADVISORY,
            onClick = { selectedFilter = NoticeRelatedEntityType.ADVISORY },
            label = { Text("Advisories ($announcementCount)", fontSize = 11.sp, maxLines = 1) }
          )
        }

        if (notifications.isNotEmpty()) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (unreadCount > 0) {
              TextButton(
                onClick = onMarkAllRead,
                modifier = Modifier.testTag("btn_mark_all_notifications_read")
              ) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp), tint = TutsvilleSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mark All Read", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvilleSecondary)
              }
            } else {
              Spacer(modifier = Modifier.width(1.dp))
            }

            if (currentRole == UserRole.MANAGER) {
              TextButton(
                onClick = { showClearConfirm = true },
                modifier = Modifier.testTag("btn_clear_all_notifications")
              ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusUrgentText)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusUrgentText)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredItems.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = TutsvilleBackground,
            border = BorderStroke(1.dp, TutsvilleOutline),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = TutsvilleSecondary,
                modifier = Modifier.size(30.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No Notifications Yet",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Case lifecycle updates, amenity bookings, and manager advisories from the database will appear here automatically.",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 11.sp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }
        } else {
          filteredItems.forEach { rem ->
            val isRead = readNoticeIds.contains(rem.id)
            val isHighPriority = rem.urgency.equals("High Priority", ignoreCase = true) ||
              rem.urgency.equals("Action Required", ignoreCase = true)
            val actorRoleLabel = if (rem.createdByRole == UserRole.MANAGER) "Manager" else "Resident"

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isRead) TutsvilleBackground else Color.White,
              border = BorderStroke(
                width = if (isRead) 1.dp else 1.3.dp,
                color = if (isHighPriority && !isRead) Color(0xFFF0B3B3) else if (!isRead) GoldAccent.copy(alpha = 0.7f) else TutsvilleOutline
              ),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onNoticeClick(rem) }
                .testTag("notification_item_${rem.id}")
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    if (!isRead) {
                      Box(
                        modifier = Modifier
                          .size(8.dp)
                          .background(GoldAccent, androidx.compose.foundation.shape.CircleShape)
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = if (rem.recordType == NoticeRecordType.ANNOUNCEMENT) StatusPendingBg else StatusCompletedBg
                    ) {
                      Text(
                        text = rem.triggerEvent.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          color = if (rem.recordType == NoticeRecordType.ANNOUNCEMENT) StatusPendingText else StatusCompletedText
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                    if (isHighPriority) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StatusUrgentBg
                      ) {
                        Text(
                          text = rem.urgency.uppercase(),
                          style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusUrgentText
                          ),
                          modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isRead) {
                      IconButton(
                        onClick = { onMarkRead(rem.id) },
                        modifier = Modifier.size(48.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.CheckCircle,
                          contentDescription = "Mark Read",
                          tint = StatusCompletedText,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }
                    if (currentRole == UserRole.MANAGER) {
                      IconButton(
                        onClick = { onDeleteNotice(rem.id) },
                        modifier = Modifier
                          .size(48.dp)
                          .testTag("delete_notice_${rem.id}")
                      ) {
                        Icon(
                          imageVector = Icons.Default.DeleteOutline,
                          contentDescription = "Delete Notification",
                          tint = StatusUrgentText,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }
                  }
                }

                Text(
                  text = rem.title,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isRead) FontWeight.SemiBold else FontWeight.Bold,
                    color = TutsvillePrimary
                  )
                )
                if (rem.message.isNotBlank()) {
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = rem.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontSize = 12.sp,
                      color = TutsvilleOnBackground
                    )
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "By ${rem.createdByName} ($actorRoleLabel)" +
                    (rem.targetUnit?.let { " • $it" } ?: "") +
                    (rem.timestamp.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = TutsvilleSecondary,
                    fontWeight = FontWeight.SemiBold
                  )
                )
              }
            }
          }
        }
      }
    }
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Clear All Notifications?", fontWeight = FontWeight.Bold, color = TutsvillePrimary) },
      text = { Text("This will permanently remove all advisories and activity notifications from the database for all users.") },
      confirmButton = {
        Button(
          onClick = {
            showClearConfirm = false
            onClearAllNotices()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
          Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel", color = TutsvilleOnSurfaceVariant)
        }
      }
    )
  }
}

