package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.AccountType
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import com.example.util.ReceiptImageHelper

@Composable
fun NeumorphicCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 22.dp,
  shadowElevation: Dp = 6.dp,
  containerColor: Color? = null,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val baseModifier = modifier
    .neumorphicRaised(
      cornerRadius = cornerRadius,
      shadowElevation = shadowElevation,
      containerColor = containerColor,
    )
    .clip(RoundedCornerShape(cornerRadius))
    .let { mod ->
      if (onClick != null) mod.clickable(onClick = onClick) else mod
    }

  Column(
    modifier = baseModifier,
    content = content,
  )
}

@Composable
fun NeumorphicProgressBar(
  progress: Float,
  modifier: Modifier = Modifier,
  height: Dp = 10.dp,
  fillColors: List<Color> = HisabTheme.neumorphic.ringGradient,
) {
  val clamped = progress.coerceIn(0f, 1f)
  val radius = height / 2
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .neumorphicInset(cornerRadius = radius)
      .clip(RoundedCornerShape(radius)),
  ) {
    if (clamped > 0f) {
      Box(
        modifier = Modifier
          .fillMaxWidth(clamped)
          .fillMaxHeight()
          .clip(RoundedCornerShape(radius))
          .background(Brush.horizontalGradient(fillColors)),
      )
    }
  }
}

@Composable
fun AccountSwitcherPill(
  activeAccount: AccountType,
  onSelectAccount: (AccountType) -> Unit,
  modifier: Modifier = Modifier,
) {
  val neo = HisabTheme.neumorphic
  Box(
    modifier = modifier
      .fillMaxWidth()
      .neumorphicInset(cornerRadius = 18.dp)
      .padding(5.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      AccountType.entries.forEach { account ->
        val selected = account == activeAccount
        val contentColor by animateColorAsState(
          targetValue =
            if (selected) Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant,
          label = "account_pill_fg",
        )
        val itemModifier = Modifier
          .weight(1f)
          .let { mod ->
            if (selected) {
              mod
                .neumorphicRaised(
                  cornerRadius = 14.dp,
                  shadowElevation = 4.dp,
                  drawContainer = false,
                )
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(neo.heroGradient))
            } else {
              mod.clip(RoundedCornerShape(14.dp))
            }
          }
          .clickable { onSelectAccount(account) }
          .padding(vertical = 11.dp, horizontal = 8.dp)
          .testTag("account_switch_${account.value}")

        Row(
          modifier = itemModifier,
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector =
              if (account == AccountType.PERSONAL) Icons.Rounded.Person
              else Icons.Rounded.BusinessCenter,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = account.labelBn,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun ConfirmDeleteDialog(
  title: String,
  message: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Rounded.DeleteForever,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(30.dp),
      )
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
      )
    },
    text = {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = Modifier.testTag("confirm_delete_button"),
      ) {
        Text("হ্যাঁ, মুছে ফেলুন")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("বাতিল")
      }
    },
  )
}

@Composable
fun EmptyStateCard(
  title: String,
  subtitle: String,
  modifier: Modifier = Modifier,
) {
  NeumorphicCard(
    modifier = modifier.fillMaxWidth(),
    cornerRadius = 22.dp,
    shadowElevation = 6.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_empty_ledger),
        contentDescription = title,
        modifier = Modifier
          .size(width = 160.dp, height = 120.dp)
          .clip(RoundedCornerShape(16.dp)),
        contentScale = ContentScale.Crop,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
fun ReceiptPreviewDialog(
  receiptBase64: String,
  onDismiss: () -> Unit,
) {
  val bitmap = remember(receiptBase64) { ReceiptImageHelper.decodeBase64ToImageBitmap(receiptBase64) }
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "রসিদের ছবি",
            style = MaterialTheme.typography.titleMedium,
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = "বন্ধ করুন")
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = "রসিদের ছবি",
            modifier = Modifier
              .fillMaxWidth()
              .height(340.dp)
              .clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Fit,
          )
        } else {
          Text(
            text = "ছবিটি প্রদর্শন করা যাচ্ছে না",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}
