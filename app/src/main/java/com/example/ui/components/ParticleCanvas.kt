package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun ParticleCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_offset"
    )

    val randomList = rememberParticleList()

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        randomList.forEach { particle ->
            val curY = (particle.initialY + animatedProgress * height * particle.speed) % height
            val curX = (particle.initialX * width + kotlin.math.sin(animatedProgress * 6.28f + particle.phase) * 30f) % width

            drawCircle(
                color = particle.color.copy(alpha = particle.alpha),
                radius = particle.radius,
                center = Offset(curX, curY)
            )
        }
    }
}

private data class ParticleSpec(
    val initialX: Float,
    val initialY: Float,
    val radius: Float,
    val alpha: Float,
    val speed: Float,
    val phase: Float,
    val color: Color
)

@Composable
private fun rememberParticleList(): List<ParticleSpec> {
    return androidx.compose.runtime.remember {
        val rand = Random(42)
        val colors = listOf(
            Color(0xFF005AC1),
            Color(0xFF006A67),
            Color(0xFF00A896),
            Color(0xFFD8E2FF)
        )
        List(25) {
            ParticleSpec(
                initialX = rand.nextFloat(),
                initialY = rand.nextFloat() * 1000f,
                radius = rand.nextFloat() * 8f + 3f,
                alpha = rand.nextFloat() * 0.4f + 0.15f,
                speed = rand.nextFloat() * 0.5f + 0.5f,
                phase = rand.nextFloat() * 3.14f,
                color = colors[rand.nextInt(colors.size)]
            )
        }
    }
}
