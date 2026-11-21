package com.example.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Tactile press-scale clickable modifier backed by MutableInteractionSource (no pointerInput restarts).
 * Use on non-Button Surfaces/Cards only — do NOT stack on Material Button(onClick = ...).
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Shakes the element horizontally to indicate validation errors or deletion prompts.
 */
fun Modifier.shakeEffect(
    trigger: Boolean,
    onAnimationEnd: (() -> Unit)? = null
): Modifier = composed {
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframeShakeSpec()
            )
            onAnimationEnd?.invoke()
        }
    }

    this.graphicsLayer {
        translationX = shakeOffset.value
    }
}

private fun keyframeShakeSpec() = keyframes<Float> {
    durationMillis = 350
    0f at 0 using FastOutSlowInEasing
    -12f at 50 using FastOutSlowInEasing
    12f at 100 using FastOutSlowInEasing
    -9f at 150 using FastOutSlowInEasing
    9f at 200 using FastOutSlowInEasing
    -4f at 250 using FastOutSlowInEasing
    0f at 350 using FastOutSlowInEasing
}

/**
 * Breathing / pulse effect for biometric scanning, NFC detection, or active badges.
 */
fun Modifier.pulseEffect(
    enabled: Boolean = true,
    minScale: Float = 0.95f,
    maxScale: Float = 1.08f,
    durationMillis: Int = 1200
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
