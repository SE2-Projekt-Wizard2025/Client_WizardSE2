package at.klu.client_wizardse2.model.response

@kotlinx.serialization.Serializable
enum class GameStatus {
    LOBBY,
    PLAYING,
    ENDED
}