package com.example.tutsville.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
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
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(TutsvilleBackground)
      .windowInsetsPadding(WindowInsets.safeDrawing)
      .padding(horizontal = 20.dp, vertical = 20.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 480.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Grand Logo Hero Card taking the majority of the top half of the screen
      ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
          containerColor = Color(0xFF000000)
        ),
        elevation = CardDefaults.elevatedCardElevation(
          defaultElevation = 8.dp
        ),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1.3f)
          .border(1.5.dp, GoldAccent.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
          .testTag("auth_hero_logo_card")
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.tutsville_estate_logo_trimmed),
            contentDescription = "Tutsville Estate",
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxSize(0.92f)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 2. Bottom Action Container: Title placed closely above the sign-in button
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f, fill = false),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // One-line 3-word statement close to the signing button
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

        Spacer(modifier = Modifier.height(14.dp))

        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = StatusUrgentBg,
            border = BorderStroke(1.dp, Color(0xFFF0B3B3)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
          ) {
            Text(
              text = errorMessage ?: "",
              style = MaterialTheme.typography.bodySmall.copy(color = StatusUrgentText),
              modifier = Modifier.padding(10.dp),
              textAlign = TextAlign.Center
            )
          }
        }

        // Google Sign-In Button
        Button(
          onClick = {
            isLoading = true
            errorMessage = null
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
              onCancelled = {
                isLoading = false
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
            .height(52.dp)
            .testTag("google_sign_in_button")
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = Color.White,
              strokeWidth = 2.dp
            )
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 8.dp)
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

fun signInWithGoogle(
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
  onCancelled: () -> Unit
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onSuccess()
      } else {
        onError("Unexpected credential type returned")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
      onCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onError(e.localizedMessage ?: "Sign in failed")
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
