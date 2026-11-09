package com.example.tutsville.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
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
import com.example.tutsville.data.model.CaseItem
import com.example.tutsville.data.model.CaseStatus
import com.example.tutsville.data.model.VendorItem
import com.example.tutsville.ui.components.OperationalCard
import com.example.tutsville.ui.components.StarRatingBar
import com.example.tutsville.ui.components.StatusPill
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.*

@Composable
fun ReviewsScreen(
  viewModel: EstateViewModel,
  modifier: Modifier = Modifier
) {
  val vendors by viewModel.vendors.collectAsState()
  val cases by viewModel.cases.collectAsState()

  val reviewedCases = remember(cases) {
    cases.filter { it.status == CaseStatus.RESOLVED && it.rating != null }
  }

  val pendingReviewCases = remember(cases) {
    cases.filter { it.status == CaseStatus.RESOLVED && it.rating == null }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground),
    contentPadding = PaddingValues(bottom = 90.dp)
  ) {
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
          text = "Service Reviews & Vendor Scorecards",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          )
        )
        Text(
          text = "Audited quality ratings, resident feedback & contractor satisfaction indexes",
          style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
        )
      }
    }

    // Pending review section
    if (pendingReviewCases.isNotEmpty()) {
      item {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = TutsvilleSecondaryContainer,
            border = BorderStroke(1.dp, TutsvilleSecondary),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Pending Resolution Verification",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TutsvilleOnSecondaryContainer
                  )
                )
                Text(
                  text = "${pendingReviewCases.first().title} resolved. Please rate the service quality.",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = TutsvilleOnSecondaryContainer
                  )
                )
              }
              Button(
                onClick = { viewModel.activeReviewCase.value = pendingReviewCases.first() },
                colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("rate_pending_service_button")
              ) {
                Icon(Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rate Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // Vendor Performance Scorecard Section
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
          text = "Certified Vendor Scorecards",
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary,
            letterSpacing = 0.04.sp
          )
        )
        Spacer(modifier = Modifier.height(6.dp))
      }
    }

    items(vendors, key = { it.id }) { vendor ->
      Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
        OperationalCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Verified, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = vendor.name,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = TutsvillePrimary
                )
              )
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFF1EEE6)
            ) {
              Text(
                text = vendor.specialty.label,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TutsvillePrimary
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              StarRatingBar(rating = vendor.rating.toInt(), starSize = 16.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${vendor.rating} / 5.0",
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = TutsvilleSecondary
                )
              )
              Text(
                text = " (${vendor.reviewCount} resident audits)",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.sp,
                  color = TutsvilleOnSurfaceVariant
                )
              )
            }
            Text(
              text = "${vendor.activeJobsCount} active jobs",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TutsvillePrimary
              )
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Lead Technician: ${vendor.leadTech} • Contact: ${vendor.phone}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
          )
        }
      }
    }

    // Resident Audit Testimonial Logs
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
          text = "Recent Verified Case Reviews (${reviewedCases.size})",
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary,
            letterSpacing = 0.04.sp
          )
        )
      }
    }

    items(reviewedCases, key = { it.id }) { c ->
      Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        OperationalCard {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = c.title,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = TutsvillePrimary)
            )
            c.rating?.let { r ->
              StarRatingBar(rating = r, starSize = 14.dp)
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Contractor: ${c.assignedVendorName ?: "Certified Vendor"} • Reported by: ${c.reportedBy}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TutsvilleOnSurfaceVariant)
          )

          if (!c.reviewComment.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "\"${c.reviewComment}\"",
              style = MaterialTheme.typography.bodySmall.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = TutsvilleOnBackground
              )
            )
          }

          if (c.reviewTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              c.reviewTags.forEach { tag ->
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = TutsvilleSecondaryContainer
                ) {
                  Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 9.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = TutsvilleOnSecondaryContainer
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
