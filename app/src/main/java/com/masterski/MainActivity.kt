package com.masterski

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.masterski.ui.MasterSkiNavHost
import com.masterski.ui.theme.MasterSkiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MasterSkiTheme { MasterSkiNavHost() } }
    }
}
