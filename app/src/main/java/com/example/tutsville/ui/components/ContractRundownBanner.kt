package com.example.tutsville.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.ContractItem
import com.example.tutsville.data.model.ContractUrgency
import com.example.ui.theme.*

@Composable
fun ContractRundownBanner(
  contracts: List<ContractItem>,
  onTriggerAlert: (String) -> Unit,
  onViewContract: (ContractItem) -> Unit,
  modifier: Modifier = Modifier
) {
  val expiringContracts = contracts.filter { it.urgency != ContractUrgency.HEALTHY }
  if (expiringContracts.isEmpty()) return

  val mostCritical = expiringContracts.minByOrNull { it.daysRemaining } ?: return

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    shape = RoundedCornerShape(14.dp),
    color = if (mostCritical.daysRemaining <= 14) Color(0xFFFAF0F0) else Color(0xFFFDF6EB),
    border = BorderStroke(
      1.dp,
      if (mostCritical.daysRemaining <= 14) Color(0xFFF0B3B3) else Color(0xFFF5DCB5)
    )
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (mostCritical.daysRemaining <= 14) Icons.Default.WarningAmber else Icons.Default.HourglassBottom,
          contentDescription = "Contract Countdown Alert",
          tint = if (mostCritical.daysRemaining <= 14) StatusUrgentText else StatusPendingText,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (mostCritical.daysRemaining <= 14) "URGENT CONTRACT EXPIRY" else "CONTRACT RUNNING DOWN",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.08.sp
          ),
          color = if (mostCritical.daysRemaining <= 14) StatusUrgentText else StatusPendingText
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
          text = "${mostCritical.daysRemaining} days left",
          style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = if (mostCritical.daysRemaining <= 14) StatusUrgentText else StatusPendingText
          )
        )
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = mostCritical.title,
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.SemiBold,
          color = TutsvilleOnBackground
        )
      )
      Text(
        text = "Partner: ${mostCritical.counterparty} • Expires on ${mostCritical.expiryDate}",
        style = MaterialTheme.typography.bodySmall.copy(
          color = TutsvilleOnSurfaceVariant,
          fontSize = 11.sp
        )
      )

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = { onViewContract(mostCritical) },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TutsvillePrimary),
          border = BorderStroke(1.dp, TutsvilleOutline),
          modifier = Modifier
            .height(36.dp)
            .testTag("view_expiring_contract_button")
        ) {
          Text(text = "Review Terms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = { onTriggerAlert(mostCritical.id) },
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (mostCritical.daysRemaining <= 14) StatusUrgentText else TutsvillePrimary,
            contentColor = Color.White
          ),
          modifier = Modifier
            .height(36.dp)
            .testTag("send_contract_rundown_notice_button")
        ) {
          Text(
            text = if (mostCritical.isNotificationSent) "Notice Sent ✓" else "Send Expiry Alert",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}
