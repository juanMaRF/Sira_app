package com.example.sira.data.model

/**
 * Los cuatro sensores de la maceta. Centraliza el nombre, la unidad y el rango
 * ideal de cada uno para reutilizarlos en el dashboard, el detalle y el histórico.
 */
enum class SensorType(
    val displayName: String,
    val unit: String,
    val idealMin: Float,
    val idealMax: Float
) {
    SOIL_MOISTURE("Humedad de la tierra", "%", idealMin = 40f, idealMax = 70f),
    TEMPERATURE("Temperatura", "°C", idealMin = 18f, idealMax = 27f),
    LIGHT("Luz", "%", idealMin = 50f, idealMax = 90f),
    WATER("Nivel del agua", "%", idealMin = 20f, idealMax = 100f);

    /** Extrae el valor numérico de este sensor desde un [PlantStatus]. */
    fun valueFrom(status: PlantStatus): Float = when (this) {
        SOIL_MOISTURE -> status.soilMoisture.toFloat()
        TEMPERATURE -> status.temperature
        LIGHT -> status.lightLevel.toFloat()
        WATER -> status.waterLevel.toFloat()
    }

    /** Texto listo para mostrar, p. ej. "62%" o "23.5 °C". */
    fun format(value: Float): String = when (this) {
        TEMPERATURE -> "%.1f °C".format(value)
        else -> "${value.toInt()}$unit"
    }

    fun format(status: PlantStatus): String = format(valueFrom(status))

    /** Para sensores en porcentaje devuelve 0f..1f; en temperatura, `null`. */
    fun progressFrom(status: PlantStatus): Float? =
        if (this == TEMPERATURE) null else (valueFrom(status) / 100f).coerceIn(0f, 1f)

    /** Evalúa si el valor está por debajo, dentro o por encima del rango ideal. */
    fun statusOf(value: Float): SensorHealth = when {
        value < idealMin -> SensorHealth.LOW
        value > idealMax -> SensorHealth.HIGH
        else -> SensorHealth.OK
    }
}

/** Estado de salud de una lectura respecto a su rango ideal. */
enum class SensorHealth(val label: String) {
    LOW("Bajo"),
    OK("Óptimo"),
    HIGH("Alto")
}
