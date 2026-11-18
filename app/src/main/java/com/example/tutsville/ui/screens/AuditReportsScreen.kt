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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tutsville.data.model.*
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class AuditTimelineItem(
  val id: String,
  val caseId: String,
  val caseTitle: String,
  val location: String,
  val badgeLabel: String,
  val isNote: Boolean,
  val detailText: String,
  val actorLabel: String,
  val timestampLabel: String,
  val epochMs: Long
)

@Composable
fun AuditReportsScreen(
  viewModel: EstateViewModel
) {
  val cases by viewModel.cases.collectAsState()

  val initialYm = remember { EstateDateUtils.parseKeyToYearMonth(EstateDateUtils.todayKey()) }
  var selectedYear by remember { mutableIntStateOf(initialYm.first) }
  var selectedMonth by remember { mutableIntStateOf(initialYm.second) }
  var isRolling30Days by remember { mutableStateOf(false) }

  val nowMs = remember(cases) { System.currentTimeMillis() }
  val thirtyDaysAgoMs = remember(nowMs) { nowMs - 30L * 24L * 60L * 60L * 1000L }

  fun resolveCaseEpoch(caseItem: CaseItem): Long {
    if (caseItem.createdAtEpochMs > 0L) return caseItem.createdAtEpochMs
    val key = EstateDateUtils.toDateKey(caseItem.reportedDate) ?: return 0L
    return try {
      SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key)?.time ?: 0L
    } catch (_: Exception) {
      0L
    }
  }

  fun isEpochInSelectedPeriod(epochMs: Long, fallbackDateStr: String): Boolean {
    if (isRolling30Days) {
      if (epochMs > 0L) return epochMs >= thirtyDaysAgoMs
      val key = EstateDateUtils.toDateKey(fallbackDateStr) ?: return true
      val minKey = EstateDateUtils.offsetKey(-30)
      return key >= minKey
    }
    if (epochMs > 0L) {
      val cal = Calendar.getInstance().apply { timeInMillis = epochMs }
      return cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth
    }
    val key = EstateDateUtils.toDateKey(fallbackDateStr) ?: return false
    val (y, m) = EstateDateUtils.parseKeyToYearMonth(key)
    return y == selectedYear && m == selectedMonth
  }

  val filteredCases = remember(cases, selectedYear, selectedMonth, isRolling30Days) {
    cases.filter { c ->
      val caseMatches = isEpochInSelectedPeriod(resolveCaseEpoch(c), c.reportedDate)
      val historyMatches = c.statusHistory.any { isEpochInSelectedPeriod(it.epochMs, it.timestamp) }
      val notesMatch = c.notes.any { isEpochInSelectedPeriod(it.createdAtEpochMs, it.createdAt) }
      caseMatches || historyMatches || notesMatch
    }
  }

  val totalCases = filteredCases.size
  val openCases = filteredCases.count { it.status == CaseStatus.OPEN || it.status == CaseStatus.ASSIGNED }
  val inProgressCases = filteredCases.count { it.status == CaseStatus.IN_PROGRESS }
  val resolvedCases = filteredCases.count { it.status == CaseStatus.RESOLVED }

  val timelineEntries = remember(filteredCases, selectedYear, selectedMonth, isRolling30Days) {
    val entries = mutableListOf<AuditTimelineItem>()
    filteredCases.forEach { c ->
      val historyList = c.statusHistory.ifEmpty {
        listOf(
          CaseMovementEntry(
            status = c.status.label,
            note = "Logged at ${c.location}",
            updatedBy = c.reportedBy,
            timestamp = c.reportedDate,
            epochMs = resolveCaseEpoch(c)
          )
        )
      }
      historyList.forEachIndexed { idx, move ->
        if (isEpochInSelectedPeriod(move.epochMs, move.timestamp)) {
          entries.add(
            AuditTimelineItem(
              id = "${c.id}_move_$idx",
              caseId = c.id,
              caseTitle = c.title,
              location = c.location,
              badgeLabel = move.status.ifBlank { c.status.label },
              isNote = false,
              detailText = move.note.ifBlank { "Status updated to ${move.status}" },
              actorLabel = move.updatedBy.ifBlank { c.reportedBy },
              timestampLabel = move.timestamp.ifBlank { c.reportedDate },
              epochMs = if (move.epochMs > 0L) move.epochMs else resolveCaseEpoch(c)
            )
          )
        }
      }
      c.notes.forEachIndexed { idx, n ->
        if (isEpochInSelectedPeriod(n.createdAtEpochMs, n.createdAt)) {
          entries.add(
            AuditTimelineItem(
              id = "${c.id}_note_${n.id.ifBlank { idx.toString() }}",
              caseId = c.id,
              caseTitle = c.title,
              location = c.location,
              badgeLabel = "Note Added",
              isNote = true,
              detailText = n.text,
              actorLabel = "${n.authorName.ifBlank { "Manager" }} (${n.authorRole})",
              timestampLabel = n.createdAt.ifBlank { c.reportedDate },
              epochMs = if (n.createdAtEpochMs > 0L) n.createdAtEpochMs else resolveCaseEpoch(c)
            )
          )
        }
      }
    }
    entries.sortedByDescending { it.epochMs }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(TutsvilleBackground)
      .testTag("audit_reports_screen")
  ) {
    Surface(
      color = TutsvilleCardBg,
      border = BorderStroke(1.dp, TutsvilleOutline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Reports & Audits",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        )
        Text(
          text = "Case KPIs, movement audit logs & manager note timelines",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = !isRolling30Days,
            onClick = { isRolling30Days = false },
            label = { Text("Monthly View", fontSize = 12.sp) },
            modifier = Modifier.testTag("filter_audit_monthly")
          )
          FilterChip(
            selected = isRolling30Days,
            onClick = { isRolling30Days = true },
            label = { Text("Rolling 30 Days", fontSize = 12.sp) },
            modifier = Modifier.testTag("filter_audit_rolling30")
          )
        }

        if (!isRolling30Days) {
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = TutsvilleBackground,
            border = BorderStroke(1.dp, TutsvilleOutline),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = {
                  val cal = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, 1)
                    add(Calendar.MONTH, -1)
                  }
                  selectedYear = cal.get(Calendar.YEAR)
                  selectedMonth = cal.get(Calendar.MONTH)
                },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("btn_audit_prev_month")
              ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = TutsvillePrimary)
              }

              Text(
                text = EstateDateUtils.monthTitle(selectedYear, selectedMonth),
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )

              IconButton(
                onClick = {
                  val cal = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, 1)
                    add(Calendar.MONTH, 1)
                  }
                  selectedYear = cal.get(Calendar.YEAR)
                  selectedMonth = cal.get(Calendar.MONTH)
                },
                modifier = Modifier
                  .size(48.dp)
                  .testTag("btn_audit_next_month")
              ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TutsvillePrimary)
              }
            }
          }
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AuditKpiCard(
              label = "Total Cases",
              count = totalCases,
              icon = Icons.Default.Assignment,
              accentColor = TutsvillePrimary,
              modifier = Modifier.weight(1f)
            )
            AuditKpiCard(
              label = "Open / Assigned",
              count = openCases,
              icon = Icons.Default.Schedule,
              accentColor = StatusPendingText,
              modifier = Modifier.weight(1f)
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AuditKpiCard(
              label = "In Progress",
              count = inProgressCases,
              icon = Icons.Default.Build,
              accentColor = Color(0xFF134E96),
              modifier = Modifier.weight(1f)
            )
            AuditKpiCard(
              label = "Resolved",
              count = resolvedCases,
              icon = Icons.Default.CheckCircle,
              accentColor = StatusCompletedText,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Case Movement & Notes Timeline",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = TutsvillePrimary
            )
          )
          Text(
            text = "${timelineEntries.size} entries",
            style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant)
          )
        }
      }

      if (timelineEntries.isEmpty()) {
        item {
          OperationalCard {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = null,
                tint = TutsvilleSecondary,
                modifier = Modifier.size(32.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "No Case Activity in Period",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
              )
              Text(
                text = "Switch months or enable Rolling 30 Days to review earlier activity.",
                style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
              )
            }
          }
        }
      } else {
        items(timelineEntries, key = { it.id }) { item ->
          OperationalCard {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "#${item.caseId} • ${item.location}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TutsvilleSecondary
                )
              )
              StatusPill(
                text = item.badgeLabel,
                bgColor = if (item.isNote) TutsvilleSecondaryContainer else StatusCompletedBg,
                textColor = if (item.isNote) TutsvilleOnSecondaryContainer else StatusCompletedText,
                dotColor = if (item.isNote) GoldAccent else StatusCompletedText
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = item.caseTitle,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = item.detailText,
              style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnBackground)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = item.actorLabel,
                style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
              )
              Text(
                text = item.timestampLabel,
                style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 10.sp)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AuditKpiCard(
  label: String,
  count: Int,
  icon: ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = TutsvilleCardBg,
    border = BorderStroke(1.dp, TutsvilleOutline)
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = accentColor,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "$count",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            color = accentColor
          )
        )
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall.copy(
            color = TutsvilleOnSurfaceVariant,
            fontSize = 11.sp
          )
        )
      }
    }
  }
}
