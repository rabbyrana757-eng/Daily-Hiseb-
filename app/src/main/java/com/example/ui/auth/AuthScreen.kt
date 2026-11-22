package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.local.AppPreferences
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.neumorphicCircle
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  onContinueAsGuest: () -> Unit,
  preferences: AppPreferences? = null,
  onCustomLoginSuccess: (email: String, name: String) -> Unit = { _, _ -> },
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }
  var showEmailAuthDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = coroutineScope,
    )
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.Center,
    ) {
      val neo = HisabTheme.neumorphic
      Column(
        modifier = Modifier
          .widthIn(max = 520.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier = Modifier
            .size(68.dp)
            .neumorphicCircle(shadowElevation = 6.dp, containerColor = neo.heroGradient.first())
            .clip(CircleShape)
            .background(Brush.linearGradient(neo.heroGradient)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.AccountBalanceWallet,
            contentDescription = "My Daily হিসাব লোগো",
            tint = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.size(32.dp),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "My Daily হিসাব",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "দৈনিক আয়, খরচ, বাজেট ও সঞ্চয়ের নির্ভরযোগ্য ডিজিটাল খাতা",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .neumorphicRaised(cornerRadius = 24.dp, shadowElevation = 6.dp)
            .padding(6.dp),
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_auth_hero),
            contentDescription = "আর্থিক হিসাব ও বিশ্লেষণ ব্যানার",
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop,
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        NeumorphicCard(
          cornerRadius = 22.dp,
          shadowElevation = 6.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            FeatureHighlightRow(
              icon = Icons.Rounded.BarChart,
              title = "ব্যক্তিগত ও ব্যবসায়িক হিসাব",
              subtitle = "আলাদা অ্যাকাউন্টে দৈনিক ও মাসিক রিপোর্ট",
            )
            FeatureHighlightRow(
              icon = Icons.Rounded.Savings,
              title = "বাজেট সতর্কতা ও সঞ্চয় লক্ষ্য",
              subtitle = "৮০% ও ১০০% খরচে তাৎক্ষণিক বাজেট সতর্কতা",
            )
            FeatureHighlightRow(
              icon = Icons.Rounded.CloudDone,
              title = "ক্লাউড সিঙ্ক ও PDF/Excel এক্সপোর্ট",
              subtitle = "সব ডিভাইসে স্বয়ংক্রিয় ব্যাকআপ ও নিরাপদ সংরক্ষণ",
            )
          }
        }

        if (errorMsg != null) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = errorMsg!!,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp),
              textAlign = TextAlign.Center,
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Google Sign-In
        Button(
          onClick = {
            errorMsg = null
            isLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMsg = msg
              },
              scope = coroutineScope,
              onAuthCancelled = { isLoading = false },
            )
          },
          enabled = !isLoading,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("google_sign_in_button"),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.5.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.Login,
              contentDescription = null,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Google দিয়ে সাইন-ইন করুন",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Gmail / Email & Password Login Option
        Button(
          onClick = {
            errorMsg = null
            showEmailAuthDialog = true
          },
          enabled = !isLoading,
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("email_password_sign_in_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Email,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Gmail ও পাসওয়ার্ড দিয়ে প্রবেশ",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Continue without Account (Guest / Offline)
        OutlinedButton(
          onClick = {
            errorMsg = null
            onContinueAsGuest()
          },
          enabled = !isLoading,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("guest_mode_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.PersonOutline,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "গেস্ট হিসেবে প্রবেশ করুন (অফলাইন)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "অফলাইন বা Gmail অ্যাকাউন্টে আপনার হিসাব নিরাপদে থাকবে। পরে চাইলে সেটিংস থেকে সরাসরি Google ক্লাউড সিঙ্ক করতে পারবেন।",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 8.dp),
        )
      }
    }
  }

  if (showEmailAuthDialog && preferences != null) {
    EmailPasswordAuthDialog(
      preferences = preferences,
      onDismiss = { showEmailAuthDialog = false },
      onSuccess = { email, name ->
        showEmailAuthDialog = false
        onCustomLoginSuccess(email, name)
      },
    )
  }
}

