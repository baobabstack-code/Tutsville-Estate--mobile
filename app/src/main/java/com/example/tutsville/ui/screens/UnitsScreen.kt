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
import com.example.tutsville.data.model.PropertyArchitecture
import com.example.tutsville.data.model.PropertyItem
import com.example.tutsville.data.model.PropertyStatus
import com.example.tutsville.ui.components.DataColumn
import com.example.tutsville.ui.components.DeleteConfirmDialog
import com.example.tutsville.ui.components.InteractiveDataTable
import com.example.ui.theme.*
import com.example.tutsville.ui.viewmodel.EstateViewModel

@Composable
fun UnitsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val properties by viewModel.properties.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf<PropertyStatus?>(null) }

  var editingProperty by remember { mutableStateOf<PropertyItem?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }
  var deletingProperty by remember { mutableStateOf<PropertyItem?>(null) }

  val filteredProperties = remember(properties, statusFilter) {
    if (statusFilter == null) properties
    else properties.filter { it.status == statusFilter }
  }

  val columns = remember(viewModel) {
    listOf(
      DataColumn<PropertyItem>(
        header = "Unit Number",
        widthDp = 130,
        getValue = { it.unitNumber },
        cellContent = { prop ->
          Text(
            text = prop.unitNumber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        }
      ),
      DataColumn<PropertyItem>(
        header = "Wing / Pavilion",
        widthDp = 120,
        getValue = { it.pavilionWing },
        cellContent = { prop ->
          Text(text = prop.pavilionWing, fontSize = 11.sp, color = TutsvilleSecondary)
        }
      ),
      DataColumn<PropertyItem>(
        header = "Size (Sq Ft)",
        widthDp = 100,
        getValue = { "${it.architecture.sqFt}" },
        cellContent = { prop ->
          Text(
            text = "${prop.architecture.sqFt} sq ft",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        }
      ),
      DataColumn<PropertyItem>(
        header = "Layout (Beds/Baths)",
        widthDp = 130,
        getValue = { "${it.architecture.bedrooms} Bed, ${it.architecture.bathrooms} Bath" },
        cellContent = { prop ->
          Text(
            text = "${prop.architecture.bedrooms} Bed • ${prop.architecture.bathrooms.toInt()} Bath",
            fontSize = 11.sp,
            color = TutsvilleOnSurfaceVariant
          )
        }
      ),
      DataColumn<PropertyItem>(
        header = "Floor Level",
        widthDp = 110,
        getValue = { it.architecture.floorLevel },
        cellContent = { prop ->
          Text(text = prop.architecture.floorLevel, fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
        }
      ),
      DataColumn<PropertyItem>(
        header = "Monthly Rent",
        widthDp = 120,
        getValue = { it.monthlyRental },
        cellContent = { prop ->
          Text(
            text = prop.monthlyRental,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent
          )
        }
      ),
      DataColumn<PropertyItem>(
        header = "Occupancy Status",
        widthDp = 120,
        getValue = { it.status.label },
        cellContent = { prop ->
          val isAvail = prop.status == PropertyStatus.AVAILABLE
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isAvail) StatusPendingBg else StatusCompletedBg
          ) {
            Text(
              text = if (isAvail) "Available to Lease" else "Occupied / Leased",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (isAvail) StatusPendingText else StatusCompletedText,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      ),
      DataColumn<PropertyItem>(
        header = "Actions",
        widthDp = 110,
        cellContent = { prop ->
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
              onClick = { editingProperty = prop },
              modifier = Modifier.size(28.dp).testTag("btn_edit_unit_${prop.id.lowercase()}")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Unit", tint = TutsvillePrimary, modifier = Modifier.size(16.dp))
            }

            IconButton(
              onClick = { deletingProperty = prop },
              modifier = Modifier.size(28.dp).testTag("btn_delete_unit_${prop.id.lowercase()}")
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete Unit", tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
            }
          }
        }
      )
    )
  }

  Box(modifier = modifier.fillMaxSize()) {
    InteractiveDataTable(
      title = "Units & Residences Inventory",
      subtitle = "Full row-and-column ledger of residential units, size (sq ft), valuation & lease status",
      items = filteredProperties,
      columns = columns,
      searchQuery = searchQuery,
      onSearchChange = { searchQuery = it },
      onAddNew = { isAddingNew = true },
      addNewLabel = "+ List Property",
      filterChips = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FilterChip(
            selected = statusFilter == null,
            onClick = { statusFilter = null },
            label = { Text("All Units (${properties.size})", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
          )
          FilterChip(
            selected = statusFilter == PropertyStatus.AVAILABLE,
            onClick = { statusFilter = PropertyStatus.AVAILABLE },
            label = { Text("Available (${properties.count { it.status == PropertyStatus.AVAILABLE }})", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
          )
          FilterChip(
            selected = statusFilter == PropertyStatus.OCCUPIED,
            onClick = { statusFilter = PropertyStatus.OCCUPIED },
            label = { Text("Occupied (${properties.count { it.status == PropertyStatus.OCCUPIED }})", fontSize = 11.sp) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White)
          )
        }
      },
      getItemKey = { it.id }
    )

    // Add / Edit Property Modal
    if (isAddingNew || editingProperty != null) {
      val isEdit = editingProperty != null
      val current = editingProperty

      var unitNumber by remember { mutableStateOf(current?.unitNumber ?: "") }
      var wing by remember { mutableStateOf(current?.pavilionWing ?: "North Pavilion") }
      var sqFt by remember { mutableStateOf(current?.architecture?.sqFt?.toString() ?: "1850") }
      var bedrooms by remember { mutableStateOf(current?.architecture?.bedrooms?.toString() ?: "2") }
      var bathrooms by remember { mutableStateOf(current?.architecture?.bathrooms?.toInt()?.toString() ?: "2") }
      var rent by remember { mutableStateOf(current?.monthlyRental ?: "$3,500/mo") }

      AlertDialog(
        onDismissRequest = {
          isAddingNew = false
          editingProperty = null
        },
        title = {
          Text(
            text = if (isEdit) "Edit Unit Specifications" else "List New Residential Property",
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
              value = unitNumber,
              onValueChange = { unitNumber = it },
              label = { Text("Unit Number (e.g. Tower A • Apt 402)") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = wing,
              onValueChange = { wing = it },
              label = { Text("Wing / Pavilion") },
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = sqFt,
              onValueChange = { sqFt = it },
              label = { Text("Size in Sq Ft") },
              modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = bedrooms,
                onValueChange = { bedrooms = it },
                label = { Text("Bedrooms") },
                modifier = Modifier.weight(1f)
              )
              OutlinedTextField(
                value = bathrooms,
                onValueChange = { bathrooms = it },
                label = { Text("Bathrooms") },
                modifier = Modifier.weight(1f)
              )
            }
            OutlinedTextField(
              value = rent,
              onValueChange = { rent = it },
              label = { Text("Monthly Rent Valuation") },
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val sqFtInt = sqFt.toIntOrNull() ?: 1850
              val bedsInt = bedrooms.toIntOrNull() ?: 2
              val bathsFloat = bathrooms.toFloatOrNull() ?: 2f

              viewModel.listProperty(
                unitNumber = unitNumber.ifEmpty { "Apt ${properties.size + 101}" },
                pavilionWing = wing,
                architecture = PropertyArchitecture(
                  bedrooms = bedsInt,
                  bathrooms = bathsFloat,
                  sqFt = sqFtInt,
                  floorLevel = "Upper Elevation",
                  viewType = "Garden View"
                ),
                parkingSpots = listOf("Bay P-01"),
                amenities = listOf("Balcony"),
                monthlyRental = rent
              )
              isAddingNew = false
              editingProperty = null
            },
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(if (isEdit) "Save Changes" else "List Property", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          OutlinedButton(
            onClick = {
              isAddingNew = false
              editingProperty = null
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
    deletingProperty?.let { prop ->
      DeleteConfirmDialog(
        title = "Delete Property Listing?",
        message = "Are you sure you want to remove unit ${prop.unitNumber} from the estate inventory?",
        onConfirm = { deletingProperty = null },
        onDismiss = { deletingProperty = null }
      )
    }
  }
}
