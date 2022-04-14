/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.keyple.plugin.famoco

import com.famoco.secommunication.ALPARProtocol
import com.famoco.secommunication.SmartcardReader
import org.calypsonet.keyple.plugin.famoco.AndroidFamocoReader.Companion.READER_NAME
import org.eclipse.keyple.core.plugin.spi.reader.ReaderSpi
import org.eclipse.keyple.core.util.HexUtil
import timber.log.Timber

internal class AndroidFamocoReaderAdapter :
    ReaderSpi,
    AndroidFamocoReader {

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

    override fun transmitApdu(apduIn: ByteArray?): ByteArray {
        Timber.d("Data Length to be sent to tag : ${apduIn?.size}")
        Timber.d("Data In : ${HexUtil.toHex(apduIn)}")
        val apduOut = mSmarcardReader.sendApdu(apduIn)
        Timber.d("Data Out : ${HexUtil.toHex(apduOut)}")
        return apduOut
    }

    override fun getPowerOnData(): String {
        Timber.d("getAtr()")
        Timber.d("ATR = ${HexUtil.toHex(atr)}")
        return HexUtil.toHex(atr)
    }

    override fun openPhysicalChannel() {
        Timber.d("openPhysicalChannel()")
        atr = mSmarcardReader.powerOn()
        Timber.d("ATR = ${HexUtil.toHex(atr)}")
        mSmarcardReader.setClockCard(ALPARProtocol.PARAM_CLOCK_FREQUENCY_3_68MHz)
        poweredOn = true
    }

    override fun isPhysicalChannelOpen(): Boolean {
        Timber.d("isPhysicalChannelOpen()")
        return poweredOn
    }

    override fun checkCardPresence(): Boolean {
        Timber.d("checkSePresence()")
        return mSmarcardReader.isCardPresent
    }

    override fun closePhysicalChannel() {
        Timber.d("closePhysicalChannel()")
        mSmarcardReader.powerOff()
        poweredOn = false
    }

    override fun isContactless(): Boolean {
        return false
    }

    override fun getName(): String = READER_NAME

    override fun onUnregister() {
        // Do nothing
    }
}
