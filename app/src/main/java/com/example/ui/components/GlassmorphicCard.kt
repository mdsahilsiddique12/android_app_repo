package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color = Color(0xFFFFFFFF).copy(alpha = 0.82f),
    borderColor: Color = Color(0xFFC4D6ED).copy(alpha = 0.6f),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val glassBrush = Brush.verticalGradient(
        colors = listOf(
            backgroundColor,
            backgroundColor.copy(alpha = 0.65f)
        )
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(glassBrush)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(16.dp),
        content = content
    )
}
