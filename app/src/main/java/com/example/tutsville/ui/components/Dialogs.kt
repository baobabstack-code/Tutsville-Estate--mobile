package com.example.tutsville.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
  onDismiss: () -> Unit,
  onSubmit: (title: String, desc: String, category: CaseCategory, priority: CasePriority, location: String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var location by remember { mutableStateOf("Tower A • Apt 402") }
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
          label = { Text("Location / Unit") },
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
            if (title.isNotBlank()) {
              onSubmit(title, description, category, priority, location)
            }
          },
          enabled = title.isNotBlank(),
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

@Composable
fun BookFacilitySheet(
  facility: Facility,
  onDismiss: () -> Unit,
  onConfirm: (date: String, slot: String, guests: Int, notes: String) -> Unit
) {
  var selectedDate by remember { mutableStateOf("Tomorrow (Nov 10)") }
  var selectedSlot by remember { mutableStateOf(facility.allowedSlots.firstOrNull() ?: "02:00 PM - 05:00 PM") }
  var guestCount by remember { mutableStateOf(4) }
  var specialNotes by remember { mutableStateOf("") }

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
        Text(
          text = "Select Date",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("Today (Nov 09)", "Tomorrow (Nov 10)", "Wednesday (Nov 11)").forEach { d ->
            val sel = d == selectedDate
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (sel) TutsvillePrimary else TutsvilleBackground,
              border = BorderStroke(1.dp, if (sel) TutsvillePrimary else TutsvilleOutline),
              modifier = Modifier
                .weight(1f)
                .clickable { selectedDate = d }
            ) {
              Text(
                text = d.substringBefore(" ("),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                  color = if (sel) Color.White else TutsvilleOnSurfaceVariant
                ),
                modifier = Modifier.padding(vertical = 8.dp),
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
            val isSlotSel = slot == selectedSlot
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSlotSel) TutsvilleSecondaryContainer else TutsvilleBackground,
              border = BorderStroke(1.dp, if (isSlotSel) TutsvilleSecondary else TutsvilleOutline),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedSlot = slot }
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
                    color = if (isSlotSel) TutsvilleOnSecondaryContainer else TutsvilleOnBackground
                  )
                )
                if (isSlotSel) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(18.dp))
                }
              }
            }
          }
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
          onClick = { onConfirm(selectedDate, selectedSlot, guestCount, specialNotes) },
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
  var isBroadcast by remember { mutableStateOf(true) }

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
            text = "Dispatch Reminder",
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
          text = "Send broadcast notices or targeted task reminders",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Reminder Title") },
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
        listOf("All Residents & Tenants", "Tower A Residents Only", "Certified Vendors Pool", "Estate Operations Board").forEach { aud ->
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
        OutlinedTextField(
          value = message,
          onValueChange = { message = it },
          label = { Text("Notice Body") },
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
              onSend(title, message, audience, urgency, isBroadcast)
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
          Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Send Reminder Notice", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun ContractDetailDialog(
  contract: ContractItem,
  onDismiss: () -> Unit,
  onTriggerAlert: () -> Unit,
  onRenew: () -> Unit
) {
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
              text = "Contract Details",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = contract.type.label,
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
        ContractUrgencyBadge(urgency = contract.urgency, days = contract.daysRemaining)

        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = TutsvilleBackground,
          border = BorderStroke(1.dp, TutsvilleOutline),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Counterparty: ${contract.counterparty}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = "Annual Value: ${contract.annualValue}", style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleSecondary, fontWeight = FontWeight.Bold))
            Text(text = "Term: ${contract.startDate} to ${contract.expiryDate}", style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant))
            Text(text = "Point of Contact: ${contract.contactPerson} (${contract.contactEmail})", style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Renewal & Termination Terms",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Text(
          text = contract.renewalTerms,
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(18.dp))
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

@Composable
fun AddMaintenanceDialog(
  onDismiss: () -> Unit,
  onSchedule: (title: String, discipline: MaintenanceDiscipline, date: String, time: String, contractor: String, targetArea: String, intervalMonths: Int, notes: String) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var discipline by remember { mutableStateOf(MaintenanceDiscipline.HVAC) }
  var date by remember { mutableStateOf("Nov 20, 2026") }
  var time by remember { mutableStateOf("09:00 AM - 01:00 PM") }
  var contractor by remember { mutableStateOf("Apex HVAC Solutions") }
  var targetArea by remember { mutableStateOf("Plant Room • Wing A") }
  var intervalMonths by remember { mutableStateOf(3) }
  var notes by remember { mutableStateOf("") }

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
            text = "Schedule Servicing",
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
          label = { Text("Maintenance Title") },
          placeholder = { Text("e.g., Primary Chiller Seasonal Overhaul") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("maint_title_input"),
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
                text = d.name.lowercase().capitalize(),
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
          label = { Text("Assigned Contractor / Vendor") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = targetArea,
          onValueChange = { targetArea = it },
          label = { Text("Target Area / Wing") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Servicing Scope / Checklist") },
          modifier = Modifier.fillMaxWidth().height(80.dp),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = {
            if (title.isNotBlank()) {
              onSchedule(title, discipline, date, time, contractor, targetArea, intervalMonths, notes)
            }
          },
          enabled = title.isNotBlank(),
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
  reminders: List<ReminderItem>,
  contracts: List<ContractItem>,
  onDismiss: () -> Unit,
  onContractClick: (ContractItem) -> Unit
) {
  val criticalContracts = contracts.filter { it.urgency != ContractUrgency.HEALTHY }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 20.dp)
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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Estate Notifications",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        if (criticalContracts.isNotEmpty()) {
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "CONTRACT RUNDOWN ALERTS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StatusUrgentText, letterSpacing = 0.08.sp)
          )
          Spacer(modifier = Modifier.height(6.dp))
          criticalContracts.forEach { c ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFFAF0F0),
              border = BorderStroke(1.dp, Color(0xFFF0B3B3)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onContractClick(c) }
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = c.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = StatusUrgentText))
                  Text(text = "Expires in ${c.daysRemaining} days (${c.counterparty})", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant))
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StatusUrgentText, modifier = Modifier.size(18.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "RECENT REMINDERS & NOTICES",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary, letterSpacing = 0.08.sp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        reminders.take(4).forEach { rem ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = TutsvilleBackground,
            border = BorderStroke(1.dp, TutsvilleOutline),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(text = rem.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
              Text(text = rem.message, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnBackground))
              Spacer(modifier = Modifier.height(4.dp))
              Text(text = rem.timestamp, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleSecondary))
            }
          }
        }
      }
    }
  }
}

