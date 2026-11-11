package com.example.tutsville.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tutsville.data.model.PropertyArchitecture
import com.example.tutsville.data.model.PropertyItem
import com.example.tutsville.data.model.PropertyStatus
import com.example.tutsville.data.model.TenantProfile
import com.example.ui.theme.*

@Composable
fun ListPropertyDialog(
  onDismiss: () -> Unit,
  onSubmit: (
    unitNumber: String,
    pavilionWing: String,
    architecture: PropertyArchitecture,
    parkingSpots: List<String>,
    amenities: List<String>,
    monthlyRental: String
  ) -> Unit
) {
  var unitNumber by remember { mutableStateOf("") }
  var pavilionWing by remember { mutableStateOf("North Tower") }
  var bedrooms by remember { mutableStateOf("3") }
  var bathrooms by remember { mutableStateOf("3.5") }
  var sqFt by remember { mutableStateOf("3200") }
  var floorLevel by remember { mutableStateOf("Level 12") }
  var viewType by remember { mutableStateOf("Panoramic Mountain & Forest Grounds") }
  var parkingBay by remember { mutableStateOf("Bay N-15") }
  var hasEvCharging by remember { mutableStateOf(true) }
  var monthlyRental by remember { mutableStateOf("$10,500 / mo") }
  var validationError by remember { mutableStateOf<String?>(null) }

  val defaultAmenities = listOf(
    "Private Plunge Pool",
    "Sommelier Wine Cellar",
    "Smart Bang & Olufsen Automation",
    "Calacatta Marble Spa Bath",
    "Wraparound Cedar Terrace",
    "Keyed Private Elevator",
    "Chef's Wolf & Sub-Zero Kitchen"
  )
  val selectedAmenities = remember { mutableStateListOf("Private Plunge Pool", "Smart Bang & Olufsen Automation") }

  val wings = listOf(
    "North Tower",
    "West Pavilion",
    "South Courtyard",
    "Tower B • Sky Terraces",
    "East Garden Villas"
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f)),
      shadowElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("list_property_dialog_surface")
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
              text = "List New Residence",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Add luxury estate villa or penthouse specifications",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Unit Identity
        OutlinedTextField(
          value = unitNumber,
          onValueChange = { unitNumber = it },
          label = { Text("Unit Number & Title") },
          placeholder = { Text("e.g. Villa 502 • Rosewood Suite") },
          leadingIcon = { Icon(Icons.Default.Apartment, contentDescription = null, tint = TutsvillePrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("prop_unit_number_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Pavilion / Wing",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          wings.forEach { wing ->
            FilterChip(
              selected = pavilionWing == wing,
              onClick = { pavilionWing = wing },
              label = { Text(wing) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TutsvillePrimary,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Architectural Details Grid
        Text(
          text = "Architectural Specifications",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = bedrooms,
            onValueChange = { bedrooms = it },
            label = { Text("Bedrooms") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("prop_beds_input")
          )
          OutlinedTextField(
            value = bathrooms,
            onValueChange = { bathrooms = it },
            label = { Text("Bathrooms") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("prop_baths_input")
          )
          OutlinedTextField(
            value = sqFt,
            onValueChange = { sqFt = it },
            label = { Text("Sq Ft") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("prop_sqft_input")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = floorLevel,
          onValueChange = { floorLevel = it },
          label = { Text("Floor / Elevation") },
          placeholder = { Text("e.g. Level 18 or Ground & Terrace") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("prop_floor_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = viewType,
          onValueChange = { viewType = it },
          label = { Text("View & Outlook") },
          placeholder = { Text("e.g. 360° Mountain & Forest Lawn") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("prop_view_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Parking Spots & EV
        Text(
          text = "Designated Parking Bay",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = parkingBay,
          onValueChange = { parkingBay = it },
          label = { Text("Bay Number(s)") },
          placeholder = { Text("e.g. Bay N-15, Bay N-16") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("prop_parking_input")
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { hasEvCharging = !hasEvCharging }
            .padding(vertical = 4.dp)
        ) {
          Checkbox(
            checked = hasEvCharging,
            onCheckedChange = { hasEvCharging = it },
            colors = CheckboxDefaults.colors(checkedColor = TutsvillePrimary)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Equipped with dedicated Level 2 EV Fast Charging",
            style = MaterialTheme.typography.bodySmall.copy(color = TutsvillePrimary, fontWeight = FontWeight.Medium)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Luxury Amenities Selection
        Text(
          text = "Luxury Amenities & Features",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          defaultAmenities.forEach { amenity ->
            val isSelected = selectedAmenities.contains(amenity)
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) TutsvilleSecondaryContainer else Color(0xFFF7F5F0),
              border = BorderStroke(1.dp, if (isSelected) TutsvilleSecondary else TutsvilleOutline),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (isSelected) selectedAmenities.remove(amenity) else selectedAmenities.add(amenity)
                }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                  contentDescription = null,
                  tint = if (isSelected) TutsvilleSecondary else TutsvilleOnSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = amenity,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) TutsvilleOnSecondaryContainer else TutsvilleOnSurface
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Monthly Valuation
        OutlinedTextField(
          value = monthlyRental,
          onValueChange = { monthlyRental = it },
          label = { Text("Monthly Lease Valuation") },
          placeholder = { Text("e.g. $12,500 / mo") },
          leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = TutsvilleSecondary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("prop_rent_input")
        )

        if (validationError != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = validationError ?: "", color = Color(0xFFC62828), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            if (unitNumber.isBlank()) {
              validationError = "Please enter the unit title or number."
              return@Button
            }
            val parsedBeds = bedrooms.toIntOrNull() ?: 3
            val parsedBaths = bathrooms.toFloatOrNull() ?: 3.0f
            val parsedSqFt = sqFt.toIntOrNull() ?: 2500

            val arch = PropertyArchitecture(
              bedrooms = parsedBeds,
              bathrooms = parsedBaths,
              sqFt = parsedSqFt,
              floorLevel = floorLevel.ifBlank { "Level 1" },
              viewType = viewType.ifBlank { "Estate Grounds" }
            )

            val fullParking = mutableListOf<String>()
            if (parkingBay.isNotBlank()) {
              val evTag = if (hasEvCharging) " (EV Charging)" else ""
              fullParking.add("${parkingBay.trim()}$evTag")
            } else {
              fullParking.add("Designated Garage Space")
            }

            onSubmit(
              unitNumber,
              pavilionWing,
              arch,
              fullParking,
              selectedAmenities.toList(),
              monthlyRental.ifBlank { "$10,000 / mo" }
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_list_property_button")
        ) {
          Icon(Icons.Default.AddHome, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Publish Property Listing", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun AssignTenantDialog(
  property: PropertyItem,
  onDismiss: () -> Unit,
  onAssign: (name: String, email: String, start: String, end: String) -> Unit
) {
  var tenantName by remember { mutableStateOf("") }
  var tenantEmail by remember { mutableStateOf("") }
  var leaseStartDate by remember { mutableStateOf("Dec 01, 2026") }
  var leaseDurationMonths by remember { mutableStateOf("12") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.7f)),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("assign_tenant_dialog_surface")
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
              text = "Assign Resident Tenant",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Issue digital estate keys & onboarding invite",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Property Preview Banner
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = TutsvilleSecondaryContainer,
          border = BorderStroke(1.dp, TutsvilleSecondary.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.VpnKey, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = property.unitNumber,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvilleOnSecondaryContainer)
              )
              Text(
                text = "${property.pavilionWing} • ${property.architecture.bedrooms} Beds • ${property.monthlyRental}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSecondaryContainer.copy(alpha = 0.85f))
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tenant Full Name
        OutlinedTextField(
          value = tenantName,
          onValueChange = { tenantName = it },
          label = { Text("Tenant Full Name") },
          placeholder = { Text("e.g. Sir Julian Sterling") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TutsvillePrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("assign_tenant_name_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tenant Email Address
        OutlinedTextField(
          value = tenantEmail,
          onValueChange = { tenantEmail = it },
          label = { Text("Tenant Email Address") },
          placeholder = { Text("e.g. resident@tutsville.estate") },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TutsvillePrimary) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("assign_tenant_email_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Lease Start Date
        OutlinedTextField(
          value = leaseStartDate,
          onValueChange = { leaseStartDate = it },
          label = { Text("Lease Start Date") },
          leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TutsvillePrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("assign_tenant_start_date_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Lease Duration
        Text(
          text = "Lease Duration",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("6 Months", "12 Months", "24 Months").forEach { dur ->
            val num = dur.split(" ")[0]
            FilterChip(
              selected = leaseDurationMonths == num,
              onClick = { leaseDurationMonths = num },
              label = { Text(dur) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = TutsvillePrimary,
                selectedLabelColor = Color.White
              )
            )
          }
        }

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = errorMessage ?: "", color = Color(0xFFC62828), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            if (tenantName.isBlank() || tenantEmail.isBlank()) {
              errorMessage = "Please enter both tenant name and email address."
              return@Button
            }
            if (!tenantEmail.contains("@")) {
              errorMessage = "Please provide a valid email address."
              return@Button
            }
            val leaseEnd = "Nov 30, 2027"
            onAssign(tenantName, tenantEmail, leaseStartDate, leaseEnd)
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("confirm_assign_tenant_button")
        ) {
          Icon(Icons.Default.Send, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Assign & Generate Invite Link", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun ShareTenantInviteDialog(
  property: PropertyItem,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val tenant = property.assignedTenant
  val inviteLink = tenant?.inviteLink ?: "tutsville://onboard?propertyId=${property.id}&email=${tenant?.email}"
  val inviteCode = tenant?.inviteCode ?: "TUTS-${property.id.replace("PROP-", "")}"

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.5.dp, GoldAccent),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("share_invite_dialog_surface")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Surface(
          shape = CircleShape,
          color = StatusCompletedBg,
          modifier = Modifier.size(54.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = StatusCompletedText, modifier = Modifier.size(30.dp))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Tenant Onboarding Link Ready",
          style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        )

        Text(
          text = "Assigned to ${tenant?.name ?: "Resident"} for ${property.unitNumber}",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Digital Invite Pass Card
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFFF9F7F2),
          border = BorderStroke(1.dp, TutsvilleOutline),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "ASSIGNED EMAIL",
                style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
              )
              Text(
                text = "INVITE CODE",
                style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = tenant?.email ?: "",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              )
              Text(
                text = inviteCode,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = GoldAccent)
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = TutsvilleOutline)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "ONBOARDING LINK",
              style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
            )
            Text(
              text = inviteLink,
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = TutsvillePrimary,
                fontFamily = FontFamily.Monospace
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Copy Link & Native Android Share Sheet
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Copy to Clipboard
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Tutsville Tenant Onboarding Link", inviteLink)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(46.dp).testTag("copy_invite_link_button")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Link")
          }

          // Share via Native Share Sheet
          Button(
            onClick = {
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                  Intent.EXTRA_SUBJECT,
                  "Welcome to Tutsville Estate • Your Resident Onboarding Pass"
                )
                putExtra(
                  Intent.EXTRA_TEXT,
                  """
                  Welcome to Tutsville Estate!
                  
                  You have been registered as the resident for ${property.unitNumber} (${property.pavilionWing}).
                  
                  • Resident: ${tenant?.name}
                  • Assigned Email: ${tenant?.email}
                  • Lease Start Date: ${tenant?.leaseStartDate}
                  • Invite Code: $inviteCode
                  
                  To claim your keys and activate your resident access portal, open the Tutsville Estate app using this link:
                  $inviteLink
                  """.trimIndent()
                )
              }
              context.startActivity(Intent.createChooser(shareIntent, "Send Onboarding Pass to Resident"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(46.dp).testTag("share_invite_via_apps_button")
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Link")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = onDismiss) {
          Text("Done", color = TutsvilleOnSurfaceVariant)
        }
      }
    }
  }
}

@Composable
fun CreateTenantDialog(
  onDismiss: () -> Unit,
  onSubmit: (
    fullName: String,
    email: String,
    phone: String,
    emergencyContactName: String,
    emergencyContactPhone: String
  ) -> Unit
) {
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var emergencyName by remember { mutableStateOf("") }
  var emergencyPhone by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.7f)),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("create_tenant_dialog_surface")
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
              text = "Add New Tenant",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Register resident details and generate digital invite",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Full Name
        OutlinedTextField(
          value = fullName,
          onValueChange = { fullName = it },
          label = { Text("Full Name *") },
          placeholder = { Text("e.g. Lady Vivienne Montgomery") },
          leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TutsvillePrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("tenant_fullname_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Email Address
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address *") },
          placeholder = { Text("e.g. resident@tutsville.estate") },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TutsvillePrimary) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("tenant_email_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Phone Number
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Mobile Phone *") },
          placeholder = { Text("+1 (555) 329-8812") },
          leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TutsvillePrimary) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("tenant_phone_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Emergency Contact Section
        Text(
          text = "Emergency Contact Information",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = emergencyName,
          onValueChange = { emergencyName = it },
          label = { Text("Contact Name *") },
          placeholder = { Text("e.g. Robert Montgomery (Brother)") },
          leadingIcon = { Icon(Icons.Default.ContactEmergency, contentDescription = null, tint = GoldAccent) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("tenant_emergency_name_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = emergencyPhone,
          onValueChange = { emergencyPhone = it },
          label = { Text("Emergency Phone *") },
          placeholder = { Text("+1 (555) 914-4321") },
          leadingIcon = { Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = GoldAccent) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("tenant_emergency_phone_input")
        )

        if (validationError != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = validationError ?: "", color = Color(0xFFC62828), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            if (fullName.isBlank() || email.isBlank() || phone.isBlank()) {
              validationError = "Please enter full name, email, and phone number."
              return@Button
            }
            if (!email.contains("@")) {
              validationError = "Please enter a valid email address."
              return@Button
            }
            if (emergencyName.isBlank() || emergencyPhone.isBlank()) {
              validationError = "Please enter an emergency contact name and phone number."
              return@Button
            }
            onSubmit(
              fullName.trim(),
              email.trim(),
              phone.trim(),
              emergencyName.trim(),
              emergencyPhone.trim()
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_create_tenant_button")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Create Tenant & Issue Invite", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun AssignPropertyToTenantDialog(
  tenant: TenantProfile,
  availableProperties: List<PropertyItem>,
  onDismiss: () -> Unit,
  onAssign: (propertyId: String, leaseStart: String, leaseEnd: String) -> Unit
) {
  var selectedPropertyId by remember {
    mutableStateOf(availableProperties.firstOrNull()?.id ?: "")
  }
  var leaseStartDate by remember { mutableStateOf("Dec 01, 2026") }
  var leaseEndDate by remember { mutableStateOf("Nov 30, 2027") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.7f)),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("assign_property_to_tenant_dialog_surface")
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
              text = "Assign Property",
              style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
            Text(
              text = "Allocate residence to ${tenant.fullName}",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TutsvilleOnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tenant Summary Card
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = TutsvilleSecondaryContainer,
          border = BorderStroke(1.dp, TutsvilleSecondary.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = TutsvilleSecondary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = tenant.fullName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvilleOnSecondaryContainer)
              )
              Text(
                text = "${tenant.email} • ${tenant.phone}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSecondaryContainer.copy(alpha = 0.85f))
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Select Available Property",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))

        if (availableProperties.isEmpty()) {
          Text(
            text = "No vacant properties available. Please list a new property first.",
            color = Color(0xFFC62828),
            style = MaterialTheme.typography.bodySmall
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            availableProperties.forEach { prop ->
              val isSelected = selectedPropertyId == prop.id
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) TutsvilleSecondaryContainer else Color(0xFFF7F5F0),
                border = BorderStroke(1.dp, if (isSelected) TutsvilleSecondary else TutsvilleOutline),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selectedPropertyId = prop.id }
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(
                      imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                      contentDescription = null,
                      tint = if (isSelected) TutsvilleSecondary else TutsvilleOnSurfaceVariant,
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = prop.unitNumber,
                        style = MaterialTheme.typography.bodyMedium.copy(
                          fontWeight = FontWeight.Bold,
                          color = TutsvillePrimary
                        )
                      )
                      Text(
                        text = "${prop.pavilionWing} • ${prop.architecture.bedrooms} Bed • ${prop.monthlyRental}",
                        style = MaterialTheme.typography.bodySmall.copy(
                          fontSize = 11.sp,
                          color = TutsvilleOnSurfaceVariant
                        )
                      )
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lease Start Date
        OutlinedTextField(
          value = leaseStartDate,
          onValueChange = { leaseStartDate = it },
          label = { Text("Lease Start Date") },
          leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TutsvillePrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("assign_prop_lease_start")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Lease End Date
        OutlinedTextField(
          value = leaseEndDate,
          onValueChange = { leaseEndDate = it },
          label = { Text("Lease End Date") },
          leadingIcon = { Icon(Icons.Default.Event, contentDescription = null, tint = TutsvillePrimary) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("assign_prop_lease_end")
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = errorMessage ?: "", color = Color(0xFFC62828), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            if (selectedPropertyId.isBlank()) {
              errorMessage = "Please select a property to assign."
              return@Button
            }
            onAssign(selectedPropertyId, leaseStartDate, leaseEndDate)
          },
          enabled = selectedPropertyId.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("confirm_assign_property_to_tenant_button")
        ) {
          Icon(Icons.Default.Key, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Confirm Property Assignment", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun TenantInviteDialog(
  tenant: TenantProfile,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val playStoreLink = "https://play.google.com/store/apps/details?id=com.aistudio.tutsville.kxpm"
  val unitLabel = tenant.assignedPropertyUnit ?: "Pending Residence Assignment"

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.5.dp, GoldAccent),
      shadowElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("tenant_invite_sheet_dialog_surface")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Surface(
          shape = CircleShape,
          color = StatusCompletedBg,
          modifier = Modifier.size(54.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SendToMobile, contentDescription = null, tint = StatusCompletedText, modifier = Modifier.size(30.dp))
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Tenant Onboarding Invite",
          style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        )

        Text(
          text = "Send app download link & invite code to ${tenant.fullName}",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Digital Invite Pass Card
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFFF9F7F2),
          border = BorderStroke(1.dp, TutsvilleOutline),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "ASSIGNED RESIDENCE",
                style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
              )
              Text(
                text = "INVITE CODE",
                style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = unitLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              )
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = GoldAccent.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, GoldAccent)
              ) {
                Text(
                  text = tenant.inviteCode,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = GoldAccent),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = TutsvilleOutline)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "REGISTERED EMAIL",
              style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
            )
            Text(
              text = tenant.email,
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TutsvillePrimary)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = "EMERGENCY CONTACT",
              style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
            )
            Text(
              text = "${tenant.emergencyContactName} (${tenant.emergencyContactPhone})",
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = TutsvilleOutline)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "GOOGLE PLAY STORE DOWNLOAD LINK",
              style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
            )
            Text(
              text = playStoreLink,
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = TutsvilleSecondary,
                fontFamily = FontFamily.Monospace
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons: Copy Download Link & Native Android Share Sheet
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Copy to Clipboard
          OutlinedButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val copyText = "Welcome to Tutsville Private Estate!\nDownload the app on Google Play: $playStoreLink\nResident: ${tenant.fullName}\nAssigned Email: ${tenant.email}\nResidence: $unitLabel\n\nSign in with Google using your assigned email (${tenant.email}) to immediately activate your digital access keys and concierge pass."
              val clip = ClipData.newPlainText("Tutsville Tenant Invite & App Link", copyText)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Download link and invite instructions copied!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(46.dp).testTag("copy_tenant_invite_link_button")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copy Link")
          }

          // Share via Native Share Sheet
          Button(
            onClick = {
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                  Intent.EXTRA_SUBJECT,
                  "Welcome to Tutsville Private Estate • Your Resident Onboarding Pass"
                )
                putExtra(
                  Intent.EXTRA_TEXT,
                  """
                  Welcome to Tutsville Private Estate!

                  You have been registered as our resident for ${unitLabel}.

                  • Resident: ${tenant.fullName}
                  • Assigned Email: ${tenant.email}

                  Step 1: Download the Tutsville Estate App on Google Play:
                  $playStoreLink

                  Step 2: Sign in with your registered Google account (${tenant.email}) to immediately activate your smart access keys, concierge, and valet pass.
                  """.trimIndent()
                )
              }
              context.startActivity(Intent.createChooser(shareIntent, "Send Resident Invite Pass"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(46.dp).testTag("share_tenant_invite_button")
          ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Pass")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = onDismiss) {
          Text("Done", color = TutsvilleOnSurfaceVariant)
        }
      }
    }
  }
}
