package com.example.tutsville.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.UserRole
import com.example.ui.theme.*

@Composable
fun RoleSwitcherBar(
  currentRole: UserRole,
  onRoleSelected: (UserRole) -> Unit,
  onNotificationClick: () -> Unit,
  unreadCount: Int,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(TutsvilleBackground)
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // 1. Full-Width Elevated Card Container showcasing Tutsville Estate Logo across the entire top section
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
          painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.tutsville_estate_logo_trimmed),
          contentDescription = "Tutsville Estate",
          contentScale = androidx.compose.ui.layout.ContentScale.Fit,
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

    // 3-way Role Switcher Segmented Pill Bar
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = TutsvilleCardBg,
      border = androidx.compose.foundation.BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        UserRole.values().forEach { role ->
          val isSelected = role == currentRole
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(9.dp))
              .background(if (isSelected) TutsvillePrimary else Color.Transparent)
              .clickable { onRoleSelected(role) }
              .padding(vertical = 8.dp)
              .testTag("role_tab_${role.name.lowercase()}"),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = when (role) {
                  UserRole.MANAGER -> "Manager"
                  UserRole.TENANT -> "Resident"
                  UserRole.VENDOR -> "Vendor"
                },
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color.White else TutsvilleOnSurfaceVariant
                )
              )
              Text(
                text = role.badge,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  color = if (isSelected) GoldAccent else Color(0xFF8A8F8B)
                )
              )
            }
          }
        }
      }
    }
  }
}
