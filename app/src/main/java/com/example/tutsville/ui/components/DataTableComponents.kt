package com.example.tutsville.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tutsville.data.model.*
import com.example.ui.theme.*

/**
 * Reusable Column Header descriptor for InteractiveDataTable
 */
data class DataColumn<T>(
  val header: String,
  val weight: Float = 1f,
  val widthDp: Int = 130,
  val getValue: (T) -> String = { "" },
  val cellContent: @Composable (T) -> Unit
)

/**
 * Generic Interactive Data Table with Sticky Header, Search, Column Sorting, and Horizontal Scroll
 */
@Composable
fun <T> InteractiveDataTable(
  title: String,
  subtitle: String,
  items: List<T>,
  columns: List<DataColumn<T>>,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  onAddNew: (() -> Unit)? = null,
  addNewLabel: String = "Add Record",
  filterChips: @Composable (() -> Unit)? = null,
  emptyMessage: String = "No records found matching table filters.",
  getItemKey: (T) -> String
) {
  var sortColumnIndex by remember { mutableStateOf<Int?>(null) }
  var sortAscending by remember { mutableStateOf(true) }

  val filteredAndSortedItems = remember(items, searchQuery, sortColumnIndex, sortAscending) {
    var result = items.filter { item ->
      if (searchQuery.isBlank()) true
      else {
        columns.any { col ->
          col.getValue(item).contains(searchQuery, ignoreCase = true)
        }
      }
    }
    sortColumnIndex?.let { colIdx ->
      val col = columns.getOrNull(colIdx)
      if (col != null) {
        result = if (sortAscending) {
          result.sortedBy { col.getValue(it) }
        } else {
          result.sortedByDescending { col.getValue(it) }
        }
      }
    }
    result
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Title & Primary Add Action Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )
      }

      onAddNew?.let { onAdd ->
        Button(
          onClick = onAdd,
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier
            .height(40.dp)
            .testTag("btn_table_add_new_${title.lowercase().replace(" ", "_")}")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(addNewLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Live Search Input Box
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("input_table_search_${title.lowercase().replace(" ", "_")}"),
      placeholder = { Text("Search rows & columns...", fontSize = 13.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TutsvilleSecondary) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchChange("") }) {
            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TutsvilleOnSurfaceVariant)
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

    filterChips?.let {
      Spacer(modifier = Modifier.height(8.dp))
      it()
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Interactive Scrollable Data Table Container
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = Color.White,
      border = BorderStroke(1.dp, TutsvilleOutline),
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      val horizontalScrollState = rememberScrollState()

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(horizontalScrollState)
      ) {
        // Table Header Row (#1B3B2B Forest Emerald)
        Row(
          modifier = Modifier
            .background(TutsvillePrimary)
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          columns.forEachIndexed { index, col ->
            Box(
              modifier = Modifier
                .width(col.widthDp.dp)
                .clickable {
                  if (sortColumnIndex == index) {
                    sortAscending = !sortAscending
                  } else {
                    sortColumnIndex = index
                    sortAscending = true
                  }
                }
                .padding(horizontal = 4.dp),
              contentAlignment = Alignment.CenterStart
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = col.header.uppercase(),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 11.sp
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                if (sortColumnIndex == index) {
                  Icon(
                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(12.dp).padding(start = 2.dp)
                  )
                }
              }
            }
          }
        }

        HorizontalDivider(color = TutsvilleOutline)

        // Table Data Rows
        if (filteredAndSortedItems.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = emptyMessage,
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
        } else {
          filteredAndSortedItems.forEachIndexed { rowIndex, item ->
            val bg = if (rowIndex % 2 == 0) Color.White else Color(0xFFFAF8F5)
            Row(
              modifier = Modifier
                .background(bg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              columns.forEach { col ->
                Box(
                  modifier = Modifier
                    .width(col.widthDp.dp)
                    .padding(horizontal = 4.dp),
                  contentAlignment = Alignment.CenterStart
                ) {
                  col.cellContent(item)
                }
              }
            }
            if (rowIndex < filteredAndSortedItems.size - 1) {
              HorizontalDivider(color = TutsvilleOutline.copy(alpha = 0.5f))
            }
          }
        }
      }
    }
  }
}

/**
 * Generic Delete Confirmation Modal
 */
@Composable
fun DeleteConfirmDialog(
  title: String,
  message: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = TutsvillePrimary, fontWeight = FontWeight.Bold)
      }
    },
    text = { Text(message, color = TutsvilleOnSurfaceVariant, fontSize = 13.sp) },
    confirmButton = {
      Button(
        onClick = {
          onConfirm()
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Delete Record", fontWeight = FontWeight.Bold, color = Color.White)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Cancel", color = TutsvillePrimary)
      }
    },
    containerColor = Color.White,
    shape = RoundedCornerShape(16.dp)
  )
}
