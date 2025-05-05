package at.klu.client_wizardse2.model.response

import at.klu.client_wizardse2.model.response.dto.CardDto
import at.klu.client_wizardse2.model.response.dto.PlayerDto

@kotlinx.serialization.Serializable
data class GameResponse(
    val gameId: String,
    val status: GameStatus,
    val currentPlayerId: String? = null,
    val players: List<PlayerDto>,
    val handCards: List<CardDto>,
    val lastPlayedCard: String? = null
)
