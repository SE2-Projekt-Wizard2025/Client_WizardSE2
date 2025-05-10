package at.klu.client_wizardse2.model.response.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    val playerId: String,
    val playerName: String,
    val score: Int,
    val ready: Boolean,
    val tricksWon: Int = 0,
    val prediction: Int = 0
)