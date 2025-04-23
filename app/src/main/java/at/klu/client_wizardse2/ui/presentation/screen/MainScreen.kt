package at.klu.client_wizardse2.ui.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import at.klu.client_wizardse2.ui.presentation.sections.JoinSection
import at.klu.client_wizardse2.ui.presentation.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Wizard SE2") })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            JoinSection(viewModel)
            // later: SettingsSection(), LobbySection(), GameSection() ...
        }
    }
}

