package at.klu.client_wizardse2.model.request

import kotlinx.serialization.Serializable

@Serializable
data class PredictionRequest(
    val gameId: String,
    val playerId: String,
    val prediction: Int
)
