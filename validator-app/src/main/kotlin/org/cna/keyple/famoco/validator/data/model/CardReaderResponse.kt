package org.cna.keyple.famoco.validator.data.model

data class CardReaderResponse(
    val status: Status,
    val ticketsNumber: Int,
    val contract: String,
    val cardType: String
)