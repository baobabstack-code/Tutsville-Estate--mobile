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
import com.example.tutsville.data.model.TenantProfile
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.components.TutsvilleFloatingActionButton
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun FacilitiesBookingScreen(
  viewModel: EstateViewModel,
  defaultSection: Int = 0,
  showSegmentedHeader: Boolean = false,
  modifier: Modifier = Modifier
) {
  val facilities by viewModel.facilities.collectAsState()
  val bookings by viewModel.bookings.collectAsState()
  val properties by viewModel.properties.collectAsState()
  val tenants by viewModel.tenants.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val currentTenant by viewModel.currentTenant.collectAsState()

  // 0 = Estate Amenities & Passes, 1 = Properties & Residences, 2 = Tenant Manager
  var selectedSection by remember(defaultSection) { mutableStateOf(defaultSection) }
  var propertyFilter by remember { mutableStateOf<PropertyStatus?>(null) }
  var tenantFilter by remember { mutableStateOf("ALL") }

  val filteredProperties = remember(properties, propertyFilter) {
    if (propertyFilter == null) properties else properties.filter { it.status == propertyFilter }
  }

  val filteredTenants = remember(tenants, tenantFilter) {
    when (tenantFilter) {
      "ASSIGNED" -> tenants.filter { it.assignedPropertyId != null }
      "UNASSIGNED" -> tenants.filter { it.assignedPropertyId == null }
      "APP_VERIFIED" -> tenants.filter { it.isAppRegistered }
      else -> tenants
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
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
                text = when (selectedSection) {
                  0 -> "Estate Amenities & Facilities"
                  1 -> if (currentRole == UserRole.TENANT) "My Residence & Specifications" else "Properties & Residences"
                  else -> "Tenant Manager"
                },
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
              Text(
                text = when (selectedSection) {
                  0 -> "Clubhouse passes, sports courts & private screening room"
                  1 -> if (currentRole == UserRole.TENANT) "Unit architecture, assigned parking & lease details" else "Manage residential listings & architectural specifications"
                  else -> "Create resident profiles, assign residences & send download links"
                },
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }
          }

          if (showSegmentedHeader) {
            Spacer(modifier = Modifier.height(14.dp))

            // 3-Way Segmented Switcher Control: Amenities, Properties, Tenant Manager
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
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Amenities",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedSection == 0) TutsvillePrimary else TutsvilleOnSurfaceVariant,
                        fontSize = 11.sp
                      )
                    )
                  }
                }

                // Tab 1: Properties & Residences / My Residence
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
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (currentRole == UserRole.TENANT) "My Residence" else "Properties (${properties.size})",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedSection == 1) TutsvillePrimary else TutsvilleOnSurfaceVariant,
                        fontSize = 11.sp
                      )
                    )
                  }
                }

                // Tab 2: Tenant Manager (ONLY visible to Managers)
                if (currentRole == UserRole.MANAGER) {
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedSection == 2) Color.White else Color.Transparent,
                    shadowElevation = if (selectedSection == 2) 2.dp else 0.dp,
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                      .clickable { selectedSection = 2 }
                      .testTag("tab_tenant_manager")
                  ) {
                    Row(
                      modifier = Modifier.fillMaxSize(),
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = if (selectedSection == 2) TutsvillePrimary else TutsvilleOnSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "Tenants (${tenants.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontWeight = if (selectedSection == 2) FontWeight.Bold else FontWeight.Medium,
                          color = if (selectedSection == 2) TutsvillePrimary else TutsvilleOnSurfaceVariant,
                          fontSize = 11.sp
                        )
                      )
                    }
                  }
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
      if (currentRole == UserRole.TENANT) {
        // Show Resident's Assigned Residence Showcase
        item {
          val assignedProp = properties.find { prop ->
            val unit = currentTenant?.assignedPropertyUnit
            (unit != null && prop.unitNumber.equals(unit, ignoreCase = true)) ||
            prop.assignedTenant?.email.equals(currentTenant?.email, ignoreCase = true)
          } ?: properties.firstOrNull()

          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            if (assignedProp != null) {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = TutsvilleCardBg,
                border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.7f)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth().testTag("resident_residence_specs_card")
              ) {
                Column(modifier = Modifier.padding(18.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = assignedProp.unitNumber,
                        style = MaterialTheme.typography.titleLarge.copy(
                          fontFamily = FontFamily.Serif,
                          fontWeight = FontWeight.Bold,
                          color = TutsvillePrimary
                        )
                      )
                      Text(
                        text = "${assignedProp.pavilionWing} • ${assignedProp.architecture.floorLevel}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleSecondary)
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = StatusCompletedBg,
                      border = BorderStroke(0.8.dp, StatusCompletedText.copy(alpha = 0.5f))
                    ) {
                      Text(
                        text = "ACTIVE RESIDENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                          color = StatusCompletedText,
                          fontWeight = FontWeight.Bold,
                          fontSize = 9.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(14.dp))
                  HorizontalDivider(color = TutsvilleOutline)
                  Spacer(modifier = Modifier.height(12.dp))

                  // Architecture Grid
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column {
                      Text("BEDROOMS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TutsvilleOnSurfaceVariant))
                      Text("${assignedProp.architecture.bedrooms} Suites", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
                    }
                    Column {
                      Text("BATHROOMS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TutsvilleOnSurfaceVariant))
                      Text("${assignedProp.architecture.bathrooms.toInt()} Baths", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
                    }
                    Column {
                      Text("LIVING AREA", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TutsvilleOnSurfaceVariant))
                      Text("${assignedProp.architecture.sqFt} Sq Ft", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text("VIEW", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TutsvilleOnSurfaceVariant))
                      Text(assignedProp.architecture.viewType.split("&")[0].trim(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = GoldAccent))
                    }
                  }

                  Spacer(modifier = Modifier.height(14.dp))

                  // Parking Spots
                  Text("ASSIGNED PARKING BAYS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TutsvilleOnSurfaceVariant))
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    assignedProp.parkingSpots.forEach { spot ->
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldAccent.copy(alpha = 0.12f),
                        border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.5f))
                      ) {
                        Text(
                          text = spot,
                          style = MaterialTheme.typography.labelSmall.copy(color = TutsvillePrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // Private Residence Amenities
                  Text("RESIDENCE AMENITIES", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = TutsvilleOnSurfaceVariant))
                  Spacer(modifier = Modifier.height(4.dp))
                  Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    assignedProp.amenities.forEach { am ->
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TutsvilleBackground,
                        border = BorderStroke(1.dp, TutsvilleOutline)
                      ) {
                        Text(
                          text = am,
                          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = TutsvilleOnSurfaceVariant),
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(14.dp))
                  HorizontalDivider(color = TutsvilleOutline)
                  Spacer(modifier = Modifier.height(12.dp))

                  // Action Button: Request Repair for this unit
                  Button(
                    onClick = { viewModel.activeReportCaseDialog.value = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("resident_report_issue_for_unit")
                  ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Report Maintenance for this Residence", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            } else {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = TutsvilleCardBg,
                border = BorderStroke(1.dp, TutsvilleOutline),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.HomeWork, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(36.dp))
                  Spacer(modifier = Modifier.height(8.dp))
                  Text("Residence Details Pending", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary))
                  Text("Your estate manager will assign your living unit shortly.", style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant))
                }
              }
            }
          }
        }
      } else {
        // Manager Occupancy Telemetry & Property List
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
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Estate Amenities Directory",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary,
              letterSpacing = 0.04.sp
            )
          )
          if (currentRole == UserRole.MANAGER) {
            Button(
              onClick = { viewModel.activeAddFacilityDialog.value = true },
              colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(34.dp).testTag("button_open_add_facility")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Amenity", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
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

    // ==========================================
    // SECTION 2: TENANT MANAGER DIRECTORY
    // ==========================================
    if (selectedSection == 2) {
      item {
        val assignedCount = tenants.count { it.assignedPropertyId != null }
        val registeredCount = tenants.count { it.isAppRegistered }
        val pendingCount = tenants.size - registeredCount

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          // Telemetry Row
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
                Text(text = "Total Tenants", style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant))
                Text(
                  text = "${tenants.size} Residents",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                )
                Text(text = "$assignedCount allocated", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = StatusCompletedText))
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = TutsvilleCardBg,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "App Onboarding", style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant))
                Text(
                  text = "$registeredCount Registered",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TutsvilleSecondary)
                )
                Text(text = "$pendingCount pending invite", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = StatusPendingText))
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Filter Chips
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = tenantFilter == "ALL",
              onClick = { tenantFilter = "ALL" },
              label = { Text("All Tenants (${tenants.size})") },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White),
              shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
              selected = tenantFilter == "ASSIGNED",
              onClick = { tenantFilter = "ASSIGNED" },
              label = { Text("Assigned ($assignedCount)") },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White),
              shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
              selected = tenantFilter == "UNASSIGNED",
              onClick = { tenantFilter = "UNASSIGNED" },
              label = { Text("Unassigned (${tenants.size - assignedCount})") },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White),
              shape = RoundedCornerShape(8.dp)
            )
            FilterChip(
              selected = tenantFilter == "APP_VERIFIED",
              onClick = { tenantFilter = "APP_VERIFIED" },
              label = { Text("App Registered ($registeredCount)") },
              colors = FilterChipDefaults.filterChipColors(selectedContainerColor = TutsvillePrimary, selectedLabelColor = Color.White),
              shape = RoundedCornerShape(8.dp)
            )
          }
        }
      }

      if (filteredTenants.isEmpty()) {
        item {
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color.White,
              border = BorderStroke(1.dp, TutsvilleOutline),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = TutsvilleOnSurfaceVariant, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No resident tenant profiles found", fontWeight = FontWeight.Bold, color = TutsvillePrimary)
                Text("Tap '+ Add Tenant' below to create a resident profile.", style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant))
              }
            }
          }
        }
      } else {
        items(filteredTenants, key = { it.id }) { tenant ->
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            OperationalCard {
              // Top: Tenant Avatar, Name, and App Registration Badge
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                  Surface(
                    shape = CircleShape,
                    color = TutsvillePrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, TutsvillePrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.size(42.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text(
                        text = tenant.fullName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                        style = MaterialTheme.typography.titleSmall.copy(
                          fontWeight = FontWeight.Bold,
                          color = TutsvillePrimary
                        )
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = tenant.fullName,
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TutsvillePrimary
                      )
                    )
                    Text(
                      text = "${tenant.email} • ${tenant.phone}",
                      style = MaterialTheme.typography.bodySmall.copy(
                        color = TutsvilleOnSurfaceVariant,
                        fontSize = 11.sp
                      )
                    )
                  }
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = if (tenant.isAppRegistered) StatusCompletedBg else StatusPendingBg,
                  border = BorderStroke(0.8.dp, if (tenant.isAppRegistered) Color(0xFFC4ECD4) else Color(0xFFF2DCAE))
                ) {
                  Text(
                    text = if (tenant.isAppRegistered) "App Verified ✓" else "Invite Sent ⏳",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 9.sp,
                      color = if (tenant.isAppRegistered) StatusCompletedText else StatusPendingText
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Emergency Contact Pill
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFAF7F2),
                border = BorderStroke(1.dp, TutsvilleOutline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.ContactEmergency,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Emergency: ${tenant.emergencyContactName} (${tenant.emergencyContactPhone})",
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontSize = 11.sp,
                      color = TutsvilleOnSurfaceVariant
                    )
                  )
                }
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Residence Assignment Banner
              if (tenant.assignedPropertyId != null) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = TutsvilleSecondaryContainer,
                  border = BorderStroke(1.dp, TutsvilleSecondary.copy(alpha = 0.5f)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Home, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = tenant.assignedPropertyUnit ?: "Assigned Unit",
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontWeight = FontWeight.Bold,
                          color = TutsvilleOnSecondaryContainer,
                          fontSize = 11.sp
                        )
                      )
                    }

                    TextButton(
                      onClick = { viewModel.unassignTenant(tenant.id) },
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                      modifier = Modifier.height(28.dp)
                    ) {
                      Text("Unassign", fontSize = 10.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                    }
                  }
                }
              } else {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = StatusUrgentBg,
                  border = BorderStroke(1.dp, Color(0xFFF0B3B3)),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(Icons.Default.Warning, contentDescription = null, tint = StatusUrgentText, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "No property allocated",
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontWeight = FontWeight.Bold,
                          color = StatusUrgentText,
                          fontSize = 11.sp
                        )
                      )
                    }

                    TextButton(
                      onClick = { viewModel.activeAssignPropertyToTenant.value = tenant },
                      contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                      modifier = Modifier.height(28.dp).testTag("assign_property_btn_${tenant.id.lowercase()}")
                    ) {
                      Text("Assign Unit →", fontSize = 10.sp, color = TutsvillePrimary, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Actions: Share Download & Invite Link
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { viewModel.activeTenantInviteSheet.value = tenant },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .testTag("share_tenant_link_${tenant.id.lowercase()}")
                ) {
                  Icon(Icons.Default.SendToMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Share Invite & App Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (tenant.assignedPropertyId == null) {
                  Button(
                    onClick = { viewModel.activeAssignPropertyToTenant.value = tenant },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                  ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assign", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Consistent Bottom-Right Tutsville Extended FAB across tabs
  TutsvilleFloatingActionButton(
    text = when (selectedSection) {
      0 -> "Reserve Amenity"
      1 -> "List Property"
      else -> "Add Tenant"
    },
    icon = when (selectedSection) {
      0 -> Icons.Default.Add
      1 -> Icons.Default.AddHome
      else -> Icons.Default.PersonAdd
    },
    onClick = {
      when (selectedSection) {
        0 -> {
          val firstFac = facilities.firstOrNull()
          viewModel.activeBookFacility.value = firstFac
        }
        1 -> viewModel.activeListPropertyDialog.value = true
        else -> viewModel.activeCreateTenantDialog.value = true
      }
    },
    modifier = Modifier
      .align(Alignment.BottomEnd)
      .padding(end = 20.dp, bottom = 20.dp),
    testTag = when (selectedSection) {
      0 -> "fab_reserve_amenity"
      1 -> "fab_list_property"
      else -> "fab_add_tenant"
    }
  )
}
}
