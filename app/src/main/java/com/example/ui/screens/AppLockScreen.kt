package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Premium "Glassy App Lock V15" UI
 *
 * Features:
 * - Smoky Dark Brown Transparent Glass Card with multi-layer specular highlights and soft lighting
 * - Ambient backdrop with glowing warm amber and emerald mesh lights
 * - 3D Circular Glass Lock Icon transitioning into an animated emerald checkmark upon correct PIN
 * - 4 PIN Input capsules with neon emerald glow pulses
 * - Circular Glass Numeric Keypad with smooth press physics, scale spring, and vibrant green glow effect
 * - Subtle sinusoidal shake animation on invalid PIN
 * - Real SHA-256 PIN verification with secure unlock system
 */
@Composable
fun AppLockScreen(
  onVerifyPin: suspend (String) -> Boolean,
  onUnlocked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var enteredPin by remember { mutableStateOf("") }
  var isVerifying by remember { mutableStateOf(false) }
  var isSuccess by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val shakeOffset = remember { Animatable(0f) }

  val scope = rememberCoroutineScope()
  val haptic = LocalHapticFeedback.current

  // Trigger PIN verification when 4 digits are completed
  fun onDigitClick(digit: String) {
    if (isVerifying || isSuccess || enteredPin.length >= 4) return
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    errorMessage = null
    val nextPin = enteredPin + digit
    enteredPin = nextPin

    if (nextPin.length == 4) {
      isVerifying = true
      scope.launch {
        delay(120)
        val valid = onVerifyPin(nextPin)
        if (valid) {
          isSuccess = true
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          delay(420)
          onUnlocked()
        } else {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          errorMessage = "ভুল পিন! আবার সঠিক পিন দিন।"
          // Perform smooth video-game style sinusoidal shake
          shakeOffset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
              durationMillis = 420
              0f at 0
              -22f at 40
              22f at 90
              -16f at 140
              16f at 190
              -10f at 240
              10f at 290
              -4f at 340
              0f at 420
            },
          )
          delay(300)
          enteredPin = ""
          isVerifying = false
        }
      }
    }
  }

  fun onBackspaceClick() {
    if (isVerifying || isSuccess || enteredPin.isEmpty()) return
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    errorMessage = null
    enteredPin = enteredPin.dropLast(1)
  }

  fun onClearClick() {
    if (isVerifying || isSuccess || enteredPin.isEmpty()) return
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    errorMessage = null
    enteredPin = ""
  }

  // Root Atmospheric Background
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0D0907)),
    contentAlignment = Alignment.Center,
  ) {
    // Ambient Background Glowing Orbs for realistic Glassmorphism refraction
    AmbientBackdropLights()

    // Main Scrollable Container for Compact & Tablet responsiveness
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 24.dp),
      contentAlignment = Alignment.Center,
    ) {
      // Smoky Dark Brown Transparent Glass Card (V15)
      SmokyGlassCard(
        modifier = Modifier
          .widthIn(max = 420.dp)
          .fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // Top V15 Secure Pill Badge
          GlassStatusBadge(isSuccess = isSuccess)

          Spacer(modifier = Modifier.height(18.dp))

          // 3D Circular Glass Lock Icon / Animated Green Checkmark
          GlassLockHeaderOrb(
            isSuccess = isSuccess,
            isVerifying = isVerifying,
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Title & Subtitle
          Text(
            text = if (isSuccess) "Access Granted" else "Enter Your PIN",
            style = MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
            ),
            color = if (isSuccess) Color(0xFF34D399) else Color(0xFFF8FAFC),
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = if (isSuccess) "লক সফলভাবে খোলা হয়েছে..." else "আপনার হিসাব সুরক্ষিত রাখতে ৪ সংখ্যার পিন দিন",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFD1C7BD),
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(24.dp))

          // 4 PIN Input Indicators with subtle shake animation on error
          Row(
            modifier = Modifier
              .graphicsLayer { translationX = shakeOffset.value }
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            repeat(4) { index ->
              val isFilled = index < enteredPin.length
              GlassPinIndicatorDot(
                isFilled = isFilled,
                isSuccess = isSuccess,
                isError = errorMessage != null,
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Error / Status Pill with smooth fade transition
          Box(
            modifier = Modifier.height(30.dp),
            contentAlignment = Alignment.Center,
          ) {
            if (isVerifying && !isSuccess && errorMessage == null) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF10B981),
              )
            } else if (errorMessage != null) {
              Surface(
                color = Color(0x33EF4444),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x66EF4444)),
              ) {
                Text(
                  text = errorMessage!!,
                  color = Color(0xFFFCA5A5),
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Circular Glass Numeric Keypad (1 to 9, C, 0, ⌫)
          GlassNumericKeypad(
            onDigitClick = ::onDigitClick,
            onClearClick = ::onClearClick,
            onBackspaceClick = ::onBackspaceClick,
          )
        }
      }
    }
  }
}

