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
package org.eclipse.keyple.famoco.se.plugin

import com.famoco.secommunication.ALPARProtocol
import com.famoco.secommunication.SmartcardReader
import java.util.HashMap
import org.eclipse.keyple.core.seproxy.plugin.local.AbstractLocalReader
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode
import org.eclipse.keyple.core.util.ByteArrayUtil
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoPlugin.Companion.PLUGIN_NAME
import org.eclipse.keyple.famoco.se.plugin.AndroidFamocoReader.Companion.READER_NAME
import timber.log.Timber

internal object AndroidFamocoReaderImpl : AbstractLocalReader(PLUGIN_NAME, READER_NAME), AndroidFamocoReader {

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
        if (key == AndroidFamocoReader.FLAG_READER_RESET_STATE) {
            closeLogicalAndPhysicalChannels()
        }
        Timber.w("No parameters are supported by AndroidFamocoReader")
        parameters[key] = value
    }

    override fun isSePresent(): Boolean {
        // FIXED: Broken in famoco lib?
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
        Timber.d("getATR()")
        Timber.d("ATR = ${ByteArrayUtil.toHex(atr)}")
        return atr
    }

    override fun openPhysicalChannel() {
        Timber.d("openPhysicalChannel()")
        atr = mSmarcardReader.powerOn()
        Timber.d("ATR = ${ByteArrayUtil.toHex(atr)}")
        mSmarcardReader.setClockCard(ALPARProtocol.PARAM_CLOCK_FREQUENCY_3_68MHz)
        poweredOn = true
    }

    override fun protocolFlagMatches(protocolFlag: SeProtocol?): Boolean {
        return protocolFlag == SeCommonProtocols.PROTOCOL_ISO7816_3
    }

    override fun isPhysicalChannelOpen(): Boolean {
        Timber.d("isPhysicalChannelOpen()")
        return poweredOn
    }

    override fun checkSePresence(): Boolean {
        Timber.d("checkSePresence()")
        return isSePresent
    }

    override fun closePhysicalChannel() {
        Timber.d("closePhysicalChannel()")
        mSmarcardReader.powerOff()
        poweredOn = false
    }
}
