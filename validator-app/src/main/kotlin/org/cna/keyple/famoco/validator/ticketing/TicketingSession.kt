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
import org.eclipse.keyple.calypso.command.po.parser.ReadRecordsRespPars
import org.eclipse.keyple.calypso.exception.NoResourceAvailableException
import org.eclipse.keyple.calypso.transaction.CalypsoPo
import org.eclipse.keyple.calypso.transaction.PoResource
import org.eclipse.keyple.calypso.transaction.PoSelectionRequest
import org.eclipse.keyple.calypso.transaction.PoSelector
import org.eclipse.keyple.calypso.transaction.PoSelector.PoAidSelector
import org.eclipse.keyple.calypso.transaction.PoTransaction
import org.eclipse.keyple.calypso.transaction.SamResource
import org.eclipse.keyple.calypso.transaction.SecuritySettings
import org.eclipse.keyple.core.selection.AbstractMatchingSe
import org.eclipse.keyple.core.selection.AbstractSeSelectionRequest
import org.eclipse.keyple.core.selection.SeSelection
import org.eclipse.keyple.core.selection.SelectionsResult
import org.eclipse.keyple.core.seproxy.ChannelControl
import org.eclipse.keyple.core.seproxy.MultiSeRequestProcessing
import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.SeSelector
import org.eclipse.keyple.core.seproxy.SeSelector.AidSelector
import org.eclipse.keyple.core.seproxy.SeSelector.AidSelector.IsoAid
import org.eclipse.keyple.core.seproxy.SeSelector.AtrFilter
import org.eclipse.keyple.core.seproxy.event.AbstractDefaultSelectionsResponse
import org.eclipse.keyple.core.seproxy.event.ObservableReader
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException
import org.eclipse.keyple.core.seproxy.message.SeResponse
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode
import org.eclipse.keyple.core.util.ByteArrayUtil
import timber.log.Timber

