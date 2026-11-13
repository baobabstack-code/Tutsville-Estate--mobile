package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.tutsville.data.model.Facility
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun AmenitiesScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val facilities by viewModel.facilities.collectAsState()
  val bookings by viewModel.bookings.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
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
      contentPadding = PaddingValues(bottom = 96.dp)
    ) {
      // 1. Header & Active Passes Banner
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
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

            if (currentRole == com.example.tutsville.data.model.UserRole.MANAGER) {
              Button(
                onClick = { isAddingNew = true },
                colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(40.dp).testTag("btn_add_amenity_card_view")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Add Amenity", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Active Passes Header
          if (bookings.isNotEmpty()) {
            Text(
              text = "Active Estate Passes (${bookings.size})",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvilleSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))

            bookings.forEach { booking ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = TutsvilleCardBg,
                border = BorderStroke(1.dp, TutsvilleSecondary),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    StatusPill(text = "Confirmed Pass", bgColor = StatusCompletedBg, textColor = StatusCompletedText, dotColor = StatusCompletedText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = booking.facilityName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
                    Text(text = "${booking.date}  |  ${booking.timeSlot}", style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary, fontWeight = FontWeight.Bold))
                  }
                  IconButton(onClick = { viewModel.cancelBooking(booking.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel Pass", tint = TutsvilleOnSurfaceVariant)
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
          val allTags = remember(facilities) { facilities.map { it.tag }.distinct() }
          Row(
            modifier = Modifier.fillMaxWidth(),
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
                  text = facility.tag,
                  bgColor = TutsvilleSecondaryContainer,
                  textColor = TutsvilleOnSecondaryContainer,
                  dotColor = TutsvilleSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.People, contentDescription = null, tint = TutsvilleOnSurfaceVariant, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Cap: ${facility.capacity} Max",
                    style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontWeight = FontWeight.Bold)
                  )
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

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = facility.description,
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnBackground)
              )

              Spacer(modifier = Modifier.height(8.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Place, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = facility.location, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant))
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Timeslot Badges
              LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(facility.allowedSlots) { slot ->
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = TutsvilleBackground,
                    border = BorderStroke(1.dp, TutsvilleOutline)
                  ) {
                    Text(
                      text = slot,
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
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
                  onClick = { viewModel.activeBookFacility.value = facility },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  modifier = Modifier.weight(1f).height(38.dp).testTag("btn_book_facility_${facility.id.lowercase()}")
                ) {
                  Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Book Session", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (currentRole == com.example.tutsville.data.model.UserRole.MANAGER) {
                  IconButton(
                    onClick = { editingFacility = facility },
                    modifier = Modifier.size(38.dp).testTag("btn_edit_amenity_card_${facility.id.lowercase()}")
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Amenity", tint = TutsvillePrimary, modifier = Modifier.size(18.dp))
                  }

                  IconButton(
                    onClick = { deletingFacility = facility },
                    modifier = Modifier.size(38.dp).testTag("btn_delete_amenity_card_${facility.id.lowercase()}")
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

    // Add / Edit Amenity Modal
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

      val capInt = capacity.trim().toIntOrNull()
      val isFormValid = name.trim().length >= 2 &&
        tag.trim().isNotBlank() &&
        (capInt ?: 0) >= 1 &&
        location.trim().isNotBlank()

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
              label = { Text("Amenity Title *") },
              placeholder = { Text("e.g. Clubhouse Lounge") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = tag,
              onValueChange = { tag = it },
              label = { Text("Category Tag *") },
              placeholder = { Text("e.g. Wellness, Sports, Dining") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = capacity,
              onValueChange = { capacity = it },
              label = { Text("Max Occupancy Capacity *") },
              placeholder = { Text("e.g. 20") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = location,
              onValueChange = { location = it },
              label = { Text("Physical Location *") },
              placeholder = { Text("e.g. North Wing Level 2") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = desc,
              onValueChange = { desc = it },
              label = { Text("Description") },
              placeholder = { Text("Amenity details and equipment") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = slotsInput,
              onValueChange = { slotsInput = it },
              label = { Text("Bookable Time Slots") },
              placeholder = { Text("e.g. 08:00 AM - 10:00 AM, 02:00 PM - 04:00 PM") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = rules,
              onValueChange = { rules = it },
              label = { Text("Usage Rules") },
              placeholder = { Text("Resident usage rules (optional)") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val parsedSlots = slotsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
              val safeCap = capInt ?: 1
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
            shape = RoundedCornerShape(8.dp)
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
