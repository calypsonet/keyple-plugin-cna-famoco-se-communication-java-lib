package org.cna.keyple.famoco.demo.activity

import android.nfc.NfcAdapter
import android.view.MenuItem
import androidx.core.view.GravityCompat
import kotlinx.android.synthetic.main.activity_main.drawerLayout
import kotlinx.android.synthetic.main.activity_main.toolbar
import org.cna.keyple.famoco.demo.R
import org.eclipse.keyple.core.selection.SeSelection
import org.eclipse.keyple.core.seproxy.SeProxyService
import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.core.seproxy.event.ObservableReader
import org.eclipse.keyple.core.seproxy.event.ReaderEvent
import org.eclipse.keyple.core.seproxy.exception.KeyplePluginNotFoundException
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPluginFactory
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactory
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcProtocolSettings
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader
import timber.log.Timber

class MainActivity: AbstractExampleActivity(){

    private lateinit var poReader: AndroidNfcReader
    private lateinit var samReader: SeReader

    private lateinit var seSelection: SeSelection

    override fun initContentView() {
        setContentView(R.layout.activity_main)
        initActionBar(toolbar, "Keyple demo", "Famoco Plugin")
    }

    override fun initReaders() {
        SeProxyService.getInstance().registerPlugin(AndroidNfcPluginFactory())
        SeProxyService.getInstance().registerPlugin(AndroidFamocoPluginFactory())

        //Configuration of AndroidNfc Reader
        poReader = SeProxyService.getInstance().getPlugin(AndroidNfcPlugin.PLUGIN_NAME).getReader(AndroidNfcReader.PLUGIN_NAME) as AndroidNfcReader
        poReader.setParameter("FLAG_READER_RESET_STATE", null)
        poReader.setParameter("FLAG_READER_PRESENCE_CHECK_DELAY", "100")
        poReader.setParameter("FLAG_READER_NO_PLATFORM_SOUNDS", "0")
        poReader.setParameter("FLAG_READER_SKIP_NDEF_CHECK", "0")
        (poReader as ObservableReader).addObserver(this)
        (poReader as ObservableReader).addSeProtocolSetting(SeCommonProtocols.PROTOCOL_ISO14443_4, AndroidNfcProtocolSettings.getSetting(SeCommonProtocols.PROTOCOL_ISO14443_4))

    }

    override fun onResume() {
        super.onResume()
        addActionEvent("Starting PO Read Write Mode")
        poReader.enableNFCReaderMode(this)
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
                //configureUseCase1ExplicitSelectionAid()
            }
            R.id.usecase2 -> {
                clearEvents()
                //configureUseCase2DefaultSelectionNotification()
            }
            R.id.usecase3 -> {
                clearEvents()
                //configureUseCase3GroupedMultiSelection()
            }
            R.id.usecase4 -> {
                clearEvents()
                //configureUseCase4SequentialMultiSelection()
            }
        }
        return true
    }

    override fun update(event: ReaderEvent?) {
        addResultEvent("New ReaderEvent received : $event")
        useCase?.onEventUpdate(event)
    }
}
