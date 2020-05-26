/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 */
package org.cna.keyple.famoco.validator.ticketing

import org.eclipse.keyple.calypso.command.po.parser.ReadRecordsRespPars
import org.eclipse.keyple.calypso.command.sam.SamRevision
import org.eclipse.keyple.calypso.transaction.CalypsoPo
import org.eclipse.keyple.calypso.transaction.CalypsoSam
import org.eclipse.keyple.calypso.transaction.SamResource
import org.eclipse.keyple.calypso.transaction.SamSelectionRequest
import org.eclipse.keyple.calypso.transaction.SamSelector
import org.eclipse.keyple.core.selection.SeSelection
import org.eclipse.keyple.core.selection.SelectionsResult
import org.eclipse.keyple.core.seproxy.ChannelControl
import org.eclipse.keyple.core.seproxy.MultiSeRequestProcessing
import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.event.ObservableReader
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException
import org.eclipse.keyple.core.util.ByteArrayUtil
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoReader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractTicketingSession protected constructor(val poReader: SeReader, protected val samReader: SeReader) {
    protected lateinit var calypsoPo: CalypsoPo
    protected lateinit var seSelection: SeSelection
    protected var readEventLogParserIndex = 0
    public var readEnvironmentHolderParserIndex = 0
    protected var readCounterParserIndex = 0
    protected var readContractParserIndex = 0
    var poTypeName: String? = null
        protected set
    var cardContent: CardContent = CardContent()
        protected set
    protected var currentPoSN: ByteArray? = null
    val logger =
        LoggerFactory.getLogger(TicketingSessionExplicitSelection::class.java) as Logger
    protected var calypsoPoIndex = 0
    protected lateinit var readEnvironmentHolderParser: ReadRecordsRespPars
    protected lateinit var readEventLogParser: ReadRecordsRespPars
    protected lateinit var readCounterParser: ReadRecordsRespPars
    protected lateinit var readContractParser: ReadRecordsRespPars
    protected fun pad(text: String, c: Char, length: Int): String {
        val sb = StringBuffer(length)
        sb.append(text)
        for (i in text.length until length) {
            sb.append(c)
        }
        return sb.toString()
    }

    fun processSelectionsResult(selectionsResult: SelectionsResult) {
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

            /*
             * fill in cardContent
             */analyzePoProfile()
        } else {
            poTypeName = "OTHER"
        }
        logger.info("PO type = {}", poTypeName)
    }

    val poIdentification: String
        get() = (ByteArrayUtil.toHex(calypsoPo!!.applicationSerialNumber) + ", "
                + calypsoPo!!.revision.toString())

    /**
     * initial PO content analysis
     *
     * @return
     */
    fun analyzePoProfile(): Boolean {
        var status = false
        if (calypsoPo.isSelected) {
            currentPoSN = calypsoPo.applicationSerialNumber
            cardContent.serialNumber = currentPoSN
            cardContent.poRevision = calypsoPo.revision.toString()
            cardContent.extraInfo = calypsoPo.selectionExtraInfo
            cardContent.environment = readEnvironmentHolderParser.records
            cardContent.eventLog = readEventLogParser.records
            cardContent.counters = readCounterParser.counters
            cardContent.contracts = readContractParser.records
            status = true
        }
        return status
    }

    fun notifySeProcessed() {
        (poReader as ObservableReader).notifySeProcessed()
    }

    @Throws(KeypleReaderException::class, IllegalStateException::class)
    protected fun checkSamAndOpenChannel(samReader: SeReader): SamResource {
        samReader.setParameter(AndroidFamocoReader.FLAG_READER_RESET_STATE, "")
        /*
         * check the availability of the SAM doing a ATR based selection, open its physical and
         * logical channels and keep it open
         */
        val samSelection = SeSelection(MultiSeRequestProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN)

        val samSelector = SamSelector(SamRevision.C1, null, "Sam Selector")

        samSelection.prepareSelection(SamSelectionRequest(samSelector))

        return try {
            if (samReader.isSePresent) {
                val selectionResult = samSelection.processExplicitSelection(samReader);
                if(selectionResult.hasActiveSelection()){
                    val calypsoSam = selectionResult.activeSelection.matchingSe as CalypsoSam
                    if (!calypsoSam.isSelected) {
                        throw IllegalStateException("Unable to open a logical channel for SAM!")
                    }
                    SamResource(samReader, calypsoSam)
                }else{
                    throw IllegalStateException("Sam selection failed")
                }
            } else {
                throw IllegalStateException("Sam is not present in the reader")
            }
        } catch (e: KeypleReaderException) {
            throw IllegalStateException("Reader exception: " + e.message)
        }
    }

}