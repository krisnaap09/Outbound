package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.OutboundRepository
import com.example.ui.screens.MainAppScaffold
import com.example.ui.theme.Navy900
import com.example.ui.theme.OutboundTheme
import com.example.ui.viewmodel.OutboundViewModel

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      OutboundTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = Navy900
        ) {
          val outboundViewModel: OutboundViewModel = viewModel(
            factory = viewModelFactory {
              initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val repository = OutboundRepository(app)
                OutboundViewModel(app, repository)
              }
            }
          )

          MainAppScaffold(viewModel = outboundViewModel)
        }
      }
    }
  }
}
