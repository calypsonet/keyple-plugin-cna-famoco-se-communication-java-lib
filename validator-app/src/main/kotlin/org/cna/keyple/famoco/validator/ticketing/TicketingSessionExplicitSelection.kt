/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.cna.keyple.famoco.validator.ticketing

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import org.eclipse.keyple.calypso.command.po.parser.ReadDataStructure
import org.eclipse.keyple.calypso.exception.NoResourceAvailableException
import org.eclipse.keyple.calypso.transaction.PoResource
import org.eclipse.keyple.calypso.transaction.PoSelectionRequest
import org.eclipse.keyple.calypso.transaction.PoSelector
import org.eclipse.keyple.calypso.transaction.PoSelector.PoAidSelector
import org.eclipse.keyple.calypso.transaction.PoTransaction
import org.eclipse.keyple.calypso.transaction.SamResource
import org.eclipse.keyple.calypso.transaction.SecuritySettings
import org.eclipse.keyple.core.selection.SeSelection
import org.eclipse.keyple.core.selection.SelectionsResult
import org.eclipse.keyple.core.seproxy.ChannelControl
import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.SeSelector.AidSelector.IsoAid
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols

class TicketingSessionExplicitSelection(poReader: SeReader, samReader: SeReader) : AbstractTicketingSession(poReader, samReader), ITicketingSession {
    /**
     * prepare the default selection
     */
    @Throws(KeypleReaderException::class)
    fun processExplicitSelection(): SelectionsResult {
        /*
         * Prepare a PO selection
         */
        seSelection = SeSelection()

        /* Select Calypso */
        val poSelectionRequest = PoSelectionRequest(
            PoSelector(
                SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                PoAidSelector(
                    IsoAid(CalypsoInfo.AID),
                    PoSelector.InvalidatedPo.REJECT
                ),
                "AID: ${CalypsoInfo.AID}"
            )
        )
        readEnvironmentHolderParserIndex = poSelectionRequest.prepareReadRecordsCmd(
            CalypsoInfo.SFI_EnvironmentAndHolder, ReadDataStructure.SINGLE_RECORD_DATA,
            CalypsoInfo.RECORD_NUMBER_1, String.format(
                "EnvironmentHolder (SFI=%02X))",
                CalypsoInfo.SFI_EnvironmentAndHolder
            )
        )
        readContractParserIndex = poSelectionRequest.prepareReadRecordsCmd(
            CalypsoInfo.SFI_Contracts,
            ReadDataStructure.SINGLE_RECORD_DATA,
            CalypsoInfo.RECORD_NUMBER_1,
            String.format("Contracts#1 (SFI=%02X))", CalypsoInfo.SFI_Contracts)
        )
        readCounterParserIndex = poSelectionRequest.prepareReadRecordsCmd(
            CalypsoInfo.SFI_Counter,
            ReadDataStructure.MULTIPLE_COUNTER,
            CalypsoInfo.RECORD_NUMBER_1,
            String.format("Counter (SFI=%02X))", CalypsoInfo.SFI_Counter)
        )
        readEventLogParserIndex = poSelectionRequest.prepareReadRecordsCmd(
            CalypsoInfo.SFI_EventLog,
            ReadDataStructure.MULTIPLE_RECORD_DATA,
            CalypsoInfo.RECORD_NUMBER_1,
            String.format("EventLog (SFI=%02X))", CalypsoInfo.SFI_EventLog)
        )

        /*
         * Add the selection case to the current selection (we could have added other cases here)
         */
        calypsoPoIndex = seSelection.prepareSelection(poSelectionRequest)
        return seSelection.processExplicitSelection(poReader)
    }

    /**
     * load the PO according to the choice provided as an argument
     *
     * @param ticketNumber
     * @return
     * @throws KeypleReaderException
     */
    @Throws(KeypleReaderException::class)
    override fun loadTickets(ticketNumber: Int): Int {
        var samResource: SamResource? = null
        return try {

            samResource = checkSamAndOpenChannel(samReader)

            if (samResource == null) {
                throw KeypleReaderException("Unable to get a Sam Resource")
            }
            /**
             * Open channel (again?)
             */
            val selectionsResult = processExplicitSelection()

            /* No sucessful selection */if (!selectionsResult.hasActiveSelection()) {
                logger.error("PO Not selected")
                return ITicketingSession.STATUS_SESSION_ERROR
            }
            val poTransaction = PoTransaction(
                PoResource(poReader, calypsoPo),
                samResource, SecuritySettings()
            )
            if (!Arrays.equals(currentPoSN, calypsoPo.applicationSerialNumber)) {
                logger.info("Load ticket status  : {}", "STATUS_CARD_SWITCHED")
                return ITicketingSession.STATUS_CARD_SWITCHED
            }
            var poProcessStatus = false

            /*
             * Open a transaction to read/write the Calypso PO
             */poProcessStatus = poTransaction.processOpening(
                PoTransaction.ModificationMode.ATOMIC,
                PoTransaction.SessionAccessLevel.SESSION_LVL_LOAD,
                0.toByte(),
                0.toByte()
            )
            if (!poProcessStatus) {
                logger.info("Load ticket status  : {}", "STATUS_SESSION_ERROR")
                return ITicketingSession.STATUS_SESSION_ERROR
            }

            /*
             * Read actual ticket number
             */poTransaction.prepareReadRecordsCmd(
                CalypsoInfo.SFI_Counter,
                ReadDataStructure.MULTIPLE_COUNTER,
                CalypsoInfo.RECORD_NUMBER_1,
                String.format("Counter (SFI=%02X))", CalypsoInfo.SFI_Counter)
            )
            poTransaction.processPoCommandsInSession()
            poTransaction.prepareIncreaseCmd(
                CalypsoInfo.SFI_Counter, 0x01.toByte(), ticketNumber,
                "Increase $ticketNumber"
            )

            /*
             * Prepare record to be sent to Calypso PO log journal
             */
            val dateFormat: DateFormat = SimpleDateFormat("yyMMdd HH:mm:ss")
            val dateTime = dateFormat.format(Date())
            var event = ""
            event = if (ticketNumber > 0) {
                pad("$dateTime OP = +$ticketNumber", ' ', 29)
            } else {
                pad("$dateTime T1", ' ', 29)
            }
            poTransaction.prepareAppendRecordCmd(
                CalypsoInfo.SFI_EventLog, event.toByteArray(),
                "Event: $event"
            )

            /*
             * Process transaction
             */poProcessStatus = poTransaction.processClosing(ChannelControl.CLOSE_AFTER)

            /*
             * End of transaction, that's it !! Only using high level Calypso Keyple API
             */if (!poProcessStatus) {
                logger.info("Load ticket status  : {}", ITicketingSession.STATUS_SESSION_ERROR)
                return ITicketingSession.STATUS_SESSION_ERROR
            }
            logger.info("Load ticket status  : {}", ITicketingSession.STATUS_OK)
            ITicketingSession.STATUS_OK
        } catch (e: KeypleReaderException) {
            e.printStackTrace()
            ITicketingSession.STATUS_UNKNOWN_ERROR
        } catch (e: NoResourceAvailableException) {
            e.printStackTrace()
            ITicketingSession.STATUS_UNKNOWN_ERROR
        }
    }
}
