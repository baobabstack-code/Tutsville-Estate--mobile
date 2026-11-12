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
            val repo = EstateRepository.getInstance(dbId)
            LaunchedEffect(currentUser?.uid) {
              repo.seedDatabase(force = false)
            }
            TutsvilleApp(
              viewModel = viewModel {
                EstateViewModel(repo)
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

  // Automatically resolve user role and claim property for authenticated resident email
  LaunchedEffect(userEmail) {
    viewModel.setUserEmail(userEmail)
  }

  val currentRole by viewModel.currentRole.collectAsState()
  val currentTenant by viewModel.currentTenant.collectAsState()

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  var selectedDrawerId by remember { mutableStateOf<Int?>(null) }

  // Count unread notices and expiring contracts for the notification bell
  val unreadAlertsCount = remember(contracts, reminders) {
    val expiringContractsCount = contracts.count { it.daysRemaining <= 60 && !it.isNotificationSent }
    expiringContractsCount + reminders.take(2).size
  }

  // Manager Bottom Navigation: 3 Core Tabs (Dashboard, Cases, Units)
  // Resident Bottom Navigation: 4 Core Tabs (Dashboard, My Requests, Amenities, My Residence)
  val navTabs = remember(currentRole) {
    if (currentRole == UserRole.MANAGER) {
      listOf(
        NavTabItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavTabItem("Cases", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
        NavTabItem("Units", Icons.Filled.HomeWork, Icons.Outlined.HomeWork)
      )
    } else {
      listOf(
        NavTabItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavTabItem("My Requests", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
        NavTabItem("Amenities", Icons.Filled.Apartment, Icons.Outlined.Apartment),
        NavTabItem("My Residence", Icons.Filled.VpnKey, Icons.Outlined.VpnKey)
      )
    }
  }

  val safeTabIndex = if (selectedTab in navTabs.indices) selectedTab else 0

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = true,
    drawerContent = {
      SidebarDrawerContent(
        currentRole = currentRole,
        userEmail = userEmail,
        userName = currentTenant?.fullName ?: "Julian Vance",
        selectedDrawerId = selectedDrawerId,
        onSelectDrawerItem = { drawerId ->
          selectedDrawerId = drawerId
          scope.launch { drawerState.close() }
        },
        onSignOut = {
          scope.launch { drawerState.close() }
          onSignOut()
        }
      )
    }
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = TutsvilleBackground,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        EstateHeaderBar(
          currentRole = currentRole,
          unreadCount = unreadAlertsCount,
          onOpenDrawer = { scope.launch { drawerState.open() } },
          onOpenNotifications = { viewModel.activeNotificationPanel.value = true }
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
            val isSelected = selectedDrawerId == null && index == safeTabIndex
            NavigationBarItem(
              selected = isSelected,
              onClick = {
                selectedDrawerId = null
                viewModel.setSelectedTab(index)
              },
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
                  fontSize = 11.sp,
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
        Crossfade(targetState = Pair(selectedDrawerId, safeTabIndex), label = "ScreenTransition") { (drawerId, tabIndex) ->
          if (drawerId != null) {
            when (drawerId) {
              1 -> TenantsScreen(viewModel)
              2 -> UnitsScreen(viewModel)
              3 -> AmenitiesScreen(viewModel)
              4 -> CasesTableScreen(viewModel)
              5 -> ContractsScreen(viewModel)
              6 -> VendorsScreen(viewModel)
              else -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) },
                onNavigateToModule = { selectedDrawerId = it }
              )
            }
          } else if (currentRole == UserRole.MANAGER) {
            when (tabIndex) {
              0 -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) },
                onNavigateToModule = { selectedDrawerId = it }
              )
              1 -> CasesTableScreen(viewModel) // Cases
              2 -> UnitsScreen(viewModel)      // Units
              else -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) },
                onNavigateToModule = { selectedDrawerId = it }
              )
            }
          } else {
            when (tabIndex) {
              0 -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) }
              )
              1 -> CasesTableScreen(viewModel)   // My Requests
              2 -> AmenitiesScreen(viewModel)    // Amenities
              3 -> FacilitiesBookingScreen(      // My Residence
                viewModel = viewModel,
                defaultSection = 1,
                showSegmentedHeader = false
              )
              else -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) }
              )
            }
          }
        }
      }
    }
  }
    // Interactive Dialogs & Sheets

    val showReportCase by viewModel.activeReportCaseDialog.collectAsState()
    if (showReportCase) {
      val defaultLoc = currentTenant?.assignedPropertyUnit ?: "Tower A • Apt 402"
      ReportCaseDialog(
        initialLocation = defaultLoc,
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
        },
        onAddNewVendor = {
          viewModel.activeAddVendorDialog.value = true
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

    val showAddFacility by viewModel.activeAddFacilityDialog.collectAsState()
    if (showAddFacility) {
      AddFacilityDialog(
        onDismiss = { viewModel.activeAddFacilityDialog.value = false },
        onSubmit = { name, tag, cap, loc, desc, am, slots, rules ->
          viewModel.submitNewFacility(name, tag, cap, loc, desc, am, slots, rules)
        }
      )
    }

    val showAddVendor by viewModel.activeAddVendorDialog.collectAsState()
    if (showAddVendor) {
      AddVendorDialog(
        onDismiss = { viewModel.activeAddVendorDialog.value = false },
        onSubmit = { name, specialty, phone, email, tech ->
          viewModel.submitNewVendor(name, specialty, phone, email, tech)
        }
      )
    }

    val showAddContract by viewModel.activeAddContractDialog.collectAsState()
    if (showAddContract) {
      AddContractDialog(
        onDismiss = { viewModel.activeAddContractDialog.value = false },
        onSubmit = { title, counterparty, type, value, start, end, days, terms, contact, contactEmail ->
          viewModel.submitNewContract(title, counterparty, type, value, start, end, days, terms, contact, contactEmail)
        }
      )
    }
  }
