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
import androidx.compose.ui.graphics.Color

@Composable
fun CardDealScreen(viewModel: MainViewModel) {
    val handCards = viewModel.gameResponse?.handCards ?: emptyList()
    val trumpCard = viewModel.gameResponse?.trumpCard

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (trumpCard != null) {
            Text("🃏 Trumpfkarte", style = MaterialTheme.typography.headlineSmall)
            CardView(trumpCard)
        } else {
            Text("Noch keine Trumpfkarte bekannt.", style = MaterialTheme.typography.bodyMedium)
        }

        // Handkarten
        Text("🎴 Deine Handkarte${if (handCards.size > 1) "n" else ""}", style = MaterialTheme.typography.headlineSmall)

        if (handCards.isEmpty()) {
            Text("Keine Karten erhalten oder noch nicht verteilt.")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(handCards) { card ->
                    CardView(card)
                }
            }
        }
    }
}

@Composable
fun CardView(card: CardDto) {
    val backgroundColor = when (card.color?.uppercase()) {
        "RED" -> Color.Red
        "BLUE" -> Color.Blue
        "GREEN" -> Color.Green
        "YELLOW" -> Color.Yellow
        else -> Color.LightGray
    }

    Card(
        modifier = Modifier
            .width(80.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (card.type) {
                    "WIZARD" -> "Wizard"
                    "JESTER" -> "Narr"
                    else -> card.color ?: "?"
                },
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Text(
                text = when (card.type) {
                    "WIZARD", "JESTER" -> ""
                    else -> card.value ?: "?"
                },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }
    }
}