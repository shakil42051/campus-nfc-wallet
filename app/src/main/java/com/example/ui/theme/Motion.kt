package com.example.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

object MotionDuration {
    const val Instant = 50
    const val Fast = 150
    const val Short = 220
    const val Medium = 320
    const val Long = 450
    const val ExtraLong = 650
}

object MotionEasing {
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Gentle: Easing = FastOutSlowInEasing
    val Decelerate: Easing = LinearOutSlowInEasing
}

object MotionSpec {
    fun <T> fastTween(easing: Easing = MotionEasing.Standard): TweenSpec<T> =
        tween(durationMillis = MotionDuration.Fast, easing = easing)

    fun <T> shortTween(easing: Easing = MotionEasing.Emphasized): TweenSpec<T> =
        tween(durationMillis = MotionDuration.Short, easing = easing)

    fun <T> mediumTween(easing: Easing = MotionEasing.Emphasized): TweenSpec<T> =
        tween(durationMillis = MotionDuration.Medium, easing = easing)

    fun <T> longTween(easing: Easing = MotionEasing.EmphasizedDecelerate): TweenSpec<T> =
        tween(durationMillis = MotionDuration.Long, easing = easing)

    fun <T> smoothSpring(): SpringSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

    fun <T> bouncySpring(): SpringSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

    fun <T> crispSpring(): SpringSpec<T> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
}

object NavigationTransitions {
    val ForwardEnter: EnterTransition =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> (fullWidth * 0.18f).toInt() },
            animationSpec = MotionSpec.mediumTween(MotionEasing.EmphasizedDecelerate)
        ) + fadeIn(
            animationSpec = MotionSpec.mediumTween(MotionEasing.Emphasized)
        ) + scaleIn(
            initialScale = 0.96f,
            animationSpec = MotionSpec.mediumTween(MotionEasing.Emphasized)
        )

    val ForwardExit: ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> (-fullWidth * 0.18f).toInt() },
            animationSpec = MotionSpec.mediumTween(MotionEasing.EmphasizedAccelerate)
        ) + fadeOut(
            animationSpec = MotionSpec.mediumTween(MotionEasing.Emphasized)
        ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = MotionSpec.mediumTween(MotionEasing.Emphasized)
        )

    val BackEnter: EnterTransition =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> (-fullWidth * 0.18f).toInt() },
            animationSpec = MotionSpec.mediumTween(MotionEasing.EmphasizedDecelerate)
        ) + fadeIn(
            animationSpec = MotionSpec.mediumTween(MotionEasing.Emphasized)
        ) + scaleIn(
            initialScale = 0.96f,
            animationSpec = MotionSpec.mediumTween(MotionEasing.Emphasized)
        )

    val BackExit: ExitTransition =
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> (fullWidth * 0.18f).toInt() },
            animationSpec = MotionSpec.mediumTween(MotionEasing.EmphasizedAccelerate)
        ) + fadeOut(
            animationSpec = MotionSpec.mediumTween(MotionEasing.Emphasized)
        ) + scaleOut(
            targetScale = 0.96f,
            animationSpec = MotionSpec.mediumTween(MotionEasing.Emphasized)
        )

    val TabEnter: EnterTransition =
        fadeIn(
            animationSpec = tween(
                durationMillis = MotionDuration.Short,
                easing = MotionEasing.EmphasizedDecelerate
            )
        ) + scaleIn(
            initialScale = 0.975f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )

    val TabExit: ExitTransition =
        fadeOut(
            animationSpec = tween(
                durationMillis = MotionDuration.Fast,
                easing = MotionEasing.EmphasizedAccelerate
            )
        ) + scaleOut(
            targetScale = 0.985f,
            animationSpec = tween(
                durationMillis = MotionDuration.Fast,
                easing = MotionEasing.EmphasizedAccelerate
            )
        )

    fun tabSlideTransition(isGoingRight: Boolean): ContentTransform {
        val enter = slideInHorizontally(
            initialOffsetX = { if (isGoingRight) (it * 0.20f).toInt() else (-it * 0.20f).toInt() },
            animationSpec = tween(MotionDuration.Medium, easing = MotionEasing.EmphasizedDecelerate)
        ) + fadeIn(animationSpec = tween(MotionDuration.Short, easing = MotionEasing.Emphasized))

        val exit = slideOutHorizontally(
            targetOffsetX = { if (isGoingRight) (-it * 0.20f).toInt() else (it * 0.20f).toInt() },
            animationSpec = tween(MotionDuration.Medium, easing = MotionEasing.EmphasizedAccelerate)
        ) + fadeOut(animationSpec = tween(MotionDuration.Fast, easing = MotionEasing.Emphasized))

        return enter togetherWith exit
    }
}

/**
 * Staggered entrance animation for dashboard cards and items.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    staggerDelayMs: Int = 45,
    durationMs: Int = MotionDuration.Medium
): Modifier = composed {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index.coerceAtMost(8) * staggerDelayMs).toLong())
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = durationMs, easing = MotionEasing.Emphasized),
        label = "staggeredAlpha"
    )

    val translateY by animateFloatAsState(
        targetValue = if (isVisible) 0.0f else 16.0f,
        animationSpec = tween(durationMillis = durationMs, easing = MotionEasing.EmphasizedDecelerate),
        label = "staggeredTranslateY"
    )

    this.graphicsLayer {
        this.alpha = alpha
        this.translationY = translateY.dp.toPx()
    }
}
