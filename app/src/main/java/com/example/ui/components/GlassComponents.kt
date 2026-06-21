package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    glowOpacity: Float = 0.15f,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_glow_transition")
    val animFactor by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glass_glow_anim"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(SurfaceGrey.copy(alpha = 0.75f))
            .drawBehind {
                // Draw a subtle animated neon glow behind the borders
                val glowBrush = Brush.radialGradient(
                    colors = listOf(
                        NeonPurplePrimary.copy(alpha = glowOpacity * animFactor),
                        Color.Transparent
                    ),
                    center = this.center,
                    radius = this.size.maxDimension * 0.7f
                )
                drawRect(brush = glowBrush)
            }
            .border(
                borderWidth,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        NeonPurplePrimary.copy(alpha = 0.4f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String = "submit_button"
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .testTag(tag)
            .height(50.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(25.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            NeonPurplePrimary,
                            PurpleGlow
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            ElevatedSurface,
                            SurfaceGrey
                        )
                    )
                }
            )
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun SecondaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = NeonPurplePrimary.copy(alpha = 0.35f),
    tag: String = "secondary_button"
) {
    Box(
        modifier = modifier
            .testTag(tag)
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGrey.copy(alpha = 0.5f))
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    tag: String = "input_text"
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .testTag(tag)
            .fillMaxWidth()
            .background(SurfaceGrey.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
        placeholder = { Text(placeholderText, color = TextSecondary.copy(alpha = 0.6f)) },
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        trailingIcon = trailingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonPurplePrimary,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = NeonPurplePrimary
        )
    )
}

@Composable
fun FosphorusLogo(
    modifier: Modifier = Modifier,
    active: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim_transition")
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    val rotateDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_rotate"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = if (active) pulse else 1f
                scaleY = if (active) pulse else 1f
            }
            .size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow canvas flame ring
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotateDegrees }
        ) {
            val centerOffset = this.center
            val radius = size.minDimension / 2.6f
            
            // Draw secondary ambient glowing ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        NeonPurplePrimary.copy(alpha = 0.02f),
                        PurpleGlow.copy(alpha = 0.35f),
                        NeonPurplePrimary.copy(alpha = 0.65f),
                        Color.Transparent,
                        PurpleGlow.copy(alpha = 0.02f)
                    )
                ),
                radius = radius,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Inner glowing flame icon
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(54.dp)
        ) {
            val width = size.width
            val height = size.height
            val flamePath = Path().apply {
                // Draw a beautiful flame icon (starting from center bottom, sweeping left, dynamic peaks, right curving down)
                moveTo(width * 0.5f, height * 0.95f)
                cubicTo(
                    width * 0.15f, height * 0.82f,
                    width * 0.02f, height * 0.55f,
                    width * 0.18f, height * 0.32f
                )
                cubicTo(
                    width * 0.22f, height * 0.25f,
                    width * 0.32f, height * 0.18f,
                    width * 0.38f, height * 0.28f
                )
                cubicTo(
                    width * 0.25f, height * 0.38f,
                    width * 0.28f, height * 0.52f,
                    width * 0.42f, height * 0.55f
                )
                // Main highest peak
                cubicTo(
                    width * 0.45f, height * 0.35f,
                    width * 0.55f, height * 0.02f,
                    width * 0.68f, height * 0.08f
                )
                cubicTo(
                    width * 0.85f, height * 0.28f,
                    width * 0.99f, height * 0.55f,
                    width * 0.88f, height * 0.78f
                )
                cubicTo(
                    width * 0.75f, height * 0.98f,
                    width * 0.58f, height * 0.97f,
                    width * 0.50f, height * 0.95f
                )
                close()
            }

            drawPath(
                path = flamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PurpleGlow,
                        NeonPurplePrimary
                    )
                )
            )
        }
    }
}