@Composable
private fun EmailPasswordAuthDialog(
  preferences: AppPreferences,
  onDismiss: () -> Unit,
  onSuccess: (email: String, name: String) -> Unit,
) {
  var selectedTab by remember { mutableStateOf(0) } // 0 = Login, 1 = Register
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var name by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var dialogError by remember { mutableStateOf<String?>(null) }
  var isProcessing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (selectedTab == 0) "Gmail ও পাসওয়ার্ড লগইন" else "নতুন অ্যাকাউন্ট তৈরি",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        TabRow(selectedTabIndex = selectedTab) {
          Tab(
            selected = selectedTab == 0,
            onClick = {
              selectedTab = 0
              dialogError = null
            },
            text = { Text("লগইন") },
          )
          Tab(
            selected = selectedTab == 1,
            onClick = {
              selectedTab = 1
              dialogError = null
            },
            text = { Text("নতুন অ্যাকাউন্ট") },
          )
        }

        if (selectedTab == 1) {
          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              dialogError = null
            },
            label = { Text("আপনার নাম / প্রতিষ্ঠানের নাম") },
            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom_user_name_input"),
          )
        }

        OutlinedTextField(
          value = email,
          onValueChange = {
            email = it
            dialogError = null
          },
          label = { Text("Gmail / ইমেইল অ্যাড্রেস") },
          leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("custom_user_email_input"),
        )

        OutlinedTextField(
          value = password,
          onValueChange = {
            password = it
            dialogError = null
          },
          label = { Text("পাসওয়ার্ড") },
          leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
          trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
              Icon(
                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = null,
              )
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("custom_user_password_input"),
        )

        if (selectedTab == 1) {
          OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
              confirmPassword = it
              dialogError = null
            },
            label = { Text("পাসওয়ার্ড নিশ্চিত করুন") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom_user_confirm_password_input"),
          )
        }

        if (dialogError != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = dialogError!!,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(8.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanEmail = email.trim()
          val cleanPassword = password.trim()
          if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            dialogError = "দয়া করে সঠিক Gmail বা ইমেইল অ্যাড্রেস লিখুন।"
            return@Button
          }
          if (cleanPassword.length < 4) {
            dialogError = "পাসওয়ার্ড অন্তত ৪ সংখ্যার বা অক্ষরের হতে হবে।"
            return@Button
          }

          if (selectedTab == 0) {
            // Login
            isProcessing = true
            scope.launch {
              val valid = preferences.verifyCustomUser(cleanEmail, cleanPassword)
              isProcessing = false
              if (valid) {
                val displayName = name.ifBlank { cleanEmail.substringBefore("@") }
                onSuccess(cleanEmail, displayName)
              } else {
                dialogError = "ইমেইল অথবা পাসওয়ার্ড মেলেনি! নতুন ব্যবহারকারী হলে 'নতুন অ্যাকাউন্ট' ট্যাবে গিয়ে তৈরি করুন।"
              }
            }
          } else {
            // Register
            val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@") }
            if (cleanPassword != confirmPassword.trim()) {
              dialogError = "উভয় পাসওয়ার্ড মেলেনি! চেক করে পুনরায় লিখুন।"
              return@Button
            }
            isProcessing = true
            scope.launch {
              preferences.saveCustomUserAccount(cleanEmail, cleanName, cleanPassword)
              isProcessing = false
              onSuccess(cleanEmail, cleanName)
            }
          }
        },
        enabled = !isProcessing,
        modifier = Modifier.testTag("submit_email_auth_button"),
      ) {
        if (isProcessing) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
          )
        } else {
          Text(if (selectedTab == 0) "প্রবেশ করুন" else "নিবন্ধন সম্পন্ন করুন")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("বাতিল")
      }
    },
  )
}

@Composable
private fun FeatureHighlightRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier = Modifier
        .size(44.dp)
        .neumorphicInset(cornerRadius = 14.dp),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(22.dp),
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
