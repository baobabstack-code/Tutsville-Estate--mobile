package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.ui.components.*
import com.example.tutsville.ui.screens.*
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.TutsvilleBackground
import com.example.ui.theme.TutsvilleOutline
import com.example.ui.theme.TutsvillePrimary
import com.example.ui.theme.TutsvilleTheme
import kotlinx.coroutines.launch

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.example.tutsville.data.repository.EstateRepository
import com.example.tutsville.ui.auth.AuthScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      TutsvilleTheme {
        var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

        DisposableEffect(Unit) {
          val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
          }
          Firebase.auth.addAuthStateListener(listener)
          onDispose {
            Firebase.auth.removeAuthStateListener(listener)
          }
        }

        if (currentUser == null) {
          AuthScreen(
            onAuthSuccess = {
              currentUser = Firebase.auth.currentUser
            }
          )
        } else {
          key(currentUser?.uid ?: "default_user") {
            val context = androidx.compose.ui.platform.LocalContext.current
            val dbId = try {
              context.getString(R.string.firestore_database_id)
            } catch (e: Exception) {
              null
            }
            TutsvilleApp(
              viewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                EstateViewModel(EstateRepository(dbId))
              }
            )
          }
        }
      }
    }
  }
}

data class NavTabItem(
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
)

@Composable
fun TutsvilleApp(
  viewModel: EstateViewModel = viewModel()
) {
  val currentRole by viewModel.currentRole.collectAsState()
  val selectedTab by viewModel.selectedTab.collectAsState()
  val contracts by viewModel.contracts.collectAsState()
  val reminders by viewModel.reminders.collectAsState()
  val vendors by viewModel.vendors.collectAsState()
  val snackbarMessage by viewModel.snackbarMessage.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  // Display transient snackbars
  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(
        message = msg,
        duration = SnackbarDuration.Short
      )
      viewModel.clearSnackbar()
    }
  }

  // Count unread notices and expiring contracts for the notification bell
  val unreadAlertsCount = remember(contracts, reminders) {
    val expiringContractsCount = contracts.count { it.daysRemaining <= 60 && !it.isNotificationSent }
    expiringContractsCount + reminders.take(2).size
  }

  // Define Navigation Tabs according to active Role
  val navTabs = remember(currentRole) {
    when (currentRole) {
      UserRole.MANAGER -> listOf(
        NavTabItem("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavTabItem("Cases", Icons.Filled.Assignment, Icons.Outlined.Assignment),
        NavTabItem("Contracts", Icons.Filled.Description, Icons.Outlined.Description),
        NavTabItem("Facilities", Icons.Filled.Apartment, Icons.Outlined.Apartment),
        NavTabItem("Reminders", Icons.Filled.Campaign, Icons.Outlined.Campaign),
        NavTabItem("Reviews", Icons.Filled.Star, Icons.Outlined.StarOutline)
      )
      UserRole.TENANT -> listOf(
        NavTabItem("Facilities", Icons.Filled.Apartment, Icons.Outlined.Apartment),
        NavTabItem("My Cases", Icons.Filled.Assignment, Icons.Outlined.Assignment),
        NavTabItem("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavTabItem("Reviews", Icons.Filled.Star, Icons.Outlined.StarOutline),
        NavTabItem("Notices", Icons.Filled.Campaign, Icons.Outlined.Campaign)
      )
      UserRole.VENDOR -> listOf(
        NavTabItem("Work Orders", Icons.Filled.Engineering, Icons.Outlined.Engineering),
        NavTabItem("Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavTabItem("Scorecard", Icons.Filled.Star, Icons.Outlined.StarOutline),
        NavTabItem("Notices", Icons.Filled.Campaign, Icons.Outlined.Campaign)
      )
    }
  }

  // Ensure selected tab doesn't exceed bounds when switching roles
  val safeTabIndex = if (selectedTab in navTabs.indices) selectedTab else 0

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = TutsvilleBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      RoleSwitcherBar(
        currentRole = currentRole,
        onRoleSelected = { role -> viewModel.setRole(role) },
        onNotificationClick = { viewModel.activeNotificationPanel.value = true },
        unreadCount = unreadAlertsCount
      )
    },
    bottomBar = {
      NavigationBar(
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.navigationBars)
          .testTag("main_navigation_bar"),
        containerColor = Color(0xFFFAF8F5),
        tonalElevation = 3.dp
      ) {
        navTabs.forEachIndexed { index, tab ->
          val isSelected = index == safeTabIndex
          NavigationBarItem(
            selected = isSelected,
            onClick = { viewModel.setSelectedTab(index) },
            icon = {
              Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title,
                modifier = Modifier.size(22.dp)
              )
            },
            label = {
              Text(
                text = tab.title,
                fontSize = 10.sp,
                maxLines = 1
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = TutsvillePrimary,
              selectedTextColor = TutsvillePrimary,
              indicatorColor = Color(0xFFC4ECD4),
              unselectedIconColor = Color(0xFF7E827F),
              unselectedTextColor = Color(0xFF7E827F)
            ),
            modifier = Modifier.testTag("nav_tab_${tab.title.lowercase().replace(" ", "_")}")
          )
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      Crossfade(targetState = (currentRole to safeTabIndex), label = "ScreenTransition") { (role, tab) ->
        when (role) {
          UserRole.MANAGER -> when (tab) {
            0 -> MaintenancesCalendarScreen(viewModel)
            1 -> CasesScreen(viewModel)
            2 -> ContractsScreen(viewModel)
            3 -> FacilitiesBookingScreen(viewModel)
            4 -> RemindersScreen(viewModel)
            5 -> ReviewsScreen(viewModel)
            else -> MaintenancesCalendarScreen(viewModel)
          }
          UserRole.TENANT -> when (tab) {
            0 -> FacilitiesBookingScreen(viewModel)
            1 -> CasesScreen(viewModel)
            2 -> MaintenancesCalendarScreen(viewModel)
            3 -> ReviewsScreen(viewModel)
            4 -> RemindersScreen(viewModel)
            else -> FacilitiesBookingScreen(viewModel)
          }
          UserRole.VENDOR -> when (tab) {
            0 -> CasesScreen(viewModel)
            1 -> MaintenancesCalendarScreen(viewModel)
            2 -> ReviewsScreen(viewModel)
            3 -> RemindersScreen(viewModel)
            else -> CasesScreen(viewModel)
          }
        }
      }
    }

    // Interactive Dialogs & Sheets

    val showReportCase by viewModel.activeReportCaseDialog.collectAsState()
    if (showReportCase) {
      ReportCaseDialog(
        onDismiss = { viewModel.activeReportCaseDialog.value = false },
        onSubmit = { title, desc, category, priority, loc ->
          viewModel.submitNewCase(title, desc, category, priority, loc)
        }
      )
    }

    val assignCase by viewModel.activeAssignVendorCase.collectAsState()
    assignCase?.let { c ->
      AssignVendorDialog(
        caseItem = c,
        vendors = vendors,
        onDismiss = { viewModel.activeAssignVendorCase.value = null },
        onAssign = { vendorId ->
          viewModel.assignVendor(c.id, vendorId)
        }
      )
    }

    val bookFacility by viewModel.activeBookFacility.collectAsState()
    bookFacility?.let { fac ->
      BookFacilitySheet(
        facility = fac,
        onDismiss = { viewModel.activeBookFacility.value = null },
        onConfirm = { date, slot, guests, notes ->
          viewModel.bookFacilitySlot(fac.id, date, slot, guests, notes)
        }
      )
    }

    val reviewCase by viewModel.activeReviewCase.collectAsState()
    reviewCase?.let { c ->
      ServiceReviewDialog(
        caseItem = c,
        onDismiss = { viewModel.activeReviewCase.value = null },
        onSubmit = { rating, tags, comment ->
          viewModel.submitReview(c.id, rating, tags, comment)
        }
      )
    }

    val showSendReminder by viewModel.activeSendReminderDialog.collectAsState()
    if (showSendReminder) {
      SendReminderDialog(
        onDismiss = { viewModel.activeSendReminderDialog.value = false },
        onSend = { title, msg, audience, urgency, isBroadcast ->
          viewModel.sendReminder(title, msg, audience, urgency, isBroadcast)
        }
      )
    }

    val contractDetail by viewModel.activeContractDetail.collectAsState()
    contractDetail?.let { ctr ->
      ContractDetailDialog(
        contract = ctr,
        onDismiss = { viewModel.activeContractDetail.value = null },
        onTriggerAlert = { viewModel.triggerContractRundownNotice(ctr.id) },
        onRenew = { viewModel.renewContract(ctr.id) }
      )
    }

    val showAddMaintenance by viewModel.activeAddMaintenanceDialog.collectAsState()
    if (showAddMaintenance) {
      AddMaintenanceDialog(
        onDismiss = { viewModel.activeAddMaintenanceDialog.value = false },
        onSchedule = { title, disc, date, time, contractor, area, interval, notes ->
          viewModel.scheduleMaintenance(title, disc, date, time, contractor, area, interval, notes)
        }
      )
    }

    val showNotificationPanel by viewModel.activeNotificationPanel.collectAsState()
    if (showNotificationPanel) {
      NotificationPanelSheet(
        reminders = reminders,
        contracts = contracts,
        onDismiss = { viewModel.activeNotificationPanel.value = false },
        onContractClick = { ctr ->
          viewModel.activeNotificationPanel.value = false
          viewModel.activeContractDetail.value = ctr
        }
      )
    }
  }
}
