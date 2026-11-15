package com.example.tutsville.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Robustly retrieves the host Activity from any ContextWrapper (such as ContextThemeWrapper in Compose).
 */
fun Context.findActivity(): Activity? {
  var currentContext = this
  while (currentContext is ContextWrapper) {
    if (currentContext is Activity) return currentContext
    currentContext = currentContext.baseContext
  }
  return null
}

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  initialEmail: String? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var infoMessage by remember {
    mutableStateOf(
      if (!initialEmail.isNullOrBlank()) {
        "Welcome! Sign in with Google using your invited email ($initialEmail) to claim your residence and lease."
      } else null
    )
  }
  val scrollState = rememberScrollState()

  // Silent auto sign-in attempt on startup for returning authorized Google accounts
  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = coroutineScope
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(12.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = TutsvilleBackground,
      border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.75f)),
      modifier = Modifier
        .fillMaxSize()
        .testTag("auth_screen_gold_border_frame")
    ) {
      BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
      ) {
        val viewportHeight = maxHeight
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .heightIn(min = viewportHeight)
            .padding(horizontal = 20.dp, vertical = 18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // 1. Top Visual Block: Grand Logo Card + Scenic Grounds Photo
          Column(
            modifier = Modifier
              .widthIn(max = 480.dp)
              .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            ElevatedCard(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF000000)
              ),
              elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .border(1.2.dp, GoldAccent.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .testTag("auth_hero_logo_card")
            ) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(16.dp),
                contentAlignment = Alignment.Center
              ) {
                Image(
                  painter = painterResource(id = R.drawable.tutsville_estate_logo_trimmed),
                  contentDescription = "Tutsville Estate",
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.fillMaxSize(0.92f)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = Color.Black),
              border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
              elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
              modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("estate_scenic_photo_card")
            ) {
              Image(
                painter = painterResource(id = R.drawable.tutsville_estate_scenic),
                contentDescription = "Tutsville Estate Residences & Grounds",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
            }
          }

          // 2. Bottom-Anchored Google Sign-In Controls & Concierge Notice
          Column(
            modifier = Modifier
              .widthIn(max = 480.dp)
              .fillMaxWidth()
              .padding(top = 24.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Sign in with your verified Google account to access residences, leases, cases, and amenities.",
              style = MaterialTheme.typography.bodySmall.copy(
                color = TutsvilleOnSurfaceVariant,
                lineHeight = 18.sp
              ),
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (errorMessage != null) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = StatusUrgentBg,
                border = BorderStroke(1.dp, Color(0xFFF0B3B3)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 12.dp)
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = StatusUrgentText,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(color = StatusUrgentText, fontSize = 12.sp),
                    textAlign = TextAlign.Start
                  )
                }
              }
            } else if (infoMessage != null) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = StatusPendingBg,
                border = BorderStroke(1.dp, Color(0xFFF2DCAE)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 12.dp)
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = StatusPendingText,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = infoMessage ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(color = StatusPendingText, fontSize = 12.sp),
                    textAlign = TextAlign.Start
                  )
                }
              }
            }

            // Primary Interactive Google Sign-In Button
            Surface(
              shape = RoundedCornerShape(28.dp),
              color = Color.White,
              border = BorderStroke(1.2.dp, GoldAccent),
              shadowElevation = 3.dp,
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(28.dp))
                .clickable(enabled = !isLoading) {
                  isLoading = true
                  errorMessage = null
                  infoMessage = null
                  onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                      isLoading = false
                      onAuthSuccess()
                    },
                    onAuthError = { err ->
                      isLoading = false
                      errorMessage = err
                    },
                    scope = coroutineScope,
                    onAuthCancelled = { notice ->
                      isLoading = false
                      infoMessage = notice
                    }
                  )
                }
                .testTag("google_capsule_button")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = TutsvillePrimary,
                    strokeWidth = 2.dp
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Signing in...",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = TutsvillePrimary
                    )
                  )
                } else {
                  Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Google logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(22.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.titleSmall.copy(
                      fontWeight = FontWeight.Bold,
                      color = TutsvillePrimary,
                      fontSize = 15.sp
                    )
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Resident Onboarding Notice
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = GoldAccent.copy(alpha = 0.08f),
              border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.4f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_resident_onboarding_notice")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.VerifiedUser,
                  contentDescription = null,
                  tint = GoldAccent,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Estate Residents & Managers: Sign in with your verified Google email to unlock your residence keys, active lease contract, and concierge portal.",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = TutsvillePrimary,
                    lineHeight = 15.sp
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(bottom = 2.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = TutsvilleOnSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Secured with Google Sign-In & Cloud Firestore",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 11.sp,
                  color = TutsvilleOnSurfaceVariant.copy(alpha = 0.85f)
                )
              )
            }
          }
        }
      }
    }
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val activity = context.findActivity() ?: run {
    onUnauthenticated()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: (String?) -> Unit = {}
) {
  val activity = context.findActivity()
  if (activity == null) {
    onAuthError("Unable to find host Activity for Google Sign-In")
    return
  }

  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (_: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder()
    .addCredentialOption(signInOption)
    .build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type: ${credential::class.java.simpleName}")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled: ${e.message}", e)
      onAuthCancelled("Sign-in was dismissed. Tap Sign in with Google to continue.")
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      val msg = e.localizedMessage ?: "Sign in failed"
      val friendlyMessage = when {
        e.javaClass.simpleName.contains("NoCredential", ignoreCase = true) || msg.contains("No credentials", ignoreCase = true) ->
          "No Google Account found on this device. Please add a Google account in device settings and try again."
        msg.contains("16", ignoreCase = true) ->
          "Google Sign-In was dismissed or no account was selected. Please try again."
        msg.contains("Network", ignoreCase = true) ->
          "Network connection error during sign-in. Please check your connection."
        else -> msg
      }
      onErrorMessageSafe(onAuthError, friendlyMessage)
    }
  }
}

private fun onErrorMessageSafe(callback: (String) -> Unit, message: String) {
  callback(message)
}

fun performSignOut(
  context: Context,
  scope: CoroutineScope,
  onComplete: () -> Unit
) {
  val credentialManager = CredentialManager.create(context)
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Clear credential state error: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onComplete()
      }
    }
  }
}
