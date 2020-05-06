/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 ********************************************************************************/
package org.cna.keyple.famoco.example.model

open class EventModel(val type: Int, val text: String) {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ACTION = 1
        const val TYPE_RESULT = 2
        const val TYPE_MULTICHOICE = 3
    }
}
