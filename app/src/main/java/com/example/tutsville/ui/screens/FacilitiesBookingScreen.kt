package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.Facility
import com.example.tutsville.data.model.FacilityBooking
import com.example.tutsville.data.model.PropertyItem
import com.example.tutsville.data.model.PropertyStatus
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
  val properties by viewModel.properties.collectAsState()

  // 0 = Estate Amenities & Passes, 1 = Properties & Residences
  var selectedSection by remember { mutableStateOf(0) }
  var propertyFilter by remember { mutableStateOf<PropertyStatus?>(null) }

  val filteredProperties = remember(properties, propertyFilter) {
    if (propertyFilter == null) properties else properties.filter { it.status == propertyFilter }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground)
      .testTag("facilities_screen_lazy_column"),
    contentPadding = PaddingValues(bottom = 96.dp)
  ) {
    // 1. Header & Section Switcher
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (selectedSection == 0) "Estate Amenities & Facilities" else "Properties & Residences",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = if (selectedSection == 0)
                "Clubhouse passes, sports courts & private screening room"
              else
                "Manage residential listings, tenant assignments & invite links",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
            )
          }

          if (selectedSection == 1) {
            Button(
              onClick = { viewModel.activeListPropertyDialog.value = true },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.height(36.dp).testTag("list_new_property_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("List Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Segmented Switcher Control
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFEBE6DC),
          modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxSize().padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            // Tab 0: Amenities & Passes
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (selectedSection == 0) Color.White else Color.Transparent,
              shadowElevation = if (selectedSection == 0) 2.dp else 0.dp,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { selectedSection = 0 }
                .testTag("tab_amenities_facilities")
            ) {
              Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Apartment,
                  contentDescription = null,
                  tint = if (selectedSection == 0) TutsvillePrimary else TutsvilleOnSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Amenities & Passes",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedSection == 0) TutsvillePrimary else TutsvilleOnSurfaceVariant
                  )
                )
              }
            }

            // Tab 1: Properties & Residences
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (selectedSection == 1) Color.White else Color.Transparent,
              shadowElevation = if (selectedSection == 1) 2.dp else 0.dp,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { selectedSection = 1 }
                .testTag("tab_properties_residences")
            ) {
              Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.VpnKey,
                  contentDescription = null,
                  tint = if (selectedSection == 1) TutsvillePrimary else TutsvilleOnSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Properties (${properties.size})",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedSection == 1) TutsvillePrimary else TutsvilleOnSurfaceVariant
                  )
                )
              }
            }
          }
        }
      }
    }

    // ==========================================
    // SECTION 1: PROPERTIES & TENANT ASSIGNMENTS
    // ==========================================
    if (selectedSection == 1) {
      item {
        // High-level Occupancy Telemetry
        val occupiedCount = properties.count { it.status == PropertyStatus.OCCUPIED }
        val availableCount = properties.count { it.status == PropertyStatus.AVAILABLE }

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "TOTAL UNITS",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant)
                )
                Text(
                  text = "${properties.size}",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "OCCUPIED / LEASED",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = StatusCompletedText)
                )
                Text(
                  text = "$occupiedCount",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = StatusCompletedText)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "AVAILABLE TO LEASE",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = GoldAccent)
                )
                Text(
                  text = "$availableCount",
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = GoldAccent)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Filter chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = propertyFilter == null,
              onClick = { propertyFilter = null },
              label = { Text("All Properties (${properties.size})") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TutsvillePrimary,
                selectedLabelColor = Color.White
              )
            )
            FilterChip(
              selected = propertyFilter == PropertyStatus.AVAILABLE,
              onClick = { propertyFilter = PropertyStatus.AVAILABLE },
              label = { Text("Available ($availableCount)") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TutsvillePrimary,
                selectedLabelColor = Color.White
              )
            )
            FilterChip(
              selected = propertyFilter == PropertyStatus.OCCUPIED,
              onClick = { propertyFilter = PropertyStatus.OCCUPIED },
              label = { Text("Occupied ($occupiedCount)") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TutsvillePrimary,
                selectedLabelColor = Color.White
              )
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
        }
      }

      // Property Cards List
      items(filteredProperties, key = { it.id }) { property ->
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          OperationalCard(
            border = BorderStroke(
              1.dp,
              if (property.status == PropertyStatus.AVAILABLE) GoldAccent.copy(alpha = 0.6f) else TutsvilleOutline
            )
          ) {
            // Header Row: Unit name, Wing, and Occupancy Status Pill
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = property.unitNumber,
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = TutsvillePrimary
                  )
                )
                Text(
                  text = property.pavilionWing,
                  style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary, fontWeight = FontWeight.SemiBold)
                )
              }

              if (property.status == PropertyStatus.AVAILABLE) {
                StatusPill(
                  text = "Available",
                  bgColor = StatusPendingBg,
                  textColor = StatusPendingText,
                  dotColor = GoldAccent
                )
              } else {
                StatusPill(
                  text = "Leased",
                  bgColor = StatusCompletedBg,
                  textColor = StatusCompletedText,
                  dotColor = Color(0xFF2E7D32)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Architectural Specs Grid (Bedrooms, Bathrooms, Sq Ft, Floor Level)
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFF9F7F2),
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("BEDS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                  Text("${property.architecture.bedrooms}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("BATHS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                  Text("${property.architecture.bathrooms}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("SQ FT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                  Text("${property.architecture.sqFt}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("ELEVATION", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TutsvilleOnSurfaceVariant)
                  Text(property.architecture.floorLevel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // View & Outlook
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Visibility, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "View: ${property.architecture.viewType}",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 11.sp)
              )
            }

            // Parking Bays
            if (property.parkingSpots.isNotEmpty()) {
              Spacer(modifier = Modifier.height(4.dp))
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = TutsvillePrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Parking: ${property.parkingSpots.joinToString(", ")}",
                  style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 11.sp)
                )
              }
            }

            // Amenities Tags
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(property.amenities) { amenity ->
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = TutsvilleBackground,
                  border = BorderStroke(1.dp, TutsvilleOutline)
                ) {
                  Text(
                    text = amenity,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = TutsvilleOutline)
            Spacer(modifier = Modifier.height(10.dp))

            // Valuation & Management Actions (Assign to Tenant / Share Invite / Vacate)
            if (property.status == PropertyStatus.AVAILABLE) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = "MONTHLY VALUATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TutsvilleOnSurfaceVariant)
                  )
                  Text(
                    text = property.monthlyRental,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                  )
                }

                Button(
                  onClick = { viewModel.activeAssignTenantProperty.value = property },
                  shape = RoundedCornerShape(10.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                  modifier = Modifier
                    .height(38.dp)
                    .testTag("assign_tenant_button_${property.id.lowercase()}")
                ) {
                  Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Assign to Tenant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            } else {
              // Occupied: Resident Info Box + Share Invite Link + Vacate Options
              val tenant = property.assignedTenant
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = TutsvilleSecondaryContainer,
                border = BorderStroke(1.dp, TutsvilleSecondary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.AccountCircle, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(20.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = tenant?.name ?: "Resident",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvilleOnSecondaryContainer)
                      )
                    }

                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = if (tenant?.isClaimed == true) StatusCompletedBg else StatusPendingBg
                    ) {
                      Text(
                        text = if (tenant?.isClaimed == true) "Active Resident ✓" else "Invite Dispatched ⏳",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 9.sp,
                          color = if (tenant?.isClaimed == true) StatusCompletedText else StatusPendingText,
                          fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Text(
                    text = "Email: ${tenant?.email ?: ""}  •  Lease: ${tenant?.leaseStartDate ?: ""}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSecondaryContainer.copy(alpha = 0.85f))
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedButton(
                      onClick = { viewModel.activeShareInviteDialog.value = property },
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.weight(1f).height(34.dp).testTag("share_invite_button_${property.id.lowercase()}")
                    ) {
                      Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Share Invite Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                      onClick = { viewModel.vacateProperty(property.id) },
                      shape = RoundedCornerShape(8.dp),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      modifier = Modifier.height(34.dp)
                    ) {
                      Text("Vacate Unit", fontSize = 11.sp, color = Color(0xFFC62828))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // ==========================================
    // SECTION 0: ESTATE AMENITIES & PASSES
    // ==========================================
    if (selectedSection == 0) {
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
                    style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant)
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

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(
                      text = "${booking.date}  |  ${booking.timeSlot}",
                      style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary, fontWeight = FontWeight.Bold)
                    )
                    Text(
                      text = "Reserved for ${booking.residentName} (${booking.unit}) • ${booking.guestCount} Guests",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
                    )
                  }

                  IconButton(
                    onClick = { viewModel.cancelBooking(booking.id) },
                    modifier = Modifier.size(32.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Cancel reservation",
                      tint = TutsvilleOnSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Facility Listings Section
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          Text(
            text = "Estate Amenities Directory",
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
                  text = "Cap: ${facility.capacity}",
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
}
