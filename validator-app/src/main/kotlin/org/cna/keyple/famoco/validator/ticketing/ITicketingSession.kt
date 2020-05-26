/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 */
package org.cna.keyple.famoco.validator.ticketing

import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException

interface ITicketingSession {
    val poReader: SeReader
    val cardContent: CardContent
    val poTypeName: String?
    fun analyzePoProfile(): Boolean
    val poIdentification: String?

    @Throws(KeypleReaderException::class)
    fun loadTickets(ticketNumber: Int): Int
    fun notifySeProcessed()

    companion object {
        const val STATUS_OK = 0
        const val STATUS_UNKNOWN_ERROR = 1
        const val STATUS_CARD_SWITCHED = 2
        const val STATUS_SESSION_ERROR = 3
    }
}