/**
 * Ambient background glowing orbs generating organic light refraction
 * through the smoky dark glass card.
 */
@Composable
private fun AmbientBackdropLights() {
  Box(modifier = Modifier.fillMaxSize()) {
    // Warm amber luminous glow at top-left
    Box(
      modifier = Modifier
        .size(360.dp)
        .offset(x = (-80).dp, y = (-60).dp)
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color(0x38D97706),
              Color(0x18B45309),
              Color.Transparent,
            ),
          ),
          shape = CircleShape,
        ),
    )

    // Vibrant emerald neon glow at bottom-right
    Box(
      modifier = Modifier
        .size(380.dp)
        .align(Alignment.BottomEnd)
        .offset(x = 90.dp, y = 70.dp)
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color(0x3310B981),
              Color(0x15059669),
              Color.Transparent,
            ),
          ),
          shape = CircleShape,
        ),
    )

    // Warm smoky chocolate glow in center
    Box(
      modifier = Modifier
        .size(460.dp)
        .align(Alignment.Center)
        .background(
          Brush.radialGradient(
            colors = listOf(
              Color(0x2E4A2818),
              Color(0x1A28140B),
              Color.Transparent,
            ),
          ),
          shape = CircleShape,
        ),
    )
  }
}

/**
 * Smoky Dark Brown Transparent Glass Card with rounded border,
 * inner specular refraction, and soft 3D floating shadow.
 */
@Composable
private fun SmokyGlassCard(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = modifier
      .shadow(
        elevation = 24.dp,
        shape = RoundedCornerShape(32.dp),
        ambientColor = Color(0x80000000),
        spotColor = Color(0x8010B981),
      )
      .clip(RoundedCornerShape(32.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xDE2B1B13), // 87% Smoky Dark Brown
            Color(0xCC1A100B), // 80% Deep Espresso
            Color(0xD9140C08), // 85% Dark Charcoal Brown
          ),
        ),
      )
      .border(
        BorderStroke(
          width = 1.5.dp,
          brush = Brush.verticalGradient(
            colors = listOf(
              Color(0x66F59E0B), // Specular Golden Light on Top Rim
              Color(0x338A5A36), // Soft Smoked Amber
              Color(0x1A251710), // Translucent Dark Edge
              Color(0x4D10B981), // Ambient Emerald Glow on Bottom Rim
            ),
          ),
        ),
        shape = RoundedCornerShape(32.dp),
      ),
  ) {
    // Subtle internal specular gleam at the top border
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.5.dp)
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              Color.Transparent,
              Color(0x80FDE68A),
              Color(0x8034D399),
              Color.Transparent,
            ),
          ),
        ),
    )

    content()
  }
}

/**
 * Top pill badge indicating V15 security state
 */
@Composable
private fun GlassStatusBadge(isSuccess: Boolean) {
  val badgeBg = if (isSuccess) Color(0x3310B981) else Color(0x263B2317)
  val badgeBorder = if (isSuccess) Color(0x8010B981) else Color(0x3DF59E0B)
  val badgeText = if (isSuccess) "VERIFIED • UNLOCKED" else "GLASSY V15 • SECURE"
  val iconTint = if (isSuccess) Color(0xFF34D399) else Color(0xFFF59E0B)

  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .background(badgeBg)
      .border(1.dp, badgeBorder, RoundedCornerShape(20.dp))
      .padding(horizontal = 14.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Icon(
      imageVector = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Shield,
      contentDescription = null,
      tint = iconTint,
      modifier = Modifier.size(13.dp),
    )
    Text(
      text = badgeText,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.5.sp,
      color = if (isSuccess) Color(0xFF34D399) else Color(0xFFFDE68A),
    )
  }
}

/**
 * 3D Circular Glass Lock Icon orb with soft neon glow ring,
 * smoothly morphing into an animated green checkmark on correct PIN.
 */
