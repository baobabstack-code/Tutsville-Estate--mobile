package com.example.tutsville.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.UserRole
import com.example.ui.theme.*

data class DrawerItemData(
  val id: Int,
  val title: String,
  val subtitle: String,
  val icon: ImageVector
)

val managerDrawerItems = listOf(
  DrawerItemData(1, "Tenants", "Directory, IDs & App Status", Icons.Default.People),
  DrawerItemData(2, "Units", "Property inventory & size specs", Icons.Default.HomeWork),
  DrawerItemData(3, "Amenities", "Facilities catalog & pass bookings", Icons.Default.Apartment),
  DrawerItemData(4, "Cases", "Work orders & vendor dispatch", Icons.Default.Assignment),
  DrawerItemData(5, "Contracts", "Lease SLAs & vendor agreements", Icons.Default.Description),
  DrawerItemData(6, "Vendors", "Approved contractors & phone contacts", Icons.Default.Engineering)
)

/**
 * Clean Top App Bar: Menu Icon, Estate Brand Title, and Notification Bell
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstateHeaderBar(
  currentRole: UserRole,
  unreadCount: Int,
  onOpenDrawer: () -> Unit,
  onOpenNotifications: () -> Unit
) {
  Surface(
    color = TutsvillePrimary,
    tonalElevation = 4.dp,
    shadowElevation = 2.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .height(60.dp)
        .padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Drawer Menu Toggle Button
        IconButton(
          onClick = onOpenDrawer,
          modifier = Modifier.testTag("btn_open_sidebar_drawer")
        ) {
          Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Open Navigation Drawer",
            tint = Color.White
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Estate Crest & Title
        Column {
          Text(
            text = "TUTSVILLE ESTATE",
            style = MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              letterSpacing = 1.sp
            )
          )
          Text(
            text = if (currentRole == UserRole.MANAGER) "Estate Manager Portal" else "Resident Concierge",
            style = MaterialTheme.typography.labelSmall.copy(
              color = GoldAccent,
              fontSize = 10.sp
            )
          )
        }
      }

      // Notification Bell with Badge Counter
      IconButton(
        onClick = onOpenNotifications,
        modifier = Modifier.testTag("btn_top_bar_notifications")
      ) {
        BadgedBox(
          badge = {
            if (unreadCount > 0) {
              Badge(
                containerColor = StatusUrgentBg,
                contentColor = StatusUrgentText
              ) {
                Text("$unreadCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Estate Advisories & Alerts",
            tint = Color.White
          )
        }
      }
    }
  }
}

/**
 * Side Navigation Drawer Content: Header, Items 1-6, and Footer Auth / Profile Card
 */
@Composable
fun SidebarDrawerContent(
  currentRole: UserRole,
  userEmail: String?,
  userName: String,
  selectedDrawerId: Int?,
  onSelectDrawerItem: (Int) -> Unit,
  onSignOut: () -> Unit
) {
  val scrollState = rememberScrollState()

  ModalDrawerSheet(
    drawerContainerColor = TutsvilleBackground,
    modifier = Modifier.width(310.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
    ) {
      // 1. Sidebar Header (Tutsville Crest)
      Surface(
        color = TutsvillePrimary,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(20.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = GoldAccent.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, GoldAccent),
            modifier = Modifier.size(48.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.Castle, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = "Tutsville Management",
            style = MaterialTheme.typography.titleMedium.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          )
          Text(
            text = "Luxury Residential Command Center",
            style = MaterialTheme.typography.bodySmall.copy(
              color = TutsvilleSecondaryContainer,
              fontSize = 11.sp
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 2. Navigation Items 1-6
      Text(
        text = "MANAGEMENT MODULES",
        style = MaterialTheme.typography.labelSmall.copy(
          color = TutsvilleOnSurfaceVariant,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.8.sp
        ),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
      )

      managerDrawerItems.forEach { item ->
        val isSelected = selectedDrawerId == item.id
        NavigationDrawerItem(
          icon = {
            Icon(
              imageVector = item.icon,
              contentDescription = null,
              tint = if (isSelected) TutsvillePrimary else TutsvilleSecondary
            )
          },
          label = {
            Column {
              Text(
                text = "${item.id}. ${item.title}",
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isSelected) TutsvillePrimary else TutsvilleOnBackground
              )
              Text(
                text = item.subtitle,
                fontSize = 10.sp,
                color = TutsvilleOnSurfaceVariant
              )
            }
          },
          selected = isSelected,
          onClick = { onSelectDrawerItem(item.id) },
          colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color(0xFFC4ECD4).copy(alpha = 0.6f),
            unselectedContainerColor = Color.Transparent
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .testTag("drawer_item_${item.title.lowercase()}")
        )
      }

      Spacer(modifier = Modifier.weight(1f))
      HorizontalDivider(color = TutsvilleOutline, modifier = Modifier.padding(vertical = 12.dp))

      // 3. User Profile & Settings Auth Footer Card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = TutsvilleCardBg,
        border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .testTag("drawer_user_profile_footer")
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "ACTIVE USER PROFILE",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              color = GoldAccent,
              fontWeight = FontWeight.Bold
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Surface(
              shape = CircleShape,
              color = TutsvillePrimary.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, TutsvillePrimary),
              modifier = Modifier.size(42.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""),
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TutsvillePrimary
                  )
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = userName,
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                ),
                maxLines = 1
              )
              Text(
                text = userEmail ?: "baobabstack@gmail.com",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  color = TutsvilleOnSurfaceVariant
                ),
                maxLines = 1
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Role Badge Pill
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = StatusCompletedBg,
            border = BorderStroke(0.8.dp, StatusCompletedText.copy(alpha = 0.5f))
          ) {
            Text(
              text = if (currentRole == UserRole.MANAGER) "Role: Property Manager" else "Role: Verified Resident",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = StatusCompletedText
              ),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Sign Out Action Button
          Button(
            onClick = onSignOut,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(38.dp)
              .testTag("btn_sidebar_sign_out")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ExitToApp,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sign Out / Switch Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }

      Spacer(modifier = Modifier.navigationBarsPadding())
    }
  }
}
