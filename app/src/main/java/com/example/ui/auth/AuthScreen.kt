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
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.neumorphicCircle
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  onContinueAsGuest: () -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

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
            text = "গুগল ছাড়াই প্রবেশ করুন (অফলাইন)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "অফলাইন মোডে সব হিসাব শুধু এই ফোনেই সংরক্ষিত থাকবে। পরে চাইলে সেটিংস থেকে Google অ্যাকাউন্ট যুক্ত করতে পারবেন।",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 8.dp),
        )
      }
    }
  }
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
