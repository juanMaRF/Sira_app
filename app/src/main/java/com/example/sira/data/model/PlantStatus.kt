package com.example.sira.data.model

/**
 * Estado en tiempo real de la planta reportado por la maceta inteligente.
 *
 * Los nombres de los campos están pensados para mapear directamente con los
 * documentos de Firestore / Realtime Database cuando conectes la nube.
 *
 * @param plantName     Nombre que el usuario le dio a la planta.
 * @param soilMoisture  Humedad de la tierra en porcentaje (0–100).
 * @param temperature   Temperatura en grados Celsius.
 * @param lightLevel    Cantidad de luz en porcentaje (0–100).
 * @param waterLevel    Nivel del depósito de agua en porcentaje (0–100).
 * @param lastUpdated   Marca de tiempo (epoch millis) de la última lectura.
 */
data class PlantStatus(
    val plantName: String = "Mi planta",
    val soilMoisture: Int = 0,
    val temperature: Float = 0f,
    val lightLevel: Int = 0,
    val waterLevel: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
