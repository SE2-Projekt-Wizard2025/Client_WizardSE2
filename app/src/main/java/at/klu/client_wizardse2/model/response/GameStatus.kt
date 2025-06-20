package at.klu.client_wizardse2.model.response

@kotlinx.serialization.Serializable
enum class GameStatus {
    LOBBY,
    PREDICTION,
    PLAYING,
    ROUND_END_SUMMARY,
    ENDED
}