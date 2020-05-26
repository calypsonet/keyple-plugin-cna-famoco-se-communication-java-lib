/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 */
package org.cna.keyple.famoco.validator.ticketing

import org.eclipse.keyple.calypso.transaction.SamResource
import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException
import org.slf4j.LoggerFactory
import java.util.*

class TicketingSessionManager {
    private val logger =
        LoggerFactory.getLogger(TicketingSessionManager::class.java)
    var ticketingSessions: MutableList<ITicketingSession>
    private var samResource: SamResource? = null

    @Deprecated("")
    fun setSamResource(samResource: SamResource?) {
        if (samResource == null) {
            logger.warn("--------------------------------------------")
            logger.warn("--------     SAM IS DISCONNECTED         ---")
            logger.warn("--------------------------------------------")
        } else {
            logger.info("--------------------------------------------")
            logger.info("--------     SAM IS CONNECTED         ------")
            logger.info("--------------------------------------------")
        }
        this.samResource = samResource
    }

    @Deprecated("")
    fun getSamResource(): SamResource? {
        return samResource
    }

    @get:Deprecated("")
    val isSamResourceSet: Boolean
        get() = samResource != null

    @JvmOverloads
    @Throws(KeypleReaderException::class)
    fun createTicketingSession(
        poReader: SeReader,
        samReader: SeReader?,
        explicitSelection: Boolean = false
    ): ITicketingSession {
        // if(!isSamResourceSet()){
        // logger.error("--------------------------------------------");
        // logger.error("CAN NOT CREATE TICKETING SESSION WITHOUT SAM");
        // logger.error("-------CHECK SAM CONNECTION-----------------");
        // logger.error("--------------------------------------------");
        // throw new KeypleReaderException("No sam connected");
        // }
        val ticketingSession: ITicketingSession
        if (explicitSelection) {
            logger.debug(
                "Create a new TicketingSessionExplicitSelection for reader {}",
                poReader.name
            )
            ticketingSession = TicketingSessionExplicitSelection(poReader, samReader!!)
        } else {
            ticketingSession = TicketingSession(poReader, samReader)
            logger.debug("Created a new TicketingSession for reader {}", ticketingSession)
        }
        ticketingSessions.add(ticketingSession)
        return ticketingSession
    }

    @Throws(KeypleReaderNotFoundException::class)
    fun destroyAll() {
        logger.debug("Destroy all TicketingSession")
        ticketingSessions.clear()
    }

    @Throws(KeypleReaderNotFoundException::class)
    fun destroyTicketingSession(poReaderName: String): Boolean {
        logger.debug("Destroy a the TicketingSession for reader {}", poReaderName)
        for (ticketingSession in ticketingSessions) {
            if (ticketingSession.poReader.name == poReaderName) {
                ticketingSessions.remove(ticketingSession)
                logger.debug(
                    "Session removed for reader {} - {}", ticketingSession.poReader,
                    ticketingSession.toString()
                )
                return true
            }
        }
        logger.debug("No TicketingSession found for reader {}", poReaderName)
        return false
    }

    fun getTicketingSession(poReaderName: String): ITicketingSession? {
        logger.debug("Retrieve the TicketingSession of reader {}", poReaderName)
        for (ticketingSession in ticketingSessions) {
            if (ticketingSession.poReader.name == poReaderName) {
                logger.debug("TicketingSession found for reader {}", poReaderName)
                return ticketingSession
            }
        }
        logger.debug("No TicketingSession found for reader {}", poReaderName)
        return null
    }

    fun findAll(): List<ITicketingSession> {
        return ticketingSessions
    }

    init {
        logger.info("Initialize TicketingSessionManager")
        ticketingSessions = ArrayList()
    }
}