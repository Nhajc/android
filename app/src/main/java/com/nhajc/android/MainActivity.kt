package com.nhajc.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nhajc.android.ui.search.SearchRoute
import com.nhajc.android.ui.theme.NhajcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NhajcTheme {
                NhajcApp()
            }
        }
    }
}

@Composable
fun NhajcApp() {
    val application = LocalContext.current.applicationContext as NhajcApplication
    SearchRoute(application.searchSession)
}
