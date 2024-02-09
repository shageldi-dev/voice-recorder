package com.gs.voicerecord

import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

@Composable
fun AudioVisualizer(
    modifier: Modifier = Modifier,
    showTimestamps: Boolean = false,
    amplitudes: List<Int>
) {


    /**Set max amplitude to tune the sensitivity
     * Higher the value, lower the sensitivity of the visualizer
     */
    val maxAmplitude = 10000
    val primary = MaterialTheme.colorScheme.primary
    val primaryMuted = primary.copy(alpha = 0.3f)

    val measurer = rememberTextMeasurer()
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Canvas(modifier = Modifier.padding(vertical = 50.dp).fillMaxWidth().height(350.dp)) {
            val height = this.size.height
            val width = this.size.width
            val y = height / 2

            translate(width, y) {
                if (showTimestamps) {
                    drawLine(
                        color = primaryMuted,
                        start = Offset(-width, -y),
                        end = Offset(0f, -y),
                        strokeWidth = 5f
                    )
                    drawLine(
                        color = primaryMuted,
                        start = Offset(-width, y),
                        end = Offset(0f, y),
                        strokeWidth = 5f
                    )
                }
                amplitudes.forEachIndexed { index, amplitude ->
                    val amplitudePercentage = (amplitude.toFloat() / maxAmplitude).coerceAtMost(1f)
                    val boxHeight = height * amplitudePercentage
                    val reverseIndex = index - amplitudes.size
                    val x = 30f * reverseIndex
                    drawRoundRect(
                        color = if (amplitudePercentage > 0.05f) primary else primaryMuted,
                        topLeft = Offset(
                            x,
                            -boxHeight / 2f
                        ),
                        size = Size(15f, boxHeight),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
    }
}
