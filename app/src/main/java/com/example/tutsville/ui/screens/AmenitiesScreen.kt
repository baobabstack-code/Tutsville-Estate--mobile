package com.example.tutsville.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.Facility
import com.example.tutsville.ui.components.DataColumn
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.InteractiveDataTable
import com.example.ui.theme.*
import com.example.tutsville.ui.viewmodel.EstateViewModel

@Composable
fun AmenitiesScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val facilities by viewModel.facilities.collectAsState()
  var searchQuery by remember { mutableStateOf("") }

  var editingFacility by remember { mutableStateOf<Facility?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingFacility by remember { mutableStateOf<Facility?>(null) }

  val columns = remember(viewModel) {
    listOf(
      DataColumn<Facility>(
        header = "Amenity Code",
        widthDp = 110,
        getValue = { "AMN-${it.id.takeLast(3)}" },
        cellContent = { fac ->
          Text(
            text = "AMN-${fac.id.takeLast(3)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvilleSecondary
          )
        }
      ),
      DataColumn<Facility>(
        header = "Amenity Name",
        widthDp = 150,
        getValue = { it.name },
        cellContent = { fac ->
          Text(
            text = fac.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        }
      ),
      DataColumn<Facility>(
        header = "Category / Tag",
        widthDp = 110,
        getValue = { it.tag },
        cellContent = { fac ->
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = TutsvilleSecondaryContainer
          ) {
            Text(
              text = fac.tag,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = TutsvilleOnSecondaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      ),
      DataColumn<Facility>(
        header = "Capacity",
        widthDp = 90,
        getValue = { "${it.capacity}" },
        cellContent = { fac ->
          Text(
            text = "${fac.capacity} Max",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        }
      ),
      DataColumn<Facility>(
        header = "Venue Location",
        widthDp = 140,
        getValue = { it.location },
        cellContent = { fac ->
          Text(text = fac.location, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<Facility>(
        header = "Daily Slots",
        widthDp = 110,
        getValue = { "${it.allowedSlots.size} slots" },
        cellContent = { fac ->
          Text(
            text = "${fac.allowedSlots.size} slots daily",
            fontSize = 11.sp,
            color = GoldAccent,
            fontWeight = FontWeight.Bold
          )
        }
      ),
      DataColumn<Facility>(
        header = "Actions",
        widthDp = 110,
        cellContent = { fac ->
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
              onClick = { viewModel.activeBookFacility.value = fac },
              modifier = Modifier.size(28.dp).testTag("btn_book_amenity_${fac.id.lowercase()}")
            ) {
              Icon(Icons.Default.BookmarkAdd, contentDescription = "Book Session", tint = TutsvillePrimary, modifier = Modifier.size(16.dp))
            }

            IconButton(
              onClick = { editingFacility = fac },
              modifier = Modifier.size(28.dp).testTag("btn_edit_amenity_${fac.id.lowercase()}")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Amenity", tint = TutsvilleSecondary, modifier = Modifier.size(16.dp))
            }

            IconButton(
              onClick = { deletingFacility = fac },
              modifier = Modifier.size(28.dp).testTag("btn_delete_amenity_${fac.id.lowercase()}")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Amenity", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
            }
          }
        }
      )
    )
  }

  Box(modifier = modifier.fillMaxSize()) {
    InteractiveDataTable(
      title = "Estate Amenities & Facilities Catalog",
      subtitle = "Full row-and-column ledger of estate clubhouses, wellness facilities & reservation rules",
      items = facilities,
      columns = columns,
      searchQuery = searchQuery,
      onSearchChange = { searchQuery = it },
      onAddNew = { isAddingNew = true },
      addNewLabel = "+ Add Amenity",
      getItemKey = { it.id }
    )

    // Add / Edit Amenity Modal
    if (isAddingNew || editingFacility != null) {
      val isEdit = editingFacility != null
      val current = editingFacility

      var name by remember { mutableStateOf(current?.name ?: "") }
      var tag by remember { mutableStateOf(current?.tag ?: "Wellness") }
      var capacity by remember { mutableStateOf(current?.capacity?.toString() ?: "20") }
      var location by remember { mutableStateOf(current?.location ?: "Clubhouse Terrace") }
      var desc by remember { mutableStateOf(current?.description ?: "") }

      AlertDialog(
        onDismissRequest = {
          isAddingNew = false
          editingFacility = null
        },
        title = {
          Text(
            text = if (isEdit) "Edit Amenity Specifications" else "Register New Estate Amenity",
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
              label = { Text("Amenity Title") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = tag,
              onValueChange = { tag = it },
              label = { Text("Category Tag (e.g. Sports / Wellness)") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = capacity,
              onValueChange = { capacity = it },
              label = { Text("Max Occupancy Capacity") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = location,
              onValueChange = { location = it },
              label = { Text("Physical Location") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = desc,
              onValueChange = { desc = it },
              label = { Text("Description & Usage Rules") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val capInt = capacity.toIntOrNull() ?: 20
              viewModel.submitNewFacility(
                name = name.ifEmpty { "New Estate Amenity" },
                tag = tag,
                capacity = capInt,
                location = location,
                description = desc.ifEmpty { "Private resident facility" },
                amenities = listOf("Air Conditioned", "Reservable"),
                slots = listOf("08:00 AM - 10:00 AM", "02:00 PM - 04:00 PM"),
                rules = "Resident access pass required."
              )
              isAddingNew = false
              editingFacility = null
            },
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
        title = "Delete Amenity Facility?",
        message = "Are you sure you want to remove ${fac.name} from the estate amenity directory?",
        onConfirm = { deletingFacility = null },
        onDismiss = { deletingFacility = null }
      )
    }
  }
}
