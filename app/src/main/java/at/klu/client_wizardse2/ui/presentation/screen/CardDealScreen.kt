package at.klu.client_wizardse2.ui.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import at.klu.client_wizardse2.model.response.dto.CardDto
import at.klu.client_wizardse2.ui.presentation.viewmodels.MainViewModel

@Composable
fun CardDealScreen(viewModel: MainViewModel) {
    val handCards = viewModel.gameResponse?.handCards ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎴 Deine Handkarten", style = MaterialTheme.typography.headlineMedium)

        if (handCards.isEmpty()) {
            Text("Keine Karten erhalten oder noch nicht verteilt.")
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(handCards) { card ->
                    CardView(card)
                }
            }
        }
    }
}

@Composable
fun CardView(card: CardDto) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .height(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = card.color ?: "?", style = MaterialTheme.typography.labelMedium)
            Text(text = card.value ?: "?", style = MaterialTheme.typography.titleLarge)
        }
    }
}