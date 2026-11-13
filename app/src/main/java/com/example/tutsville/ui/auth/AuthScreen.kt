package com.example.tutsville.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.tutsville.data.repository.EstateRepository
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
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
        "Welcome! Complete sign-in with your assigned email ($initialEmail) to claim your estate keys."
      } else null
    )
  }

  // Email / Password Form States
  var isCreateAccountMode by remember { mutableStateOf(false) }
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf(initialEmail ?: "") }
  var password by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  var resetEmail by remember { mutableStateOf("") }
  val scrollState = rememberScrollState()

  // Root Box with safe drawing insets and outer margin
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(12.dp),
    contentAlignment = Alignment.Center
  ) {
    // Inset frame around the entire sign-in screen in refined caramel gold
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = TutsvilleBackground,
      border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.75f)),
      modifier = Modifier
        .fillMaxSize()
        .testTag("auth_screen_gold_border_frame")
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // 1. Top Visual Block: Expanded Grand Logo Card + Scenic Grounds Photo (Grouped tightly)
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Expanded Grand Logo Hero Card taking the vertical space as requested
            ElevatedCard(
              shape = RoundedCornerShape(20.dp),
              colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF000000)
              ),
              elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 6.dp
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // Tutsville Estate Scenic Grounds Photo Card (Natural 16:9 Landscape)
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

          // Exact same compact gap from Sign Up used on both screens
          Spacer(modifier = Modifier.height(12.dp))

          // 2. Form & Authentication controls
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {

            // Diagnostic / Error feedback banner
            if (errorMessage != null) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = StatusUrgentBg,
                border = BorderStroke(1.dp, Color(0xFFF0B3B3)),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 10.dp)
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
                  .padding(bottom = 10.dp)
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

            // Reference Layout Form Fields with Standalone Labels Above Inputs
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              if (isCreateAccountMode) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "Full Name",
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = TutsvilleOnSurface
                    )
                  )
                  OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    placeholder = { Text("Enter your full name", color = TutsvilleOnSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = {
                      Icon(Icons.Default.Person, contentDescription = null, tint = TutsvillePrimary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = Color.White,
                      unfocusedContainerColor = Color.White,
                      focusedBorderColor = TutsvillePrimary,
                      unfocusedBorderColor = TutsvilleOutline
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("fullname_input")
                  )
                }
              }

              // Email Address Label & Pill Input Field
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Email Address",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TutsvilleOnSurface
                  )
                )
                OutlinedTextField(
                  value = email,
                  onValueChange = { email = it },
                  placeholder = { Text("elementary221b@gmail.com", color = TutsvilleOnSurfaceVariant.copy(alpha = 0.6f)) },
                  leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = TutsvillePrimary)
                  },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                  shape = RoundedCornerShape(24.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = TutsvillePrimary,
                    unfocusedBorderColor = TutsvilleOutline
                  ),
                  modifier = Modifier.fillMaxWidth().testTag("email_input")
                )
              }

              // Password Label & Pill Input Field
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Password",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TutsvilleOnSurface
                  )
                )
                OutlinedTextField(
                  value = password,
                  onValueChange = { password = it },
                  placeholder = { Text("••••••••••••••••", color = TutsvilleOnSurfaceVariant.copy(alpha = 0.6f)) },
                  leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TutsvillePrimary)
                  },
                  trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                      Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = TutsvilleOnSurfaceVariant
                      )
                    }
                  },
                  visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                  shape = RoundedCornerShape(24.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = TutsvillePrimary,
                    unfocusedBorderColor = TutsvilleOutline
                  ),
                  modifier = Modifier.fillMaxWidth().testTag("password_input")
                )
              }

              Spacer(modifier = Modifier.height(2.dp))

              // Primary Pill Action Button: Sign In [→] / Sign Up [→]
              Button(
                onClick = {
                  if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter both email and password."
                    return@Button
                  }
                  if (isCreateAccountMode && fullName.isBlank()) {
                    errorMessage = "Please enter your full name."
                    return@Button
                  }
                  isLoading = true
                  errorMessage = null
                  infoMessage = null

                  coroutineScope.launch {
                    try {
                      if (isCreateAccountMode) {
                        val authResult = Firebase.auth.createUserWithEmailAndPassword(email.trim(), password).await()
                        val user = authResult.user
                        if (user != null && fullName.isNotBlank()) {
                          val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName.trim())
                            .build()
                          user.updateProfile(profileUpdates).await()
                        }
                        isLoading = false
                        onAuthSuccess()
                      } else {
                        Firebase.auth.signInWithEmailAndPassword(email.trim(), password).await()
                        isLoading = false
                        onAuthSuccess()
                      }
                    } catch (e: Exception) {
                      isLoading = false
                      Log.e("Auth", "Email auth failed", e)
                      val msg = e.localizedMessage ?: "Authentication failed"
                      errorMessage = when {
                        msg.contains("The email address is already in use", ignoreCase = true) ->
                          "This email is already registered. Please sign in instead."
                        msg.contains("The email address is badly formatted", ignoreCase = true) ->
                          "Please enter a valid email address."
                        msg.contains("Password should be at least", ignoreCase = true) ->
                          "Password must be at least 6 characters."
                        msg.contains("no user record", ignoreCase = true) || msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                          "Invalid email or password. Please check your credentials or create an account."
                        else -> msg
                      }
                    }
                  }
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = GoldAccent,
                  contentColor = Color.White
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("email_auth_submit_button")
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                  )
                } else {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Text(
                      text = if (isCreateAccountMode) "Sign Up" else "Sign In",
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Login,
                      contentDescription = null,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }

              // Links: Don't have an account? Sign Up & Forgot Password
              Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                  modifier = Modifier.padding(top = 2.dp)
                ) {
                  Text(
                    text = if (isCreateAccountMode) "Already have an account? " else "Don't have an account? ",
                    style = MaterialTheme.typography.bodySmall.copy(color = TutsvilleOnSurfaceVariant)
                  )
                  Text(
                    text = if (isCreateAccountMode) "Sign In" else "Sign Up",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = TutsvillePrimary,
                      fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                      .clickable {
                        isCreateAccountMode = !isCreateAccountMode
                        errorMessage = null
                        infoMessage = null
                      }
                      .testTag("toggle_create_account_button")
                  )
                }

                if (!isCreateAccountMode) {
                  Text(
                    text = "Forgot Password",
                    style = MaterialTheme.typography.bodySmall.copy(
                      color = TutsvillePrimary,
                      fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                      .clickable {
                        resetEmail = email.trim()
                        showForgotPasswordDialog = true
                      }
                      .padding(vertical = 4.dp)
                      .testTag("forgot_password_button")
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Real Official Google Sign-In Capsule Button (matching user screenshot)
            Surface(
              shape = RoundedCornerShape(percent = 50),
              color = Color.White,
              border = BorderStroke(1.dp, Color(0xFFD2D5DA)),
              shadowElevation = 1.dp,
              modifier = Modifier
                .width(108.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(percent = 50))
                .clickable {
                  isLoading = true
                  errorMessage = null
                  infoMessage = null
                  signInWithGoogle(
                    context = context,
                    credentialManager = credentialManager,
                    scope = coroutineScope,
                    onSuccess = {
                      isLoading = false
                      onAuthSuccess()
                    },
                    onError = { err ->
                      isLoading = false
                      errorMessage = err
                    },
                    onCancelled = { notice ->
                      isLoading = false
                      infoMessage = notice
                    }
                  )
                }
                .testTag("google_capsule_button")
            ) {
              Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color(0xFF4285F4),
                    strokeWidth = 2.dp
                  )
                } else {
                  Image(
                    painter = painterResource(id = R.drawable.ic_google_logo),
                    contentDescription = "Sign in with Google",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(26.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Resident Google Sign-In Onboarding Guidance Card
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = GoldAccent.copy(alpha = 0.08f),
              border = BorderStroke(0.8.dp, GoldAccent.copy(alpha = 0.4f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("auth_resident_onboarding_notice")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = GoldAccent,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Estate Residents: Sign in with your registered Google account to automatically link your residence keys and concierge pass.",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = TutsvillePrimary,
                    lineHeight = 14.sp
                  )
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                text = "Secured with Firebase Firestore Enterprise",
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


  // Forgot Password Dialog
  if (showForgotPasswordDialog) {
    var dialogEmail by remember { mutableStateOf(resetEmail) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    var dialogSuccess by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showForgotPasswordDialog = false },
      title = {
        Text("Reset Password", fontWeight = FontWeight.Bold, color = TutsvillePrimary)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Enter your registered email address to receive password reset instructions.",
            style = MaterialTheme.typography.bodySmall
          )
          OutlinedTextField(
            value = dialogEmail,
            onValueChange = { dialogEmail = it },
            label = { Text("Email Address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          if (dialogError != null) {
            Text(text = dialogError ?: "", color = StatusUrgentText, fontSize = 12.sp)
          }
          if (dialogSuccess != null) {
            Text(text = dialogSuccess ?: "", color = StatusCompletedText, fontSize = 12.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (dialogEmail.isBlank()) {
              dialogError = "Please enter an email address."
              return@Button
            }
            isSendingReset = true
            dialogError = null
            coroutineScope.launch {
              try {
                Firebase.auth.sendPasswordResetEmail(dialogEmail.trim()).await()
                isSendingReset = false
                dialogSuccess = "Password reset email sent. Please check your inbox."
              } catch (e: Exception) {
                isSendingReset = false
                dialogError = e.localizedMessage ?: "Failed to send reset email."
              }
            }
          },
          enabled = !isSendingReset,
          colors = ButtonDefaults.buttonColors(containerColor = TutsvillePrimary)
        ) {
          Text("Send Reset Link")
        }
      },
      dismissButton = {
        TextButton(onClick = { showForgotPasswordDialog = false }) {
          Text("Close", color = TutsvilleOnSurfaceVariant)
        }
      }
    )
  }
}

fun signInWithGoogle(
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
  onCancelled: (String?) -> Unit
) {
  val activity = context.findActivity()
  if (activity == null) {
    onError("Unable to find host Activity for Google Sign-In")
    return
  }

  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onError("Google Sign-In configuration missing: default_web_client_id not found")
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
        onSuccess()
      } else {
        onError("Unexpected credential type: ${credential::class.java.simpleName}")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
      onCancelled("Sign-in was dismissed. Tap below to retry.")
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      val msg = e.localizedMessage ?: "Sign in failed"
      val friendlyMessage = when {
        e.javaClass.simpleName.contains("NoCredential", ignoreCase = true) || msg.contains("No credentials", ignoreCase = true) ->
          "No Google Account found on this device. You can sign in with your email and password above."
        msg.contains("16", ignoreCase = true) ->
          "Google Sign-In was dismissed or no account was selected. You can sign in with email above."
        msg.contains("Network", ignoreCase = true) ->
          "Network connection error during sign-in. Please check your connection."
        else -> msg
      }
      onError(friendlyMessage)
    }
  }
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