@Composable
private fun GlassLockHeaderOrb(
  isSuccess: Boolean,
  isVerifying: Boolean,
) {
  val orbScale by animateFloatAsState(
    targetValue = if (isSuccess) 1.12f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "orb_scale",
  )

  val glowColor = if (isSuccess) Color(0xFF10B981) else Color(0xFFD97706)

  Box(
    modifier = Modifier
      .size(76.dp)
      .graphicsLayer {
        scaleX = orbScale
        scaleY = orbScale
      }
      .shadow(
        elevation = 16.dp,
        shape = CircleShape,
        ambientColor = glowColor,
        spotColor = glowColor,
      )
      .clip(CircleShape)
      .background(
        Brush.radialGradient(
          colors = if (isSuccess) {
            listOf(Color(0xFF059669), Color(0xCC065F46), Color(0x99042F2E))
          } else {
            listOf(Color(0xCC452A1D), Color(0xB32A180F), Color(0xCC180E08))
          },
        ),
      )
      .border(
        BorderStroke(
          width = 1.5.dp,
          brush = Brush.verticalGradient(
            colors = if (isSuccess) {
              listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0x33059669))
            } else {
              listOf(Color(0x80FDE68A), Color(0x40D97706), Color(0x2610B981))
            },
          ),
        ),
        shape = CircleShape,
      ),
    contentAlignment = Alignment.Center,
  ) {
    if (isSuccess) {
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = "আনলক সফল",
        tint = Color(0xFFD1FAE5),
        modifier = Modifier.size(38.dp),
      )
    } else {
      Icon(
        imageVector = if (isVerifying) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
        contentDescription = "লক করা আছে",
        tint = Color(0xFFFDE68A),
        modifier = Modifier.size(34.dp),
      )
    }
  }
}

/**
 * Glassy PIN Dot indicator with neon emerald glow when filled
 */
