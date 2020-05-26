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
package org.cna.keyple.famoco.example.activity

import android.view.MenuItem
import androidx.core.view.GravityCompat
import kotlinx.android.synthetic.main.activity_main.drawerLayout
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cna.keyple.famoco.example.R
import org.cna.keyple.famoco.example.util.CalypsoClassicInfo
import org.eclipse.keyple.calypso.command.po.parser.ReadDataStructure
import org.eclipse.keyple.calypso.command.po.parser.ReadRecordsRespPars
import org.eclipse.keyple.calypso.command.sam.SamRevision
import org.eclipse.keyple.calypso.transaction.CalypsoPo
import org.eclipse.keyple.calypso.transaction.CalypsoSam
import org.eclipse.keyple.calypso.transaction.PoResource
import org.eclipse.keyple.calypso.transaction.PoSelectionRequest
import org.eclipse.keyple.calypso.transaction.PoSelector
import org.eclipse.keyple.calypso.transaction.PoTransaction
import org.eclipse.keyple.calypso.transaction.SamResource
import org.eclipse.keyple.calypso.transaction.SamSelectionRequest
import org.eclipse.keyple.calypso.transaction.SamSelector
import org.eclipse.keyple.calypso.transaction.SecuritySettings
import org.eclipse.keyple.core.selection.SeSelection
import org.eclipse.keyple.core.seproxy.ChannelControl
import org.eclipse.keyple.core.seproxy.MultiSeRequestProcessing
import org.eclipse.keyple.core.seproxy.SeProxyService
import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.SeSelector
import org.eclipse.keyple.core.seproxy.event.AbstractDefaultSelectionsResponse
import org.eclipse.keyple.core.seproxy.event.ObservableReader
import org.eclipse.keyple.core.seproxy.event.ReaderEvent
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException
import org.eclipse.keyple.core.seproxy.exception.KeyplePluginNotFoundException
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols
import org.eclipse.keyple.core.util.ByteArrayUtil
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPlugin
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPluginFactory
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactory
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcProtocolSettings
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import timber.log.Timber

class MainActivity : AbstractExampleActivity() {

    private lateinit var poReader: AndroidNfcReader
    private lateinit var samReader: SeReader
    private lateinit var seSelection: SeSelection
    private var readEnvironmentParserIndex: Int = 0

    private val securitySettings = SecuritySettings()

    private enum class TransactionType {
        DECREASE,
        INCREASE
    }

    override fun initContentView() {
        setContentView(R.layout.activity_main)
        initActionBar(toolbar, "Keyple demo", "Famoco Plugin")
    }

