package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.tutsville.data.model.ReminderItem
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun RemindersScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val reminders by viewModel.reminders.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()

  Box(modifier = modifier.fillMaxSize().background(TutsvilleBackground)) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 90.dp)
    ) {
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          Text(
            text = "Reminders & Notices Dispatch",
            style = MaterialTheme.typography.headlineMedium.copy(
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          Text(
            text = "Automated servicing advisories, wing broadcasts & operational alerts",
            style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
          )
        }
      }

      items(reminders, key = { it.id }) { reminder ->
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
          OperationalCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (reminder.urgency == "High Priority" || reminder.urgency == "Action Required") StatusUrgentBg else StatusPendingBg
              ) {
                Text(
                  text = reminder.urgency.uppercase(),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = if (reminder.urgency == "High Priority" || reminder.urgency == "Action Required") StatusUrgentText else StatusPendingText
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
              Text(
                text = reminder.timestamp,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = TutsvilleOnSurfaceVariant,
                  fontSize = 11.sp
                )
              )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = reminder.title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = reminder.message,
              style = MaterialTheme.typography.bodyMedium.copy(color = TutsvilleOnBackground)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (reminder.isBroadcast) Icons.Default.Campaign else Icons.Default.Group,
                contentDescription = null,
                tint = TutsvilleSecondary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Recipient: ${reminder.targetAudience}",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontWeight = FontWeight.SemiBold,
                  color = TutsvilleSecondary,
                  fontSize = 11.sp
                )
              )
            }
          }
        }
      }
    }

    // Dispatch Reminder FAB (For Managers & Staff)
    FloatingActionButton(
      onClick = { viewModel.activeSendReminderDialog.value = true },
      containerColor = TutsvillePrimary,
      contentColor = Color.White,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 20.dp, bottom = 20.dp)
        .testTag("send_reminder_fab")
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Campaign, contentDescription = "Send Reminder")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Send Reminder", fontWeight = FontWeight.Bold)
      }
    }
  }
}
