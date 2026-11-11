package com.example.tutsville.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.CasePriority
import com.example.tutsville.data.model.CaseStatus
import com.example.tutsville.data.model.ContractUrgency
import com.example.tutsville.data.model.MaintenanceStatus
import com.example.ui.theme.*

@Composable
fun OperationalCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  border: BorderStroke = BorderStroke(1.dp, TutsvilleOutline),
  backgroundColor: Color = TutsvilleCardBg,
  cornerRadius: Dp = 16.dp,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(cornerRadius)
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    shape = shape,
    color = backgroundColor,
    tonalElevation = 1.dp,
    shadowElevation = 1.dp,
    border = border
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      content = content
    )
  }
}

@Composable
fun StatusPill(
  text: String,
  bgColor: Color,
  textColor: Color,
  dotColor: Color? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(9999.dp),
    color = bgColor
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (dotColor != null) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.08.sp
        ),
        color = textColor
      )
    }
  }
}

@Composable
fun CaseStatusPill(status: CaseStatus, modifier: Modifier = Modifier) {
  when (status) {
    CaseStatus.OPEN -> StatusPill(
      text = "Reported",
      bgColor = StatusPendingBg,
      textColor = StatusPendingText,
      dotColor = StatusPendingText,
      modifier = modifier
    )
    CaseStatus.ASSIGNED -> StatusPill(
      text = "Assigned",
      bgColor = TutsvilleSecondaryContainer,
      textColor = TutsvilleOnSecondaryContainer,
      dotColor = TutsvilleSecondary,
      modifier = modifier
    )
    CaseStatus.IN_PROGRESS -> StatusPill(
      text = "In Progress",
      bgColor = Color(0xFFE8F1FC),
      textColor = Color(0xFF134E96),
      dotColor = Color(0xFF134E96),
      modifier = modifier
    )
    CaseStatus.RESOLVED -> StatusPill(
      text = "Resolved",
      bgColor = StatusCompletedBg,
      textColor = StatusCompletedText,
      dotColor = StatusCompletedText,
      modifier = modifier
    )
  }
}

@Composable
fun PriorityBadge(priority: CasePriority, modifier: Modifier = Modifier) {
  val (bgColor, textColor) = when (priority) {
    CasePriority.LOW -> StatusNeutralBg to StatusNeutralText
    CasePriority.MEDIUM -> Color(0xFFF6F3EB) to TutsvillePrimary
    CasePriority.HIGH -> StatusPendingBg to StatusPendingText
    CasePriority.EMERGENCY -> StatusUrgentBg to StatusUrgentText
  }
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = bgColor
  ) {
    Text(
      text = priority.label,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
      color = textColor,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

@Composable
fun ContractUrgencyBadge(urgency: ContractUrgency, days: Int, modifier: Modifier = Modifier) {
  val (text, bg, fg) = when (urgency) {
    ContractUrgency.URGENT_CRITICAL -> Triple("$days Days Left", StatusUrgentBg, StatusUrgentText)
    ContractUrgency.EXPIRING_30_DAYS -> Triple("$days Days Left", StatusPendingBg, StatusPendingText)
    ContractUrgency.EXPIRING_60_DAYS -> Triple("$days Days Left", Color(0xFFFFF9ED), Color(0xFF9E650D))
    ContractUrgency.HEALTHY -> Triple("Active ($days d)", StatusCompletedBg, StatusCompletedText)
  }
  StatusPill(
    text = text,
    bgColor = bg,
    textColor = fg,
    dotColor = fg,
    modifier = modifier
  )
}

@Composable
fun MaintenanceStatusPill(status: MaintenanceStatus, modifier: Modifier = Modifier) {
  when (status) {
    MaintenanceStatus.SCHEDULED -> StatusPill("Upcoming", StatusPendingBg, StatusPendingText, StatusPendingText, modifier)
    MaintenanceStatus.IN_PROGRESS -> StatusPill("Active Work", Color(0xFFE8F1FC), Color(0xFF134E96), Color(0xFF134E96), modifier)
    MaintenanceStatus.COMPLETED -> StatusPill("Completed", StatusCompletedBg, StatusCompletedText, StatusCompletedText, modifier)
    MaintenanceStatus.OVERDUE -> StatusPill("Overdue", StatusUrgentBg, StatusUrgentText, StatusUrgentText, modifier)
  }
}

@Composable
fun StarRatingBar(
  rating: Int,
  maxStars: Int = 5,
  onRatingChanged: ((Int) -> Unit)? = null,
  modifier: Modifier = Modifier,
  starSize: Dp = 22.dp
) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    for (i in 1..maxStars) {
      val isSelected = i <= rating
      IconButton(
        onClick = { onRatingChanged?.invoke(i) },
        enabled = onRatingChanged != null,
        modifier = Modifier.size(starSize + 12.dp)
      ) {
        Icon(
          imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarOutline,
          contentDescription = "$i Stars",
          tint = if (isSelected) GoldAccent else TutsvilleOutline,
          modifier = Modifier.size(starSize)
        )
      }
    }
  }
}

@Composable
fun TutsvilleFloatingActionButton(
  text: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = ""
) {
  ExtendedFloatingActionButton(
    onClick = onClick,
    containerColor = TutsvillePrimary,
    contentColor = Color.White,
    shape = RoundedCornerShape(16.dp),
    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 10.dp),
    icon = {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(20.dp)
      )
    },
    text = {
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
      )
    },
    modifier = modifier.then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
  )
}

