package org.cna.keyple.famoco.validator.data.model

enum class Status
/**
 * @param status
 */(private val status: String) {
    LOADING("loading"), ERROR("error"), TICKETS_FOUND("tickets_found"), INVALID_CARD("invalid_card"), EMPTY_CARD(
        "empty_card"
    ),
    SUCCESS("success");

    override fun toString(): String {
        return status
    }

    companion object {
        @JvmStatic
        fun getStatus(name: String): Status {
            return try {
                valueOf(name.toUpperCase())
            } catch (e: Exception) {
                // If the given state does not exist, return the default value.
                ERROR
            }
        }
    }

}