class TicketingSession(poReader: SeReader?, samReader: SeReader?) :
    AbstractTicketingSession(poReader!!, samReader!!), ITicketingSession {
    private var mifareClassicIndex = 0
    private var mifareDesfireIndex = 0
    private var bankingCardIndex = 0
    private var navigoCardIndex = 0

    /**
     * prepare the default selection
     */
    fun prepareAndSetPoDefaultSelection() {
        /*
         * Prepare a PO selection
         */
        seSelection = SeSelection(MultiSeRequestProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)

        /* Select Calypso */
        val poSelectionRequest = PoSelectionRequest(
            PoSelector(
                SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                PoAidSelector(
                    IsoAid(CalypsoInfo.AID),
                    PoSelector.InvalidatedPo.REJECT
                ),
                "AID: " + CalypsoInfo.AID
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

        /* Select Mifare Classic PO */
        val mifareClassicSelectionRequest = GenericSeSelectionRequest(
            SeSelector(
                SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC,
                AtrFilter(".*"), null, "Mifare classic"
            )
        )

        /*
         * Add the selection case to the current selection
         */
        mifareClassicIndex = seSelection.prepareSelection(mifareClassicSelectionRequest)

        /* Select Mifare Desfire PO */
        val mifareDesfireSelectionRequest = GenericSeSelectionRequest(
            SeSelector(
                SeCommonProtocols.PROTOCOL_MIFARE_DESFIRE,
                AtrFilter(".*"), null, "Mifare desfire"
            )
        )

        /*
         * Add the selection case to the current selection
         */mifareDesfireIndex = seSelection.prepareSelection(mifareDesfireSelectionRequest)
        val bankingCardSelectionRequest = GenericSeSelectionRequest(
            SeSelector(
                SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                AidSelector(
                    IsoAid("325041592e5359532e4444463031"),
                    null, AidSelector.FileOccurrence.FIRST,
                    AidSelector.FileControlInformation.FCI
                ),
                "EMV"
            )
        )

        /*
         * Add the selection case to the current selection
         */bankingCardIndex = seSelection.prepareSelection(bankingCardSelectionRequest)
        val naviogCardSelectionRequest = GenericSeSelectionRequest(
            SeSelector(
                SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                AidSelector(
                    IsoAid("A0000004040125090101"), null,
                    AidSelector.FileOccurrence.FIRST,
                    AidSelector.FileControlInformation.FCI
                ),
                "NAVIGO"
            )
        )

        /*
         * Add the selection case to the current selection
         */navigoCardIndex = seSelection.prepareSelection(naviogCardSelectionRequest)

        /*
         * Provide the SeReader with the selection operation to be processed when a PO is inserted.
         */(poReader as ObservableReader).setDefaultSelectionRequest(
            seSelection.selectionOperation, ObservableReader.NotificationMode.ALWAYS
        )
    }

    fun processDefaultSelection(
        selectionResponse: AbstractDefaultSelectionsResponse?
    ): SelectionsResult {
        val selectionsResult: SelectionsResult
        logger.info("selectionResponse = {}", selectionResponse)
        selectionsResult = seSelection.processDefaultSelection(selectionResponse)
        if (selectionsResult.hasActiveSelection()) {
            val selectionIndex =
                selectionsResult.matchingSelections[0].selectionIndex
            if (selectionIndex == calypsoPoIndex) {
                calypsoPo = selectionsResult.activeSelection.matchingSe as CalypsoPo
                poTypeName = "CALYPSO"
                readEnvironmentHolderParser = selectionsResult
                    .activeSelection
                    .getResponseParser(readEnvironmentHolderParserIndex) as ReadRecordsRespPars
                readEventLogParser = selectionsResult.activeSelection
                    .getResponseParser(readEventLogParserIndex) as ReadRecordsRespPars
                readCounterParser = selectionsResult.activeSelection
                    .getResponseParser(readCounterParserIndex) as ReadRecordsRespPars
                readContractParser = selectionsResult.activeSelection
                    .getResponseParser(readContractParserIndex) as ReadRecordsRespPars
            } else if (selectionIndex == mifareClassicIndex) {
                poTypeName = "MIFARE Classic"
            } else if (selectionIndex == mifareDesfireIndex) {
                poTypeName = "MIFARE Desfire"
            } else if (selectionIndex == bankingCardIndex) {
                poTypeName = "EMV"
            } else if (selectionIndex == navigoCardIndex) {
                poTypeName = "NAVIGO"
            } else {
                poTypeName = "OTHER"
            }
        }
        logger.info("PO type = {}", poTypeName)
        return selectionsResult
    }

    /**
     * do the personalization of the PO according to the specified profile
     *
     * @param profile
     * @return
     */
    fun personalize(profile: String): Boolean {
        var samResource: SamResource? = null
        try {
            /*
             * Allocate a Sam Resource
             */
            samResource = checkSamAndOpenChannel(samReader)
            if (samResource == null) {
                throw KeypleReaderException("Unable to get a Sam Resource")
            }
            val poTransaction = PoTransaction(
                PoResource(poReader, calypsoPo),
                samResource, SecuritySettings()
            )
            var poProcessStatus = false
            poProcessStatus = poTransaction.processOpening(
                PoTransaction.ModificationMode.ATOMIC,
                PoTransaction.SessionAccessLevel.SESSION_LVL_PERSO,
                0.toByte(),
                0.toByte()
            )
            if ("PROFILE1" == profile) {
                poTransaction.prepareUpdateRecordCmd(
                    CalypsoInfo.SFI_EnvironmentAndHolder,
                    CalypsoInfo.RECORD_NUMBER_1, pad("John Smith", ' ', 29).toByteArray(),
                    "HolderName: John Smith"
                )
                poTransaction.prepareUpdateRecordCmd(
                    CalypsoInfo.SFI_Contracts,
                    CalypsoInfo.RECORD_NUMBER_1, pad("NO CONTRACT", ' ', 29).toByteArray(),
                    "Contract: NO CONTRACT"
                )
            } else {
                poTransaction.prepareUpdateRecordCmd(
                    CalypsoInfo.SFI_EnvironmentAndHolder,
                    CalypsoInfo.RECORD_NUMBER_1, pad("Harry Potter", ' ', 29).toByteArray(),
                    "HolderName: Harry Potter"
                )
                poTransaction.prepareUpdateRecordCmd(
                    CalypsoInfo.SFI_Contracts,
                    CalypsoInfo.RECORD_NUMBER_1,
                    pad("1 MONTH SEASON TICKET", ' ', 29).toByteArray(),
                    "Contract: 1 MONTH SEASON TICKET"
                )
            }
            val dateFormat: DateFormat = SimpleDateFormat("yyMMdd HH:mm:ss")
            val dateTime = dateFormat.format(Date())
            poTransaction.prepareAppendRecordCmd(
                CalypsoInfo.SFI_EventLog,
                pad("$dateTime OP = PERSO", ' ', 29).toByteArray(), "Event: blank"
            )
            poTransaction.prepareUpdateRecordCmd(
                CalypsoInfo.SFI_Counter,
                CalypsoInfo.RECORD_NUMBER_1, ByteArrayUtil.fromHex(pad("", '0', 29 * 2)),
                "Reset all counters"
            )
            poProcessStatus = poTransaction.processClosing(ChannelControl.CLOSE_AFTER)
            return poProcessStatus
        } catch (e: KeypleReaderException) {
            e.printStackTrace()
            return false
        } catch (e: NoResourceAvailableException) {
            e.printStackTrace()
        }
        return false
    }
    /*
     * public void forceCloseChannel() throws KeypleReaderException {
     * logger.debug("Force close logical channel (hack for nfc reader)"); List<ApduRequest>
     * requestList = new ArrayList<>(); ((ProxyReader)poReader).transmit(new
     * SeRequest(requestList)); }
     */
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
            /*
             * Allocate a Sam Resource
             */
            samResource = checkSamAndOpenChannel(samReader)
            if (samResource == null) {
                throw KeypleReaderException("Unable to get a Sam Resource")
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
             */
            poProcessStatus = poTransaction.processClosing(ChannelControl.CLOSE_AFTER)

            /*
             * End of transaction, that's it !! Only using high level Calypso Keyple API
             */if (!poProcessStatus || !poTransaction.isSuccessful) {
                logger.info("Load ticket status  : {}", "STATUS_SESSION_ERROR")
                return ITicketingSession.STATUS_SESSION_ERROR
            }
            logger.info("Load ticket status  : {}", "STATUS_OK")
            ITicketingSession.STATUS_OK
        } catch (e: KeypleReaderException) {
            Timber.e(e)
            ITicketingSession.STATUS_UNKNOWN_ERROR
        } catch (e: NoResourceAvailableException) {
            Timber.e(e)
            ITicketingSession.STATUS_UNKNOWN_ERROR
        }
    }

    fun debitTickets(ticketNumber: Int): Int {
        var samResource: SamResource? = null
        return try {
            /*
             * Allocate a Sam Resource
             */
            samResource = checkSamAndOpenChannel(samReader)
            if (samResource == null) {
                throw KeypleReaderException("Unable to get a Sam Resource")
            }
            val poTransaction = PoTransaction(
                PoResource(poReader, calypsoPo), samResource, SecuritySettings()
            )
            if (!Arrays.equals(currentPoSN, calypsoPo.applicationSerialNumber)) {
                logger.info("Validate ticket status  : {}", "STATUS_CARD_SWITCHED")
                return ITicketingSession.STATUS_CARD_SWITCHED
            }

            /*
             * Open a transaction to read/write the Calypso PO
             */
            var poProcessStatus = poTransaction.processOpening(
                PoTransaction.ModificationMode.ATOMIC,
                PoTransaction.SessionAccessLevel.SESSION_LVL_LOAD,
                0,
                0)

            if (!poProcessStatus) {
                logger.info("Validate ticket status  : {}", "STATUS_SESSION_ERROR")
                return ITicketingSession.STATUS_SESSION_ERROR
            }

            /* allow to determine the anticipated response */
            poTransaction.prepareReadRecordsCmd(
                CalypsoInfo.SFI_Counter,
                ReadDataStructure.MULTIPLE_COUNTER,
                CalypsoInfo.RECORD_NUMBER_1,
                String.format("Counter (SFI=%02X))", CalypsoInfo.SFI_Counter)
            )
            poTransaction.processPoCommandsInSession()

            /*
             * Prepare decrease command
             */
            poTransaction.prepareDecreaseCmd(CalypsoInfo.SFI_Counter, 0x01, 1, "Decrease counter")

            /*
             * Process transaction
             */
            poProcessStatus = poTransaction.processClosing(ChannelControl.CLOSE_AFTER)

            /*
             * End of transaction, that's it !! Only using high level Calypso Keyple API
             */if (!poProcessStatus || !poTransaction.isSuccessful) {
                logger.info("Load ticket status  : {}", "STATUS_SESSION_ERROR")
                return ITicketingSession.STATUS_SESSION_ERROR
            }
            logger.info("Load ticket status  : {}", "STATUS_OK")
            ITicketingSession.STATUS_OK
        } catch (e: KeypleReaderException) {
            Timber.e(e)
            ITicketingSession.STATUS_UNKNOWN_ERROR
        } catch (e: NoResourceAvailableException) {
            Timber.e(e)
            ITicketingSession.STATUS_UNKNOWN_ERROR
        }
    }

    /**
     * Load a season ticket contract
     *
     * @return
     * @throws KeypleReaderException
     */
    @Throws(KeypleReaderException::class)
    fun loadContract(): Int {
        var samResource: SamResource? = null
        return try {
            /*
             * Allocate a Sam Resource
             */
            samResource = checkSamAndOpenChannel(samReader)
            if (samResource == null) {
                throw KeypleReaderException("Unable to get a Sam Resource")
            }
            val poTransaction = PoTransaction(
                PoResource(poReader, calypsoPo),
                samResource, SecuritySettings()
            )
            if (!Arrays.equals(currentPoSN, calypsoPo.applicationSerialNumber)) {
                return ITicketingSession.STATUS_CARD_SWITCHED
            }
            var poProcessStatus = false
            poProcessStatus = poTransaction.processOpening(
                PoTransaction.ModificationMode.ATOMIC,
                PoTransaction.SessionAccessLevel.SESSION_LVL_LOAD,
                0.toByte(),
                0.toByte()
            )
            if (!poProcessStatus) {
                return ITicketingSession.STATUS_SESSION_ERROR
            }

            /* allow to determine the anticipated response */poTransaction.prepareReadRecordsCmd(
                CalypsoInfo.SFI_Counter,
                ReadDataStructure.MULTIPLE_COUNTER,
                CalypsoInfo.RECORD_NUMBER_1,
                String.format("Counter (SFI=%02X))", CalypsoInfo.SFI_Counter)
            )
            poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)
            poTransaction.prepareUpdateRecordCmd(
                CalypsoInfo.SFI_Contracts,
                CalypsoInfo.RECORD_NUMBER_1, pad("1 MONTH SEASON TICKET", ' ', 29).toByteArray(),
                "Contract: 1 MONTH SEASON TICKET"
            )

            // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("");
            // String dateTime = LocalDateTime.now().format(formatter);
            val dateFormat: DateFormat = SimpleDateFormat("yyMMdd HH:mm:ss")
            val event =
                pad(dateFormat.format(Date()) + " OP = +ST", ' ', 29)
            poTransaction.prepareAppendRecordCmd(
                CalypsoInfo.SFI_EventLog, event.toByteArray(),
                "Event: $event"
            )
            poProcessStatus = poTransaction.processClosing(ChannelControl.CLOSE_AFTER)
            if (!poProcessStatus) {
                ITicketingSession.STATUS_SESSION_ERROR
            } else ITicketingSession.STATUS_OK
        } catch (e: KeypleReaderException) {
            e.printStackTrace()
            ITicketingSession.STATUS_UNKNOWN_ERROR
        } catch (e: NoResourceAvailableException) {
            e.printStackTrace()
            ITicketingSession.STATUS_UNKNOWN_ERROR
        }
    }

    /**
     * Create a new class extending AbstractSeSelectionRequest
     */
    inner class GenericSeSelectionRequest(seSelector: SeSelector) :
        AbstractSeSelectionRequest(seSelector) {
        var transmissionMode: TransmissionMode
        override fun parse(seResponse: SeResponse): AbstractMatchingSe {
            class GenericMatchingSe(
                selectionResponse: SeResponse?,
                transmissionMode: TransmissionMode?,
                extraInfo: String?
            ) : AbstractMatchingSe(selectionResponse, transmissionMode, extraInfo)
            return GenericMatchingSe(seResponse, transmissionMode, "Generic Matching SE")
        }

        init {
            transmissionMode = seSelector.seProtocol.transmissionMode
        }
    }

    /*
     * Should be instanciated through the ticketing session mananger
     */
    init {
        prepareAndSetPoDefaultSelection()
    }
}
