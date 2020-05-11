package org.cna.keyple.famoco.validator.data;

import android.util.Log;

import org.cna.keyple.famoco.validator.di.scopes.AppScoped;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;
import org.eclipse.keyple.core.seproxy.exception.KeyplePluginInstantiationException;
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols;
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPlugin;
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPluginFactory;
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoReader;
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPlugin;
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcPluginFactory;
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcProtocolSettings;
import org.eclipse.keyple.plugin.android.nfc.AndroidNfcReader;


import javax.inject.Inject;


@AppScoped
public class CardReaderApi {
    private final static String TAG = CardReaderApi.class.getSimpleName();

    private SeReader poReader, samReader = null;

    @Inject
    public CardReaderApi() {
    }

    public void init(ObservableReader.ReaderObserver observer) throws KeyplePluginInstantiationException, KeypleBaseException {
        Log.d(TAG, "Initialize SEProxy with Android Plugin");
        SeProxyService.getInstance().registerPlugin(new AndroidNfcPluginFactory());
        SeProxyService.getInstance().registerPlugin(new AndroidFamocoPluginFactory());

        // define task as an observer for ReaderEvents
        Log.d(TAG,"Define this view as an observer for ReaderEvents");
        poReader = SeProxyService.getInstance().getPlugin(AndroidNfcPlugin.PLUGIN_NAME).getReader(AndroidNfcReader.Companion.getREADER_NAME());
        Log.d(TAG,"PO (NFC) reader name:" + poReader.getName());
        /* remove the observer if it already exist */
        ((ObservableReader) poReader).addObserver(observer);
        poReader.setParameter("FLAG_READER_RESET_STATE", "0");
        poReader.setParameter("FLAG_READER_PRESENCE_CHECK_DELAY", "100");
        poReader.setParameter("FLAG_READER_NO_PLATFORM_SOUNDS", "0");
        poReader.setParameter("FLAG_READER_SKIP_NDEF_CHECK", "0");
        // with this protocol settings we activate the nfc for ISO1443_4 protocol
        poReader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_ISO14443_4, AndroidNfcProtocolSettings.INSTANCE.getSetting(SeCommonProtocols.PROTOCOL_ISO14443_4));
        poReader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC, AndroidNfcProtocolSettings.INSTANCE.getSetting(SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC));

        samReader  = SeProxyService.getInstance().getPlugin(AndroidFamocoPlugin.PLUGIN_NAME).getReader(AndroidFamocoReader.READER_NAME);
    }
}
