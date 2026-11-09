package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.Facility
import com.example.tutsville.data.model.FacilityBooking
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun FacilitiesBookingScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val facilities by viewModel.facilities.collectAsState()
  val bookings by viewModel.bookings.collectAsState()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground),
    contentPadding = PaddingValues(bottom = 90.dp)
  ) {
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
          text = "Estate Amenities & Facilities",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        )
        Text(
          text = "Instant slot reservations for residential clubhouses, sports & lounges",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )
      }
    }

    // Active Reservations Section
    if (bookings.isNotEmpty()) {
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          Text(
            text = "Active Estate Passes (${bookings.size})",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = TutsvilleSecondary,
              letterSpacing = 0.04.sp
            )
          )
          Spacer(modifier = Modifier.height(8.dp))
        }
      }

      items(bookings, key = { it.id }) { booking ->
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = TutsvilleCardBg,
            border = BorderStroke(1.dp, TutsvilleSecondary),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                StatusPill(
                  text = "Confirmed Pass",
                  bgColor = StatusCompletedBg,
                  textColor = StatusCompletedText,
                  dotColor = StatusCompletedText
                )
                Text(
                  text = booking.id,
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TutsvilleSecondary
                  )
                )
              }

              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = booking.facilityName,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = "Reserved for: ${booking.residentName} (${booking.unit}) • ${booking.guestCount} guests",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnBackground)
              )

              Spacer(modifier = Modifier.height(6.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = booking.date, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = booking.timeSlot, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
              }

              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                TextButton(
                  onClick = { viewModel.cancelBooking(booking.id) },
                  colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFBA1A1A)),
                  modifier = Modifier.testTag("cancel_booking_${booking.id.lowercase()}")
                ) {
                  Text("Cancel Reservation", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(14.dp))
      }
    }

    // Available Facilities Catalog
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
          text = "Available Estate Venues",
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary,
            letterSpacing = 0.04.sp
          )
        )
        Spacer(modifier = Modifier.height(6.dp))
      }
    }

    items(facilities, key = { it.id }) { facility ->
      Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        OperationalCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = TutsvilleSecondaryContainer
            ) {
              Text(
                text = facility.tag.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 10.sp,
                  color = TutsvilleOnSecondaryContainer
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Group, contentDescription = null, tint = TutsvilleOnSurfaceVariant, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Up to ${facility.capacity}",
                style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant)
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
          // Amenity Tags
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(facility.amenities) { tag ->
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = TutsvilleBackground,
                border = BorderStroke(1.dp, TutsvilleOutline)
              ) {
                Text(
                  text = tag,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${facility.allowedSlots.size} slots daily",
              style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleSecondary, fontWeight = FontWeight.Bold)
            )
            Button(
              onClick = { viewModel.activeBookFacility.value = facility },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
              modifier = Modifier
                .height(38.dp)
                .testTag("book_facility_${facility.id.lowercase()}")
            ) {
              Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Book Facility", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
