package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.BookingApprovalStatus
import com.example.tutsville.data.model.Facility
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.ManageOptionsSubDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.components.TutsvilleFloatingActionButton
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmenitiesScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val facilities by viewModel.facilities.collectAsState()
  val bookings by viewModel.bookings.collectAsState()
  val estateConfigOptions by viewModel.estateConfigOptions.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val currentTenant by viewModel.currentTenant.collectAsState()
  val currentUserDisplayName by viewModel.currentUserDisplayName.collectAsState()
  val visibleBookings = remember(bookings, currentRole, currentTenant, currentUserDisplayName) {
    if (currentRole == com.example.tutsville.data.model.UserRole.TENANT) {
      if (viewModel.isUsingFirestore) {
        bookings
      } else {
        val tName = currentTenant?.fullName?.trim().orEmpty()
        val tUnit = currentTenant?.assignedPropertyUnit?.trim().orEmpty()
        val authName = currentUserDisplayName?.trim().orEmpty()
        bookings.filter { b ->
          (tName.isNotEmpty() && b.residentName.equals(tName, ignoreCase = true)) ||
            (tUnit.isNotEmpty() && b.unit.equals(tUnit, ignoreCase = true)) ||
            (authName.isNotEmpty() && b.residentName.equals(authName, ignoreCase = true))
        }
      }
    } else {
      bookings
    }
  }
  var searchQuery by remember { mutableStateOf("") }
  var selectedTagFilter by remember { mutableStateOf<String?>(null) }

  var editingFacility by remember { mutableStateOf<Facility?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingFacility by remember { mutableStateOf<Facility?>(null) }

  val filteredFacilities = remember(facilities, searchQuery, selectedTagFilter) {
    facilities.filter { fac ->
      val matchesTag = selectedTagFilter == null || fac.tag.equals(selectedTagFilter, ignoreCase = true)
      val matchesSearch = if (searchQuery.isBlank()) true
      else {
        fac.name.contains(searchQuery, ignoreCase = true) ||
        fac.location.contains(searchQuery, ignoreCase = true) ||
        fac.tag.contains(searchQuery, ignoreCase = true) ||
        fac.description.contains(searchQuery, ignoreCase = true)
      }
      matchesTag && matchesSearch
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("amenities_card_list"),
      contentPadding = PaddingValues(bottom = 104.dp)
    ) {
      // 1. Header & Active Passes Banner
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Amenities",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Clubhouse passes, sports courts, heated pools & private screening room",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Amenity Bookings & Approval Requests
          if (visibleBookings.isNotEmpty()) {
            Text(
              text = "Amenity Bookings & Requests (${visibleBookings.size})",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvilleSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))

            visibleBookings.forEach { booking ->
              val (pillBg, pillText, pillDot, pillLabel) = when (booking.approvalStatus) {
                BookingApprovalStatus.PENDING -> listOf(StatusPendingBg, StatusPendingText, GoldAccent, "Pending Approval")
                BookingApprovalStatus.APPROVED -> listOf(StatusCompletedBg, StatusCompletedText, StatusCompletedText, "Approved Pass")
                BookingApprovalStatus.DECLINED -> listOf(StatusUrgentBg, StatusUrgentText, StatusUrgentText, "Declined")
              }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = TutsvilleCardBg,
                border = BorderStroke(1.dp, (pillDot as Color).copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      StatusPill(
                        text = pillLabel as String,
                        bgColor = pillBg as Color,
                        textColor = pillText as Color,
                        dotColor = pillDot
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        text = booking.facilityName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                      )
                      Text(
                        text = "${booking.date}  |  ${booking.timeSlot}  •  ${booking.residentName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary, fontWeight = FontWeight.Bold)
                      )
                    }
                    if (booking.approvalStatus != BookingApprovalStatus.DECLINED) {
                      IconButton(
                        onClick = { viewModel.cancelBooking(booking.id) },
                        modifier = Modifier.size(48.dp)
                      ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Pass", tint = TutsvilleOnSurfaceVariant)
                      }
                    }
                  }

                  if (currentRole == UserRole.MANAGER && booking.approvalStatus == BookingApprovalStatus.PENDING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Button(
                        onClick = { viewModel.approveBooking(booking.id) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier
                          .weight(1f)
                          .heightIn(min = 48.dp)
                          .testTag("btn_approve_booking_${booking.id.lowercase()}")
                      ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                      }
                      OutlinedButton(
                        onClick = { viewModel.declineBooking(booking.id) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StatusUrgentText),
                        modifier = Modifier
                          .weight(1f)
                          .heightIn(min = 48.dp)
                          .testTag("btn_decline_booking_${booking.id.lowercase()}")
                      ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = StatusUrgentText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Decline", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusUrgentText)
                      }
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
          }

          // Search Bar
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("input_search_amenities"),
            placeholder = { Text("Search amenities...", fontSize = 13.sp) },
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

          // Category Tags Filter Chips
          val allTags = remember(facilities) {
            facilities.map { it.tag.trim() }.filter { it.isNotEmpty() }.distinct()
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = selectedTagFilter == null,
              onClick = { selectedTagFilter = null },
              label = { Text("All Amenities (${facilities.size})", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
            )
            allTags.forEach { tag ->
              FilterChip(
                selected = selectedTagFilter == tag,
                onClick = { selectedTagFilter = if (selectedTagFilter == tag) null else tag },
                label = { Text(tag, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      // 2. Amenity Cards List
      if (filteredFacilities.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No amenities match the search criteria.",
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        }
      } else {
        items(filteredFacilities, key = { it.id }) { facility ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard {
              // Top Row: Tag, Capacity & Name
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                StatusPill(
                  text = facility.tag.ifBlank { "Amenity" },
                  bgColor = TutsvilleSecondaryContainer,
                  textColor = TutsvilleOnSecondaryContainer,
                  dotColor = TutsvilleSecondary
                )
                if (facility.capacity > 0) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = TutsvilleOnSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Cap: ${facility.capacity} Max",
                      style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = facility.name,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )

              if (facility.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = facility.description,
                  style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnBackground)
                )
              }

              if (facility.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Place, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(text = facility.location, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant))
                }
              }

              if (facility.rules.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Notes: ${facility.rules}",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
                )
              }

              if (facility.allowedSlots.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "No bookable time slots configured yet",
                  style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Medium)
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Minimal Card Action Buttons Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Button(
                  onClick = { viewModel.activeBookFacility.value = facility },
                  enabled = facility.allowedSlots.isNotEmpty(),
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                  modifier = Modifier.weight(1f).height(48.dp).testTag("btn_book_facility_${facility.id.lowercase()}")
                ) {
                  Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Book Session", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (currentRole == UserRole.MANAGER) {
                  IconButton(
                    onClick = { editingFacility = facility },
                    modifier = Modifier.size(48.dp).testTag("btn_edit_amenity_card_${facility.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Amenity", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                  }

                  IconButton(
                    onClick = { deletingFacility = facility },
                    modifier = Modifier.size(48.dp).testTag("btn_delete_amenity_card_${facility.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Amenity", tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                  }
                }
              }
            }
          }
        }
      }
    }

    if (currentRole == UserRole.MANAGER) {
      TutsvilleFloatingActionButton(
        text = "Add Amenity",
        icon = Icons.Default.Add,
        onClick = { isAddingNew = true },
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 20.dp, bottom = 20.dp),
        testTag = "fab_add_amenity"
      )
    }

    // Add / Edit Amenity Modal (Optional-First: only Amenity Name required)
    if (isAddingNew || editingFacility != null) {
      val isEdit = editingFacility != null
      val current = editingFacility

      var name by remember(current?.id, isAddingNew) { mutableStateOf(current?.name ?: "") }
      var tag by remember(current?.id, isAddingNew) { mutableStateOf(current?.tag ?: "") }
      var capacity by remember(current?.id, isAddingNew) {
        mutableStateOf(current?.capacity?.takeIf { it > 0 }?.toString() ?: "")
      }
      var location by remember(current?.id, isAddingNew) { mutableStateOf(current?.location ?: "") }
      var desc by remember(current?.id, isAddingNew) { mutableStateOf(current?.description ?: "") }
      var slotsInput by remember(current?.id, isAddingNew) {
        mutableStateOf(current?.allowedSlots?.joinToString(", ") ?: "")
      }
      var rules by remember(current?.id, isAddingNew) { mutableStateOf(current?.rules ?: "") }

      var tagExpanded by remember { mutableStateOf(false) }
      var showManageTags by remember { mutableStateOf(false) }
      var showManageTimes by remember { mutableStateOf(false) }

      val managedTags = estateConfigOptions.amenityCategoryTags
      val dropdownTags = remember(managedTags, tag) {
        (managedTags + listOfNotNull(tag.trim().takeIf { it.isNotEmpty() })).distinct()
      }
      val managedTimes = estateConfigOptions.amenityTimeSlots

      val capInt = capacity.trim().toIntOrNull()
      val parsedSlots = remember(slotsInput) {
        slotsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
      }
      val allSlotChips = remember(managedTimes, parsedSlots) {
        (managedTimes + parsedSlots).distinct()
      }
      val hasValidSlots = parsedSlots.isEmpty() || (parsedSlots.size <= 30 && parsedSlots.all { it.length in 3..80 })
      val isFormValid = name.trim().length >= 2 && hasValidSlots

      AlertDialog(
        onDismissRequest = {
          isAddingNew = false
          editingFacility = null
        },
        title = {
          Text(
            text = if (isEdit) "Edit Amenity Specifications" else "Register New Amenity",
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        },
        text = {
          Column(
            modifier = Modifier
              .verticalScroll(rememberScrollState())
              .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = name,
              onValueChange = { name = it },
              label = { Text("Amenity Name *") },
              placeholder = { Text("e.g. Clubhouse Lounge") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("input_amenity_name")
            )

            // Category Tag Dropdown + Manage Options
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Category Tag (Optional)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              )
              TextButton(
                onClick = { showManageTags = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("btn_manage_amenity_tags_screen")
              ) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manage Options", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TutsvilleSecondary)
              }
            }

            ExposedDropdownMenuBox(
              expanded = tagExpanded,
              onExpandedChange = { tagExpanded = !tagExpanded },
              modifier = Modifier.fillMaxWidth()
            ) {
              OutlinedTextField(
                value = tag,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category Tag") },
                placeholder = { Text("Select category tag (optional)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tagExpanded) },
                singleLine = true,
                modifier = Modifier
                  .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                  .fillMaxWidth()
                  .testTag("dropdown_amenity_category_tag")
              )
              ExposedDropdownMenu(
                expanded = tagExpanded,
                onDismissRequest = { tagExpanded = false }
              ) {
                DropdownMenuItem(
                  text = { Text("None (Optional)", color = TutsvilleOnSurfaceVariant) },
                  onClick = {
                    tag = ""
                    tagExpanded = false
                  }
                )
                dropdownTags.forEach { option ->
                  DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                      tag = option
                      tagExpanded = false
                    }
                  )
                }
              }
            }

            OutlinedTextField(
              value = capacity,
              onValueChange = { capacity = it.filter { ch -> ch.isDigit() } },
              label = { Text("Max Occupancy Capacity") },
              placeholder = { Text("Optional (e.g. 20)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = location,
              onValueChange = { location = it },
              label = { Text("Physical Location") },
              placeholder = { Text("Optional (e.g. North Wing Level 2)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = desc,
              onValueChange = { desc = it },
              label = { Text("Description") },
              placeholder = { Text("Optional amenity details and equipment") },
              modifier = Modifier.fillMaxWidth()
            )

            // Managed Times Section
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Available Times (Optional)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              )
              TextButton(
                onClick = { showManageTimes = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                  .heightIn(min = 48.dp)
                  .testTag("btn_manage_amenity_times_screen")
              ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manage Times", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TutsvilleSecondary)
              }
            }

            OutlinedTextField(
              value = slotsInput,
              onValueChange = { slotsInput = it },
              label = { Text("Bookable Time Slots") },
              placeholder = { Text("Optional (tap times below or enter comma-separated)") },
              isError = !hasValidSlots,
              modifier = Modifier.fillMaxWidth().testTag("input_amenity_slots")
            )
            if (allSlotChips.isNotEmpty()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                allSlotChips.forEach { preset ->
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
              Text(
                text = "Each time slot must be between 3 and 80 characters (max 30).",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = StatusUrgentText)
              )
            }
            OutlinedTextField(
              value = rules,
              onValueChange = { rules = it },
              label = { Text("Notes") },
              placeholder = { Text("Optional amenity notes or guidelines") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val safeCap = (capInt ?: 0).coerceAtLeast(0)
              if (isEdit && current != null) {
                viewModel.updateFacility(
                  facilityId = current.id,
                  name = name,
                  tag = tag,
                  capacity = safeCap,
                  location = location,
                  description = desc,
                  amenities = current.amenities,
                  slots = parsedSlots,
                  rules = rules
                )
              } else {
                viewModel.submitNewFacility(
                  name = name,
                  tag = tag,
                  capacity = safeCap,
                  location = location,
                  description = desc,
                  amenities = emptyList(),
                  slots = parsedSlots,
                  rules = rules
                )
              }
              isAddingNew = false
              editingFacility = null
            },
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .heightIn(min = 48.dp)
              .testTag("btn_save_amenity_screen")
          ) {
            Text(if (isEdit) "Save Changes" else "Create Amenity", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = {
              isAddingNew = false
              editingFacility = null
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.heightIn(min = 48.dp)
          ) {
            Text("Cancel", color = TutsvillePrimary)
          }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
      )

      if (showManageTags) {
        ManageOptionsSubDialog(
          title = "Manage Category Tags",
          options = managedTags,
          placeholder = "e.g., Wellness, Sports, Lounge",
          minItems = 0,
          onDismiss = { showManageTags = false },
          onSave = { updated ->
            viewModel.updateAmenityCategoryTags(updated)
            if (tag.isNotBlank() && updated.none { it.equals(tag, ignoreCase = true) }) {
              tag = ""
            }
            showManageTags = false
          }
        )
      }

      if (showManageTimes) {
        ManageOptionsSubDialog(
          title = "Manage Amenity Times",
          options = managedTimes,
          placeholder = "e.g., 09:00 AM - 11:00 AM",
          minItems = 0,
          saveButtonLabel = "Save Times",
          onDismiss = { showManageTimes = false },
          onSave = { updated ->
            viewModel.updateAmenityTimeSlots(updated)
            showManageTimes = false
          }
        )
      }
    }

    // Delete Confirmation Modal
    deletingFacility?.let { fac ->
      DeleteConfirmDialog(
        title = "Delete Amenity?",
        message = "Are you sure you want to delete ${fac.name} from the estate amenity directory?",
        onConfirm = {
          viewModel.deleteFacility(fac.id)
          deletingFacility = null
        },
        onDismiss = { deletingFacility = null }
      )
    }
  }
}
