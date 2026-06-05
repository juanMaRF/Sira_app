package com.example.sira

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.sira.ui.navigation.SiraNavGraph
import com.example.sira.ui.theme.SiraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiraApp()
        }
    }
}

@Composable
fun SiraApp() {
    SiraTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            SiraNavGraph(modifier = Modifier.padding(innerPadding))
        }
    }
}
