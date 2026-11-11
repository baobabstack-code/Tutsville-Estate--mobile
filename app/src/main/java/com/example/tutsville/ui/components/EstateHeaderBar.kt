package com.example.tutsville.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun EstateHeaderBar(
  userEmail: String?,
  onNotificationClick: () -> Unit,
  onSignOutClick: () -> Unit,
  unreadCount: Int,
  modifier: Modifier = Modifier
) {
  var showSignOutDialog by remember { mutableStateOf(false) }

  // Sleek, compact unified luxury header bar (~52dp) replacing double stacked banners
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF101B14),
    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
    shadowElevation = 3.dp,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .testTag("app_brand_logo_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Left: Golden script Tutsville Estate logo
      Image(
        painter = painterResource(id = R.drawable.tutsville_estate_logo_trimmed),
        contentDescription = "Tutsville Estate",
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .height(30.dp)
          .widthIn(max = 160.dp)
      )

      // Right: Manager badge + Notifications bell + Sign out icon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Manager Pill with optional email tooltip
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = GoldAccent.copy(alpha = 0.18f),
          border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.6f))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = GoldAccent,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Manager",
              style = MaterialTheme.typography.labelSmall.copy(
                color = GoldAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
              )
            )
          }
        }

        // Notification Bell Icon with Badge
        IconButton(
          onClick = onNotificationClick,
          modifier = Modifier
            .size(36.dp)
            .testTag("notification_bell_button")
        ) {
          BadgedBox(
            badge = {
              if (unreadCount > 0) {
                Badge(
                  containerColor = TutsvilleSecondary,
                  contentColor = Color.White
                ) {
                  Text(text = "$unreadCount", fontSize = 9.sp)
                }
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = "Estate Alerts and Notices",
              tint = GoldAccent,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // Sign Out Icon Button
        IconButton(
          onClick = { showSignOutDialog = true },
          modifier = Modifier
            .size(34.dp)
            .testTag("sign_out_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Sign Out",
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }

  if (showSignOutDialog) {
    AlertDialog(
      onDismissRequest = { showSignOutDialog = false },
      title = {
        Text("Sign Out", fontWeight = FontWeight.Bold, color = TutsvillePrimary)
      },
      text = {
        Text("Are you sure you want to sign out of the Tutsville Estate Operations portal?")
      },
      confirmButton = {
        Button(
          onClick = {
            showSignOutDialog = false
            onSignOutClick()
          },
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary)
        ) {
          Text("Sign Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutDialog = false }) {
          Text("Cancel", color = TutsvilleOnSurfaceVariant)
        }
      }
    )
  }
}
