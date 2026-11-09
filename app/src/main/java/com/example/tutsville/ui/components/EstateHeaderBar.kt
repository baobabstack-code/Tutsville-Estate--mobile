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

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(TutsvilleBackground)
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // 1. Full-Width Elevated Luxury Card Container showcasing Tutsville Estate Logo
    ElevatedCard(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.elevatedCardColors(
        containerColor = Color(0xFF000000)
      ),
      elevation = CardDefaults.elevatedCardElevation(
        defaultElevation = 5.dp,
        pressedElevation = 8.dp
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(68.dp)
        .border(1.dp, GoldAccent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
        .testTag("app_brand_logo_card")
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        // Centered Golden Script Logo
        Image(
          painter = painterResource(id = R.drawable.tutsville_estate_logo_trimmed),
          contentDescription = "Tutsville Estate",
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.75f)
            .align(Alignment.Center)
        )

        // Notification Bell Icon in top-right corner
        IconButton(
          onClick = onNotificationClick,
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .size(42.dp)
            .testTag("notification_bell_button")
        ) {
          BadgedBox(
            badge = {
              if (unreadCount > 0) {
                Badge(
                  containerColor = TutsvilleSecondary,
                  contentColor = Color.White
                ) {
                  Text(text = "$unreadCount", fontSize = 10.sp)
                }
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = "Estate Alerts and Notices",
              tint = GoldAccent,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Authenticated User Profile Chip Bar (Replaces manual role switcher)
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("user_profile_header_bar")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Left: Avatar / Account Icon and User Email
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(TutsvillePrimary.copy(alpha = 0.12f))
              .border(1.dp, TutsvillePrimary.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AccountCircle,
              contentDescription = null,
              tint = TutsvillePrimary,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.padding(end = 8.dp)) {
            Text(
              text = userEmail ?: "Authenticated User",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TutsvilleOnSurface
              ),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Google Identity Verified",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = TutsvilleOnSurfaceVariant
              )
            )
          }
        }

        // Right: Estate Manager Role Badge & Sign Out Button
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Gold Role Badge
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = GoldAccent.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Estate Manager",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = GoldAccent,
                  fontWeight = FontWeight.Bold,
                  fontSize = 11.sp
                )
              )
            }
          }

          // Sign Out Action Button
          IconButton(
            onClick = { showSignOutDialog = true },
            modifier = Modifier
              .size(36.dp)
              .testTag("sign_out_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Sign Out",
              tint = TutsvilleOnSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
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
