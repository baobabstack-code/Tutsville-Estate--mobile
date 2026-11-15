package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TutsvilleBackground
import com.example.ui.theme.TutsvilleCardBg
import com.example.ui.theme.TutsvilleOnSurfaceVariant
import com.example.ui.theme.TutsvilleOutline
import com.example.ui.theme.TutsvillePrimary
import com.example.ui.theme.TutsvilleTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
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
          val uid = currentUser!!.uid
          key(uid) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val dbId = try {
              context.getString(R.string.firestore_database_id)
            } catch (_: Exception) {
              null
            }
            val repo = EstateRepository.getInstance(dbId)
            val estateViewModel: EstateViewModel = viewModel(key = uid) {
              EstateViewModel(repo)
            }
            TutsvilleApp(
              viewModel = estateViewModel,
              userEmail = currentUser?.email,
              userDisplayName = currentUser?.displayName,
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
  userDisplayName: String? = null,
  onSignOut: () -> Unit = {}
) {
  val selectedTab by viewModel.selectedTab.collectAsState()
  val contracts by viewModel.contracts.collectAsState()
  val vendors by viewModel.vendors.collectAsState()
  val properties by viewModel.properties.collectAsState()
  val tenants by viewModel.tenants.collectAsState()
  val slotLocks by viewModel.slotLocks.collectAsState()
  val snackbarMessage by viewModel.snackbarMessage.collectAsState()
  val isRoleResolved by viewModel.isRoleResolved.collectAsState()
  val currentRole by viewModel.currentRole.collectAsState()
  val currentTenant by viewModel.currentTenant.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let { msg ->
      snackbarHostState.showSnackbar(
        message = msg,
        duration = SnackbarDuration.Short
      )
      viewModel.clearSnackbar()
    }
  }

  LaunchedEffect(userEmail, userDisplayName) {
    viewModel.setUserIdentity(userEmail, userDisplayName)
  }

  // Gate loading until role resolution finishes on Firestore
  if (viewModel.isUsingFirestore && !isRoleResolved) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(TutsvilleBackground)
        .windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        CircularProgressIndicator(color = GoldAccent, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Verifying Tutsville account access...",
          style = MaterialTheme.typography.bodyMedium.copy(color = TutsvillePrimary, fontWeight = FontWeight.SemiBold)
        )
      }
    }
    return
  }

  // Gate unlisted / uninvited accounts cleanly
  if (currentRole == UserRole.UNREGISTERED) {
    PendingAccessScreen(
      userEmail = userEmail,
      onRefreshAccess = { viewModel.refreshAccess() },
      onSignOut = onSignOut
    )
    return
  }

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  var selectedDrawerId by remember { mutableStateOf<Int?>(null) }

  val visibleNotifications by viewModel.visibleNotifications.collectAsState()
  val readNoticeIds by viewModel.readNoticeIds.collectAsState()
  val unreadAlertsCount by viewModel.unreadNotificationCount.collectAsState()

  val navTabs = remember(currentRole) {
    if (currentRole == UserRole.MANAGER) {
      listOf(
        NavTabItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavTabItem("Cases", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
        NavTabItem("Units", Icons.Filled.HomeWork, Icons.Outlined.HomeWork),
        NavTabItem("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
      )
    } else {
      listOf(
        NavTabItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
        NavTabItem("Cases", Icons.AutoMirrored.Filled.Assignment, Icons.AutoMirrored.Outlined.Assignment),
        NavTabItem("Amenities", Icons.Filled.Apartment, Icons.Outlined.Apartment),
        NavTabItem("Units", Icons.Filled.VpnKey, Icons.Outlined.VpnKey),
        NavTabItem("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
      )
    }
  }

  val safeTabIndex = if (selectedTab in navTabs.indices) selectedTab else 0

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = true,
    drawerContent = {
      val resolvedUserName = currentTenant?.fullName?.takeIf { it.isNotBlank() }
        ?: userDisplayName?.takeIf { it.isNotBlank() }
        ?: userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: "User"
      SidebarDrawerContent(
        currentRole = currentRole,
        userEmail = userEmail,
        userName = resolvedUserName,
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
                  fontSize = 10.sp,
                  maxLines = 1,
                  softWrap = false,
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
            if (currentRole == UserRole.MANAGER) {
              when (drawerId) {
                1 -> TenantsScreen(viewModel)
                2 -> UnitsScreen(viewModel)
                3 -> AmenitiesScreen(viewModel)
                4 -> CasesTableScreen(viewModel)
                5 -> ContractsScreen(viewModel)
                6 -> VendorsScreen(viewModel)
                7 -> MaintenancesCalendarScreen(
                  viewModel = viewModel,
                  onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) }
                )
                else -> DashboardScreen(
                  viewModel = viewModel,
                  onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) },
                  onNavigateToModule = { selectedDrawerId = it }
                )
              }
            } else {
              when (drawerId) {
                1 -> FacilitiesBookingScreen(
                  viewModel = viewModel,
                  defaultSection = 1,
                  showSegmentedHeader = false
                )
                2 -> AmenitiesScreen(viewModel)
                3 -> CasesTableScreen(viewModel)
                4 -> {
                  LaunchedEffect(Unit) {
                    viewModel.activeNotificationPanel.value = true
                  }
                  DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) }
                  )
                }
                5 -> MaintenancesCalendarScreen(
                  viewModel = viewModel,
                  onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) }
                )
                6 -> ContractsScreen(viewModel)
                else -> DashboardScreen(
                  viewModel = viewModel,
                  onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) }
                )
              }
            }
          } else if (currentRole == UserRole.MANAGER) {
            when (tabIndex) {
              0 -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) },
                onNavigateToModule = { selectedDrawerId = it }
              )
              1 -> CasesTableScreen(viewModel)
              2 -> UnitsScreen(viewModel)
              3 -> MaintenancesCalendarScreen(
                viewModel = viewModel,
                onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) }
              )
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
              1 -> CasesTableScreen(viewModel)
              2 -> AmenitiesScreen(viewModel)
              3 -> FacilitiesBookingScreen(
                viewModel = viewModel,
                defaultSection = 1,
                showSegmentedHeader = false
              )
              4 -> MaintenancesCalendarScreen(
                viewModel = viewModel,
                onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) }
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
    val defaultLoc = currentTenant?.assignedPropertyUnit.orEmpty()
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
    // Ensure recomposition whenever live slotLocks change
    val locksSnapshot = slotLocks
    BookFacilitySheet(
      facility = fac,
      initialDate = viewModel.selectedCalendarDisplayDate(),
      isSlotBooked = { date, slot ->
        @Suppress("UNUSED_EXPRESSION")
        locksSnapshot
        viewModel.isSlotBooked(fac.id, date, slot)
      },
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
      currentRole = currentRole,
      onDismiss = { viewModel.activeContractDetail.value = null },
      onTriggerAlert = { viewModel.triggerContractRundownNotice(ctr.id) },
      onRenew = { viewModel.renewContract(ctr.id) },
      onSignContract = { viewModel.signContract(ctr.id) }
    )
  }

  val showAddMaintenance by viewModel.activeAddMaintenanceDialog.collectAsState()
  if (showAddMaintenance) {
    AddMaintenanceDialog(
      initialDate = viewModel.selectedCalendarDisplayDate(),
      vendors = vendors,
      properties = properties,
      onDismiss = { viewModel.activeAddMaintenanceDialog.value = false },
      onSchedule = { title, disc, date, time, contractor, area, interval, notes ->
        viewModel.scheduleMaintenance(title, disc, date, time, contractor, area, interval, notes)
      }
    )
  }

  val showNotificationPanel by viewModel.activeNotificationPanel.collectAsState()
  if (showNotificationPanel) {
    NotificationPanelSheet(
      notifications = visibleNotifications,
      readNoticeIds = readNoticeIds,
      currentRole = currentRole,
      onDismiss = { viewModel.activeNotificationPanel.value = false },
      onNoticeClick = { notice ->
        viewModel.openNotificationTarget(
          reminder = notice,
          onNavigateToTab = { selectedDrawerId = null; viewModel.setSelectedTab(it) },
          onNavigateToModule = { selectedDrawerId = it }
        )
      },
      onMarkAllRead = {
        viewModel.markAllNoticesAsRead()
      },
      onMarkRead = { noticeId ->
        viewModel.markNoticeAsRead(noticeId)
      },
      onDeleteNotice = { noticeId ->
        viewModel.deleteNotification(noticeId)
      },
      onClearAllNotices = {
        viewModel.clearAllNotifications()
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
      tenants = tenants,
      vendors = vendors,
      properties = properties,
      onDismiss = { viewModel.activeAddContractDialog.value = false },
      onSubmit = { title, counterparty, type, value, start, end, days, terms, contact, contactEmail, unit, tenantId, tenantEmail, propertyId ->
        viewModel.submitNewContract(
          title = title,
          counterparty = counterparty,
          type = type,
          annualValue = value,
          startDate = start,
          expiryDate = end,
          daysRemaining = days,
          renewalTerms = terms,
          contactPerson = contact,
          contactEmail = contactEmail,
          unit = unit,
          tenantId = tenantId,
          tenantEmail = tenantEmail,
          propertyId = propertyId
        )
      }
    )
  }
}