@Composable
private fun GlassPinIndicatorDot(
  isFilled: Boolean,
  isSuccess: Boolean,
  isError: Boolean,
) {
  val scale by animateFloatAsState(
    targetValue = if (isFilled || isSuccess) 1.15f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "pin_dot_scale",
  )

  val dotColor = when {
    isSuccess -> Color(0xFF10B981)
    isError -> Color(0xFFEF4444)
    isFilled -> Color(0xFF10B981) // Vibrant Emerald Glow
    else -> Color(0x26FFFFFF)
  }

  val borderColor = when {
    isSuccess -> Color(0xFF6EE7B7)
    isError -> Color(0xFFF87171)
    isFilled -> Color(0xFF34D399)
    else -> Color(0x33F59E0B)
  }

  Box(
    modifier = Modifier
      .size(20.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .let { mod ->
        if (isFilled || isSuccess) {
          mod.shadow(
            elevation = 10.dp,
            shape = CircleShape,
            ambientColor = dotColor,
            spotColor = dotColor,
          )
        } else {
          mod
        }
      }
      .clip(CircleShape)
      .background(
        if (isFilled || isSuccess) {
          Brush.radialGradient(
            colors = listOf(
              if (isError) Color(0xFFEF4444) else Color(0xFF34D399),
              dotColor,
              Color(0xCC064E3B),
            ),
          )
        } else {
          Brush.verticalGradient(
            colors = listOf(
              Color(0x2B3D2619),
              Color(0x1A1F130C),
            ),
          )
        },
      )
      .border(
        BorderStroke(1.2.dp, borderColor),
        CircleShape,
      ),
  )
}

/**
 * 0-9 Circular Glass Numeric Keypad with smooth press physics,
 * spring scale animation, and energetic Green Glow effect.
 */
@Composable
private fun GlassNumericKeypad(
  onDigitClick: (String) -> Unit,
  onClearClick: () -> Unit,
  onBackspaceClick: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(22.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      GlassKeypadDigitButton(digit = "1", onClick = { onDigitClick("1") })
      GlassKeypadDigitButton(digit = "2", onClick = { onDigitClick("2") })
      GlassKeypadDigitButton(digit = "3", onClick = { onDigitClick("3") })
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(22.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      GlassKeypadDigitButton(digit = "4", onClick = { onDigitClick("4") })
      GlassKeypadDigitButton(digit = "5", onClick = { onDigitClick("5") })
      GlassKeypadDigitButton(digit = "6", onClick = { onDigitClick("6") })
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(22.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      GlassKeypadDigitButton(digit = "7", onClick = { onDigitClick("7") })
      GlassKeypadDigitButton(digit = "8", onClick = { onDigitClick("8") })
      GlassKeypadDigitButton(digit = "9", onClick = { onDigitClick("9") })
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(22.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Clear (C) button
      GlassKeypadActionButton(
        label = "C",
        testTag = "keypad_clear",
        onClick = onClearClick,
        textColor = Color(0xFFFBBF24),
      )
      // 0 button
      GlassKeypadDigitButton(digit = "0", onClick = { onDigitClick("0") })
      // Backspace (⌫) button
      GlassKeypadIconButton(
        icon = Icons.AutoMirrored.Rounded.Backspace,
        testTag = "keypad_backspace",
        onClick = onBackspaceClick,
      )
    }
  }
}

/**
 * Circular Glass Digit Button with smooth press animation & emerald green glow
 */
@Composable
private fun GlassKeypadDigitButton(
  digit: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  // Spring scale animation for tactile physical feeling
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.91f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "keypad_press_scale",
  )

  Box(
    modifier = modifier
      .size(72.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .let { mod ->
        if (isPressed) {
          // Intense Green Glow Effect on Press
          mod.shadow(
            elevation = 18.dp,
            shape = CircleShape,
            ambientColor = Color(0xFF10B981),
            spotColor = Color(0xFF34D399),
          )
        } else {
          mod.shadow(
            elevation = 6.dp,
            shape = CircleShape,
            ambientColor = Color(0x66000000),
            spotColor = Color(0x33D97706),
          )
        }
      }
      .clip(CircleShape)
      .background(
        if (isPressed) {
          // Glowing translucent emerald green glass on press
          Brush.radialGradient(
            colors = listOf(
              Color(0x8010B981),
              Color(0x4D059669),
              Color(0x66022C22),
            ),
          )
        } else {
          // Smoky Dark Brown Glass in normal state
          Brush.verticalGradient(
            colors = listOf(
              Color(0x3D3D271B), // Translucent Smoky Brown
              Color(0x2B271911),
              Color(0x38190E08),
            ),
          )
        },
      )
      .border(
        BorderStroke(
          width = if (isPressed) 2.dp else 1.2.dp,
          brush = if (isPressed) {
            Brush.verticalGradient(
              colors = listOf(
                Color(0xFF6EE7B7), // Neon Green Border Glow
                Color(0xFF10B981),
                Color(0xFF047857),
              ),
            )
          } else {
            Brush.verticalGradient(
              colors = listOf(
                Color(0x59F59E0B), // Specular Golden Rim
                Color(0x264A2F20),
                Color(0x1F2A1A12),
              ),
            )
          },
        ),
        shape = CircleShape,
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      )
      .testTag("keypad_digit_$digit"),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = digit,
      fontSize = 28.sp,
      fontWeight = FontWeight.Bold,
      color = if (isPressed) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
    )
  }
}

/**
 * Circular Glass Action Button (e.g. "C" Clear)
 */
@Composable
private fun GlassKeypadActionButton(
  label: String,
  testTag: String,
  onClick: () -> Unit,
  textColor: Color,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.91f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "action_press_scale",
  )

  Box(
    modifier = modifier
      .size(72.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .let { mod ->
        if (isPressed) {
          mod.shadow(
            elevation = 16.dp,
            shape = CircleShape,
            ambientColor = Color(0xFFF59E0B),
            spotColor = Color(0xFFFDE68A),
          )
        } else {
          mod.shadow(
            elevation = 6.dp,
            shape = CircleShape,
            ambientColor = Color(0x66000000),
            spotColor = Color(0x26000000),
          )
        }
      }
      .clip(CircleShape)
      .background(
        if (isPressed) {
          Brush.radialGradient(
            colors = listOf(
              Color(0x66B45309),
              Color(0x4078350F),
              Color(0x4D2A180F),
            ),
          )
        } else {
          Brush.verticalGradient(
            colors = listOf(
              Color(0x2E362116),
              Color(0x1F24150D),
            ),
          )
        },
      )
      .border(
        BorderStroke(
          width = 1.2.dp,
          brush = Brush.verticalGradient(
            colors = if (isPressed) {
              listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
            } else {
              listOf(Color(0x40F59E0B), Color(0x1A452B1E))
            },
          ),
        ),
        shape = CircleShape,
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = textColor,
    )
  }
}

/**
 * Circular Glass Icon Button (e.g. Backspace ⌫)
 */
@Composable
private fun GlassKeypadIconButton(
  icon: ImageVector,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.91f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium,
    ),
    label = "icon_press_scale",
  )

  Box(
    modifier = modifier
      .size(72.dp)
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .let { mod ->
        if (isPressed) {
          mod.shadow(
            elevation = 16.dp,
            shape = CircleShape,
            ambientColor = Color(0xFF10B981),
            spotColor = Color(0xFF34D399),
          )
        } else {
          mod.shadow(
            elevation = 6.dp,
            shape = CircleShape,
            ambientColor = Color(0x66000000),
            spotColor = Color(0x26000000),
          )
        }
      }
      .clip(CircleShape)
      .background(
        if (isPressed) {
          Brush.radialGradient(
            colors = listOf(
              Color(0x8010B981),
              Color(0x4D059669),
              Color(0x4D022C22),
            ),
          )
        } else {
          Brush.verticalGradient(
            colors = listOf(
              Color(0x2E362116),
              Color(0x1F24150D),
            ),
          )
        },
      )
      .border(
        BorderStroke(
          width = 1.2.dp,
          brush = Brush.verticalGradient(
            colors = if (isPressed) {
              listOf(Color(0xFF6EE7B7), Color(0xFF10B981))
            } else {
              listOf(Color(0x40F59E0B), Color(0x1A452B1E))
            },
          ),
        ),
        shape = CircleShape,
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = "Backspace",
      tint = if (isPressed) Color(0xFFECFDF5) else Color(0xFFD1C7BD),
      modifier = Modifier.size(24.dp),
    )
  }
}
