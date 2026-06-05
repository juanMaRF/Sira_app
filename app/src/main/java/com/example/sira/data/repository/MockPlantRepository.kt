package com.example.sira.data.repository

import com.example.sira.data.model.PlantStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Implementación SIMULADA del repositorio de la planta.
 *
 * Emite un [PlantStatus] inicial y luego lo va actualizando cada pocos segundos
 * con pequeñas variaciones, para que el dashboard se sienta "en tiempo real"
 * sin necesidad de hardware ni de Firebase.
 *
 * Para conectar la nube más adelante: crea `FirestorePlantRepository : PlantRepository`
 * y cámbialo en `ServiceLocator`. Nada más necesita cambiar.
 */
class MockPlantRepository : PlantRepository {

    override fun observePlantStatus(plantId: String): Flow<PlantStatus> = flow {
        var status = PlantStatus(
            plantName = "Albahaca de la cocina",
            soilMoisture = 62,
            temperature = 23.5f,
            lightLevel = 74,
            waterLevel = 88
        )
        emit(status)

        // Bucle de actualización en tiempo real simulado.
        while (true) {
            delay(3_000)
            status = status.copy(
                soilMoisture = jitter(status.soilMoisture, range = 3, min = 0, max = 100),
                temperature = (status.temperature + Random.nextDouble(-0.4, 0.4).toFloat())
                    .coerceIn(15f, 35f),
                lightLevel = jitter(status.lightLevel, range = 6, min = 0, max = 100),
                waterLevel = jitter(status.waterLevel, range = 1, min = 0, max = 100),
                lastUpdated = System.currentTimeMillis()
            )
            emit(status)
        }
    }

    override suspend fun getHistory(plantId: String, limit: Int): List<PlantStatus> {
        delay(500) // Simula la latencia de la consulta.
        val now = System.currentTimeMillis()
        val stepMillis = 60 * 60 * 1000L // una lectura por hora
        return (0 until limit).map { i ->
            // i = 0 es la más antigua; i = limit-1 es la más reciente.
            val phase = i / 3.0
            PlantStatus(
                plantName = "Albahaca de la cocina",
                soilMoisture = (58 + 12 * sin(phase) + Random.nextInt(-3, 4))
                    .toInt().coerceIn(0, 100),
                temperature = (23f + 4f * sin(phase / 2).toFloat())
                    .coerceIn(15f, 35f),
                lightLevel = (60 + 30 * sin(phase + 1)).toInt().coerceIn(0, 100),
                waterLevel = (90 - i / 2).coerceIn(0, 100),
                lastUpdated = now - (limit - 1 - i) * stepMillis
            )
        }
    }

    /** Mueve [value] aleatoriamente dentro de [range] sin salirse de [min]..[max]. */
    private fun jitter(value: Int, range: Int, min: Int, max: Int): Int =
        (value + Random.nextInt(-range, range + 1)).coerceIn(min, max)
}
