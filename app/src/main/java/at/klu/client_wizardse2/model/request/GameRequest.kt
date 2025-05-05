package at.klu.client_wizardse2.model.request

@kotlinx.serialization.Serializable
data class GameRequest(
    val gameId: String,
    val playerId: String,
    val playerName: String? = null,
    val card: String? = null,
    val action: String? = null
)
