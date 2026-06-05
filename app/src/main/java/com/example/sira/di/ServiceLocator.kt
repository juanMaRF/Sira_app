package com.example.sira.di

import com.example.sira.data.repository.AuthRepository
import com.example.sira.data.repository.FirebaseAuthRepository
import com.example.sira.data.repository.FirestorePlantRepository
import com.example.sira.data.repository.FirestorePlantsRepository
import com.example.sira.data.repository.PlantRepository
import com.example.sira.data.repository.PlantsRepository

/**
 * Inyección de dependencias mínima y centralizada.
 *
 * Para probar la UI sin Firebase, sustituye las implementaciones reales por las
 * Mock: MockPlantRepository(), MockPlantsRepository(), MockAuthRepository().
 */
object ServiceLocator {

    // Datos en tiempo real de UNA planta (estado actual + histórico).
    //val plantRepository: PlantRepository = MockPlantRepository()
    val plantRepository: PlantRepository = FirestorePlantRepository()

    // Gestión de la colección de plantas (listar / registrar / reclamar).
    //val plantsRepository: PlantsRepository = MockPlantsRepository()
    val plantsRepository: PlantsRepository = FirestorePlantsRepository()

    //val authRepository: AuthRepository = MockAuthRepository()
    val authRepository: AuthRepository = FirebaseAuthRepository(webClientId = "101085390356-dvkui6600a1ec8p6he8hib9t56qnubtt.apps.googleusercontent.com")
}
