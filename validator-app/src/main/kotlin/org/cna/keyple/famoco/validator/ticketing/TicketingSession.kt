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
import org.eclipse.keyple.calypso.command.po.exception.CalypsoPoCommandException
import org.eclipse.keyple.calypso.command.sam.exception.CalypsoSamCommandException
import org.eclipse.keyple.calypso.transaction.CalypsoPo
import org.eclipse.keyple.calypso.transaction.PoResource
import org.eclipse.keyple.calypso.transaction.PoSelectionRequest
import org.eclipse.keyple.calypso.transaction.PoSelector
import org.eclipse.keyple.calypso.transaction.PoTransaction
import org.eclipse.keyple.calypso.transaction.exception.CalypsoPoTransactionException
import org.eclipse.keyple.core.command.AbstractApduCommandBuilder
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

class TicketingSession(poReader: SeReader, samReader: SeReader?) :
    AbstractTicketingSession(poReader, samReader), ITicketingSession {
    private var mifareClassicIndex = 0
    private var mifareDesfireIndex = 0
    private var bankingCardIndex = 0
    private var navigoCardIndex = 0

    /*
    * Should be instanciated through the ticketing session mananger
    */
    init {
        prepareAndSetPoDefaultSelection()
    }

    /**
     * prepare the default selection
     */
    fun prepareAndSetPoDefaultSelection() {
        /*
         * Prepare a PO selection
         */
        seSelection = SeSelection(MultiSeRequestProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)

        /* Select Calypso */
        val poSelectionRequest = PoSelectionRequest(PoSelector(SeCommonProtocols.PROTOCOL_ISO14443_4, null,
            AidSelector(IsoAid(CalypsoInfo.AID)), PoSelector.InvalidatedPo.REJECT))

        // Prepare the reading of the Environment and Holder file.
        poSelectionRequest.prepareReadRecordFile(CalypsoInfo.SFI_EnvironmentAndHolder, CalypsoInfo.RECORD_NUMBER_1.toInt())
        poSelectionRequest.prepareReadRecordFile(CalypsoInfo.SFI_Contracts, CalypsoInfo.RECORD_NUMBER_1.toInt())
        poSelectionRequest.prepareReadRecordFile(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1.toInt())
        poSelectionRequest.prepareReadRecordFile(CalypsoInfo.SFI_EventLog, CalypsoInfo.RECORD_NUMBER_1.toInt())

        /*
         * Add the selection case to the current selection (we could have added other cases here)
         */
        calypsoPoIndex = seSelection.prepareSelection(poSelectionRequest)

        /* Select Mifare Classic PO */
        val mifareClassicSelectionRequest = GenericSeSelectionRequest(
            SeSelector(SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC, AtrFilter(".*"), null)
        )

        /*
         * Add the selection case to the current selection
         */
        mifareClassicIndex = seSelection.prepareSelection(mifareClassicSelectionRequest)

        /* Select Mifare Desfire PO */
        val mifareDesfireSelectionRequest = GenericSeSelectionRequest(SeSelector(SeCommonProtocols.PROTOCOL_MIFARE_DESFIRE, AtrFilter(".*"), null))

        /*
         * Add the selection case to the current selection
         */mifareDesfireIndex = seSelection.prepareSelection(mifareDesfireSelectionRequest)
        val bankingCardSelectionRequest = GenericSeSelectionRequest(
            SeSelector(SeCommonProtocols.PROTOCOL_ISO14443_4, null, AidSelector(
                    IsoAid("325041592e5359532e4444463031"),
                    AidSelector.FileOccurrence.FIRST,
                    AidSelector.FileControlInformation.FCI
                )
            )
        )

        /*
         * Add the selection case to the current selection
         */
        bankingCardIndex = seSelection.prepareSelection(bankingCardSelectionRequest)
        val naviogCardSelectionRequest = GenericSeSelectionRequest(
            SeSelector(
                SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                AidSelector(
                    IsoAid("A0000004040125090101"),
                    AidSelector.FileOccurrence.FIRST,
                    AidSelector.FileControlInformation.FCI
                )
            )
        )

        /*
         * Add the selection case to the current selection
         */
        navigoCardIndex = seSelection.prepareSelection(naviogCardSelectionRequest)

        /*
         * Provide the SeReader with the selection operation to be processed when a PO is inserted.
         */
        (poReader as ObservableReader).setDefaultSelectionRequest(
            seSelection.selectionOperation, ObservableReader.NotificationMode.ALWAYS
        )
    }

    fun processDefaultSelection(selectionResponse: AbstractDefaultSelectionsResponse?): SelectionsResult {
        Timber.i("selectionResponse = $selectionResponse")
        val selectionsResult: SelectionsResult = seSelection.processDefaultSelection(selectionResponse)
        if (selectionsResult.hasActiveSelection()) {
            val selectionIndex = selectionsResult.matchingSelections.keys.first()
            if (selectionIndex == calypsoPoIndex) {
                calypsoPo = selectionsResult.activeMatchingSe as CalypsoPo
                poTypeName = "CALYPSO"
                efEnvironmentHolder = calypsoPo.getFileBySfi(CalypsoInfo.SFI_EnvironmentAndHolder)
                efEventLog = calypsoPo.getFileBySfi(CalypsoInfo.SFI_EventLog)
                efCounter = calypsoPo.getFileBySfi(CalypsoInfo.SFI_Counter)
                efContractParser = calypsoPo.getFileBySfi(CalypsoInfo.SFI_Contracts)
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
        Timber.i("PO type = $poTypeName")
        return selectionsResult
    }

    /**
     * do the personalization of the PO according to the specified profile
     *
     * @param profile
     * @return
     */
    @Throws(CalypsoPoTransactionException::class, CalypsoPoCommandException::class, CalypsoSamCommandException::class)
    fun personalize(profile: String): Boolean {
        try {
            // Should block poTransaction without Sam?
            val poTransaction = if (samReader != null)
                    PoTransaction(PoResource(poReader, calypsoPo), getSecuritySettings(checkSamAndOpenChannel(samReader)))
                else
                    PoTransaction(PoResource(poReader, calypsoPo))
            poTransaction.processOpening(PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_PERSO)

            if ("PROFILE1" == profile) {
                poTransaction.prepareUpdateRecord(CalypsoInfo.SFI_EnvironmentAndHolder, CalypsoInfo.RECORD_NUMBER_1, pad("John Smith", ' ', 29).toByteArray())
                poTransaction.prepareUpdateRecord(CalypsoInfo.SFI_Contracts, CalypsoInfo.RECORD_NUMBER_1, pad("NO CONTRACT", ' ', 29).toByteArray())
            } else {
                poTransaction.prepareUpdateRecord(CalypsoInfo.SFI_EnvironmentAndHolder, CalypsoInfo.RECORD_NUMBER_1, pad("Harry Potter", ' ', 29).toByteArray())
                poTransaction.prepareUpdateRecord(CalypsoInfo.SFI_Contracts, CalypsoInfo.RECORD_NUMBER_1, pad("1 MONTH SEASON TICKET", ' ', 29).toByteArray())
            }
            val dateFormat: DateFormat = SimpleDateFormat("yyMMdd HH:mm:ss")
            val dateTime = dateFormat.format(Date())
            poTransaction.prepareAppendRecord(CalypsoInfo.SFI_EventLog, pad("$dateTime OP = PERSO", ' ', 29).toByteArray())
            poTransaction.prepareUpdateRecord(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1, ByteArrayUtil.fromHex(pad("", '0', 29 * 2)))
            poTransaction.processClosing(ChannelControl.CLOSE_AFTER)
            return true
        } catch (e: CalypsoPoTransactionException) {
            Timber.e(e)
        } catch (e: CalypsoPoCommandException) {
            Timber.e(e)
        } catch (e: CalypsoSamCommandException) {
            Timber.e(e)
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

        return try {
            // Should block poTransaction without Sam?
            val poTransaction = if (samReader != null)
                PoTransaction(PoResource(poReader, calypsoPo), getSecuritySettings(checkSamAndOpenChannel(samReader)))
            else
                PoTransaction(PoResource(poReader, calypsoPo))

            if (!Arrays.equals(currentPoSN, calypsoPo.applicationSerialNumber)) {
                Timber.i("Load ticket status  : STATUS_CARD_SWITCHED")
                return ITicketingSession.STATUS_CARD_SWITCHED
            }
            /*
             * Open a transaction to read/write the Calypso PO
             */
            poTransaction.processOpening(PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_LOAD)

            /*
             * Read actual ticket number
             */
            poTransaction.prepareReadRecordFile(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1.toInt())
            poTransaction.processPoCommandsInSession()
            poTransaction.prepareIncrease(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1, ticketNumber)

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
            poTransaction.prepareAppendRecord(CalypsoInfo.SFI_EventLog, event.toByteArray())

            /*
             * Process transaction
             */
            poTransaction.processClosing(ChannelControl.CLOSE_AFTER)
            Timber.i("Load ticket status  : STATUS_OK")
            ITicketingSession.STATUS_OK
        } catch (e: CalypsoSamCommandException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        } catch (e: CalypsoPoCommandException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        } catch (e: CalypsoPoTransactionException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        }
    }

    fun debitTickets(ticketNumber: Int): Int {
        return try {
            // Should block poTransaction without Sam?

            val poTransaction =
                if (samReader != null)
                    PoTransaction(PoResource(poReader, calypsoPo), getSecuritySettings(checkSamAndOpenChannel(samReader)))
                else
                    PoTransaction(PoResource(poReader, calypsoPo))

            /*
             * Open a transaction to read/write the Calypso PO
             */
            poTransaction.processOpening(PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_DEBIT)

            /* allow to determine the anticipated response */
            poTransaction.prepareReadRecordFile(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1.toInt())
            poTransaction.processPoCommandsInSession()

            /*
             * Prepare decrease command
             */
            poTransaction.prepareDecrease(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1, 1)

            /*
             * Process transaction
             */
            poTransaction.processClosing(ChannelControl.CLOSE_AFTER)

            Timber.i("Load ticket status  : STATUS_OK")
            ITicketingSession.STATUS_OK
        } catch (e: CalypsoSamCommandException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        } catch (e: CalypsoPoCommandException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        } catch (e: CalypsoPoTransactionException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
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
        return try {
            // Should block poTransaction without Sam?
            val poTransaction = if (samReader != null)
                PoTransaction(PoResource(poReader, calypsoPo), getSecuritySettings(checkSamAndOpenChannel(samReader)))
            else
                PoTransaction(PoResource(poReader, calypsoPo))

            if (!Arrays.equals(currentPoSN, calypsoPo.applicationSerialNumber)) {
                return ITicketingSession.STATUS_CARD_SWITCHED
            }

            poTransaction.processOpening(PoTransaction.SessionSetting.AccessLevel.SESSION_LVL_LOAD)

            /* allow to determine the anticipated response */
            poTransaction.prepareReadRecordFile(CalypsoInfo.SFI_Counter, CalypsoInfo.RECORD_NUMBER_1.toInt())
            poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)
            poTransaction.prepareUpdateRecord(CalypsoInfo.SFI_Contracts, CalypsoInfo.RECORD_NUMBER_1, pad("1 MONTH SEASON TICKET", ' ', 29).toByteArray())

            // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("");
            // String dateTime = LocalDateTime.now().format(formatter);
            val dateFormat: DateFormat = SimpleDateFormat("yyMMdd HH:mm:ss")
            val event =
                pad(dateFormat.format(Date()) + " OP = +ST", ' ', 29)
            poTransaction.prepareAppendRecord(CalypsoInfo.SFI_EventLog, event.toByteArray())
            poTransaction.processClosing(ChannelControl.CLOSE_AFTER)
            ITicketingSession.STATUS_OK
        } catch (e: CalypsoSamCommandException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        } catch (e: CalypsoPoCommandException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        } catch (e: CalypsoPoTransactionException) {
            Timber.e(e)
            ITicketingSession.STATUS_SESSION_ERROR
        }
    }

    /**
     * Create a new class extending AbstractSeSelectionRequest
     */
    inner class GenericSeSelectionRequest(seSelector: SeSelector) : AbstractSeSelectionRequest<AbstractApduCommandBuilder>(seSelector) {
        private val transmissionMode = seSelector.seProtocol.transmissionMode
        override fun parse(seResponse: SeResponse): AbstractMatchingSe {
            class GenericMatchingSe(selectionResponse: SeResponse?, transmissionMode: TransmissionMode?) : AbstractMatchingSe(selectionResponse, transmissionMode)
            return GenericMatchingSe(seResponse, transmissionMode)
        }
    }
}