    override fun initReaders() {
        // Initialize SEProxy with Android Plugins
        SeProxyService.getInstance().registerPlugin(AndroidNfcPluginFactory())
        SeProxyService.getInstance().registerPlugin(AndroidFamocoPluginFactory())

        // Configuration of AndroidNfc Reader
        poReader = SeProxyService.getInstance().getPlugin(AndroidNfcPlugin.PLUGIN_NAME).getReader(AndroidNfcReader.READER_NAME) as AndroidNfcReader
        poReader.setParameter("FLAG_READER_RESET_STATE", "0")
        poReader.setParameter("FLAG_READER_PRESENCE_CHECK_DELAY", "100")
        poReader.setParameter("FLAG_READER_NO_PLATFORM_SOUNDS", "0")
        poReader.setParameter("FLAG_READER_SKIP_NDEF_CHECK", "0")
        (poReader as ObservableReader).addObserver(this)
        (poReader as ObservableReader).addSeProtocolSetting(SeCommonProtocols.PROTOCOL_ISO14443_4, AndroidNfcProtocolSettings.getSetting(SeCommonProtocols.PROTOCOL_ISO14443_4))
        /* Uncomment to active protocol listening for Mifare ultralight or Mifare Classic (AndroidNfcReader) */
        // (poReader as ObservableReader).addSeProtocolSetting(SeCommonProtocols.PROTOCOL_MIFARE_UL, AndroidNfcProtocolSettings.getAllSettings()[SeCommonProtocols.PROTOCOL_MIFARE_UL])
        // (poReader as ObservableReader).addSeProtocolSetting(SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC, AndroidNfcProtocolSettings.getAllSettings()[SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC])

        // Configuration for Sam Reader. Sam access provided by Famoco lib-secommunication
        // FIXME: Initialisation Delay to handler?
        samReader = SeProxyService.getInstance().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME).getReader(
            AndroidFamocoReader.READER_NAME)
    }

    override fun onResume() {
        super.onResume()
        addActionEvent("Enabling NFC Reader mode")
        poReader.enableNFCReaderMode(this)
        addResultEvent("Please choose a use case")
    }

    override fun onPause() {
        addActionEvent("Stopping PO Read Write Mode")
        try {
            // notify reader that se detection has been switched off
            poReader.stopSeDetection()
            // Disable Reader Mode for NFC Adapter
            poReader.disableNFCReaderMode(this)
        } catch (e: KeyplePluginNotFoundException) {
            Timber.e(e, "NFC Plugin not found")
            addResultEvent("Error: NFC Plugin not found")
        }
        super.onPause()
    }

    override fun onDestroy() {
        (poReader as ObservableReader).removeObserver(this)
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        when (item.itemId) {
            R.id.usecase1 -> {
                clearEvents()
                addHeaderEvent("Running Calypso Read transaction (without SAM)")
                configureCalypsoTransaction(::runPoReadTransactionWithoutSam)
            }
            R.id.usecase2 -> {
                clearEvents()
                addHeaderEvent("Running Calypso Read transaction (with SAM)")
                configureCalypsoTransaction(::runPoReadTransactionWithSam)
            }
            R.id.usecase3 -> {
                clearEvents()
                addHeaderEvent("Running Calypso Read/Write transaction")
                configureCalypsoTransaction(::runPoReadWriteIncreaseTransaction)
            }
            R.id.usecase4 -> {
                clearEvents()
                addHeaderEvent("Running Calypso Read/Write transaction")
                configureCalypsoTransaction(::runPoReadWriteDecreaseTransaction)
            }
        }
        return true
    }

    override fun update(event: ReaderEvent?) {
        addResultEvent("New ReaderEvent received : ${event?.eventType?.name}")
        useCase?.onEventUpdate(event)
    }

    private fun configureCalypsoTransaction(responseProcessor: (selectionsResponse: AbstractDefaultSelectionsResponse) -> Unit) {
        addActionEvent("Prepare Calypso PO Selection with AID: ${CalypsoClassicInfo.AID}")
        try {
            /* Prepare a Calypso PO selection */
            seSelection = SeSelection(MultiSeRequestProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)

            /* Calypso selection: configures a PoSelector with all the desired attributes to make the selection and read additional information afterwards */
            val poSelector = PoSelector(
                SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                PoSelector.PoAidSelector(
                    SeSelector.AidSelector.IsoAid(CalypsoClassicInfo.AID),
                    PoSelector.InvalidatedPo.REJECT),
                "AID: " + CalypsoClassicInfo.AID)

            val poSelectionRequest = PoSelectionRequest(poSelector)

            /* Prepare the reading order and keep the associated parser for later use once the
             selection has been made. */
            readEnvironmentParserIndex = poSelectionRequest.prepareReadRecordsCmd(
                CalypsoClassicInfo.SFI_EnvironmentAndHolder,
                ReadDataStructure.SINGLE_RECORD_DATA, CalypsoClassicInfo.RECORD_NUMBER_1,
                String.format("EnvironmentAndHolder (SFI=%02X))",
                    CalypsoClassicInfo.SFI_EnvironmentAndHolder))

            /*
             * Add the selection case to the current selection (we could have added other cases
             * here)
             */
            seSelection.prepareSelection(poSelectionRequest)

            /*
            * Provide the SeReader with the selection operation to be processed when a PO is
            * inserted.
            */
            (poReader as ObservableReader).setDefaultSelectionRequest(seSelection.selectionOperation,
                ObservableReader.NotificationMode.MATCHED_ONLY)

            useCase = object : UseCase {
                override fun onEventUpdate(event: ReaderEvent?) {
                    CoroutineScope(Dispatchers.Main).launch {
                        when (event?.eventType) {
                            ReaderEvent.EventType.SE_MATCHED -> {
                                addResultEvent("PO detected with AID: ${CalypsoClassicInfo.AID}")
                                responseProcessor(event.defaultSelectionsResponse)
                                (poReader as ObservableReader).notifySeProcessed()
                            }

                            ReaderEvent.EventType.SE_INSERTED -> {
                                addResultEvent("PO detected but AID didn't match with ${CalypsoClassicInfo.AID}")
                                (poReader as ObservableReader).notifySeProcessed()
                            }

                            ReaderEvent.EventType.SE_REMOVED -> {
                                addResultEvent("PO removed")
                            }

                            ReaderEvent.EventType.TIMEOUT_ERROR -> {
                                addResultEvent("PO Timeout")
                            }
                        }
                    }
                    // eventRecyclerView.smoothScrollToPosition(events.size - 1)
                }
            }

            // notify reader that se detection has been launched
            poReader.startSeDetection(ObservableReader.PollingMode.REPEATING)
            addActionEvent("Waiting for PO presentation")
        } catch (e: KeypleBaseException) {
            Timber.e(e)
            addResultEvent("Error : ${e.message}")
        }
    }

    private fun runPoReadTransactionWithSam(selectionsResponse: AbstractDefaultSelectionsResponse) {
        runPoReadTransaction(selectionsResponse, true)
    }

    private fun runPoReadTransactionWithoutSam(selectionsResponse: AbstractDefaultSelectionsResponse) {
        runPoReadTransaction(selectionsResponse, false)
    }

    private fun runPoReadTransaction(selectionsResponse: AbstractDefaultSelectionsResponse, withSam: Boolean) {
        try {
            /*
             * print tag info in View
             */
            addActionEvent("Process selection")
            val selectionsResult = seSelection.processDefaultSelection(selectionsResponse)

            if (selectionsResult.hasActiveSelection()) {
                addResultEvent("Selection successful")
                val calypsoPo = selectionsResult.activeSelection.matchingSe as CalypsoPo

                /*
                 * Retrieve the data read from the parser updated during the selection process
                 */
                addActionEvent("Read environment and holder data")
                val readEnvironmentParser = selectionsResult
                    .activeSelection.getResponseParser(readEnvironmentParserIndex) as ReadRecordsRespPars

                val environmentAndHolder = readEnvironmentParser.records[CalypsoClassicInfo.RECORD_NUMBER_1.toInt()]
                addResultEvent("Environment and Holder file: ${ByteArrayUtil.toHex(environmentAndHolder)}")

                addHeaderEvent("2nd PO exchange: read the event log file")

                val poTransaction = if (withSam) {
                    samReader.setParameter(AndroidFamocoReader.FLAG_READER_RESET_STATE, "")
                    addActionEvent("Init Sam and open channel")
                    val samResource = checkSamAndOpenChannel(samReader)
                    PoTransaction(PoResource(poReader, calypsoPo), samResource, securitySettings)
                } else {
                    PoTransaction(PoResource(poReader, calypsoPo))
                }

                /*
                 * Prepare the reading order and keep the associated parser for later use once the
                 * transaction has been processed.
                 */
                val readEventLogParserIndex = poTransaction.prepareReadRecordsCmd(
                    CalypsoClassicInfo.SFI_EventLog, ReadDataStructure.SINGLE_RECORD_DATA,
                    CalypsoClassicInfo.RECORD_NUMBER_1,
                    String.format("EventLog (SFI=%02X, recnbr=%d))",
                        CalypsoClassicInfo.SFI_EventLog,
                        CalypsoClassicInfo.RECORD_NUMBER_1))

                val readCounterParserIndex = poTransaction.prepareReadRecordsCmd(
                    CalypsoClassicInfo.SFI_Counter1, ReadDataStructure.SINGLE_COUNTER,
                    CalypsoClassicInfo.RECORD_NUMBER_1,
                    String.format("Counter (SFI=%02X, recnbr=%d))",
                        CalypsoClassicInfo.SFI_Counter1,
                        CalypsoClassicInfo.RECORD_NUMBER_1))

                /*
                 * Actual PO communication: send the prepared read order, then close the channel
                 * with the PO
                 */
                addActionEvent("Process PO Command for counter and event logs reading")

                if (withSam) {
                    addActionEvent("Process PO Opening session for transactions")
                    var poProcessStatus = poTransaction.processOpening(PoTransaction.ModificationMode.ATOMIC, PoTransaction.SessionAccessLevel.SESSION_LVL_LOAD, 0, 0)
                    if (!poProcessStatus) {
                        addResultEvent("Error processingOpening failure.")
                        throw IllegalStateException("processingOpening failure.")
                    }
                    addResultEvent("Opening session: SUCCESS")
                    val counter = readCounter(poTransaction, readCounterParserIndex)
                    val eventLog = ByteArrayUtil.toHex(readEventLog(poTransaction, readEventLogParserIndex))

                    addActionEvent("Process PO Closing session")
                    poProcessStatus = poTransaction.processClosing(ChannelControl.CLOSE_AFTER)
                    if (!poProcessStatus) {
                        addResultEvent("Error processClosing failure.")
                        throw IllegalStateException("processClosing failure.")
                    }
                    addResultEvent("Closing session: SUCCESS")

                    // In secured reading, value read elements can only be trusted if the session is closed without error.
                    addResultEvent("Counter value: $counter")
                    addResultEvent("EventLog file: $eventLog")
                } else {
                    val poProcessStatus = poTransaction.processPoCommands(ChannelControl.CLOSE_AFTER)
                    if (!poProcessStatus) {
                        addResultEvent("Error reading failure.")
                        throw IllegalStateException("processPoCommands failure.")
                    }
                    addResultEvent("Counter value: ${readCounter(poTransaction, readCounterParserIndex)}")
                    addResultEvent("EventLog file: ${ByteArrayUtil.toHex(readEventLog(poTransaction, readEventLogParserIndex))}")
                }

                addResultEvent("End of the Calypso PO processing.")
                addResultEvent("You can remove the card now")
            } else {
                addResultEvent("The selection of the PO has failed. Should not have occurred due to the MATCHED_ONLY selection mode.")
            }
        } catch (e: KeypleReaderException) {
            Timber.e(e)
            addResultEvent("Exception: ${e.message}")
        } catch (e: Exception) {
            Timber.e(e)
            addResultEvent("Exception: ${e.message}")
        }
    }

    private fun readCounter(poTransaction: PoTransaction, readCounterParserIndex: Int): Int? {
        val readCounterParser = poTransaction
            .getResponseParser(readCounterParserIndex) as ReadRecordsRespPars
        return readCounterParser.counters[1]
    }

    private fun readEventLog(poTransaction: PoTransaction, readEventLogParserIndex: Int): ByteArray? {
        val readEventLogParser = poTransaction
            .getResponseParser(readEventLogParserIndex) as ReadRecordsRespPars
        return readEventLogParser.records[CalypsoClassicInfo.RECORD_NUMBER_1.toInt()]
    }

    private fun runPoReadWriteIncreaseTransaction(selectionsResponse: AbstractDefaultSelectionsResponse) {
        runPoReadWriteTransaction(selectionsResponse, TransactionType.INCREASE)
    }

    private fun runPoReadWriteDecreaseTransaction(selectionsResponse: AbstractDefaultSelectionsResponse) {
        runPoReadWriteTransaction(selectionsResponse, TransactionType.DECREASE)
    }

    private fun runPoReadWriteTransaction(selectionsResponse: AbstractDefaultSelectionsResponse, transactionType: TransactionType) {
        try {
            addResultEvent("Tag Id : ${poReader.printTagId()}")

            // FIXME: Trick to reopen all channel
            samReader.setParameter(AndroidFamocoReader.FLAG_READER_RESET_STATE, "")
            addActionEvent("Init Sam and open channel")
            val samResource = checkSamAndOpenChannel(samReader)

            addActionEvent("1st PO exchange: aid selection")
            val selectionsResult = seSelection.processDefaultSelection(selectionsResponse)

            if (selectionsResult.hasActiveSelection()) {
                addResultEvent("Calypso PO selection: SUCCESS")
                val calypsoPo = selectionsResult.activeSelection.matchingSe as CalypsoPo
                addResultEvent("AID: ${ByteArrayUtil.fromHex(CalypsoClassicInfo.AID)}")

                val poTransaction = PoTransaction(PoResource(poReader, calypsoPo), samResource, securitySettings)

                /*
                * Open Session for the debit key
                */
                addActionEvent("Process PO Opening session for transactions")
                var poProcessStatus = poTransaction.processOpening(PoTransaction.ModificationMode.ATOMIC, PoTransaction.SessionAccessLevel.SESSION_LVL_LOAD, 0, 0)
                if (!poProcessStatus) {
                    addResultEvent("Error processingOpening failure.")
                    throw IllegalStateException("processingOpening failure.")
                }
                addResultEvent("Opening session: SUCCESS")

                when (transactionType) {
                    TransactionType.INCREASE -> {
                        poTransaction.prepareIncreaseCmd(CalypsoClassicInfo.SFI_Counter1, 0x01, 10, "Increase Counter")
                        addActionEvent("Process PO increase counter by 10")
                        poProcessStatus = poTransaction.processClosing(ChannelControl.CLOSE_AFTER)
                        if (!poProcessStatus) {
                            addResultEvent("Error: Increasing failure.")
                            throw IllegalStateException("Increasing failure.")
                        }
                        addResultEvent("Increase by 10: SUCCESS")
                    }
                    TransactionType.DECREASE -> {
                        /*
                             * A ratification command will be sent (CONTACTLESS_MODE).
                             */
                        poTransaction.prepareDecreaseCmd(CalypsoClassicInfo.SFI_Counter1, 1,
                            1, "Decrease Counter")
                        addActionEvent("Process PO decreasing counter and close transaction")
                        poProcessStatus = poTransaction.processClosing(ChannelControl.CLOSE_AFTER)

                        if (!poProcessStatus) {
                            addResultEvent("Error processClosing failure.")
                            throw IllegalStateException("processClosing failure.")
                        }
                        addResultEvent("Decrease by 1: SUCCESS")
                    }
                }

                addResultEvent("End of the Calypso PO processing.")
                addResultEvent("You can remove the card now")
            } else {
                addResultEvent("The selection of the PO has failed. Should not have occurred due to the MATCHED_ONLY selection mode.")
            }
        } catch (e: KeypleReaderException) {
            Timber.e(e)
            addResultEvent("Exception: ${e.message}")
        } catch (e: Exception) {
            Timber.e(e)
            addResultEvent("Exception: ${e.message}")
        }
    }

    @Throws(KeypleReaderException::class, IllegalStateException::class)
    private fun checkSamAndOpenChannel(samReader: SeReader): SamResource {
        /*
         * check the availability of the SAM doing a ATR based selection, open its physical and
         * logical channels and keep it open
         */
        val samSelection = SeSelection(MultiSeRequestProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)

        val samSelector = SamSelector(SamRevision.C1, null, "Sam Selector")

        samSelection.prepareSelection(SamSelectionRequest(samSelector))

        return try {
            if (samReader.isSePresent) {
                val calypsoSam = samSelection.processExplicitSelection(samReader).activeSelection.matchingSe as CalypsoSam
                if (!calypsoSam.isSelected) {
                    addResultEvent("Error: Unable to open a logical channel for SAM!")
                    throw IllegalStateException("Unable to open a logical channel for SAM!")
                }
                SamResource(samReader, calypsoSam)
            } else {
                addResultEvent("Error: Sam is not present in the reader")
                throw IllegalStateException("Sam is not present in the reader")
            }
        } catch (e: KeypleReaderException) {
            addResultEvent("Error: Reader exception ${e.message}")
            throw IllegalStateException("Reader exception: " + e.message)
        }
    }
}
