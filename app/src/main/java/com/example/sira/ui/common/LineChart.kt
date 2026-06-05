package com.example.sira.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Gráfica de línea minimalista dibujada con Canvas (sin dependencias externas).
 *
 * @param values Serie de valores de izquierda (más antiguo) a derecha (más reciente).
 * @param color  Color de la línea y el área bajo la curva.
 */
@Composable
fun LineChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "Aún no hay suficientes datos para graficar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Canvas(modifier = modifier) {
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f

        val stepX = size.width / (values.size - 1)
        // Margen vertical para que la línea no toque los bordes.
        val padY = size.height * 0.1f
        val usableHeight = size.height - padY * 2

        fun pointFor(index: Int): Offset {
            val v = values[index]
            val x = stepX * index
            val y = padY + usableHeight * (1f - (v - min) / range)
            return Offset(x, y)
        }

        // Líneas guía horizontales.
        repeat(4) { i ->
            val y = padY + usableHeight * i / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
        }

        val points = values.indices.map { pointFor(it) }

        // Área rellena bajo la curva.
        val fillPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(fillPath, color = color.copy(alpha = 0.12f))

        // Línea principal.
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(linePath, color = color, style = Stroke(width = 4f))

        // Punto final destacado.
        drawCircle(color = color, radius = 6f, center = points.last())
    }
}
