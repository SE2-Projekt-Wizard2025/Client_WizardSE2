package at.klu.client_wizardse2.ui.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import at.klu.client_wizardse2.model.response.dto.CardDto
import kotlin.collections.listOf


@Composable
fun CardView(card: CardDto, onCardClick: ((String) -> Unit)? = null) {
    val backgroundColor = when (card.color?.uppercase()) {
        "RED" -> Color.Red
        "BLUE" -> Color.Blue
        "GREEN" -> Color.Green
        "YELLOW" -> Color.Yellow
        else -> Color.LightGray
    }

    val textColor = if (card.color?.uppercase() in listOf("YELLOW", "GREEN")) Color.Black else Color.White

    val actualCardString = when (card.type) {
        "WIZARD" -> "WIZARD"
        "JESTER" -> "JESTER"
        else -> "${card.color}_${card.value}"
    }

    val cardModifier = Modifier
        .width(80.dp)
        .height(120.dp)
        .then(
            if (onCardClick != null) Modifier.clickable { onCardClick(actualCardString) }
            else Modifier
        )

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
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
                color = textColor
            )
            Text(
                text = when (card.type) {
                    "WIZARD", "JESTER" -> ""
                    else -> card.value ?: "?"
                },
                style = MaterialTheme.typography.titleLarge,
                color = textColor
            )
        }
    }
}

fun String.toCardDto(): CardDto? {
    return when {
        equals("WIZARD", ignoreCase = true) -> CardDto(
            color = "SPECIAL",
            value = "0",
            type = "WIZARD"
        )

        equals("JESTER", ignoreCase = true) -> CardDto(
            color = "SPECIAL",
            value = "0",
            type = "JESTER"
        )

        contains("_") -> {
            val parts = this.split("_")
            if (parts.size == 2) {
                CardDto(color = parts[0], value = parts[1], type = "NUMBER")
            } else null
        }

        else -> null
    }
}
