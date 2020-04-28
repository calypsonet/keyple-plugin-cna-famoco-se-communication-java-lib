/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 ********************************************************************************/
package org.eclipse.keyple.famoco.se.plugin

import com.famoco.secommunication.ALPARProtocol
import com.famoco.secommunication.SmartcardReader
import java.util.HashMap
import org.eclipse.keyple.core.seproxy.plugin.local.AbstractLocalReader
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode
import org.eclipse.keyple.core.util.ByteArrayUtil
import timber.log.Timber

const val READER_NAME = "AndroidFamocoReader"

internal object AndroidFamocoReader : AbstractLocalReader(PLUGIN_NAME, READER_NAME) {

    // FIXME: Utile??
    private val parameters: MutableMap<String, String> = HashMap()

    private val mSmarcardReader: SmartcardReader = SmartcardReader.getInstance()
    private var poweredOn = false
    private var atr: ByteArray? = null

    init {
        Timber.i("Initialize Famoco reader: $READER_NAME")
        SmartcardReader.setDebuggingEnabled(true)
        mSmarcardReader.openReader(115200)
        Timber.d("firmwareVersion = ${mSmarcardReader.firmwareVersion}")
        mSmarcardReader.isAutoNegotiate = true
    }

    override fun getTransmissionMode(): TransmissionMode {
        return TransmissionMode.CONTACTLESS
    }

    override fun getParameters(): MutableMap<String, String> {
        Timber.w("No parameters are supported by AndroidFamocoReader")
        return parameters
    }

    override fun setParameter(key: String, value: String) {
//        if (key == AndroidFamocoReaderOld.FLAG_READER_RESET_STATE) {
//            //closeLogicalChannel()
//            closePhysicalChannel()
//        }
        Timber.w("No parameters are supported by AndroidFamocoReader")
        parameters[key] = value
    }

    override fun isSePresent(): Boolean {
        return mSmarcardReader.isCardPresent
    }

    override fun transmitApdu(apduIn: ByteArray?): ByteArray {
        Timber.d("Data Length to be sent to tag : ${apduIn?.size}")
        Timber.d("Data In : ${ByteArrayUtil.toHex(apduIn)}")
        val apduOut = mSmarcardReader.sendApdu(apduIn)
        Timber.d("Data Out : ${ByteArrayUtil.toHex(apduOut)}")
        return apduOut
    }

    override fun getATR(): ByteArray? {
        Timber.d("ATR = ${ByteArrayUtil.toHex(atr)}")
        return atr
    }

    override fun openPhysicalChannel() {
        atr = mSmarcardReader.powerOn()
        mSmarcardReader.setClockCard(ALPARProtocol.PARAM_CLOCK_FREQUENCY_3_68MHz)
        Timber.d("ATR = ${ByteArrayUtil.toHex(atr)}")
        poweredOn = true
    }

    override fun protocolFlagMatches(protocolFlag: SeProtocol?): Boolean {
        return protocolFlag == SeCommonProtocols.PROTOCOL_ISO7816_3
    }

    override fun isPhysicalChannelOpen(): Boolean {
        return poweredOn
    }

    override fun checkSePresence(): Boolean {
        return isSePresent
    }

    override fun closePhysicalChannel() {
        mSmarcardReader.powerOff()
        poweredOn = false
    }
}
