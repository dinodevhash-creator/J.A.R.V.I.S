package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.JarvisCoreStatus
import com.example.ui.theme.LocalJarvisHudColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisCoreOrb(
    status: JarvisCoreStatus,
    voiceAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    compact: Boolean = false
) {
    val hudColors = LocalJarvisHudColors.current

    val primaryCoreColor by animateColorAsState(
        targetValue = when (status) {
            JarvisCoreStatus.STANDBY -> hudColors.cyanGlow
            JarvisCoreStatus.LISTENING -> hudColors.emeraldStatus
            JarvisCoreStatus.THINKING -> hudColors.amberAccent
            JarvisCoreStatus.SPEAKING -> hudColors.cyanGlow
            JarvisCoreStatus.ERROR -> hudColors.crimsonAlert
        },
        animationSpec = tween(350),
        label = "corePrimaryColor"
    )

    val secondaryCoreColor by animateColorAsState(
        targetValue = when (status) {
            JarvisCoreStatus.THINKING -> hudColors.cyanGlow
            JarvisCoreStatus.LISTENING -> hudColors.cyanGlow
            else -> hudColors.amberAccent
        },
        animationSpec = tween(350),
        label = "coreSecondaryColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "jarvisOrbTransition")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == JarvisCoreStatus.THINKING) 3200 else 16000,
                easing = LinearEasing
            )
        ),
        label = "outerRotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == JarvisCoreStatus.THINKING) 2400 else 11000,
                easing = LinearEasing
            )
        ),
        label = "innerRotation"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (status) {
                    JarvisCoreStatus.SPEAKING -> 450
                    JarvisCoreStatus.LISTENING -> 650
                    JarvisCoreStatus.THINKING -> 550
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    val smoothedAmplitude by animateFloatAsState(
        targetValue = if (status == JarvisCoreStatus.LISTENING) voiceAmplitude else 0f,
        animationSpec = spring(stiffness = 300f),
        label = "smoothedAmplitude"
    )

    val statusLabel = when (status) {
        JarvisCoreStatus.STANDBY -> "SIAGA"
        JarvisCoreStatus.LISTENING -> "MENDENGARKAN"
        JarvisCoreStatus.THINKING -> "MEMPROSES"
        JarvisCoreStatus.SPEAKING -> "BERBICARA"
        JarvisCoreStatus.ERROR -> "GANGGUAN"
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = primaryCoreColor),
                role = Role.Button,
                onClick = onClick
            )
            .semantics {
                contentDescription = "Inti AI J.A.R.V.I.S. status $statusLabel, ketuk untuk perintah suara"
            }
            .testTag("jarvis_core_orb"),
        contentAlignment = Alignment.Center
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val canvasMin = this.size.minDimension
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val baseRadius = canvasMin * 0.44f
                    val ambientRadius = (canvasMin * 0.50f).coerceAtLeast(1f)
                    val majorTickWidth = 2.5.dp.toPx()
                    val minorTickWidth = 1.2.dp.toPx()
                    val outerArcStroke = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                    val innerArcStroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    val nucleusStroke = Stroke(width = 1.5.dp.toPx())
                    val tickOuter = baseRadius * 1.05f
                    val tickInner = baseRadius * 0.94f
                    val tickPairs = List(24) { i ->
                        val angleRad = Math.toRadians(i * 15.0)
                        val cosA = cos(angleRad).toFloat()
                        val sinA = sin(angleRad).toFloat()
                        Offset(center.x + tickInner * cosA, center.y + tickInner * sinA) to
                            Offset(center.x + tickOuter * cosA, center.y + tickOuter * sinA)
                    }
                    val arcRadius = baseRadius * 0.86f
                    val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                    val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

                    val ambientBrush = Brush.radialGradient(
                        colors = listOf(
                            primaryCoreColor.copy(alpha = 0.28f),
                            primaryCoreColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = ambientRadius
                    )

                    onDrawBehind {
                        val reactiveRadius = baseRadius * (breathingScale + smoothedAmplitude * 0.14f)

                        // Ambient radial reactor glow
                        drawCircle(
                            brush = ambientBrush,
                            radius = ambientRadius,
                            center = center
                        )

                        // Outer precision tick ring
                        rotate(degrees = outerRotation, pivot = center) {
                            tickPairs.forEachIndexed { i, (start, end) ->
                                val isMajor = i % 3 == 0
                                drawLine(
                                    color = primaryCoreColor.copy(alpha = if (isMajor) 0.85f else 0.30f),
                                    start = start,
                                    end = end,
                                    strokeWidth = if (isMajor) majorTickWidth else minorTickWidth,
                                    cap = StrokeCap.Round
                                )
                            }

                            // Segmented outer arc reactor rings
                            for (segment in 0 until 3) {
                                drawArc(
                                    color = primaryCoreColor.copy(alpha = 0.85f),
                                    startAngle = segment * 120f + 12f,
                                    sweepAngle = 76f,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = arcSize,
                                    style = outerArcStroke
                                )
                            }
                        }

                        // Counter-rotating inner telemetry ring
                        rotate(degrees = innerRotation, pivot = center) {
                            val innerArcRadius = reactiveRadius * 0.68f
                            val innerTopLeft = Offset(center.x - innerArcRadius, center.y - innerArcRadius)
                            val innerSize = Size(innerArcRadius * 2f, innerArcRadius * 2f)
                            for (segment in 0 until 4) {
                                drawArc(
                                    color = secondaryCoreColor.copy(alpha = 0.72f),
                                    startAngle = segment * 90f + 18f,
                                    sweepAngle = 48f,
                                    useCenter = false,
                                    topLeft = innerTopLeft,
                                    size = innerSize,
                                    style = innerArcStroke
                                )
                            }
                        }

                        // Inner glowing core nucleus
                        val nucleusRadius = (reactiveRadius * 0.45f).coerceAtLeast(1f)
                        drawCircle(
                            color = primaryCoreColor.copy(alpha = 0.22f),
                            radius = nucleusRadius,
                            center = center
                        )

                        drawCircle(
                            color = primaryCoreColor.copy(alpha = 0.8f),
                            radius = nucleusRadius * 0.88f,
                            center = center,
                            style = nucleusStroke
                        )
                    }
                }
        )

        if (!compact) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    text = "CORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryCoreColor.copy(alpha = 0.8f)
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
