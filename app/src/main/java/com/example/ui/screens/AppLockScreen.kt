package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HisabTheme
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.neumorphicCircle
import com.example.ui.theme.neumorphicInset
import com.example.ui.theme.neumorphicRaised
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class NeoLockStage {
  INPUT,
  VERIFYING,
  VERIFIED_BOXES_GLOW,
  VERIFIED_CHECK_MORPH,
  UNLOCKED_CARD,
}

/**
 * App-Themed Neomorphic (Soft UI) Security Lock Screen
 *
 * Matches My Daily হিসাব's Light & Dark Soft UI Neomorphic design system while providing
 * an interactive, multi-stage verification animation:
 * - Raised Neomorphic Soft UI Vault Card with subtle hero aura
 * - Orbiting Gradient Radar Ring around the top Lock Orb + shockwave pulse on unlock
 * - 4 Recessed PIN Wells + Connecting Animated Energy Beam that sweeps sequentially (1 -> 2 -> 3 -> 4)
 * - Sequential checking glow -> Unified Emerald Success Glow -> Morph into Checkmark -> Smooth App Entry
 * - Tactile Neomorphic Circular Keypad (depresses from raised to inset on press with accent glow)
 */
@Composable
fun AppLockScreen(
  onVerifyPin: suspend (String) -> Boolean,
  onUnlocked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var enteredPin by remember { mutableStateOf("") }
  var revealIndex by remember { mutableStateOf(-1) }
  var checkingIndex by remember { mutableStateOf(-1) }
  var stage by remember { mutableStateOf(NeoLockStage.INPUT) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isExitingToApp by remember { mutableStateOf(false) }

  val shakeOffset = remember { Animatable(0f) }
  val shockwaveProgress = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current
  val neo = HisabTheme.neumorphic

  // Smooth fade & scale exit transition into main app
  val exitAlpha by animateFloatAsState(
    targetValue = if (isExitingToApp) 0f else 1f,
    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
    label = "unlock_exit_alpha",
  )
  val exitScale by animateFloatAsState(
    targetValue = if (isExitingToApp) 1.04f else 1f,
    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
    label = "unlock_exit_scale",
  )

  // Brief digit flash before masking to a glowing dot
  LaunchedEffect(enteredPin.length) {
    if (enteredPin.isNotEmpty() && stage == NeoLockStage.INPUT) {
      val currentIdx = enteredPin.lastIndex
      revealIndex = currentIdx
      delay(200)
      if (revealIndex == currentIdx) {
        revealIndex = -1
      }
    } else {
      revealIndex = -1
    }
  }

  fun onDigitClick(digit: String) {
    if (stage != NeoLockStage.INPUT || enteredPin.length >= 4) return
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    errorMessage = null
    val nextPin = enteredPin + digit
    enteredPin = nextPin

    if (nextPin.length == 4) {
      stage = NeoLockStage.VERIFYING
      scope.launch {
        // Sequential checking beam sweeping smoothly across all 4 PIN wells (0 -> 1 -> 2 -> 3)
        for (idx in 0..3) {
          checkingIndex = idx
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          delay(150)
        }
        checkingIndex = 4
        delay(110)

        val valid = onVerifyPin(nextPin)
        checkingIndex = -1
        if (valid) {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          // Step 1: All 4 wells turn emerald green + trigger expanding shockwave pulse
          stage = NeoLockStage.VERIFIED_BOXES_GLOW
          launch {
            shockwaveProgress.snapTo(0f)
            shockwaveProgress.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
          }
          delay(340)
          // Step 2: 4 wells collapse/morph into a central Neomorphic Emerald Checkmark Orb
          stage = NeoLockStage.VERIFIED_CHECK_MORPH
          delay(420)
          // Step 3: Card reveals Success confirmation & smoothly enters the app
          stage = NeoLockStage.UNLOCKED_CARD
          delay(500)
          isExitingToApp = true
          delay(250)
          onUnlocked()
        } else {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          stage = NeoLockStage.INPUT
          errorMessage = "ভুল পিন! আবার সঠিক পিন দিন"
          shakeOffset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
              durationMillis = 420
              0f at 0
              -20f at 45
              20f at 95
              -15f at 145
              15f at 195
              -9f at 245
              9f at 295
              -4f at 350
              0f at 420
            },
          )
          delay(220)
          enteredPin = ""
        }
      }
    }
  }

  fun onBackspaceClick() {
    if (stage != NeoLockStage.INPUT || enteredPin.isEmpty()) return
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    errorMessage = null
    enteredPin = enteredPin.dropLast(1)
  }

  fun onClearClick() {
    if (stage != NeoLockStage.INPUT || enteredPin.isEmpty()) return
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    errorMessage = null
    enteredPin = ""
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center,
  ) {
    // Subtle App-Themed Radial Ambient Aura (Blue -> Violet soft wash)
    Box(
      modifier = Modifier
        .size(460.dp)
        .align(Alignment.TopCenter)
        .background(
          Brush.radialGradient(
            colors = listOf(
              neo.heroGradient.first().copy(alpha = 0.14f),
              neo.heroGradient.last().copy(alpha = 0.06f),
              Color.Transparent,
            ),
          ),
          shape = CircleShape,
        ),
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp)
        .graphicsLayer {
          alpha = exitAlpha
          scaleX = exitScale
          scaleY = exitScale
        },
      contentAlignment = Alignment.Center,
    ) {
      // Main Raised Neomorphic Vault Card
      Box(
        modifier = Modifier
          .widthIn(max = 410.dp)
          .fillMaxWidth()
          .neumorphicRaised(cornerRadius = 32.dp, shadowElevation = 8.dp)
          .clip(RoundedCornerShape(32.dp)),
      ) {
        AnimatedContent(
          targetState = stage == NeoLockStage.UNLOCKED_CARD,
          transitionSpec = {
            (fadeIn(tween(340)) + scaleIn(initialScale = 0.94f, animationSpec = tween(340)))
              .togetherWith(fadeOut(tween(240)) + scaleOut(targetScale = 0.96f, animationSpec = tween(240)))
          },
          label = "neo_card_unlock_transition",
        ) { isUnlockedCard ->
          if (isUnlockedCard) {
            NeoUnlockedSuccessContent()
          } else {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              // Top Brand Mini Tag
              Row(
                modifier = Modifier
                  .neumorphicInset(cornerRadius = 20.dp)
                  .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.AccountBalanceWallet,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = "My Daily হিসাব • সিকিউরিটি লক",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Orbiting Radar-Ring Lock Orb + Shockwave Pulse
              NeomorphicRadarLockOrb(
                stage = stage,
                shockwaveProgress = shockwaveProgress.value,
              )

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = "পিন কোড দিয়ে প্রবেশ করুন",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = "আপনার ব্যক্তিগত ও ব্যবসায়িক হিসাব সুরক্ষিত আছে",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )

              Spacer(modifier = Modifier.height(18.dp))

              // 4 Recessed Neomorphic PIN Wells + Connecting Energy Beam
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(82.dp),
                contentAlignment = Alignment.Center,
              ) {
                AnimatedContent(
                  targetState = stage == NeoLockStage.VERIFIED_CHECK_MORPH,
                  transitionSpec = {
                    (scaleIn(
                      initialScale = 0.55f,
                      animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                      ),
                    ) + fadeIn(tween(240))).togetherWith(
                      scaleOut(targetScale = 0.45f, animationSpec = tween(200)) + fadeOut(tween(180)),
                    )
                  },
                  label = "neo_pin_morph",
                ) { showCheckMorph ->
                  if (showCheckMorph) {
                    NeoEmeraldCheckMorphOrb()
                  } else {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      modifier = Modifier.graphicsLayer { translationX = shakeOffset.value },
                    ) {
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        repeat(4) { index ->
                          val isFilled = index < enteredPin.length
                          val isActive = stage == NeoLockStage.INPUT && index == enteredPin.length
                          val isCheckingNow = stage == NeoLockStage.VERIFYING && checkingIndex == index
                          val isAlreadyChecked = stage == NeoLockStage.VERIFYING && checkingIndex > index
                          val showDigitChar =
                            isFilled &&
                              (index == revealIndex || isCheckingNow || stage == NeoLockStage.VERIFIED_BOXES_GLOW)
                          val digitText = if (isFilled) enteredPin[index].toString() else ""

                          NeoPinWell(
                            digit = digitText,
                            isFilled = isFilled,
                            isActive = isActive,
                            showDigit = showDigitChar,
                            isCheckingNow = isCheckingNow,
                            isAlreadyChecked = isAlreadyChecked,
                            isVerifiedGlow = stage == NeoLockStage.VERIFIED_BOXES_GLOW,
                            isError = errorMessage != null,
                          )
                        }
                      }

                      Spacer(modifier = Modifier.height(10.dp))

                      // Unique Connecting Verification Energy Beam Rail
                      VerificationBeamProgressRail(
                        stage = stage,
                        enteredLength = enteredPin.length,
                        checkingIndex = checkingIndex,
                        isError = errorMessage != null,
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Recessed / Elevated Status Pill
              NeoVerificationStatusPill(
                stage = stage,
                enteredLength = enteredPin.length,
                checkingIndex = checkingIndex,
                errorMessage = errorMessage,
              )

              Spacer(modifier = Modifier.height(18.dp))

              // Soft UI Neomorphic Circular Keypad (0-9, C, ⌫)
              NeoCircularKeypad(
                enabled = stage == NeoLockStage.INPUT,
                onDigitClick = ::onDigitClick,
                onClearClick = ::onClearClick,
                onBackspaceClick = ::onBackspaceClick,
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Top Neomorphic Lock Orb with an Orbiting Gradient Radar Ring during verification
 * and an expanding emerald shockwave ring upon unlock!
 */
@Composable
private fun NeomorphicRadarLockOrb(
  stage: NeoLockStage,
  shockwaveProgress: Float,
) {
  val neo = HisabTheme.neumorphic
  val isVerifying = stage == NeoLockStage.VERIFYING
  val isVerified =
    stage == NeoLockStage.VERIFIED_BOXES_GLOW ||
      stage == NeoLockStage.VERIFIED_CHECK_MORPH ||
      stage == NeoLockStage.UNLOCKED_CARD

  val infiniteTransition = rememberInfiniteTransition(label = "radar_orbit")
  val orbitAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 950, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "orbit_angle",
  )

  val orbScale by animateFloatAsState(
    targetValue = when {
      isVerified -> 1.12f
      isVerifying -> 1.06f
      else -> 1.0f
    },
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "orb_scale",
  )

  val activeColors = if (isVerified) {
    listOf(Color(0xFF10B981), IncomeGreen, Color(0xFF047857))
  } else {
    neo.heroGradient
  }

  Box(
    modifier = Modifier.size(84.dp),
    contentAlignment = Alignment.Center,
  ) {
    // Expanding Shockwave & Orbiting Radar Ring Canvas
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val ringRadius = size.minDimension / 2f - 4.dp.toPx()

      if (isVerifying) {
        drawArc(
          brush = Brush.sweepGradient(neo.ringGradient + neo.ringGradient.first()),
          startAngle = orbitAngle,
          sweepAngle = 220f,
          useCenter = false,
          topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
          size = Size(ringRadius * 2f, ringRadius * 2f),
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
      } else if (isVerified) {
        drawCircle(
          color = IncomeGreen.copy(alpha = 0.65f),
          radius = ringRadius,
          center = center,
          style = Stroke(width = 2.5.dp.toPx()),
        )
      }

      if (shockwaveProgress in 0.01f..0.99f) {
        val waveRadius = ringRadius + (18.dp.toPx() * shockwaveProgress)
        drawCircle(
          color = IncomeGreen.copy(alpha = (1f - shockwaveProgress) * 0.7f),
          radius = waveRadius,
          center = center,
          style = Stroke(width = 3.dp.toPx() * (1f - shockwaveProgress)),
        )
      }
    }

    // Center Raised Neomorphic Orb
    Box(
      modifier = Modifier
        .size(62.dp)
        .graphicsLayer {
          scaleX = orbScale
          scaleY = orbScale
        }
        .neumorphicCircle(
          shadowElevation = 6.dp,
          containerColor = activeColors.first(),
        )
        .clip(CircleShape)
        .background(Brush.linearGradient(activeColors)),
      contentAlignment = Alignment.Center,
    ) {
      AnimatedContent(
        targetState = isVerified,
        transitionSpec = {
          (scaleIn(initialScale = 0.6f) + fadeIn()).togetherWith(scaleOut() + fadeOut())
        },
        label = "orb_icon_swap",
      ) { verified ->
        Icon(
          imageVector = when {
            verified -> Icons.Rounded.Check
            isVerifying -> Icons.Rounded.LockOpen
            else -> Icons.Rounded.Lock
          },
          contentDescription = if (verified) "পিন সঠিক" else "অ্যাপ লক",
          tint = Color.White,
          modifier = Modifier.size(28.dp),
        )
      }
    }
  }
}

/**
 * Recessed Neomorphic PIN Well that elevates and glows when scanned or verified
 */
@Composable
private fun NeoPinWell(
  digit: String,
  isFilled: Boolean,
  isActive: Boolean,
  showDigit: Boolean,
  isCheckingNow: Boolean,
  isAlreadyChecked: Boolean,
  isVerifiedGlow: Boolean,
  isError: Boolean,
) {
  val neo = HisabTheme.neumorphic

  val scale by animateFloatAsState(
    targetValue = when {
      isCheckingNow -> 1.13f
      isVerifiedGlow -> 1.07f
      isAlreadyChecked -> 1.04f
      isFilled -> 1.02f
      else -> 1.0f
    },
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "well_scale",
  )

  val borderColor by animateColorAsState(
    targetValue = when {
      isVerifiedGlow -> IncomeGreen
      isCheckingNow -> MaterialTheme.colorScheme.primary
      isAlreadyChecked -> IncomeGreen.copy(alpha = 0.85f)
      isError -> MaterialTheme.colorScheme.error
      isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
      isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
      else -> Color.Transparent
    },
    animationSpec = tween(150),
    label = "well_border",
  )

  val boxModifier =
    if (isCheckingNow || isVerifiedGlow) {
      Modifier
        .neumorphicRaised(
          cornerRadius = 16.dp,
          shadowElevation = 5.dp,
          containerColor = if (isVerifiedGlow) IncomeGreen.copy(alpha = 0.14f) else neo.surface,
        )
    } else {
      Modifier.neumorphicInset(
        cornerRadius = 16.dp,
        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else neo.insetSurface,
      )
    }

  Box(
    modifier = Modifier
      .size(width = 58.dp, height = 64.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .then(boxModifier)
      .clip(RoundedCornerShape(16.dp))
      .then(
        if (borderColor != Color.Transparent) {
          Modifier.border(
            width = if (isCheckingNow || isVerifiedGlow || isError) 1.8.dp else 1.2.dp,
            color = borderColor,
            shape = RoundedCornerShape(16.dp),
          )
        } else {
          Modifier
        },
      ),
    contentAlignment = Alignment.Center,
  ) {
    AnimatedVisibility(
      visible = isFilled,
      enter = scaleIn(
        initialScale = 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      ) + fadeIn(tween(120)),
      exit = scaleOut(targetScale = 0.5f) + fadeOut(tween(120)),
    ) {
      if (showDigit) {
        Text(
          text = digit,
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = when {
            isVerifiedGlow -> IncomeGreen
            isCheckingNow -> MaterialTheme.colorScheme.primary
            isError -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
          },
        )
      } else {
        // Glowing Gradient Security Dot
        val dotColors = when {
          isError -> listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
          isVerifiedGlow || isAlreadyChecked -> listOf(Color(0xFF34D399), IncomeGreen)
          else -> neo.heroGradient
        }
        Box(
          modifier = Modifier
            .size(if (isAlreadyChecked) 16.dp else 14.dp)
            .shadow(
              elevation = 4.dp,
              shape = CircleShape,
              ambientColor = dotColors.first(),
              spotColor = dotColors.first(),
            )
            .clip(CircleShape)
            .background(Brush.linearGradient(dotColors)),
        )
      }
    }
  }
}

/**
 * Unique Connecting Energy Beam Rail underneath the 4 PIN boxes.
 * Smoothly fills during typing and sends a glowing scanner beam across all 4 slots during verification.
 */
@Composable
private fun VerificationBeamProgressRail(
  stage: NeoLockStage,
  enteredLength: Int,
  checkingIndex: Int,
  isError: Boolean,
) {
  val neo = HisabTheme.neumorphic
  val targetProgress = when {
    stage == NeoLockStage.VERIFIED_BOXES_GLOW || stage == NeoLockStage.VERIFIED_CHECK_MORPH -> 1f
    stage == NeoLockStage.VERIFYING -> ((checkingIndex + 1).coerceIn(0, 4)) / 4f
    else -> (enteredLength / 4f) * 0.4f
  }

  val animatedProgress by animateFloatAsState(
    targetValue = targetProgress,
    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
    label = "beam_progress",
  )

  val barBrush = when {
    isError -> Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error))
    stage == NeoLockStage.VERIFIED_BOXES_GLOW -> Brush.horizontalGradient(listOf(Color(0xFF34D399), IncomeGreen))
    else -> Brush.horizontalGradient(neo.heroGradient)
  }

  Box(
    modifier = Modifier
      .width(240.dp)
      .height(5.dp)
      .neumorphicInset(cornerRadius = 4.dp)
      .clip(RoundedCornerShape(4.dp)),
  ) {
    Box(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
        .clip(RoundedCornerShape(4.dp))
        .background(barBrush),
    )
  }
}

/**
 * Central Emerald Checkmark Orb shown during the morph stage
 */
@Composable
private fun NeoEmeraldCheckMorphOrb() {
  Box(
    modifier = Modifier
      .size(66.dp)
      .neumorphicCircle(
        shadowElevation = 7.dp,
        containerColor = IncomeGreen,
      )
      .clip(CircleShape)
      .background(
        Brush.linearGradient(
          colors = listOf(Color(0xFF34D399), IncomeGreen, Color(0xFF047857)),
        ),
      ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.Check,
      contentDescription = "যাচাই সফল",
      tint = Color.White,
      modifier = Modifier.size(34.dp),
    )
  }
}

/**
 * Theme-matched Neomorphic Status Pill ("যাচাই হচ্ছে..." -> "ভেরিফিকেশন সফল")
 */
@Composable
private fun NeoVerificationStatusPill(
  stage: NeoLockStage,
  enteredLength: Int,
  checkingIndex: Int,
  errorMessage: String?,
) {
  val neo = HisabTheme.neumorphic
  val isSuccessPill =
    stage == NeoLockStage.VERIFIED_BOXES_GLOW || stage == NeoLockStage.VERIFIED_CHECK_MORPH
  val isVerifyingPill = stage == NeoLockStage.VERIFYING
  val isErrorPill = errorMessage != null && stage == NeoLockStage.INPUT

  val pillModifier = when {
    isSuccessPill -> Modifier
      .neumorphicRaised(cornerRadius = 16.dp, shadowElevation = 5.dp, containerColor = IncomeGreen)
      .background(Brush.horizontalGradient(listOf(Color(0xFF10B981), IncomeGreen)), RoundedCornerShape(16.dp))
    isVerifyingPill -> Modifier
      .neumorphicRaised(cornerRadius = 16.dp, shadowElevation = 4.dp, containerColor = MaterialTheme.colorScheme.primaryContainer)
    isErrorPill -> Modifier
      .neumorphicInset(cornerRadius = 16.dp, containerColor = MaterialTheme.colorScheme.errorContainer)
    else -> Modifier
      .neumorphicInset(cornerRadius = 16.dp, containerColor = neo.insetSurface)
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp)
      .then(pillModifier)
      .clip(RoundedCornerShape(16.dp)),
    contentAlignment = Alignment.Center,
  ) {
    AnimatedContent(
      targetState = Triple(stage, isErrorPill, checkingIndex to enteredLength),
      transitionSpec = { fadeIn(tween(150)).togetherWith(fadeOut(tween(110))) },
      label = "neo_pill_text",
    ) { (curStage, err, counts) ->
      val (chkIdx, len) = counts
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        when {
          curStage == NeoLockStage.VERIFYING -> {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            val bnDigits = listOf("১", "২", "৩", "৪")
            val stepText = if (chkIdx in 0..3) {
              "কোড যাচাই হচ্ছে (${bnDigits[chkIdx]}/৪)..."
            } else {
              "পিন ভেরিফাই হচ্ছে..."
            }
            Text(
              text = stepText,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          curStage == NeoLockStage.VERIFIED_BOXES_GLOW || curStage == NeoLockStage.VERIFIED_CHECK_MORPH -> {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "ভেরিফিকেশন সফল!",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
          }
          err -> {
            Text(
              text = errorMessage ?: "ভুল পিন!",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
          }
          else -> {
            Icon(
              imageVector = Icons.Rounded.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(15.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            val bnCount = when (len) {
              1 -> "১"
              2 -> "২"
              3 -> "৩"
              4 -> "৪"
              else -> "০"
            }
            Text(
              text = if (len == 0) "৪ সংখ্যার গোপন পিন দিন" else "$bnCount / ৪ সংখ্যা পূর্ণ হয়েছে",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

/**
 * Theme-matched Success Card shown right before smoothly entering the main app
 */
@Composable
private fun NeoUnlockedSuccessContent() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 34.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(72.dp)
        .neumorphicCircle(shadowElevation = 7.dp, containerColor = IncomeGreen)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            colors = listOf(Color(0xFF34D399), IncomeGreen, Color(0xFF047857)),
          ),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.VerifiedUser,
        contentDescription = "সফলভাবে আনলক হয়েছে",
        tint = Color.White,
        modifier = Modifier.size(36.dp),
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = "সফলভাবে আনলক হয়েছে!",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "পিন যাচাই সম্পন্ন হয়েছে। আপনার হিসাব খাতায় প্রবেশ করা হচ্ছে...",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(22.dp))

    NeoSuccessDetailRow(
      icon = Icons.Rounded.CheckCircle,
      title = "পিন ভেরিফিকেশন সফল",
      subtitle = "SHA-256 এনক্রিপ্টেড নিরাপত্তা যাচাই",
    )

    Spacer(modifier = Modifier.height(10.dp))

    NeoSuccessDetailRow(
      icon = Icons.Rounded.Shield,
      title = "My Daily হিসাব সক্রিয়",
      subtitle = "নিরাপদ সেশনে প্রবেশ করছে",
    )
  }
}

@Composable
private fun NeoSuccessDetailRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .neumorphicInset(cornerRadius = 16.dp)
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(38.dp)
        .neumorphicCircle(shadowElevation = 3.dp, containerColor = IncomeGreen.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = IncomeGreen,
        modifier = Modifier.size(20.dp),
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
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

/**
 * Tactile Neomorphic Circular Keypad (0–9, C, ⌫)
 * Depresses cleanly from raised to inset on touch with a vibrant accent glow!
 */
@Composable
private fun NeoCircularKeypad(
  enabled: Boolean,
  onDigitClick: (String) -> Unit,
  onClearClick: () -> Unit,
  onBackspaceClick: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NeoDigitButton("1", enabled) { onDigitClick("1") }
      NeoDigitButton("2", enabled) { onDigitClick("2") }
      NeoDigitButton("3", enabled) { onDigitClick("3") }
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NeoDigitButton("4", enabled) { onDigitClick("4") }
      NeoDigitButton("5", enabled) { onDigitClick("5") }
      NeoDigitButton("6", enabled) { onDigitClick("6") }
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NeoDigitButton("7", enabled) { onDigitClick("7") }
      NeoDigitButton("8", enabled) { onDigitClick("8") }
      NeoDigitButton("9", enabled) { onDigitClick("9") }
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      NeoActionButton(
        label = "C",
        testTag = "keypad_clear",
        enabled = enabled,
        onClick = onClearClick,
      )
      NeoDigitButton("0", enabled) { onDigitClick("0") }
      NeoIconButton(
        icon = Icons.AutoMirrored.Rounded.Backspace,
        contentDescription = "মুছুন",
        testTag = "keypad_backspace",
        enabled = enabled,
        onClick = onBackspaceClick,
      )
    }
  }
}

@Composable
private fun NeoDigitButton(
  digit: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.90f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "neo_digit_scale",
  )

  val buttonSurfaceModifier = if (isPressed) {
    Modifier
      .neumorphicInset(cornerRadius = 33.dp, containerColor = IncomeGreen.copy(alpha = 0.14f))
      .border(1.5.dp, IncomeGreen.copy(alpha = 0.7f), CircleShape)
  } else {
    Modifier.neumorphicCircle(shadowElevation = 5.dp)
  }

  Box(
    modifier = Modifier
      .size(66.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .then(buttonSurfaceModifier)
      .clip(CircleShape)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick,
      )
      .testTag("keypad_digit_$digit"),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = digit,
      fontSize = 25.sp,
      fontWeight = FontWeight.Bold,
      color = if (isPressed) IncomeGreen else MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun NeoActionButton(
  label: String,
  testTag: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.90f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "neo_action_scale",
  )

  val buttonSurfaceModifier = if (isPressed) {
    Modifier
      .neumorphicInset(cornerRadius = 33.dp)
      .border(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f), CircleShape)
  } else {
    Modifier.neumorphicCircle(shadowElevation = 4.dp)
  }

  Box(
    modifier = Modifier
      .size(66.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .then(buttonSurfaceModifier)
      .clip(CircleShape)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick,
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      fontSize = 19.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.secondary,
    )
  }
}

@Composable
private fun NeoIconButton(
  icon: ImageVector,
  contentDescription: String,
  testTag: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.90f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "neo_icon_scale",
  )

  val buttonSurfaceModifier = if (isPressed) {
    Modifier
      .neumorphicInset(cornerRadius = 33.dp)
      .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), CircleShape)
  } else {
    Modifier.neumorphicCircle(shadowElevation = 4.dp)
  }

  Box(
    modifier = Modifier
      .size(66.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .then(buttonSurfaceModifier)
      .clip(CircleShape)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick,
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(23.dp),
    )
  }
}
