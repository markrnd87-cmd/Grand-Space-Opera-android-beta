package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.sim.SpaceOperaViewModel
import com.example.ui.GrandSpaceOperaApp
import com.example.ui.theme.GrandSpaceOperaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SpaceOperaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GrandSpaceOperaTheme {
                GrandSpaceOperaApp(viewModel = viewModel)
            }
        }
    }
}
