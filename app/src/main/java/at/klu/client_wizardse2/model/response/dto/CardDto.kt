package at.klu.client_wizardse2.model.response.dto

import kotlinx.serialization.Serializable

@Serializable
data class CardDto(
    val color: String,
    val value: String,
    val type: String
)