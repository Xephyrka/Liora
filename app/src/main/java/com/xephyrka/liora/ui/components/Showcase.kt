package com.xephyrka.liora.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.xephyrka.liora.R

/**
 * Data class representing a single step in the showcase onboarding.
 */
data class ShowcaseStep(
    val id: String,
    val title: String,
    val description: String,
    val targetTag: String? = null,
    val isFullScreen: Boolean = false
)

/**
 * State manager for the showcase system.
 */
class ShowcaseState(
    steps: List<ShowcaseStep>,
    val onFinish: () -> Unit
) {
    var steps by mutableStateOf(steps)
    var currentStepIndex by mutableIntStateOf(0)
    var isVisible by mutableStateOf(false)
    
    private val targetCoordinates = mutableStateMapOf<String, LayoutCoordinates>()

    fun updateTargetCoordinates(tag: String, coordinates: LayoutCoordinates) {
        targetCoordinates[tag] = coordinates
    }

    fun getTargetBounds(tag: String?): Rect? {
        if (tag == null) return null
        val coords = targetCoordinates[tag] ?: return null
        if (!coords.isAttached) return null
        val pos = coords.positionInRoot()
        val size = coords.size.toSize()
        return Rect(pos, size)
    }

    fun next() {
        if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
        } else {
            isVisible = false
            onFinish()
        }
    }

    fun skip() {
        isVisible = false
        currentStepIndex = 0
        onFinish()
    }

    fun reset() {
        currentStepIndex = 0
        isVisible = true
    }
}

@Composable
fun rememberShowcaseState(steps: List<ShowcaseStep>, onFinish: () -> Unit): ShowcaseState {
    return remember { ShowcaseState(steps, onFinish) }
}

fun Modifier.showcaseTarget(tag: String, state: ShowcaseState): Modifier = this.onGloballyPositioned {
    state.updateTargetCoordinates(tag, it)
}

@Composable
fun Showcase(
    state: ShowcaseState,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible || state.steps.isEmpty()) return

    val currentStep = state.steps[state.currentStepIndex]
    val targetBounds = state.getTargetBounds(currentStep.targetTag)

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1000f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Block clicks */ }
    ) {
        if (currentStep.isFullScreen) {
            FullScreenGuide(
                step = currentStep,
                onNext = { state.next() },
                onSkip = { state.skip() },
                isLastStep = state.currentStepIndex == state.steps.size - 1
            )
        } else {
            SpotlightOverlay(targetBounds)

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val cardModifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 400.dp)

                TooltipCard(
                    step = currentStep,
                    onNext = { state.next() },
                    onSkip = { state.skip() },
                    isLastStep = state.currentStepIndex == state.steps.size - 1,
                    modifier = cardModifier
                )
            }
        }
    }
}

@Composable
private fun FullScreenGuide(
    step: ShowcaseStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastStep: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.liora_icon),
                contentDescription = "Liora Icon",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isLastStep) "Get Started" else "Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onSkip,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SpotlightOverlay(targetBounds: Rect?) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val overlayPath = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
        }

        val cutoutPath = Path().apply {
            if (targetBounds != null) {
                val padding = 12.dp.toPx()
                val rect = targetBounds.inflate(padding)
                val center = rect.center
                val width = rect.width * pulseScale
                val height = rect.height * pulseScale
                
                addRoundRect(
                    RoundRect(
                        left = center.x - width / 2,
                        top = center.y - height / 2,
                        right = center.x + width / 2,
                        bottom = center.y + height / 2,
                        cornerRadius = CornerRadius(20.dp.toPx())
                    )
                )
            }
        }

        drawPath(
            path = Path.combine(PathOperation.Difference, overlayPath, cutoutPath),
            color = Color.Black.copy(alpha = 0.85f) // Increased dimming for better contrast, especially in Dark Mode
        )
    }
}

@Composable
private fun TooltipCard(
    step: ShowcaseStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastStep: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isLastStep) "Done" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
