package org.cna.keyple.famoco.validator.data

import android.app.Activity
import org.cna.keyple.famoco.validator.di.scopes.AppScoped
import org.cna.keyple.famoco.validator.ticketing.CalypsoInfo
import org.cna.keyple.famoco.validator.ticketing.ITicketingSession
import org.cna.keyple.famoco.validator.ticketing.TicketingSession
import org.cna.keyple.famoco.validator.ticketing.TicketingSessionManager
import org.eclipse.keyple.calypso.transaction.PoSelectionRequest
import org.eclipse.keyple.calypso.transaction.PoSelector
import org.eclipse.keyple.calypso.transaction.SamResource
import org.eclipse.keyple.calypso.transaction.SamResourceManager
import org.eclipse.keyple.calypso.transaction.SamResourceManagerDefault
import org.eclipse.keyple.calypso.transaction.SamResourceManagerFactory
import org.eclipse.keyple.core.selection.SeSelection
import org.eclipse.keyple.core.seproxy.ChannelControl
import org.eclipse.keyple.core.seproxy.MultiSeRequestProcessing
import org.eclipse.keyple.core.seproxy.SeProxyService
import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.SeSelector
import org.eclipse.keyple.core.seproxy.event.ObservableReader
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException
import org.eclipse.keyple.core.seproxy.exception.KeyplePluginInstantiationException
import org.eclipse.keyple.core.seproxy.exception.KeyplePluginNotFoundException
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPlugin
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPluginFactory
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoReader
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactory
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcProtocolSettings.getSetting
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import timber.log.Timber
import javax.inject.Inject

@AppScoped
class CardReaderApi @Inject constructor() {

    private lateinit var poReader: SeReader
    private lateinit var samReader: SeReader
    private lateinit var ticketingSessionManager: TicketingSessionManager
    private lateinit var ticketingSession: TicketingSession

    @Throws(KeyplePluginInstantiationException::class, KeypleBaseException::class)
    fun init(observer: ObservableReader.ReaderObserver?) {
        Timber.d("Initialize SEProxy with Android Plugin")
        SeProxyService.getInstance().registerPlugin(AndroidNfcPluginFactory())
        SeProxyService.getInstance().registerPlugin(AndroidFamocoPluginFactory())
        // define task as an observer for ReaderEvents
        poReader = SeProxyService.getInstance().getPlugin(AndroidNfcPlugin.PLUGIN_NAME).getReader(AndroidNfcReader.READER_NAME)
        Timber.d("PO (NFC) reader name: ${poReader.name}")

        poReader.setParameter("FLAG_READER_RESET_STATE", "0")
        poReader.setParameter("FLAG_READER_PRESENCE_CHECK_DELAY", "100")
        poReader.setParameter("FLAG_READER_NO_PLATFORM_SOUNDS", "0")
        poReader.setParameter("FLAG_READER_SKIP_NDEF_CHECK", "0")

        // with this protocol settings we activate the nfc for ISO1443_4 protocol
        poReader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_ISO14443_4, getSetting(SeCommonProtocols.PROTOCOL_ISO14443_4))
        poReader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC, getSetting(SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC))

        /* remove the observer if it already exist */
        (poReader as ObservableReader).addObserver(observer)

        //val samResourceManager = SamResourceManagerFactory.instantiate(SeProxyService.getInstance().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME), CalypsoInfo.SAM_C1_ATR_REGEX)
        val samResourceManager = SamResourceManagerFactory.instantiate(SeProxyService.getInstance().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME), "AndroidFamocoReader")
        samReader = SeProxyService.getInstance().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME).getReader(AndroidFamocoReader.READER_NAME)

        ticketingSessionManager = TicketingSessionManager()

        ticketingSession = ticketingSessionManager.createTicketingSession(poReader, samReader) as TicketingSession

    }

    fun startNfcDetection(activity: Activity){
        (poReader as AndroidNfcReader).enableNFCReaderMode(activity)

        /*
        * Provide the SeReader with the selection operation to be processed when a PO is
        * inserted.
        */
        ticketingSession.prepareAndSetPoDefaultSelection()
//
//
//        (poReader as ObservableReader).setDefaultSelectionRequest(ticketingSession.selectionOperation,
//            ObservableReader.NotificationMode.MATCHED_ONLY)
//
        (poReader as ObservableReader).startSeDetection(ObservableReader.PollingMode.REPEATING)
    }

    fun stopNfcDetection(activity: Activity){
        try {
            // notify reader that se detection has been switched off
            (poReader as AndroidNfcReader).stopSeDetection()
            // Disable Reader Mode for NFC Adapter
            (poReader as AndroidNfcReader).disableNFCReaderMode(activity)
        } catch (e: KeyplePluginNotFoundException) {
            Timber.e(e, "NFC Plugin not found")
        }
    }

    fun getTicketingSession(): TicketingSession{
        return ticketingSession
    }
}