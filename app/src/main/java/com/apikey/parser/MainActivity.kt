package com.apikey.parser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.apikey.parser.ui.theme.ApiKeyParserTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ApiKeyParserTheme {
                androidx.compose.material3.Text("API Key Parser")
            }
        }
    }
}
