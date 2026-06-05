// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Plugin de Firebase / Google Services. Declarado aquí pero NO aplicado a nivel raíz.
    alias(libs.plugins.google.services) apply false
}