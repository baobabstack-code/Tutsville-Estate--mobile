package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tutsville.data.model.UserRole
import com.example.tutsville.data.repository.EstateRepository
import com.example.tutsville.ui.auth.AuthScreen
import com.example.tutsville.ui.auth.performSignOut
import com.example.tutsville.ui.components.*
import com.example.tutsville.ui.screens.*
import com.example.tutsville.ui.viewmodel.EstateViewModel
import com.example.ui.theme.TutsvilleBackground
import com.example.ui.theme.TutsvillePrimary
import com.example.ui.theme.TutsvilleTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val deepLinkEmail = intent?.data?.getQueryParameter("email")

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
            initialEmail = deepLinkEmail,
            onAuthSuccess = {
              currentUser = Firebase.auth.currentUser
            }
          )
        } else {
          key(currentUser?.uid ?: "default_user") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val dbId = try {
              context.getString(R.string.firestore_database_id)
            } catch (e: Exception) {
              null
            }
            TutsvilleApp(
              viewModel = viewModel {
                EstateViewModel(EstateRepository.getInstance(dbId))
              },
              userEmail = currentUser?.email,
              onSignOut = {
                performSignOut(context, scope) {
                  currentUser = null
                }
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
  viewModel: EstateViewModel = viewModel(),
  userEmail: String? = null,
  onSignOut: () -> Unit = {}
) {
  val selectedTab by viewModel.selectedTab.collectAsState()
  val contracts by viewModel.contracts.collectAsState()
  val reminders by viewModel.reminders.collectAsState()
  val vendors by viewModel.vendors.collectAsState()
  val properties by viewModel.properties.collectAsState()
  val snackbarMessage by viewModel.snackbarMessage.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

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

  // Automatically match and claim property assignment for authenticated resident email
  LaunchedEffect(userEmail) {
    if (!userEmail.isNullOrBlank()) {
      viewModel.claimPropertyForEmail(userEmail)
    }
  }

  // Count unread notices and expiring contracts for the notification bell
  val unreadAlertsCount = remember(contracts, reminders) {
    val expiringContractsCount = contracts.count { it.daysRemaining <= 60 && !it.isNotificationSent }
    expiringContractsCount + reminders.take(2).size
  }

  // Authenticated Estate Manager Navigation Tabs
  val navTabs = remember {
    listOf(
      NavTabItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
      NavTabItem("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
      NavTabItem("Cases", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
      NavTabItem("Facilities", Icons.Filled.Apartment, Icons.Outlined.Apartment),
      NavTabItem("Contracts", Icons.Filled.Description, Icons.Outlined.Description),
      NavTabItem("Notices", Icons.Filled.Campaign, Icons.Outlined.Campaign),
      NavTabItem("Reviews", Icons.Filled.Star, Icons.Outlined.StarOutline)
    )
  }

  val safeTabIndex = if (selectedTab in navTabs.indices) selectedTab else 0

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = TutsvilleBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      EstateHeaderBar(
        userEmail = userEmail,
        onNotificationClick = { viewModel.activeNotificationPanel.value = true },
        onSignOutClick = onSignOut,
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
                modifier = Modifier.size(20.dp)
              )
            },
            label = {
              Text(
                text = tab.title,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
      Crossfade(targetState = safeTabIndex, label = "ScreenTransition") { tabIndex ->
        when (tabIndex) {
          0 -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToTab = { viewModel.setSelectedTab(it) }
          )
          1 -> MaintenancesCalendarScreen(viewModel)
          2 -> CasesScreen(viewModel)
          3 -> FacilitiesBookingScreen(viewModel)
          4 -> ContractsScreen(viewModel)
          5 -> RemindersScreen(viewModel)
          6 -> ReviewsScreen(viewModel)
          else -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToTab = { viewModel.setSelectedTab(it) }
          )
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

    // Property Management Dialogs
    val showListProperty by viewModel.activeListPropertyDialog.collectAsState()
    if (showListProperty) {
      ListPropertyDialog(
        onDismiss = { viewModel.activeListPropertyDialog.value = false },
        onSubmit = { unit, wing, arch, parking, amenities, rent ->
          viewModel.listProperty(unit, wing, arch, parking, amenities, rent)
        }
      )
    }

    val assignTenantProp by viewModel.activeAssignTenantProperty.collectAsState()
    assignTenantProp?.let { prop ->
      AssignTenantDialog(
        property = prop,
        onDismiss = { viewModel.activeAssignTenantProperty.value = null },
        onAssign = { name, email, start, end ->
          viewModel.assignPropertyToTenant(prop.id, name, email, start, end)
        }
      )
    }

    val shareInviteProp by viewModel.activeShareInviteDialog.collectAsState()
    shareInviteProp?.let { prop ->
      ShareTenantInviteDialog(
        property = prop,
        onDismiss = { viewModel.activeShareInviteDialog.value = null }
      )
    }

    // Tenant Management Dialogs
    val showCreateTenant by viewModel.activeCreateTenantDialog.collectAsState()
    if (showCreateTenant) {
      CreateTenantDialog(
        onDismiss = { viewModel.activeCreateTenantDialog.value = false },
        onSubmit = { fullName, email, phone, emergencyName, emergencyPhone ->
          viewModel.createTenant(fullName, email, phone, emergencyName, emergencyPhone)
        }
      )
    }

    val assignPropToTenant by viewModel.activeAssignPropertyToTenant.collectAsState()
    assignPropToTenant?.let { tenant ->
      val availableProps = properties.filter { it.status == com.example.tutsville.data.model.PropertyStatus.AVAILABLE }
      AssignPropertyToTenantDialog(
        tenant = tenant,
        availableProperties = availableProps,
        onDismiss = { viewModel.activeAssignPropertyToTenant.value = null },
        onAssign = { propertyId, leaseStart, leaseEnd ->
          viewModel.assignTenantToPropertyById(tenant.id, propertyId, leaseStart, leaseEnd)
        }
      )
    }

    val shareTenantInvite by viewModel.activeTenantInviteSheet.collectAsState()
    shareTenantInvite?.let { tenant ->
      TenantInviteDialog(
        tenant = tenant,
        onDismiss = { viewModel.activeTenantInviteSheet.value = null }
      )
    }
  }
}
