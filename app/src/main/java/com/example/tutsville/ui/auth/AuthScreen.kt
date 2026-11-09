package com.example.tutsville.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var infoMessage by remember { mutableStateOf<String?>(null) }

  // Email / Password Form States
  var isCreateAccountMode by remember { mutableStateOf(false) }
  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }
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
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minContentHeight = maxHeight - 36.dp

        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp)
            .defaultMinSize(minHeight = minContentHeight),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // 1. Top Section: Grand Logo Hero Card (preserved design, elevated at the top)
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
              .height(180.dp)
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

          // Generous Luxury Gap: dynamically expands between top logo and bottom auth controls
          Spacer(modifier = Modifier.height(20.dp))

          // 2. Bottom Section: All authentication controls anchored cleanly to the bottom
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Operations Stewardship Portal Title
            Text(
              text = "Operations Stewardship Portal",
              style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = TutsvillePrimary,
                letterSpacing = 0.5.sp
              ),
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

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

            // Google Sign-In Button
            Button(
              onClick = {
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
              },
              enabled = !isLoading,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = TutsvillePrimary,
                contentColor = Color.White
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("google_sign_in_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = Color.White,
                  strokeWidth = 2.dp
                )
              } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = GoldAccent,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Divider: or continue with email
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              HorizontalDivider(modifier = Modifier.weight(1f), color = TutsvilleOutline)
              Text(
                text = " or continue with email ",
                style = MaterialTheme.typography.labelSmall.copy(color = TutsvilleOnSurfaceVariant, fontSize = 11.sp),
                modifier = Modifier.padding(horizontal = 8.dp)
              )
              HorizontalDivider(modifier = Modifier.weight(1f), color = TutsvilleOutline)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Email / Password Form
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (isCreateAccountMode) {
                OutlinedTextField(
                  value = fullName,
                  onValueChange = { fullName = it },
                  label = { Text("Full Name") },
                  placeholder = { Text("e.g. Eleanor Vance") },
                  leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TutsvillePrimary)
                  },
                  singleLine = true,
                  shape = RoundedCornerShape(12.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = TutsvillePrimary,
                    unfocusedBorderColor = TutsvilleOutline
                  ),
                  modifier = Modifier.fillMaxWidth().testTag("fullname_input")
                )
              }

              OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                placeholder = { Text("e.g. director@tutsville.com") },
                leadingIcon = {
                  Icon(Icons.Default.Email, contentDescription = null, tint = TutsvillePrimary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.White,
                  unfocusedContainerColor = Color.White,
                  focusedBorderColor = TutsvillePrimary,
                  unfocusedBorderColor = TutsvilleOutline
                ),
                modifier = Modifier.fillMaxWidth().testTag("email_input")
              )

              OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                placeholder = { Text("At least 6 characters") },
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
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.White,
                  unfocusedContainerColor = Color.White,
                  focusedBorderColor = TutsvillePrimary,
                  unfocusedBorderColor = TutsvilleOutline
                ),
                modifier = Modifier.fillMaxWidth().testTag("password_input")
              )

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
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = TutsvilleSecondary,
                  contentColor = Color.White
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(46.dp)
                  .testTag("email_auth_submit_button")
              ) {
                Text(
                  text = if (isCreateAccountMode) "Create Estate Account" else "Sign In with Email",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }

              // Toggle Mode Button
              TextButton(
                onClick = {
                  isCreateAccountMode = !isCreateAccountMode
                  errorMessage = null
                  infoMessage = null
                },
                modifier = Modifier
                  .align(Alignment.CenterHorizontally)
                  .testTag("toggle_create_account_button")
              ) {
                Text(
                  text = if (isCreateAccountMode)
                    "Already have an account? Sign In"
                  else
                    "Don't have an account? Create one",
                  style = MaterialTheme.typography.bodySmall.copy(
                    color = TutsvillePrimary,
                    fontWeight = FontWeight.SemiBold
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
          "No Google Account found on this device. You can create an email account or sign into Google in Settings."
        msg.contains("16", ignoreCase = true) ->
          "Google Sign-In was dismissed or no account was selected. You can also sign in with email."
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