@Composable
fun PendingAccessScreen(
  userEmail: String?,
  onRefreshAccess: () -> Unit,
  onSignOut: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(TutsvilleBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(20.dp)
      .testTag("pending_access_screen"),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = TutsvilleCardBg,
      border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.65f)),
      shadowElevation = 4.dp,
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 460.dp)
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Surface(
          shape = CircleShape,
          color = GoldAccent.copy(alpha = 0.16f),
          border = BorderStroke(1.dp, GoldAccent),
          modifier = Modifier.size(60.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.LockPerson,
              contentDescription = "Access Pending Verification",
              tint = TutsvillePrimary,
              modifier = Modifier.size(30.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Estate Access Pending",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TutsvillePrimary
          ),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = if (!userEmail.isNullOrBlank()) {
            "Signed in as $userEmail"
          } else {
            "Your account is authenticated."
          },
          style = MaterialTheme.typography.labelMedium.copy(
            color = GoldAccent,
            fontWeight = FontWeight.Bold
          ),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "Tutsville Estate restricts operational data to approved Estate Managers and verified Residents registered by management. Please ask your Estate Manager to onboard this Google email or assign you to a unit, then tap Check Access Again.",
          style = MaterialTheme.typography.bodySmall.copy(
            color = TutsvilleOnSurfaceVariant,
            lineHeight = 19.sp
          ),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
          onClick = onRefreshAccess,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_check_access_again")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Check Access Again", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onSignOut,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, TutsvilleOutline),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_pending_sign_out")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null,
            tint = Color(0xFFC62828),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sign Out", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        }
      }
    }
  }
}
