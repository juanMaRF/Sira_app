package com.example.sira.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.sira.data.model.SensorType
import com.example.sira.ui.theme.LeafGreen
import com.example.sira.ui.theme.Soil
import com.example.sira.ui.theme.Sun
import com.example.sira.ui.theme.Water

/** Ícono y color de acento asociados a cada sensor. */
data class SensorVisual(val icon: ImageVector, val accent: Color)

/** Mapeo (puro) de un [SensorType] a su representación visual. */
fun SensorType.visual(): SensorVisual = when (this) {
    SensorType.SOIL_MOISTURE -> SensorVisual(Icons.Filled.Opacity, Soil)
    SensorType.TEMPERATURE -> SensorVisual(Icons.Filled.Thermostat, LeafGreen)
    SensorType.LIGHT -> SensorVisual(Icons.Filled.WbSunny, Sun)
    SensorType.WATER -> SensorVisual(Icons.Filled.WaterDrop, Water)
}
